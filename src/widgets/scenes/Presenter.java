package widgets.scenes;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The picture of the presenter of the event being commented: it comes up from
 * the bottom of the screen when there is something to say and goes down when
 * there is nothing more
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class Presenter extends ImageView {

	private final Map<Topic, Image> images = new EnumMap<Topic, Image>(Topic.class);
	private Topic topic;
	private double hold = 0;
	private boolean up = false;
	private TranslateTransition slide;

	/**
	 * @param height
	 *            how tall it is shown
	 */
	Presenter(double height) {
		this.setFitHeight(height);
		this.setPreserveRatio(true);
		this.setSmooth(true);
		this.setMouseTransparent(true);
		this.setVisible(false);
	}

	/**
	 * The presenter of the topic says something
	 *
	 * @param seconds
	 *            how long it stays on the screen, from now
	 */
	void present(Topic next, double seconds) {
		Image image = this.image(next);
		if (image == null) {
			return;
		}
		this.hold = Math.max(this.hold, seconds);
		if (next != this.topic) {
			this.topic = next;
			this.setImage(image);
			// Another one comes: the new one jumps in
			this.slide(0, this.up ? 14 : this.getFitHeight() + 40, 0, true);
		} else if (!this.up) {
			this.slide(this.getFitHeight() + 40, 0, 0, false);
		}
		this.up = true;
		this.setVisible(true);
	}

	/**
	 * Goes down when nothing is said for a while
	 */
	void tick(double dt) {
		if (!this.up) {
			return;
		}
		this.hold -= dt;
		if (this.hold <= 0) {
			this.up = false;
			this.topic = null;
			this.slide(0, this.getFitHeight() + 40, 0, false);
		}
	}

	private void slide(double from, double to, double delay, boolean bounce) {
		if (this.slide != null) {
			this.slide.stop();
		}
		this.slide = new TranslateTransition(Duration.millis(bounce ? 320 : 280), this);
		this.slide.setFromY(bounce ? to : from);
		this.slide.setToY(bounce ? 0 : to);
		this.slide.setInterpolator(bounce ? Theme.OVERSHOOT : Interpolator.EASE_BOTH);
		if (!bounce && to > 0) {
			this.slide.setOnFinished(event -> this.setVisible(this.up));
		}
		this.slide.play();
	}

	/**
	 * The picture, null if it does not exist
	 */
	private Image image(Topic topic) {
		return this.images.computeIfAbsent(topic, t -> {
			try (InputStream in = Presenter.class.getResourceAsStream(t.image())) {
				// Loaded bigger than shown, it stays sharp
				return in == null ? null : new Image(in, 0, this.getFitHeight() * 2, true, true);
			} catch (IOException e) {
				return null;
			}
		});
	}
}
