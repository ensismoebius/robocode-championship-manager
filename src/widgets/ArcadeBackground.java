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

		@Override
		public void handle(long now) {
			if (this.start < 0) {
				this.start = now;
			}
			ArcadeBackground.this.draw((now - this.start) / 1e9);
		}
	};

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

	@Override
	protected void layoutChildren() {
		this.canvas.setWidth(this.getWidth());
		this.canvas.setHeight(this.getHeight());
	}

	private void draw(double seconds) {
		double width = this.canvas.getWidth();
		double height = this.canvas.getHeight();
		if (width <= 0 || height <= 0) {
			return;
		}
		GraphicsContext g = this.canvas.getGraphicsContext2D();
		double horizon = height * HORIZON;

		// Sky
		g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#05060c")), new Stop(0.58, Color.web("#1a1033")), new Stop(1, Color.web("#0b0e14"))));
		g.fillRect(0, 0, width, height);

		// Stars
		for (double[] star : this.stars) {
			double twinkle = 0.35 + 0.65 * Math.abs(Math.sin(seconds * 1.3 + star[2]));
			g.setFill(Color.gray(1, twinkle * 0.8));
			double size = star[2] > 5 ? 2.5 : 1.5;
			g.fillRect(star[0] * width, star[1] * height, size, size);
		}

		// Glow on the horizon
		g.setFill(new RadialGradient(0, 0, width / 2, horizon, width * 0.45, false, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#ff3d6e", 0.35)), new Stop(1, Color.TRANSPARENT)));
		g.fillRect(0, 0, width, height);

		// Floor
		g.setFill(Color.web("#07080f"));
		g.fillRect(0, horizon, width, height - horizon);

		g.setLineWidth(1.5);

		// Lines going to the vanishing point
		double vanishX = width / 2;
		for (int i = -24; i <= 24; i++) {
			double bottomX = vanishX + i * width / 12;
			g.setStroke(this.gridColor.deriveColor(0, 1, 1, 0.45));
			g.strokeLine(vanishX + i * 6, horizon, bottomX, height);
		}

		// Horizontal lines moving towards the viewer
		double offset = (seconds * 0.6) % 1.0;
		for (int i = 0; i < 16; i++) {
			double depth = (i + offset) / 16;
			double y = horizon + (height - horizon) * depth * depth;
			g.setStroke(this.gridColor.deriveColor(0, 1, 1, 0.15 + 0.6 * depth));
			g.strokeLine(0, y, width, y);
		}

		// The horizon line itself
		g.setStroke(Color.web("#ff3d6e", 0.9));
		g.setLineWidth(2);
		g.strokeLine(0, horizon, width, horizon);
	}
}
