package widgets.scenes;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import widgets.Theme;
import javafx.util.Duration;
import javafx.animation.Interpolator;

/**
 * The entrances of the podium pieces: how a figure, a trophy or a word slams, drops, spins or rises into place,
 * each one after its own delay
 */
final class PodiumAnimations {

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
		fall.setInterpolator(Podium.BOUNCE);
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
	static FadeTransition fadeIn(Node node, double millis) {
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
}
