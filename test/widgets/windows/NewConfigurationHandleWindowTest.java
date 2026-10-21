package widgets.windows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static support.DialogFields.button;
import static support.DialogFields.check;
import static support.DialogFields.choose;
import static support.DialogFields.input;
import static support.DialogFields.nodes;
import static support.DialogFields.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import support.FxSupport;
import utils.configuration.Configuration;
import utils.configuration.INewConfigurationHandle;
import widgets.scenes.ArenaStyle;

class NewConfigurationHandleWindowTest {

	/** What the form showed when it refused the save: the error and the tab that is open. */
	private record Refusal(String error, String tab) {
	}

	private static NewConfigurationHandleWindow window;
	private static Path robocode;

	@BeforeAll
	static void openTheProgram() throws IOException {
		FxSupport.gameWindow();
		robocode = Path.of("target", "test-home", "robocode-form");
		Files.createDirectories(robocode.resolve("robots"));
		window = new NewConfigurationHandleWindow();
	}

	private static Properties saved() {
		Properties p = new Properties();
		p.setProperty(INewConfigurationHandle.ROBOCODE_PATH, robocode.toString());
		p.setProperty(INewConfigurationHandle.DATABASE_FILE, "target/test-db/form.db");
		p.setProperty(INewConfigurationHandle.ROBOCODE_ROUNDS, "1");
		p.setProperty(INewConfigurationHandle.WEBSERVER_PORT, "8080");
		p.setProperty(INewConfigurationHandle.WINNER_CRITERION, "SCORE");
		p.setProperty(INewConfigurationHandle.BATTLE_VIEW, "3D");
		return p;
	}

	private static Refusal refusal(Node panel) {
		String error = nodes(panel).stream().filter(node -> node instanceof Label label && label.getStyleClass().contains("error-text")).map(node -> ((Label) node).getText()).findFirst().orElse("");
		String tab = nodes(panel).stream().filter(node -> node instanceof TabPane).map(node -> ((TabPane) node).getSelectionModel().getSelectedItem().getText()).findFirst().orElse("");
		return new Refusal(error, tab);
	}

	/** Saves the form after the change, which must be refused, and cancels it. */
	private static Refusal refused(Consumer<Node> change) {
		AtomicReference<Refusal> shown = new AtomicReference<Refusal>();
		FxSupport.onNextDialog(panel -> {
			change.accept(panel);
			button(panel, "Salvar").fire();
			shown.set(refusal(panel));
			button(panel, "Cancelar").fire();
		});
		assertNull(FxSupport.run(() -> window.askForProperties(saved())));
		return shown.get();
	}

