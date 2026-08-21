package com.lilithsthrone.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Smoke test proving TestFX (JavaFX's robot-based UI testing framework) works in this environment
 * on JavaFX 25. If green, TestFX can drive real UI interactions (clicks/hover) for higher-level tests.
 */
@ExtendWith(ApplicationExtension.class)
class TestFxSmokeTest {

	private Button button;

	@Start
	private void start(Stage stage) {
		button = new Button("click");
		button.setId("smoke-button");
		button.setOnAction(e -> button.setText("clicked"));
		stage.setScene(new Scene(new StackPane(button), 200, 100));
		stage.show();
	}

	@Test
	void clickChangesButtonText(FxRobot robot) {
		robot.clickOn("#smoke-button");
		assertThat(button.getText()).isEqualTo("clicked");
	}
}
