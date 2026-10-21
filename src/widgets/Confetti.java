package widgets;

import java.util.Random;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Pixel confetti falling forever, for the champion. Animates only while it is
 * in a showing window.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Confetti extends TimedPane {

	private static final Color[] COLORS = { Color.web("#ffd400"), Color.web("#2ec4ff"), Color.web("#ff3d6e"), Color.web("#5af08f"), Color.web("#c86bff"), Color.WHITE };
	private static final int PIECES = 180;

	private final Canvas canvas = new Canvas();
	private final Random random = new Random();

	// x, y (0..1 of the screen), speed, sway phase, size, color
	private final double[][] pieces = new double[PIECES][6];

	public Confetti() {
		super(Double.MAX_VALUE);
		this.getChildren().add(this.canvas);
		for (double[] piece : this.pieces) {
			this.reset(piece);
			// Starts spread over the screen, not all at the top
			piece[1] = this.random.nextDouble() * -1.2;
		}
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

	@Override
	protected void step(double delta, double seconds) {
		double width = this.canvas.getWidth();
		double height = this.canvas.getHeight();
		GraphicsContext graphicsContext = this.canvas.getGraphicsContext2D();
		graphicsContext.clearRect(0, 0, width, height);

		for (double[] piece : this.pieces) {
			piece[1] += piece[2] * delta;
			if (piece[1] > 1.05) {
				this.reset(piece);
			}
			double posX = piece[0] * width + Math.sin(seconds * 2 + piece[3]) * 18;
			double posY = piece[1] * height;
			// Flipping, like a paper falling
			double flip = Math.abs(Math.cos(seconds * 4 + piece[3]));
			graphicsContext.setFill(COLORS[(int) piece[5]]);
			graphicsContext.fillRect(posX, posY, piece[4], Math.max(2, piece[4] * flip));
		}
	}
}
