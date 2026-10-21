package widgets;

import java.io.IOException;
import java.io.InputStream;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The arcade look of the program (theme.css, fonts and small animations).
 * The window goes through here (see GameWindow) so everything looks the same.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Theme {

	/**
	 * Background color, also used where CSS does not reach (scene fill)
	 */
	public static final Color BACKGROUND = Color.web("#0b0e14");

	public static final Color ACCENT = Color.web("#ffd400");
	public static final Color PLAYER_1 = Color.web("#2ec4ff");
	public static final Color PLAYER_2 = Color.web("#ff3d6e");

	/**
	 * Goes a bit past the end and comes back, for things that "pop"
	 */
	public static final Interpolator OVERSHOOT = new Interpolator() {
		/** The overshoot easing: goes a little past the end and comes back. */
		@Override
		protected double curve(double time) {
			double overshoot = 1.70158;
			double uCoord = time - 1;
			return uCoord * uCoord * ((overshoot + 1) * uCoord + overshoot) + 1;
		}
	};

	private static final String STYLESHEET = Theme.class.getResource("theme.css").toExternalForm();

	private static final String[] FONTS = { "PressStart2P-Regular.ttf", "Rajdhani-Medium.ttf", "Rajdhani-SemiBold.ttf", "Rajdhani-Bold.ttf" };

	private static boolean fontsLoaded = false;

	/**
	 * Registers the bundled fonts ("Press Start 2P" and "Rajdhani"), must be called
	 * before any screen is created
	 */
	public static synchronized void loadFonts() {
		if (fontsLoaded) {
			return;
		}
		for (String font : FONTS) {
			try (InputStream inputStream = Theme.class.getResourceAsStream("fonts/" + font)) {
				Font.loadFont(inputStream, 12);
			} catch (IOException | NullPointerException nullPointerException) {
				System.err.println("Fonte não carregada: " + font);
			}
		}
		fontsLoaded = true;
	}

	/** Applies the theme to a scene: its fonts and its stylesheet. */
	public static <T extends Scene> T apply(T scene) {
		Theme.loadFonts();
		if (!scene.getStylesheets().contains(STYLESHEET)) {
			// First, so specific stylesheets can override it
			scene.getStylesheets().add(0, STYLESHEET);
		}
		scene.setFill(BACKGROUND);
		return scene;
	}

	// ------------------------------------------------------------- animations

	/**
	 * Blinks forever, like "PRESS START"
	 */
	public static void blink(Node node) {
		Timeline blink = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 1)), new KeyFrame(Duration.millis(550), new KeyValue(node.opacityProperty(), 1)),
				new KeyFrame(Duration.millis(600), new KeyValue(node.opacityProperty(), 0.1)), new KeyFrame(Duration.millis(1000), new KeyValue(node.opacityProperty(), 0.1)));
		blink.setCycleCount(Animation.INDEFINITE);
		blink.play();
	}

	/**
	 * Neon glow that breathes forever
	 */
	public static DropShadow pulseGlow(Node node, Color color) {
		DropShadow glow = new DropShadow(20, color);
		glow.setSpread(0.25);
		node.setEffect(glow);
		Timeline pulse = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 12, Interpolator.EASE_BOTH)),
				new KeyFrame(Duration.millis(800), new KeyValue(glow.radiusProperty(), 34, Interpolator.EASE_BOTH)));
		pulse.setAutoReverse(true);
		pulse.setCycleCount(Animation.INDEFINITE);
		pulse.play();
		return glow;
	}

	/**
	 * A static neon glow
	 */
	public static DropShadow glow(Color color, double radius) {
		DropShadow glow = new DropShadow(radius, color);
		glow.setSpread(0.2);
		return glow;
	}

	/**
	 * How a new screen appears: a quick fade and zoom
	 */
	public static void enter(Node screen) {
		FadeTransition fade = new FadeTransition(Duration.millis(260), screen);
		fade.setFromValue(0);
		fade.setToValue(1);
		ScaleTransition zoom = new ScaleTransition(Duration.millis(260), screen);
		zoom.setFromX(1.04);
		zoom.setFromY(1.04);
		zoom.setToX(1);
		zoom.setToY(1);
		zoom.setInterpolator(Interpolator.EASE_OUT);
		new ParallelTransition(fade, zoom).play();
	}
}
