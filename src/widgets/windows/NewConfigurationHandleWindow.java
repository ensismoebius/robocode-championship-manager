/**
 * 
 */
package widgets.windows;

import java.util.Properties;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.configuration.NewConfigurationHandle;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class NewConfigurationHandleWindow extends Stage implements NewConfigurationHandle {

	private Button save;
	private TextField databaseURL;
	private TextField databaseUser;
	private TextField databasePassword;

	private Label lbldatabaseURL;
	private Label lbldatabaseUser;
	private Label lbldatabasePassword;

	@Override
	public Properties askForProperties() {

		this.save = new Button("Salvar");
		this.save.setOnAction(event -> this.close());

		this.databaseURL = new TextField();
		this.databaseURL.setPromptText("Endereço do banco de dados");

		this.databaseUser = new TextField();
		this.databaseUser.setPromptText("Usuário do banco de dados");

		this.databasePassword = new TextField();
		this.databasePassword.setPromptText("Senha do banco de dados");

		this.lbldatabaseURL = new Label("Endereço do banco de dados");
		this.lbldatabaseUser = new Label("Usuário do banco de dados");
		this.lbldatabasePassword = new Label("Senha do banco de dados");

		VBox vb = new VBox(10);
		vb.getChildren().addAll(lbldatabaseURL, databaseURL, lbldatabaseUser, databaseUser, lbldatabasePassword,
				databasePassword, save);
		this.setScene(new Scene(vb, 320, 240));
		this.showAndWait();

		if (this.databaseURL.getText().trim().equals("")) {
			return null;
		}
		if (this.databaseUser.getText().trim().equals("")) {
			return null;
		}
		if (this.databasePassword.getText().trim().equals("")) {
			return null;
		}

		Properties properties = new Properties();
		properties.put("databaseURL", this.databaseURL.getText().trim());
		properties.put("databaseUser", this.databaseUser.getText().trim());
		properties.put("databasePassword", this.databasePassword.getText().trim());

		return properties;
	}
}
