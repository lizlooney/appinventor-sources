// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;

import com.qualcomm.hardware.adafruit.BNO055IMU;
import com.qualcomm.hardware.adafruit.BNO055IMU.AccelUnit;
import com.qualcomm.hardware.adafruit.BNO055IMU.AngleUnit;
import com.qualcomm.hardware.adafruit.BNO055IMU.CalibrationStatus;
import com.qualcomm.hardware.adafruit.BNO055IMU.SensorMode;
import com.qualcomm.hardware.adafruit.BNO055IMU.SystemError;
import com.qualcomm.hardware.adafruit.BNO055IMU.SystemStatus;
import com.qualcomm.hardware.adafruit.BNO055IMU.TempUnit;
import com.qualcomm.hardware.adafruit.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.util.ReadWriteFile;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.MagneticFlux;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.Temperature;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;
import org.firstinspires.ftc.robotcore.internal.AppUtil;

import android.text.TextUtils;

/**
 * A component for an BNO055IMU sensor of an FTC robot.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@DesignerComponent(version = YaVersion.FTC_BNO055IMU_COMPONENT_VERSION,
    description = "A component for a BNO055IMU sensor of an FTC robot.",
    category = ComponentCategory.FIRSTTECHCHALLENGE,
    nonVisible = true,
    iconName = "images/ftc.png")
@SimpleObject
@UsesLibraries(libraries = "FtcHardware.jar,FtcRobotCore.jar")
public final class FtcBNO055IMU extends FtcHardwareDevice {

  private static final String DEFAULT_LOGGING_TAG = "AdaFruitIMU";

  private volatile BNO055IMU bno055imu;
  private volatile AccelUnit accelUnit = AccelUnit.METERS_PERSEC_PERSEC;
  private volatile AngleUnit angleUnit = AngleUnit.DEGREES;
  private volatile TempUnit tempUnit = TempUnit.CELSIUS;
  private volatile SensorMode sensorMode = SensorMode.IMU;
  private volatile String calibrationDataFile = "";
  private volatile AccelerationIntegrationAlgorithm accelerationIntegrationAlgorithm =
      AccelerationIntegrationAlgorithm.NAIVE;
  private volatile boolean loggingEnabled = false;
  private volatile String loggingTag = DEFAULT_LOGGING_TAG;

  /**
   * Creates a new FtcBNO055IMU component.
   */
  public FtcBNO055IMU(ComponentContainer container) {
    super(container.$form(), BNO055IMU.class);
  }

  // Designer properties

  /**
   * AccelUnit property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "The acceleration unit.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public int AccelUnit() {
    return accelUnit.bVal;
  }

  /**
   * AccelUnit property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FTC_BNO055_ACCEL_UNIT,
      defaultValue = "0") // AccelUnit.METERS_PERSEC_PERSEC.bVal
  @SimpleProperty(userVisible = false)
  public void AccelUnit(int accelUnitValue) {
    for (AccelUnit accelUnit : AccelUnit.values()) {
      if ((byte) accelUnitValue == accelUnit.bVal) {
        this.accelUnit = accelUnit;
      }
    }
  }

  /**
   * AngleUnit property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "The angle unit.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public int AngleUnit() {
    return angleUnit.bVal;
  }

  /**
   * AngleUnit property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FTC_BNO055_ANGLE_UNIT,
      defaultValue = "0") // AngleUnit.DEGREES.bVal
  @SimpleProperty(userVisible = false)
  public void AngleUnit(int angleUnitValue) {
    for (AngleUnit angleUnit : AngleUnit.values()) {
      if ((byte) angleUnitValue == angleUnit.bVal) {
        this.angleUnit = angleUnit;
      }
    }
  }

  /**
   * SensorMode property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "The sensor mode.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public int SensorMode() {
    return sensorMode.bVal;
  }

  /**
   * SensorMode property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FTC_BNO055_SENSOR_MODE,
      defaultValue = "8") // SensorMode.IMU.bVal
  @SimpleProperty(userVisible = false)
  public void SensorMode(int sensorModeValue) {
    for (SensorMode sensorMode : SensorMode.values()) {
      if ((byte) sensorModeValue == sensorMode.bVal) {
        this.sensorMode = sensorMode;
      }
    }
  }

  /**
   * TempUnit property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "The temperature unit.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public int TempUnit() {
    return tempUnit.bVal;
  }

  /**
   * TempUnit property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FTC_BNO055_TEMP_UNIT,
      defaultValue = "0") // TempUnit.CELSIUS.bVal
  @SimpleProperty(userVisible = false)
  public void TempUnit(int tempUnitValue) {
    for (TempUnit tempUnit : TempUnit.values()) {
      if ((byte) tempUnitValue == tempUnit.bVal) {
        this.tempUnit = tempUnit;
      }
    }
  }

  /**
   * CalibrationDataFile property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "The name of the calibration data file.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public String CalibrationDataFile() {
    return calibrationDataFile;
  }

  /**
   * CalibrationDataFile property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "")
  @SimpleProperty(userVisible = false)
  public void CalibrationDataFile(String calibrationDataFile) {
    this.calibrationDataFile = calibrationDataFile;
  }

  private enum AccelerationIntegrationAlgorithm {
    NAIVE(0),
    JUST_LOGGING(1);

    private final byte bVal;

    AccelerationIntegrationAlgorithm(int i) {
      bVal = (byte) i;
    }
  }

  /**
   * AccelerationIntegrationAlgorithm property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "The acceleration integration algorithm.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public int AccelerationIntegrationAlgorithm() {
    return accelerationIntegrationAlgorithm.bVal;
  }

  /**
   * AccelerationIntegrationAlgorithm property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FTC_BNO055_ACCELERATION_INTEGRATION_ALGORITHM,
      defaultValue = "0") // AccelerationIntegrationAlgorithm.NAIVE.bVal
  @SimpleProperty(userVisible = false)
  public void AccelerationIntegrationAlgorithm(int accelerationIntegrationAlgorithmValue) {
    for (AccelerationIntegrationAlgorithm accelerationIntegrationAlgorithm : AccelerationIntegrationAlgorithm.values()) {
      if ((byte) accelerationIntegrationAlgorithmValue == accelerationIntegrationAlgorithm.bVal) {
        this.accelerationIntegrationAlgorithm = accelerationIntegrationAlgorithm;
      }
    }
  }

  /**
   * LoggingEnabled property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "Whether logging is enabled for this device.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public boolean LoggingEnabled() {
    return loggingEnabled;
  }

  /**
   * LoggingEnabled property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty(userVisible = false)
  public void LoggingEnabled(boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
  }

  /**
   * LoggingTag property getter.
   * Not visible in blocks.
   */
  @SimpleProperty(description = "The logging tag.",
      category = PropertyCategory.BEHAVIOR, userVisible = false)
  public String LoggingTag() {
    return loggingTag;
  }

  /**
   * LoggingTag property setter.
   * Can only be set in designer; not visible in blocks.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = DEFAULT_LOGGING_TAG)
  @SimpleProperty(userVisible = false)
  public void LoggingTag(String loggingTag) {
    this.loggingTag = loggingTag;
  }

  @SimpleFunction(description = "Initialize the IMU sensor")
  public void Initialize() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = angleUnit;
        parameters.accelUnit = accelUnit;
        parameters.mode = sensorMode;
        parameters.temperatureUnit = tempUnit;
        if (!TextUtils.isEmpty(calibrationDataFile)) {
          parameters.calibrationDataFile = calibrationDataFile;
        }
        switch (accelerationIntegrationAlgorithm) {
          case NAIVE:
            parameters.accelerationIntegrationAlgorithm = null;
            break;
          case JUST_LOGGING:
            parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
            break;
        }
        parameters.loggingEnabled = loggingEnabled;
        parameters.loggingTag = loggingTag;
        bno055imu.initialize(parameters);
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Initialize",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
  }

  @SimpleFunction(description = "Saves the calibration data to the given file.")
  public void SaveCalibrationData(String fileName) {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        ReadWriteFile.writeFile(
            AppUtil.getInstance().getSettingsFile(fileName),
            bno055imu.readCalibrationData().serialize());
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "SaveCalibrationData",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
  }

  /**
   * AngularOrientation property getter.
   */
  @SimpleProperty(description = "Returns an Orientation object representing the absolute " +
      "orientation of the sensor as a set three angles.",
      category = PropertyCategory.BEHAVIOR)
  public Object AngularOrientation() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.getAngularOrientation();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "AngularOrientation",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  /**
   * OverallAcceleration property getter.
   */
  @SimpleProperty(description = "Returns an Acceleration object representing the overall " +
      "acceleration experienced by the sensor. This is composed of a component due to the " +
      "movement of the sensor and a component due to the force of gravity.",
      category = PropertyCategory.BEHAVIOR)
  public Object OverallAcceleration() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.getOverallAcceleration();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "OverallAcceleration",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  /**
   * AngularVelocity property getter.
   */
  @SimpleProperty(description = "Returns an AngularVelocity object representing the rate of " +
      "change of the absolute orientation of the sensor.",
      category = PropertyCategory.BEHAVIOR)
  public Object AngularVelocity() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.getAngularVelocity();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "AngularVelocity",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }


  /**
   * LinearAcceleration property getter.
   */
  @SimpleProperty(description = "Returns an Acceleration object representing the acceleration " +
      "experienced by the sensor due to the movement of the sensor.",
      category = PropertyCategory.BEHAVIOR)
  public Object LinearAcceleration() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.getLinearAcceleration();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "LinearAcceleration",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  /**
   * Gravity property getter.
   */
  @SimpleProperty(description = "Returns an Acceleration object representing the direction of the " +
      "force of gravity relative to the sensor.",
      category = PropertyCategory.BEHAVIOR)
  public Object Gravity() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.getGravity();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Gravity",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  /**
   * Temperature property getter.
   */
  @SimpleProperty(description = "Returns the current temperature.",
      category = PropertyCategory.BEHAVIOR)
  public double Temperature() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        Temperature temperature = bno055imu.getTemperature();
        if (temperature != null) {
          return temperature.temperature;
        }
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Temperature",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return 0;
  }

  /**
   * MagneticFieldStrength property getter.
   */
  @SimpleProperty(description = "Returns a MagneticFlux object representing the magnetic field " +
      "strength experienced by the sensor.",
      category = PropertyCategory.BEHAVIOR)
  public Object MagneticFieldStrength() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.getMagneticFieldStrength();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "MagneticFieldStrength",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  /**
   * QuaternionOrientation property getter.
   */
  @SimpleProperty(description = "Returns a Quaternion object representing the absolute " +
      "orientation of the sensor as a quaternion.",
      category = PropertyCategory.BEHAVIOR)
  public Object QuaternionOrientation() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.getQuaternionOrientation();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "QuaternionOrientation",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  /**
   * Position property getter.
   */
  @SimpleProperty(description = "Returns a Position object representing the current position of " +
      "the sensor as calculated by doubly integrating the observed sensor accelerations.",
      category = PropertyCategory.BEHAVIOR)
  public Object Position() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.getPosition();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Position",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  /**
   * Velocity property getter.
   */
  @SimpleProperty(description = "Returns a Velocity object representing the current velocity of " +
      "the sensor as calculated by integrating the observed sensor accelerations.",
      category = PropertyCategory.BEHAVIOR)
  public Object Velocity() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.getVelocity();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Velocity",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  /**
   * Acceleration property getter.
   */
  @SimpleProperty(description = "Returns an Acceleration object representing the last observed " +
      "acceleration of the sensor. Note that this does not communicate with the sensor, but " +
      "rather returns the most recent value reported to the acceleration integration algorithm.",
      category = PropertyCategory.BEHAVIOR)
  public Object Acceleration() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.getAcceleration();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "Acceleration",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return null;
  }

  @SimpleFunction(description = "Start (or re-start) a thread that continuously at intervals " +
      "polls the current linear acceleration of the sensor and integrates it to provide velocity " +
      "and position information. The poll interval is specified in milliseconds.")
  public void StartAccelerationIntegration(int pollInterval) {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        bno055imu.startAccelerationIntegration(new Position(), new Velocity(), pollInterval);
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "StopAccelerationIntegration",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
  }

  @SimpleFunction(description = "Stop the integration thread if it is currently running.")
  public void StopAccelerationIntegration() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        bno055imu.stopAccelerationIntegration();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "StopAccelerationIntegration",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
  }

  @SimpleProperty(description = "Returns the current status of the system.",
      category = PropertyCategory.BEHAVIOR)
  public String SystemStatus() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        SystemStatus systemStatus = bno055imu.getSystemStatus();
        if (systemStatus != null) {
          return systemStatus.toString();
        }
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "SystemStatus",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return "";
  }

  @SimpleProperty(description = "If SystemStatus is \"SYSTEM_ERROR\", returns particulars " +
      "regarding that error.",
      category = PropertyCategory.BEHAVIOR)
  public String SystemError() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        SystemError systemError = bno055imu.getSystemError();
        if (systemError != null) {
          return systemError.toString();
        }
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "SystemError",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return "";
  }

  @SimpleProperty(description = "Returns the calibration status of the IMU.",
      category = PropertyCategory.BEHAVIOR)
  public String CalibrationStatus() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        CalibrationStatus calibrationStatus = bno055imu.getCalibrationStatus();
        if (calibrationStatus != null) {
          return calibrationStatus.toString();
        }
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "CalibrationStatus",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return "";
  }


  @SimpleFunction(description = "Answers as to whether the system is fully calibrated. The " +
      "system is fully calibrated if the gyro, accelerometer, and magnetometer are fully " +
      "calibrated.")
  public boolean IsSystemCalibrated() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.isSystemCalibrated();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "IsSystemCalibrated",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return false;
  }

  @SimpleFunction(description = "Answers as to whether the gyro is fully calibrated.")
  public boolean IsGyroCalibrated() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.isGyroCalibrated();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "IsGyroCalibrated",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return false;
  }

  @SimpleFunction(description = "Answers as to whether the accelerometer is fully calibrated.")
  public boolean IsAccelerometerCalibrated() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.isAccelerometerCalibrated();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "IsAccelerometerCalibrated",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return false;
  }

  @SimpleFunction(description = "Answers as to whether the magnetometer is fully calibrated.")
  public boolean IsMagnetometerCalibrated() {
    checkHardwareDevice();
    if (bno055imu != null) {
      try {
        return bno055imu.isMagnetometerCalibrated();
      } catch (Throwable e) {
        e.printStackTrace();
        form.dispatchErrorOccurredEvent(this, "IsMagnetometerCalibrated",
            ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
      }
    }
    return false;
  }

  // Functions to extract Acceleration, MagneticFlux, Position, Quaternion, and Velocity fields

  @SimpleFunction(description = "Returns the W value of the given Quaternion object.")
  public double GetW(Object object) {
    if (object instanceof Quaternion) {
      return ((Quaternion) object).w;
    }
    form.dispatchErrorOccurredEvent(this, "GetW",
        ErrorMessages.ERROR_FTC_INVALID_QUATERNION, "object");
    return 0;
  }

  @SimpleFunction(description = "Returns the X value of the given Acceleration, MagneticFlux, " +
      "Position, Quaternion, or Velocity object.")
  public double GetX(Object object) {
    if (object instanceof Acceleration) {
      return ((Acceleration) object).xAccel;
    }
    if (object instanceof MagneticFlux) {
      return ((MagneticFlux) object).x;
    }
    if (object instanceof Position) {
      return ((Position) object).x;
    }
    if (object instanceof Quaternion) {
      return ((Quaternion) object).x;
    }
    if (object instanceof Velocity) {
      return ((Velocity) object).xVeloc;
    }
    form.dispatchErrorOccurredEvent(this, "GetX",
        ErrorMessages.ERROR_FTC_INVALID_ACCELERATION_MAGNETIC_FLUX_POSITION_QUATERNION_VELOCITY, "object");
    return 0;
  }

  @SimpleFunction(description = "Returns the Y value of the given Acceleration, MagneticFlux, " +
      "Position, Quaternion, or Velocity object.")
  public double GetY(Object object) {
    if (object instanceof Acceleration) {
      return ((Acceleration) object).yAccel;
    }
    if (object instanceof MagneticFlux) {
      return ((MagneticFlux) object).y;
    }
    if (object instanceof Position) {
      return ((Position) object).y;
    }
    if (object instanceof Quaternion) {
      return ((Quaternion) object).y;
    }
    if (object instanceof Velocity) {
      return ((Velocity) object).yVeloc;
    }
    form.dispatchErrorOccurredEvent(this, "GetY",
        ErrorMessages.ERROR_FTC_INVALID_ACCELERATION_MAGNETIC_FLUX_POSITION_QUATERNION_VELOCITY, "object");
    return 0;
  }

  @SimpleFunction(description = "Returns the Z value of the given Acceleration, MagneticFlux, " +
      "Position, Quaternion, or Velocity object.")
  public double GetZ(Object object) {
    if (object instanceof Acceleration) {
      return ((Acceleration) object).zAccel;
    }
    if (object instanceof MagneticFlux) {
      return ((MagneticFlux) object).z;
    }
    if (object instanceof Position) {
      return ((Position) object).z;
    }
    if (object instanceof Quaternion) {
      return ((Quaternion) object).z;
    }
    if (object instanceof Velocity) {
      return ((Velocity) object).zVeloc;
    }
    form.dispatchErrorOccurredEvent(this, "GetZ",
        ErrorMessages.ERROR_FTC_INVALID_ACCELERATION_MAGNETIC_FLUX_POSITION_QUATERNION_VELOCITY, "object");
    return 0;
  }

  @SimpleFunction(description = "Returns a text representation of the given Acceleration, MagneticFlux, " +
      "Orientation, Position, or Velocity object.")
  public String ToText(Object object) {
    if (object != null) {
      return object.toString();
    }
    form.dispatchErrorOccurredEvent(this, "ToText",
        ErrorMessages.ERROR_FTC_INVALID_OBJECT, "object");
    return "";
  }

  // Functions to extract Orientation fields

  @SimpleFunction(description = "Returns the first angle of the given Orientation object.")
  public float OrientationFirstAngle(Object orientation) {
    if (!(orientation instanceof Orientation)) {
      form.dispatchErrorOccurredEvent(this, "OrientationFirstAngle",
          ErrorMessages.ERROR_FTC_INVALID_ORIENTATION, "orientation");
      return 0;
    }
    return ((Orientation) orientation).firstAngle;
  }

  @SimpleFunction(description = "Returns the Second angle of the given Orientation object.")
  public float OrientationSecondAngle(Object orientation) {
    if (!(orientation instanceof Orientation)) {
      form.dispatchErrorOccurredEvent(this, "OrientationSecondAngle",
          ErrorMessages.ERROR_FTC_INVALID_ORIENTATION, "orientation");
      return 0;
    }
    return ((Orientation) orientation).secondAngle;
  }

  @SimpleFunction(description = "Returns the third angle of the given Orientation object.")
  public float OrientationThirdAngle(Object orientation) {
    if (!(orientation instanceof Orientation)) {
      form.dispatchErrorOccurredEvent(this, "OrientationThirdAngle",
          ErrorMessages.ERROR_FTC_INVALID_ORIENTATION, "orientation");
      return 0;
    }
    return ((Orientation) orientation).thirdAngle;
  }

  @SimpleFunction(description = "Converts an Orientation object to an equivalent one with the " +
      "indicated point of view. Returns an Orientation object.")
  public Object OrientationToAxesReference(Object orientation, String axesReference) {
    if (!(orientation instanceof Orientation)) {
      form.dispatchErrorOccurredEvent(this, "OrientationToAxesReference",
          ErrorMessages.ERROR_FTC_INVALID_ORIENTATION, "orientation");
      return null;
    }
    AxesReference axesReferenceValue = parseAxesReference(axesReference, "OrientationToAxesReference");
    if (axesReferenceValue == null) {
      return null;
    }
    return ((Orientation) orientation).toAxesReference(axesReferenceValue);
  }

  @SimpleFunction(description = "Converts an Orientation object to an equivalent one with the " +
      "indicated ordering of axes. Returns an Orientation object.")
  public Object OrientationToAxesOrder(Object orientation, String axesOrder) {
    if (!(orientation instanceof Orientation)) {
      form.dispatchErrorOccurredEvent(this, "OrientationToAxesOrder",
          ErrorMessages.ERROR_FTC_INVALID_ORIENTATION, "orientation");
      return null;
    }
    AxesOrder axesOrderValue = parseAxesOrder(axesOrder, "OrientationToAxesOrder");
    if (axesOrderValue == null) {
      return null;
    }
    return ((Orientation) orientation).toAxesOrder(axesOrderValue);
  }

  // Functions to extract AngularVelocity fields

  @SimpleFunction(description = "Converts an AngularVelocity object to an equivalent one with " +
      "the indicated point of view. Returns an AngularVelocity object.")
  public Object AngularVelocityToAxesReference(Object angularVelocity, String axesReference) {
    if (!(angularVelocity instanceof AngularVelocity)) {
      form.dispatchErrorOccurredEvent(this, "AngularVelocityToAxesReference",
          ErrorMessages.ERROR_FTC_INVALID_ANGULAR_VELOCITY, "angularVelocity");
      return null;
    }
    AxesReference axesReferenceValue = parseAxesReference(axesReference, "AngularVelocityToAxesReference");
    if (axesReferenceValue == null) {
      return null;
    }
    return ((AngularVelocity) angularVelocity).toAxesReference(axesReferenceValue);
  }

  @SimpleFunction(description = "Returns the first angle rate of the given AngularVelocity object.")
  public float AngularVelocityFirstAngleRate(Object angularVelocity) {
    if (!(angularVelocity instanceof AngularVelocity)) {
      form.dispatchErrorOccurredEvent(this, "AngularVelocityFirstAngleRate",
          ErrorMessages.ERROR_FTC_INVALID_ANGULAR_VELOCITY, "angularVelocity");
      return 0;
    }
    return ((AngularVelocity) angularVelocity).firstAngleRate;
  }

  @SimpleFunction(description = "Returns the second angle rate of the given AngularVelocity object.")
  public float AngularVelocitySecondAngleRate(Object angularVelocity) {
    if (!(angularVelocity instanceof AngularVelocity)) {
      form.dispatchErrorOccurredEvent(this, "AngularVelocitySecondAngleRate",
          ErrorMessages.ERROR_FTC_INVALID_ANGULAR_VELOCITY, "angularVelocity");
      return 0;
    }
    return ((AngularVelocity) angularVelocity).secondAngleRate;
  }

  @SimpleFunction(description = "Returns the third angle rate of the given AngularVelocity object.")
  public float AngularVelocityThirdAngleRate(Object angularVelocity) {
    if (!(angularVelocity instanceof AngularVelocity)) {
      form.dispatchErrorOccurredEvent(this, "AngularVelocityThirdAngleRate",
          ErrorMessages.ERROR_FTC_INVALID_ANGULAR_VELOCITY, "angularVelocity");
      return 0;
    }
    return ((AngularVelocity) angularVelocity).thirdAngleRate;
  }

  // AxesOrder enum values

  /**
   * AxesOrder_XYX property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_XYX.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_XYX() {
    return AxesOrder.XYX.toString();
  }

  /**
   * AxesOrder_XZX property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_XZX.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_XZX() {
    return AxesOrder.XZX.toString();
  }

  /**
   * AxesOrder_XYZ property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_XYZ.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_XYZ() {
    return AxesOrder.XYZ.toString();
  }

  /**
   * AxesOrder_XZY property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_XZY.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_XZY() {
    return AxesOrder.XZY.toString();
  }

  /**
   * AxesOrder_YXY property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_YXY.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_YXY() {
    return AxesOrder.YXY.toString();
  }

  /**
   * AxesOrder_YXZ property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_YXZ.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_YXZ() {
    return AxesOrder.YXZ.toString();
  }

  /**
   * AxesOrder_YZX property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_YZX.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_YZX() {
    return AxesOrder.YZX.toString();
  }

  /**
   * AxesOrder_YZY property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_YZY.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_YZY() {
    return AxesOrder.YZY.toString();
  }

  /**
   * AxesOrder_ZYZ property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_ZYZ.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_ZYZ() {
    return AxesOrder.ZYZ.toString();
  }

  /**
   * AxesOrder_ZXZ property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_ZXZ.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_ZXZ() {
    return AxesOrder.ZXZ.toString();
  }

  /**
   * AxesOrder_ZYX property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_ZYX.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_ZYX() {
    return AxesOrder.ZYX.toString();
  }

  /**
   * AxesOrder_ZXY property getter.
   */
  @SimpleProperty(description = "The constant for AxesOrder_ZXY.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesOrder_ZXY() {
    return AxesOrder.ZXY.toString();
  }

  private AxesOrder parseAxesOrder(String axesOrder, String functionName) {
    for (AxesOrder axesOrderValue : AxesOrder.values()) {
      if (axesOrderValue.toString().equalsIgnoreCase(axesOrder)) {
        return axesOrderValue;
      }
    }
    form.dispatchErrorOccurredEvent(this, functionName,
        ErrorMessages.ERROR_FTC_INVALID_AXES_ORDER, axesOrder);
    return null;
  }

  // AxesReference enum values

  /**
   * AxesReference_EXTRINSIC property getter.
   */
  @SimpleProperty(description = "The constant for AxesReference_EXTRINSIC. " +
      "Indicates that the axes remain fixed in the world around the object.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesReference_EXTRINSIC() {
    return AxesReference.EXTRINSIC.toString();
  }

  /**
   * AxesReference_INTRINSIC property getter.
   */
  @SimpleProperty(description = "The constant for AxesReference_INTRINSIC." +
      "Indicates that the axes move with the object that is rotating.",
      category = PropertyCategory.BEHAVIOR)
  public String AxesReference_INTRINSIC() {
    return AxesReference.INTRINSIC.toString();
  }

  private AxesReference parseAxesReference(String axesReference, String functionName) {
    for (AxesReference axesReferenceValue : AxesReference.values()) {
      if (axesReferenceValue.toString().equalsIgnoreCase(axesReference)) {
        return axesReferenceValue;
      }
    }
    form.dispatchErrorOccurredEvent(this, functionName,
        ErrorMessages.ERROR_FTC_INVALID_AXES_REFERENCE, axesReference);
    return null;
  }

  // FtcHardwareDevice implementation

  @Override
  protected Object initHardwareDeviceImpl() {
    bno055imu = (BNO055IMU) hardwareMap.get(deviceClass, getDeviceName());
    return bno055imu;
  }

  @Override
  protected void clearHardwareDeviceImpl() {
    bno055imu = null;
  }
}
