package widgets.scenes;

import java.io.IOException;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import widgets.Sound;
import utils.configuration.Configuration;
import utils.network.Network;
/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class StartScreen extends Scene {

	private ImageView robocodeLogo;

	private AnchorPane anchorPane;
	private StackPane stackPane;
	private Sound music;

	public StartScreen() {
		super(new StackPane());

		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

		try {
			this.anchorPane = new AnchorPane();
			Label lblAtalhos = new Label(
			"i = Tela inicial\t" +
			"ctrl + g = Sorteia um novo campeonato\t" +
			"ctrl + s = Habilita ou desabilita o cadastro de novos robôs\t" +
			"r = Chaveamento\n" +
			"b = Tela de \"versus\" da partida selecionada\t" +
			"s = Inicia a batalha\t" +
			"1 / 2 = Declara o vencedor\t" +
			"ctrl + z = Desfaz o último resultado\t" +
			"m = Detalhes dos robôs da partida\t" +
			"x = Exporta o código fonte dos robôs"
			);
			lblAtalhos.setTextFill(Color.LIGHTGRAY);
			AnchorPane.setTopAnchor(lblAtalhos, 20d);
			AnchorPane.setLeftAnchor(lblAtalhos, 20d);
			this.anchorPane.getChildren().add(lblAtalhos);
			
			Label lblEndereco = new Label("http://" + Network.getLocalIpAddress() + ":" + Configuration.getWebserverPort());
			Font f = new Font(50);
			lblEndereco.setWrapText(false);
			lblEndereco.setFont(f);
			lblEndereco.setTextFill(Color.YELLOW);
			lblEndereco.setTextAlignment(TextAlignment.CENTER);
			AnchorPane.setBottomAnchor(lblEndereco, 20d);
			AnchorPane.setLeftAnchor(lblEndereco, 20d);
			AnchorPane.setRightAnchor(lblEndereco, 20d);
			this.anchorPane.getChildren().add(lblEndereco);
			
			this.stackPane = new StackPane();
			this.stackPane.setPrefWidth(primaryScreenBounds.getWidth());
			this.stackPane.setPrefHeight(primaryScreenBounds.getHeight());

			this.robocodeLogo = new ImageView();
			this.robocodeLogo.setImage(new Image(getClass().getResource("robocodeLogo.png").openStream()));
			this.robocodeLogo.effectProperty().set(new Reflection());
			this.robocodeLogo.setPreserveRatio(true);

			this.music = Sound.fromResource(getClass(), "versusMusic.mp3");

			if (this.music != null) {
				this.music.setVolume(0.15);
				this.music.setLoop(true);
			}
		} catch (IOException e) {
			new Alert(AlertType.ERROR, e.getMessage()).showAndWait();
			return;
		}

		this.stackPane.getChildren().addAll(this.anchorPane, this.robocodeLogo);
		this.stackPane.setStyle("-fx-background-color: #000;");

		this.setRoot(this.stackPane);
	}

	public void playBackgroundMusic() {
		if (this.music != null)
			this.music.play();
	}

	public void stop() {
		if (this.music != null)
			this.music.stop();
	}
}
