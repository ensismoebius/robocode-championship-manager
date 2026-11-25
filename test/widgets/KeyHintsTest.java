package widgets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import support.FxSupport;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The hints of the keys show only while Alt is held.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
class KeyHintsTest {

	private static void key(Scene scene, javafx.event.EventType<KeyEvent> type, boolean alt) {
		FxSupport.runVoid(() -> scene.getRoot().fireEvent(new KeyEvent(type, "", "", KeyCode.ALT, false, false, alt, false)));
	}

	@Test
	void aHintShowsOnlyWhileAltIsHeld() {
		Label hint = FxSupport.run(() -> KeyHints.only(new Label("ESPAÇO: PAUSA")));
		Scene scene = FxSupport.run(() -> new Scene(new StackPane(hint), 200, 100));
		FxSupport.runVoid(() -> scene.getRoot().applyCss());
		// Other tests may have left Alt pressed: it is released first
		key(scene, KeyEvent.KEY_RELEASED, false);

		assertFalse(hint.isVisible(), "hidden by default");
		key(scene, KeyEvent.KEY_PRESSED, true);
		assertTrue(hint.isVisible(), "shown while Alt is held");
		key(scene, KeyEvent.KEY_RELEASED, false);
		assertFalse(hint.isVisible(), "hidden when Alt is released");
	}
}
