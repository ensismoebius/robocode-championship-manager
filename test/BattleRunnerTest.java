import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static support.DialogFields.button;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
import widgets.scenes.ArenaSimulation;
import widgets.scenes.BattleArena3D;
import widgets.scenes.BattleViewer;
import widgets.scenes.ShowOponent;

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

	/** Points the configuration to a Robocode folder with its robots. */
	private static void useRobocodeFolder() throws IOException {
		Path folder = Path.of("target", "test-home", "robocode", "robots");
		Files.createDirectories(folder);
		TestConfiguration.load(Configuration.RANDOM, folder.getParent().toString());
	}

	/** Waits until the battle in the background is over and no dialog is open, for up to the time given. */
	private static void waitUntilTheBattleEnds(long millis) {
		for (long waited = 0; waited < millis && (FxSupport.run(() -> host.battleRunning) || FxSupport.dialogShowing()); waited += 100) {
			FxSupport.sleep(100);
		}
	}

	@Test
	void aBattleIsNotStartedWhileAnotherRuns() {
		host.battleRunning = true;

		FxSupport.runVoid(runner::startsBattle);

		assertTrue(FxSupport.run(() -> host.battleRunning));
		host.battleRunning = false;
	}

	@Test
	void aBattleNeedsAPlayableMatch() throws IOException {
		useRobocodeFolder();
		host.selectedMatch = null;
		FxSupport.onNextDialog(dialog -> button(dialog, "OK").fire());

		FxSupport.runVoid(runner::startsBattle);

		assertFalse(FxSupport.run(() -> host.battleRunning));
		host.selectedMatch = match.getCode();
	}

	/** Draws a championship of four new robots and returns its next match, which is ready to be played. */
	private static Match drawChampionship() throws IOException {
		ChampionshipFixture.registerRobots(4);
		FxSupport.onNextDialog(dialog -> button(dialog, "OK").fire());
		FxSupport.runVoid(host::newChampionship);
		return FxSupport.run(() -> host.bracket.getNextMatch().orElseThrow());
	}

	/** The robots that fight in the match, as the championship has them. */
	private static List<Opponent> fightersOf(Match playing) {
		return FxSupport.run(() -> List.of(host.opponents.get(playing.getA().getParticipant()), host.opponents.get(playing.getB().getParticipant())));
	}

	@Test
	void aBattleThatFailsOnRobocodeIsReportedToTheOperator() throws IOException {
		useRobocodeFolder();
		host.selectedMatch = FxSupport.run(() -> host.bracket.getNextMatch().orElseThrow().getCode());
		AtomicBoolean reported = new AtomicBoolean();
		FxSupport.onNextDialog(dialog -> {
			reported.set(true);
			button(dialog, "OK").fire();
		});

		FxSupport.runVoid(runner::startsBattle);
		waitUntilTheBattleEnds(30000);

		assertTrue(reported.get());
		assertFalse(FxSupport.run(() -> host.battleRunning));
	}

	@Test
	void aBattleWithoutTheArenaShowsItsResultAtOnce() throws IOException {
		Match next = drawChampionship();
		List<Opponent> players = fightersOf(next);
		BattleOutcome outcome = new BattleOutcome(List.of(new BattleResult(players.get(0), 1, 100, 2), new BattleResult(players.get(1), 2, 40, 1)), BattleRecording.parse(List.of()));
		host.battleRunning = true;

		FxSupport.runVoid(() -> runner.onBattleSucceeded(outcome, next, players, null));

		assertEquals(1, FxSupport.run(() -> host.championship.getResults().size()));
		assertFalse(FxSupport.run(() -> host.battleRunning));
		assertInstanceOf(ShowOponent.class, FxSupport.run(() -> GameWindow.getScreen()));
	}

	@Test
	void aBattleOfTheArenaIsSavedAndItsResultComesWhenTheReplayEnds() throws IOException {
		Match next = drawChampionship();
		List<Opponent> players = fightersOf(next);
		BattleArena3D arena = FxSupport.run(() -> runner.openArena(next, players, true));
		BattleOutcome outcome = new BattleOutcome(List.of(new BattleResult(players.get(0), 1, 100, 2), new BattleResult(players.get(1), 2, 40, 1)), ArenaSimulation.recording(ArenaSimulation.OUTPUT));
		host.battleRunning = true;

		FxSupport.runVoid(() -> runner.onBattleSucceeded(outcome, next, players, arena));

		// The result is saved at once and shown when the replay of the arena ends
		assertEquals(1, FxSupport.run(() -> host.championship.getResults().size()));
		for (long waited = 0; waited < 90000 && !(FxSupport.run(() -> GameWindow.getScreen()) instanceof ShowOponent); waited += 100) {
			FxSupport.sleep(100);
		}
		assertInstanceOf(ShowOponent.class, FxSupport.run(() -> GameWindow.getScreen()));
		assertFalse(FxSupport.run(() -> host.battleRunning));
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
