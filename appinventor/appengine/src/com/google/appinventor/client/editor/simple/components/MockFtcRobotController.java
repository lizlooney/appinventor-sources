// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2015 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Mock FtcRobotController component.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public final class MockFtcRobotController extends MockVisibleComponent {

  /**
   * Component type name.
   */
  public static final String TYPE = "FtcRobotController";

  private static final int WIDTH = 320 - MockComponent.BORDER_SIZE;
  private static final int HEIGHT = 470 - MockComponent.BORDER_SIZE;

  private static final String PROPERTY_NAME_CONFIGURATION = "Configuration";

  private static final String STYLE_BORDER = "border";
  private static final String STYLE_BACKGROUND_COLOR = "backgroundColor";
  private static final String STYLE_TEXT_COLOR = "color";
  private static final String STYLE_TEXT_ALIGN = "textAlign";
  private static final String STYLE_FONT_SIZE = "fontSize";

  private static final String COLOR_TRANSPARENT= "transparent";
  private static final String COLOR_BLACK = "#000000";
  private static final String COLOR_WHITE = "#ffffff";
  private static final String COLOR_MEDIUM_RED = "#4e0106";

  private static final String TEXT_ALIGN_RIGHT = "right";

  private static final String LABEL_DEVICE_NAME = "<Device>";
  private static final String LABEL_CONFIGURATION = "Active Configuration:";
  private static final String LABEL_NETWORK = "Network:";
  private static final String LABEL_ROBOT_STATUS = "Robot Status:";
  private static final String LABEL_OP_MODE = "Op Mode:";
  private static final String LABEL_GAMEPAD_1 = "Gamepad 1";
  private static final String LABEL_GAMEPAD_2 = "Gamepad 2";

  private final VerticalPanel verticalPanel;
  private final Label configuration;

  /**
   * Creates a new MockFtcRobotController component.
   *
   * @param editor  editor of source file the component belongs to
   */
  public MockFtcRobotController(SimpleEditor editor) {
    super(editor, TYPE, images.ftcRobotController());

    // Initialize mock FtcRobotController UI
    verticalPanel = new VerticalPanel();
    verticalPanel.setStylePrimaryName("ode-SimpleMockComponent");
    verticalPanel.setSize("100%", "100%");

    VerticalPanel topSection = new VerticalPanel();
    // Black bar on top
    HorizontalPanel topBar = new HorizontalPanel();
    DOM.setStyleAttribute(topBar.getElement(), STYLE_BACKGROUND_COLOR, COLOR_BLACK);
    topBar.setSize("100%", "80px");
    Image robotIcon = new Image(images.robotControllerIcon());
    DOM.setStyleAttribute(robotIcon.getElement(), STYLE_BORDER, "5px solid" + COLOR_BLACK);
    robotIcon.setSize("40px", "70px");
    topBar.add(robotIcon);
    Label label = new Label(LABEL_DEVICE_NAME);
    DOM.setStyleAttribute(label.getElement(), STYLE_FONT_SIZE, "16px");
    DOM.setStyleAttribute(label.getElement(), STYLE_TEXT_COLOR, COLOR_WHITE);
    int labelWidth = WIDTH
        - 40  // width of robot icon
        - 10  // border around robot icon
        - 8   // width of menu dots
        - 30; // border around of menu dots
    label.setSize(labelWidth + "px", "100%");
    topBar.add(label);
    Image menuDots = new Image(images.robotControllerMenu());
    DOM.setStyleAttribute(menuDots.getElement(), STYLE_BORDER, "15px solid" + COLOR_BLACK);
    menuDots.setSize("8px", "50px");
    topBar.add(menuDots);
    topSection.add(topBar);

    HorizontalPanel header = new HorizontalPanel();
    DOM.setStyleAttribute(header.getElement(), STYLE_BACKGROUND_COLOR, COLOR_MEDIUM_RED);
    header.setSize("100%", "100%");
    label = new Label(LABEL_CONFIGURATION);
    DOM.setStyleAttribute(label.getElement(), STYLE_FONT_SIZE, "15px");
    DOM.setStyleAttribute(label.getElement(), STYLE_TEXT_COLOR, COLOR_WHITE);
    header.add(label);
    configuration = new Label();
    DOM.setStyleAttribute(configuration.getElement(), STYLE_BORDER, "3px solid transparent");
    DOM.setStyleAttribute(configuration.getElement(), STYLE_TEXT_COLOR, COLOR_WHITE);
    DOM.setStyleAttribute(configuration.getElement(), STYLE_TEXT_ALIGN, TEXT_ALIGN_RIGHT);
    header.add(configuration);
    topSection.add(header);

    topSection.add(new Label(LABEL_NETWORK));
    topSection.add(new Label(LABEL_ROBOT_STATUS));
    topSection.add(new Label(LABEL_OP_MODE));

    Label spacer = new Label(" ");

    VerticalPanel bottomSection = new VerticalPanel();
    bottomSection.add(new Label(LABEL_GAMEPAD_1));
    bottomSection.add(new Label(LABEL_GAMEPAD_2));

    int topSectionHeight = MockComponentsUtil.getPreferredSizeOfDetachedWidget(topSection)[1];
    int bottomSectionHeight = MockComponentsUtil.getPreferredSizeOfDetachedWidget(bottomSection)[1];
    int spacerHeight = HEIGHT - topSectionHeight - bottomSectionHeight;
    spacer.setSize("100%", spacerHeight + "px");
    verticalPanel.add(topSection);
    verticalPanel.add(spacer);
    verticalPanel.add(bottomSection);

    initComponent(verticalPanel);
  }

  // Don't show some of the properties.
  @Override
  protected boolean isPropertyVisible(String propertyName){
    if (propertyName.equals(PROPERTY_NAME_WIDTH) ||
        propertyName.equals(PROPERTY_NAME_HEIGHT) ||
        propertyName.equals(PROPERTY_NAME_VISIBLE)) {
      return false;
    }
    return super.isPropertyVisible(propertyName);
  }

  // Override the width and height hints, so that automatic will in fact be fill-parent
  @Override
  int getWidthHint() {
    return WIDTH;
  }

  @Override int getHeightHint() {
    return HEIGHT;
  }

  // PropertyChangeListener implementation

  @Override
  public void onPropertyChange(String propertyName, String newValue) {
    super.onPropertyChange(propertyName, newValue);

    if (propertyName.equals(PROPERTY_NAME_CONFIGURATION)) {
      configuration.setText(newValue);
      refreshForm();
    }
  }
}
