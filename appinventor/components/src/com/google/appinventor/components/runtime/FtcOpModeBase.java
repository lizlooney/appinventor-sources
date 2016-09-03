// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2015 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeMeta.Flavor;

/**
 * A base class for components for operation modes for an FTC robot.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@SimpleObject
public abstract class FtcOpModeBase extends AndroidNonvisibleComponent
    implements Component, OnDestroyListener, Deleteable, FtcRobotController.OpModeWrapper {

  private static final String DEFAULT_NAME = "Unnamed Op Mode";

  protected final OpMode opMode;

  private volatile String opModeName = DEFAULT_NAME;
  private volatile boolean autonomous;
  private volatile String group = "";

  protected FtcOpModeBase(ComponentContainer container) {
    super(container.$form());

    this.opMode = createOpMode();

    FtcRobotController.addOpMode(this);
    form.registerForOnDestroy(this);
  }

  protected abstract OpMode createOpMode();

  /**
   * OpModeName property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "The name of this Op Mode.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public String OpModeName() {
    return opModeName;
  }

  /**
   * OpModeName property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = DEFAULT_NAME)
  @SimpleProperty(userVisible = false)
  public void OpModeName(String opModeName) {
    this.opModeName = opModeName;
  }

  /**
   * Autonomous property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "Whether this op mode is autonomous.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public boolean Autonomous() {
    return autonomous;
  }

  /**
   * Autonomous property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty(userVisible = false)
  public void Autonomous(boolean autonomous) {
    this.autonomous = autonomous;
  }

  /**
   * Group property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "The group for this Op Mode.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public String Group() {
    return group;
  }

  /**
   * Group property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "")
  @SimpleProperty(userVisible = false)
  public void Group(String group) {
    this.group = (group == null) ? "" : group;
  }

  @SimpleFunction(description = "Get the number of seconds this op mode has been running.")
  public double GetRuntime() {
    return opMode.getRuntime();
  }

  @SimpleFunction(description = "Requests that this op mode be shut down as if the stop button " +
      "had been pressed on the driver station.")
  public void RequestOpModeStop() {
    opMode.requestOpModeStop();
  }

  // OnDestroyListener implementation

  @Override
  public void onDestroy() {
    FtcRobotController.removeOpMode(this);
  }

  // Deleteable implementation

  @Override
  public void onDelete() {
    FtcRobotController.removeOpMode(this);
  }

  // FtcRobotController.OpModeWrapper implementation

  @Override
  public String getOpModeName() {
    return opModeName;
  }

  @Override
  public Flavor getFlavor() {
    return autonomous ? Flavor.AUTONOMOUS : Flavor.TELEOP;
  }

  @Override
  public String getGroup() {
    return group;
  }

  @Override
  public OpMode getOpMode() {
    return opMode;
  }
}
