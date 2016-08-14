// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2015 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.ftc.R;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.Parameters;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.Parameters.CameraMonitorFeedback;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import java.util.ArrayList;
import java.util.List;

/**
 * A component for interacting with subsystems that can help support localization through visual
 * means, for an FTC robot.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@DesignerComponent(version = YaVersion.FTC_VUFORIA_LOCALIZER_COMPONENT_VERSION,
    description = "A component for interacting with subsystems that can help support localization " +
        "through visual means, for an FTC robot.",
    category = ComponentCategory.FIRSTTECHCHALLENGE,
    nonVisible = true,
    iconName = "images/ftc.png")
@SimpleObject
@UsesLibraries(libraries = "FtcRobotCore.jar,FtcVuforia.jar")
public final class FtcVuforiaLocalizer extends AndroidNonvisibleComponent
    implements Component, OnDestroyListener, Deleteable {

  private volatile VuforiaLocalizer vuforiaLocalizer;
  private final List<VuforiaTrackables> trackablesList = new ArrayList<VuforiaTrackables>();
  private final List<VuforiaTrackable> trackableList = new ArrayList<VuforiaTrackable>();

  /**
   * Creates a new FtcVuforiaLocalizer component.
   */
  public FtcVuforiaLocalizer(ComponentContainer container) {
    super(container.$form());

    form.registerForOnDestroy(this);
  }

  /**
   * CameraDirection_FRONT property getter.
   */
  @SimpleProperty(description = "The constant for CameraDirection_FRONT.",
      category = PropertyCategory.BEHAVIOR)
  public String CameraDirection_FRONT() {
    return CameraDirection.FRONT.toString();
  }

  /**
   * CameraDirection_BACK property getter.
   */
  @SimpleProperty(description = "The constant for CameraDirection_BACK.",
      category = PropertyCategory.BEHAVIOR)
  public String CameraDirection_BACK() {
    return CameraDirection.BACK.toString();
  }

  /**
   * CameraMonitorFeedback_NONE property getter.
   */
  @SimpleProperty(description = "The constant for CameraMonitorFeedback_NONE.",
      category = PropertyCategory.BEHAVIOR)
  public String CameraMonitorFeedback_NONE() {
    return CameraMonitorFeedback.NONE.toString();
  }

  /**
   * CameraMonitorFeedback_AXES property getter.
   */
  @SimpleProperty(description = "The constant for CameraMonitorFeedback_AXES.",
      category = PropertyCategory.BEHAVIOR)
  public String CameraMonitorFeedback_AXES() {
    return CameraMonitorFeedback.AXES.toString();
  }

  /**
   * CameraMonitorFeedback_TEAPOT property getter.
   */
  @SimpleProperty(description = "The constant for CameraMonitorFeedback_TEAPOT.",
      category = PropertyCategory.BEHAVIOR)
  public String CameraMonitorFeedback_TEAPOT() {
    return CameraMonitorFeedback.TEAPOT.toString();
  }

  /**
   * CameraMonitorFeedback_BUILDINGS property getter.
   */
  @SimpleProperty(description = "The constant for CameraMonitorFeedback_BUILDINGS.",
      category = PropertyCategory.BEHAVIOR)
  public String CameraMonitorFeedback_BUILDINGS() {
    return CameraMonitorFeedback.BUILDINGS.toString();
  }

  @SimpleFunction(description = "Create the Vuforia localizer.")
  public void CreateVuforiaLocalizer(
      String cameraDirection, boolean useExtendedTracking, String cameraMonitorFeedback,
      boolean fillCameraMonitorViewParent) {
    try {
      VuforiaLocalizer.Parameters parameters =
          new VuforiaLocalizer.Parameters(R.id.cameraMonitorViewId);
      CameraDirection cameraDirectionValue = parseCameraDirection(cameraDirection);
      if (cameraDirectionValue != null) {
        parameters.cameraDirection = cameraDirectionValue;
      }
      parameters.useExtendedTracking = useExtendedTracking;
      CameraMonitorFeedback cameraMonitorFeedbackValue =
          parseCameraMonitorFeedback(cameraMonitorFeedback);
      if (cameraMonitorFeedbackValue != null) {
        parameters.cameraMonitorFeedback = cameraMonitorFeedbackValue;
      }
      parameters.fillCameraMonitorViewParent = fillCameraMonitorViewParent;
      vuforiaLocalizer = ClassFactory.createVuforiaLocalizer(parameters);
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "CreateVuforiaLocalizer",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
  }

  private CameraDirection parseCameraDirection(String cameraDirection) {
    for (CameraDirection cameraDirectionValue : CameraDirection.values()) {
      if (cameraDirectionValue.toString().equalsIgnoreCase(cameraDirection)) {
        return cameraDirectionValue;
      }
    }
    form.dispatchErrorOccurredEvent(this, "CreateVuforiaLocalizer",
        ErrorMessages.ERROR_FTC_INVALID_CAMERA_DIRECTION, cameraDirection);
    return null;
  }
  
  private CameraMonitorFeedback parseCameraMonitorFeedback(String cameraMonitorFeedback) {
    for (CameraMonitorFeedback cameraMonitorFeedbackValue : CameraMonitorFeedback.values()) {
      if (cameraMonitorFeedbackValue.toString().equalsIgnoreCase(cameraMonitorFeedback)) {
        return cameraMonitorFeedbackValue;
      }
    }
    form.dispatchErrorOccurredEvent(this, "CreateVuforiaLocalizer",
        ErrorMessages.ERROR_FTC_INVALID_CAMERA_MONITOR_FEEDBACK, cameraMonitorFeedback);
    return null;
  }
  
  @SimpleFunction(description = "Loads a Vuforia dataset from the indicated application asset, " +
      "which must be of type .XML. The corresponding .DAT asset must be a sibling. Note that " +
      "this operation can be extremely lengthy, possibly taking a few seconds to execute. " +
      "Loading datasets from an asset you stored in your application APK is the recommended " +
      "approach to packaging datasets so they always travel along with your code. " +
      "In App Inventor, assets are called media.")
  public void LoadTrackablesFromAsset(String assetName) {
    try {
      if (vuforiaLocalizer != null) {
        VuforiaTrackables vuforiaTrackables = vuforiaLocalizer.loadTrackablesFromAsset(assetName);
        trackablesList.add(vuforiaTrackables);
        trackableList.addAll(vuforiaTrackables);
      } else {
        form.dispatchErrorOccurredEvent(this, "LoadTrackablesFromAsset",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "LoadTrackablesFromAsset",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
  }

  @SimpleFunction(description = "Loads a Vuforia dataset from the indicated file, which must be " +
      "a .XML file and contain the full file path. The corresponding .DAT file must be a sibling " +
      "file in the same directory. Note that this operation can be extremely lengthy, possibly " +
      "taking a few seconds to execute. Loading datasets from an asset you stored in your " +
      "application APK is the recommended approach to packaging datasets so they always travel " +
      "along with your code.")
  public void LoadTrackablesFromFile(String absoluteFileName) {
    try {
      if (vuforiaLocalizer != null) {
        vuforiaLocalizer.loadTrackablesFromFile(absoluteFileName);
      } else {
        form.dispatchErrorOccurredEvent(this, "LoadTrackablesFromFile",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "LoadTrackablesFromFile",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
  }

  /**
   * TrackableCount property getter.
   */
  @SimpleProperty(description = "The number of trackables that were loaded with " +
      "LoadTrackablesFromAsset or LoadTrackablesFromFile.",
      category = PropertyCategory.BEHAVIOR)
  public int TrackableCount() {
    return trackableList.size();
  }

  @SimpleFunction(description = "Set the name of a trackable that was loaded with " +
      "LoadTrackablesFromAsset or LoadTrackablesFromFile.")
  public void SetTrackableName(int trackable, String name) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackable >= 0 && trackable < trackableList.size()) {
          trackableList.get(trackable).setName(name);
        } else {
          form.dispatchErrorOccurredEvent(this, "SetTrackableName",
              ErrorMessages.ERROR_FTC_INVALID_VUFORIA_TRACKABLE, trackable, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "SetTrackableName",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "SetTrackableName",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
  }

  @SimpleFunction(description = "Get the name of a trackable that was loaded with " +
      "LoadTrackablesFromAsset or LoadTrackablesFromFile.")
  public String GetTrackableName(int trackable) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackable >= 0 && trackable < trackableList.size()) {
          return trackableList.get(trackable).getName();
        } else {
          form.dispatchErrorOccurredEvent(this, "GetTrackableName",
              ErrorMessages.ERROR_FTC_INVALID_VUFORIA_TRACKABLE, trackable, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "GetTrackableName",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "GetTrackableName",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
    return "";
  }

  @SimpleFunction(description = "Activiate trackables that wer loaded with " +
      "LoadTrackablesFromAsset or LoadTrackablesFromFile.")
  public void ActivateTrackables() {
    try {
      if (vuforiaLocalizer != null) {
        for (VuforiaTrackables trackables : trackablesList) {
          trackables.activate();
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "ActivateTrackables",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "ActivateTrackables",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
  }

  // OnDestroyListener implementation

  @Override
  public void onDestroy() {
    vuforiaLocalizer = null;
    trackablesList.clear();
    trackableList.clear();
  }

  // Deleteable implementation

  @Override
  public void onDelete() {
    vuforiaLocalizer = null;
    trackablesList.clear();
    trackableList.clear();
  }
}
