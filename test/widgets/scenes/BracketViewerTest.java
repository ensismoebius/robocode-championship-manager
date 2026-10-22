package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Opponent;
import support.ChampionshipFixture;
import support.FxSupport;
import tournament.Bracket;
import tournament.Match;

class BracketViewerTest {

	@Test
	void unfinishedBracketCanBeNavigatedAndPlayed() {
		List<Opponent> opponents = ChampionshipFixture.opponents(8);
		Bracket bracket = ChampionshipFixture.fresh(opponents, false);
		List<String> chosen = new ArrayList<String>();
		BracketViewer viewer = FxSupport.run(() -> new BracketViewer("Campeonato", bracket, ChampionshipFixture.byId(opponents), "V1-1", chosen::add, 2));

		Stage window = FxSupport.show(viewer);
		FxSupport.runVoid(() -> {
			viewer.select("V1-2");
			viewer.showNextTag();
			viewer.focusSelected();
			assertNotNull(viewer.overview());
			assertNotNull(viewer.centerOf("V1-1"));
			viewer.moveTo(100, 100, 1.5, Duration.millis(200));
			viewer.playResult("V1-1");
		});
		FxSupport.sleep(600);
		FxSupport.close(window);

		// The selection is kept by the viewer: the card of V1-2 is the one highlighted
		FxSupport.runVoid(() -> {
			VBox selected = viewer.buildCard(bracket.getMatch("V1-2"));
			viewer.styleSelected(selected, true);
			assertTrue(selected.getStyleClass().contains("selected"));
		});
		assertTrue(chosen.isEmpty());
	}

	@Test
	void finishedBracketWithRepechageShowsTheStandings() {
		List<Opponent> opponents = ChampionshipFixture.opponents(8);
		Bracket bracket = ChampionshipFixture.finished(opponents, true);
		BracketViewer viewer = FxSupport.run(() -> new BracketViewer("Campeonato", bracket, ChampionshipFixture.byId(opponents), null, code -> {
		}, 0));

		Stage window = FxSupport.show(viewer);
		FxSupport.runVoid(() -> {
			Node standings = viewer.buildStandings();
			assertNotNull(standings);
			viewer.select(bracket.getFinals().get(0).getCode());
			viewer.playResult(bracket.getFinals().get(0).getCode());
		});
		FxSupport.sleep(600);
		FxSupport.close(window);
	}

	@Test
	void everyCardShowsItsMatch() {
		List<Opponent> opponents = ChampionshipFixture.opponents(4);
		Bracket bracket = ChampionshipFixture.finished(opponents, false);
		BracketViewer viewer = FxSupport.run(() -> new BracketViewer("Campeonato", bracket, ChampionshipFixture.byId(opponents), null, code -> {
		}, 0));

		FxSupport.runVoid(() -> {
			for (Match match : bracket.getMatches()) {
				VBox card = viewer.buildCard(match);
				assertNotNull(card);
				viewer.styleSelected(card, true);
				assertTrue(card.getStyleClass().contains("selected"), match.getCode());
				viewer.styleSelected(card, false);
				assertFalse(card.getStyleClass().contains("selected"), match.getCode());
			}
		});
	}
}
