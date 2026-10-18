package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import model.Author;
import widgets.Sound;
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

	private Sound mp;

	public void setOponent(model.Opponent oponent) throws IOException {
		this.setOponent(oponent, null);
	}

	/**
	 * @param title
	 *            shown above the name, like "VENCEDOR" or "CAMPEÃO", may be null
	 */
	public void setOponent(model.Opponent oponent, String title) throws IOException {

		this.mp = null;

		this.mp = Sound.fromBytes(oponent.getMusic(), oponent.getFullName());

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
		this.oponentImage.fitHeightProperty().bind(this.heightProperty());
		this.oponentImage.fitWidthProperty().bind(this.widthProperty().divide(2));
		if (oponent.getImage() != null) {
			this.oponentImage.setImage(new Image(new ByteArrayInputStream(oponent.getImage())));
		}

		this.name.setText(oponent.getName());
		this.name.setTextFill(Color.YELLOWGREEN);
		this.name.setTextAlignment(TextAlignment.CENTER);

		this.battleCry.setText(oponent.getBattleCry());
		this.battleCry.setTextFill(Color.YELLOWGREEN);
		this.battleCry.setTextAlignment(TextAlignment.CENTER);

		VBox vbox = new VBox(20);
		if (title != null) {
			Label titleLabel = new Label(title);
			titleLabel.setFont(Font.font(null, javafx.scene.text.FontWeight.BOLD, 70));
			titleLabel.setTextFill(Color.GOLD);
			titleLabel.setEffect(new javafx.scene.effect.Glow(0.8));
			vbox.getChildren().add(titleLabel);
		}
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
		if (this.mp != null)
			this.mp.play();
	}

	public void stop() {
		if (this.mp != null)
			this.mp.stop();
	}
}
