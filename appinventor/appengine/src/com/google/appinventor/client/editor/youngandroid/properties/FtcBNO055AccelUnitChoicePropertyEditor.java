// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid.properties;

import com.google.appinventor.client.widgets.properties.ChoicePropertyEditor;

/**
 * Property editor for choosing the acceleration unit for an FtcBNO055IMU component.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class FtcBNO055AccelUnitChoicePropertyEditor extends ChoicePropertyEditor {

  // FTC BNO055IMU accel unit choices
  private static final Choice[] accelUnits = new Choice[] {
    new Choice("Meters per second per second", "0"),
    new Choice("Milli earth gravity", "1"),
  };

  public FtcBNO055AccelUnitChoicePropertyEditor() {
    super(accelUnits);
  }
}
