// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid.properties;

import com.google.appinventor.client.widgets.properties.ChoicePropertyEditor;

/**
 * Property editor for choosing the angle unit for an FtcBNO055IMU component.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class FtcBNO055AngleUnitChoicePropertyEditor extends ChoicePropertyEditor {

  // FTC BNO055IMU angle unit choices
  private static final Choice[] angleUnits = new Choice[] {
    new Choice("Degrees", "0"),
    new Choice("Radians", "1"),
  };

  public FtcBNO055AngleUnitChoicePropertyEditor() {
    super(angleUnits);
  }
}
