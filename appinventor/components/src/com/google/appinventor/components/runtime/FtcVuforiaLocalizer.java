// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2016 MIT, All rights reserved
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
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.Parameters;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.Parameters.CameraMonitorFeedback;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
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

  private volatile VuforiaLocalizer.Parameters parameters;
  private volatile VuforiaLocalizer vuforiaLocalizer;
  private volatile OpenGLMatrix phoneLocationOnRobot;
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

  private CameraDirection parseCameraDirection(String cameraDirection, String functionName) {
    for (CameraDirection cameraDirectionValue : CameraDirection.values()) {
      if (cameraDirectionValue.toString().equalsIgnoreCase(cameraDirection)) {
        return cameraDirectionValue;
      }
    }
    form.dispatchErrorOccurredEvent(this, functionName,
        ErrorMessages.ERROR_FTC_INVALID_CAMERA_DIRECTION, cameraDirection);
    return null;
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

  private CameraMonitorFeedback parseCameraMonitorFeedback(
      String cameraMonitorFeedback, String functionName) {
    for (CameraMonitorFeedback cameraMonitorFeedbackValue : CameraMonitorFeedback.values()) {
      if (cameraMonitorFeedbackValue.toString().equalsIgnoreCase(cameraMonitorFeedback)) {
        return cameraMonitorFeedbackValue;
      }
    }
    form.dispatchErrorOccurredEvent(this, functionName,
        ErrorMessages.ERROR_FTC_INVALID_CAMERA_MONITOR_FEEDBACK, cameraMonitorFeedback);
    return null;
  }

  @SimpleFunction(description = "Create the Vuforia localizer.")
  public void CreateVuforiaLocalizer(
      String cameraDirection, boolean useExtendedTracking, String cameraMonitorFeedback,
      boolean fillCameraMonitorViewParent) {
    try {
      parameters = new VuforiaLocalizer.Parameters(R.id.cameraMonitorViewId);
      CameraDirection cameraDirectionValue = parseCameraDirection(
          cameraDirection, "CreateVuforiaLocalizer");
      if (cameraDirectionValue != null) {
        parameters.cameraDirection = cameraDirectionValue;
      }
      parameters.useExtendedTracking = useExtendedTracking;
      CameraMonitorFeedback cameraMonitorFeedbackValue =
          parseCameraMonitorFeedback(cameraMonitorFeedback, "CreateVuforiaLocalizer");
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

  @SimpleFunction(description = "Loads a Vuforia dataset from the indicated application asset, " +
      "which must be of type .XML. The corresponding .DAT asset must be a sibling. Note that " +
      "this operation can be extremely lengthy, possibly taking a few seconds to execute. " +
      "Loading datasets from an asset you stored in your application APK is the recommended " +
      "approach to packaging datasets so they always travel along with your code. " +
      "In App Inventor, assets are called media.")
  public void LoadTrackablesFromAsset(String assetName) {
    try {
      if (vuforiaLocalizer != null && parameters != null) {
        afterLoad(vuforiaLocalizer.loadTrackablesFromAsset(assetName));
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
        afterLoad(vuforiaLocalizer.loadTrackablesFromFile(absoluteFileName));
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

  private void afterLoad(VuforiaTrackables vuforiaTrackables) {
    if (vuforiaTrackables != null) {
      trackablesList.add(vuforiaTrackables);
      for (VuforiaTrackable trackable : vuforiaTrackables) {
        trackableList.add(trackable);
        if (phoneLocationOnRobot != null) {
          ((VuforiaTrackableDefaultListener) trackable.getListener())
              .setPhoneInformation(phoneLocationOnRobot, parameters.cameraDirection);
        }
      }
    }
  }

  // VuforiaTrackables functions

  @SimpleFunction(description = "Activate trackables that were loaded with " +
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

  @SimpleFunction(description = "Deactivate trackables that were loaded with " +
      "LoadTrackablesFromAsset or LoadTrackablesFromFile.")
  public void DeactivateTrackables() {
    try {
      if (vuforiaLocalizer != null) {
        for (VuforiaTrackables trackables : trackablesList) {
          trackables.deactivate();
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "DeactivateTrackables",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "DeactivateTrackables",
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

  // Trackable functions

  @SimpleFunction(description = "Set the name of a trackable that was loaded with " +
      "LoadTrackablesFromAsset or LoadTrackablesFromFile.")
  public void SetTrackableName(int trackableNumber, String name) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          trackableList.get(trackableNumber).setName(name);
        } else {
          form.dispatchErrorOccurredEvent(this, "SetTrackableName",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
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
  public String GetTrackableName(int trackableNumber) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          return trackableList.get(trackableNumber).getName();
        } else {
          form.dispatchErrorOccurredEvent(this, "GetTrackableName",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
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

  @SimpleFunction(description = "Set the location (an OpenGLMatrix) of a trackable in the field.")
  public void SetTrackableLocation(int trackableNumber, Object matrix) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          if (matrix instanceof OpenGLMatrix) {
            trackableList.get(trackableNumber).setLocation((OpenGLMatrix) matrix);
          } else {
            form.dispatchErrorOccurredEvent(this, "SetTrackableLocation",
                ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix");
          }
        } else {
          form.dispatchErrorOccurredEvent(this, "SetTrackableLocation",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "SetTrackableLocation",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "SetTrackableLocation",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
  }

  @SimpleFunction(description = "Get the location (an OpenGLMatrix) of a trackable in the field.")
  public Object GetTrackableLocation(int trackableNumber) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          return trackableList.get(trackableNumber).getLocation();
        } else {
          form.dispatchErrorOccurredEvent(this, "GetTrackableLocation",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber,
              0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "GetTrackableLocation",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "GetTrackableLocation",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
    return null;
  }

  // VuforiaTrackableDefaultListener functions

  @SimpleFunction(description = "Specifies the location (an OpenGLMatrix) of the phone on the " +
      "robot. This is needed in order to compute the robot location.")
  public void SetPhoneLocationOnRobot(Object matrix) {
    try {
      if (matrix instanceof OpenGLMatrix) {
        phoneLocationOnRobot = (OpenGLMatrix) matrix;
        if (vuforiaLocalizer != null && parameters != null) {
          for (VuforiaTrackable trackable : trackableList) {
            ((VuforiaTrackableDefaultListener) trackable.getListener())
                .setPhoneInformation(phoneLocationOnRobot, parameters.cameraDirection);
          }
        } else {
          form.dispatchErrorOccurredEvent(this, "SetPhoneLocationOnRobot",
              ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "SetPhoneLocationOnRobot",
            ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix");
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "SetPhoneLocationOnRobot",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
  }

  @SimpleFunction(description = "Return true if the trackable (specified by number) is visible.")
  public boolean IsTrackableVisible(int trackableNumber) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          return ((VuforiaTrackableDefaultListener) trackableList.get(trackableNumber).getListener())
              .isVisible();
        } else {
          form.dispatchErrorOccurredEvent(this, "IsTrackableVisible",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "IsTrackableVisible",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "IsTrackableVisible",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
    return false;
  }

  @SimpleFunction(description = "Return the transform (an OpenGLMatrix) that represents the " +
      "location of the robot on the field computed from the specified tracker, or null if the " +
      "location cannot be computed.")
  public Object GetRobotLocation(int trackableNumber) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          return ((VuforiaTrackableDefaultListener) trackableList.get(trackableNumber).getListener())
              .getRobotLocation();
        } else {
          form.dispatchErrorOccurredEvent(this, "GetRobotLocation",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "GetRobotLocation",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "GetRobotLocation",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
    return null;
  }

  @SimpleFunction(description = "Return the transform (an OpenGLMatrix) that represents the " +
      "location of the robot on the field computed from the specified tracker, but only if a new " +
      "location has been detected since the last call to GetUpdatedRobotLocation.")
  public Object GetUpdatedRobotLocation(int trackableNumber) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          return ((VuforiaTrackableDefaultListener) trackableList.get(trackableNumber).getListener())
              .getUpdatedRobotLocation();
        } else {
          form.dispatchErrorOccurredEvent(this, "GetUpdatedRobotLocation",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "GetUpdatedRobotLocation",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "GetUpdatedRobotLocation",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
    return null;
  }

  @SimpleFunction(description = "Return the location (an OpenGLMatrix) of the trackable in the " +
      "phone's coordinate system, if it is currently visible, or null if it is not visible.")
  public Object GetPose(int trackableNumber) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          return ((VuforiaTrackableDefaultListener) trackableList.get(trackableNumber).getListener())
              .getPose();
        } else {
          form.dispatchErrorOccurredEvent(this, "GetPose",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "GetPose",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "GetPose",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
    return null;
  }

  // listener.getRawPose
  @SimpleFunction(description = "Return the raw location (an OpenGLMatrix) of the trackable in " +
      "the phone's coordinate system, as reported by Vuforia.")
  public Object GetRawPose(int trackableNumber) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          return ((VuforiaTrackableDefaultListener) trackableList.get(trackableNumber).getListener())
              .getRawPose();
        } else {
          form.dispatchErrorOccurredEvent(this, "GetRawPose",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "GetRawPose",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "GetRawPose",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
    return null;
  }

  @SimpleFunction(description = "Return the raw location (an OpenGLMatrix) of the trackable in " +
      "the phone's coordinate system, as reported by Vuforia, but only if a new location is " +
      "available since the last call to GetRawUpdatedPose.")
  public Object GetRawUpdatedPose(int trackableNumber) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          return ((VuforiaTrackableDefaultListener) trackableList.get(trackableNumber).getListener())
              .getRawUpdatedPose();
        } else {
          form.dispatchErrorOccurredEvent(this, "GetRawUpdatedPose",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "GetRawUpdatedPose",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "GetRawUpdatedPose",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
    return null;
  }

  @SimpleFunction(description = "Return the last known location (an OpenGLMatrix) of the " +
      "trackable in the phone's coordinate system, even if the trackable is no longer visible.")
  public Object GetLastTrackedRawPose(int trackableNumber) {
    try {
      if (vuforiaLocalizer != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          return ((VuforiaTrackableDefaultListener) trackableList.get(trackableNumber).getListener())
              .getLastTrackedRawPose();
        } else {
          form.dispatchErrorOccurredEvent(this, "GetLastTrackedRawPose",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "GetLastTrackedRawPose",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "GetLastTrackedRawPose",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
    return null;
  }

/*
  @SimpleFunction(description = "Sets the matrix to correct for the different coordinate systems " +
      "used in Vuforia and our phone coordinate system here. Here, with the phone in flat front " +
      "of you in portrait mode (as it is when running the robot controller app), Z is pointing " +
      "upwards, up out of the screen, X points to your right, and Y points away from you.")
  public void SetPoseCorrectionMatrix(int trackableNumber, Object matrix) {
    try {
      if (vuforiaLocalizer != null && parameters != null) {
        if (trackableNumber >= 0 && trackableNumber < trackableList.size()) {
          if (matrix instanceof OpenGLMatrix) {
            ((VuforiaTrackableDefaultListener) trackableList.get(trackableNumber).getListener())
                .setPoseCorrectionMatrix(parameters.cameraDirection, (OpenGLMatrix) matrix);
          } else {
            form.dispatchErrorOccurredEvent(this, "SetPoseCorrectionMatrix",
                ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix");
          }
        } else {
          form.dispatchErrorOccurredEvent(this, "SetPoseCorrectionMatrix",
              ErrorMessages.ERROR_FTC_INVALID_TRACKABLE_NUMBER, trackableNumber, 0, trackableList.size() - 1);
        }
      } else {
        form.dispatchErrorOccurredEvent(this, "SetPoseCorrectionMatrix",
            ErrorMessages.ERROR_FTC_VUFORIA_LOCALIZER_NOT_CREATED);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(this, "SetPoseCorrectionMatrix",
          ErrorMessages.ERROR_FTC_UNEXPECTED_ERROR, e.toString());
    }
  }
  */

  // OpenGLMatrix functions

  @SimpleFunction(description = "Returns true if the given OpenGLMatrix is null.")
  public boolean OpenGLMatrixIsNull(Object matrix) {
    if (matrix == null) {
      return true;
    }
    if (!(matrix instanceof OpenGLMatrix)) {
      form.dispatchErrorOccurredEvent(this, "OpenGLMatrixIsNull",
          ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix");
    }
    return false;
  }

  @SimpleFunction(description = "Returns a new OpenGLMatrix, initialized as the identity matrix.")
  public Object OpenGLMatrixIdentity() {
    return OpenGLMatrix.identityMatrix();
  }

  @SimpleFunction(description = "Returns a new OpenGLMatrix for rotation.")
  public Object OpenGLMatrixRotation(float angle, float dx, float dy, float dz) {
    return OpenGLMatrix.rotation(AngleUnit.DEGREES, angle, dx, dy, dz);
  }

  @SimpleFunction(description = "Returns a new OpenGLMatrix for translation.")
  public Object OpenGLMatrixTranslation(float dx, float dy, float dz) {
    return OpenGLMatrix.translation(dx, dy, dz);
  }

  @SimpleFunction(description = "Returns a new OpenGLMatrix, created by scaling an existing " +
      "matrix.")
  public Object OpenGLMatrixScaled(Object matrix, float scaleX, float scaleY, float scaleZ) {
    if (!(matrix instanceof OpenGLMatrix)) {
      form.dispatchErrorOccurredEvent(this, "OpenGLMatrixScaled",
          ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix");
      return null;
    }
    return ((OpenGLMatrix) matrix).scaled(scaleX, scaleY, scaleZ);
  }

  @SimpleFunction(description = "Returns a new OpenGLMatrix, created by translating an existing " +
      "matrix.")
  public Object OpenGLMatrixTranslated(Object matrix, float dx, float dy, float dz) {
    if (!(matrix instanceof OpenGLMatrix)) {
      form.dispatchErrorOccurredEvent(this, "OpenGLMatrixTranslated",
          ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix");
      return null;
    }
    return ((OpenGLMatrix) matrix).translated(dx, dy, dz);
  }

  @SimpleFunction(description = "Returns a new OpenGLMatrix, created by rotating an existing " +
      "matrix.")
  public Object OpenGLMatrixRotated(Object matrix, float angle, float dx, float dy, float dz) {
    if (!(matrix instanceof OpenGLMatrix)) {
      form.dispatchErrorOccurredEvent(this, "OpenGLMatrixRotated",
          ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix");
      return null;
    }
    return ((OpenGLMatrix) matrix).rotated(AngleUnit.DEGREES, angle, dx, dy, dz);
  }

  @SimpleFunction(description = "Returns a new OpenGLMatrix, created by inverting an existing " +
      "matrix.")
  public Object OpenGLMatrixInverted(Object matrix) {
    if (!(matrix instanceof OpenGLMatrix)) {
      form.dispatchErrorOccurredEvent(this, "OpenGLMatrixInverted",
          ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix");
      return null;
    }
    return ((OpenGLMatrix) matrix).inverted();
  }

  @SimpleFunction(description = "Returns a new OpenGLMatrix, created by transposing an existing " +
      "matrix.")
  public Object OpenGLMatrixTransposed(Object matrix) {
    if (!(matrix instanceof OpenGLMatrix)) {
      form.dispatchErrorOccurredEvent(this, "OpenGLMatrixTransposed",
          ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix");
      return null;
    }
    return ((OpenGLMatrix) matrix).transposed();
  }

  @SimpleFunction(description = "Returns a new OpenGLMatrix, created by multiplying two existing " +
      "matrices.")
  public Object OpenGLMatrixMultiplied(Object matrix1, Object matrix2) {
    if (!(matrix1 instanceof OpenGLMatrix)) {
      form.dispatchErrorOccurredEvent(this, "OpenGLMatrixMultiplied",
          ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix1");
      return null;
    }
    if (!(matrix2 instanceof OpenGLMatrix)) {
      form.dispatchErrorOccurredEvent(this, "OpenGLMatrixMultiplied",
          ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix2");
      return null;
    }
    return ((OpenGLMatrix) matrix1).multiplied((OpenGLMatrix) matrix2);
  }

  @SimpleFunction(description = "Returns the OpenGLMatrix associated with a particular set of " +
      "three rotational angles.")
  public Object OrientationGetRotationMatrix(
      String axesReference, String axesOrder, float angle1, float angle2, float angle3) {
    AxesReference axesReferenceValue =
        parseAxesReference(axesReference, "OrientationGetRotationMatrix");
    if (axesReferenceValue == null) {
      return null;
    }
    AxesOrder axesOrderValue = parseAxesOrder(axesOrder, "OrientationGetRotationMatrix");
    if (axesOrderValue == null) {
      return null;
    }
    return Orientation.getRotationMatrix(
        axesReferenceValue, axesOrderValue, AngleUnit.DEGREES, angle1, angle2, angle3);
  }

  @SimpleFunction(description = "Formats an OpenGLMatrix as text.")
  public String OpenGLMatrixFormat(Object matrix) {
    if (!(matrix instanceof OpenGLMatrix)) {
      form.dispatchErrorOccurredEvent(this, "OpenGLMatrixTransposed",
          ErrorMessages.ERROR_FTC_INVALID_OPEN_GL_MATRIX, "matrix");
      return "";
    }
    return ((OpenGLMatrix) matrix).formatAsTransform();
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
