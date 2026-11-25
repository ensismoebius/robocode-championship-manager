package widgets;

import javafx.animation.Animation;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Whether a node is on the screen: in a scene, in a window that is showing. What animates or sounds runs only then, so a screen
 * that is gone, or a window that was closed, does not keep using the processor nor the sound, and can be let go.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class OnScreen {

	/** Only static methods: this class is not instantiated. */
	private OnScreen() {
	}

	/** Whether the node is in a window that is showing, as it changes. */
	public static ObservableValue<Boolean> of(Node node) {
		return node.sceneProperty().flatMap(Scene::windowProperty).flatMap(Window::showingProperty).orElse(false);
	}

	/**
	 * Plays the animation while the node is on the screen, from now if it already is, and stops it when the node leaves it. An
	 * animation that runs forever and is never stopped is held by the clock of JavaFX, and holds its node, and the whole screen of
	 * the node, forever.
	 */
	public static <T extends Animation> T animate(Node node, T animation) {
		ObservableValue<Boolean> shown = of(node);
		shown.addListener((observable, was, now) -> {
			if (now) {
				animation.play();
			} else {
				animation.stop();
			}
		});
		if (shown.getValue()) {
			animation.play();
		}
		return animation;
	}

	/** Runs the first action when the node comes on the screen, and the second when it leaves it. */
	public static void whileShown(Node node, Runnable shown, Runnable gone) {
		of(node).addListener((observable, was, now) -> (now ? shown : gone).run());
	}
}
