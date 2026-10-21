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
import widgets.Song;
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
		ConfigForm form = new ConfigForm(oldProperties == null ? new Properties() : oldProperties);

		Label error = new Label();
		error.getStyleClass().add("error-text");
		error.setWrapText(true);

		TabPane tabs = new TabPane(form.startTab, form.battleTab, form.soundTab, form.systemTab);
		tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		// The first tab is the one the user sees first
		tabs.getSelectionModel().select(form.startTab);

		Button save = new Button("Salvar");
		save.getStyleClass().add("primary");
		Button cancel = new Button("Cancelar");
		VBox panel = GameDialog.panel("Configurações", new VBox(12, tabs, error), cancel, save);
		panel.setStyle("-fx-pref-width: 900;");

		save.setOnAction(event -> {
			Problem problem = form.problem();
			if (problem != null) {
				error.setText(problem.message());
				tabs.getSelectionModel().select(problem.tab());
				return;
			}
			GameWindow.close(panel, form.properties());
		});
		cancel.setOnAction(event -> GameWindow.close(panel, null));

		// The save button gets the focus, so Enter saves
		return (Properties) GameWindow.showModal(panel, save, cancel);
	}

	/** What is wrong in the form, and the tab where it is */
	private record Problem(String message, Tab tab) {
	}

	/**
	 * The controls of the configuration, one tab for each subject
	 */
	private static final class ConfigForm {

		private final TextField title;
		private final TextField subtitle;
		private final CheckBox registrationEnabled;
		private final CheckBox antialiasing;
		private final TextField robocodeRounds;
		private final ChoiceBox<String> winnerCriterion;
		private final ChoiceBox<String> battleView;
		private final TextField fightText;
		private final ChoiceBox<String> battleSpeed;
		private final ChoiceBox<String> arena;
		private final ChoiceBox<String> music;
		private final CheckBox arenaMusic;
		private final Slider musicVolume;
		private final TextField webserverPort;
		private final TextField databaseFile;
		private final TextField robocodeDirectory;

		final Tab startTab;
		final Tab battleTab;
		final Tab soundTab;
		final Tab systemTab;

		ConfigForm(Properties old) {
			this.title = new TextField(old.getProperty(TITLE, Configuration.DEFAULT_TITLE));
			this.title.setPromptText(Configuration.DEFAULT_TITLE);

			this.subtitle = new TextField(old.getProperty(SUBTITLE, Configuration.DEFAULT_SUBTITLE));
			this.subtitle.setPromptText("Vazio para não exibir");

			this.registrationEnabled = new CheckBox("Aceitar novos robôs");
			this.registrationEnabled.setSelected(Boolean.parseBoolean(old.getProperty(REGISTRATION_ENABLED, "true")));

			this.antialiasing = new CheckBox("Suavização de bordas (antialiasing)");
			this.antialiasing.setSelected(Boolean.parseBoolean(old.getProperty(ANTIALIASING, "true")));

			this.robocodeRounds = new TextField(old.getProperty(ROBOCODE_ROUNDS, String.valueOf(Configuration.DEFAULT_ROBOCODE_ROUNDS)));

			this.winnerCriterion = criterionChoice(old);
			this.battleView = new ChoiceBox<String>();
			this.battleView.getItems().addAll("3D, no gerenciador", "Janela clássica do robocode");
			this.battleView.getSelectionModel().select("CLASSIC".equals(old.getProperty(BATTLE_VIEW)) ? 1 : 0);
			this.battleView.setMaxWidth(Double.MAX_VALUE);

			this.fightText = new TextField(old.getProperty(FIGHT_TEXT, Configuration.DEFAULT_FIGHT_TEXT));
			this.fightText.setPromptText(Configuration.DEFAULT_FIGHT_TEXT);

			this.battleSpeed = new ChoiceBox<String>();
			for (double speed : Configuration.BATTLE_SPEEDS) {
				this.battleSpeed.getItems().add(formatSpeed(speed) + "x" + (speed == Configuration.DEFAULT_BATTLE_SPEED ? " (normal)" : ""));
			}
			this.battleSpeed.getSelectionModel().select(speedIndex(old.getProperty(BATTLE_SPEED)));
			this.battleSpeed.setMaxWidth(Double.MAX_VALUE);

			// Random first, then each one
			this.arena = new ChoiceBox<String>();
			this.arena.getItems().add("Aleatória, uma em cada batalha");
			for (ArenaStyle style : ArenaStyle.values()) {
				this.arena.getItems().add(style.label);
			}
			this.arena.getSelectionModel().select(choiceIndex(old.getProperty(ARENA), ArenaStyle.values()));
			this.arena.setMaxWidth(Double.MAX_VALUE);

			this.music = new ChoiceBox<String>();
			this.music.getItems().add("Aleatória, uma em cada batalha");
			for (Song song : Song.values()) {
				this.music.getItems().add(song.label);
			}
			this.music.getSelectionModel().select(choiceIndex(old.getProperty(BATTLE_MUSIC), Song.values()));
			this.music.setMaxWidth(Double.MAX_VALUE);
			this.arenaMusic = new CheckBox("Usar mesma música da arena");
			this.arenaMusic.setSelected(Boolean.parseBoolean(old.getProperty(ARENA_MUSIC, "false")));
			// Each arena brings its own song, so the choice does not matter
			this.music.disableProperty().bind(this.arenaMusic.selectedProperty());

			this.musicVolume = new Slider(0, 100, Configuration.parseInt(old.getProperty(MUSIC_VOLUME), Configuration.DEFAULT_MUSIC_VOLUME));
			this.musicVolume.setBlockIncrement(5);
			this.musicVolume.setMajorTickUnit(25);
			this.musicVolume.setShowTickMarks(true);
			HBox.setHgrow(this.musicVolume, Priority.ALWAYS);

			this.webserverPort = new TextField(old.getProperty(WEBSERVER_PORT, String.valueOf(Configuration.DEFAULT_WEBSERVER_PORT)));

			this.databaseFile = new TextField(old.getProperty(DATABASE_FILE, Configuration.DEFAULT_DATABASE_FILE));
			this.databaseFile.setPromptText(Configuration.DEFAULT_DATABASE_FILE);
			HBox.setHgrow(this.databaseFile, Priority.ALWAYS);

			this.robocodeDirectory = new TextField(old.getProperty(ROBOCODE_PATH, ""));
			this.robocodeDirectory.setPromptText("Obrigatório: pasta onde o robocode está instalado (a que contém robots)");
			HBox.setHgrow(this.robocodeDirectory, Priority.ALWAYS);

			this.startTab = this.startTab();
			this.battleTab = this.battleTab();
			this.soundTab = this.soundTab();
			this.systemTab = this.systemTab();
		}

		/**
		 * The two kinds of battle: by points, or by knockout (the best of the rounds).
		 * Its explanation follows the choice and the number of rounds.
		 */
		private ChoiceBox<String> criterionChoice(Properties old) {
			ChoiceBox<String> choice = new ChoiceBox<String>();
			choice.getItems().addAll("Por pontos", "Por K.O. (melhor de)");
			choice.getSelectionModel().select(WinnerCriterion.SCORE.name().equals(old.getProperty(WINNER_CRITERION)) ? 0 : 1);
			choice.setMaxWidth(Double.MAX_VALUE);
			return choice;
		}

		/** Explains the rule of the battle type chosen, when the choice or the rounds change */
		private void explainBattle(Label roundsCaption, Label battleRule) {
			Runnable explain = () -> {
				boolean knockout = this.winnerCriterion.getSelectionModel().getSelectedIndex() == 1;
				int rounds = Configuration.parseInt(this.robocodeRounds.getText(), -1);
				roundsCaption.setText(knockout ? "Melhor de quantas partidas" : "Quantas partidas");
				if (rounds < 1) {
					battleRule.setText("");
				} else if (knockout) {
					battleRule.setText("Melhor de " + rounds + ": vence quem ganhar " + BattleControl.roundsToWin(rounds) + " partidas, e a batalha termina assim que alguém chegar lá. Se as vitórias empatarem, inclusive em duplo K.O., decidem os pontos.");
				} else {
					battleRule.setText(rounds + (rounds == 1 ? " partida" : " partidas") + ": vence quem somar mais pontos. Se empatar nos pontos, decide quem ganhou mais partidas.");
				}
			};
			this.winnerCriterion.getSelectionModel().selectedIndexProperty().addListener((observable, before, after) -> explain.run());
			this.robocodeRounds.textProperty().addListener((observable, before, after) -> explain.run());
			explain.run();
		}

		private Tab startTab() {
			GridPane grid = grid();
			grid.add(field("Título da tela inicial", this.title), 0, 0);
			grid.add(field("Subtítulo da tela inicial", this.subtitle), 1, 0);
			grid.add(field("Inscrições", this.registrationEnabled), 0, 1);
			grid.add(field("Porta do servidor web de cadastro", this.webserverPort), 1, 1);
			return new Tab("Tela inicial", grid);
		}

		private Tab battleTab() {
			Label roundsCaption = new Label();
			roundsCaption.getStyleClass().add("form-label");
			Label battleRule = new Label();
			battleRule.setWrapText(true);
			battleRule.getStyleClass().add("form-hint");
			this.explainBattle(roundsCaption, battleRule);

			GridPane grid = grid();
			grid.add(field("Tipo de batalha", this.winnerCriterion), 0, 0);
			grid.add(new VBox(6, roundsCaption, this.robocodeRounds), 1, 0);
			grid.add(battleRule, 0, 1, 2, 1);
			grid.add(field("Exibição da batalha", this.battleView), 0, 2);
			grid.add(field("Velocidade inicial da batalha 3D", this.battleSpeed), 1, 2);
			grid.add(field("Texto do início do round", this.fightText), 0, 3, 2, 1);
			return new Tab("Batalha", grid);
		}

		private Tab soundTab() {
			Label musicVolumeValue = new Label();
			musicVolumeValue.setMinWidth(54);
			musicVolumeValue.textProperty().bind(this.musicVolume.valueProperty().asString("%.0f%%"));

			GridPane grid = grid();
			grid.add(field("Arena da batalha 3D", this.arena), 0, 0);
			grid.add(field("Qualidade gráfica da batalha 3D", this.antialiasing), 1, 0);
			grid.add(field("Música da batalha 3D", new VBox(8, this.music, this.arenaMusic)), 0, 1);
			grid.add(field("Volume da música de fundo", new HBox(10, this.musicVolume, musicVolumeValue)), 1, 1);
			return new Tab("Arena e som", grid);
		}

		private Tab systemTab() {
			Button chooseDatabaseFile = new Button("Escolher...");
			chooseDatabaseFile.setOnAction(event -> {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Arquivo do banco de dados (é criado se não existir)");
				fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Banco SQLite", "*.db", "*.sqlite", "*.sqlite3"));
				File chosen = fileChooser.showSaveDialog(GameWindow.getWindow());
				if (chosen != null) {
					this.databaseFile.setText(chosen.getPath());
				}
			});

			Button chooseRobocodeDirectory = new Button("Escolher...");
			chooseRobocodeDirectory.setOnAction(event -> {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				directoryChooser.setTitle("Escolha o diretório onde está instalado o robocode");
				File chosen = directoryChooser.showDialog(GameWindow.getWindow());
				if (chosen != null) {
					this.robocodeDirectory.setText(chosen.getPath());
				}
			});

			GridPane grid = grid();
			grid.add(field("Arquivo do banco de dados SQLite", new HBox(6, this.databaseFile, chooseDatabaseFile)), 0, 0, 2, 1);
			grid.add(field("Diretório do robocode", new HBox(6, this.robocodeDirectory, chooseRobocodeDirectory)), 0, 1, 2, 1);
			return new Tab("Sistema", grid);
		}

		/**
		 * What is wrong in the form, checked in the order the tabs are: database,
		 * robocode folder, rounds, port
		 *
		 * @return null if everything is fine
		 */
		Problem problem() {
			int rounds = Configuration.parseInt(this.robocodeRounds.getText(), -1);
			int port = Configuration.parseInt(this.webserverPort.getText(), -1);
			String robocodePath = this.robocodeDirectory.getText().trim();

			if (this.databaseFile.getText().isBlank() || Files.isDirectory(Path.of(this.databaseFile.getText().trim()))) {
				return new Problem("Informe o arquivo do banco de dados", this.systemTab);
			}
			if (robocodePath.isEmpty() || !Files.isDirectory(Path.of(robocodePath, "robots"))) {
				return new Problem("Escolha o diretório onde o robocode está instalado (o que contém o diretório robots)", this.systemTab);
			}
			if (rounds < 1) {
				return new Problem("O número de partidas deve ser um número maior que zero", this.battleTab);
			}
			if (port < 1 || port > 65535) {
				return new Problem("A porta deve ser um número entre 1 e 65535", this.startTab);
			}
			return null;
		}

		/**
		 * The configuration as typed. Only valid after {@link #problem()} is null.
		 */
		Properties properties() {
			Properties properties = new Properties();
			properties.setProperty(ROBOCODE_ROUNDS, String.valueOf(Configuration.parseInt(this.robocodeRounds.getText(), -1)));
			properties.setProperty(WEBSERVER_PORT, String.valueOf(Configuration.parseInt(this.webserverPort.getText(), -1)));
			properties.setProperty(WINNER_CRITERION, (this.winnerCriterion.getSelectionModel().getSelectedIndex() == 0 ? WinnerCriterion.SCORE : WinnerCriterion.RANK).name());
			properties.setProperty(DATABASE_FILE, this.databaseFile.getText().trim());
			properties.setProperty(ROBOCODE_PATH, this.robocodeDirectory.getText().trim());
			properties.setProperty(REGISTRATION_ENABLED, String.valueOf(this.registrationEnabled.isSelected()));
			properties.setProperty(TITLE, this.title.getText().isBlank() ? Configuration.DEFAULT_TITLE : this.title.getText().strip());
			properties.setProperty(SUBTITLE, this.subtitle.getText().strip());
			properties.setProperty(BATTLE_SPEED, String.valueOf(Configuration.BATTLE_SPEEDS[Math.max(0, this.battleSpeed.getSelectionModel().getSelectedIndex())]));
			properties.setProperty(FIGHT_TEXT, this.fightText.getText().isBlank() ? Configuration.DEFAULT_FIGHT_TEXT : this.fightText.getText().strip());
			int arenaIndex = this.arena.getSelectionModel().getSelectedIndex();
			properties.setProperty(ARENA, arenaIndex <= 0 ? Configuration.RANDOM : ArenaStyle.values()[arenaIndex - 1].name());
			int musicIndex = this.music.getSelectionModel().getSelectedIndex();
			properties.setProperty(BATTLE_MUSIC, musicIndex <= 0 ? Configuration.RANDOM : Song.values()[musicIndex - 1].name());
			properties.setProperty(ANTIALIASING, String.valueOf(this.antialiasing.isSelected()));
			properties.setProperty(ARENA_MUSIC, String.valueOf(this.arenaMusic.isSelected()));
			properties.setProperty(MUSIC_VOLUME, String.valueOf((int) Math.round(this.musicVolume.getValue())));
			properties.setProperty(BATTLE_VIEW, this.battleView.getSelectionModel().getSelectedIndex() == 1 ? "CLASSIC" : "3D");
			return properties;
		}
	}

	/**
	 * The position of the configured speed in {@link Configuration#BATTLE_SPEEDS}
	 */
	private static int speedIndex(String configured) {
		double speed = Configuration.DEFAULT_BATTLE_SPEED;
		try {
			speed = configured == null ? speed : Double.parseDouble(configured.trim());
		} catch (NumberFormatException numberFormatException) {
			// The default one
		}
		int best = 0;
		for (int index = 0; index < Configuration.BATTLE_SPEEDS.length; index++) {
			if (Math.abs(Configuration.BATTLE_SPEEDS[index] - speed) < Math.abs(Configuration.BATTLE_SPEEDS[best] - speed)) {
				best = index;
			}
		}
		return best;
	}

	/**
	 * The position of the configured option, after the random one (0)
	 */
	private static int choiceIndex(String configured, Enum<?>[] options) {
		for (int index = 0; index < options.length; index++) {
			if (options[index].name().equals(configured)) {
				return index + 1;
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
