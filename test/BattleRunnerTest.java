import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Opponent;
import tournament.Bracket.Match;
import support.ChampionshipFixture;
import support.FxSupport;
import support.TestConfiguration;
import utils.database.Database;
import utils.configuration.Configuration;
import utils.robocode.BattleControl.BattleOutcome;
import utils.robocode.BattleControl.BattleResult;
import utils.robocode.BattleRecording;
import widgets.GameWindow;
import widgets.scenes.BattleArena3D;
import widgets.scenes.BattleViewer;

class BattleRunnerTest {

	private static Main host;
	private static BattleRunner runner;
	private static List<Opponent> fighters;
	private static Match match;

	@BeforeAll
	static void openTheProgram() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
		FxSupport.gameWindow();
		host = FxSupport.run(Main::new);
		runner = new BattleRunner(host);

		// A bracket of four robots, the first match is ready to be played
		fighters = ChampionshipFixture.opponents(4);
		host.opponents = ChampionshipFixture.byId(fighters);
		host.bracket = ChampionshipFixture.fresh(fighters, false);
		match = host.bracket.getMatches().get(0);
		host.selectedMatch = match.getCode();
	}

	/** Closes the database, so the next test class opens its own. */
	@AfterAll
	static void closeTheDatabase() {
		Database.close();
	}

	@Test
	void theArenaIsOnlyShownWhenTheBattleIsViewedIn3D() {
		assertNull(FxSupport.run(() -> runner.openArena(match, fighters, false)));
	}

	@Test
	void theArenaOfTheMatchIsShownIn3D() {
		BattleArena3D arena = FxSupport.run(() -> runner.openArena(match, fighters, true));

		assertSame(arena, FxSupport.run(() -> GameWindow.getScreen()));
	}

	@Test
	void theRobocodeFolderIsFoundWhenItHasRobots() throws IOException {
		Path folder = Path.of("target", "test-home", "robocode", "robots");
		Files.createDirectories(folder);
		TestConfiguration.load(Configuration.RANDOM, folder.getParent().toString());

		assertTrue(FxSupport.run(() -> runner.chooseRobocodeDir()));
	}

	@Test
	void theRobocodeFolderIsAskedForWhenItIsMissing() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");

		assertFalse(FxSupport.run(() -> runner.chooseRobocodeDir()));
	}

	@Test
	void aFailedBattleTellsTheOperator() {
		host.battleRunning = true;
		FxSupport.answerNextDialog(null);
		FxSupport.runVoid(() -> runner.onBattleFailed(new IllegalStateException("sem robocode"), null));

		assertFalse(host.battleRunning);
	}

	@Test
	void aFailedBattleInTheArenaGoesBackToTheBattleScreen() {
		BattleArena3D arena = FxSupport.run(() -> runner.openArena(match, fighters, true));
		host.battleRunning = true;
		FxSupport.answerNextDialog(null);
		FxSupport.runVoid(() -> runner.onBattleFailed(new IllegalStateException("sem robocode"), arena));

		assertFalse(host.battleRunning);
		assertInstanceOf(BattleViewer.class, FxSupport.run(() -> GameWindow.getScreen()));
	}

	@Test
	void aBattleThatCannotBeSavedIsNotShown() {
		BattleResult first = new BattleResult(fighters.get(0), 1, 100, 2);
		BattleResult second = new BattleResult(fighters.get(1), 2, 40, 1);
		BattleOutcome outcome = new BattleOutcome(List.of(first, second), BattleRecording.parse(List.of()));
		host.battleRunning = true;
		FxSupport.answerNextDialog(null);
		FxSupport.runVoid(() -> runner.onBattleSucceeded(outcome, match, fighters, null));

		assertFalse(host.battleRunning);
	}
}
