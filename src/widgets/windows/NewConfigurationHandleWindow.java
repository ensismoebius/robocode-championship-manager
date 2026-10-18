/**
 *
 */
package widgets.windows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
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
import utils.robocode.BattleControl.WinnerCriterion;
import widgets.GameDialog;
import widgets.GameWindow;

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

		TextField robocodeRounds = new TextField(old.getProperty(ROBOCODE_ROUNDS, String.valueOf(Configuration.DEFAULT_ROBOCODE_ROUNDS)));

		ChoiceBox<String> winnerCriterion = new ChoiceBox<String>();
		winnerCriterion.getItems().addAll("Mais rounds vencidos, pontuação total desempata", "Pontuação total, rounds vencidos desempatam");
		winnerCriterion.getSelectionModel().select(WinnerCriterion.SCORE.name().equals(old.getProperty(WINNER_CRITERION)) ? 1 : 0);
		winnerCriterion.setMaxWidth(Double.MAX_VALUE);

		ChoiceBox<String> battleView = new ChoiceBox<String>();
		battleView.getItems().addAll("3D, no gerenciador", "Janela clássica do robocode");
		battleView.getSelectionModel().select("CLASSIC".equals(old.getProperty(BATTLE_VIEW)) ? 1 : 0);
		battleView.setMaxWidth(Double.MAX_VALUE);

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
		robocodeDirectory.setPromptText("Caminho do diretório do robocode");
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

		// Two columns, so it fits on short screens
		GridPane form = new GridPane();
		form.setHgap(24);
		form.setVgap(14);
		ColumnConstraints half = new ColumnConstraints();
		half.setPercentWidth(50);
		form.getColumnConstraints().addAll(half, half);
		form.add(field("Título da tela inicial", title), 0, 0);
		form.add(field("Subtítulo da tela inicial", subtitle), 1, 0);
		form.add(field("Rounds por batalha", robocodeRounds), 0, 1);
		form.add(field("Vencedor da batalha", winnerCriterion), 1, 1);
		form.add(field("Porta do servidor web de cadastro", webserverPort), 0, 2);
		form.add(field("Inscrições", registrationEnabled), 1, 2);
		form.add(field("Exibição da batalha", battleView), 0, 3);
		form.add(field("Arquivo do banco de dados SQLite", new HBox(6, databaseFile, chooseDatabaseFile)), 0, 4, 2, 1);
		form.add(field("Diretório do robocode", new HBox(6, robocodeDirectory, chooseRobocodeDirectory)), 0, 5, 2, 1);
		form.add(error, 0, 6, 2, 1);

		Button save = new Button("Salvar");
		save.getStyleClass().add("primary");
		Button cancel = new Button("Cancelar");
		VBox panel = GameDialog.panel("Configurações", form, cancel, save);
		panel.setStyle("-fx-pref-width: 940;");

		save.setOnAction(event -> {
			int rounds = parseInt(robocodeRounds.getText(), -1);
			int port = parseInt(webserverPort.getText(), -1);
			String robocodePath = robocodeDirectory.getText().trim();

			if (databaseFile.getText().isBlank() || Files.isDirectory(Path.of(databaseFile.getText().trim()))) {
				error.setText("Informe o arquivo do banco de dados");
				return;
			}
			if (robocodePath.isEmpty() || !Files.isDirectory(Path.of(robocodePath, "robots"))) {
				error.setText("Escolha o diretório onde o robocode está instalado (o que contém o diretório robots)");
				return;
			}
			if (rounds < 1) {
				error.setText("O número de rounds deve ser um número maior que zero");
				return;
			}
			if (port < 1 || port > 65535) {
				error.setText("A porta deve ser um número entre 1 e 65535");
				return;
			}

			Properties properties = new Properties();
			properties.setProperty(ROBOCODE_ROUNDS, String.valueOf(rounds));
			properties.setProperty(WEBSERVER_PORT, String.valueOf(port));
			properties.setProperty(WINNER_CRITERION, (winnerCriterion.getSelectionModel().getSelectedIndex() == 1 ? WinnerCriterion.SCORE : WinnerCriterion.RANK).name());
			properties.setProperty(DATABASE_FILE, databaseFile.getText().trim());
			properties.setProperty(ROBOCODE_PATH, robocodePath);
			properties.setProperty(REGISTRATION_ENABLED, String.valueOf(registrationEnabled.isSelected()));
			properties.setProperty(TITLE, title.getText().isBlank() ? Configuration.DEFAULT_TITLE : title.getText().strip());
			properties.setProperty(SUBTITLE, subtitle.getText().strip());
			properties.setProperty(BATTLE_VIEW, battleView.getSelectionModel().getSelectedIndex() == 1 ? "CLASSIC" : "3D");

			GameWindow.close(panel, properties);
		});
		cancel.setOnAction(event -> GameWindow.close(panel, null));

		// The save button gets the focus, so Enter saves
		return (Properties) GameWindow.showModal(panel, save, cancel);
	}

	/**
	 * A caption above the field
	 */
	private static VBox field(String caption, Node input) {
		Label label = new Label(caption);
		label.getStyleClass().add("form-label");
		return new VBox(6, label, input);
	}

	private static int parseInt(String value, int defaultValue) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NullPointerException | NumberFormatException e) {
			return defaultValue;
		}
	}
}
