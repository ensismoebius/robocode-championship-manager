package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;
import model.Opponent;
import support.ChampionshipFixture;
import support.FxSupport;
import tournament.Bracket;

/** The replay of a result on the bracket: the robots move to their next matches, unless a key ends it. */
class BracketReplayTest {

	/** The nodes of the tree that match. */
	private static int count(Node root, Predicate<Node> match) {
		int total = match.test(root) ? 1 : 0;
		if (root instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				total += count(child, match);
			}
		}
		return total;
	}

	/** The connector being drawn by the replay, which is removed when the replay ends. */
	private static int drawings(BracketViewer viewer) {
		return FxSupport.run(() -> count(viewer, node -> node instanceof Polyline line && line.getStyleClass().contains("drawing")));
	}

	/** Waits, up to the time given, until the connector is being drawn, or until it is gone when the drawing is false. */
	private static boolean waitFor(BracketViewer viewer, boolean drawing, long millis) {
		for (long waited = 0; waited < millis; waited += 100) {
			if ((drawings(viewer) > 0) == drawing) {
				return true;
			}
			FxSupport.sleep(100);
		}
		return false;
	}

	private static BracketViewer viewer(Bracket bracket, List<Opponent> opponents) {
		return FxSupport.run(() -> new BracketViewer("Campeonato", bracket, ChampionshipFixture.byId(opponents), null, code -> {
		}, 0));
	}

	@Test
	void theResultOfAMatchMovesTheWinnerToTheNextMatchAndEndsWithIt() {
		List<Opponent> opponents = ChampionshipFixture.opponents(8);
		Bracket bracket = ChampionshipFixture.finished(opponents, false);
		BracketViewer viewer = viewer(bracket, opponents);
		Stage window = FxSupport.show(viewer);

		FxSupport.runVoid(() -> viewer.playResult(bracket.getMatches().get(0).getCode()));
		// The camera, the result and the robots take a few seconds to get there
		assertTrue(waitFor(viewer, true, 8000), "the winner is moving to the next match");
		assertTrue(waitFor(viewer, false, 8000), "the replay ends by itself");
		FxSupport.close(window);
	}

	@Test
	void aKeyEndsTheReplayAtOnce() {
		List<Opponent> opponents = ChampionshipFixture.opponents(8);
		Bracket bracket = ChampionshipFixture.finished(opponents, false);
		BracketViewer viewer = viewer(bracket, opponents);
		Stage window = FxSupport.show(viewer);

		FxSupport.runVoid(() -> viewer.playResult(bracket.getMatches().get(0).getCode()));
		assertTrue(waitFor(viewer, true, 8000), "the winner is moving to the next match");
		FxSupport.runVoid(() -> viewer.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE, false, false, false, false)));
		FxSupport.sleep(300);
		FxSupport.close(window);

		assertEquals(0, drawings(viewer));
	}
}
