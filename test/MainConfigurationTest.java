import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static support.DialogFields.button;
import static support.DialogFields.find;
import static support.DialogFields.text;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import support.FxSupport;
import support.TestConfiguration;
import utils.configuration.Configuration;
import widgets.GameWindow;
import widgets.scenes.StartScreen;

/**
 * The program follows its configuration while it runs: a new database reloads the
 * championship, a new port restarts the web server, and what can not be done is
 * reported to the operator.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MainConfigurationTest {

	private static Main program;
	private static Path robocode;
	private static ServerSocket busy;

	@BeforeAll
	static void startTheProgramWithAPortTaken() throws IOException {
		robocode = Path.of("target", "test-home", "robocode-config");
		Files.createDirectories(robocode.resolve("robots"));
		TestConfiguration.load(Configuration.RANDOM, robocode.toString());
		// The port is taken by another program: the web server can not start and the operator is told
		busy = new ServerSocket(0);
		int port = busy.getLocalPort();

		FxSupport.onNextDialogs(dialog -> {
			text(dialog, "Porta do servidor web de cadastro", String.valueOf(port));
			button(dialog, "Salvar").fire();
		}, dialog -> button(dialog, "OK").fire());
		program = FxSupport.run(Main::new);
		Stage stage = FxSupport.run(Stage::new);
		FxSupport.runVoid(() -> program.start(stage));
	}

	@AfterAll
	static void stopTheProgram() throws IOException {
		FxSupport.runVoid(program::stop);
		busy.close();
	}

	/** The address shown to the teams on the start screen: it is there while the web server runs and the registration is open. */
	private static String address() {
		return FxSupport.run(() -> ((Label) find(GameWindow.getScreen(), node -> node instanceof Label label && label.getStyleClass().contains("address"))).getText());
	}

	/** The status of the registration on the start screen, the badge with the square or the circle in front. */
	private static String badge() {
		return FxSupport.run(() -> ((Label) find(GameWindow.getScreen(), node -> node instanceof Label label && label.getText() != null && label.getText().matches("[■●].*"))).getText());
	}

	/** Reloads the configuration on the JavaFX thread, where the program reacts to it. */
	private static void reconfigure(String database, int port) {
		FxSupport.runVoid(() -> TestConfiguration.load(Configuration.RANDOM, robocode.toString(), database, port));
	}

	@Test
	@Order(1)
	void theProgramStartsOnTheStartScreenWithTheWebServerDown() {
		assertInstanceOf(StartScreen.class, FxSupport.run(() -> GameWindow.getScreen()));
		assertEquals("■ SERVIDOR DE INSCRIÇÕES PARADO", badge());
	}

	@Test
	@Order(2)
	void theWebServerStartsOnAFreePort() throws IOException {
		int free;
		try (ServerSocket socket = new ServerSocket(0)) {
			free = socket.getLocalPort();
		}

		reconfigure("target/test-db/main-config.db", free);

		assertTrue(address().endsWith(":" + free), address());
	}

	@Test
	@Order(3)
	void aPortTakenByAnotherProgramIsReportedToTheOperator() {
		FxSupport.onNextDialog(dialog -> button(dialog, "OK").fire());

		reconfigure("target/test-db/main-config.db", busy.getLocalPort());

		assertEquals("■ SERVIDOR DE INSCRIÇÕES PARADO", badge());
	}

	@Test
	@Order(4)
	void aNewDatabaseReloadsTheProgramAndGoesBackToTheStartScreen() {
		FxSupport.runVoid(() -> GameWindow.setScreen(new StackPane()));

		reconfigure("target/test-db/main-config-two.db", 8080);

		assertInstanceOf(StartScreen.class, FxSupport.run(() -> GameWindow.getScreen()));
		assertNull(program.bracket);
	}

	@Test
	@Order(5)
	void aDatabaseThatCanNotBeOpenedIsReportedToTheOperator() throws IOException {
		Path blocker = Path.of("target", "test-home", "config-blocker");
		Files.createDirectories(blocker.getParent());
		Files.writeString(blocker, "um arquivo, não uma pasta");
		FxSupport.onNextDialog(dialog -> button(dialog, "OK").fire());

		reconfigure(blocker.resolve("robochamp.db").toString(), 8080);

		assertNull(program.championship);
		assertNull(program.bracket);
	}
}
