import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static support.ChampionshipFixture.registerRobots;
import static support.DialogFields.button;
import static support.FxSupport.press;
import static support.FxSupport.pressAndAnswer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import support.DialogFields;
import support.FxSupport;
import support.TestConfiguration;
import utils.configuration.Configuration;
import widgets.GameWindow;
import widgets.scenes.BracketViewer;
import widgets.scenes.StartScreen;

/**
 * The championship as the operator draws it: a draw needs robots and can be
 * cancelled, a draw over results warns that the results are lost, and another
 * database brings its own championship to the screen. The steps run in order.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MainChampionshipTest {

	private static final String FIRST_DATABASE = "target/test-db/championship-first.db";
	private static final String SECOND_DATABASE = "target/test-db/championship-second.db";

	private static Main program;
	private static Path robocode;

	@BeforeAll
	static void startTheProgram() throws IOException {
		robocode = Path.of("target", "test-home", "robocode-championship");
		Files.createDirectories(robocode.resolve("robots"));
		TestConfiguration.load(Configuration.RANDOM, robocode.toString(), FIRST_DATABASE);
		TestConfiguration.freshDatabase();
		int port;
		try (ServerSocket free = new ServerSocket(0)) {
			port = free.getLocalPort();
		}

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

	/** Loads the configuration with another database, on the JavaFX thread where the program follows it. The port stays the same. */
	private static void reconfigure(String database) {
		FxSupport.runVoid(() -> TestConfiguration.load(Configuration.RANDOM, robocode.toString(), database, Configuration.getWebserverPort()));
	}

	@Test
	@Order(1)
	void withoutRobotsThereIsNoChampionshipToDraw() {
		FxSupport.onNextDialog(dialog -> button(dialog, "OK").fire());
		press(KeyCode.G, true);
		assertNull(program.championship);

		FxSupport.onNextDialog(dialog -> button(dialog, "OK").fire());
		FxSupport.runVoid(program::showBracket);
		assertNull(program.bracket);

		FxSupport.onNextDialog(dialog -> button(dialog, "OK").fire());
		assertTrue(FxSupport.run(() -> program.playableSelectedMatch().isEmpty()));
	}

	@Test
	@Order(2)
	void aDrawCanBeCancelled() throws IOException {
		registerRobots(2);
		FxSupport.onNextDialog(dialog -> button(dialog, "Cancelar").fire());

		press(KeyCode.G, true);

		assertNull(program.championship);
	}

	@Test
	@Order(3)
	void aDrawnChampionshipShowsItsBracketAndTheMatchToPlay() {
		pressAndAnswer(KeyCode.G, true, "OK");

		assertNotNull(program.championship);
		assertInstanceOf(BracketViewer.class, FxSupport.run(() -> GameWindow.getScreen()));
		assertTrue(FxSupport.run(() -> program.playableSelectedMatch().isPresent()));

		// Without a selected match there is nothing to play
		FxSupport.runVoid(() -> program.selectedMatch = null);
		FxSupport.onNextDialog(dialog -> button(dialog, "OK").fire());
		assertTrue(FxSupport.run(() -> program.playableSelectedMatch().isEmpty()));
	}

	@Test
	@Order(4)
	void aDrawOverResultsWarnsThatTheResultsAreLost() {
		press(KeyCode.N, false);
		pressAndAnswer(KeyCode.DIGIT1, false, "OK");
		assertEquals(1, program.championship.getResults().size());

		AtomicBoolean warned = new AtomicBoolean();
		FxSupport.onNextDialog(dialog -> {
			warned.set(DialogFields.nodes(dialog).stream().anyMatch(node -> node instanceof Label label && label.getText().contains("ATENÇÃO")));
			button(dialog, "Cancelar").fire();
		});
		press(KeyCode.G, true);

		assertTrue(warned.get());
		assertEquals(1, program.championship.getResults().size());
	}

	@Test
	@Order(5)
	void anotherDatabaseBringsItsOwnChampionshipToTheScreen() throws IOException {
		reconfigure(SECOND_DATABASE);

		assertInstanceOf(StartScreen.class, FxSupport.run(() -> GameWindow.getScreen()));
		assertNull(program.championship);

		registerRobots(2);
		pressAndAnswer(KeyCode.G, true, "OK");
		assertNotNull(program.championship);

		// Back to the first database: its championship and its result come back to the screen
		reconfigure(FIRST_DATABASE);

		assertInstanceOf(BracketViewer.class, FxSupport.run(() -> GameWindow.getScreen()));
		assertEquals(1, program.championship.getResults().size());
	}

	@Test
	@Order(6)
	void aFinishedBracketHasNoMatchToPlayAndCrownsTheChampion() {
		while (!program.bracket.isFinished()) {
			program.selectedMatch = program.bracket.getNextMatch().orElseThrow().getCode();
			pressAndAnswer(KeyCode.DIGIT1, false, "OK");
		}

		assertTrue(FxSupport.run(() -> program.playableSelectedMatch().isEmpty()));

		// The screen fades to the champion: the class ends once the podium is in place, or the fade would reach a stopped program
		for (int wait = 0; wait < 200 && FxSupport.run(() -> program.championTransition); wait++) {
			FxSupport.sleep(50);
		}
		assertFalse(FxSupport.run(() -> program.championTransition));
	}

	@Test
	@Order(7)
	void aDatabaseThatCanNotBeReadIsReportedWhenTheBracketIsShown() throws IOException {
		Path blocker = Path.of("target", "test-home", "championship-blocker");
		Files.createDirectories(blocker.getParent());
		Files.writeString(blocker, "um arquivo, não uma pasta");
		// The program reports the database it can not open, and then the bracket it can not load
		FxSupport.onNextDialogs(dialog -> button(dialog, "OK").fire(), dialog -> button(dialog, "OK").fire());

		reconfigure(blocker.resolve("robochamp.db").toString());
		FxSupport.runVoid(program::showBracket);

		assertNull(program.bracket);
	}
}
