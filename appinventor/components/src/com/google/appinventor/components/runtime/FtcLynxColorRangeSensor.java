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

import com.qualcomm.hardware.lynx.LynxI2cColorRangeSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * A component for a Lynx color range sensor of an FTC robot.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@DesignerComponent(version = YaVersion.FTC_LYNX_COLOR_RANGE_SENSOR_COMPONENT_VERSION,
    description = "A component for a Lynx color range sensor of an FTC robot.",
    category = ComponentCategory.FIRSTTECHCHALLENGE,
    nonVisible = true,
    iconName = "images/ftc.png")
@SimpleObject
@UsesLibraries(libraries = "FtcHardware.jar,FtcRobotCore.jar")
public final class FtcLynxColorRangeSensor extends FtcHardwareDevice {

  private volatile LynxI2cColorRangeSensor colorRangeSensor;

  /**
   * Creates a new FtcLynxColorRangeSensor component.
   */
  public FtcLynxColorRangeSensor(ComponentContainer container) {
    super(container.$form(), LynxI2cColorRangeSensor.class);
  }

  /**
   * Status property getter.
   */
  @SimpleProperty(description = "The status.",
      category = PropertyCategory.BEHAVIOR)
  public String Status() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        String status = colorRangeSensor.status();
        if (status != null) {
          return status;
        }
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Status",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return "";
  }

  // LynxI2cColorRangeSensor

//  /**
//   * MAX_NEW_I2C_ADDRESS property getter.
//   */
//  @SimpleProperty(description = "The constant for MAX_NEW_I2C_ADDRESS.",
//      category = PropertyCategory.BEHAVIOR)
//  public int MAX_NEW_I2C_ADDRESS() {
//    return ModernRoboticsUsbDeviceInterfaceModule.MAX_NEW_I2C_ADDRESS;
//  }
//
//  /**
//   * MIN_NEW_I2C_ADDRESS property getter.
//   */
//  @SimpleProperty(description = "The constant for MIN_NEW_I2C_ADDRESS.",
//      category = PropertyCategory.BEHAVIOR)
//  public int MIN_NEW_I2C_ADDRESS() {
//    return ModernRoboticsUsbDeviceInterfaceModule.MIN_NEW_I2C_ADDRESS;
//  }

  /**
   * Red property getter.
   */
  @SimpleProperty(description = "The red value detected by the sensor as an integer.",
      category = PropertyCategory.BEHAVIOR)
  public int Red() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        return colorRangeSensor.red();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Red",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * Green property getter.
   */
  @SimpleProperty(description = "The green value detected by the sensor as an integer.",
      category = PropertyCategory.BEHAVIOR)
  public int Green() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        return colorRangeSensor.green();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Green",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * Blue property getter.
   */
  @SimpleProperty(description = "The blue value detected by the sensor as an integer.",
      category = PropertyCategory.BEHAVIOR)
  public int Blue() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        return colorRangeSensor.blue();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Blue",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * Alpha property getter.
   */
  @SimpleProperty(description = "The amount of light detected by the sensor as an integer.",
      category = PropertyCategory.BEHAVIOR)
  public int Alpha() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        return colorRangeSensor.alpha();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Alpha",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * ARGB property getter.
   */
  @SimpleProperty(description = "The color detected by the sensor as " +
      "an integer ARGB (alpha, red, green, blue) color.",
      category = PropertyCategory.BEHAVIOR)
  public int ARGB() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        return colorRangeSensor.argb();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "ARGB",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * I2cAddress property setter.
   */
  @SimpleProperty
  public void I2cAddress(int newAddress) {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        colorRangeSensor.setI2cAddress(I2cAddr.create8bit(newAddress));
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "I2cAddress",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
  }

  /**
   * I2cAddress property getter.
   */
  @SimpleProperty(description = "The I2C address of the color range sensor.",
      category = PropertyCategory.BEHAVIOR)
  public int I2cAddress() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        return colorRangeSensor.getI2cAddress().get8Bit();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "I2cAddress",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * LightDetected property getter.
   */
  @SimpleProperty(description = "The light detected by the sensor, on a scale of 0 to 1.",
      category = PropertyCategory.BEHAVIOR)
  public double LightDetected() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        return colorRangeSensor.getLightDetected();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "LightDetected",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * RawLightDetected property getter.
   */
  @SimpleProperty(description = "A value proportional to the amount of light detected, in unspecified units.",
      category = PropertyCategory.BEHAVIOR)
  public double RawLightDetected() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        return colorRangeSensor.getRawLightDetected();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "RawLightDetected",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * RawLightDetectedMax property getter.
   */
  @SimpleProperty(description = "Returns the maximum value that can be returned for RawLightDetected.",
      category = PropertyCategory.BEHAVIOR)
  public double RawLightDetectedMax() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        return colorRangeSensor.getRawLightDetectedMax();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "RawLightDetectedMax",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * RawOptical property getter.
   */
  @SimpleProperty(description = "Returns the raw optical value.",
      category = PropertyCategory.BEHAVIOR)
  public double RawOptical() {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        return colorRangeSensor.rawOptical();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "RawOptical",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * DistanceUnit_METER property getter.
   */
  @SimpleProperty(description = "The constant for DistanceUnit_METER.",
      category = PropertyCategory.BEHAVIOR)
  public String DistanceUnit_METER() {
    return DistanceUnit.METER.toString();
  }

  /**
   * DistanceUnit_CM property getter.
   */
  @SimpleProperty(description = "The constant for DistanceUnit_CM.",
      category = PropertyCategory.BEHAVIOR)
  public String DistanceUnit_CM() {
    return DistanceUnit.CM.toString();
  }

  /**
   * DistanceUnit_MM property getter.
   */
  @SimpleProperty(description = "The constant for DistanceUnit_MM.",
      category = PropertyCategory.BEHAVIOR)
  public String DistanceUnit_MM() {
    return DistanceUnit.MM.toString();
  }

  /**
   * DistanceUnit_INCH property getter.
   */
  @SimpleProperty(description = "The constant for DistanceUnit_INCH.",
      category = PropertyCategory.BEHAVIOR)
  public String DistanceUnit_INCH() {
    return DistanceUnit.INCH.toString();
  }

  @SimpleFunction(description = "Returns the current distance in the indicated distance units.")
  public double GetDistance(String distanceUnit) {
    checkHardwareDevice();
    if (colorRangeSensor != null) {
      try {
        for (DistanceUnit distanceUnitValue : DistanceUnit.values()) {
          if (distanceUnitValue.toString().equalsIgnoreCase(distanceUnit)) {
            return colorRangeSensor.getDistance(distanceUnitValue);
          }
        }

        form.dispatchErrorOccurredEvent(this, "GetDistance",
            ErrorMessages.ERROR_FTC_INVALID_DISTANCE_UNIT, distanceUnit);
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "GetDistance",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  // FtcHardwareDevice implementation

  @Override
  protected Object initHardwareDeviceImpl() {
    colorRangeSensor = (LynxI2cColorRangeSensor) hardwareMap.get(deviceClass, getDeviceName());
    return colorRangeSensor;
  }

  @Override
  protected void clearHardwareDeviceImpl() {
    colorRangeSensor = null;
  }
}
