package widgets;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The hints of the keys and the commands of the program: they are hidden, so the screens, and the battles above all, are clean,
 * and show only while Alt is held. One state for the whole program: the first scene a hint is put in is watched for the key, and
 * Alt released in another window, which never arrives here, is told by the focus.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class KeyHints {

	private static final String WATCHED = "widgets.KeyHints.watched";
	private static final BooleanProperty HELD = new SimpleBooleanProperty(false);
	private static final EventHandler<KeyEvent> ALT_WATCHER = event -> {
		if (event.getEventType() == KeyEvent.KEY_PRESSED || event.getEventType() == KeyEvent.KEY_RELEASED) {
			HELD.set(event.isAltDown());
		}
	};

	/** Only static methods: this class is not instantiated. */
	private KeyHints() {
	}

	/** Whether Alt is held: the hints are shown. */
	public static boolean shown() {
		return HELD.get();
	}

	/** The state, to listen to it. */
	public static BooleanProperty heldProperty() {
		return HELD;
	}

	/** Makes the node a hint of keys: it shows only while Alt is held. */
	public static <T extends Node> T only(T hint) {
		hint.visibleProperty().bind(HELD);
		watch(hint.getScene(), hint);
		hint.sceneProperty().addListener((observable, was, scene) -> watch(scene, hint));
		return hint;
	}

	/** Watches the scene for Alt, once, and its window for the focus it loses. */
	private static void watch(Scene scene, Node hint) {
		if (scene == null || scene.getProperties().putIfAbsent(WATCHED, Boolean.TRUE) != null) {
			return;
		}
		scene.addEventFilter(KeyEvent.ANY, ALT_WATCHER);
		scene.windowProperty().addListener((observable, was, window) -> hold(window));
		hold(scene.getWindow());
	}

	private static void hold(javafx.stage.Window window) {
		if (window != null) {
			window.focusedProperty().addListener((observable, was, focused) -> HELD.set(false));
		}
	}
}
