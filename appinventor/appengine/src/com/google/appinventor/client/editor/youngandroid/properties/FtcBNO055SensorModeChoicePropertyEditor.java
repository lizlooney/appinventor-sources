// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid.properties;

import com.google.appinventor.client.widgets.properties.ChoicePropertyEditor;

/**
 * Property editor for choosing the sensor mode for an FtcBNO055IMU component.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class FtcBNO055SensorModeChoicePropertyEditor extends ChoicePropertyEditor {

  // FTC BNO055IMU sensor mode choices
  private static final Choice[] sensorModes = new Choice[] {
    new Choice("CONFIG", "0"),
    new Choice("ACCONLY", "1"),
    new Choice("MAGONLY", "2"),
    new Choice("GYRONLY", "3"),
    new Choice("ACCMAG", "4"),
    new Choice("ACCGYRO", "5"),
    new Choice("MAGGYRO", "6"),
    new Choice("AMG", "7"),
    new Choice("IMU", "8"),
    new Choice("COMPASS", "9"),
    new Choice("M4G", "10"),
    new Choice("NDOF_FMC_OFF", "11"),
    new Choice("NDOF", "12"),
  };

  public FtcBNO055SensorModeChoicePropertyEditor() {
    super(sensorModes);
  }
}
