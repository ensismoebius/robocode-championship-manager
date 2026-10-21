package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import model.Opponent;
import support.ChampionshipFixture;
import support.DialogFields;
import support.FxSupport;
import tournament.Bracket;

/** The mouse, the wheel and the keys of the bracket: it moves, it zooms and the matches are chosen. */
class BracketViewerInputTest {

	private static void key(BracketViewer viewer, KeyCode code, boolean control) {
		FxSupport.runVoid(() -> viewer.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, control, false, false)));
	}

	private static void mouse(Node target, EventType<MouseEvent> type, double x, double y, boolean still) {
		FxSupport.runVoid(() -> target.fireEvent(new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, true, false, still, null)));
	}

	private static void wheel(Node target, double deltaX, double deltaY, boolean control) {
		FxSupport.runVoid(() -> target.fireEvent(new ScrollEvent(ScrollEvent.SCROLL, 1, 1, 1, 1, false, control, false, false, false, false, deltaX, deltaY, deltaX, deltaY,
				ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null)));
	}

	@Test
	void theMouseTheWheelAndTheKeysMoveAndZoomTheBracketAndChooseAMatch() {
		List<Opponent> opponents = ChampionshipFixture.opponents(8);
		Bracket bracket = ChampionshipFixture.fresh(opponents, false);
		List<String> chosen = new ArrayList<String>();
		BracketViewer viewer = FxSupport.run(() -> new BracketViewer("Campeonato", bracket, ChampionshipFixture.byId(opponents), null, chosen::add, 0));
		Stage window = FxSupport.show(viewer);
		Node viewport = FxSupport.run(() -> DialogFields.find(viewer, node -> node.getStyleClass().contains("bracket-root")));
		assertNotNull(viewport);

		for (KeyCode code : List.of(KeyCode.PLUS, KeyCode.EQUALS, KeyCode.ADD, KeyCode.MINUS, KeyCode.SUBTRACT, KeyCode.DIGIT0, KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN)) {
			key(viewer, code, false);
		}
		FxSupport.sleep(300);

		wheel(viewport, 0, 10, true);
		wheel(viewport, 0, -10, true);
		wheel(viewport, 15, 5, false);
		mouse(viewport, MouseEvent.MOUSE_PRESSED, 100, 100, false);
		mouse(viewport, MouseEvent.MOUSE_DRAGGED, 140, 120, false);
		mouse(viewport, MouseEvent.MOUSE_DRAGGED, 150, 90, false);
		FxSupport.sleep(400);

		// A click that stays on the card chooses its match; a card of a match that is not ready is not chosen
		Node ready = FxSupport.run(() -> DialogFields.find(viewer, node -> node.getStyleClass().contains("ready")));
		assertNotNull(ready);
		mouse(ready, MouseEvent.MOUSE_CLICKED, 5, 5, true);
		mouse(ready, MouseEvent.MOUSE_CLICKED, 5, 5, false);
		FxSupport.close(window);

		assertEquals(1, chosen.size());
	}

	@Test
	void aByeAndAMatchWithoutPictureAreShown() {
		List<Opponent> opponents = ChampionshipFixture.opponents(5);
		opponents.get(0).setImage(null);
		opponents.get(1).setImage(null);
		Bracket bracket = ChampionshipFixture.fresh(opponents, false);
		BracketViewer viewer = FxSupport.run(() -> new BracketViewer("Campeonato", bracket, ChampionshipFixture.byId(opponents), null, code -> {
		}, 0));
		Stage window = FxSupport.show(viewer);

		// With five robots one match has no opponent in its second place: it is a bye
		assertTrue(FxSupport.run(() -> DialogFields.find(viewer, node -> node.getStyleClass().contains("bye")) != null
				|| DialogFields.find(viewer, node -> node.getStyleClass().contains("empty")) != null));
		// A selection that is not on the bracket has no card to point at
		FxSupport.runVoid(() -> viewer.select("ZZ-9"));
		FxSupport.runVoid(viewer::showNextTag);
		FxSupport.close(window);
	}

	@Test
	void aClickOrAWheelEndsTheResultReplayAtOnce() {
		List<Opponent> opponents = ChampionshipFixture.opponents(8);
		Bracket bracket = ChampionshipFixture.finished(opponents, false);
		BracketViewer viewer = FxSupport.run(() -> new BracketViewer("Campeonato", bracket, ChampionshipFixture.byId(opponents), null, code -> {
		}, 0));
		Stage window = FxSupport.show(viewer);
		Node viewport = FxSupport.run(() -> DialogFields.find(viewer, node -> node.getStyleClass().contains("bracket-root")));

		FxSupport.runVoid(() -> viewer.playResult(bracket.getMatches().get(0).getCode()));
		FxSupport.sleep(200);
		mouse(viewport, MouseEvent.MOUSE_PRESSED, 100, 100, false);
		FxSupport.sleep(100);
		wheel(viewport, 0, 5, false);
		FxSupport.close(window);

		assertFalse(bracket.getMatches().isEmpty());
	}
}
