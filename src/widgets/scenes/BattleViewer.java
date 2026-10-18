package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import model.Opponent;
import widgets.ArcadeBackground;
import widgets.Sfx;
import widgets.Sound;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The "versus" screen, like the ones of fighting games: the robots come from
 * both sides and the VS slams the screen
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class BattleViewer extends StackPane {

	private final BorderPane layout = new BorderPane();
	private final ImageView vsImage = new ImageView();
	private final Region flash = new Region();
	private VBox player1;
	private VBox player2;
	private Sound versusMusic;

	public BattleViewer() {
		super();

		try (InputStream vs = getClass().getResourceAsStream("vs.gif")) {
			this.vsImage.setImage(new Image(vs));
		} catch (IOException e) {
			Theme.alert(AlertType.ERROR, e.getMessage()).showAndWait();
		}
		this.vsImage.setPreserveRatio(true);
		this.vsImage.fitWidthProperty().bind(this.widthProperty().multiply(0.16));
		this.vsImage.setEffect(new Reflection(0, 0.4, 0.5, 0));
		// Blends the black background of the image with the background
		this.vsImage.setBlendMode(BlendMode.LIGHTEN);

		this.versusMusic = Sound.fromResource(getClass(), "versusMusic.mp3");

		this.layout.setPadding(new Insets(30, 0, 30, 0));

		this.flash.getStyleClass().add("flash");
		this.flash.setOpacity(0);
		this.flash.setMouseTransparent(true);

		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		scanlines.setMouseTransparent(true);

		this.getChildren().setAll(new ArcadeBackground(Color.web("#ff3d6e")), this.layout, this.flash, scanlines);
	}

	/**
	 * @param stage
	 *            name of the round, like "Semifinal"
	 * @param code
	 *            code of the match
	 */
	public void setOpponents(Opponent opponent1, Opponent opponent2, String stage, String code) {
		Label stageLabel = new Label(stage.toUpperCase());
		stageLabel.getStyleClass().add("versus-stage");
		stageLabel.setEffect(Theme.glow(Theme.ACCENT, 12));
		Label codeLabel = new Label("PARTIDA " + code);
		codeLabel.getStyleClass().add("versus-code");
		VBox header = new VBox(10, stageLabel, codeLabel);
		header.setAlignment(Pos.CENTER);

		this.player1 = this.fighter(opponent1, "P1", "p1");
		this.player2 = this.fighter(opponent2, "P2", "p2");
		HBox.setHgrow(this.player1, Priority.ALWAYS);
		HBox.setHgrow(this.player2, Priority.ALWAYS);
		this.player1.setMaxWidth(Double.MAX_VALUE);
		this.player2.setMaxWidth(Double.MAX_VALUE);
		HBox fighters = new HBox(this.player1, this.vsImage, this.player2);
		fighters.setAlignment(Pos.CENTER);

		Label key = new Label("S");
		key.getStyleClass().add("keycap");
		Label before = new Label("PRESSIONE");
		before.getStyleClass().add("press-start");
		Label after = new Label("PARA LUTAR!");
		after.getStyleClass().add("press-start");
		HBox prompt = new HBox(16, before, key, after);
		prompt.setAlignment(Pos.CENTER);
		Theme.blink(prompt);

		this.layout.setTop(header);
		this.layout.setCenter(fighters);
		this.layout.setBottom(prompt);
	}

	/**
	 * Tag, logo, name and team of one robot
	 */
	private VBox fighter(Opponent opponent, String tag, String styleClass) {
		Label playerTag = new Label(tag);
		playerTag.getStyleClass().add("player-tag");

		ImageView logo = new ImageView();
		logo.setSmooth(true);
		logo.setPreserveRatio(true);
		logo.fitWidthProperty().bind(this.widthProperty().multiply(0.3));
		logo.fitHeightProperty().bind(this.heightProperty().multiply(0.42));
		logo.setEffect(new Reflection(0, 0.3, 0.35, 0));
		if (opponent.getImage() != null) {
			logo.setImage(new Image(new ByteArrayInputStream(opponent.getImage())));
		}

		Label name = new Label(opponent.getName().toUpperCase());
		name.getStyleClass().add("fighter-name");
		name.setEffect(Theme.glow(styleClass.equals("p1") ? Theme.PLAYER_1 : Theme.PLAYER_2, 16));
		Label team = new Label(opponent.getTeam());
		team.getStyleClass().add("fighter-team");

		VBox fighter = new VBox(playerTag, logo, name, team);
		fighter.getStyleClass().addAll("fighter-panel", styleClass);
		return fighter;
	}

	/**
	 * The entrance animation and the music
	 */
	public void play() {
		if (this.versusMusic != null)
			this.versusMusic.play();

		if (this.player1 == null) {
			return;
		}

		double distance = Math.max(this.getWidth(), 2400);

		TranslateTransition in1 = new TranslateTransition(Duration.millis(450), this.player1);
		in1.setFromX(-distance);
		in1.setToX(0);
		in1.setInterpolator(Interpolator.EASE_OUT);
		TranslateTransition in2 = new TranslateTransition(Duration.millis(450), this.player2);
		in2.setFromX(distance);
		in2.setToX(0);
		in2.setInterpolator(Interpolator.EASE_OUT);

		this.vsImage.setOpacity(0);
		ScaleTransition slam = new ScaleTransition(Duration.millis(220), this.vsImage);
		slam.setFromX(5);
		slam.setFromY(5);
		slam.setToX(1);
		slam.setToY(1);
		slam.setInterpolator(Interpolator.EASE_IN);
		FadeTransition appear = new FadeTransition(Duration.millis(220), this.vsImage);
		appear.setFromValue(0);
		appear.setToValue(1);
		ParallelTransition vs = new ParallelTransition(slam, appear);
		vs.setOnFinished(event -> {
			Sfx.slam();
			this.flashAndShake();
		});

		new SequentialTransition(new ParallelTransition(in1, in2), new PauseTransition(Duration.millis(150)), vs).play();
	}

	private void flashAndShake() {
		FadeTransition flash = new FadeTransition(Duration.millis(350), this.flash);
		flash.setFromValue(0.85);
		flash.setToValue(0);
		flash.play();

		Timeline shake = new Timeline();
		double[] offsets = { 14, -12, 9, -7, 4, -2, 0 };
		for (int i = 0; i < offsets.length; i++) {
			shake.getKeyFrames().add(new KeyFrame(Duration.millis(40 * (i + 1)), new KeyValue(this.layout.translateXProperty(), offsets[i])));
		}
		shake.play();
	}

	public void stop() {
		if (this.versusMusic != null)
			this.versusMusic.stop();
	}
}
