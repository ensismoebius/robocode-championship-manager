/**
 *
 */
package widgets.windows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import utils.configuration.Configuration;
import utils.configuration.INewConfigurationHandle;
import utils.robocode.BattleControl;
import utils.robocode.BattleControl.WinnerCriterion;
import widgets.GameDialog;
import widgets.BattleAudio;
import widgets.GameWindow;
import widgets.scenes.ArenaStyle;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The configuration, as a panel inside the full screen window
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class NewConfigurationHandleWindow implements INewConfigurationHandle {

	@Override
	public Properties askForProperties(Properties oldProperties) {

		Properties old = oldProperties == null ? new Properties() : oldProperties;

		TextField title = new TextField(old.getProperty(TITLE, Configuration.DEFAULT_TITLE));
		title.setPromptText(Configuration.DEFAULT_TITLE);

		TextField subtitle = new TextField(old.getProperty(SUBTITLE, Configuration.DEFAULT_SUBTITLE));
		subtitle.setPromptText("Vazio para não exibir");

		CheckBox registrationEnabled = new CheckBox("Aceitar novos robôs");
		registrationEnabled.setSelected(Boolean.parseBoolean(old.getProperty(REGISTRATION_ENABLED, "true")));

		CheckBox antialiasing = new CheckBox("Suavização de bordas (antialiasing)");
		antialiasing.setSelected(Boolean.parseBoolean(old.getProperty(ANTIALIASING, "true")));

		TextField robocodeRounds = new TextField(old.getProperty(ROBOCODE_ROUNDS, String.valueOf(Configuration.DEFAULT_ROBOCODE_ROUNDS)));

		// The two kinds of battle: by points, or by knockout (the best of the rounds)
		ChoiceBox<String> winnerCriterion = new ChoiceBox<String>();
		winnerCriterion.getItems().addAll("Por pontos", "Por K.O. (melhor de)");
		winnerCriterion.getSelectionModel().select(WinnerCriterion.SCORE.name().equals(old.getProperty(WINNER_CRITERION)) ? 0 : 1);
		winnerCriterion.setMaxWidth(Double.MAX_VALUE);
		Label roundsCaption = new Label();
		roundsCaption.getStyleClass().add("form-label");
		Label battleRule = new Label();
		battleRule.setWrapText(true);
		battleRule.getStyleClass().add("form-hint");
		Runnable explainBattle = () -> {
			boolean knockout = winnerCriterion.getSelectionModel().getSelectedIndex() == 1;
			int rounds = Configuration.parseInt(robocodeRounds.getText(), -1);
			roundsCaption.setText(knockout ? "Melhor de quantas partidas" : "Quantas partidas");
			if (rounds < 1) {
				battleRule.setText("");
			} else if (knockout) {
				battleRule.setText("Melhor de " + rounds + ": vence quem ganhar " + BattleControl.roundsToWin(rounds) + " partidas, e a batalha termina assim que alguém chegar lá. Se as vitórias empatarem, inclusive em duplo K.O., decidem os pontos.");
			} else {
				battleRule.setText(rounds + (rounds == 1 ? " partida" : " partidas") + ": vence quem somar mais pontos. Se empatar nos pontos, decide quem ganhou mais partidas.");
			}
		};
		winnerCriterion.getSelectionModel().selectedIndexProperty().addListener((observable, before, after) -> explainBattle.run());
		robocodeRounds.textProperty().addListener((observable, before, after) -> explainBattle.run());
		explainBattle.run();

		ChoiceBox<String> battleView = new ChoiceBox<String>();
		battleView.getItems().addAll("3D, no gerenciador", "Janela clássica do robocode");
		battleView.getSelectionModel().select("CLASSIC".equals(old.getProperty(BATTLE_VIEW)) ? 1 : 0);
		battleView.setMaxWidth(Double.MAX_VALUE);

		TextField fightText = new TextField(old.getProperty(FIGHT_TEXT, Configuration.DEFAULT_FIGHT_TEXT));
		fightText.setPromptText(Configuration.DEFAULT_FIGHT_TEXT);

		ChoiceBox<String> battleSpeed = new ChoiceBox<String>();
		for (double speed : Configuration.BATTLE_SPEEDS) {
			battleSpeed.getItems().add(formatSpeed(speed) + "x" + (speed == Configuration.DEFAULT_BATTLE_SPEED ? " (normal)" : ""));
		}
		battleSpeed.getSelectionModel().select(speedIndex(old.getProperty(BATTLE_SPEED)));
		battleSpeed.setMaxWidth(Double.MAX_VALUE);

		// Random first, then each one
		ChoiceBox<String> arena = new ChoiceBox<String>();
		arena.getItems().add("Aleatória, uma em cada batalha");
		for (ArenaStyle style : ArenaStyle.values()) {
			arena.getItems().add(style.label);
		}
		arena.getSelectionModel().select(choiceIndex(old.getProperty(ARENA), ArenaStyle.values()));
		arena.setMaxWidth(Double.MAX_VALUE);

		ChoiceBox<String> music = new ChoiceBox<String>();
		music.getItems().add("Aleatória, uma em cada batalha");
		for (BattleAudio.Song song : BattleAudio.Song.values()) {
			music.getItems().add(song.label);
		}
		music.getSelectionModel().select(choiceIndex(old.getProperty(BATTLE_MUSIC), BattleAudio.Song.values()));
		music.setMaxWidth(Double.MAX_VALUE);
		CheckBox arenaMusic = new CheckBox("Usar mesma música da arena");
		arenaMusic.setSelected(Boolean.parseBoolean(old.getProperty(ARENA_MUSIC, "false")));
		// Each arena brings its own song, so the choice does not matter
		music.disableProperty().bind(arenaMusic.selectedProperty());

		Slider musicVolume = new Slider(0, 100, Configuration.parseInt(old.getProperty(MUSIC_VOLUME), Configuration.DEFAULT_MUSIC_VOLUME));
		musicVolume.setBlockIncrement(5);
		musicVolume.setMajorTickUnit(25);
		musicVolume.setShowTickMarks(true);
		Label musicVolumeValue = new Label();
		musicVolumeValue.setMinWidth(54);
		musicVolumeValue.textProperty().bind(musicVolume.valueProperty().asString("%.0f%%"));
		HBox.setHgrow(musicVolume, Priority.ALWAYS);

		TextField webserverPort = new TextField(old.getProperty(WEBSERVER_PORT, String.valueOf(Configuration.DEFAULT_WEBSERVER_PORT)));

		TextField databaseFile = new TextField(old.getProperty(DATABASE_FILE, Configuration.DEFAULT_DATABASE_FILE));
		databaseFile.setPromptText(Configuration.DEFAULT_DATABASE_FILE);
		HBox.setHgrow(databaseFile, Priority.ALWAYS);
		Button chooseDatabaseFile = new Button("Escolher...");
		chooseDatabaseFile.setOnAction(event -> {
			FileChooser fc = new FileChooser();
			fc.setTitle("Arquivo do banco de dados (é criado se não existir)");
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Banco SQLite", "*.db", "*.sqlite", "*.sqlite3"));
			File chosen = fc.showSaveDialog(GameWindow.getWindow());
			if (chosen != null) {
				databaseFile.setText(chosen.getPath());
			}
		});

		TextField robocodeDirectory = new TextField(old.getProperty(ROBOCODE_PATH, ""));
		robocodeDirectory.setPromptText("Obrigatório: pasta onde o robocode está instalado (a que contém robots)");
		HBox.setHgrow(robocodeDirectory, Priority.ALWAYS);
		Button chooseRobocodeDirectory = new Button("Escolher...");
		chooseRobocodeDirectory.setOnAction(event -> {
			DirectoryChooser dc = new DirectoryChooser();
			dc.setTitle("Escolha o diretório onde está instalado o robocode");
			File chosen = dc.showDialog(GameWindow.getWindow());
			if (chosen != null) {
				robocodeDirectory.setText(chosen.getPath());
			}
		});

		Label error = new Label();
		error.getStyleClass().add("error-text");
		error.setWrapText(true);

		// One tab for each subject, each one in two columns
		GridPane startGrid = grid();
		startGrid.add(field("Título da tela inicial", title), 0, 0);
		startGrid.add(field("Subtítulo da tela inicial", subtitle), 1, 0);
		startGrid.add(field("Inscrições", registrationEnabled), 0, 1);
		startGrid.add(field("Porta do servidor web de cadastro", webserverPort), 1, 1);
		Tab startTab = new Tab("Tela inicial", startGrid);

		GridPane battleGrid = grid();
		battleGrid.add(field("Tipo de batalha", winnerCriterion), 0, 0);
		battleGrid.add(new VBox(6, roundsCaption, robocodeRounds), 1, 0);
		battleGrid.add(battleRule, 0, 1, 2, 1);
		battleGrid.add(field("Exibição da batalha", battleView), 0, 2);
		battleGrid.add(field("Velocidade inicial da batalha 3D", battleSpeed), 1, 2);
		battleGrid.add(field("Texto do início do round", fightText), 0, 3, 2, 1);
		Tab battleTab = new Tab("Batalha", battleGrid);

		GridPane soundGrid = grid();
		soundGrid.add(field("Arena da batalha 3D", arena), 0, 0);
		soundGrid.add(field("Qualidade gráfica da batalha 3D", antialiasing), 1, 0);
		soundGrid.add(field("Música da batalha 3D", new VBox(8, music, arenaMusic)), 0, 1);
		soundGrid.add(field("Volume da música de fundo", new HBox(10, musicVolume, musicVolumeValue)), 1, 1);
		Tab soundTab = new Tab("Arena e som", soundGrid);

		GridPane systemGrid = grid();
		systemGrid.add(field("Arquivo do banco de dados SQLite", new HBox(6, databaseFile, chooseDatabaseFile)), 0, 0, 2, 1);
		systemGrid.add(field("Diretório do robocode", new HBox(6, robocodeDirectory, chooseRobocodeDirectory)), 0, 1, 2, 1);
		Tab systemTab = new Tab("Sistema", systemGrid);
		TabPane tabs = new TabPane(startTab, battleTab, soundTab, systemTab);
		tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		// The first tab is the one the user sees first
		tabs.getSelectionModel().select(startTab);

		Button save = new Button("Salvar");
		save.getStyleClass().add("primary");
		Button cancel = new Button("Cancelar");
		VBox panel = GameDialog.panel("Configurações", new VBox(12, tabs, error), cancel, save);
		panel.setStyle("-fx-pref-width: 900;");

		save.setOnAction(event -> {
			int rounds = Configuration.parseInt(robocodeRounds.getText(), -1);
			int port = Configuration.parseInt(webserverPort.getText(), -1);
			String robocodePath = robocodeDirectory.getText().trim();

			if (databaseFile.getText().isBlank() || Files.isDirectory(Path.of(databaseFile.getText().trim()))) {
				error.setText("Informe o arquivo do banco de dados");
				tabs.getSelectionModel().select(systemTab);
				return;
			}
			if (robocodePath.isEmpty() || !Files.isDirectory(Path.of(robocodePath, "robots"))) {
				error.setText("Escolha o diretório onde o robocode está instalado (o que contém o diretório robots)");
				tabs.getSelectionModel().select(systemTab);
				return;
			}
			if (rounds < 1) {
				error.setText("O número de partidas deve ser um número maior que zero");
				tabs.getSelectionModel().select(battleTab);
				return;
			}
			if (port < 1 || port > 65535) {
				error.setText("A porta deve ser um número entre 1 e 65535");
				tabs.getSelectionModel().select(startTab);
				return;
			}

			Properties properties = new Properties();
			properties.setProperty(ROBOCODE_ROUNDS, String.valueOf(rounds));
			properties.setProperty(WEBSERVER_PORT, String.valueOf(port));
			properties.setProperty(WINNER_CRITERION, (winnerCriterion.getSelectionModel().getSelectedIndex() == 0 ? WinnerCriterion.SCORE : WinnerCriterion.RANK).name());
			properties.setProperty(DATABASE_FILE, databaseFile.getText().trim());
			properties.setProperty(ROBOCODE_PATH, robocodePath);
			properties.setProperty(REGISTRATION_ENABLED, String.valueOf(registrationEnabled.isSelected()));
			properties.setProperty(TITLE, title.getText().isBlank() ? Configuration.DEFAULT_TITLE : title.getText().strip());
			properties.setProperty(SUBTITLE, subtitle.getText().strip());
			properties.setProperty(BATTLE_SPEED, String.valueOf(Configuration.BATTLE_SPEEDS[Math.max(0, battleSpeed.getSelectionModel().getSelectedIndex())]));
			properties.setProperty(FIGHT_TEXT, fightText.getText().isBlank() ? Configuration.DEFAULT_FIGHT_TEXT : fightText.getText().strip());
			int arenaIndex = arena.getSelectionModel().getSelectedIndex();
			properties.setProperty(ARENA, arenaIndex <= 0 ? Configuration.RANDOM : ArenaStyle.values()[arenaIndex - 1].name());
			int musicIndex = music.getSelectionModel().getSelectedIndex();
			properties.setProperty(BATTLE_MUSIC, musicIndex <= 0 ? Configuration.RANDOM : BattleAudio.Song.values()[musicIndex - 1].name());
			properties.setProperty(ANTIALIASING, String.valueOf(antialiasing.isSelected()));
			properties.setProperty(ARENA_MUSIC, String.valueOf(arenaMusic.isSelected()));
			properties.setProperty(MUSIC_VOLUME, String.valueOf((int) Math.round(musicVolume.getValue())));
			properties.setProperty(BATTLE_VIEW, battleView.getSelectionModel().getSelectedIndex() == 1 ? "CLASSIC" : "3D");

			GameWindow.close(panel, properties);
		});
		cancel.setOnAction(event -> GameWindow.close(panel, null));

		// The save button gets the focus, so Enter saves
		return (Properties) GameWindow.showModal(panel, save, cancel);
	}

	/**
	 * The position of the configured speed in {@link Configuration#BATTLE_SPEEDS}
	 */
	private static int speedIndex(String configured) {
		double speed = Configuration.DEFAULT_BATTLE_SPEED;
		try {
			speed = configured == null ? speed : Double.parseDouble(configured.trim());
		} catch (NumberFormatException e) {
			// The default one
		}
		int best = 0;
		for (int i = 0; i < Configuration.BATTLE_SPEEDS.length; i++) {
			if (Math.abs(Configuration.BATTLE_SPEEDS[i] - speed) < Math.abs(Configuration.BATTLE_SPEEDS[best] - speed)) {
				best = i;
			}
		}
		return best;
	}

	/**
	 * The position of the configured option, after the random one (0)
	 */
	private static int choiceIndex(String configured, Enum<?>[] options) {
		for (int i = 0; i < options.length; i++) {
			if (options[i].name().equals(configured)) {
				return i + 1;
			}
		}
		return 0;
	}

	private static String formatSpeed(double speed) {
		return speed == Math.rint(speed) ? String.valueOf((int) speed) : String.valueOf(speed);
	}

	/**
	 * A grid of two columns with some room around, the content of a tab
	 */
	private static GridPane grid() {
		GridPane grid = new GridPane();
		grid.setHgap(24);
		grid.setVgap(14);
		grid.setPadding(new Insets(16));
		ColumnConstraints half = new ColumnConstraints();
		half.setPercentWidth(50);
		grid.getColumnConstraints().addAll(half, half);
		return grid;
	}

	/**
	 * A caption above the field
	 */
	private static VBox field(String caption, Node input) {
		Label label = new Label(caption);
		label.getStyleClass().add("form-label");
		return new VBox(6, label, input);
	}
}
