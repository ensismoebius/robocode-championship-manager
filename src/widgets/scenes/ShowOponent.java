package widgets.scenes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sun.xml.messaging.saaj.util.ByteInputStream;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import model.Author;
import utils.configuration.Configuration;
/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class ShowOponent extends Scene {

	public ShowOponent() {
		super(new StackPane());
	}

	private ImageView oponentImage;

	private HBox hbox;
	private StackPane stackPane;

	private Label name;
	private Label battleCry;

	private MediaPlayer mp;

	public void setOponent(model.Opponent oponent) throws FileNotFoundException, IOException {

		this.mp = null;

		// Create a temp music file and play it!
		File file = new File(Configuration.getTempDir() + "/temp.mp3");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(oponent.getMusic());
		fos.close();
		this.mp = new MediaPlayer(new Media(file.toURI().toString()));

		Font f = new Font(50);
		this.name = new Label();
		this.name.setWrapText(true);
		this.name.setFont(f);
		this.name.setTextFill(Color.YELLOW);
		this.name.setTextAlignment(TextAlignment.CENTER);
		this.name.effectProperty().set(new Reflection());

		f = new Font(40);
		this.battleCry = new Label();
		this.battleCry.setPadding(new Insets(50, 0, 100, 0));
		this.battleCry.setWrapText(true);
		this.battleCry.setFont(f);
		this.battleCry.setTextFill(Color.YELLOWGREEN);
		this.battleCry.setTextAlignment(TextAlignment.CENTER);
		this.battleCry.effectProperty().set(new javafx.scene.effect.Lighting());

		this.hbox = new HBox(20);
		this.stackPane = new StackPane();
		this.stackPane.prefWidthProperty().bind(this.widthProperty());

		this.oponentImage = new ImageView();
		this.oponentImage.setSmooth(true);
		this.oponentImage.preserveRatioProperty().set(true);
		this.oponentImage.maxWidth(this.widthProperty().divide(2).get());
		this.oponentImage.fitHeightProperty().bind(this.heightProperty());
		this.oponentImage.fitWidthProperty().bind(this.widthProperty().divide(2));
		this.oponentImage.setImage(new Image(new ByteInputStream(oponent.getImage(), oponent.getImage().length)));

		this.name.setText(oponent.getName());
		this.name.setTextFill(Color.YELLOWGREEN);
		this.name.setTextAlignment(TextAlignment.CENTER);

		this.battleCry.setText(oponent.getBattleCry());
		this.battleCry.setTextFill(Color.YELLOWGREEN);
		this.battleCry.setTextAlignment(TextAlignment.CENTER);

		VBox vbox = new VBox(20);
		vbox.getChildren().addAll(name, battleCry);

		f = new Font(30);

		for (Author author : oponent.getAuthors()) {
			Label lblAuthor = new Label(author.getName());
			lblAuthor.setWrapText(false);
			lblAuthor.setFont(f);
			lblAuthor.setTextFill(Color.YELLOWGREEN);
			lblAuthor.setTextAlignment(TextAlignment.CENTER);
			lblAuthor.prefWidthProperty().bind(this.widthProperty().divide(2));
			vbox.getChildren().add(lblAuthor);
		}

		this.hbox.getChildren().addAll(this.oponentImage, vbox);

		Label source = new Label(oponent.getSourceCode());
		source.setTextFill(Color.DARKCYAN);
		source.setFont(new Font(30));
		source.setOpacity(.2);
		source.setTextAlignment(TextAlignment.RIGHT);
		source.prefWidthProperty().bind(this.widthProperty());
		source.prefHeightProperty().bind(this.heightProperty());

		this.stackPane.getChildren().addAll(source, this.hbox);
		this.stackPane.setStyle("-fx-background-color: #000;");

		this.setRoot(this.stackPane);
	}

	public void play() {
		this.mp.play();
	}

	public void stop() {
		this.mp.stop();
	}
}
