package widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * A layer of celebration over a screen: rotating rays of light, fireworks that
 * rise and burst, cannons of confetti and a rain of it. Everything is drawn on
 * one canvas, over a transparent background. It animates only while it is in a
 * showing window.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Celebration extends Pane {

	private static final Color[] PARTY = { Color.web("#ffd400"), Color.web("#2ec4ff"), Color.web("#ff3d6e"), Color.web("#5af08f"), Color.web("#c86bff"), Color.WHITE };
	private static final double GRAVITY = 0.55;

	/** A spark of a firework, thrown by a rocket or a burst */
	private static final class Spark {
		double x, y, previousX, previousY, vx, vy, age, life, size;
		Color color;
	}

	/** A rocket going up, to burst */
	private static final class Rocket {
		double x, y, vy, fuse;
		Color color;
		int count;
	}

	/** A piece of confetti */
	private static final class Piece {
		double x, y, vx, vy, phase, size, age, life;
		Color color;
	}

	private final Canvas canvas = new Canvas();
	private final Random random = new Random();
	private final List<Spark> sparks = new ArrayList<Spark>();
	private final List<Rocket> rockets = new ArrayList<Rocket>();
	private final List<Piece> pieces = new ArrayList<Piece>();

	private Color rayColor = null;
	private double rayCenterX = 0.5;
	private double rayCenterY = 0.45;
	private double raySpeed = 0.18;
	/** Rockets per second launched on their own, 0 for none */
	private double autoRockets = 0;
	private double rain = 0;
	private double rainDebt = 0;
	private double rocketDebt = 0;
	private Color[] palette = PARTY;
	private double boost = 0;
	private RadialGradient rayPaint;
	private double rayPaintSize;

	private final AnimationTimer timer = new AnimationTimer() {
		private long last = -1;

		@Override
		public void handle(long now) {
			double delta = this.last < 0 ? 0 : Math.min(0.05, (now - this.last) / 1e9);
			this.last = now;
			Celebration.this.step(delta, now / 1e9);
		}
	};

	public Celebration() {
		this.getChildren().add(this.canvas);
		this.setMouseTransparent(true);
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
		this.rayPaint = null;
	}

	// -------------------------------------------------------------- settings

	/**
	 * Rays of light turning around a point of the screen (0 to 1), null for none
	 */
	public void setRays(Color color, double centerX, double centerY) {
		this.rayColor = color;
		this.rayCenterX = centerX;
		this.rayCenterY = centerY;
		this.rayPaint = null;
	}

	/**
	 * Fireworks launched on their own, in these colors (null: party colors)
	 */
	public void setAutoFireworks(double perSecond, Color... colors) {
		this.autoRockets = perSecond;
		this.palette = colors == null || colors.length == 0 ? PARTY : colors;
	}

	/**
	 * A rain of confetti, in pieces per second
	 */
	public void setRain(double perSecond) {
		this.rain = perSecond;
	}

	/**
	 * Everything stronger for a moment: the rays blaze
	 */
	public void flare() {
		this.boost = 1;
	}

	// --------------------------------------------------------------- actions

	/**
	 * A firework that bursts at once at a point of the screen (0 to 1)
	 */
	public void burst(double x, double y, Color color, int count) {
		double width = Math.max(1, this.canvas.getWidth());
		double height = Math.max(1, this.canvas.getHeight());
		this.explode(x * width, y * height, color, count);
	}

	/**
	 * A rocket that rises from the bottom and bursts
	 */
	public void rocket(double x, Color color, int count) {
		Rocket rocket = new Rocket();
		rocket.x = x * Math.max(1, this.canvas.getWidth());
		rocket.y = this.canvas.getHeight();
		double height = Math.max(1, this.canvas.getHeight());
		rocket.vy = -(height * (0.9 + this.random.nextDouble() * 0.5));
		rocket.fuse = 0.55 + this.random.nextDouble() * 0.35;
		rocket.color = color;
		rocket.count = count;
		this.rockets.add(rocket);
	}

	/**
	 * Cannons in the bottom corners shoot confetti up and to the middle
	 */
	public void cannons(int perCannon) {
		double width = Math.max(1, this.canvas.getWidth());
		double height = Math.max(1, this.canvas.getHeight());
		for (int side = 0; side < 2; side++) {
			for (int i = 0; i < perCannon; i++) {
				Piece piece = this.piece();
				piece.x = side == 0 ? 0 : width;
				piece.y = height * 0.95;
				double angle = Math.toRadians(side == 0 ? 55 + this.random.nextDouble() * 25 : 125 - this.random.nextDouble() * 25);
				double speed = height * (0.7 + this.random.nextDouble() * 0.9);
				piece.vx = Math.cos(angle) * speed;
				piece.vy = -Math.sin(angle) * speed;
				piece.life = 4 + this.random.nextDouble() * 3;
				this.pieces.add(piece);
			}
		}
	}

	/**
	 * Confetti thrown up from the point (0 to 1), like a party popper
	 */
	public void popper(double x, double y, int count, Color... colors) {
		double width = Math.max(1, this.canvas.getWidth());
		double height = Math.max(1, this.canvas.getHeight());
		for (int i = 0; i < count; i++) {
			Piece piece = this.piece();
			if (colors != null && colors.length > 0) {
				piece.color = colors[this.random.nextInt(colors.length)];
			}
			piece.x = x * width;
			piece.y = y * height;
			double angle = Math.toRadians(60 + this.random.nextDouble() * 60);
			double speed = height * (0.4 + this.random.nextDouble() * 0.6);
			piece.vx = Math.cos(angle) * speed * (this.random.nextBoolean() ? 1 : -1) * 0.8;
			piece.vy = -Math.sin(angle) * speed;
			piece.life = 3 + this.random.nextDouble() * 3;
			this.pieces.add(piece);
		}
	}

	/**
	 * Nothing flying anymore
	 */
	public void clear() {
		this.sparks.clear();
		this.rockets.clear();
		this.pieces.clear();
	}

	private Piece piece() {
		Piece piece = new Piece();
		piece.color = PARTY[this.random.nextInt(PARTY.length)];
		piece.size = 7 + this.random.nextInt(4) * 3;
		piece.phase = this.random.nextDouble() * Math.PI * 2;
		return piece;
	}

	private void explode(double x, double y, Color color, int count) {
		double height = Math.max(1, this.canvas.getHeight());
		double power = height * 0.5;
		// Some bursts have a ring, some are round, all with a few white sparks
		boolean ring = this.random.nextInt(3) == 0;
		for (int i = 0; i < count; i++) {
			Spark spark = new Spark();
			spark.x = spark.previousX = x;
			spark.y = spark.previousY = y;
			double angle = this.random.nextDouble() * Math.PI * 2;
			double speed = power * (ring ? 0.85 + this.random.nextDouble() * 0.1 : 0.15 + this.random.nextDouble() * 0.85);
			spark.vx = Math.cos(angle) * speed;
			spark.vy = Math.sin(angle) * speed;
			spark.life = 1.1 + this.random.nextDouble() * 0.9;
			spark.size = 2 + this.random.nextDouble() * 2.2;
			spark.color = i % 7 == 0 ? Color.WHITE : color;
			this.sparks.add(spark);
		}
	}

	// ------------------------------------------------------------ animation

	private void step(double delta, double seconds) {
		double width = this.canvas.getWidth();
		double height = this.canvas.getHeight();
		if (width < 2 || height < 2) {
			return;
		}
		GraphicsContext g = this.canvas.getGraphicsContext2D();
		g.clearRect(0, 0, width, height);
		g.setGlobalBlendMode(BlendMode.SRC_OVER);

		this.boost = Math.max(0, this.boost - delta * 0.9);
		this.drawRays(g, width, height, seconds);

		// Launches of their own
		this.rocketDebt += this.autoRockets * delta;
		while (this.rocketDebt >= 1) {
			this.rocketDebt--;
			this.rocket(0.1 + this.random.nextDouble() * 0.8, this.palette[this.random.nextInt(this.palette.length)], 70 + this.random.nextInt(60));
		}
		this.rainDebt += this.rain * delta;
		while (this.rainDebt >= 1) {
			this.rainDebt--;
			Piece piece = this.piece();
			piece.x = this.random.nextDouble() * width;
			piece.y = -10;
			piece.vx = (this.random.nextDouble() - 0.5) * 40;
			piece.vy = height * (0.12 + this.random.nextDouble() * 0.12);
			piece.life = 12;
			this.pieces.add(piece);
		}

		// Rockets rise, leaving sparks, and burst
		for (int i = this.rockets.size() - 1; i >= 0; i--) {
			Rocket rocket = this.rockets.get(i);
			rocket.fuse -= delta;
			rocket.y += rocket.vy * delta;
			rocket.vy *= Math.exp(-1.2 * delta);
			Spark trail = new Spark();
			trail.x = trail.previousX = rocket.x + (this.random.nextDouble() - 0.5) * 3;
			trail.y = trail.previousY = rocket.y;
			trail.vx = (this.random.nextDouble() - 0.5) * 20;
			trail.vy = 30;
			trail.life = 0.5;
			trail.size = 2;
			trail.color = Color.web("#ffe9a8");
			this.sparks.add(trail);
			if (rocket.fuse <= 0) {
				this.explode(rocket.x, rocket.y, rocket.color, rocket.count);
				this.rockets.remove(i);
			}
		}

		// The sparks of the fireworks add their light
		g.setGlobalBlendMode(BlendMode.ADD);
		for (int i = this.sparks.size() - 1; i >= 0; i--) {
			Spark spark = this.sparks.get(i);
			spark.age += delta;
			if (spark.age >= spark.life) {
				this.sparks.remove(i);
				continue;
			}
			spark.previousX = spark.x;
			spark.previousY = spark.y;
			double drag = Math.exp(-1.6 * delta);
			spark.vx *= drag;
			spark.vy = spark.vy * drag + height * GRAVITY * delta;
			spark.x += spark.vx * delta;
			spark.y += spark.vy * delta;
			double fade = 1 - spark.age / spark.life;
			g.setStroke(Color.color(spark.color.getRed(), spark.color.getGreen(), spark.color.getBlue(), Math.min(1, fade * 1.4)));
			g.setLineWidth(spark.size * (0.5 + 0.5 * fade));
			g.strokeLine(spark.previousX, spark.previousY, spark.x, spark.y);
		}
		g.setGlobalBlendMode(BlendMode.SRC_OVER);

		// Confetti flips like paper falling
		for (int i = this.pieces.size() - 1; i >= 0; i--) {
			Piece piece = this.pieces.get(i);
			piece.age += delta;
			if (piece.age >= piece.life || piece.y > height + 20) {
				this.pieces.remove(i);
				continue;
			}
			piece.vx *= Math.exp(-0.9 * delta);
			piece.vy = piece.vy * Math.exp(-0.8 * delta) + height * 0.28 * delta;
			// Terminal speed, like paper
			piece.vy = Math.min(piece.vy, height * 0.32);
			piece.x += (piece.vx + Math.sin(seconds * 2.5 + piece.phase) * 36) * delta;
			piece.y += piece.vy * delta;
			double flip = Math.abs(Math.cos(seconds * 6 + piece.phase));
			g.setFill(piece.color);
			g.fillRect(piece.x, piece.y, piece.size, Math.max(2, piece.size * flip));
		}
	}

	private void drawRays(GraphicsContext g, double width, double height, double seconds) {
		if (this.rayColor == null) {
			return;
		}
		double cx = this.rayCenterX * width;
		double cy = this.rayCenterY * height;
		double reach = Math.hypot(Math.max(cx, width - cx), Math.max(cy, height - cy)) * 1.1;
		if (this.rayPaint == null || this.rayPaintSize != reach) {
			Color c = this.rayColor;
			this.rayPaint = new RadialGradient(0, 0, cx, cy, reach, false, CycleMethod.NO_CYCLE, new Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.55)),
					new Stop(0.6, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.16)), new Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0)));
			this.rayPaintSize = reach;
		}
		g.setGlobalBlendMode(BlendMode.ADD);
		g.setGlobalAlpha(Math.min(1, 0.75 + this.boost));
		g.setFill(this.rayPaint);
		int rays = 16;
		double turn = seconds * this.raySpeed;
		for (int i = 0; i < rays; i++) {
			// Wide and narrow rays alternate and breathe
			double angle = turn + Math.PI * 2 * i / rays;
			double half = Math.PI / rays * (0.35 + 0.2 * Math.sin(seconds * 1.7 + i * 1.3) + (i % 2 == 0 ? 0.15 : 0));
			g.fillPolygon(new double[] { cx, cx + Math.cos(angle - half) * reach, cx + Math.cos(angle + half) * reach }, new double[] { cy, cy + Math.sin(angle - half) * reach,
					cy + Math.sin(angle + half) * reach }, 3);
		}
		g.setGlobalAlpha(1);
		g.setGlobalBlendMode(BlendMode.SRC_OVER);
	}
}
