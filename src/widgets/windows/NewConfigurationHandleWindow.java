/**
 * 
 */
package widgets.windows;

import java.util.Properties;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import utils.configuration.INewConfigurationHandle;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class NewConfigurationHandleWindow extends Stage implements INewConfigurationHandle {

	private Button save;
	private Slider groupsSize;
	private TextField databaseURL;
	private TextField databaseUser;
	private TextField databasePassword;
	private TextField robocodeDirectory;
	private CheckBox registrationEnabled;

	private Label lblgroupsSize;
	private Label lbldatabaseURL;
	private Label lbldatabaseUser;
	private Label lbldatabasePassword;
	private Label lblrobocodeDirectory;

	@Override
	public Properties askForProperties(Properties oldProperties) {

		if (oldProperties.isEmpty()) {
			oldProperties = null;
		}

		this.registrationEnabled = new CheckBox("Aceitar novos robôs");
		this.registrationEnabled.setSelected(oldProperties == null ? true : Boolean.parseBoolean(oldProperties.getProperty(INewConfigurationHandle.REGISTRATION_ENABLED)));

		this.lblgroupsSize = new Label();
		this.groupsSize = new Slider();
		this.groupsSize.setValue(oldProperties == null ? 1 : Double.parseDouble(oldProperties.getProperty(INewConfigurationHandle.GROUPS_SIZE)));
		// just integers please...
		this.groupsSize.valueProperty().addListener((obs, oldval, newVal) -> this.groupsSize.setValue(newVal.intValue()));
		this.lblgroupsSize.textProperty().bind(this.groupsSize.valueProperty().asString().concat(" robôs por grupo"));

		this.save = new Button("Salvar");
		this.save.setOnAction(event -> this.close());

		this.databaseURL = new TextField();
		this.databaseURL.setPromptText("Endereço do banco de dados");
		this.databaseURL.setText(oldProperties == null ? "" : oldProperties.getProperty(INewConfigurationHandle.DATABASE_URL));
		this.lbldatabaseURL = new Label("Endereço do banco de dados");

		this.databaseUser = new TextField();
		this.databaseUser.setPromptText("Usuário do banco de dados");
		this.databaseUser.setText(oldProperties == null ? "" : oldProperties.getProperty(INewConfigurationHandle.DATABASE_USER));
		this.lbldatabaseUser = new Label("Usuário do banco de dados");

		this.databasePassword = new TextField();
		this.databasePassword.setPromptText("Senha do banco de dados");
		this.databasePassword.setText(oldProperties == null ? "" : oldProperties.getProperty(INewConfigurationHandle.DATABASE_PASSWORD));
		this.lbldatabasePassword = new Label("Senha do banco de dados");

		this.robocodeDirectory = new TextField();
		this.robocodeDirectory.setPromptText("Caminho do diretório do robocode");
		this.robocodeDirectory.setText(oldProperties == null ? "" : oldProperties.getProperty(INewConfigurationHandle.ROBOCODE_PATH));
		this.robocodeDirectory.setOnMouseClicked(event -> {
			DirectoryChooser dc = new DirectoryChooser();
			dc.setTitle("Escolha o diretório onde está instalado o robocode");

			try {
				this.robocodeDirectory.setText(dc.showDialog(null).getPath());
			} catch (Exception e) {
			}
		});
		this.lblrobocodeDirectory = new Label("Diretório do robocode");

		VBox vb = new VBox(10);
		vb.getChildren().addAll(registrationEnabled, lblgroupsSize, groupsSize, lbldatabaseURL, databaseURL, lbldatabaseUser, databaseUser, lbldatabasePassword, databasePassword, lblrobocodeDirectory, robocodeDirectory, save);

		this.setTitle("Entre com as configurações");
		this.setScene(new Scene(vb, 500, 380));
		this.showAndWait();

		if (this.groupsSize.getValue() == 0) {
			return null;
		}

		if (this.databaseURL.getText().trim().equals("")) {
			return null;
		}
		if (this.databaseUser.getText().trim().equals("")) {
			return null;
		}
		if (this.databasePassword.getText().trim().equals("")) {
			return null;
		}

		if (this.robocodeDirectory.getText().trim().equals("")) {
			return null;
		}

		Properties properties = new Properties();
		properties.put(INewConfigurationHandle.GROUPS_SIZE, String.valueOf(this.groupsSize.getValue()));
		properties.put(INewConfigurationHandle.DATABASE_URL, this.databaseURL.getText().trim());
		properties.put(INewConfigurationHandle.DATABASE_USER, this.databaseUser.getText().trim());
		properties.put(INewConfigurationHandle.DATABASE_PASSWORD, this.databasePassword.getText());
		properties.put(INewConfigurationHandle.ROBOCODE_PATH, this.robocodeDirectory.getText().trim());
		properties.put(INewConfigurationHandle.REGISTRATION_ENABLED, String.valueOf(this.registrationEnabled.isSelected()));

		return properties;
	}
}
