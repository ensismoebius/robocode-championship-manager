package widgets;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Dialogs of the program, shown as game menus inside the full screen window
 * (see {@link GameWindow})
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class GameDialog {

	private GameDialog() {
	}

	/**
	 * A panel with a title, the content and the buttons at the bottom
	 */
	public static VBox panel(String title, Node content, Button... buttons) {
		Label titleLabel = new Label(title.toUpperCase());
		titleLabel.getStyleClass().add("game-dialog-title");

		HBox buttonBar = new HBox(12, buttons);
		buttonBar.setAlignment(Pos.CENTER_RIGHT);

		VBox panel = new VBox(18, titleLabel, content, buttonBar);
		panel.getStyleClass().add("game-dialog");
		return panel;
	}

	private static Label message(String text) {
		Label message = new Label(text);
		message.setWrapText(true);
		message.getStyleClass().add("game-dialog-message");
		return message;
	}

	public static boolean confirm(String title, String message) {
		return GameDialog.confirm(title, GameDialog.message(message));
	}

	/**
	 * @return true if confirmed (OK or Enter), false if cancelled (Cancelar or
	 *         Escape)
	 */
	public static boolean confirm(String title, Node content) {
		Button ok = new Button("OK");
		ok.getStyleClass().add("primary");
		Button cancel = new Button("Cancelar");
		VBox panel = GameDialog.panel(title, content, cancel, ok);
		ok.setOnAction(event -> GameWindow.close(panel, Boolean.TRUE));
		cancel.setOnAction(event -> GameWindow.close(panel, Boolean.FALSE));
		Sfx.blip();
		return GameWindow.showModal(panel, ok, cancel) == Boolean.TRUE;
	}

	public static void error(String message) {
		Sfx.error();
		GameDialog.inform("Erro", message, "error");
	}

	public static void info(String title, String message) {
		Sfx.blip();
		GameDialog.inform(title, message, null);
	}

	private static void inform(String title, String message, String styleClass) {
		Button ok = new Button("OK");
		ok.getStyleClass().add("primary");
		VBox panel = GameDialog.panel(title, GameDialog.message(message), ok);
		if (styleClass != null) {
			panel.getStyleClass().add(styleClass);
		}
		ok.setOnAction(event -> GameWindow.close(panel, null));
		GameWindow.showModal(panel, ok, ok);
	}

	/**
	 * A message that shows for a while at the top of the screen, without
	 * stopping anything
	 */
	public static void toast(String message) {
		GameDialog.toast(message, true);
	}

	/**
	 * @param good
	 *            green with a happy sound, otherwise red
	 */
	public static void toast(String message, boolean good) {
		Label label = new Label(message);
		label.getStyleClass().add("game-toast");
		if (!good) {
			label.getStyleClass().add("bad");
		}
		// At the top, where it does not cover what is on the screen
		StackPane.setAlignment(label, Pos.TOP_CENTER);
		GameWindow.showNotice(label);
		if (good) {
			Sfx.confirm();
		} else {
			Sfx.blip();
		}

		TranslateTransition in = new TranslateTransition(Duration.millis(200), label);
		in.setFromY(-60);
		in.setToY(84);
		FadeTransition out = new FadeTransition(Duration.millis(400), label);
		out.setToValue(0);
		SequentialTransition life = new SequentialTransition(in, new PauseTransition(Duration.seconds(2.5)), out);
		life.setOnFinished(event -> GameWindow.removeNotice(label));
		life.play();
	}
}
