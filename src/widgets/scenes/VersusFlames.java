package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.beans.binding.DoubleExpression;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import widgets.Theme;

/**
 * Fire rising from the edges of the "VS" letters: the particles start on the letters, climb, sway and fade out
 *
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class VersusFlames extends Canvas {

	private static final int MAX_PARTICLES = 260;
	private static final double SPAWN_PER_SECOND = 220;
	/** Height of the space above the letters where the flames go, relative to the height of the letters */
	private static final double HEADROOM = 0.5;

	private static final Color HOT = Color.web("#fff7b0");
	private static final Color COOL = Color.web("#ff5a1f");
	private static final Color EMBER = Color.web("#5a0a00");

	private final Image letters;
	private final DoubleExpression width;
	private final List<double[]> edges;
	private final List<Particle> particles = new ArrayList<>();
	private final Random random = new Random();
	private final AnimationTimer timer;
	private long lastFrame;
	private double spawnBudget;

	/**
	 * @param letters
	 *            picture of the letters, its edges are where the flames start
	 * @param width
	 *            width the letters are shown with, the flames follow it
	 */
	VersusFlames(Image letters, DoubleExpression width) {
		this.letters = letters;
		this.width = width;
		this.edges = edgePixels(letters);
		this.setMouseTransparent(true);
		this.widthProperty().bind(width);
		this.heightProperty().bind(width.multiply(letters.getHeight() / letters.getWidth()).multiply(1 + HEADROOM));

		this.timer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				double elapsed = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000.0;
				lastFrame = now;
				update(Math.min(elapsed, 0.05));
				draw();
			}
		};
	}

	/** Starts the flames. */
	void start() {
		lastFrame = 0;
		this.timer.start();
	}

	/** Stops the flames and removes them from the screen. */
	void stop() {
		this.timer.stop();
		this.particles.clear();
		this.getGraphicsContext2D().clearRect(0, 0, this.getWidth(), this.getHeight());
	}

	/** Creates the new flames, moves the particles and removes the ones that faded out. */
	private void update(double elapsed) {
		if (!this.edges.isEmpty()) {
			double scale = this.width.get() / this.letters.getWidth();
			double headroom = this.getHeight() - this.letters.getHeight() * scale;

			this.spawnBudget = Math.min(this.spawnBudget + SPAWN_PER_SECOND * elapsed, 2);
			while (this.spawnBudget >= 1 && this.particles.size() < MAX_PARTICLES) {
				this.spawn(scale, headroom);
				this.spawnBudget--;
			}
		}

		for (Particle particle : this.particles) {
			particle.advance(elapsed);
		}
		this.particles.removeIf(particle -> particle.age >= particle.life);
	}

	/** A new flame starting on a random edge of the letters. */
	private void spawn(double scale, double headroom) {
		double[] edge = this.edges.get(this.random.nextInt(this.edges.size()));
		Particle particle = new Particle();
		particle.x = edge[0] * scale;
		particle.y = headroom + edge[1] * scale;
		particle.life = 0.6 + this.random.nextDouble() * 0.8;
		particle.rise = this.getHeight() * (0.35 + this.random.nextDouble() * 0.5);
		particle.radius = this.getWidth() * (0.012 + this.random.nextDouble() * 0.02);
		particle.sway = this.getWidth() * (0.01 + this.random.nextDouble() * 0.02);
		particle.phase = this.random.nextDouble() * Math.PI * 2;
		this.particles.add(particle);
	}

	/** Paints the particles, with additive blending so they glow where they overlap. */
	private void draw() {
		GraphicsContext graphics = this.getGraphicsContext2D();
		graphics.clearRect(0, 0, this.getWidth(), this.getHeight());
		graphics.setGlobalBlendMode(BlendMode.ADD);

		for (Particle particle : this.particles) {
			double progress = particle.age / particle.life;
			double alpha = 1 - progress;
			Color heat = heat(progress);
			double radius = particle.radius * (1 - 0.6 * progress);

			graphics.setFill(Color.color(heat.getRed(), heat.getGreen(), heat.getBlue(), alpha * 0.25));
			graphics.fillOval(particle.x - radius * 2.2, particle.y - radius * 2.2, radius * 4.4, radius * 4.4);
			graphics.setFill(Color.color(heat.getRed(), heat.getGreen(), heat.getBlue(), alpha * 0.9));
			graphics.fillOval(particle.x - radius, particle.y - radius, radius * 2, radius * 2);
		}
	}

	/** The color of a flame along its life: hot yellow at the start, orange, then dark red as it fades. */
	private static Color heat(double progress) {
		if (progress < 0.2) {
			return mix(HOT, Theme.ACCENT, progress / 0.2);
		}
		if (progress < 0.6) {
			return mix(Theme.ACCENT, COOL, (progress - 0.2) / 0.4);
		}
		return mix(COOL, EMBER, (progress - 0.6) / 0.4);
	}

	/** The color between two colors: the start when the amount is zero, the end when it is one. */
	private static Color mix(Color from, Color target, double amount) {
		return Color.color(
				from.getRed() + (target.getRed() - from.getRed()) * amount,
				from.getGreen() + (target.getGreen() - from.getGreen()) * amount,
				from.getBlue() + (target.getBlue() - from.getBlue()) * amount);
	}

	/**
	 * The pixels of the letters where a flame can start: the ones on the top, counted twice so the flames
	 * come mostly from above, and the ones on the sides
	 *
	 * @return pairs of x and y, in pixels of the picture
	 */
	private static List<double[]> edgePixels(Image image) {
		PixelReader reader = image.getPixelReader();
		int width = (int) image.getWidth();
		int height = (int) image.getHeight();
		List<double[]> edges = new ArrayList<>();
		if (reader == null) {
			return edges;
		}

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (alpha(reader, x, y, width, height) < 128) {
					continue;
				}
				if (alpha(reader, x, y - 1, width, height) < 64) {
					edges.add(new double[] { x, y });
					edges.add(new double[] { x, y });
				} else if (alpha(reader, x - 1, y, width, height) < 64 || alpha(reader, x + 1, y, width, height) < 64) {
					edges.add(new double[] { x, y });
				}
			}
		}
		return edges;
	}

	/** The opacity of a pixel, zero outside the picture. */
	private static int alpha(PixelReader reader, int x, int y, int width, int height) {
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return 0;
		}
		return reader.getArgb(x, y) >>> 24;
	}

	/** A flame: where it is, how old it is and how it moves. */
	private static final class Particle {
		double x;
		double y;
		double age;
		double life;
		double rise;
		double radius;
		double sway;
		double phase;

		/** Climbs and sways from side to side. */
		void advance(double elapsed) {
			this.age += elapsed;
			this.y -= this.rise * elapsed;
			this.x += Math.sin(this.age * 7 + this.phase) * this.sway * elapsed;
		}
	}
}
