package widgets.scenes;

import java.io.IOException;
import java.net.URISyntaxException;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Screen;

public class StartScreen extends Scene {

	private ImageView robocodeLogo;

	private AnchorPane anchorPane;
	private StackPane stackPane;
	private MediaPlayer music;

	public StartScreen() {
		super(new StackPane());

		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

		try {
			this.anchorPane = new AnchorPane();
			this.stackPane = new StackPane();
			this.stackPane.setPrefWidth(primaryScreenBounds.getWidth());
			this.stackPane.setPrefHeight(primaryScreenBounds.getHeight());

			this.robocodeLogo = new ImageView();
			this.robocodeLogo.setImage(new Image(getClass().getResource("robocodeLogo.png").openStream()));
			this.robocodeLogo.effectProperty().set(new Reflection());
			this.robocodeLogo.setPreserveRatio(true);

			this.music = new MediaPlayer(new Media(getClass().getResource("versusMusic.mp3").toURI().toString()));

			// TODO Make it flexible!!
			this.music.setVolume(0.05);
			this.music.setCycleCount(1000);
		} catch (IOException | URISyntaxException e) {
			new Alert(AlertType.ERROR, e.getMessage()).showAndWait();
			return;
		}

		this.stackPane.getChildren().addAll(this.anchorPane, this.robocodeLogo);
		this.stackPane.setStyle("-fx-background-color: #000;");

		this.setRoot(this.stackPane);
	}

	public void play() {
		this.music.play();
	}

	public void stop() {
		this.music.stop();
	}
}
