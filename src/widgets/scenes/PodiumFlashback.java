package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import model.BattleMoment;
import model.Opponent;
import tournament.Flashbacks;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The flashback of the {@link Podium}: after a robot is revealed, its key moments
 * come as photos, one after another, over the ceremony.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class PodiumFlashback {

	private final Podium host;
	/** The page over the ceremony, hidden until a flashback opens */
	final StackPane pane = new StackPane();
	private final List<Circle> flashbackDots = new ArrayList<Circle>();
	private ImageView flashbackImage;
	private Label flashbackRound;
	private Label flashbackTeam;
	private Label flashbackEvent;
	private PauseTransition flashbackWait;
	private Timeline flashbackCycle;
	private FadeTransition flashbackFade;
	private boolean flashbackOpen = false;

	/** Creates the flashback of the podium: the clips of the battles of a robot. */
	PodiumFlashback(Podium host) {
		this.host = host;
	}

	/** Seconds a photo stays, and the seconds its fades take */
	private static final double FLASHBACK_SLIDE = 2.6;
	private static final double FLASHBACK_FADE = 0.35;
	/** The robot is seen on its place first, then its photos come */
	private static final double FLASHBACK_DELAY = 1.6;

	/**
	 * A photo of the flashback and what it tells about the robot shown
	 */
	private record Clip(Image image, String round, String team, String event) {
	}

	/**
	 * The robot of the place just revealed shows the key moments of its battles,
	 * one photo after another, until the next place or the end of the ceremony
	 */
	void show(int place) {
		Opponent robot = host.places[place];
		List<BattleMoment> chosen = Flashbacks.select(host.moments, robot.getId(), Flashbacks.LIMIT);
		if (chosen.isEmpty()) {
			return;
		}
		this.flashbackWait = new PauseTransition(Duration.seconds(FLASHBACK_DELAY));
		this.flashbackWait.setOnFinished(event -> this.openFlashback(robot, chosen));
		this.flashbackWait.play();
	}

	/** Opens the flashback of a robot with its chosen clips. */
	private void openFlashback(Opponent robot, List<BattleMoment> chosen) {
		this.flashbackOpen = true;
		this.buildFlashback(robot, chosen.size());
		List<Clip> clips = new ArrayList<Clip>();
		for (BattleMoment moment : chosen) {
			clips.add(this.clip(moment, robot));
		}
		if (this.flashbackFade != null) {
			this.flashbackFade.stop();
		}
		this.pane.setVisible(true);
		this.pane.setOpacity(0);
		this.flashbackFade = new FadeTransition(Duration.seconds(0.6), this.pane);
		this.flashbackFade.setToValue(1);
		this.flashbackFade.play();
		this.flashbackCycle = this.cycle(clips);
		this.flashbackCycle.play();
		host.updateHint();
	}

	/**
	 * Closes the flashback at once, or fades it out when it is on screen
	 */
	void hide() {
		if (this.flashbackWait != null) {
			this.flashbackWait.stop();
			this.flashbackWait = null;
		}
		if (this.flashbackCycle != null) {
			this.flashbackCycle.stop();
			this.flashbackCycle = null;
		}
		if (this.flashbackOpen) {
			this.flashbackOpen = false;
			if (this.flashbackFade != null) {
				this.flashbackFade.stop();
			}
			this.flashbackFade = new FadeTransition(Duration.seconds(0.3), this.pane);
			this.flashbackFade.setToValue(0);
			this.flashbackFade.setOnFinished(event -> this.pane.setVisible(false));
			this.flashbackFade.play();
		}
	}

	/**
	 * The photo and what it tells, about the robot shown and its opponent in the
	 * battle: the round, the team and the name of the opponent are always there
	 */
	private Clip clip(BattleMoment moment, Opponent robot) {
		Opponent rival = host.opponents.get(Flashbacks.opponentOf(moment, robot.getId()));
		String string = upper(robot.getName());
		String rivalName = rival == null ? "?" : upper(rival.getName());
		String rivalTeam = rival == null ? "" : "TIME " + upper(rival.getTeam());
		String power = String.format("%.1f", moment.getPower());
		String event;
		if (moment.isDestruction()) {
			event = moment.getSubjectId() == robot.getId() ? string + " FOI DESTRUÍDO" : string + " DESTRUIU " + rivalName;
		} else if (moment.getSubjectId() == robot.getId()) {
			event = string + " ACERTOU " + rivalName + " COM FORÇA " + power;
		} else {
			event = rivalName + " ACERTOU " + string + " COM FORÇA " + power;
		}
		return new Clip(new Image(new ByteArrayInputStream(moment.getImage())), "ROUND " + moment.getRoundNumber() + " · VS " + rivalName, rivalTeam, event);
	}

	/** The text in capital letters, or an empty text when it is null. */
	private static String upper(String text) {
		return text == null ? "" : text.toUpperCase();
	}

	/**
	 * The page of the flashback: a shade over the ceremony, the photo in its frame
	 * and the words under it
	 */
	private void buildFlashback(Opponent robot, int count) {
		this.flashbackDots.clear();

		Region shade = new Region();
		shade.setStyle("-fx-background-color: rgba(6, 4, 16, 0.86);");
		shade.prefWidthProperty().bind(host.widthProperty());
		shade.prefHeightProperty().bind(host.heightProperty());

		Label title = new Label("MOMENTOS DE " + upper(robot.getName()));
		title.getStyleClass().add("podium-banner");
		title.styleProperty().bind(host.heightProperty().multiply(0.05).asString("-fx-font-size: %.0fpx;"));

		StackPane frame = this.flashbackFrame();

		this.flashbackRound = new Label();
		this.flashbackRound.getStyleClass().add("podium-name");
		this.flashbackRound.styleProperty().bind(host.heightProperty().multiply(0.036).asString("-fx-font-size: %.0fpx;"));
		this.flashbackTeam = new Label();
		this.flashbackTeam.getStyleClass().add("podium-team");
		this.flashbackTeam.styleProperty().bind(host.heightProperty().multiply(0.03).asString("-fx-font-size: %.0fpx;"));
		this.flashbackEvent = new Label();
		this.flashbackEvent.getStyleClass().add("podium-cry");
		this.flashbackEvent.styleProperty().bind(host.heightProperty().multiply(0.034).asString("-fx-font-size: %.0fpx;"));

		HBox dots = this.dotRow(count);

		VBox page = new VBox(10, title, frame, this.flashbackRound, this.flashbackTeam, this.flashbackEvent, dots);
		page.setAlignment(Pos.CENTER);
		page.setMouseTransparent(true);
		this.pane.getChildren().setAll(shade, page);
	}

	/** The photo of the flashback, in a black frame that does not stretch over the page */
	private StackPane flashbackFrame() {
		this.flashbackImage = new ImageView();
		this.flashbackImage.setSmooth(true);
		this.flashbackImage.setPreserveRatio(true);
		this.flashbackImage.fitWidthProperty().bind(host.widthProperty().multiply(0.66));
		this.flashbackImage.fitHeightProperty().bind(host.heightProperty().multiply(0.5));
		StackPane frame = new StackPane(this.flashbackImage);
		// The frame holds the photo, it does not stretch over the page
		frame.setMaxWidth(Region.USE_PREF_SIZE);
		frame.setStyle("-fx-background-color: #000000; -fx-border-color: #ffd400; -fx-border-width: 4;");
		frame.setEffect(new DropShadow(40, Podium.GOLD));
		return frame;
	}

	/** A row of dots, one for each moment, kept in the list so the page can light the current one */
	private HBox dotRow(int count) {
		HBox dots = new HBox(8);
		dots.setAlignment(Pos.CENTER);
		for (int index = 0; index < count; index++) {
			Circle dot = new Circle(5, Color.web("#ffffff", 0.3));
			this.flashbackDots.add(dot);
			dots.getChildren().add(dot);
		}
		return dots;
	}

	/**
	 * The photos one after another, each fading in and out, forever
	 */
	private Timeline cycle(List<Clip> clips) {
		Timeline timeline = new Timeline();
		for (int deepIndex = 0; deepIndex < clips.size(); deepIndex++) {
			int index = deepIndex;
			double start = deepIndex * FLASHBACK_SLIDE;
			timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(start), event -> this.showClip(clips.get(index), index), new KeyValue(this.flashbackImage.opacityProperty(), 0)));
			timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(start + FLASHBACK_FADE), new KeyValue(this.flashbackImage.opacityProperty(), 1)));
			timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(start + FLASHBACK_SLIDE - FLASHBACK_FADE), new KeyValue(this.flashbackImage.opacityProperty(), 1)));
		}
		timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(clips.size() * FLASHBACK_SLIDE), new KeyValue(this.flashbackImage.opacityProperty(), 0)));
		timeline.setCycleCount(Animation.INDEFINITE);
		return timeline;
	}

	/** Shows a clip of the flashback: its picture, its round and its team. */
	private void showClip(Clip clip, int index) {
		this.flashbackImage.setImage(clip.image());
		this.flashbackRound.setText(clip.round());
		this.flashbackTeam.setText(clip.team());
		this.flashbackEvent.setText(clip.event());
		for (int index2 = 0; index2 < this.flashbackDots.size(); index2++) {
			this.flashbackDots.get(index2).setFill(index2 == index ? Color.web("#ffd400") : Color.web("#ffffff", 0.3));
		}
	}

	/** Whether a flashback is on screen */
	boolean isOpen() {
		return this.flashbackOpen;
	}
}
