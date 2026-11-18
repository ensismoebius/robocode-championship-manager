import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import support.DialogFields;
import support.FxSupport;
import support.TestConfiguration;
import tournament.ChampionshipService;
import tournament.DataArchive;
import utils.configuration.Configuration;

/** The data panel (Ctrl + D) as the organizer uses it: report, backup, archive and erase. The steps run in order. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MainDataTest {

	private static final String DATABASE = "target/test-db/data-panel.db";

	private static Main program;

	@BeforeAll
	static void startTheProgram() throws IOException {
		Path robocode = Path.of("target", "test-home", "robocode-data");
		Files.createDirectories(robocode.resolve("robots"));
		TestConfiguration.load(Configuration.RANDOM, robocode.toString(), DATABASE);
		TestConfiguration.freshDatabase();
		registerRobots(2);
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
		pressAndAnswer(KeyCode.G, true, "OK");
	}

	@AfterAll
	static void stopTheProgram() {
		FxSupport.runVoid(program::stop);
	}

	/** The button of the panel that begins with the name: the panel writes what it does after it. */
	private static Button action(Region panel, String name) {
		return DialogFields.nodes(panel).stream().filter(node -> node instanceof Button button && button.getText().startsWith(name)).map(Button.class::cast).findFirst().orElseThrow();
	}

	@SafeVarargs
	private static void openPanel(Consumer<Region>... steps) {
		FxSupport.onNextDialogs(steps);
		press(KeyCode.D, true);
	}

	private static long files(String folder, String extension) throws IOException {
		try (Stream<Path> list = Files.list(DataArchive.folder(folder))) {
			return list.filter(file -> file.toString().endsWith(extension)).count();
		}
	}

	@Test
	@Order(1)
	void theReportAndTheBackupAreWritten() throws IOException {
		openPanel(panel -> action(panel, "Gerar relatório").fire(), dialog -> button(dialog, "OK").fire());
		openPanel(panel -> action(panel, "Backup").fire(), dialog -> button(dialog, "OK").fire());

		assertTrue(files("reports", ".html") >= 1);
		assertTrue(files("backups", ".db") >= 1);
	}

	@Test
	@Order(2)
	void archivingKeepsTheDataWhenNothingIsErased() throws IOException {
		openPanel(panel -> action(panel, "Arquivar").fire(), dialog -> button(dialog, "OK").fire(), dialog -> button(dialog, "OK").fire());

		assertTrue(files("archives", ".zip") >= 1);
		assertNotNull(program.championship);
	}

	@Test
	@Order(3)
	void erasingTheChampionshipKeepsTheRobots() {
		openPanel(panel -> action(panel, "Apagar dados").fire(), dialog -> button(dialog, "OK").fire(), dialog -> button(dialog, "OK").fire());

		assertNull(program.championship);
		assertEquals(2, ChampionshipService.countOpponents());
	}

	@Test
	@Order(4)
	void erasingEverythingEmptiesTheDatabase() {
		openPanel(panel -> action(panel, "Apagar dados").fire(), dialog -> {
			DialogFields.nodes(dialog).stream().filter(node -> node instanceof RadioButton radio && radio.getText().startsWith("Tudo")).map(RadioButton.class::cast).findFirst().orElseThrow().setSelected(true);
			button(dialog, "OK").fire();
		}, dialog -> button(dialog, "OK").fire());

		assertEquals(0, ChampionshipService.countOpponents());
	}

	@Test
	@Order(5)
	void restoringABackupBringsTheRobotsBack() throws IOException {
		assertEquals(0, ChampionshipService.countOpponents());
		registerRobots(2);
		Path withRobots = DataArchive.backup();
		DataArchive.deleteAll();

		program.data.backupPicker = () -> withRobots;
		openPanel(panel -> action(panel, "Restaurar backup").fire(), dialog -> button(dialog, "OK").fire(), dialog -> button(dialog, "OK").fire());

		assertEquals(2, ChampionshipService.countOpponents());
	}
}
