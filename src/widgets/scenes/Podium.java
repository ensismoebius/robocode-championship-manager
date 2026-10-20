package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import model.BattleMoment;
import model.Opponent;
import widgets.ArcadeBackground;
import widgets.Celebration;
import widgets.Sfx;
import widgets.Sound;
import tournament.Flashbacks;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The end of the championship. The champion is announced at once, in a blaze:
 * a trophy falling, rays of light, fireworks, cannons of confetti and a
 * fanfare. Then the operator controls the trophies ceremony: each press shows
 * the next place on the podium, the third, the second and the first again.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Podium extends StackPane {

	private static final Color GOLD = Color.web("#ffd400");
	private static final Color SILVER = Color.web("#d6deea");
	private static final Color BRONZE = Color.web("#d98a45");

	/** Lands and bounces, like something heavy falling on the step */
	private static final Interpolator BOUNCE = new Interpolator() {
		@Override
		protected double curve(double t) {
			if (t < 1 / 2.75) {
				return 7.5625 * t * t;
			} else if (t < 2 / 2.75) {
				double u = t - 1.5 / 2.75;
				return 7.5625 * u * u + 0.75;
			} else if (t < 2.5 / 2.75) {
				double u = t - 2.25 / 2.75;
				return 7.5625 * u * u + 0.9375;
			}
			double u = t - 2.625 / 2.75;
			return 7.5625 * u * u + 0.984375;
		}
	};

	/** How much of the width of the screen a column of the podium takes: a step, a picture and the names. The columns are farther apart than this, so nothing of one touches another. */
	private static final double COLUMN_WIDTH = 0.19;
	private static final double COLUMN_SPACING = 0.215;

	private final Random random = new Random();
	/** The robots by place, 1 to 3, null if there is none */
	private final Opponent[] places = new Opponent[4];
	/** The places revealed by the operator, in order: 3, 2 and 1 (the ones that exist) */
	private final List<Integer> reveals = new ArrayList<Integer>();
	private final List<Animation> animations = new ArrayList<Animation>();
	// The photos of the battles, the robots by id, and the flashback of the robot on screen
	private final List<BattleMoment> moments;
	private final Map<Long, Opponent> opponents;
	private final StackPane flashback = new StackPane();
	private final List<Circle> flashbackDots = new ArrayList<Circle>();
	private ImageView flashbackImage;
	private Label flashbackRound;
	private Label flashbackTeam;
	private Label flashbackEvent;
	private PauseTransition flashbackWait;
	private Timeline flashbackCycle;
	private FadeTransition flashbackFade;
	private boolean flashbackOpen = false;
	private final Celebration rays = new Celebration();
	private final Celebration fireworks = new Celebration();
	private final Region flash = new Region();
	private final Label hint = new Label();
	private final StackPane content = new StackPane();
	private ArcadeBackground background;
	private PodiumStage stage;
	private Sound music;
	private final DoubleProperty musicVolume = new SimpleDoubleProperty(1);
	/** 0 is the champion, 1 and more the places that were revealed */
	private int step = 0;
	private boolean started = false;

	/**
	 * @param podium
	 *            the champion, the second place and the third place, the last two
	 *            may be missing
	 * @param moments
	 *            the photos of the battles of the robots of the podium, may be
	 *            empty
	 * @param opponents
	 *            every robot of the championship by id, to name the opponents of
	 *            the photos
	 */
	public Podium(List<Opponent> podium, List<BattleMoment> moments, Map<Long, Opponent> opponents) {
		this.moments = moments;
		this.opponents = opponents;
		for (int i = 0; i < Math.min(3, podium.size()); i++) {
			this.places[i + 1] = podium.get(i);
		}
		// The third, the second and then the first, the ones that exist
		for (int place = 3; place >= 1; place--) {
			if (this.places[place] != null) {
				this.reveals.add(place);
			}
		}
		// Only the champion: there is no ceremony
		if (this.reveals.size() <= 1) {
			this.reveals.clear();
		}

		this.flashback.setVisible(false);
		this.flashback.setMouseTransparent(true);
		this.flash.setStyle("-fx-background-color: white;");
		this.flash.setOpacity(0);
		this.flash.setMouseTransparent(true);
		this.hint.getStyleClass().add("podium-hint");
		this.hint.setMouseTransparent(true);
		this.hint.setMinWidth(Region.USE_PREF_SIZE);
		this.hint.setMaxWidth(Region.USE_PREF_SIZE);
		this.hint.setMaxHeight(Region.USE_PREF_SIZE);
		this.hint.styleProperty().bind(this.heightProperty().multiply(0.03).asString("-fx-font-size: %.0fpx;"));
		StackPane.setAlignment(this.hint, Pos.BOTTOM_CENTER);
		StackPane.setMargin(this.hint, new Insets(0, 0, 24, 0));
		Theme.blink(this.hint);
		this.setFocusTraversable(true);
		this.musicVolume.addListener((observable, before, now) -> {
			if (this.music != null) {
				this.music.setVolume(now.doubleValue());
			}
		});
	}

	// ------------------------------------------------------------- control

	/**
	 * The champion, at once: call it when the screen is on
	 */
	public void play() {
		this.started = true;
		this.step = 0;
		this.showChampion();
	}

	/**
	 * The next place of the ceremony
	 */
	public void next() {
		if (!this.started) {
			return;
		}
		// The photos of the last robot close with the next place, or alone at the end
		this.hideFlashback();
		if (this.step >= this.reveals.size()) {
			this.updateHint();
			return;
		}
		this.step++;
		int place = this.reveals.get(this.step - 1);
		if (this.step == 1) {
			this.showStage();
		}
		this.reveal(place);
		this.showFlashback(place);
		this.updateHint();
	}

	/**
	 * Takes back the last place revealed
	 */
	public void previous() {
		if (!this.started || this.step == 0) {
			return;
		}
		this.hideFlashback();
		int undone = this.reveals.get(this.step - 1);
		this.step--;
		if (this.step == 0) {
			this.stopAnimations();
			this.fireworks.clear();
			this.showChampion();
			return;
		}
		this.stage.hide(undone);
		this.stage.setBanner(this.reveals.get(this.step - 1));
		this.updateHint();
	}

	public void stop() {
		this.hideFlashback();
		this.stopAnimations();
		if (this.music != null) {
			this.music.stop();
			this.music = null;
		}
	}

	private void stopAnimations() {
		for (Animation animation : this.animations) {
			animation.stop();
		}
		this.animations.clear();
	}

	private <T extends Animation> T animate(T animation) {
		this.animations.add(animation);
		animation.setOnFinished(event -> this.animations.remove(animation));
		return animation;
	}

	/**
	 * The default music of the program plays under the whole ceremony, it starts
	 * with this volume (0 to 1), that the screen may raise with
	 * {@link #musicVolumeProperty()}
	 */
	public void startMusic(double volume) {
		this.musicVolume.set(volume);
		if (this.music == null) {
			this.music = Sound.fromResource(getClass(), "versusMusic.mp3");
		}
		if (this.music != null) {
			this.music.setLoop(true);
			this.music.setVolume(volume);
			this.music.play();
		}
	}

	public DoubleProperty musicVolumeProperty() {
		return this.musicVolume;
	}

	private void updateHint() {
		if (this.step < this.reveals.size()) {
			int next = this.reveals.get(this.step);
			this.hint.setText("ESPAÇO: " + (this.step == 0 ? "INICIAR A PREMIAÇÃO" : "REVELAR") + " - " + next + "º LUGAR" + (this.step > 0 ? "   ←: VOLTAR" : ""));
		} else if (this.flashbackOpen) {
			this.hint.setText("ESPAÇO: FECHAR OS MOMENTOS   ←: VOLTAR   R: CHAVEAMENTO");
		} else {
			this.hint.setText((this.step > 0 ? "←: VOLTAR   " : "") + "R: CHAVEAMENTO");
		}
	}

	// ----------------------------------------------------------- the champion

	/**
	 * The champion, big and loud
	 */
	private void showChampion() {
		this.stopAnimations();
		this.fireworks.clear();
		this.fireworks.setRain(0);
		this.fireworks.setAutoFireworks(0);
		Opponent champion = this.places[1];

		this.background = new ArcadeBackground(GOLD);
		this.rays.setRays(GOLD, 0.5, 0.42);

		// The trophy
		Group trophy = trophy(GOLD);
		trophy.scaleXProperty().bind(this.heightProperty().multiply(0.00135));
		trophy.scaleYProperty().bind(this.heightProperty().multiply(0.00135));
		StackPane trophyBox = new StackPane(trophy);
		trophyBox.setMaxHeight(Region.USE_PREF_SIZE);
		trophyBox.prefHeightProperty().bind(this.heightProperty().multiply(0.22));
		trophyBox.prefWidthProperty().bind(this.widthProperty().multiply(0.2));
		trophyBox.setMinHeight(0);

		Node title = this.title("CAMPEÃO!", 0.09);
		DropShadow glow = new DropShadow(40, GOLD);
		glow.setSpread(0.3);
		title.setEffect(glow);

		ImageView image = this.image(champion, 0.30, 0.5, true);
		image.setEffect(new DropShadow(50, GOLD));

		Label name = new Label(champion.getName().toUpperCase());
		name.getStyleClass().add("podium-name");
		name.styleProperty().bind(this.heightProperty().multiply(0.05).asString("-fx-font-size: %.0fpx;"));
		name.setEffect(Theme.glow(Theme.PLAYER_1, 22));
		Label team = new Label("TIME " + champion.getTeam().toUpperCase());
		team.getStyleClass().add("podium-team");
		team.styleProperty().bind(this.heightProperty().multiply(0.03).asString("-fx-font-size: %.0fpx;"));
		Label cry = new Label("“" + champion.getBattleCry() + "”");
		cry.getStyleClass().add("podium-cry");
		cry.setWrapText(true);
		cry.maxWidthProperty().bind(this.widthProperty().multiply(0.7));
		cry.styleProperty().bind(this.heightProperty().multiply(0.034).asString("-fx-font-size: %.0fpx;"));

		VBox column = new VBox(4, trophyBox, title, image, name, team, cry);
		column.setAlignment(Pos.CENTER);
		column.setMouseTransparent(true);
		this.content.getChildren().setAll(column);
		this.getChildren().setAll(this.background, this.rays, this.content, this.fireworks, this.flashback, this.flash, this.hint);
		this.updateHint();

		// Everything at once, in sequence
		this.flash.setOpacity(1);
		this.animate(new Timeline(new KeyFrame(Duration.millis(1100), new KeyValue(this.flash.opacityProperty(), 0, Interpolator.EASE_OUT)))).play();
		this.rays.flare();
		this.shake(this.content, 26, 700);
		Sfx.ceremony(1);

		for (Node node : List.of(trophy, title, image, name, team, cry)) {
			node.setOpacity(0);
		}
		this.animate(slam(title, 0, 700)).play();
		this.animate(drop(trophy, 120, 900)).play();
		this.animate(zoomSpin(image, 260, 800)).play();
		this.animate(rise(name, 800)).play();
		this.animate(rise(team, 950)).play();
		this.animate(rise(cry, 1100)).play();
		this.sparkles(trophy);

		this.celebrateBig(0.35);
		this.fireworks.setRain(22);
		this.fireworks.setAutoFireworks(1.6, GOLD, Color.web("#2ec4ff"), Color.web("#ff3d6e"), Color.WHITE);
	}

	/**
	 * Cannons, bursts and a sequence of rockets
	 */
	private void celebrateBig(double delay) {
		Color[] colors = { GOLD, Color.web("#2ec4ff"), Color.web("#ff3d6e"), Color.web("#5af08f"), Color.WHITE };
		this.animate(new Timeline(new KeyFrame(Duration.millis(delay * 1000), event -> {
			this.fireworks.cannons(70);
			this.fireworks.burst(0.2, 0.3, GOLD, 120);
			this.fireworks.burst(0.8, 0.3, Color.web("#2ec4ff"), 120);
			this.fireworks.burst(0.5, 0.2, Color.web("#ff3d6e"), 140);
		}))).play();
		Timeline barrage = new Timeline();
		for (int i = 0; i < 14; i++) {
			int index = i;
			barrage.getKeyFrames().add(new KeyFrame(Duration.millis(delay * 1000 + 250 + i * 170), event -> this.fireworks.rocket(0.08 + this.random.nextDouble() * 0.84, colors[index % colors.length], 110)));
		}
		this.animate(barrage).play();
		this.animate(new Timeline(new KeyFrame(Duration.millis(delay * 1000 + 1500), event -> {
			this.fireworks.cannons(50);
			this.rays.flare();
		}))).play();
	}

	// ----------------------------------------------------------- the ceremony

	private void showStage() {
		this.stopAnimations();
		this.fireworks.clear();
		this.fireworks.setRain(0);
		this.fireworks.setAutoFireworks(0);
		this.stage = new PodiumStage();
		this.background = new ArcadeBackground(Color.web("#2ec4ff"));
		this.rays.setRays(null, 0.5, 0.4);
		this.content.getChildren().setAll(this.stage);
		this.getChildren().setAll(this.background, this.rays, this.content, this.fireworks, this.flashback, this.flash, this.hint);
		// The curtain: a quick flash covers the change
		this.flash.setOpacity(0.9);
		this.animate(new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(this.flash.opacityProperty(), 0)))).play();
	}

	/**
	 * Shows a robot on its step, with the celebration of the place
	 */
	private void reveal(int place) {
		Color color = place == 1 ? GOLD : place == 2 ? SILVER : BRONZE;
		this.stage.show(place);
		Sfx.ceremony(place);
		double x = this.stage.columnCenter(place);

		this.animate(new Timeline(new KeyFrame(Duration.millis(750), event -> {
			this.shake(this.stage, place == 1 ? 22 : 10, 450);
			this.fireworks.popper(x, 0.7, place == 1 ? 90 : 45, color, Color.WHITE, color.brighter());
			this.fireworks.burst(x, 0.28, color, place == 1 ? 150 : 100);
			this.fireworks.burst(Math.max(0.1, x - 0.14), 0.36, Color.WHITE, 70);
			this.fireworks.burst(Math.min(0.9, x + 0.14), 0.36, Color.WHITE, 70);
			if (place != 1) {
				this.fireworks.cannons(place == 2 ? 36 : 22);
			}
		}))).play();

		if (place == 1) {
			// The first place again: all out
			this.flash.setOpacity(1);
			this.animate(new Timeline(new KeyFrame(Duration.millis(1200), new KeyValue(this.flash.opacityProperty(), 0, Interpolator.EASE_OUT)))).play();
			this.rays.setRays(GOLD, 0.5, 0.4);
			this.rays.flare();
			this.celebrateBig(0.7);
			this.fireworks.setRain(22);
			this.fireworks.setAutoFireworks(1.8, GOLD, Color.WHITE, Color.web("#ff3d6e"), Color.web("#2ec4ff"));
		} else {
			this.rays.setRays(color, x, 0.35);
			this.rays.flare();
		}
	}

	// --------------------------------------------------------------- flashback

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
	private void showFlashback(int place) {
		Opponent robot = this.places[place];
		List<BattleMoment> chosen = Flashbacks.of(this.moments, robot.getId(), Flashbacks.LIMIT);
		if (chosen.isEmpty()) {
			return;
		}
		this.flashbackWait = new PauseTransition(Duration.seconds(FLASHBACK_DELAY));
		this.flashbackWait.setOnFinished(event -> this.openFlashback(robot, chosen));
		this.flashbackWait.play();
	}

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
		this.flashback.setVisible(true);
		this.flashback.setOpacity(0);
		this.flashbackFade = new FadeTransition(Duration.seconds(0.6), this.flashback);
		this.flashbackFade.setToValue(1);
		this.flashbackFade.play();
		this.flashbackCycle = this.cycle(clips);
		this.flashbackCycle.play();
		this.updateHint();
	}

	/**
	 * Closes the flashback at once, or fades it out when it is on screen
	 */
	private void hideFlashback() {
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
			this.flashbackFade = new FadeTransition(Duration.seconds(0.3), this.flashback);
			this.flashbackFade.setToValue(0);
			this.flashbackFade.setOnFinished(event -> this.flashback.setVisible(false));
			this.flashbackFade.play();
		}
	}

	/**
	 * The photo and what it tells, about the robot shown and its opponent in the
	 * battle: the round, the team and the name of the opponent are always there
	 */
	private Clip clip(BattleMoment moment, Opponent robot) {
		Opponent rival = this.opponents.get(Flashbacks.opponentOf(moment, robot.getId()));
		String me = upper(robot.getName());
		String rivalName = rival == null ? "?" : upper(rival.getName());
		String rivalTeam = rival == null ? "" : "TIME " + upper(rival.getTeam());
		String power = String.format("%.1f", moment.getPower());
		String event;
		if (moment.isDestruction()) {
			event = moment.getSubjectId() == robot.getId() ? me + " FOI DESTRUÍDO" : me + " DESTRUIU " + rivalName;
		} else if (moment.getSubjectId() == robot.getId()) {
			event = me + " ACERTOU " + rivalName + " COM FORÇA " + power;
		} else {
			event = rivalName + " ACERTOU " + me + " COM FORÇA " + power;
		}
		return new Clip(new Image(new ByteArrayInputStream(moment.getImage())), "ROUND " + moment.getRoundNumber() + " · VS " + rivalName, rivalTeam, event);
	}

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
		shade.prefWidthProperty().bind(this.widthProperty());
		shade.prefHeightProperty().bind(this.heightProperty());

		Label title = new Label("MOMENTOS DE " + upper(robot.getName()));
		title.getStyleClass().add("podium-banner");
		title.styleProperty().bind(this.heightProperty().multiply(0.05).asString("-fx-font-size: %.0fpx;"));

		this.flashbackImage = new ImageView();
		this.flashbackImage.setSmooth(true);
		this.flashbackImage.setPreserveRatio(true);
		this.flashbackImage.fitWidthProperty().bind(this.widthProperty().multiply(0.66));
		this.flashbackImage.fitHeightProperty().bind(this.heightProperty().multiply(0.5));
		StackPane frame = new StackPane(this.flashbackImage);
		// The frame holds the photo, it does not stretch over the page
		frame.setMaxWidth(Region.USE_PREF_SIZE);
		frame.setStyle("-fx-background-color: #000000; -fx-border-color: #ffd400; -fx-border-width: 4;");
		frame.setEffect(new DropShadow(40, GOLD));

		this.flashbackRound = new Label();
		this.flashbackRound.getStyleClass().add("podium-name");
		this.flashbackRound.styleProperty().bind(this.heightProperty().multiply(0.036).asString("-fx-font-size: %.0fpx;"));
		this.flashbackTeam = new Label();
		this.flashbackTeam.getStyleClass().add("podium-team");
		this.flashbackTeam.styleProperty().bind(this.heightProperty().multiply(0.03).asString("-fx-font-size: %.0fpx;"));
		this.flashbackEvent = new Label();
		this.flashbackEvent.getStyleClass().add("podium-cry");
		this.flashbackEvent.styleProperty().bind(this.heightProperty().multiply(0.034).asString("-fx-font-size: %.0fpx;"));

		HBox dots = new HBox(8);
		dots.setAlignment(Pos.CENTER);
		for (int i = 0; i < count; i++) {
			Circle dot = new Circle(5, Color.web("#ffffff", 0.3));
			this.flashbackDots.add(dot);
			dots.getChildren().add(dot);
		}

		VBox page = new VBox(10, title, frame, this.flashbackRound, this.flashbackTeam, this.flashbackEvent, dots);
		page.setAlignment(Pos.CENTER);
		page.setMouseTransparent(true);
		this.flashback.getChildren().setAll(shade, page);
	}

	/**
	 * The photos one after another, each fading in and out, forever
	 */
	private Timeline cycle(List<Clip> clips) {
		Timeline timeline = new Timeline();
		for (int k = 0; k < clips.size(); k++) {
			int index = k;
			double start = k * FLASHBACK_SLIDE;
			timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(start), event -> this.showClip(clips.get(index), index), new KeyValue(this.flashbackImage.opacityProperty(), 0)));
			timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(start + FLASHBACK_FADE), new KeyValue(this.flashbackImage.opacityProperty(), 1)));
			timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(start + FLASHBACK_SLIDE - FLASHBACK_FADE), new KeyValue(this.flashbackImage.opacityProperty(), 1)));
		}
		timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(clips.size() * FLASHBACK_SLIDE), new KeyValue(this.flashbackImage.opacityProperty(), 0)));
		timeline.setCycleCount(Animation.INDEFINITE);
		return timeline;
	}

	private void showClip(Clip clip, int index) {
		this.flashbackImage.setImage(clip.image());
		this.flashbackRound.setText(clip.round());
		this.flashbackTeam.setText(clip.team());
		this.flashbackEvent.setText(clip.event());
		for (int i = 0; i < this.flashbackDots.size(); i++) {
			this.flashbackDots.get(i).setFill(i == index ? Color.web("#ffd400") : Color.web("#ffffff", 0.3));
		}
	}

	// ---------------------------------------------------------------- pieces

	/**
	 * The robot picture, a fit of the height of the screen
	 */
	private ImageView image(Opponent opponent, double heightFactor, double widthFactor, boolean reflection) {
		ImageView view = new ImageView();
		view.setSmooth(true);
		view.setPreserveRatio(true);
		view.fitHeightProperty().bind(this.heightProperty().multiply(heightFactor));
		view.fitWidthProperty().bind(this.widthProperty().multiply(widthFactor));
		if (reflection) {
			view.setEffect(new Reflection(0, 0.3, 0.35, 0));
		}
		if (opponent != null && opponent.getImage() != null) {
			view.setImage(new Image(new ByteArrayInputStream(opponent.getImage())));
		}
		return view;
	}

	/**
	 * Big pixel letters. The font has no tilde on the A, so it is drawn over it.
	 *
	 * @param heightFactor
	 *            the size of the letters, from the height of the screen
	 */
	private Node title(String text, double heightFactor) {
		HBox letters = new HBox();
		letters.setAlignment(Pos.CENTER);
		for (char c : text.toCharArray()) {
			Label letter = new Label(String.valueOf(c == 'Ã' ? 'A' : c));
			letter.getStyleClass().add("podium-title");
			letter.styleProperty().bind(this.heightProperty().multiply(heightFactor).asString("-fx-font-size: %.0fpx;"));
			if (c == 'Ã') {
				Label tilde = new Label("~");
				tilde.getStyleClass().add("podium-title");
				tilde.styleProperty().bind(this.heightProperty().multiply(heightFactor * 0.8).asString("-fx-font-size: %.0fpx;"));
				tilde.translateYProperty().bind(this.heightProperty().multiply(-heightFactor * 0.62));
				tilde.setMouseTransparent(true);
				StackPane both = new StackPane(letter, tilde);
				both.setAlignment(Pos.CENTER);
				letters.getChildren().add(both);
			} else {
				letters.getChildren().add(letter);
			}
		}
		letters.setMouseTransparent(true);
		return letters;
	}

	/**
	 * A golden cup, with handles, a star and a base
	 */
	private static Group trophy(Color color) {
		Color light = color.interpolate(Color.WHITE, 0.55);
		Color dark = color.interpolate(Color.BLACK, 0.45);
		LinearGradient metal = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop(0, dark), new Stop(0.3, light), new Stop(0.55, color), new Stop(1, dark));
		SVGPath cup = new SVGPath();
		cup.setContent("M20,0 L140,0 L140,45 C140,100 112,122 80,128 C48,122 20,100 20,45 Z");
		SVGPath left = new SVGPath();
		left.setContent("M22,14 C-14,12 -12,72 32,88 L38,76 C8,64 8,30 22,28 Z");
		SVGPath right = new SVGPath();
		right.setContent("M138,14 C174,12 172,72 128,88 L122,76 C152,64 152,30 138,28 Z");
		SVGPath stem = new SVGPath();
		stem.setContent("M68,126 L92,126 L96,160 L64,160 Z");
		SVGPath base = new SVGPath();
		base.setContent("M40,160 L120,160 L132,190 L28,190 Z");
		for (SVGPath part : List.of(cup, left, right, stem, base)) {
			part.setFill(metal);
			part.setStroke(dark);
			part.setStrokeWidth(2);
		}
		SVGPath shine = new SVGPath();
		shine.setContent("M32,8 L46,8 L50,52 C52,84 66,104 80,112 C52,104 34,84 32,50 Z");
		shine.setFill(Color.color(1, 1, 1, 0.55));
		Polygon star = new Polygon();
		double cx = 80;
		double cy = 52;
		for (int i = 0; i < 10; i++) {
			double radius = i % 2 == 0 ? 24 : 10;
			double angle = -Math.PI / 2 + Math.PI * i / 5;
			star.getPoints().addAll(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
		}
		star.setFill(Color.web("#fff6c0"));
		star.setStroke(dark);
		Group group = new Group(left, right, stem, base, cup, shine, star);
		group.setEffect(new DropShadow(36, color));
		return group;
	}

	/**
	 * Stars that twinkle around the trophy
	 */
	private void sparkles(Group trophy) {
		double[][] spots = { { -10, 10 }, { 170, 24 }, { 150, 120 }, { 4, 112 }, { 80, -22 } };
		for (int i = 0; i < spots.length; i++) {
			Polygon star = new Polygon(0, -14, 3.5, -3.5, 14, 0, 3.5, 3.5, 0, 14, -3.5, 3.5, -14, 0, -3.5, -3.5);
			star.setFill(Color.WHITE);
			star.setBlendMode(BlendMode.ADD);
			star.setTranslateX(spots[i][0]);
			star.setTranslateY(spots[i][1]);
			star.setScaleX(0);
			star.setScaleY(0);
			trophy.getChildren().add(star);
			Timeline twinkle = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(star.scaleXProperty(), 0), new KeyValue(star.scaleYProperty(), 0)),
					new KeyFrame(Duration.millis(260), new KeyValue(star.scaleXProperty(), 1.4), new KeyValue(star.scaleYProperty(), 1.4)),
					new KeyFrame(Duration.millis(620), new KeyValue(star.scaleXProperty(), 0), new KeyValue(star.scaleYProperty(), 0)), new KeyFrame(Duration.millis(1500 + i * 130)));
			twinkle.setDelay(Duration.millis(900 + i * 260));
			twinkle.setCycleCount(Animation.INDEFINITE);
			this.animate(twinkle).play();
		}
	}

	// ------------------------------------------------------------ animations

	/**
	 * Comes huge and slams onto the screen
	 */
	private static Animation slam(Node node, double delay, double millis) {
		node.setScaleX(7);
		node.setScaleY(7);
		ScaleTransition scale = new ScaleTransition(Duration.millis(millis), node);
		scale.setToX(1);
		scale.setToY(1);
		scale.setInterpolator(Theme.OVERSHOOT);
		FadeTransition fade = new FadeTransition(Duration.millis(millis * 0.4), node);
		fade.setFromValue(0);
		fade.setToValue(1);
		ParallelTransition both = new ParallelTransition(scale, fade);
		both.setDelay(Duration.millis(delay));
		return both;
	}

	/**
	 * Falls from above and bounces
	 */
	private static Animation drop(Node node, double delay, double millis) {
		node.setTranslateY(-900);
		TranslateTransition fall = new TranslateTransition(Duration.millis(millis), node);
		fall.setToY(0);
		fall.setInterpolator(BOUNCE);
		FadeTransition fade = new FadeTransition(Duration.millis(120), node);
		fade.setFromValue(0);
		fade.setToValue(1);
		ParallelTransition both = new ParallelTransition(fall, fade);
		both.setDelay(Duration.millis(delay));
		return both;
	}

	/**
	 * Grows from nothing, turning
	 */
	private static Animation zoomSpin(Node node, double delay, double millis) {
		node.setScaleX(0);
		node.setScaleY(0);
		node.setRotate(-35);
		ScaleTransition scale = new ScaleTransition(Duration.millis(millis), node);
		scale.setToX(1);
		scale.setToY(1);
		scale.setInterpolator(Theme.OVERSHOOT);
		RotateTransition turn = new RotateTransition(Duration.millis(millis), node);
		turn.setToAngle(0);
		turn.setInterpolator(Interpolator.EASE_OUT);
		FadeTransition fade = new FadeTransition(Duration.millis(150), node);
		fade.setFromValue(0);
		fade.setToValue(1);
		ParallelTransition all = new ParallelTransition(scale, turn, fade);
		all.setDelay(Duration.millis(delay));
		return all;
	}

	/**
	 * Fades in going up a little
	 */
	private static Animation rise(Node node, double delay) {
		node.setTranslateY(40);
		TranslateTransition up = new TranslateTransition(Duration.millis(450), node);
		up.setToY(0);
		up.setInterpolator(Interpolator.EASE_OUT);
		FadeTransition fade = new FadeTransition(Duration.millis(450), node);
		fade.setFromValue(0);
		fade.setToValue(1);
		ParallelTransition both = new ParallelTransition(up, fade);
		both.setDelay(Duration.millis(delay));
		return both;
	}

	/**
	 * The screen trembles for a moment
	 */
	private void shake(Node node, double amplitude, double millis) {
		Timeline shake = new Timeline();
		int frames = (int) (millis / 30);
		for (int i = 0; i < frames; i++) {
			double decay = 1 - (double) i / frames;
			shake.getKeyFrames().add(new KeyFrame(Duration.millis(i * 30), new KeyValue(node.translateXProperty(), (this.random.nextDouble() - 0.5) * 2 * amplitude * decay),
					new KeyValue(node.translateYProperty(), (this.random.nextDouble() - 0.5) * 2 * amplitude * decay)));
		}
		shake.getKeyFrames().add(new KeyFrame(Duration.millis(millis), new KeyValue(node.translateXProperty(), 0), new KeyValue(node.translateYProperty(), 0)));
		this.animate(shake).play();
	}

	// ----------------------------------------------------------------- stage

	/**
	 * The podium: three steps, 2 1 3, with the robots that were revealed standing
	 * on them, a spotlight on each and the place at the top
	 */
	private final class PodiumStage extends Pane {
		private final Rectangle[] steps = new Rectangle[4];
		private final Label[] numbers = new Label[4];
		private final VBox[] figures = new VBox[4];
		private final ImageView[] images = new ImageView[4];
		private final Polygon[] spots = new Polygon[4];
		private final Label banner = new Label();
		private final Group[] trophies = new Group[4];

		PodiumStage() {
			this.setMouseTransparent(true);
			Color[] colors = { null, GOLD, SILVER, BRONZE };
			for (int place = 1; place <= 3; place++) {
				Color color = colors[place];
				Polygon spot = new Polygon();
				spot.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.35)),
						new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.03))));
				spot.setBlendMode(BlendMode.ADD);
				spot.setOpacity(0);
				this.spots[place] = spot;

				Rectangle step = new Rectangle();
				step.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, color.interpolate(Color.WHITE, 0.25)), new Stop(1, color.interpolate(Color.BLACK, 0.55))));
				step.setStroke(color.interpolate(Color.WHITE, 0.5));
				step.setStrokeWidth(3);
				step.setEffect(new DropShadow(26, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.55)));
				this.steps[place] = step;

				Label number = new Label(String.valueOf(place));
				number.getStyleClass().add("podium-number");
				number.setTextFill(color.interpolate(Color.BLACK, 0.55));
				number.styleProperty().bind(Podium.this.heightProperty().multiply(place == 1 ? 0.14 : place == 2 ? 0.1 : 0.075).asString("-fx-font-size: %.0fpx;"));
				this.numbers[place] = number;

				Opponent opponent = Podium.this.places[place];
				if (opponent != null) {
					ImageView image = Podium.this.image(opponent, place == 1 ? 0.31 : place == 2 ? 0.27 : 0.24, COLUMN_WIDTH, false);
					image.setEffect(new DropShadow(30, color));
					this.images[place] = image;
					Label name = new Label(opponent.getName().toUpperCase());
					name.getStyleClass().add("podium-name");
					name.setWrapText(true);
					name.setAlignment(Pos.CENTER);
					name.setTextFill(Color.WHITE);
					// The letters shrink to fit the column: a long name never reaches the next one
					double heightFactor = place == 1 ? 0.038 : 0.032;
					name.styleProperty().bind(Bindings.createStringBinding(() -> String.format("-fx-font-size: %.0fpx;", Math.max(8, Math.min(Podium.this.getHeight() * heightFactor, Podium.this.getWidth() * COLUMN_WIDTH / Math.max(1, opponent.getName().length() * 0.9)))), Podium.this.heightProperty(), Podium.this.widthProperty()));
					String teamText = "TIME " + opponent.getTeam().toUpperCase();
					Label team = new Label(teamText);
					team.getStyleClass().add("podium-team");
					team.setAlignment(Pos.CENTER);
					team.styleProperty().bind(Bindings.createStringBinding(() -> String.format("-fx-font-size: %.0fpx;", Math.max(8, Math.min(Podium.this.getHeight() * 0.026, Podium.this.getWidth() * COLUMN_WIDTH / Math.max(1, teamText.length() * 0.9)))), Podium.this.heightProperty(), Podium.this.widthProperty()));
					VBox figure = new VBox(4, name, team, image);
					// The whole figure, names and picture, fits its column
					figure.prefWidthProperty().bind(Podium.this.widthProperty().multiply(COLUMN_WIDTH));
					figure.minWidthProperty().bind(figure.prefWidthProperty());
					figure.maxWidthProperty().bind(figure.prefWidthProperty());
					figure.setAlignment(Pos.BOTTOM_CENTER);
					figure.setVisible(false);
					this.figures[place] = figure;
				}
			}
			Group trophy = trophy(GOLD);
			trophy.scaleXProperty().bind(Podium.this.heightProperty().multiply(0.00068));
			trophy.scaleYProperty().bind(Podium.this.heightProperty().multiply(0.00068));
			trophy.setVisible(false);
			this.trophies[1] = trophy;

			this.banner.getStyleClass().add("podium-banner");
			this.banner.setVisible(false);
			this.banner.styleProperty().bind(Podium.this.heightProperty().multiply(0.07).asString("-fx-font-size: %.0fpx;"));

			for (int place = 1; place <= 3; place++) {
				this.getChildren().add(this.spots[place]);
			}
			for (int place = 1; place <= 3; place++) {
				this.getChildren().addAll(this.steps[place], this.numbers[place]);
			}
			for (int place = 1; place <= 3; place++) {
				if (this.figures[place] != null) {
					this.getChildren().add(this.figures[place]);
				}
			}
			this.getChildren().addAll(trophy, this.banner);
		}

		/** Where the center of the column of the place is, from 0 to 1 */
		double columnCenter(int place) {
			return place == 1 ? 0.5 : place == 2 ? 0.5 - COLUMN_SPACING : 0.5 + COLUMN_SPACING;
		}

		private double stepHeight(int place) {
			return this.getHeight() * (place == 1 ? 0.26 : place == 2 ? 0.19 : 0.13);
		}

		@Override
		protected void layoutChildren() {
			double width = this.getWidth();
			double height = this.getHeight();
			double baseline = height * 0.9;
			double stepWidth = width * COLUMN_WIDTH;
			for (int place = 1; place <= 3; place++) {
				double cx = this.columnCenter(place) * width;
				double top = baseline - this.stepHeight(place);
				this.steps[place].setX(cx - stepWidth / 2);
				this.steps[place].setY(top);
				this.steps[place].setWidth(stepWidth);
				this.steps[place].setHeight(this.stepHeight(place));
				Label number = this.numbers[place];
				number.autosize();
				number.relocate(cx - number.getWidth() / 2, top + (this.stepHeight(place) - number.getHeight()) / 2);
				this.spots[place].getPoints().setAll(cx - stepWidth * 0.1, 0.0, cx + stepWidth * 0.1, 0.0, cx + stepWidth * 0.75, top, cx - stepWidth * 0.75, top);
				VBox figure = this.figures[place];
				if (figure != null) {
					figure.autosize();
					figure.relocate(cx - figure.getWidth() / 2, top - figure.getHeight());
				}
			}
			double top1 = baseline - this.stepHeight(1);
			VBox champion = this.figures[1];
			double figureHeight = champion == null ? 0 : champion.getHeight();
			// The trophy is scaled around its center (95 high before the scale)
			this.trophies[1].relocate(width / 2 - 80, top1 - figureHeight - height * 0.075 - 95);
			this.banner.autosize();
			this.banner.relocate((width - this.banner.getWidth()) / 2, height * 0.04);
		}

		/**
		 * The place at the top, in its color
		 */
		void setBanner(int place) {
			Color color = place == 1 ? GOLD : place == 2 ? SILVER : BRONZE;
			this.banner.setText(place + "º LUGAR");
			this.banner.setTextFill(color);
			DropShadow glow = new DropShadow(30, color);
			glow.setSpread(0.3);
			this.banner.setEffect(glow);
			this.banner.setVisible(true);
			this.banner.setOpacity(1);
			this.layout();
		}

		void show(int place) {
			this.setBanner(place);
			Podium.this.animate(slam(this.banner, 0, 550)).play();
			Color color = place == 1 ? GOLD : place == 2 ? SILVER : BRONZE;
			this.spots[place].setOpacity(0);
			Podium.this.animate(new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(this.spots[place].opacityProperty(), 1)))).play();
			VBox figure = this.figures[place];
			if (figure != null) {
				figure.setVisible(true);
				figure.setOpacity(0);
				figure.setTranslateY(-this.getHeight());
				// Falls on the step with a bounce
				TranslateTransition fall = new TranslateTransition(Duration.millis(950), figure);
				fall.setToY(0);
				fall.setInterpolator(BOUNCE);
				FadeTransition fade = new FadeTransition(Duration.millis(150), figure);
				fade.setToValue(1);
				Podium.this.animate(new ParallelTransition(fall, fade)).play();
				if (this.images[place] != null) {
					DropShadow glow = new DropShadow(46, color);
					this.images[place].setEffect(glow);
					Timeline pulse = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 20)), new KeyFrame(Duration.millis(700), new KeyValue(glow.radiusProperty(), 60)));
					pulse.setAutoReverse(true);
					pulse.setCycleCount(Animation.INDEFINITE);
					Podium.this.animate(pulse).play();
				}
			}
			if (place == 1) {
				// The trophy comes down over the champion
				Group trophy = this.trophies[1];
				trophy.setVisible(true);
				trophy.setOpacity(0);
				trophy.setTranslateY(-this.getHeight());
				TranslateTransition fall = new TranslateTransition(Duration.millis(1100), trophy);
				fall.setToY(0);
				fall.setInterpolator(BOUNCE);
				fall.setDelay(Duration.millis(450));
				FadeTransition fade = new FadeTransition(Duration.millis(150), trophy);
				fade.setToValue(1);
				fade.setDelay(Duration.millis(450));
				Podium.this.animate(new ParallelTransition(fall, fade)).play();
				Podium.this.sparkles(trophy);
			}
			// The step lights up as the robot lands
			Rectangle step = this.steps[place];
			Podium.this.animate(new SequentialTransition(new PauseTransition(Duration.millis(700)), new Timeline(new KeyFrame(Duration.millis(120), new KeyValue(step.opacityProperty(), 0.55)),
					new KeyFrame(Duration.millis(400), new KeyValue(step.opacityProperty(), 1))))).play();
		}

		void hide(int place) {
			if (this.figures[place] != null) {
				this.figures[place].setVisible(false);
			}
			this.spots[place].setOpacity(0);
			if (place == 1) {
				this.trophies[1].setVisible(false);
				Podium.this.fireworks.setRain(0);
				Podium.this.fireworks.setAutoFireworks(0);
			}
		}
	}
}
