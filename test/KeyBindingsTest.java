import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import support.ChampionshipFixture;
import support.FxSupport;
import support.TestConfiguration;
import utils.database.Database;
import utils.configuration.Configuration;
import widgets.GameWindow;
import widgets.scenes.Podium;
import widgets.scenes.StartScreen;

class KeyBindingsTest {

	private static Main host;
	private static KeyBindings keys;

	@BeforeAll
	static void openTheProgram() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
		FxSupport.gameWindow();
		host = FxSupport.run(Main::new);
		keys = new KeyBindings(host);
	}

	/** Presses a key on the JavaFX thread, the dialogs it opens must be answered before. */
	private static void press(KeyCode code, boolean control) {
		FxSupport.runVoid(() -> keys.handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, control, false, false)));
	}

	private static void press(KeyCode code) {
		press(code, false);
	}

	/** Closes the database, so the next test class opens its own. */
	@AfterAll
	static void closeTheDatabase() {
		Database.close();
	}

	@Test
	void theInitialKeyShowsTheStartScreen() {
		press(KeyCode.I);

		assertTrue(FxSupport.run(() -> GameWindow.getScreen()) instanceof StartScreen);
	}

	@Test
	void theBattleBracketAndNextKeysNeedAChampionship() {
		host.selectedMatch = "V1-1";
		for (KeyCode code : List.of(KeyCode.B, KeyCode.M, KeyCode.R, KeyCode.N)) {
			FxSupport.answerNextDialog(null);
			press(code);
		}

		assertNull(host.selectedMatch);
		assertNull(host.bracket);
	}

	@Test
	void theWinnerKeysNeedAPlayableMatch() {
		for (KeyCode code : List.of(KeyCode.DIGIT1, KeyCode.NUMPAD2)) {
			FxSupport.answerNextDialog(null);
			press(code);
		}

		assertNull(host.bracket);
	}

	@Test
	void theNewChampionshipKeyNeedsRegisteredRobots() {
		FxSupport.answerNextDialog(null);
		press(KeyCode.G, true);

		assertNull(host.championship);
	}

	@Test
	void theUndoKeyWithoutResultsShowsTheError() {
		FxSupport.answerNextDialog(null);
		press(KeyCode.Z, true);

		assertNull(host.championship);
	}

	@Test
	void theRegistrationKeyTogglesTheRegistration() {
		boolean before = Configuration.isEnabledRegistration();

		press(KeyCode.S, true);
		assertEquals(!before, Configuration.isEnabledRegistration());
		press(KeyCode.S, true);
		assertEquals(before, Configuration.isEnabledRegistration());
	}

	@Test
	void theQuitKeyKeepsTheProgramWhenTheOperatorSaysNo() {
		FxSupport.answerNextDialog(Boolean.FALSE);
		press(KeyCode.Q, true);

		assertNotNull(GameWindow.getWindow());
	}

	@Test
	void theConfigurationKeyReviewsTheConfiguration() {
		press(KeyCode.C);

		assertFalse(host.battleRunning);
	}

	@Test
	void theBattleKeyStopsWithoutRobocodeFolder() {
		press(KeyCode.S);

		assertFalse(host.battleRunning);
	}

	@Test
	void theKeysWaitWhileTheChampionIsCrowned() {
		var screen = FxSupport.run(() -> GameWindow.getScreen());
		host.championTransition = true;
		try {
			press(KeyCode.I);
		} finally {
			host.championTransition = false;
		}

		assertSame(screen, FxSupport.run(() -> GameWindow.getScreen()));
	}

	@Test
	void thePodiumKeysGoToTheCeremonyAndTheOtherKeysStillWork() {
		var opponents = ChampionshipFixture.opponents(3);
		host.podiumScene = FxSupport.run(() -> new Podium(opponents, List.of(), ChampionshipFixture.byId(opponents)));
		FxSupport.runVoid(() -> GameWindow.setScreen(host.podiumScene));

		press(KeyCode.RIGHT);
		press(KeyCode.LEFT);
		press(KeyCode.SPACE);
		assertSame(host.podiumScene, FxSupport.run(() -> GameWindow.getScreen()));

		press(KeyCode.I);
		assertNull(host.podiumScene);
	}

	@Test
	void unknownKeysDoNothing() {
		var screen = FxSupport.run(() -> GameWindow.getScreen());

		press(KeyCode.F);
		press(KeyCode.A, true);

		assertSame(screen, FxSupport.run(() -> GameWindow.getScreen()));
	}
}
