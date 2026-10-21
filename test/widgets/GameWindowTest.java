package widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import support.FxSupport;

class GameWindowTest {

	@BeforeAll
	static void openTheWindow() {
		FxSupport.gameWindow();
	}

	/** The opacity of the curtain that covers the window, which the window keeps private. */
	private static double curtainOpacity() throws ReflectiveOperationException {
		Field curtain = GameWindow.class.getDeclaredField("curtain");
		curtain.setAccessible(true);
		return ((Region) curtain.get(null)).getOpacity();
	}

	/** A panel with an OK and a cancel button, which close it with their answers. */
	private static VBox panel() {
		VBox panel = new VBox();
		Button ok = new Button("OK");
		ok.setOnAction(event -> GameWindow.close(panel, "ok"));
		Button cancel = new Button("Cancelar");
		cancel.setOnAction(event -> GameWindow.close(panel, "cancelado"));
		panel.getChildren().setAll(ok, cancel);
		return panel;
	}

	/** Shows the panel above the screen and waits for its answer, on the JavaFX thread. */
	private static Object modal() {
		VBox panel = panel();
		return GameWindow.showModal(panel, (Button) panel.getChildren().get(0), (Button) panel.getChildren().get(1));
	}

	private static KeyEvent key(KeyCode code) {
		return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
	}

	@Test
	void theCurtainFadesAndTellsWhenItIsDone() throws ReflectiveOperationException {
		AtomicBoolean done = new AtomicBoolean();
		FxSupport.runVoid(() -> GameWindow.fadeCurtain(true, 0.2, () -> done.set(true)));
		FxSupport.sleep(600);

		assertTrue(done.get());
		assertEquals(1, curtainOpacity(), 1e-9);

		FxSupport.runVoid(() -> GameWindow.fadeCurtain(false, 0.2, null));
		FxSupport.sleep(600);
		assertEquals(0, curtainOpacity(), 1e-9);
	}

	@Test
	void theCurtainCanBeSetAtOnce() throws ReflectiveOperationException {
		FxSupport.runVoid(() -> GameWindow.setCurtain(true));
		assertEquals(1, curtainOpacity(), 1e-9);

		FxSupport.runVoid(() -> GameWindow.setCurtain(false));
		assertEquals(0, curtainOpacity(), 1e-9);
	}

	@Test
	void enterFiresTheDefaultButtonOfTheDialog() {
		FxSupport.onNextDialog(dialog -> dialog.fireEvent(key(KeyCode.ENTER)));

		assertEquals("ok", FxSupport.run(GameWindowTest::modal));
	}

	@Test
	void escapeFiresTheCancelButtonOfTheDialog() {
		FxSupport.onNextDialog(dialog -> dialog.fireEvent(key(KeyCode.ESCAPE)));

		assertEquals("cancelado", FxSupport.run(GameWindowTest::modal));
	}

	@Test
	void keysOutsideTheDialogDoNotReachTheScreen() {
		AtomicBoolean reachedScreen = new AtomicBoolean();
		AtomicBoolean reachedDialog = new AtomicBoolean();
		StackPane screen = new StackPane();
		FxSupport.runVoid(() -> GameWindow.setScreen(screen));
		screen.addEventHandler(KeyEvent.KEY_PRESSED, event -> reachedScreen.set(true));

		FxSupport.onNextDialog(dialog -> {
			dialog.addEventHandler(KeyEvent.KEY_PRESSED, event -> reachedDialog.set(true));
			screen.fireEvent(key(KeyCode.A));
			dialog.fireEvent(key(KeyCode.A));
			((Button) dialog.getChildrenUnmodifiable().get(0)).fire();
		});
		FxSupport.run(GameWindowTest::modal);

		assertFalse(reachedScreen.get());
		assertTrue(reachedDialog.get());
	}

	@Test
	void closingAPanelThatIsNotOpenDoesNothing() {
		FxSupport.runVoid(() -> GameWindow.close(new VBox(), "nada"));
	}
}
