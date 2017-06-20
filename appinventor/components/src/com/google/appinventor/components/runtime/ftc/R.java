// Copyright 2011-2015 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime.ftc;

import android.content.Context;
import android.content.res.Resources;

/**
 * R provides dynamic values for R.<type>.<name> identifiers used in
 * FtcRobotControllerActivity.java and FtcRobotControllerSettingsActivity.java.
 */
public class R {
  public static class Ids {
    final int action_about;
    final int action_blocks;
    final int action_configure_robot;
    final int action_exit_app;
    final int action_inspection_mode;
    final int action_program_and_manage;
    final int action_programming_mode;
    final int action_restart_robot;
    final int action_settings;
    final int entire_screen;
    final int menu_buttons;
    final int textDeviceName;
    final int textErrorMessage;
    final int textGamepad1;
    final int textGamepad2;
    final int textOpMode;
    final int textRobotStatus;
    final int textNetworkConnectionStatus;
    final int textRemoteProgrammingMode;
    final int webViewBlocksRuntime;
    public final int cameraMonitorViewId;

    Ids(Resources resources, String packageName) {
      action_about = getIdentifier(resources, "action_about", "id", packageName);
      action_blocks = getIdentifier(resources, "action_blocks", "id", packageName);
      action_configure_robot = getIdentifier(resources, "action_configure_robot", "id", packageName);
      action_exit_app = getIdentifier(resources, "action_exit_app", "id", packageName);
      action_inspection_mode = getIdentifier(resources, "action_inspection_mode", "id", packageName);
      action_program_and_manage = getIdentifier(resources, "action_program_and_manage", "id", packageName);
      action_programming_mode = getIdentifier(resources, "action_programming_mode", "id", packageName);
      action_restart_robot = getIdentifier(resources, "action_restart_robot", "id", packageName);
      action_settings = getIdentifier(resources, "action_settings", "id", packageName);
      entire_screen = getIdentifier(resources, "entire_screen", "id", packageName);
      menu_buttons = getIdentifier(resources, "menu_buttons", "id", packageName);
      textDeviceName = getIdentifier(resources, "textDeviceName", "id", packageName);
      textErrorMessage = getIdentifier(resources, "textErrorMessage", "id", packageName);
      textGamepad1 = getIdentifier(resources, "textGamepad1", "id", packageName);
      textGamepad2 = getIdentifier(resources, "textGamepad2", "id", packageName);
      textOpMode = getIdentifier(resources, "textOpMode", "id", packageName);
      textRobotStatus = getIdentifier(resources, "textRobotStatus", "id", packageName);
      textNetworkConnectionStatus = getIdentifier(resources, "textNetworkConnectionStatus", "id", packageName);
      textRemoteProgrammingMode = getIdentifier(resources, "textRemoteProgrammingMode", "id", packageName);
      webViewBlocksRuntime = getIdentifier(resources, "webViewBlocksRuntime", "id", packageName);
      cameraMonitorViewId = getIdentifier(resources, "cameraMonitorViewId", "id", packageName);
    }
  }
  static class Layouts {
    final int activity_ftc_controller;

    Layouts(Resources resources, String packageName) {
      activity_ftc_controller = getIdentifier(resources, "activity_ftc_controller", "layout", packageName);
    }
  }
  static class Menus {
    final int ftc_robot_controller;

    Menus(Resources resources, String packageName) {
      ftc_robot_controller = getIdentifier(resources, "ftc_robot_controller", "menu", packageName);
    }
  }
  static class Strings {
    final int appThemeChangeRestartNotifyRC;
    final int pref_app_theme;
    final int pref_network_connection_type;
    final int pref_rc_connected;
    final int toastConfigureRobotBeforeProgrammingMode;
    final int toastRestartRobotComplete;
    final int toastRestartingRobot;
    final int toastWifiConfigurationComplete;

    Strings(Resources resources, String packageName) {
      appThemeChangeRestartNotifyRC = getIdentifier(resources, "appThemeChangeRestartNotifyRC", "string", packageName);
      pref_app_theme = getIdentifier(resources, "pref_app_theme", "string", packageName);
      pref_network_connection_type = getIdentifier(resources, "pref_network_connection_type", "string", packageName);
      pref_rc_connected = getIdentifier(resources, "pref_rc_connected", "string", packageName);
      toastConfigureRobotBeforeProgrammingMode =
          getIdentifier(resources, "toastConfigureRobotBeforeProgrammingMode", "string", packageName);
      toastRestartRobotComplete = getIdentifier(resources, "toastRestartRobotComplete", "string", packageName);
      toastRestartingRobot = getIdentifier(resources, "toastRestartingRobot", "string", packageName);
      toastWifiConfigurationComplete =
          getIdentifier(resources, "toastWifiConfigurationComplete", "string", packageName);
    }
  }
  static class Xmls {
    final int app_settings;

    Xmls(Resources resources, String packageName) {
      app_settings = getIdentifier(resources, "app_settings", "xml", packageName);
    }
  }

  /**
   * Prevent instantiation.
   */
  private R() {
  }

  public static Ids id;
  static Layouts layout;
  static Menus menu;
  static Strings string;
  static Xmls xml;

  static void init(Context context) {
    Resources resources = context.getResources();
    String packageName = context.getPackageName();
    id = new Ids(resources, packageName);
    layout = new Layouts(resources, packageName);
    menu = new Menus(resources, packageName);
    string = new Strings(resources, packageName);
    xml = new Xmls(resources, packageName);
  }

  private static int getIdentifier(Resources resources, String name, String defType, String defPackage) {
    int id = resources.getIdentifier(name, defType, defPackage);
    if (id == 0) {
      throw new IllegalStateException("Resource " + name + " not found");
    }
    return id;
  }
}
