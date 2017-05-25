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

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsUsbDeviceInterfaceModule;
import com.qualcomm.robotcore.hardware.I2cAddr;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * A component for a Modern Robotics range sensor of an FTC robot.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@DesignerComponent(version = YaVersion.FTC_MR_RANGE_SENSOR_COMPONENT_VERSION,
    description = "A component for a Modern Robotics range sensor of an FTC robot.",
    category = ComponentCategory.FIRSTTECHCHALLENGE,
    nonVisible = true,
    iconName = "images/ftc.png")
@SimpleObject
@UsesLibraries(libraries = "FtcHardware.jar,FtcRobotCore.jar")
public final class FtcMrRangeSensor extends FtcHardwareDevice {

  private volatile ModernRoboticsI2cRangeSensor rangeSensor;

  /**
   * Creates a new FtcMrRangeSensor component.
   */
  public FtcMrRangeSensor(ComponentContainer container) {
    super(container.$form(), ModernRoboticsI2cRangeSensor.class);
  }

  /**
   * LightDetected property getter.
   */
  @SimpleProperty(description = "The light detected by the sensor, on a scale of 0 to 1.",
      category = PropertyCategory.BEHAVIOR)
  public double LightDetected() {
    checkHardwareDevice();
    if (rangeSensor != null) {
      try {
        return rangeSensor.getLightDetected();
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
    if (rangeSensor != null) {
      try {
        return rangeSensor.getRawLightDetected();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "RawLightDetected",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  @SimpleFunction(description = "Enable the LED light.")
  public void EnableLed(boolean enable) {
    checkHardwareDevice();
    if (rangeSensor != null) {
      try {
        rangeSensor.enableLed(enable);
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "EnableLed",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
  }

  /**
   * Status property getter.
   */
  @SimpleProperty(description = "The status.",
      category = PropertyCategory.BEHAVIOR)
  public String Status() {
    checkHardwareDevice();
    if (rangeSensor != null) {
      try {
        String status = rangeSensor.status();
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

  // ModernRoboticsI2cRangeSensor

  /**
   * MAX_NEW_I2C_ADDRESS property getter.
   */
  @SimpleProperty(description = "The constant for MAX_NEW_I2C_ADDRESS.",
      category = PropertyCategory.BEHAVIOR)
  public int MAX_NEW_I2C_ADDRESS() {
    return ModernRoboticsUsbDeviceInterfaceModule.MAX_NEW_I2C_ADDRESS;
  }

  /**
   * MIN_NEW_I2C_ADDRESS property getter.
   */
  @SimpleProperty(description = "The constant for MIN_NEW_I2C_ADDRESS.",
      category = PropertyCategory.BEHAVIOR)
  public int MIN_NEW_I2C_ADDRESS() {
    return ModernRoboticsUsbDeviceInterfaceModule.MIN_NEW_I2C_ADDRESS;
  }

  /**
   * I2cAddress property setter.
   */
  @SimpleProperty
  public void I2cAddress(int newAddress) {
    checkHardwareDevice();
    if (rangeSensor != null) {
      try {
        rangeSensor.setI2cAddress(I2cAddr.create8bit(newAddress));
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
  @SimpleProperty(description = "The I2C address of the range sensor.",
      category = PropertyCategory.BEHAVIOR)
  public int I2cAddress() {
    checkHardwareDevice();
    if (rangeSensor != null) {
      try {
        return rangeSensor.getI2cAddress().get8Bit();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "I2cAddress",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * CmOptical property getter.
   */
  @SimpleProperty(description = "Returns the distance in centimeters, according to the optical sensor.",
      category = PropertyCategory.BEHAVIOR)
  public double CmOptical() {
    checkHardwareDevice();
    if (rangeSensor != null) {
      try {
        return rangeSensor.cmOptical();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "CmOptical",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * CmUltrasonic property getter.
   */
  @SimpleProperty(description = "Returns the distance in centimeters, according to the ultrasonic sensor.",
      category = PropertyCategory.BEHAVIOR)
  public double CmUltrasonic() {
    checkHardwareDevice();
    if (rangeSensor != null) {
      try {
        return rangeSensor.cmUltrasonic();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "CmUltrasonic",
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
    if (rangeSensor != null) {
      try {
        return rangeSensor.getRawLightDetectedMax();
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
    if (rangeSensor != null) {
      try {
        return rangeSensor.rawOptical();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "RawOptical",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * RawUltrasonic property getter.
   */
  @SimpleProperty(description = "Returns the raw ultrasonic value.",
      category = PropertyCategory.BEHAVIOR)
  public double RawUltrasonic() {
    checkHardwareDevice();
    if (rangeSensor != null) {
      try {
        return rangeSensor.rawUltrasonic();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "RawUltrasonic",
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
    if (rangeSensor != null) {
      try {
        for (DistanceUnit distanceUnitValue : DistanceUnit.values()) {
          if (distanceUnitValue.toString().equalsIgnoreCase(distanceUnit)) {
            return rangeSensor.getDistance(distanceUnitValue);
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
    rangeSensor = (ModernRoboticsI2cRangeSensor) hardwareMap.get(deviceClass, getDeviceName());
    return rangeSensor;
  }

  @Override
  protected void clearHardwareDeviceImpl() {
    rangeSensor = null;
  }
}
