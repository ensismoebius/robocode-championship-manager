import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static support.DialogFields.button;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import model.Opponent;
import support.ChampionshipFixture;
import support.DialogFields;
import support.FxSupport;
import support.TestConfiguration;
import utils.configuration.Configuration;
import utils.database.Database;
import widgets.GameWindow;
import widgets.scenes.BattleViewer;
import widgets.scenes.BracketViewer;
import widgets.scenes.Podium;
import widgets.scenes.ShowOponent;
import widgets.scenes.StartScreen;

/**
 * The program itself, started as the user starts it: the configuration is saved
 * in the first window, the championship is drawn and played with the keys, and
 * the champion is crowned. The steps run in order, each one on the state of the
 * one before.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MainTest {

	private static Main program;
	private static Path robocode;

	@BeforeAll
	static void startTheProgram() throws IOException {
		robocode = Path.of("target", "test-home", "robocode-main");
		Files.createDirectories(robocode.resolve("robots"));
		TestConfiguration.load(Configuration.RANDOM, robocode.toString());
		TestConfiguration.freshDatabase();
		int port;
		try (ServerSocket free = new ServerSocket(0)) {
			port = free.getLocalPort();
		}

		// The first window is the configuration, the values are the ones of the test, the operator saves them
		FxSupport.onNextDialog(dialog -> {
			DialogFields.text(dialog, "Porta do servidor web de cadastro", String.valueOf(port));
			button(dialog, "Salvar").fire();
		});
		program = FxSupport.run(Main::new);
		Stage stage = FxSupport.run(Stage::new);
		FxSupport.runVoid(() -> program.start(stage));
	}

	@AfterAll
	static void stopTheProgram() {
		FxSupport.runVoid(program::stop);
	}

	/** Waits until the condition holds, up to the time given. The condition runs on the test thread and goes to the JavaFX thread itself. */
	private static boolean waitUntil(BooleanSupplier condition, long millis) {
		for (long waited = 0; waited < millis; waited += 100) {
			if (condition.getAsBoolean()) {
				return true;
			}
			FxSupport.sleep(100);
		}
		return false;
	}

	/** Presses a key of the program: the key goes to the screen, the window hands it to the program. */
	private static void press(KeyCode code, boolean control) {
		FxSupport.runVoid(() -> GameWindow.getScreen().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, control, false, false)));
	}

	/** Presses the key of a dialog that is going to show: the dialog gets the answer of the button. */
	private static void pressAndAnswer(KeyCode code, boolean control, String answer) {
		FxSupport.onNextDialog(dialog -> button(dialog, answer).fire());
		press(code, control);
	}

	/** Registers the robots of the test in the database, with their pictures, music and source, as the registration does. */
	private static void registerRobots(int count) throws IOException {
		byte[] music = MainTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes();
		Database.inTransaction(entityManager -> {
			for (int index = 1; index <= count; index++) {
				Opponent opponent = new Opponent();
				opponent.setTeam("time" + index);
				opponent.setName("Robo" + index);
				opponent.setBattleCry("Grito " + index);
				opponent.setImage(ChampionshipFixture.logo());
				opponent.setMusic(music);
				opponent.setSourceCode("public class Robo" + index + " {}");
				entityManager.persist(opponent);
			}
			return null;
		});
	}

	@Test
	@Order(1)
	void theProgramOpensOnTheStartScreen() {
		assertInstanceOf(StartScreen.class, FxSupport.run(() -> GameWindow.getScreen()));
		assertEquals(robocode.toString(), Configuration.getRobocodePath());
	}

	@Test
	@Order(2)
	void theRegistrationKeyOpensAndClosesTheRegistration() {
		boolean before = Configuration.isEnabledRegistration();

		press(KeyCode.S, true);

		assertEquals(!before, Configuration.isEnabledRegistration());
		press(KeyCode.S, true);
		assertEquals(before, Configuration.isEnabledRegistration());
	}

	@Test
	@Order(3)
	void aChampionshipIsDrawnFromTheRegisteredRobots() throws IOException {
		registerRobots(4);

		pressAndAnswer(KeyCode.G, true, "OK");

		assertNotNull(program.championship);
		assertInstanceOf(BracketViewer.class, FxSupport.run(() -> GameWindow.getScreen()));
	}

	@Test
	@Order(4)
	void theNextMatchIsSelectedAndTheBattleStartsOnRobocode() {
		// The next key selects the next match of the bracket by itself
		press(KeyCode.N, false);
		assertEquals(program.bracket.getNextMatch().orElseThrow().getCode(), program.selectedMatch);

		// Robocode is not installed here: the battle fails and the operator is told
		FxSupport.onNextDialog(dialog -> button(dialog, "OK").fire());
		press(KeyCode.S, false);

		assertTrue(waitUntil(() -> !program.battleRunning && !FxSupport.dialogShowing(), 15000));
		assertFalse(program.battleRunning);
	}

	@Test
	@Order(5)
	void theWinnerIsDeclaredByHandAndTheRobotIsShown() {
		pressAndAnswer(KeyCode.DIGIT1, false, "OK");

		assertEquals(1, program.championship.getResults().size());
		assertInstanceOf(ShowOponent.class, FxSupport.run(() -> GameWindow.getScreen()));
	}

	@Test
	@Order(6)
	void theLastResultIsUndone() {
		pressAndAnswer(KeyCode.Z, true, "OK");

		assertTrue(program.championship.getResults().isEmpty());
	}

	@Test
	@Order(7)
	void theRobotsAndTheVersusScreenOfTheMatchAreShown() {
		press(KeyCode.M, false);
		assertInstanceOf(ShowOponent.class, FxSupport.run(() -> GameWindow.getScreen()));

		press(KeyCode.B, false);
		assertInstanceOf(BattleViewer.class, FxSupport.run(() -> GameWindow.getScreen()));
	}

	@Test
	@Order(8)
	void theRobotsAreExportedToFiles() {
		pressAndAnswer(KeyCode.X, false, "OK");

		assertTrue(Files.isDirectory(SourceExporter.defaultDir()));
	}

	@Test
	@Order(9)
	void theConfigurationIsReviewedButNotWhileABattleRuns() {
		program.battleRunning = true;
		pressAndAnswer(KeyCode.C, false, "OK");
		program.battleRunning = false;

		FxSupport.onNextDialog(dialog -> button(dialog, "Cancelar").fire());
		press(KeyCode.C, false);

		assertEquals(robocode.toString(), Configuration.getRobocodePath());
	}

	@Test
	@Order(10)
	void theChampionIsCrownedWhenTheBracketEnds() {
		while (!program.bracket.isFinished()) {
			program.selectedMatch = program.bracket.getNextMatch().orElseThrow().getCode();
			pressAndAnswer(KeyCode.DIGIT1, false, "OK");
		}

		assertTrue(program.bracket.isFinished());
		assertTrue(waitUntil(() -> FxSupport.run(() -> GameWindow.getScreen()) instanceof Podium, 15000));
	}
}
