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

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cCompassSensor;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsUsbDeviceInterfaceModule;
import com.qualcomm.robotcore.hardware.CompassSensor;
import com.qualcomm.robotcore.hardware.CompassSensor.CompassMode;
import com.qualcomm.robotcore.hardware.I2cAddr;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.MagneticFlux;

/**
 * A component for a compass sensor of an FTC robot.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@DesignerComponent(version = YaVersion.FTC_COMPASS_SENSOR_COMPONENT_VERSION,
    description = "A component for a compass sensor of an FTC robot.",
    category = ComponentCategory.FIRSTTECHCHALLENGE,
    nonVisible = true,
    iconName = "images/ftcCompassSensor.png")
@SimpleObject
@UsesLibraries(libraries = "FtcHardware.jar,FtcRobotCore.jar")
public final class FtcCompassSensor extends FtcHardwareDevice {

  private volatile CompassSensor compassSensor;

  /**
   * Creates a new FtcCompassSensor component.
   */
  public FtcCompassSensor(ComponentContainer container) {
    super(container.$form(), CompassSensor.class);
  }

  /**
   * Direction property getter.
   */
  @SimpleProperty(description = "The direction, in degrees.",
      category = PropertyCategory.BEHAVIOR)
  public double Direction() {
    checkHardwareDevice();
    if (compassSensor != null) {
      try {
        return compassSensor.getDirection();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Direction",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * Status property getter.
   */
  @SimpleProperty(description = "The status.",
      category = PropertyCategory.BEHAVIOR)
  public String Status() {
    checkHardwareDevice();
    if (compassSensor != null) {
      try {
        String status = compassSensor.status();
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

  /**
   * CompassMode_MEASUREMENT_MODE property getter.
   */
  @SimpleProperty(description = "The constant for CompassMode_MEASUREMENT_MODE.",
      category = PropertyCategory.BEHAVIOR)
  public String CompassMode_MEASUREMENT_MODE() {
    return CompassMode.MEASUREMENT_MODE.toString();
  }

  /**
   * CompassMode_CALIBRATION_MODE property getter.
   */
  @SimpleProperty(description = "The constant for CompassMode_CALIBRATION_MODE.",
      category = PropertyCategory.BEHAVIOR)
  public String CompassMode_CALIBRATION_MODE() {
    return CompassMode.CALIBRATION_MODE.toString();
  }

  @SimpleFunction(description = "Change to calibration or measurement mode.\n" +
      "Valid values are CompassMode_CALIBRATION_MODE or CompassMode_MEASUREMENT_MODE.")
  public void SetMode(String compassMode) {
    checkHardwareDevice();
    if (compassSensor != null) {
      try {
        for (CompassMode compassModeValue : CompassMode.values()) {
          if (compassModeValue.toString().equalsIgnoreCase(compassMode)) {
            compassSensor.setMode(compassModeValue);
            return;
          }
        }

        form.dispatchErrorOccurredEvent(this, "SetMode",
            ErrorMessages.ERROR_FTC_INVALID_COMPASS_MODE, compassMode);
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "SetMode",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
  }

  /**
   * CalibrationFailed property getter.
   */
  @SimpleProperty(description = "Whether calibration failed.",
      category = PropertyCategory.BEHAVIOR)
  public boolean CalibrationFailed() {
    checkHardwareDevice();
    if (compassSensor != null) {
      try {
        return compassSensor.calibrationFailed();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "CalibrationFailed",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return false;
  }

  // ModernRoboticsI2cCompassSensor

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
    if (compassSensor != null) {
      try {
        if (compassSensor instanceof ModernRoboticsI2cCompassSensor) {
          ((ModernRoboticsI2cCompassSensor) compassSensor)
              .setI2cAddress(I2cAddr.create8bit(newAddress));
        }
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
  @SimpleProperty(description = "The I2C address of the compass sensor. " + 
      "Not all compass sensors support this feature.",
      category = PropertyCategory.BEHAVIOR)
  public int I2cAddress() {
    checkHardwareDevice();
    if (compassSensor != null) {
      try {
        if (compassSensor instanceof ModernRoboticsI2cCompassSensor) {
          return ((ModernRoboticsI2cCompassSensor) compassSensor).getI2cAddress().get8Bit();
        }
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "I2cAddress",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * Acceleration property getter.
   */
  @SimpleProperty(description = "Returns an Acceleration object representing the acceleration " +
      "detected by the sensor.",
      category = PropertyCategory.BEHAVIOR)
  public Object Acceleration() {
    checkHardwareDevice();
    if (compassSensor != null) {
      try {
        if (compassSensor instanceof ModernRoboticsI2cCompassSensor) {
          return ((ModernRoboticsI2cCompassSensor) compassSensor).getAcceleration();
        }
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Acceleration",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  /**
   * MagneticFlux property getter.
   */
  @SimpleProperty(description = "Returns a MagneticFlux object representing the magnetic flux " +
      "detected by the sensor. Not all compass sensors support this feature.",
      category = PropertyCategory.BEHAVIOR)
  public Object MagneticFlux() {
    checkHardwareDevice();
    if (compassSensor != null) {
      try {
        if (compassSensor instanceof ModernRoboticsI2cCompassSensor) {
          return ((ModernRoboticsI2cCompassSensor) compassSensor).getMagneticFlux();
        }
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "MagneticFlux",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  // Functions to extract fields from Acceleration and MagneticFlux

  @SimpleFunction(description = "Returns the X value of the given Acceleration or MagneticFlux object.")
  public double GetX(Object object) {
    if (object instanceof Acceleration) {
      return ((Acceleration) object).xAccel;
    }
    if (object instanceof MagneticFlux) {
      return ((MagneticFlux) object).x;
    }

    form.dispatchErrorOccurredEvent(this, "GetX",
        ErrorMessages.ERROR_FTC_INVALID_ACCELERATION_MAGNETIC_FLUX, "object");
    return 0;
  }

  @SimpleFunction(description = "Returns the Y value of the given Acceleration or MagneticFlux object.")
  public double GetY(Object object) {
    if (object instanceof Acceleration) {
      return ((Acceleration) object).yAccel;
    }
    if (object instanceof MagneticFlux) {
      return ((MagneticFlux) object).y;
    }

    form.dispatchErrorOccurredEvent(this, "GetY",
        ErrorMessages.ERROR_FTC_INVALID_ACCELERATION_MAGNETIC_FLUX, "object");
    return 0;
  }

  @SimpleFunction(description = "Returns the Z value of the given Acceleration or MagneticFlux.")
  public double GetZ(Object object) {
    if (object instanceof Acceleration) {
      return ((Acceleration) object).zAccel;
    }
    if (object instanceof MagneticFlux) {
      return ((MagneticFlux) object).z;
    }

    form.dispatchErrorOccurredEvent(this, "GetZ",
        ErrorMessages.ERROR_FTC_INVALID_ACCELERATION_MAGNETIC_FLUX, "object");
    return 0;
  }

  // FtcHardwareDevice implementation

  @Override
  protected Object initHardwareDeviceImpl() {
    compassSensor = (CompassSensor) hardwareMap.get(deviceClass, getDeviceName());
    return compassSensor;
  }

  @Override
  protected void clearHardwareDeviceImpl() {
    compassSensor = null;
  }
}
