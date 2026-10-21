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
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import model.BattleMoment;
import model.Opponent;
import widgets.ArcadeBackground;
import widgets.Celebration;
import widgets.Sfx;
import widgets.Sound;
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

	static final Color GOLD = Color.web("#ffd400");
	static final Color SILVER = Color.web("#d6deea");
	static final Color BRONZE = Color.web("#d98a45");

	/** Lands and bounces, like something heavy falling on the step */
	static final Interpolator BOUNCE = new Interpolator() {
		/** The bounce easing: the position of the animation at the given time, between zero and one. */
		@Override
		protected double curve(double time) {
			if (time < 1 / 2.75) {
				return 7.5625 * time * time;
			} else if (time < 2 / 2.75) {
				double uCoord = time - 1.5 / 2.75;
				return 7.5625 * uCoord * uCoord + 0.75;
			} else if (time < 2.5 / 2.75) {
				double uCoord2 = time - 2.25 / 2.75;
				return 7.5625 * uCoord2 * uCoord2 + 0.9375;
			}
			double uCoord3 = time - 2.625 / 2.75;
			return 7.5625 * uCoord3 * uCoord3 + 0.984375;
		}
	};

	/** How much of the width of the screen a column of the podium takes: a step, a picture and the names. The columns are farther apart than this, so nothing of one touches another. */
	static final double COLUMN_WIDTH = 0.19;
	static final double COLUMN_SPACING = 0.215;

	private final Random random = new Random();
	/** The robots by place, 1 to 3, null if there is none */
	final Opponent[] places = new Opponent[4];
	/** The places revealed by the operator, in order: 3, 2 and 1 (the ones that exist) */
	private final List<Integer> reveals = new ArrayList<Integer>();
	private final List<Animation> animations = new ArrayList<Animation>();
	// The photos of the battles, the robots by id, and the flashback of the robot on screen
	final List<BattleMoment> moments;
	final Map<Long, Opponent> opponents;
	private final PodiumFlashback flashback = new PodiumFlashback(this);
	private final Celebration rays = new Celebration();
	final Celebration fireworks = new Celebration();
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
		for (int index = 0; index < Math.min(3, podium.size()); index++) {
			this.places[index + 1] = podium.get(index);
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

		this.flashback.pane.setVisible(false);
		this.flashback.pane.setMouseTransparent(true);
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
		this.flashback.hide();
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
		this.flashback.show(place);
		this.updateHint();
	}

	/**
	 * Takes back the last place revealed
	 */
	public void previous() {
		if (!this.started || this.step == 0) {
			return;
		}
		this.flashback.hide();
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

	/** Stops the ceremony: the flashback, the animations and the music. */
	public void stop() {
		this.flashback.hide();
		this.stopAnimations();
		if (this.music != null) {
			this.music.stop();
			this.music = null;
		}
	}

	/** Stops every animation in progress. */
	private void stopAnimations() {
		for (Animation animation : this.animations) {
			animation.stop();
		}
		this.animations.clear();
	}

	/** Keeps an animation until it finishes, so it can be stopped with the others. */
	<T extends Animation> T animate(T animation) {
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

	/** The volume of the music, so that a slider can be bound to it. */
	public DoubleProperty musicVolumeProperty() {
		return this.musicVolume;
	}

	/** Shows the hint of the next key: start the ceremony or reveal the next place. */
	void updateHint() {
		if (this.step < this.reveals.size()) {
			int next = this.reveals.get(this.step);
			this.hint.setText("ESPAÇO: " + (this.step == 0 ? "INICIAR A PREMIAÇÃO" : "REVELAR") + " - " + next + "º LUGAR" + (this.step > 0 ? "   ←: VOLTAR" : ""));
		} else if (this.flashback.isOpen()) {
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
		this.getChildren().setAll(this.background, this.rays, this.content, this.fireworks, this.flashback.pane, this.flash, this.hint);
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
		for (int index2 = 0; index2 < 14; index2++) {
			int index = index2;
			barrage.getKeyFrames().add(new KeyFrame(Duration.millis(delay * 1000 + 250 + index2 * 170), event -> this.fireworks.rocket(0.08 + this.random.nextDouble() * 0.84, colors[index % colors.length], 110)));
		}
		this.animate(barrage).play();
		this.animate(new Timeline(new KeyFrame(Duration.millis(delay * 1000 + 1500), event -> {
			this.fireworks.cannons(50);
			this.rays.flare();
		}))).play();
	}

	// ----------------------------------------------------------- the ceremony

	/** Shows the stage of the ceremony: the podium with the winners. */
	private void showStage() {
		this.stopAnimations();
		this.fireworks.clear();
		this.fireworks.setRain(0);
		this.fireworks.setAutoFireworks(0);
		this.stage = new PodiumStage(this);
		this.background = new ArcadeBackground(Color.web("#2ec4ff"));
		this.rays.setRays(null, 0.5, 0.4);
		this.content.getChildren().setAll(this.stage);
		this.getChildren().setAll(this.background, this.rays, this.content, this.fireworks, this.flashback.pane, this.flash, this.hint);
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
		double posX = this.stage.columnCenter(place);

		this.animate(new Timeline(new KeyFrame(Duration.millis(750), event -> {
			this.shake(this.stage, place == 1 ? 22 : 10, 450);
			this.fireworks.popper(posX, 0.7, place == 1 ? 90 : 45, color, Color.WHITE, color.brighter());
			this.fireworks.burst(posX, 0.28, color, place == 1 ? 150 : 100);
			this.fireworks.burst(Math.max(0.1, posX - 0.14), 0.36, Color.WHITE, 70);
			this.fireworks.burst(Math.min(0.9, posX + 0.14), 0.36, Color.WHITE, 70);
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
			this.rays.setRays(color, posX, 0.35);
			this.rays.flare();
		}
	}


	// ---------------------------------------------------------------- pieces

	/**
	 * The robot picture, a fit of the height of the screen
	 */
	ImageView image(Opponent opponent, double heightFactor, double widthFactor, boolean reflection) {
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
	Node title(String text, double heightFactor) {
		HBox letters = new HBox();
		letters.setAlignment(Pos.CENTER);
		for (char column : text.toCharArray()) {
			Label letter = new Label(String.valueOf(column == 'Ã' ? 'A' : column));
			letter.getStyleClass().add("podium-title");
			letter.styleProperty().bind(this.heightProperty().multiply(heightFactor).asString("-fx-font-size: %.0fpx;"));
			if (column == 'Ã') {
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
	static Group trophy(Color color) {
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
		double cxValue = 80;
		double cyValue = 52;
		for (int index = 0; index < 10; index++) {
			double radius = index % 2 == 0 ? 24 : 10;
			double angle = -Math.PI / 2 + Math.PI * index / 5;
			star.getPoints().addAll(cxValue + Math.cos(angle) * radius, cyValue + Math.sin(angle) * radius);
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
	void sparkles(Group trophy) {
		double[][] spots = { { -10, 10 }, { 170, 24 }, { 150, 120 }, { 4, 112 }, { 80, -22 } };
		for (int index = 0; index < spots.length; index++) {
			Polygon star = new Polygon(0, -14, 3.5, -3.5, 14, 0, 3.5, 3.5, 0, 14, -3.5, 3.5, -14, 0, -3.5, -3.5);
			star.setFill(Color.WHITE);
			star.setBlendMode(BlendMode.ADD);
			star.setTranslateX(spots[index][0]);
			star.setTranslateY(spots[index][1]);
			star.setScaleX(0);
			star.setScaleY(0);
			trophy.getChildren().add(star);
			Timeline twinkle = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(star.scaleXProperty(), 0), new KeyValue(star.scaleYProperty(), 0)),
					new KeyFrame(Duration.millis(260), new KeyValue(star.scaleXProperty(), 1.4), new KeyValue(star.scaleYProperty(), 1.4)),
					new KeyFrame(Duration.millis(620), new KeyValue(star.scaleXProperty(), 0), new KeyValue(star.scaleYProperty(), 0)), new KeyFrame(Duration.millis(1500 + index * 130)));
			twinkle.setDelay(Duration.millis(900 + index * 260));
			twinkle.setCycleCount(Animation.INDEFINITE);
			this.animate(twinkle).play();
		}
	}

	// ------------------------------------------------------------ animations

	/**
	 * Comes huge and slams onto the screen
	 */
	static Animation slam(Node node, double delay, double millis) {
		node.setScaleX(7);
		node.setScaleY(7);
		ScaleTransition scale = new ScaleTransition(Duration.millis(millis), node);
		scale.setToX(1);
		scale.setToY(1);
		scale.setInterpolator(Theme.OVERSHOOT);
		return delayed(delay, fadeIn(node, millis * 0.4), scale);
	}

	/**
	 * Falls from above and bounces
	 */
	static Animation drop(Node node, double delay, double millis) {
		node.setTranslateY(-900);
		TranslateTransition fall = new TranslateTransition(Duration.millis(millis), node);
		fall.setToY(0);
		fall.setInterpolator(BOUNCE);
		return delayed(delay, fadeIn(node, 120), fall);
	}

	/**
	 * Grows from nothing, turning
	 */
	static Animation zoomSpin(Node node, double delay, double millis) {
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
		return delayed(delay, fadeIn(node, 150), scale, turn);
	}

	/** Shows the node in the given millis */
	private static FadeTransition fadeIn(Node node, double millis) {
		FadeTransition fade = new FadeTransition(Duration.millis(millis), node);
		fade.setFromValue(0);
		fade.setToValue(1);
		return fade;
	}

	/** The animations at the same time, after the delay */
	static Animation delayed(double delay, Animation fade, Animation... moves) {
		List<Animation> all = new ArrayList<Animation>(List.of(moves));
		all.add(fade);
		ParallelTransition both = new ParallelTransition(all.toArray(new Animation[0]));
		both.setDelay(Duration.millis(delay));
		return both;
	}
	/**
	 * Fades in going up a little
	 */
	static Animation rise(Node node, double delay) {
		node.setTranslateY(40);
		TranslateTransition translateTransition = new TranslateTransition(Duration.millis(450), node);
		translateTransition.setToY(0);
		translateTransition.setInterpolator(Interpolator.EASE_OUT);
		FadeTransition fade = new FadeTransition(Duration.millis(450), node);
		fade.setFromValue(0);
		fade.setToValue(1);
		ParallelTransition both = new ParallelTransition(translateTransition, fade);
		both.setDelay(Duration.millis(delay));
		return both;
	}

	/**
	 * The screen trembles for a moment
	 */
	private void shake(Node node, double amplitude, double millis) {
		Timeline shake = new Timeline();
		int frames = (int) (millis / 30);
		for (int index = 0; index < frames; index++) {
			double decay = 1 - (double) index / frames;
			shake.getKeyFrames().add(new KeyFrame(Duration.millis(index * 30), new KeyValue(node.translateXProperty(), (this.random.nextDouble() - 0.5) * 2 * amplitude * decay),
					new KeyValue(node.translateYProperty(), (this.random.nextDouble() - 0.5) * 2 * amplitude * decay)));
		}
		shake.getKeyFrames().add(new KeyFrame(Duration.millis(millis), new KeyValue(node.translateXProperty(), 0), new KeyValue(node.translateYProperty(), 0)));
		this.animate(shake).play();
	}

	// ----------------------------------------------------------------- stage
}
