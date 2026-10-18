package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import widgets.Sound;
/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class BattleViewer extends Scene {

	private ImageView vsImage;
	private ImageView oponentImage01;
	private ImageView oponentImage02;

	private Sound versusMusic;
	private AnchorPane anchorPane;
	private StackPane stackPane;

	private final double margin;

	public BattleViewer() {
		super(new StackPane());

		this.margin = 10d;

		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

		this.oponentImage01 = new ImageView();
		this.oponentImage01.effectProperty().set(new Reflection());
		this.oponentImage02 = new ImageView();
		this.oponentImage02.effectProperty().set(new Reflection());

		try {
			this.anchorPane = new AnchorPane();
			this.stackPane = new StackPane();
			this.stackPane.setPrefWidth(primaryScreenBounds.getWidth());
			this.stackPane.setPrefHeight(primaryScreenBounds.getHeight());

			this.vsImage = new ImageView();
			this.vsImage.setImage(new Image(getClass().getResource("vs.gif").openStream()));
			this.vsImage.effectProperty().set(new Reflection());
			this.vsImage.fitWidthProperty().bind(this.widthProperty().divide(5));
			this.vsImage.fitHeightProperty().bind(this.heightProperty().divide(5));

			this.versusMusic = Sound.fromResource(getClass(), "versusMusic.mp3");
		} catch (IOException e) {
			new Alert(AlertType.ERROR, e.getMessage()).showAndWait();
			return;
		}

		AnchorPane.setTopAnchor(oponentImage01, this.margin);
		AnchorPane.setBottomAnchor(oponentImage01, this.margin);
		AnchorPane.setLeftAnchor(oponentImage01, this.margin);
		this.anchorPane.getChildren().add(this.oponentImage01);

		AnchorPane.setTopAnchor(oponentImage02, this.margin);
		AnchorPane.setBottomAnchor(oponentImage02, this.margin);
		AnchorPane.setRightAnchor(oponentImage02, this.margin);
		this.anchorPane.getChildren().add(this.oponentImage02);

		this.stackPane.getChildren().addAll(this.anchorPane, this.vsImage);
		this.stackPane.setStyle("-fx-background-color: #000;");

		this.setRoot(this.stackPane);
	}

	public void setOpponents(model.Opponent oponent01, model.Opponent oponent02) {
		this.oponentImage01.setSmooth(true);
		this.oponentImage01.preserveRatioProperty().set(true);
		this.oponentImage01.fitWidthProperty().bind(this.widthProperty().divide(2).subtract(this.margin * 2));
		this.oponentImage01.setImage(toImage(oponent01.getImage()));

		this.oponentImage02.setSmooth(true);
		this.oponentImage02.preserveRatioProperty().set(true);
		this.oponentImage02.fitWidthProperty().bind(this.widthProperty().divide(2).subtract(this.margin * 2));
		this.oponentImage02.setImage(toImage(oponent02.getImage()));
	}

	private static Image toImage(byte[] bytes) {
		return bytes == null ? null : new Image(new ByteArrayInputStream(bytes));
	}

	public void play() {
		if (this.versusMusic != null)
			this.versusMusic.play();
	}

	public void stop() {
		if (this.versusMusic != null)
			this.versusMusic.stop();
	}
}
