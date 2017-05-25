// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.buildserver;

import com.google.appinventor.common.version.FtcConstants;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import com.android.sdklib.build.ApkBuilder;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * Main entry point for the YAIL compiler.
 *
 * <p>Supplies entry points for building Young Android projects.
 *
 * @author markf@google.com (Mark Friedman)
 * @author lizlooney@google.com (Liz Looney)
 *
 * [Will 2016/9/20, Refactored {@link #writeAndroidManifest(File)} to
 *   accomodate the new annotations for adding <activity> and <receiver>
 *   elements.]
 */
public final class Compiler {
  /**
   * reading guide:
   * Comp == Component, comp == component, COMP == COMPONENT
   * Ext == External, ext == external, EXT == EXTERNAL
   */

  public static int currentProgress = 10;

  // Kawa and DX processes can use a lot of memory. We only launch one Kawa or DX process at a time.
  private static final Object SYNC_KAWA_OR_DX = new Object();

  private static final String SLASH = File.separator;
  private static final String COLON = File.pathSeparator;

  private static final String WEBVIEW_ACTIVITY_CLASS =
      "com.google.appinventor.components.runtime.WebViewActivity";

  // Copied from SdkLevel.java (which isn't in our class path so we duplicate it here)
  private static final String LEVEL_GINGERBREAD_MR1 = "10";

  public static final String RUNTIME_FILES_DIR = "/" + "files" + "/";

  // Build info constants. Used for permissions, libraries, assets and activities.
  // Must match ComponentProcessor.ARMEABI_V7A_SUFFIX
  private static final String ARMEABI_V7A_SUFFIX = "-v7a";
  // Must match Component.ASSET_DIRECTORY
  private static final String ASSET_DIRECTORY = "component";
  // Must match ComponentListGenerator.ASSETS_TARGET
  private static final String ASSETS_TARGET = "assets";
  // Must match ComponentListGenerator.ACTIVITIES_TARGET
  private static final String ACTIVITIES_TARGET = "activities";
  // Must match ComponentListGenerator.LIBRARIES_TARGET
  public static final String LIBRARIES_TARGET = "libraries";
  // Must match ComponentListGenerator.NATIVE_TARGET
  public static final String NATIVE_TARGET = "native";
  // Must match ComponentListGenerator.PERMISSIONS_TARGET
  private static final String PERMISSIONS_TARGET = "permissions";
  // Must match ComponentListGenerator.BROADCAST_RECEIVERS_TARGET
  private static final String BROADCAST_RECEIVERS_TARGET = "broadcastReceivers";
  
  // TODO(Will): Remove the following target once the deprecated
  //             @SimpleBroadcastReceiver annotation is removed. It should
  //             should remain for the time being because otherwise we'll break
  //             extensions currently using @SimpleBroadcastReceiver.
  //
  // Must match ComponentListGenerator.BROADCAST_RECEIVER_TARGET
  private static final String BROADCAST_RECEIVER_TARGET = "broadcastReceiver";

  // Native library directory names
  private static final String LIBS_DIR_NAME = "libs";
  private static final String ARMEABI_DIR_NAME = "armeabi";
  private static final String ARMEABI_V7A_DIR_NAME = "armeabi-v7a";

  private static final String EXT_COMPS_DIR_NAME = "external_comps";

  private static final String DEFAULT_APP_NAME = "";
  private static final String DEFAULT_ICON = RUNTIME_FILES_DIR + "ya.png";
  // FIRST Tech Challenge: Use the same version code as FtcRobotController-release.apk.
  private static final String DEFAULT_VERSION_CODE = FtcConstants.VERSION_CODE;
  // FIRST Tech Challenge: Use the same version name as FtcRobotController-release.apk.
  private static final String DEFAULT_VERSION_NAME = FtcConstants.VERSION_NAME;
  /*
  private static final String DEFAULT_VERSION_CODE = "1";
  private static final String DEFAULT_VERSION_NAME = "1.0";
  */
  private static final String DEFAULT_MIN_SDK = "4";

  /*
   * Resource paths to yail runtime, runtime library files and sdk tools.
   * To get the real file paths, call getResource() with one of these constants.
   */
  private static final String ACRA_RUNTIME =
      RUNTIME_FILES_DIR + "acra-4.4.0.jar";
  private static final String ANDROID_RUNTIME =
      RUNTIME_FILES_DIR + "android.jar";
  private static final String COMP_BUILD_INFO =
      RUNTIME_FILES_DIR + "simple_components_build_info.json";
  private static final String DX_JAR =
      RUNTIME_FILES_DIR + "dx.jar";
  private static final String KAWA_RUNTIME =
      RUNTIME_FILES_DIR + "kawa.jar";
  private static final String SIMPLE_ANDROID_RUNTIME_JAR =
      RUNTIME_FILES_DIR + "AndroidRuntime.jar";

  private static final String LINUX_AAPT_TOOL =
      "/tools/linux/aapt";
  private static final String LINUX_ZIPALIGN_TOOL =
      "/tools/linux/zipalign";
  private static final String MAC_AAPT_TOOL =
      "/tools/mac/aapt";
  private static final String MAC_ZIPALIGN_TOOL =
      "/tools/mac/zipalign";
  private static final String WINDOWS_AAPT_TOOL =
      "/tools/windows/aapt";
  private static final String WINDOWS_ZIPALIGN_TOOL =
      "/tools/windows/zipalign";

  @VisibleForTesting
  static final String YAIL_RUNTIME = RUNTIME_FILES_DIR + "runtime.scm";

  private final ConcurrentMap<String, Set<String>> assetsNeeded =
      new ConcurrentHashMap<String, Set<String>>();
  private final ConcurrentMap<String, Set<String>> activitiesNeeded =
      new ConcurrentHashMap<String, Set<String>>();
  private final ConcurrentMap<String, Set<String>> broadcastReceiversNeeded =
      new ConcurrentHashMap<String, Set<String>>();
  private final ConcurrentMap<String, Set<String>> libsNeeded =
      new ConcurrentHashMap<String, Set<String>>();
  private final ConcurrentMap<String, Set<String>> nativeLibsNeeded =
      new ConcurrentHashMap<String, Set<String>>();
  private final ConcurrentMap<String, Set<String>> permissionsNeeded =
      new ConcurrentHashMap<String, Set<String>>();
  private final Set<String> uniqueLibsNeeded = Sets.newHashSet();
  
  // TODO(Will): Remove the following Set once the deprecated
  //             @SimpleBroadcastReceiver annotation is removed. It should
  //             should remain for the time being because otherwise we'll break
  //             extensions currently using @SimpleBroadcastReceiver.
  private final ConcurrentMap<String, Set<String>> componentBroadcastReceiver =
      new ConcurrentHashMap<String, Set<String>>();

  /**
   * Map used to hold the names and paths of resources that we've written out
   * as temp files.
   * Don't use this map directly. Please call getResource() with one of the
   * constants above to get the (temp file) path to a resource.
   */
  private static final ConcurrentMap<String, File> resources =
      new ConcurrentHashMap<String, File>();

  // TODO(user,lizlooney): i18n here and in lines below that call String.format(...)
  private static final String COMPILATION_ERROR =
      "Error: Your build failed due to an error when compiling %s.\n";
  private static final String ERROR_IN_STAGE =
      "Error: Your build failed due to an error in the %s stage, " +
      "not because of an error in your program.\n";
  private static final String ICON_ERROR =
      "Error: Your build failed because %s cannot be used as the application icon.\n";
  private static final String NO_USER_CODE_ERROR =
      "Error: No user code exists.\n";

  private final int childProcessRamMb;  // Maximum ram that can be used by a child processes, in MB.
  private final boolean isForCompanion;
  private final Project project;
  private final PrintStream out;
  private final PrintStream err;
  private final PrintStream userErrors;

  // FIRST Tech Challenge: If the project has an FtcRobotController component, hasFtcRobotController
  // is true.
  private final boolean hasFtcRobotController;

  private File libsDir; // The directory that will contain any native libraries for packaging
  private String dexCacheDir;
  private boolean hasSecondDex = false; // True if classes2.dex should be added to the APK

  private JSONArray simpleCompsBuildInfo;
  private JSONArray extCompsBuildInfo;
  private Set<String> simpleCompTypes;  // types needed by the project
  private Set<String> extCompTypes; // types needed by the project

  private static final Logger LOG = Logger.getLogger(Compiler.class.getName());

  /*
   * Generate the set of Android permissions needed by this project.
   */
  @VisibleForTesting
  void generatePermissions() {
    try {
      loadJsonInfo(permissionsNeeded, PERMISSIONS_TARGET);
      if (project != null) {    // Only do this if we have a project (testing doesn't provide one :-( ).
        LOG.log(Level.INFO, "usesLocation = " + project.getUsesLocation());
        if (project.getUsesLocation().equals("True")) { // Add location permissions if any WebViewer requests it
          Set<String> locationPermissions = Sets.newHashSet(); // via a Property.
          // See ProjectEditor.recordLocationSettings()
          locationPermissions.add("android.permission.ACCESS_FINE_LOCATION");
          locationPermissions.add("android.permission.ACCESS_COARSE_LOCATION");
          locationPermissions.add("android.permission.ACCESS_MOCK_LOCATION");
          permissionsNeeded.put("com.google.appinventor.components.runtime.WebViewer", locationPermissions);
        }
      }
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Permissions"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Permissions"));
    }

    int n = 0;
    for (String type : permissionsNeeded.keySet()) {
      n += permissionsNeeded.get(type).size();
    }

    System.out.println("Permissions needed, n = " + n);
  }

  // Just used for testing
  @VisibleForTesting
  Map<String,Set<String>> getPermissions() {
    return permissionsNeeded;
  }
  
  // Just used for testing
  @VisibleForTesting
  Map<String, Set<String>> getBroadcastReceivers() {
    return broadcastReceiversNeeded;
  }
  
  // Just used for testing
  @VisibleForTesting
  Map<String, Set<String>> getActivities() {
    return activitiesNeeded;
  }

  /*
   * Generate the set of Android libraries needed by this project.
   */
  @VisibleForTesting
  void generateLibNames() {
    try {
      loadJsonInfo(libsNeeded, LIBRARIES_TARGET);
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Libraries"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Libraries"));
    }

    int n = 0;
    for (String type : libsNeeded.keySet()) {
      n += libsNeeded.get(type).size();
    }

    System.out.println("Libraries needed, n = " + n);
  }

  /*
   * Generate the set of conditionally included libraries needed by this project.
   */
  @VisibleForTesting
  void generateNativeLibNames() {
    try {
      loadJsonInfo(nativeLibsNeeded, NATIVE_TARGET);
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Native Libraries"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Native Libraries"));
    }

    int n = 0;
    for (String type : nativeLibsNeeded.keySet()) {
      n += nativeLibsNeeded.get(type).size();
    }

    System.out.println("Native Libraries needed, n = " + n);
  }

  /*
   * Generate the set of conditionally included assets needed by this project.
   */
  @VisibleForTesting
  void generateAssets() {
    try {
      loadJsonInfo(assetsNeeded, ASSETS_TARGET);
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Assets"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Assets"));
    }

    int n = 0;
    for (String type : assetsNeeded.keySet()) {
      n += assetsNeeded.get(type).size();
    }

    System.out.println("Component assets needed, n = " + n);
  }

  /*
   * Generate the set of conditionally included activities needed by this project.
   */
  @VisibleForTesting
  void generateActivities() {
    try {
      loadJsonInfo(activitiesNeeded, ACTIVITIES_TARGET);
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Activities"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Activities"));
    }

    int n = 0;
    for (String type : activitiesNeeded.keySet()) {
      n += activitiesNeeded.get(type).size();
    }

    System.out.println("Component activities needed, n = " + n);
  }

  /*
   * Generate a set of conditionally included broadcast receivers needed by this project.
   */
  @VisibleForTesting
  void generateBroadcastReceivers() {
    try {
      loadJsonInfo(broadcastReceiversNeeded, BROADCAST_RECEIVERS_TARGET);
    }
    catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceivers"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceivers"));
    }
  }
  
  /*
   * TODO(Will): Remove this method once the deprecated @SimpleBroadcastReceiver
   *             annotation is removed. This should remain for the time being so
   *             that we don't break extensions currently using the
   *             @SimpleBroadcastReceiver annotation.
   */
  @VisibleForTesting
  void generateBroadcastReceiver() {
    try {
      loadJsonInfo(componentBroadcastReceiver, BROADCAST_RECEIVER_TARGET);
    }
    catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceiver"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceiver"));
    }
  }


  // This patches around a bug in AAPT (and other placed in Android)
  // where an ampersand in the name string breaks AAPT.
  private String cleanName(String name) {
    return name.replace("&", "and");
  }

  /*
   * Creates an AndroidManifest.xml file needed for the Android application.
   */
  private boolean writeAndroidManifest(File manifestFile) {
    // Create AndroidManifest.xml
    String mainClass = project.getMainClass();
    String packageName = Signatures.getPackageName(mainClass);
    // FIRST Tech Challenge: Use the same package as FtcRobotController-release.apk.
    if (hasFtcRobotController) {
      packageName = FtcConstants.PACKAGE;
    }
    String className = Signatures.getClassName(mainClass);
    String projectName = project.getProjectName();
    String vCode = (project.getVCode() == null) ? DEFAULT_VERSION_CODE : project.getVCode();
    String vName = (project.getVName() == null) ? DEFAULT_VERSION_NAME : cleanName(project.getVName());
    String aName = (project.getAName() == null) ? DEFAULT_APP_NAME : cleanName(project.getAName());
    String minSDK = DEFAULT_MIN_SDK;
    LOG.log(Level.INFO, "VCode: " + project.getVCode());
    LOG.log(Level.INFO, "VName: " + project.getVName());

    // TODO(user): Use com.google.common.xml.XmlWriter
    try {
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(manifestFile), "UTF-8"));
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      // TODO(markf) Allow users to set versionCode and versionName attributes.
      // See http://developer.android.com/guide/publishing/publishing.html for
      // more info.
      out.write("<manifest " +
          "xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
          "package=\"" + packageName + "\" " +
          // TODO(markf): uncomment the following line when we're ready to enable publishing to the
          // Android Market.
         "android:versionCode=\"" + vCode +"\" " + "android:versionName=\"" + vName + "\" " +
          ">\n");

      // If we are building the Wireless Debugger (AppInventorDebugger) add the uses-feature tag which
      // is used by the Google Play store to determine which devices the app is available for. By adding
      // these lines we indicate that we use these features BUT THAT THEY ARE NOT REQUIRED so it is ok
      // to make the app available on devices that lack the feature. Without these lines the Play Store
      // makes a guess based on permissions and assumes that they are required features.
      if (isForCompanion) {
          out.write("  <uses-feature android:name=\"android.hardware.bluetooth\" android:required=\"false\" />\n");
          out.write("  <uses-feature android:name=\"android.hardware.location\" android:required=\"false\" />\n");
          out.write("  <uses-feature android:name=\"android.hardware.telephony\" android:required=\"false\" />\n");
          out.write("  <uses-feature android:name=\"android.hardware.location.network\" android:required=\"false\" />\n");
          out.write("  <uses-feature android:name=\"android.hardware.location.gps\" android:required=\"false\" />\n");
          out.write("  <uses-feature android:name=\"android.hardware.microphone\" android:required=\"false\" />\n");
          out.write("  <uses-feature android:name=\"android.hardware.touchscreen\" android:required=\"false\" />\n");
          out.write("  <uses-feature android:name=\"android.hardware.camera\" android:required=\"false\" />\n");
          out.write("  <uses-feature android:name=\"android.hardware.camera.autofocus\" android:required=\"false\" />\n");
          out.write("  <uses-feature android:name=\"android.hardware.wifi\" />\n"); // We actually require wifi
      }

      // Firebase requires at least API 10 (Gingerbread MR1)
      if (simpleCompTypes.contains("com.google.appinventor.components.runtime.FirebaseDB") && !isForCompanion) {
        minSDK = LEVEL_GINGERBREAD_MR1;
      }

      // FIRST Tech Challenge: Add features from ftc_sdk/lib/RobotCore/src/main/AndroidManifest.xml
      if (hasFtcRobotController) {
        out.write("  <uses-feature android:name=\"android.hardware.usb.accessory\" />\n");
        out.write("  <uses-feature android:glEsVersion=\"0x00020000\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.camera\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.bluetooth\" />\n");
        minSDK = FtcConstants.SDK_VERSION;
      }

      // make permissions unique by putting them in one set
      Set<String> permissions = Sets.newHashSet();
      for (Set<String> compPermissions : permissionsNeeded.values()) {
        permissions.addAll(compPermissions);
      }

      for (String permission : permissions) {
        out.write("  <uses-permission android:name=\"" + permission + "\" />\n");
      }

      if (isForCompanion) {      // This is so ACRA can do a logcat on phones older then Jelly Bean
        out.write("  <uses-permission android:name=\"android.permission.READ_LOGS\" />\n");
      }

      // TODO(markf): Change the minSdkVersion below if we ever require an SDK beyond 1.5.
      // The market will use the following to filter apps shown to devices that don't support
      // the specified SDK version.  We right now support building for minSDK 4.
      // We might also want to allow users to specify minSdk version or targetSDK version.
      out.write("  <uses-sdk android:minSdkVersion=\"" + minSDK + "\" />\n");

      // FIRST Tech Challenge:
      if (hasFtcRobotController) {
        out.write("  <uses-sdk android:targetSdkVersion=\"" + FtcConstants.TARGET_SDK_VERSION + "\" />\n");
        ftcAddApplicationToManifest(out);
      } else {

      out.write("  <application ");

      // TODO(markf): The preparing to publish doc at
      // http://developer.android.com/guide/publishing/preparing.html suggests removing the
      // 'debuggable=true' but I'm not sure that our users would want that while they're still
      // testing their packaged apps.  Maybe we should make that an option, somehow.
      // TODONE(jis): Turned off debuggable. No one really uses it and it represents a security
      // risk for App Inventor App end-users.
      out.write("android:debuggable=\"false\" ");
      if (aName.equals("")) {
        out.write("android:label=\"" + projectName + "\" ");
      } else {
        out.write("android:label=\"" + aName + "\" ");
      }
      out.write("android:icon=\"@drawable/ya\" ");
      if (isForCompanion) {              // This is to hook into ACRA
        out.write("android:name=\"com.google.appinventor.components.runtime.ReplApplication\" ");
      } else {
        out.write("android:name=\"com.google.appinventor.components.runtime.multidex.MultiDexApplication\" ");
      }
      out.write(">\n");

      for (Project.SourceDescriptor source : project.getSources()) {
        String formClassName = source.getQualifiedName();
        // String screenName = formClassName.substring(formClassName.lastIndexOf('.') + 1);
        boolean isMain = formClassName.equals(mainClass);

        if (isMain) {
          // The main activity of the application.
          out.write("    <activity android:name=\"." + className + "\" ");
        } else {
          // A secondary activity of the application.
          out.write("    <activity android:name=\"" + formClassName + "\" ");
        }

        // This line is here for NearField and NFC.   It keeps the activity from
        // restarting every time NDEF_DISCOVERED is signaled.
        // TODO:  Check that this doesn't screw up other components.  Also, it might be
        // better to do this programmatically when the NearField component is created, rather
        // than here in the manifest.
        if (simpleCompTypes.contains("com.google.appinventor.components.runtime.NearField") &&
            !isForCompanion && isMain) {
          out.write("android:launchMode=\"singleTask\" ");
        } else if (isMain && isForCompanion) {
          out.write("android:launchMode=\"singleTop\" ");
        }

        out.write("android:windowSoftInputMode=\"stateHidden\" ");

        // The keyboard option prevents the app from stopping when a external (bluetooth)
        // keyboard is attached.
        out.write("android:configChanges=\"orientation|keyboardHidden|keyboard\">\n");


        out.write("      <intent-filter>\n");
        out.write("        <action android:name=\"android.intent.action.MAIN\" />\n");
        if (isMain) {
          out.write("        <category android:name=\"android.intent.category.LAUNCHER\" />\n");
        }
        out.write("      </intent-filter>\n");

        if (simpleCompTypes.contains("com.google.appinventor.components.runtime.NearField") &&
            !isForCompanion && isMain) {
          //  make the form respond to NDEF_DISCOVERED
          //  this will trigger the form's onResume method
          //  For now, we're handling text/plain only,but we can add more and make the Nearfield
          // component check the type.
          out.write("      <intent-filter>\n");
          out.write("        <action android:name=\"android.nfc.action.NDEF_DISCOVERED\" />\n");
          out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
          out.write("        <data android:mimeType=\"text/plain\" />\n");
          out.write("      </intent-filter>\n");
        }
        out.write("    </activity>\n");
      }
      }

      // Collect any additional <application> subelements into a single set.
      Set<Map.Entry<String, Set<String>>> subelements = Sets.newHashSet();
      subelements.addAll(activitiesNeeded.entrySet());
      subelements.addAll(broadcastReceiversNeeded.entrySet());
      
      
      // If any component needs to register additional activities or
      // broadcast receivers, insert them into the manifest here.
      if (!subelements.isEmpty()) {
        for (Map.Entry<String, Set<String>> componentSubElSetPair : subelements) {
          Set<String> subelementSet = componentSubElSetPair.getValue();
          for (String subelement : subelementSet) {
            out.write(subelement);
          }
        }
      }
  
      // TODO(Will): Remove the following legacy code once the deprecated
      //             @SimpleBroadcastReceiver annotation is removed. It should
      //             should remain for the time being because otherwise we'll break
      //             extensions currently using @SimpleBroadcastReceiver.
      
      // Collect any legacy simple broadcast receivers
      Set<String> simpleBroadcastReceivers = Sets.newHashSet();
      for (String componentType : componentBroadcastReceiver.keySet()) {
        simpleBroadcastReceivers.addAll(componentBroadcastReceiver.get(componentType));
      }
      
      // The format for each legacy Broadcast Receiver in simpleBroadcastReceivers is
      // "className,Action1,Action2,..." where the class name is mandatory, and
      // actions are optional (and as many as needed).
      for (String broadcastReceiver : simpleBroadcastReceivers) {
        String[] brNameAndActions = broadcastReceiver.split(",");
        if (brNameAndActions.length == 0) continue;
        out.write(
            "<receiver android:name=\"" + brNameAndActions[0] + "\" >\n");
        if (brNameAndActions.length > 1){
          out.write("  <intent-filter>\n");
          for (int i = 1; i < brNameAndActions.length; i++) {
            out.write("    <action android:name=\"" + brNameAndActions[i] + "\" />\n");
          }
          out.write("  </intent-filter>\n");
        }
        out.write("</receiver> \n");
      }

      out.write("  </application>\n");
      out.write("</manifest>\n");
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "manifest"));
      return false;
    }

    return true;
  }

  /**
   * Builds a YAIL project.
   *
   * @param project  project to build
   * @param compTypes component types used in the project
   * @param out  stdout stream for compiler messages
   * @param err  stderr stream for compiler messages
   * @param userErrors stream to write user-visible error messages
   * @param keystoreFilePath
   * @param childProcessRam   maximum RAM for child processes, in MBs.
   * @return  {@code true} if the compilation succeeds, {@code false} otherwise
   * @throws JSONException
   * @throws IOException
   */
  public static boolean compile(Project project, Set<String> compTypes,
                                PrintStream out, PrintStream err, PrintStream userErrors,
                                boolean isForCompanion, String keystoreFilePath,
                                int childProcessRam, String dexCacheDir) throws IOException, JSONException {
    long start = System.currentTimeMillis();

    // Create a new compiler instance for the compilation
    Compiler compiler = new Compiler(project, compTypes, out, err, userErrors, isForCompanion,
                                     childProcessRam, dexCacheDir);

    compiler.generateAssets();
    compiler.generateActivities();
    compiler.generateBroadcastReceivers();
    compiler.generateLibNames();
    compiler.generateNativeLibNames();
    compiler.generatePermissions();
  
    // TODO(Will): Remove the following call once the deprecated
    //             @SimpleBroadcastReceiver annotation is removed. It should
    //             should remain for the time being because otherwise we'll break
    //             extensions currently using @SimpleBroadcastReceiver.
    compiler.generateBroadcastReceiver();

    // Create build directory.
    File buildDir = createDir(project.getBuildDirectory());

    // Prepare application icon.
    out.println("________Preparing application icon");
    File resDir = createDir(buildDir, "res");
    File drawableDir = createDir(resDir, "drawable");
    if (!compiler.prepareApplicationIcon(new File(drawableDir, "ya.png"))) {
      return false;
    }
    setProgress(15);

    // Create anim directory and animation xml files
    out.println("________Creating animation xml");
    File animDir = createDir(resDir, "anim");
    if (!compiler.createAnimationXml(animDir)) {
      return false;
    }

    // FIRST Tech Challenge: Add resources, assets, and native libraries.
    if (compiler.hasFtcRobotController) {
      // Copy resources used in FTC libraries and components.
      if (!compiler.ftcCreateResources(resDir)) {
        return false;
      }

      // Copy assets used in FTC libraries and components.
      if (!compiler.ftcCreateAssets(project.getAssetsDirectory())) {
        return false;
      }

      // Copy native libraries used in FTC libraries and components.
      if (!compiler.ftcCreateNativeLibs(buildDir)) {
        return false;
      }
    }

    // Generate AndroidManifest.xml
    out.println("________Generating manifest file");
    File manifestFile = new File(buildDir, "AndroidManifest.xml");
    if (!compiler.writeAndroidManifest(manifestFile)) {
      return false;
    }
    setProgress(20);

    // Insert native libraries
    out.println("________Attaching native libraries");
    if (!compiler.insertNativeLibs(buildDir)) {
      return false;
    }

    // Add raw assets to sub-directory of project assets.
    out.println("________Attaching component assets");
    if (!compiler.attachCompAssets()) {
      return false;
    }

    // Create class files.
    out.println("________Compiling source files");
    File classesDir = createDir(buildDir, "classes");
    if (!compiler.generateClasses(classesDir)) {
      return false;
    }
    setProgress(35);

    // FIRST Tech Challenge: Generate and compile R.java files used in FTC libraries.
    if (compiler.hasFtcRobotController) {
      // Generate R.java files used in FTC libraries.
      out.println("________Generating R.java files");
      File genDir = createDirectory(buildDir, "gen");
      String[] packages = {
        "com.google.blocks",
        "com.qualcomm.ftccommon",
        "com.qualcomm.hardware",
        "com.qualcomm.robotcore",
        "org.firstinspires.inspection"
      };
      List<String> genFileNames = Lists.newArrayListWithCapacity(packages.length);
      for (String customPackage : packages) {
        if (!compiler.ftcRunAaptPackage(manifestFile, resDir, genDir, customPackage)) {
          return false;
        }
        genFileNames.add(genDir.getAbsolutePath() + File.separatorChar + 
            customPackage.replace('.', File.separatorChar) + File.separatorChar + "R.java");
      }
      // Compile the generated R.java files.
      out.println("________Compiling R.java files");
      if (!compiler.ftcRunJavac(classesDir, genFileNames)) {
        return false;
      }
    }

    // Invoke dx on class files
    out.println("________Invoking DX");
    // TODO(markf): Running DX is now pretty slow (~25 sec overhead the first time and ~15 sec
    // overhead for subsequent runs).  I think it's because of the need to dx the entire
    // kawa runtime every time.  We should probably only do that once and then copy all the
    // kawa runtime dx files into the generated classes.dex (which would only contain the
    // files compiled for this project).
    // Aargh.  It turns out that there's no way to manipulate .dex files to do the above.  An
    // Android guy suggested an alternate approach of shipping the kawa runtime .dex file as
    // data with the application and then creating a new DexClassLoader using that .dex file
    // and with the original app class loader as the parent of the new one.
    // TODONE(zhuowei): Now using the new Android DX tool to merge dex files
    // Needs to specify a writable cache dir on the command line that persists after shutdown
    // Each pre-dexed file is identified via its MD5 hash (since the standard Android SDK's
    // method of identifying via a hash of the path won't work when files
    // are copied into temporary storage) and processed via a hacked up version of
    // Android SDK's Dex Ant task
    File tmpDir = createDirectory(buildDir, "tmp");
    String dexedClassesDir = tmpDir.getAbsolutePath();
    if (!compiler.runDx(classesDir, dexedClassesDir, false)) {
      return false;
    }
    setProgress(85);

    // Invoke aapt to package everything up
    out.println("________Invoking AAPT");
    File deployDir = createDir(buildDir, "deploy");
    String tmpPackageName = deployDir.getAbsolutePath() + SLASH +
        project.getProjectName() + ".ap_";
    if (!compiler.runAaptPackage(manifestFile, resDir, tmpPackageName)) {
      return false;
    }
    setProgress(90);

    // Seal the apk with ApkBuilder
    out.println("________Invoking ApkBuilder");
    String apkAbsolutePath = deployDir.getAbsolutePath() + SLASH +
        project.getProjectName() + ".apk";
    if (!compiler.runApkBuilder(apkAbsolutePath, tmpPackageName, dexedClassesDir)) {
      return false;
    }
    setProgress(95);

    // Sign the apk file
    out.println("________Signing the apk file");
    if (!compiler.runJarSigner(apkAbsolutePath, keystoreFilePath)) {
      return false;
    }

    // ZipAlign the apk file
    out.println("________ZipAligning the apk file");
    if (!compiler.runZipAlign(apkAbsolutePath, tmpDir)) {
      return false;
    }

    setProgress(100);

    out.println("Build finished in " +
        ((System.currentTimeMillis() - start) / 1000.0) + " seconds");

    return true;
  }

  /*
   * Creates all the animation xml files.
   */
  private boolean createAnimationXml(File animDir) {
    // Store the filenames, and their contents into a HashMap
    // so that we can easily add more, and also to iterate
    // through creating the files.
    Map<String, String> files = new HashMap<String, String>();
    files.put("fadein.xml", AnimationXmlConstants.FADE_IN_XML);
    files.put("fadeout.xml", AnimationXmlConstants.FADE_OUT_XML);
    files.put("hold.xml", AnimationXmlConstants.HOLD_XML);
    files.put("zoom_enter.xml", AnimationXmlConstants.ZOOM_ENTER);
    files.put("zoom_exit.xml", AnimationXmlConstants.ZOOM_EXIT);
    files.put("zoom_enter_reverse.xml", AnimationXmlConstants.ZOOM_ENTER_REVERSE);
    files.put("zoom_exit_reverse.xml", AnimationXmlConstants.ZOOM_EXIT_REVERSE);
    files.put("slide_exit.xml", AnimationXmlConstants.SLIDE_EXIT);
    files.put("slide_enter.xml", AnimationXmlConstants.SLIDE_ENTER);
    files.put("slide_exit_reverse.xml", AnimationXmlConstants.SLIDE_EXIT_REVERSE);
    files.put("slide_enter_reverse.xml", AnimationXmlConstants.SLIDE_ENTER_REVERSE);
    files.put("slide_v_exit.xml", AnimationXmlConstants.SLIDE_V_EXIT);
    files.put("slide_v_enter.xml", AnimationXmlConstants.SLIDE_V_ENTER);
    files.put("slide_v_exit_reverse.xml", AnimationXmlConstants.SLIDE_V_EXIT_REVERSE);
    files.put("slide_v_enter_reverse.xml", AnimationXmlConstants.SLIDE_V_ENTER_REVERSE);

    for (String filename : files.keySet()) {
      File file = new File(animDir, filename);
      if (!writeXmlFile(file, files.get(filename))) {
        return false;
      }
    }
    return true;
  }

  /*
   * Writes the given string input to the provided file.
   */
  private boolean writeXmlFile(File file, String input) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(file));
      writer.write(input);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /*
   * Runs ApkBuilder by using the API instead of calling its main method because the main method
   * can call System.exit(1), which will bring down our server.
   */
  private boolean runApkBuilder(String apkAbsolutePath, String zipArchive, String dexedClassesDir) {
    try {
      ApkBuilder apkBuilder =
          new ApkBuilder(apkAbsolutePath, zipArchive,
            dexedClassesDir + File.separator + "classes.dex", null, System.out);
      if (hasSecondDex) {
        apkBuilder.addFile(new File(dexedClassesDir + File.separator + "classes2.dex"),
          "classes2.dex");
      }
      apkBuilder.sealApk();
      return true;
    } catch (Exception e) {
      // This is fatal.
      e.printStackTrace();
      LOG.warning("YAIL compiler - ApkBuilder failed.");
      err.println("YAIL compiler - ApkBuilder failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "ApkBuilder"));
      return false;
    }
  }

  /**
   * Creates a new YAIL compiler.
   *
   * @param project  project to build
   * @param compTypes component types used in the project
   * @param out  stdout stream for compiler messages
   * @param err  stderr stream for compiler messages
   * @param userErrors stream to write user-visible error messages
   * @param childProcessMaxRam  maximum RAM for child processes, in MBs.
   */
  @VisibleForTesting
  Compiler(Project project, Set<String> compTypes, PrintStream out, PrintStream err,
           PrintStream userErrors, boolean isForCompanion,
           int childProcessMaxRam, String dexCacheDir) {
    this.project = project;

    prepareCompTypes(compTypes);
    readBuildInfo();

    this.out = out;
    this.err = err;
    this.userErrors = userErrors;
    this.isForCompanion = isForCompanion;
    this.childProcessRamMb = childProcessMaxRam;
    this.dexCacheDir = dexCacheDir;

    // FIRST Tech Challenge: If the project has an FtcRobotController component, hasFtcRobotController is true.
    hasFtcRobotController =
        simpleCompTypes.contains("com.google.appinventor.components.runtime.FtcRobotController") &&
        !isForCompanion;
  }

  /*
   * Runs the Kawa compiler in a separate process to generate classes. Returns false if not able to
   * create a class file for every source file in the project.
   *
   * As a side effect, we generate uniqueLibsNeeded which contains a set of libraries used by
   * runDx. Each library appears in the set only once (which is why it is a set!). This is
   * important because when we Dex the libraries, a given library can only appear once.
   *
   */
  private boolean generateClasses(File classesDir) {
    try {
      List<Project.SourceDescriptor> sources = project.getSources();
      List<String> sourceFileNames = Lists.newArrayListWithCapacity(sources.size());
      List<String> classFileNames = Lists.newArrayListWithCapacity(sources.size());
      boolean userCodeExists = false;
      for (Project.SourceDescriptor source : sources) {
        String sourceFileName = source.getFile().getAbsolutePath();
        LOG.log(Level.INFO, "source file: " + sourceFileName);
        int srcIndex = sourceFileName.indexOf(File.separator + ".." + File.separator + "src" + File.separator);
        String sourceFileRelativePath = sourceFileName.substring(srcIndex + 8);
        String classFileName = (classesDir.getAbsolutePath() + "/" + sourceFileRelativePath)
          .replace(YoungAndroidConstants.YAIL_EXTENSION, ".class");

        // Check whether user code exists by seeing if a left parenthesis exists at the beginning of
        // a line in the file
        // TODO(user): Replace with more robust test of empty source file.
        if (!userCodeExists) {
          Reader fileReader = new FileReader(sourceFileName);
          try {
            while (fileReader.ready()) {
              int c = fileReader.read();
              if (c == '(') {
                userCodeExists = true;
                break;
              }
            }
          } finally {
            fileReader.close();
          }
        }
        sourceFileNames.add(sourceFileName);
        classFileNames.add(classFileName);
      }

      if (!userCodeExists) {
        userErrors.print(NO_USER_CODE_ERROR);
        return false;
      }

      // Construct the class path including component libraries (jars)
      String classpath =
        getResource(KAWA_RUNTIME) + COLON +
        getResource(ACRA_RUNTIME) + COLON +
        getResource(SIMPLE_ANDROID_RUNTIME_JAR) + COLON;

      // attach the jars of external comps
      for (String type : extCompTypes) {
        String sourcePath = getExtCompDirPath(type) + SIMPLE_ANDROID_RUNTIME_JAR;
        classpath += sourcePath + COLON;
      }

      // Add component library names to classpath
      for (String type : libsNeeded.keySet()) {
        for (String lib : libsNeeded.get(type)) {
          String sourcePath = "";
          String pathSuffix = RUNTIME_FILES_DIR + lib;

          if (simpleCompTypes.contains(type)) {
            sourcePath = getResource(pathSuffix);
          } else if (extCompTypes.contains(type)) {
            sourcePath = getExtCompDirPath(type) + pathSuffix;
          } else {
            userErrors.print(String.format(ERROR_IN_STAGE, "Compile"));
            return false;
          }

          uniqueLibsNeeded.add(sourcePath);

          classpath += sourcePath + COLON;
        }
      }

      classpath +=
        getResource(ANDROID_RUNTIME);

      System.out.println("Libraries Classpath = " + classpath);

      String yailRuntime = getResource(YAIL_RUNTIME);
      List<String> kawaCommandArgs = Lists.newArrayList();
      int mx = childProcessRamMb - 200;
      Collections.addAll(kawaCommandArgs,
          System.getProperty("java.home") + "/bin/java",
          "-Dfile.encoding=UTF-8",
          "-mx" + mx + "M",
          "-cp", classpath,
          "kawa.repl",
          "-f", yailRuntime,
          "-d", classesDir.getAbsolutePath(),
          // FIRST Tech Challenge: Use the same package as FtcRobotController-release.apk.
          "-P", (hasFtcRobotController ? FtcConstants.PACKAGE : Signatures.getPackageName(project.getMainClass())) + ".",
          "-C");
      // TODO(lizlooney) - we are currently using (and have always used) absolute paths for the
      // source file names. The resulting .class files contain references to the source file names,
      // including the name of the tmp directory that contains them. We may be able to avoid that
      // by using source file names that are relative to the project root and using the project
      // root as the working directory for the Kawa compiler process.
      kawaCommandArgs.addAll(sourceFileNames);
      kawaCommandArgs.add(yailRuntime);
      String[] kawaCommandLine = kawaCommandArgs.toArray(new String[kawaCommandArgs.size()]);

      long start = System.currentTimeMillis();
      // Capture Kawa compiler stderr. The ODE server parses out the warnings and errors and adds
      // them to the protocol buffer for logging purposes. (See
      // buildserver/ProjectBuilder.processCompilerOutout.
      ByteArrayOutputStream kawaOutputStream = new ByteArrayOutputStream();
      boolean kawaSuccess;
      synchronized (SYNC_KAWA_OR_DX) {
        kawaSuccess = Execution.execute(null, kawaCommandLine,
            System.out, new PrintStream(kawaOutputStream));
      }
      if (!kawaSuccess) {
        LOG.log(Level.SEVERE, "Kawa compile has failed.");
      }
      String kawaOutput = kawaOutputStream.toString();
      out.print(kawaOutput);
      String kawaCompileTimeMessage = "Kawa compile time: " +
          ((System.currentTimeMillis() - start) / 1000.0) + " seconds";
      out.println(kawaCompileTimeMessage);
      LOG.info(kawaCompileTimeMessage);

      // Check that all of the class files were created.
      // If they weren't, return with an error.
      for (String classFileName : classFileNames) {
        File classFile = new File(classFileName);
        if (!classFile.exists()) {
          LOG.log(Level.INFO, "Can't find class file: " + classFileName);
          String screenName = classFileName.substring(classFileName.lastIndexOf('/') + 1,
              classFileName.lastIndexOf('.'));
          userErrors.print(String.format(COMPILATION_ERROR, screenName));
          return false;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Compile"));
      return false;
    }

    return true;
  }

  private boolean runJarSigner(String apkAbsolutePath, String keystoreAbsolutePath) {
    // TODO(user): maybe make a command line flag for the jarsigner location
    String javaHome = System.getProperty("java.home");
    // This works on Mac OS X.
    File jarsignerFile = new File(javaHome + SLASH + "bin" +
        SLASH + "jarsigner");
    if (!jarsignerFile.exists()) {
      // This works when a JDK is installed with the JRE.
      jarsignerFile = new File(javaHome + SLASH + ".." + SLASH + "bin" +
          SLASH + "jarsigner");
      if (System.getProperty("os.name").startsWith("Windows")) {
        jarsignerFile = new File(javaHome + SLASH + ".." + SLASH + "bin" +
            SLASH + "jarsigner.exe");
      }
      if (!jarsignerFile.exists()) {
        LOG.warning("YAIL compiler - could not find jarsigner.");
        err.println("YAIL compiler - could not find jarsigner.");
        userErrors.print(String.format(ERROR_IN_STAGE, "JarSigner"));
        return false;
      }
    }

    // FIRST Tech Challenge: Use FTC keystore.
    if (hasFtcRobotController) {
      keystoreAbsolutePath = getResource(RUNTIME_FILES_DIR + "ftc.debug.keystore");
    }

    String[] jarsignerCommandLine = {
        jarsignerFile.getAbsolutePath(),
        "-digestalg", "SHA1",
        "-sigalg", "MD5withRSA",
        "-keystore", keystoreAbsolutePath,
        "-storepass", "android",
        apkAbsolutePath,
        // FIRST Tech Challenge: Use keyAlias from ftc_sdk/app/ftc_app/build.common.gradle.
        hasFtcRobotController ? "androiddebugkey" : "AndroidKey"
    };
    if (!Execution.execute(null, jarsignerCommandLine, System.out, System.err)) {
      LOG.warning("YAIL compiler - jarsigner execution failed.");
      err.println("YAIL compiler - jarsigner execution failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "JarSigner"));
      return false;
    }

    return true;
  }

  private boolean runZipAlign(String apkAbsolutePath, File tmpDir) {
    // TODO(user): add zipalign tool appinventor->lib->android->tools->linux and windows
    // Need to make sure assets directory exists otherwise zipalign will fail.
    createDir(project.getAssetsDirectory());
    String zipAlignTool;
    String osName = System.getProperty("os.name");
    if (osName.equals("Mac OS X")) {
      zipAlignTool = MAC_ZIPALIGN_TOOL;
    } else if (osName.equals("Linux")) {
      zipAlignTool = LINUX_ZIPALIGN_TOOL;
    } else if (osName.startsWith("Windows")) {
      zipAlignTool = WINDOWS_ZIPALIGN_TOOL;
    } else {
      LOG.warning("YAIL compiler - cannot run ZIPALIGN on OS " + osName);
      err.println("YAIL compiler - cannot run ZIPALIGN on OS " + osName);
      userErrors.print(String.format(ERROR_IN_STAGE, "ZIPALIGN"));
      return false;
    }
    // TODO: create tmp file for zipaling result
    String zipAlignedPath = tmpDir.getAbsolutePath() + SLASH + "zipaligned.apk";
    // zipalign -f -v 4 infile.zip outfile.zip
    String[] zipAlignCommandLine = {
        getResource(zipAlignTool),
        "-f",
        "4",
        apkAbsolutePath,
        zipAlignedPath
    };
    long startZipAlign = System.currentTimeMillis();
    // Using System.err and System.out on purpose. Don't want to pollute build messages with
    // tools output
    if (!Execution.execute(null, zipAlignCommandLine, System.out, System.err)) {
      LOG.warning("YAIL compiler - ZIPALIGN execution failed.");
      err.println("YAIL compiler - ZIPALIGN execution failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "ZIPALIGN"));
      return false;
    }
    if (!copyFile(zipAlignedPath, apkAbsolutePath)) {
      LOG.warning("YAIL compiler - ZIPALIGN file copy failed.");
      err.println("YAIL compiler - ZIPALIGN file copy failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "ZIPALIGN"));
      return false;
    }
    String zipALignTimeMessage = "ZIPALIGN time: " +
        ((System.currentTimeMillis() - startZipAlign) / 1000.0) + " seconds";
    out.println(zipALignTimeMessage);
    LOG.info(zipALignTimeMessage);
    return true;
  }

  /*
   * Loads the icon for the application, either a user provided one or the default one.
   */
  private boolean prepareApplicationIcon(File outputPngFile) {
    String userSpecifiedIcon = Strings.nullToEmpty(project.getIcon());
    try {
      BufferedImage icon;
      if (!userSpecifiedIcon.isEmpty()) {
        File iconFile = new File(project.getAssetsDirectory(), userSpecifiedIcon);
        icon = ImageIO.read(iconFile);
        if (icon == null) {
          // This can happen if the iconFile isn't an image file.
          // For example, icon is null if the file is a .wav file.
          // TODO(lizlooney) - This happens if the user specifies a .ico file. We should fix that.
          userErrors.print(String.format(ICON_ERROR, userSpecifiedIcon));
          return false;
        }
      } else {
        // Load the default image.
        icon = ImageIO.read(Compiler.class.getResource(DEFAULT_ICON));
      }
      ImageIO.write(icon, "png", outputPngFile);
    } catch (Exception e) {
      e.printStackTrace();
      // If the user specified the icon, this is fatal.
      if (!userSpecifiedIcon.isEmpty()) {
        userErrors.print(String.format(ICON_ERROR, userSpecifiedIcon));
        return false;
      }
    }

    return true;
  }

  private boolean runDx(File classesDir, String dexedClassesDir, boolean secondTry) {
    List<File> libList = new ArrayList<File>();
    List<File> inputList = new ArrayList<File>();
    List<File> class2List = new ArrayList<File>();
    inputList.add(classesDir); //this is a directory, and won't be cached into the dex cache
    inputList.add(new File(getResource(SIMPLE_ANDROID_RUNTIME_JAR)));
    inputList.add(new File(getResource(KAWA_RUNTIME)));
    inputList.add(new File(getResource(ACRA_RUNTIME)));

    for (String lib : uniqueLibsNeeded) {
      libList.add(new File(lib));
    }

    // BEGIN DEBUG -- XXX --
    // System.err.println("runDx -- libraries");
    // for (File aFile : inputList) {
    //   System.err.println(" inputList => " + aFile.getAbsolutePath());
    // }
    // for (File aFile : libList) {
    //   System.err.println(" libList => " + aFile.getAbsolutePath());
    // }
    // END DEBUG -- XXX --

    // attach the jars of external comps to the libraries list
    for (String type : extCompTypes) {
      String sourcePath = getExtCompDirPath(type) + SIMPLE_ANDROID_RUNTIME_JAR;
      libList.add(new File(sourcePath));
    }

    int offset = libList.size();
    // Note: The choice of 12 libraries is arbitrary. We note that things
    // worked to put all libraries into the first classes.dex file when we
    // had 16 libraries and broke at 17. So this is a conservative number
    // to try.
    if (!secondTry) {           // First time through, try base + 12 libraries
      if (offset > 12)
        offset = 12;
    } else {
      offset = 0;               // Add NO libraries the second time through!
    }
    for (int i = 0; i < offset; i++) {
      inputList.add(libList.get(i));
    }

    if (libList.size() - offset > 0) { // Any left over for classes2?
      for (int i = offset; i < libList.size(); i++) {
        class2List.add(libList.get(i));
      }
    }

    DexExecTask dexTask = new DexExecTask();
    dexTask.setExecutable(getResource(DX_JAR));
    dexTask.setOutput(dexedClassesDir + File.separator + "classes.dex");
    dexTask.setChildProcessRamMb(childProcessRamMb);
    if (dexCacheDir == null) {
      dexTask.setDisableDexMerger(true);
    } else {
      createDir(new File(dexCacheDir));
      dexTask.setDexedLibs(dexCacheDir);
    }

    long startDx = System.currentTimeMillis();
    // Using System.err and System.out on purpose. Don't want to pollute build messages with
    // tools output
    boolean dxSuccess;
    synchronized (SYNC_KAWA_OR_DX) {
      setProgress(50);
      dxSuccess = dexTask.execute(inputList);
      if (dxSuccess && (class2List.size() > 0)) {
        setProgress(60);
        dexTask.setOutput(dexedClassesDir + File.separator + "classes2.dex");
        inputList = new ArrayList<File>();
        dxSuccess = dexTask.execute(class2List);
        setProgress(75);
        hasSecondDex = true;
      } else if (!dxSuccess) {  // The initial dx blew out, try more conservative
        LOG.info("DX execution failed, trying with fewer libraries.");
        if (secondTry) {        // Already tried the more conservative approach!
          LOG.warning("YAIL compiler - DX execution failed (secondTry!).");
          err.println("YAIL compiler - DX execution failed.");
          userErrors.print(String.format(ERROR_IN_STAGE, "DX"));
          return false;
        } else {
          return runDx(classesDir, dexedClassesDir, true);
        }
      }
    }
    if (!dxSuccess) {
      LOG.warning("YAIL compiler - DX execution failed.");
      err.println("YAIL compiler - DX execution failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "DX"));
      return false;
    }
    String dxTimeMessage = "DX time: " +
        ((System.currentTimeMillis() - startDx) / 1000.0) + " seconds";
    out.println(dxTimeMessage);
    LOG.info(dxTimeMessage);

    return true;
  }

  private boolean runAaptPackage(File manifestFile, File resDir, String tmpPackageName) {
    // Need to make sure assets directory exists otherwise aapt will fail.
    createDir(project.getAssetsDirectory());
    String aaptTool;
    String osName = System.getProperty("os.name");
    if (osName.equals("Mac OS X")) {
      aaptTool = MAC_AAPT_TOOL;
    } else if (osName.equals("Linux")) {
      aaptTool = LINUX_AAPT_TOOL;
    } else if (osName.startsWith("Windows")) {
      aaptTool = WINDOWS_AAPT_TOOL;
    } else {
      LOG.warning("YAIL compiler - cannot run AAPT on OS " + osName);
      err.println("YAIL compiler - cannot run AAPT on OS " + osName);
      userErrors.print(String.format(ERROR_IN_STAGE, "AAPT"));
      return false;
    }
    String[] aaptPackageCommandLine = {
        getResource(aaptTool),
        "package",
        "-v",
        "-f",
        "-M", manifestFile.getAbsolutePath(),
        "-S", resDir.getAbsolutePath(),
        "-A", project.getAssetsDirectory().getAbsolutePath(),
        "-I", getResource(ANDROID_RUNTIME),
        "-F", tmpPackageName,
        libsDir.getAbsolutePath()
    };
    long startAapt = System.currentTimeMillis();
    // Using System.err and System.out on purpose. Don't want to pollute build messages with
    // tools output
    if (!Execution.execute(null, aaptPackageCommandLine, System.out, System.err)) {
      LOG.warning("YAIL compiler - AAPT execution failed.");
      err.println("YAIL compiler - AAPT execution failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "AAPT"));
      return false;
    }
    String aaptTimeMessage = "AAPT time: " +
        ((System.currentTimeMillis() - startAapt) / 1000.0) + " seconds";
    out.println(aaptTimeMessage);
    LOG.info(aaptTimeMessage);

    return true;
  }

  private boolean insertNativeLibs(File buildDir){
    /**
     * Native libraries are targeted for particular processor architectures.
     * Here, non-default architectures (ARMv5TE is default) are identified with suffixes
     * before being placed in the appropriate directory with their suffix removed.
     */
    libsDir = createDir(buildDir, LIBS_DIR_NAME);
    File armeabiDir = createDir(libsDir, ARMEABI_DIR_NAME);
    File armeabiV7aDir = createDir(libsDir, ARMEABI_V7A_DIR_NAME);

    try {
      for (String type : nativeLibsNeeded.keySet()) {
        for (String lib : nativeLibsNeeded.get(type)) {
          boolean isV7a = lib.endsWith(ARMEABI_V7A_SUFFIX);

          String sourceDirName = isV7a ? ARMEABI_V7A_DIR_NAME : ARMEABI_DIR_NAME;
          File targetDir = isV7a ? armeabiV7aDir : armeabiDir;
          lib = isV7a ? lib.substring(0, lib.length() - ARMEABI_V7A_SUFFIX.length()) : lib;

          String sourcePath = "";
          String pathSuffix = RUNTIME_FILES_DIR + sourceDirName + SLASH + lib;

          if (simpleCompTypes.contains(type)) {
            sourcePath = getResource(pathSuffix);
          } else if (extCompTypes.contains(type)) {
            sourcePath = getExtCompDirPath(type) + pathSuffix;
            targetDir = createDir(targetDir, EXT_COMPS_DIR_NAME);
            targetDir = createDir(targetDir, type);
          } else {
            userErrors.print(String.format(ERROR_IN_STAGE, "Native Code"));
            return false;
          }

          Files.copy(new File(sourcePath), new File(targetDir, lib));
        }
      }
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Native Code"));
      return false;
    }
  }

  private boolean attachCompAssets() {
    createDir(project.getAssetsDirectory()); // Needed to insert resources.
    try {
      // Gather non-library assets to be added to apk's Asset directory.
      // The assets directory have been created before this.
      File compAssetDir = createDir(project.getAssetsDirectory(),
          ASSET_DIRECTORY);

      for (String type : assetsNeeded.keySet()) {
        for (String assetName : assetsNeeded.get(type)) {
          File targetDir = compAssetDir;
          String sourcePath = "";
          String pathSuffix = RUNTIME_FILES_DIR + assetName;

          if (simpleCompTypes.contains(type)) {
            sourcePath = getResource(pathSuffix);
          } else if (extCompTypes.contains(type)) {
            sourcePath = getExtCompDirPath(type) + pathSuffix;
            targetDir = createDir(targetDir, EXT_COMPS_DIR_NAME);
            targetDir = createDir(targetDir, type);
          } else {
            userErrors.print(String.format(ERROR_IN_STAGE, "Assets"));
            return false;
          }

          Files.copy(new File(sourcePath), new File(targetDir, assetName));
        }
      }
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Assets"));
      return false;
    }
  }

  /**
   * Writes out the given resource as a temp file and returns the absolute path.
   * Caches the location of the files, so we can reuse them.
   *
   * @param resourcePath the name of the resource
   */
  static synchronized String getResource(String resourcePath) {
    try {
      File file = resources.get(resourcePath);
      if (file == null) {
        String basename = PathUtil.basename(resourcePath);
        String prefix;
        String suffix;
        int lastDot = basename.lastIndexOf(".");
        if (lastDot != -1) {
          prefix = basename.substring(0, lastDot);
          suffix = basename.substring(lastDot);
        } else {
          prefix = basename;
          suffix = "";
        }
        while (prefix.length() < 3) {
          prefix = prefix + "_";
        }
        file = File.createTempFile(prefix, suffix);
        file.setExecutable(true);
        file.deleteOnExit();
        file.getParentFile().mkdirs();
        Files.copy(Resources.newInputStreamSupplier(Compiler.class.getResource(resourcePath)),
            file);
        resources.put(resourcePath, file);
      }
      return file.getAbsolutePath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /*
   *  Loads permissions and information on component libraries and assets.
   */
  private void loadJsonInfo(ConcurrentMap<String, Set<String>> infoMap, String targetInfo)
      throws IOException, JSONException {
    synchronized (infoMap) {
      if (!infoMap.isEmpty()) {
        return;
      }

      JSONArray buildInfo = new JSONArray(
          "[" + simpleCompsBuildInfo.join(",") + "," +
          extCompsBuildInfo.join(",") + "]");

      for (int i = 0; i < buildInfo.length(); ++i) {
        JSONObject compJson = buildInfo.getJSONObject(i);
        JSONArray infoArray = null;
        String type = compJson.getString("type");
        try {
          infoArray = compJson.getJSONArray(targetInfo);
        } catch (JSONException e) {
          // Older compiled extensions will not have a broadcastReceiver
          // defined. Rather then require them all to be recompiled, we
          // treat the missing attribute as empty.
          if (e.getMessage().contains("broadcastReceiver")) {
            LOG.log(Level.INFO, "Component \"" + type + "\" does not have a broadcast receiver.");
            continue;
          } else {
            throw e;
          }
        }

        if (!simpleCompTypes.contains(type) && !extCompTypes.contains(type)) {
          continue;
        }

        Set<String> infoSet = Sets.newHashSet();
        for (int j = 0; j < infoArray.length(); ++j) {
          String info = infoArray.getString(j);
          infoSet.add(info);
        }

        if (!infoSet.isEmpty()) {
          infoMap.put(type, infoSet);
        }
      }
    }
  }

  /**
   * Copy one file to another. If destination file does not exist, it is created.
   *
   * @param srcPath absolute path to source file
   * @param dstPath absolute path to destination file
   * @return  {@code true} if the copy succeeds, {@code false} otherwise
   */
  private static Boolean copyFile(String srcPath, String dstPath) {
    try {
      FileInputStream in = new FileInputStream(srcPath);
      FileOutputStream out = new FileOutputStream(dstPath);
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      in.close();
      out.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /**
   * Creates a new directory (if it doesn't exist already).
   *
   * @param dir  new directory
   * @return  new directory
   */
  private static File createDir(File dir) {
    if (!dir.exists()) {
      dir.mkdir();
    }
    return dir;
  }

  /**
   * Creates a new directory (if it doesn't exist already).
   *
   * @param parentDir  parent directory of new directory
   * @param name  name of new directory
   * @return  new directory
   */
  private static File createDir(File parentDir, String name) {
    File dir = new File(parentDir, name);
    if (!dir.exists()) {
      dir.mkdir();
    }
    return dir;
  }

  /**
   * Creates a new directory (if it doesn't exist already).
   *
   * @param parentDirectory  parent directory of new directory
   * @param name  name of new directory
   * @return  new directory
   */
  private static File createDirectory(File parentDirectory, String name) {
    File dir = new File(parentDirectory, name);
    if (!dir.exists()) {
      dir.mkdir();
    }
    return dir;
  }

  private static int setProgress(int increments) {
    Compiler.currentProgress = increments;
    LOG.info("The current progress is "
              + Compiler.currentProgress + "%");
    return Compiler.currentProgress;
  }

  public static int getProgress() {
    if (Compiler.currentProgress==100) {
      Compiler.currentProgress = 10;
      return 100;
    } else {
      return Compiler.currentProgress;
    }
  }

  private void readBuildInfo() {
    try {
      simpleCompsBuildInfo = new JSONArray(Resources.toString(
          Compiler.class.getResource(COMP_BUILD_INFO), Charsets.UTF_8));

      extCompsBuildInfo = new JSONArray();
      for (String type : extCompTypes) {
        // .../assets/external_comps/com.package.MyExtComp/files/component_build_info.json
        File extCompRuntimeFileDir = new File(getExtCompDirPath(type) + RUNTIME_FILES_DIR);
        if (!extCompRuntimeFileDir.exists()) {
          // try extension package name for multi-extension files
          String path = getExtCompDirPath(type);
          path = path.substring(0, path.lastIndexOf('.'));
          extCompRuntimeFileDir = new File(path + RUNTIME_FILES_DIR);
        }
        String jsonFileName = "component_build_info.json";
        File jsonFile = new File(extCompRuntimeFileDir, jsonFileName);

        String buildInfo = Resources.toString(jsonFile.toURI().toURL(), Charsets.UTF_8);
        JSONTokener tokener = new JSONTokener(buildInfo);
        Object value = tokener.nextValue();
        if (value instanceof JSONObject) {
          extCompsBuildInfo.put((JSONObject) value);
        } else if (value instanceof JSONArray) {
          JSONArray infos = (JSONArray) value;
          for (int i = 0; i < infos.length(); i++) {
            extCompsBuildInfo.put(infos.getJSONObject(i));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void prepareCompTypes(Set<String> neededTypes) {
    try {
      JSONArray buildInfo = new JSONArray(Resources.toString(
          Compiler.class.getResource(COMP_BUILD_INFO), Charsets.UTF_8));

      Set<String> allSimpleTypes = Sets.newHashSet();
      for (int i = 0; i < buildInfo.length(); ++i) {
        JSONObject comp = buildInfo.getJSONObject(i);
        allSimpleTypes.add(comp.getString("type"));
      }

      simpleCompTypes = Sets.newHashSet(neededTypes);
      simpleCompTypes.retainAll(allSimpleTypes);

      extCompTypes = Sets.newHashSet(neededTypes);
      extCompTypes.removeAll(allSimpleTypes);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String getExtCompDirPath(String type) {
    createDir(project.getAssetsDirectory());
    return project.getAssetsDirectory().getAbsolutePath() + SLASH +
        EXT_COMPS_DIR_NAME + SLASH + type.substring(0, type.lastIndexOf('.'));
  }

  // FIRST Tech Challenge

  private void ftcAddApplicationToManifest(BufferedWriter out) throws IOException {
    out.write("  <application \n");
    out.write("    android:allowBackup=\"true\"\n");
    out.write("    android:largeHeap=\"true\"\n");
    out.write("    android:icon=\"@drawable/ic_launcher\"\n");
    out.write("    android:label=\"@string/AI_app_name\"\n");
    out.write("    android:theme=\"@style/AI_AppTheme\"\n");
    out.write("    android:name=\"org.firstinspires.ftc.robotcore.internal.RobotApplication\" >\n");

    // The main activity for an FTC Robot Controller app. This is from ftc_sdk/app/ftc_app/FtcRobotController/src/main/AndroidManifest.xml.
    out.write("    <activity\n");
    out.write("      android:name=\"org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/AI_app_name\"\n");
    out.write("      android:launchMode=\"singleTask\" >\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"android.intent.action.MAIN\" />\n");
    out.write("        <category android:name=\"android.intent.category.LAUNCHER\" />\n");
    out.write("      </intent-filter>\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"android.hardware.usb.action.USB_DEVICE_ATTACHED\" />\n");
    out.write("      </intent-filter>\n");
    out.write("      <meta-data\n");
    out.write("        android:name=\"android.hardware.usb.action.USB_DEVICE_ATTACHED\"\n");
    out.write("        android:resource=\"@xml/device_filter\" />\n");
    out.write("    </activity>\n");
    // TODO(lizlooney): Add receiver tag for RunOnStartup.
    out.write("    <service\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.FtcRobotControllerService\"\n");
    out.write("      android:enabled=\"true\" />\n");
    // TODO(lizlooney): Add service tag for FtcRobotControllerWatchdogService.
    // Add other FTC related activities and service.
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.FtcRobotControllerSettingsActivity\"\n");
    out.write("      android:label=\"@string/settings_activity\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.qualcomm.ftccommon.FtcRobotControllerSettingsActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.FtcLoadFileActivity\"\n");
    out.write("      android:label=\"@string/configure_activity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.qualcomm.ftccommon.configuration.FtcLoadFileActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.ConfigureFromTemplateActivity\"\n");
    out.write("      android:label=\"@string/title_activity_configfromtemplate\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.qualcomm.ftccommon.configuration.ConfigureFromTemplateActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.ViewLogsActivity\"\n");
    out.write("      android:label=\"@string/view_logs_activity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.qualcomm.ftccommon.ViewLogsActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.FtcConfigurationActivity\"\n");
    out.write("      android:label=\"@string/AI_title_ftc_configuration_activity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.FtcNewFileActivity\"\n");
    out.write("      android:label=\"@string/AI_title_ftc_new_file_activity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.ConfigWifiDirectActivity\"\n");
    out.write("      android:label=\"@string/title_activity_config_wifi_direct\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.FtcAdvancedRCSettingsActivity\"\n");
    out.write("      android:label=\"@string/titleAdvancedRCSettings\"\n");
    out.write("      android:exported=\"true\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.qualcomm.ftccommon.FtcAdvancedRCSettingsActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.FtcLynxFirmwareUpdateActivity\"\n");
    out.write("      android:exported=\"true\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.qualcomm.ftccommon.FtcLynxFirmwareUpdateModeActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.FtcLynxModuleAddressUpdateActivity\"\n");
    out.write("      android:exported=\"true\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.qualcomm.ftccommon.FtcLynxModuleAddressUpdateActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.FtcWifiDirectChannelSelectorActivity\"\n");
    out.write("      android:label=\"@string/title_activity_wifi_channel_selector\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.qualcomm.ftccommon.FtcWifiDirectChannelSelectorActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.FtcWifiDirectRememberedGroupsActivity\"\n");
    out.write("      android:label=\"@string/title_activity_wifi_remembered_groups_editor\"\n");
    out.write("      android:exported=\"true\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.qualcomm.ftccommon.FtcWifiDirectRememberedGroupsActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.AboutActivity\"\n");
    out.write("      android:label=\"@string/about_activity\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.qualcomm.ftccommon.configuration.AboutActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditSwapUsbDevices\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_swap_devices_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditMotorControllerActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_motor_controller_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditLegacyMotorControllerActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_motor_controller_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden\" >\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditMotorListActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_motor_controller_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditServoControllerActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_servo_controller_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditLegacyServoControllerActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_servo_controller_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\" >\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditServoListActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_servo_controller_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditLegacyModuleControllerActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_legacy_module_controller_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditMatrixControllerActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_matrix_controller_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditDeviceInterfaceModuleActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_core_device_interface_module_controller_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditLynxModuleActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_lynx_module_controller_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\" >\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditLynxUsbDeviceActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_lynx_usb_device_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\" >\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditPWMDevicesActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_pwm_devices_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditAnalogInputDevicesActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_analog_input_devices_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditDigitalDevicesActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_digital_devices_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditDigitalDevicesActivityLynx\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_digital_devices_activity_lynx\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\" >\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditI2cDevicesActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_i2c_devices_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditI2cDevicesActivityLynx\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_i2c_devices_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\" >\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.qualcomm.ftccommon.configuration.EditAnalogOutputDevicesActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/edit_analog_output_devices_activity\"\n");
    out.write("      android:windowSoftInputMode=\"stateHidden|adjustResize\">\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.google.blocks.ftcrobotcontroller.ProgrammingModeActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/programming_mode_activity\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.google.blocks.ftcrobotcontroller.ProgrammingModeActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.google.blocks.ftcdriverstation.RemoteProgrammingModeActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/programming_mode_activity\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.google.blocks.ftcdriverstation.RemoteProgrammingModeActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"com.google.blocks.ftcrobotcontroller.BlocksActivity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\"\n");
    out.write("      android:label=\"@string/blocks_activity\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"com.google.blocks.ftcrobotcontroller.BlocksActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <activity\n");
    out.write("      android:name=\"org.firstinspires.inspection.RcInspectionActivity\"\n");
    out.write("      android:label=\"@string/inspection_activity\"\n");
    out.write("      android:configChanges=\"orientation|screenSize\">\n");
    out.write("      <intent-filter>\n");
    out.write("        <action android:name=\"org.firstinspires.inspection.RcInspectionActivity.intent.action.Launch\" />\n");
    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
    out.write("      </intent-filter>\n");
    out.write("    </activity>\n");
    out.write("    <receiver\n");
    out.write("      android:name=\"org.firstinspires.inspection.DeviceNameReceiver\"\n");
    out.write("      android:enabled=\"true\"\n");
    out.write("      android:exported=\"true\">\n");
    out.write("    </receiver>\n");
  }

  /*
   * Creates the resources used by FTC.
   */
  private boolean ftcCreateResources(File resDir) throws IOException {
    String csv = Resources.toString(
        Compiler.class.getResource(RUNTIME_FILES_DIR + "ftc/res.list"), Charsets.UTF_8);
    String[] ftcFiles = csv.split(",");
    for (String ftcFile : ftcFiles) {
      //out.println("________Copying " + ftcFile);
      String source = getResource(RUNTIME_FILES_DIR + "ftc/res/" + ftcFile);
      File destFile = new File(resDir, ftcFile.replace('/', File.separatorChar));
      destFile.getParentFile().mkdirs();
      if (!copyFile(source, destFile.getAbsolutePath())) {
        return false;
      }
    }
    return true;
  }

  /*
   * Creates the assets used by FTC.
   */
  private boolean ftcCreateAssets(File assetsDir) throws IOException {
    createDir(assetsDir);
    String csv = Resources.toString(
        Compiler.class.getResource(RUNTIME_FILES_DIR + "ftc/assets.list"), Charsets.UTF_8);
    String[] ftcFiles = csv.split(",");
    for (String ftcFile : ftcFiles) {
      //out.println("________Copying " + ftcFile);
      String source = getResource(RUNTIME_FILES_DIR + "ftc/assets/" + ftcFile);
      File destFile = new File(assetsDir, ftcFile.replace('/', File.separatorChar));
      destFile.getParentFile().mkdirs();
      if (!copyFile(source, destFile.getAbsolutePath())) {
        return false;
      }
    }
    return true;
  }

  /*
   * Creates the native libraries used by FTC.
   */
  private boolean ftcCreateNativeLibs(File buildDir) throws IOException {
    // libsDir field hasn't been set yet.
    File libsDir = createDirectory(buildDir, LIBS_DIR_NAME);
    File apkLibDir = createDirectory(libsDir, "lib"); // This dir will be copied to apk.

    String csv = Resources.toString(
        Compiler.class.getResource(RUNTIME_FILES_DIR + "ftc/libs.list"), Charsets.UTF_8);
    String[] ftcFiles = csv.split(",");
    for (String ftcFile : ftcFiles) {
      if (!ftcFile.startsWith(ARMEABI_DIR_NAME + "/") &&
          !ftcFile.startsWith(ARMEABI_V7A_DIR_NAME + "/")) {
        throw new RuntimeException("FTC library " + ftcFile + " does not belong in " + ARMEABI_DIR_NAME + " or " + ARMEABI_V7A_DIR_NAME);
      }
      //out.println("________Copying " + ftcFile);
      String source = getResource(RUNTIME_FILES_DIR + "ftc/libs/" + ftcFile);
      File destFile = new File(apkLibDir, ftcFile.replace('/', File.separatorChar));
      destFile.getParentFile().mkdirs();
      if (!copyFile(source, destFile.getAbsolutePath())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Runs aapt package to generate R.java files.
   */
  private boolean ftcRunAaptPackage(File manifestFile, File resDir, File genDir, String customPackage) {
    String aaptTool;
    String osName = System.getProperty("os.name");
    if (osName.equals("Mac OS X")) {
      aaptTool = MAC_AAPT_TOOL;
    } else if (osName.equals("Linux")) {
      aaptTool = LINUX_AAPT_TOOL;
    } else if (osName.startsWith("Windows")) {
      aaptTool = WINDOWS_AAPT_TOOL;
    } else {
      LOG.warning("YAIL compiler - cannot run AAPT on OS " + osName);
      err.println("YAIL compiler - cannot run AAPT on OS " + osName);
      userErrors.print(String.format(ERROR_IN_STAGE, "AAPT"));
      return false;
    }
    String[] aaptPackageCommandLine = {
      getResource(aaptTool),
      "package",
      "-f",
      "-m",
      "-I", getResource(ANDROID_RUNTIME),
      "-S", resDir.getAbsolutePath(),
      "-M", manifestFile.getAbsolutePath(),
      "-J", genDir.getAbsolutePath(),
      "--custom-package", customPackage
    };
    long startAapt = System.currentTimeMillis();
    // Using System.err and System.out on purpose. Don't want to pollute build messages with
    // tools output
    if (!Execution.execute(null, aaptPackageCommandLine, System.out, System.err)) {
      LOG.warning("YAIL compiler - AAPT execution failed.");
      err.println("YAIL compiler - AAPT execution failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "AAPT"));
      return false;
    }
    String aaptTimeMessage = "AAPT time: " +
        ((System.currentTimeMillis() - startAapt) / 1000.0) + " seconds";
    out.println(aaptTimeMessage);
    LOG.info(aaptTimeMessage);

    return true;
  }

  /**
   * Runs javac to compiler generated .java files.
   */
  private boolean ftcRunJavac(File classesDir, List<String> genFileNames) {
    String javaHome = System.getProperty("java.home");
    // This works on Mac OS X.
    File javacFile = new File(javaHome + File.separator + "bin" +
        File.separator + "javac");
    if (!javacFile.exists()) {
      // This works when a JDK is installed with the JRE.
      javacFile = new File(javaHome + File.separator + ".." + File.separator + "bin" +
          File.separator + "javac");
      if (System.getProperty("os.name").startsWith("Windows")) {
        javacFile = new File(javaHome + File.separator + ".." + File.separator + "bin" +
            File.separator + "javac.exe");
      }
      if (!javacFile.exists()) {
        LOG.warning("YAIL compiler - could not find javac.");
        err.println("YAIL compiler - could not find javac.");
        userErrors.print(String.format(ERROR_IN_STAGE, "Javac"));
        return false;
      }
    }

    List<String> javacCommandArgs = Lists.newArrayList();
    Collections.addAll(javacCommandArgs,
        javacFile.getAbsolutePath(),
        "-d", classesDir.getAbsolutePath(),
        "-source", "5",
        "-target", "5");
    javacCommandArgs.addAll(genFileNames);
    String[] javacCommandLine = javacCommandArgs.toArray(new String[javacCommandArgs.size()]);
    if (!Execution.execute(null, javacCommandLine, System.out, System.err)) {
      LOG.warning("YAIL compiler - javac execution failed.");
      err.println("YAIL compiler - javac execution failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "Javac"));
      return false;
    }

    return true;
  }
}
