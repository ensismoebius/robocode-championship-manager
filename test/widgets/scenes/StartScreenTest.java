package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import support.FxSupport;
import support.TestConfiguration;
import utils.configuration.Configuration;

class StartScreenTest {

	private static StartScreen screen;

	@BeforeAll
	static void openTheScreen() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		FxSupport.gameWindow();
		screen = FxSupport.run(() -> new StartScreen(0));
	}

	/** The first node of the tree that matches, or null. */
	private static Node find(Node root, Predicate<Node> match) {
		if (match.test(root)) {
			return root;
		}
		if (root instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				Node found = find(child, match);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/** The label of the registration status, the one with a square or a circle in front of its text. */
	private static Label badge() {
		return (Label) FxSupport.run(() -> find(screen, node -> node instanceof Label label && label.getText() != null && label.getText().matches("[■●].*")));
	}

	/** The address shown to the teams, when the registration is open. */
	private static Label address() {
		return (Label) FxSupport.run(() -> find(screen, node -> node instanceof Label label && label.getStyleClass().contains("address")));
	}

	/** The panel of the controls, hidden until Alt is held. */
	private static Node controls() {
		return FxSupport.run(() -> find(screen, node -> node.getStyleClass().contains("panel")));
	}

	@Test
	void withoutTheServerTheBadgeSaysSo() {
		assertEquals("■ SERVIDOR DE INSCRIÇÕES PARADO", badge().getText());
		assertTrue(badge().getStyleClass().contains("badge-closed"));
	}

	@Test
	void withTheServerAndTheRegistrationOpenTheAddressIsShown() {
		Configuration.setEnabledRegistration(true);
		FxSupport.runVoid(() -> screen.refresh(8080));

		assertEquals("● INSCRIÇÕES ABERTAS", badge().getText());
		assertTrue(badge().getStyleClass().contains("badge-open"));
		assertTrue(address().getText().endsWith(":8080"), address().getText());
	}

	@Test
	void withTheRegistrationClosedTheBadgeSaysSo() {
		Configuration.setEnabledRegistration(false);
		FxSupport.runVoid(() -> screen.refresh(8080));

		assertEquals("■ INSCRIÇÕES ENCERRADAS", badge().getText());
		assertTrue(badge().getStyleClass().contains("badge-closed"));
	}

	@Test
	void holdingAltShowsTheControlsAndReleasingHidesThem() {
		Stage stage = FxSupport.show(screen);
		assertFalse(controls().isVisible());

		FxSupport.runVoid(() -> screen.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ALT, false, false, true, false)));
		FxSupport.sleep(300);
		assertTrue(controls().isVisible());

		FxSupport.runVoid(() -> screen.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.ALT, false, false, false, false)));
		FxSupport.sleep(300);
		FxSupport.close(stage);

		assertFalse(controls().isVisible());
	}

	@Test
	void theScreenKeepsWatchingTheKeysWhenItIsShownAgain() {
		Stage first = FxSupport.show(screen);
		FxSupport.close(first);
		Stage second = FxSupport.show(screen);
		FxSupport.runVoid(() -> screen.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ALT, false, false, true, false)));
		FxSupport.sleep(300);
		FxSupport.close(second);

		assertNotNull(screen.getScene());
		assertTrue(controls().isVisible());
	}
}
