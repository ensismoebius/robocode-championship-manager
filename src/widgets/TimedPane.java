package widgets;

import javafx.animation.AnimationTimer;
import javafx.scene.layout.Pane;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * A pane that animates itself: {@link #step} runs at every frame while the pane
 * is in a scene, and stops when it leaves it. Mouse events go through it.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public abstract class TimedPane extends Pane {

	/** The time of a frame never goes over this, in seconds */
	private final double maxDelta;

	private final AnimationTimer timer = new AnimationTimer() {
		private long last = -1;

		@Override
		public void handle(long now) {
			double delta = this.last < 0 ? 0 : Math.min(TimedPane.this.maxDelta, (now - this.last) / 1e9);
			this.last = now;
			TimedPane.this.step(delta, now / 1e9);
		}
	};

	/**
	 * @param maxDelta
	 *            the longest frame the animation takes, in seconds, so a pause
	 *            does not jump the particles; Double.MAX_VALUE for no limit
	 */
	protected TimedPane(double maxDelta) {
		this.maxDelta = maxDelta;
		this.setMouseTransparent(true);
		this.sceneProperty().addListener((observable, oldScene, newScene) -> {
			if (newScene == null) {
				this.timer.stop();
			} else {
				this.timer.start();
			}
		});
	}

	/**
	 * One frame of the animation
	 *
	 * @param delta
	 *            seconds since the last frame
	 * @param seconds
	 *            the time of the animation, from the start of the clock
	 */
	protected abstract void step(double delta, double seconds);
}
