package widgets;

import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Animated arcade background: twinkling stars over a neon perspective grid
 * that scrolls towards the viewer. Animates only while it is in a showing
 * window.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class ArcadeBackground extends Pane {

	private static final int STARS = 140;

	/**
	 * Where the horizon is, as a fraction of the height. Above it is the sky,
	 * below it the grid.
	 */
	public static final double HORIZON = 0.58;

	private final Canvas canvas = new Canvas();
	private final double[][] stars = new double[STARS][3];
	private final Color gridColor;

	private final AnimationTimer timer = new AnimationTimer() {
		private long start = -1;

		/** Draws the background of the frame, with the time since the start of the animation. */
		@Override
		public void handle(long now) {
			if (this.start < 0) {
				this.start = now;
			}
			ArcadeBackground.this.draw((now - this.start) / 1e9);
		}
	};

	/** Creates the background in the default colour. */
	public ArcadeBackground() {
		this(Color.web("#2ec4ff"));
	}

	/**
	 * @param gridColor
	 *            color of the grid lines
	 */
	public ArcadeBackground(Color gridColor) {
		this.gridColor = gridColor;
		this.getChildren().add(this.canvas);
		this.setMouseTransparent(true);

		Random random = new Random(7);
		for (double[] star : this.stars) {
			star[0] = random.nextDouble();
			star[1] = random.nextDouble() * 0.6;
			star[2] = random.nextDouble() * Math.PI * 2;
		}

		// Runs only while visible, so screens that are gone do not use CPU
		this.sceneProperty().addListener((observable, oldScene, newScene) -> {
			if (newScene == null) {
				this.timer.stop();
			} else {
				this.timer.start();
			}
		});
	}

	/** Resizes the canvas to the size of the background. */
	@Override
	protected void layoutChildren() {
		this.canvas.setWidth(this.getWidth());
		this.canvas.setHeight(this.getHeight());
	}

	/** Draws the background for the given time, in seconds from the start. */
	private void draw(double seconds) {
		double width = this.canvas.getWidth();
		double height = this.canvas.getHeight();
		if (width <= 0 || height <= 0) {
			return;
		}
		GraphicsContext graphicsContext = this.canvas.getGraphicsContext2D();
		double horizon = height * HORIZON;

		// Sky
		graphicsContext.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#05060c")), new Stop(0.58, Color.web("#1a1033")), new Stop(1, Color.web("#0b0e14"))));
		graphicsContext.fillRect(0, 0, width, height);

		// Stars
		for (double[] star : this.stars) {
			double twinkle = 0.35 + 0.65 * Math.abs(Math.sin(seconds * 1.3 + star[2]));
			graphicsContext.setFill(Color.gray(1, twinkle * 0.8));
			double size = star[2] > 5 ? 2.5 : 1.5;
			graphicsContext.fillRect(star[0] * width, star[1] * height, size, size);
		}

		// Glow on the horizon
		graphicsContext.setFill(new RadialGradient(0, 0, width / 2, horizon, width * 0.45, false, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#ff3d6e", 0.35)), new Stop(1, Color.TRANSPARENT)));
		graphicsContext.fillRect(0, 0, width, height);

		// Floor
		graphicsContext.setFill(Color.web("#07080f"));
		graphicsContext.fillRect(0, horizon, width, height - horizon);

		graphicsContext.setLineWidth(1.5);

		// Lines going to the vanishing point
		double vanishX = width / 2;
		for (int index = -24; index <= 24; index++) {
			double bottomX = vanishX + index * width / 12;
			graphicsContext.setStroke(this.gridColor.deriveColor(0, 1, 1, 0.45));
			graphicsContext.strokeLine(vanishX + index * 6, horizon, bottomX, height);
		}

		// Horizontal lines moving towards the viewer
		double offset = (seconds * 0.6) % 1.0;
		for (int index2 = 0; index2 < 16; index2++) {
			double depth = (index2 + offset) / 16;
			double posY = horizon + (height - horizon) * depth * depth;
			graphicsContext.setStroke(this.gridColor.deriveColor(0, 1, 1, 0.15 + 0.6 * depth));
			graphicsContext.strokeLine(0, posY, width, posY);
		}

		// The horizon line itself
		graphicsContext.setStroke(Color.web("#ff3d6e", 0.9));
		graphicsContext.setLineWidth(2);
		graphicsContext.strokeLine(0, horizon, width, horizon);
	}
}
