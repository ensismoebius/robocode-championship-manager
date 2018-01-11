/**
 * 
 */
package widgets.windows;

import java.util.Properties;

import javafx.scene.Scene;
import javafx.scene.control.Button;
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

	private Label lblgroupsSize;
	private Label lbldatabaseURL;
	private Label lbldatabaseUser;
	private Label lbldatabasePassword;
	private Label lblrobocodeDirectory;

	@Override
	public Properties askForProperties(Properties oldProperties) {

		this.lblgroupsSize = new Label("Quantidade de robôs por grupo");
		this.groupsSize = new Slider();

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
			this.robocodeDirectory.setText(dc.showDialog(null).getPath());
		});
		this.lblrobocodeDirectory = new Label("Diretório do robocode");

		VBox vb = new VBox(10);
		vb.getChildren().addAll(lblgroupsSize, groupsSize, lbldatabaseURL, databaseURL, lbldatabaseUser, databaseUser, lbldatabasePassword, databasePassword, lblrobocodeDirectory, robocodeDirectory, save);
		this.setScene(new Scene(vb));
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
		properties.put(INewConfigurationHandle.GROUPS_SIZE, this.groupsSize.getValue());
		properties.put(INewConfigurationHandle.DATABASE_URL, this.databaseURL.getText().trim());
		properties.put(INewConfigurationHandle.DATABASE_USER, this.databaseUser.getText().trim());
		properties.put(INewConfigurationHandle.DATABASE_PASSWORD, this.databasePassword.getText().trim());
		properties.put(INewConfigurationHandle.ROBOCODE_PATH, this.robocodeDirectory.getText().trim());

		return properties;
	}
}