	@Test
	void theOperatorSavesTheFormAsTyped() {
		AtomicReference<Properties> result = new AtomicReference<Properties>();
		FxSupport.onNextDialog(panel -> {
			text(panel, "Título da tela inicial", "  Torneio Sul  ");
			text(panel, "Subtítulo da tela inicial", " Edição 2 ");
			check(panel, "Inscrições", false);
			text(panel, "Porta do servidor web de cadastro", "9090");
			text(panel, "Quantas partidas", "3");
			choose(panel, "Tipo de batalha", 1);
			choose(panel, "Exibição da batalha", 1);
			choose(panel, "Velocidade inicial da batalha 3D", 2);
			text(panel, "Texto do início do round", "");
			choose(panel, "Arena da batalha 3D", 2);
			check(panel, "Qualidade gráfica da batalha 3D", false);
			CheckBox sameMusic = (CheckBox) ((VBox) input(panel, "Música da batalha 3D")).getChildren().get(1);
			sameMusic.setSelected(true);
			nodes(panel).stream().filter(node -> node instanceof Slider).map(Slider.class::cast).findFirst().orElseThrow().setValue(40);
			button(panel, "Salvar").fire();
		});
		result.set(FxSupport.run(() -> window.askForProperties(saved())));

		Properties properties = result.get();
		assertEquals("Torneio Sul", properties.getProperty(INewConfigurationHandle.TITLE));
		assertEquals("Edição 2", properties.getProperty(INewConfigurationHandle.SUBTITLE));
		assertEquals("false", properties.getProperty(INewConfigurationHandle.REGISTRATION_ENABLED));
		assertEquals("9090", properties.getProperty(INewConfigurationHandle.WEBSERVER_PORT));
		assertEquals("3", properties.getProperty(INewConfigurationHandle.ROBOCODE_ROUNDS));
		assertEquals("RANK", properties.getProperty(INewConfigurationHandle.WINNER_CRITERION));
		assertEquals("CLASSIC", properties.getProperty(INewConfigurationHandle.BATTLE_VIEW));
		assertEquals(String.valueOf(Configuration.BATTLE_SPEEDS[2]), properties.getProperty(INewConfigurationHandle.BATTLE_SPEED));
		assertEquals(Configuration.DEFAULT_FIGHT_TEXT, properties.getProperty(INewConfigurationHandle.FIGHT_TEXT));
		assertEquals(ArenaStyle.values()[1].name(), properties.getProperty(INewConfigurationHandle.ARENA));
		assertEquals(Configuration.RANDOM, properties.getProperty(INewConfigurationHandle.BATTLE_MUSIC));
		assertEquals("false", properties.getProperty(INewConfigurationHandle.ANTIALIASING));
		assertEquals("true", properties.getProperty(INewConfigurationHandle.ARENA_MUSIC));
		assertEquals("40", properties.getProperty(INewConfigurationHandle.MUSIC_VOLUME));
	}

	@Test
	void aBlankDatabaseFileIsRefusedOnTheSystemTab() {
		Refusal refusal = refused(panel -> text(panel, "Arquivo do banco de dados SQLite", "   "));

		assertEquals("Informe o arquivo do banco de dados", refusal.error());
		assertEquals("Sistema", refusal.tab());
	}

	@Test
	void aRobocodeFolderWithoutRobotsIsRefused() {
		Refusal refusal = refused(panel -> text(panel, "Diretório do robocode", ""));

		assertTrue(refusal.error().startsWith("Escolha o diretório onde o robocode está instalado"));
		assertEquals("Sistema", refusal.tab());
	}

	@Test
	void theRoundsMustBePositive() {
		Refusal refusal = refused(panel -> text(panel, "Quantas partidas", "0"));

		assertEquals("O número de partidas deve ser um número maior que zero", refusal.error());
		assertEquals("Batalha", refusal.tab());
	}

	@Test
	void thePortMustBeAValidNumber() {
		Refusal refusal = refused(panel -> text(panel, "Porta do servidor web de cadastro", "70000"));

		assertEquals("A porta deve ser um número entre 1 e 65535", refusal.error());
		assertEquals("Tela inicial", refusal.tab());
	}

	@Test
	void theBattleRuleFollowsTheChoiceAndTheRounds() {
		List<String> rules = new ArrayList<String>();
		FxSupport.onNextDialog(panel -> {
			text(panel, "Quantas partidas", "1");
			rules.add(((Label) nodes(panel).stream().filter(node -> node instanceof Label label && label.getStyleClass().contains("form-hint")).findFirst().orElseThrow()).getText());
			choose(panel, "Tipo de batalha", 1);
			text(panel, "Melhor de quantas partidas", "3");
			rules.add(((Label) nodes(panel).stream().filter(node -> node instanceof Label label && label.getStyleClass().contains("form-hint")).findFirst().orElseThrow()).getText());
			button(panel, "Cancelar").fire();
		});
		assertNull(FxSupport.run(() -> window.askForProperties(saved())));

		assertTrue(rules.get(0).startsWith("1 partida:"), rules.get(0));
		assertTrue(rules.get(1).startsWith("Melhor de 3: vence quem ganhar 2 partidas"), rules.get(1));
	}

	@Test
	void cancellingWithoutOldPropertiesReturnsNothing() {
		FxSupport.onNextDialog(panel -> button(panel, "Cancelar").fire());

		assertNull(FxSupport.run(() -> window.askForProperties(null)));
	}
}
