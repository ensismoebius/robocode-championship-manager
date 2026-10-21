package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.Author;
import model.Opponent;
import widgets.ArcadeBackground;
import widgets.Confetti;
import widgets.Sfx;
import widgets.Sound;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Presents a robot: its profile, the winner of a match or the champion
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class ShowOponent extends StackPane {

	private Sound mp;
	private ImageView oponentImage;
	private Label titleLabel;
	private boolean champion;

	public ShowOponent() {
		super();
	}

	public void setOponent(Opponent oponent) throws IOException {
		this.setOponent(oponent, null, false);
	}

	public void setOponent(Opponent oponent, String title) throws IOException {
		this.setOponent(oponent, title, false);
	}

	/**
	 * @param title
	 *            shown above the name, like "VENCEU!" or "CAMPEÃO!", may be null
	 * @param champion
	 *            celebrates with confetti and a fanfare
	 */
	public void setOponent(Opponent oponent, String title, boolean champion) throws IOException {
		this.champion = champion;
		this.mp = Sound.fromBytes(oponent.getMusic(), oponent.getFullName());

		VBox info = info(oponent, title);
		this.oponentImage = image(oponent);
		HBox content = new HBox(60, this.oponentImage, info);
		content.setAlignment(Pos.CENTER);
		content.setPadding(new Insets(40, 60, 40, 60));
		info.maxWidthProperty().bind(this.widthProperty().multiply(0.45));

		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		scanlines.setMouseTransparent(true);

		this.getChildren().setAll(new ArcadeBackground(champion ? Color.web("#ffd400") : Color.web("#5af08f")), sourceBackground(oponent), content, scanlines);

		// After a result the bracket shows where the robots go
		if (title != null) {
			this.getChildren().add(bracketPrompt());
		}
		if (champion) {
			this.getChildren().add(new Confetti());
		}
	}

	/** The title, the name, the team, the battle cry and the team of the robot */
	private VBox info(Opponent oponent, String title) {
		Label name = new Label(oponent.getName().toUpperCase());
		name.setWrapText(true);
		name.getStyleClass().add("showcase-name");
		name.setEffect(Theme.glow(Theme.PLAYER_1, 18));

		Label team = new Label("Time " + oponent.getTeam());
		team.getStyleClass().add("showcase-team");

		Label battleCry = new Label("“" + oponent.getBattleCry() + "”");
		battleCry.setWrapText(true);
		battleCry.getStyleClass().add("showcase-cry");

		VBox info = new VBox(10);
		info.setAlignment(Pos.CENTER_LEFT);
		if (title != null) {
			this.titleLabel = new Label(title);
			this.titleLabel.getStyleClass().add("showcase-title");
			Theme.pulseGlow(this.titleLabel, Theme.ACCENT);
			info.getChildren().add(this.titleLabel);
		}
		info.getChildren().addAll(name, team, battleCry);

		if (!oponent.getAuthors().isEmpty()) {
			Label section = new Label("EQUIPE");
			section.getStyleClass().add("showcase-section");
			info.getChildren().add(section);
			for (Author author : oponent.getAuthors()) {
				Label lblAuthor = new Label("▸ " + author.getName());
				lblAuthor.getStyleClass().add("showcase-author");
				info.getChildren().add(lblAuthor);
			}
		}
		return info;
	}

	/** The robot picture, with its reflection, the size of the screen */
	private ImageView image(Opponent oponent) {
		ImageView image = new ImageView();
		image.setSmooth(true);
		image.setPreserveRatio(true);
		image.fitHeightProperty().bind(this.heightProperty().multiply(0.6));
		image.fitWidthProperty().bind(this.widthProperty().multiply(0.42));
		image.setEffect(new Reflection(0, 0.35, 0.4, 0));
		if (oponent.getImage() != null) {
			image.setImage(new Image(new ByteArrayInputStream(oponent.getImage())));
		}
		return image;
	}

	/** The source code of the robot scrolling in the background, like a hacker screen */
	private Pane sourceBackground(Opponent oponent) {
		Label source = new Label(oponent.getSourceCode() == null ? "" : oponent.getSourceCode());
		source.getStyleClass().add("showcase-source");
		Pane sourcePane = new Pane(source);
		sourcePane.setMouseTransparent(true);
		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(this.widthProperty());
		clip.heightProperty().bind(this.heightProperty());
		sourcePane.setClip(clip);
		source.layoutXProperty().bind(this.widthProperty().subtract(source.widthProperty()).subtract(40));
		TranslateTransition scroll = new TranslateTransition(Duration.seconds(60), source);
		scroll.setFromY(0);
		scroll.setToY(-2000);
		scroll.setCycleCount(Animation.INDEFINITE);
		scroll.play();
		return sourcePane;
	}

	/** Blinking "PRESSIONE R PARA O CHAVEAMENTO" at the bottom */
	private HBox bracketPrompt() {
		Label key = new Label("R");
		key.getStyleClass().add("keycap");
		Label before = new Label("PRESSIONE");
		before.getStyleClass().add("press-start");
		Label after = new Label("PARA O CHAVEAMENTO");
		after.getStyleClass().add("press-start");
		HBox prompt = new HBox(16, before, key, after);
		prompt.setAlignment(Pos.CENTER);
		prompt.setMaxHeight(Region.USE_PREF_SIZE);
		prompt.setMouseTransparent(true);
		StackPane.setAlignment(prompt, Pos.BOTTOM_CENTER);
		StackPane.setMargin(prompt, new Insets(0, 0, 36, 0));
		Theme.blink(prompt);
		return prompt;
	}

	public void play() {
		if (this.oponentImage != null) {
			TranslateTransition slide = new TranslateTransition(Duration.millis(500), this.oponentImage);
			slide.setFromX(-Math.max(this.getWidth(), 2000));
			slide.setToX(0);
			slide.setInterpolator(Interpolator.EASE_OUT);
			slide.play();
		}

		if (this.titleLabel != null) {
			ScaleTransition pop = new ScaleTransition(Duration.millis(450), this.titleLabel);
			pop.setFromX(0);
			pop.setFromY(0);
			pop.setToX(1);
			pop.setToY(1);
			pop.setInterpolator(Theme.OVERSHOOT);
			pop.play();

			if (this.champion) {
				Sfx.fanfare();
			} else {
				Sfx.victory();
			}
		}

		if (this.mp != null)
			this.mp.play();
	}

	public void stop() {
		if (this.mp != null)
			this.mp.stop();
	}
}
