package widgets;

import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Pixel confetti falling forever, for the champion. Animates only while it is
 * in a showing window.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Confetti extends Pane {

	private static final Color[] COLORS = { Color.web("#ffd400"), Color.web("#2ec4ff"), Color.web("#ff3d6e"), Color.web("#5af08f"), Color.web("#c86bff"), Color.WHITE };
	private static final int PIECES = 180;

	private final Canvas canvas = new Canvas();
	private final Random random = new Random();

	// x, y (0..1 of the screen), speed, sway phase, size, color
	private final double[][] pieces = new double[PIECES][6];

	private final AnimationTimer timer = new AnimationTimer() {
		private long last = -1;

		@Override
		public void handle(long now) {
			double delta = this.last < 0 ? 0 : (now - this.last) / 1e9;
			this.last = now;
			Confetti.this.step(delta, now / 1e9);
		}
	};

	public Confetti() {
		this.getChildren().add(this.canvas);
		this.setMouseTransparent(true);
		for (double[] piece : this.pieces) {
			this.reset(piece);
			// Starts spread over the screen, not all at the top
			piece[1] = this.random.nextDouble() * -1.2;
		}
		this.sceneProperty().addListener((observable, oldScene, newScene) -> {
			if (newScene == null) {
				this.timer.stop();
			} else {
				this.timer.start();
			}
		});
	}

	private void reset(double[] piece) {
		piece[0] = this.random.nextDouble();
		piece[1] = -0.05;
		piece[2] = 0.08 + this.random.nextDouble() * 0.12;
		piece[3] = this.random.nextDouble() * Math.PI * 2;
		piece[4] = 6 + this.random.nextInt(3) * 3;
		piece[5] = this.random.nextInt(COLORS.length);
	}

	@Override
	protected void layoutChildren() {
		this.canvas.setWidth(this.getWidth());
		this.canvas.setHeight(this.getHeight());
	}

	private void step(double delta, double seconds) {
		double width = this.canvas.getWidth();
		double height = this.canvas.getHeight();
		GraphicsContext g = this.canvas.getGraphicsContext2D();
		g.clearRect(0, 0, width, height);

		for (double[] piece : this.pieces) {
			piece[1] += piece[2] * delta;
			if (piece[1] > 1.05) {
				this.reset(piece);
			}
			double x = piece[0] * width + Math.sin(seconds * 2 + piece[3]) * 18;
			double y = piece[1] * height;
			// Flipping, like a paper falling
			double flip = Math.abs(Math.cos(seconds * 4 + piece[3]));
			g.setFill(COLORS[(int) piece[5]]);
			g.fillRect(x, y, piece[4], Math.max(2, piece[4] * flip));
		}
	}
}
