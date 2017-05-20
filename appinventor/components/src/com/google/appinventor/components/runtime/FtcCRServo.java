// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction;

/**
 * A component for a continuous rotation servo of an FTC robot.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@DesignerComponent(version = YaVersion.FTC_CR_SERVO_COMPONENT_VERSION,
    description = "A component for a continuous rotation servo of an FTC robot.",
    category = ComponentCategory.FIRSTTECHCHALLENGE,
    nonVisible = true,
    iconName = "images/ftc.png")
@SimpleObject
@UsesLibraries(libraries = "FtcRobotCore.jar")
public final class FtcCRServo extends FtcHardwareDevice {

  private volatile CRServo crservo;

  /**
   * Creates a new FtcCRServo component.
   */
  public FtcCRServo(ComponentContainer container) {
    super(container.$form(), CRServo.class);
  }

  protected CRServo getCRServo() {
    return crservo;
  }

  /**
   * Direction_FORWARD property getter.
   */
  @SimpleProperty(description = "The constant for Direction_FORWARD.",
      category = PropertyCategory.BEHAVIOR)
  public String Direction_FORWARD() {
    return Direction.FORWARD.toString();
  }

  /**
   * Direction_REVERSE property getter.
   */
  @SimpleProperty(description = "The constant for Direction_REVERSE.",
      category = PropertyCategory.BEHAVIOR)
  public String Direction_REVERSE() {
    return Direction.REVERSE.toString();
  }

  /**
   * Direction property setter.
   */
  @SimpleProperty
  public void Direction(String direction) {
    checkHardwareDevice();
    if (crservo != null) {
      try {
        try {
          int n = Integer.decode(direction);
          if (n == 1) {
            crservo.setDirection(Direction.FORWARD);
            return;
          }
          if (n == -1) {
            crservo.setDirection(Direction.REVERSE);
            return;
          }
        } catch (NumberFormatException e) {
          // Code below will try to interpret direction as a Direction enum string.
        }
        
        for (Direction directionValue : Direction.values()) {
          if (directionValue.toString().equalsIgnoreCase(direction)) {
            crservo.setDirection(directionValue);
            return;
          }
        }

        form.dispatchErrorOccurredEvent(this, "Direction",
            ErrorMessages.ERROR_FTC_INVALID_DIRECTION, direction);
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Direction",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
  }

  /**
   * Direction property getter.
   */
  @SimpleProperty(description = "Whether this servo should spin forward or reverse.\n" +
      "Valid values are Direction_FORWARD or Direction_REVERSE.",
      category = PropertyCategory.BEHAVIOR)
  public String Direction() {
    checkHardwareDevice();
    if (crservo != null) {
      try {
        Direction direction = crservo.getDirection();
        if (direction != null) {
          return direction.toString();
        }
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Direction",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return "";
  }

  /**
   * Power property setter.
   */
  @SimpleProperty
  public void Power(double power) {
    checkHardwareDevice();
    if (crservo != null) {
      try {
        crservo.setPower(power);
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Power",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
  }

  /**
   * Power property getter.
   */
  @SimpleProperty(description = "The current servo power, between -1 and 1.",
      category = PropertyCategory.BEHAVIOR)
  public double Power() {
    checkHardwareDevice();
    if (crservo != null) {
      try {
        return crservo.getPower();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Power",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0.0;
  }

  /**
   * PortNumber property getter.
   */
  @SimpleProperty(description = "The port number.",
      category = PropertyCategory.BEHAVIOR)
  public int PortNumber() {
    checkHardwareDevice();
    if (crservo != null) {
      try {
        return crservo.getPortNumber();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "PortNumber",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  // FtcHardwareDevice implementation

  @Override
  protected Object initHardwareDeviceImpl() {
    crservo = (CRServo) hardwareMap.get(deviceClass, getDeviceName());
    return crservo;
  }

  @Override
  protected void clearHardwareDeviceImpl() {
    crservo = null;
  }
}
