/**
 *
 */
package widgets.windows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.configuration.Configuration;
import utils.configuration.INewConfigurationHandle;
import utils.robocode.BattleControl.WinnerCriterion;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class NewConfigurationHandleWindow implements INewConfigurationHandle {

	/**
	 * The answer of the window, null when cancelled
	 */
	private Properties result;

	@Override
	public Properties askForProperties(Properties oldProperties) {

		Properties old = oldProperties == null ? new Properties() : oldProperties;
		this.result = null;

		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setTitle("Entre com as configurações");

		CheckBox registrationEnabled = new CheckBox("Aceitar novos robôs");
		registrationEnabled.setSelected(Boolean.parseBoolean(old.getProperty(REGISTRATION_ENABLED, "true")));

		TextField robocodeRounds = new TextField(old.getProperty(ROBOCODE_ROUNDS, String.valueOf(Configuration.DEFAULT_ROBOCODE_ROUNDS)));
		Label lblrobocodeRounds = new Label("Rounds por batalha");

		ChoiceBox<String> winnerCriterion = new ChoiceBox<String>();
		winnerCriterion.getItems().addAll("Rank do robocode (mais rounds vencidos), pontuação desempata", "Pontuação total, rounds vencidos desempatam");
		winnerCriterion.getSelectionModel().select(WinnerCriterion.SCORE.name().equals(old.getProperty(WINNER_CRITERION)) ? 1 : 0);
		Label lblwinnerCriterion = new Label("Vencedor da batalha");

		TextField webserverPort = new TextField(old.getProperty(WEBSERVER_PORT, String.valueOf(Configuration.DEFAULT_WEBSERVER_PORT)));
		Label lblwebserverPort = new Label("Porta do servidor web de cadastro (requer reiniciar)");

		TextField databaseFile = new TextField(old.getProperty(DATABASE_FILE, Configuration.DEFAULT_DATABASE_FILE));
		databaseFile.setPromptText(Configuration.DEFAULT_DATABASE_FILE);
		HBox.setHgrow(databaseFile, Priority.ALWAYS);
		Button chooseDatabaseFile = new Button("Escolher...");
		chooseDatabaseFile.setOnAction(event -> {
			FileChooser fc = new FileChooser();
			fc.setTitle("Arquivo do banco de dados (é criado se não existir)");
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Banco SQLite", "*.db", "*.sqlite", "*.sqlite3"));
			File chosen = fc.showSaveDialog(stage);
			if (chosen != null) {
				databaseFile.setText(chosen.getPath());
			}
		});
		Label lbldatabaseFile = new Label("Arquivo do banco de dados SQLite (requer reiniciar)");

		TextField robocodeDirectory = new TextField(old.getProperty(ROBOCODE_PATH, ""));
		robocodeDirectory.setPromptText("Caminho do diretório do robocode");
		HBox.setHgrow(robocodeDirectory, Priority.ALWAYS);
		Button chooseRobocodeDirectory = new Button("Escolher...");
		chooseRobocodeDirectory.setOnAction(event -> {
			DirectoryChooser dc = new DirectoryChooser();
			dc.setTitle("Escolha o diretório onde está instalado o robocode");
			File chosen = dc.showDialog(stage);
			if (chosen != null) {
				robocodeDirectory.setText(chosen.getPath());
			}
		});
		Label lblrobocodeDirectory = new Label("Diretório do robocode");

		Label error = new Label();
		error.getStyleClass().add("error-text");
		error.setWrapText(true);

		Button save = new Button("Salvar");
		save.setDefaultButton(true);
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

			this.result = properties;
			stage.close();
		});

		Button cancel = new Button("Cancelar");
		cancel.setCancelButton(true);
		cancel.setOnAction(event -> stage.close());

		VBox vb = new VBox(10);
		vb.setPadding(new Insets(20));
		vb.getStyleClass().add("form");
		Label title = new Label("CONFIGURAÇÕES");
		title.getStyleClass().add("form-title");
		vb.getChildren().addAll(title, registrationEnabled, lblrobocodeRounds, robocodeRounds, lblwinnerCriterion, winnerCriterion, lblwebserverPort, webserverPort, lbldatabaseFile, new HBox(5, databaseFile, chooseDatabaseFile), lblrobocodeDirectory, new HBox(5, robocodeDirectory, chooseRobocodeDirectory), error, new HBox(10, save, cancel));

		// Field captions
		vb.getChildren().stream().filter(node -> node instanceof Label && node != error && node != title).forEach(node -> node.getStyleClass().add("form-label"));

		stage.setScene(Theme.apply(new Scene(vb, 600, 580)));
		// Otherwise the checkbox gets the focus and pressing Enter toggles it
		// instead of saving
		stage.setOnShown(event -> save.requestFocus());
		stage.showAndWait();

		return this.result;
	}

	private static int parseInt(String value, int defaultValue) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NullPointerException | NumberFormatException e) {
			return defaultValue;
		}
	}
}
