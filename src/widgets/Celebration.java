package widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
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
public class Celebration extends TimedPane {

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

	/** Creates the celebration, with its canvas, which is redrawn while it is shown. */
	public Celebration() {
		super(0.05);
		this.getChildren().add(this.canvas);
	}

	/** Resizes the canvas to the size of the celebration and forgets the cached paints. */
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
	public void burst(double posX, double posY, Color color, int count) {
		double width = Math.max(1, this.canvas.getWidth());
		double height = Math.max(1, this.canvas.getHeight());
		this.explode(posX * width, posY * height, color, count);
	}

	/**
	 * A rocket that rises from the bottom and bursts
	 */
	public void rocket(double posX, Color color, int count) {
		Rocket rocket = new Rocket();
		rocket.x = posX * Math.max(1, this.canvas.getWidth());
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
			for (int index = 0; index < perCannon; index++) {
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
	public void popper(double posX, double posY, int count, Color... colors) {
		double width = Math.max(1, this.canvas.getWidth());
		double height = Math.max(1, this.canvas.getHeight());
		for (int index = 0; index < count; index++) {
			Piece piece = this.piece();
			if (colors != null && colors.length > 0) {
				piece.color = colors[this.random.nextInt(colors.length)];
			}
			piece.x = posX * width;
			piece.y = posY * height;
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

	/** A new piece of confetti, with a random colour and size. */
	private Piece piece() {
		Piece piece = new Piece();
		piece.color = PARTY[this.random.nextInt(PARTY.length)];
		piece.size = 7 + this.random.nextInt(4) * 3;
		piece.phase = this.random.nextDouble() * Math.PI * 2;
		return piece;
	}

	/** Makes a burst of pieces at the given position, with the colour and the number of pieces. */
	private void explode(double posX, double posY, Color color, int count) {
		double height = Math.max(1, this.canvas.getHeight());
		double power = height * 0.5;
		// Some bursts have a ring, some are round, all with a few white sparks
		boolean ring = this.random.nextInt(3) == 0;
		for (int index = 0; index < count; index++) {
			Spark spark = new Spark();
			spark.x = spark.previousX = posX;
			spark.y = spark.previousY = posY;
			double angle = this.random.nextDouble() * Math.PI * 2;
			double speed = power * (ring ? 0.85 + this.random.nextDouble() * 0.1 : 0.15 + this.random.nextDouble() * 0.85);
			spark.vx = Math.cos(angle) * speed;
			spark.vy = Math.sin(angle) * speed;
			spark.life = 1.1 + this.random.nextDouble() * 0.9;
			spark.size = 2 + this.random.nextDouble() * 2.2;
			spark.color = index % 7 == 0 ? Color.WHITE : color;
			this.sparks.add(spark);
		}
	}

	// ------------------------------------------------------------ animation

	/** Moves the celebration by the time that passed: the bursts, the pieces and the rays. */
	@Override
	protected void step(double delta, double seconds) {
		double width = this.canvas.getWidth();
		double height = this.canvas.getHeight();
		if (width < 2 || height < 2) {
			return;
		}
		GraphicsContext graphicsContext = this.canvas.getGraphicsContext2D();
		graphicsContext.clearRect(0, 0, width, height);
		graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);

		this.boost = Math.max(0, this.boost - delta * 0.9);
		this.drawRays(graphicsContext, width, height, seconds);
		this.launch(delta, width, height);
		this.flyRockets(delta, height);
		this.drawSparks(graphicsContext, delta, height);
		this.drawPieces(graphicsContext, delta, height, seconds);
	}

	/** Rockets launched on their own and the confetti raining */
	private void launch(double delta, double width, double height) {
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
	}

	/** The rockets rise, leaving sparks, and burst */
	private void flyRockets(double delta, double height) {
		for (int index = this.rockets.size() - 1; index >= 0; index--) {
			Rocket rocket = this.rockets.get(index);
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
				this.rockets.remove(index);
			}
		}

	}

	/** The sparks of the fireworks add their light */
	private void drawSparks(GraphicsContext graphicsContext, double delta, double height) {
		graphicsContext.setGlobalBlendMode(BlendMode.ADD);
		for (int index = this.sparks.size() - 1; index >= 0; index--) {
			Spark spark = this.sparks.get(index);
			spark.age += delta;
			if (spark.age >= spark.life) {
				this.sparks.remove(index);
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
			graphicsContext.setStroke(Color.color(spark.color.getRed(), spark.color.getGreen(), spark.color.getBlue(), Math.min(1, fade * 1.4)));
			graphicsContext.setLineWidth(spark.size * (0.5 + 0.5 * fade));
			graphicsContext.strokeLine(spark.previousX, spark.previousY, spark.x, spark.y);
		}
		graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);

	}

	/** Confetti flips like paper falling */
	private void drawPieces(GraphicsContext graphicsContext, double delta, double height, double seconds) {
		for (int index = this.pieces.size() - 1; index >= 0; index--) {
			Piece piece = this.pieces.get(index);
			piece.age += delta;
			if (piece.age >= piece.life || piece.y > height + 20) {
				this.pieces.remove(index);
				continue;
			}
			piece.vx *= Math.exp(-0.9 * delta);
			piece.vy = piece.vy * Math.exp(-0.8 * delta) + height * 0.28 * delta;
			// Terminal speed, like paper
			piece.vy = Math.min(piece.vy, height * 0.32);
			piece.x += (piece.vx + Math.sin(seconds * 2.5 + piece.phase) * 36) * delta;
			piece.y += piece.vy * delta;
			double flip = Math.abs(Math.cos(seconds * 6 + piece.phase));
			graphicsContext.setFill(piece.color);
			graphicsContext.fillRect(piece.x, piece.y, piece.size, Math.max(2, piece.size * flip));
		}
	}

	/** Draws the rays of light behind the celebration, when there is a ray colour. */
	private void drawRays(GraphicsContext graphicsContext, double width, double height, double seconds) {
		if (this.rayColor == null) {
			return;
		}
		double cxValue = this.rayCenterX * width;
		double cyValue = this.rayCenterY * height;
		double reach = Math.hypot(Math.max(cxValue, width - cxValue), Math.max(cyValue, height - cyValue)) * 1.1;
		if (this.rayPaint == null || this.rayPaintSize != reach) {
			Color color2 = this.rayColor;
			this.rayPaint = new RadialGradient(0, 0, cxValue, cyValue, reach, false, CycleMethod.NO_CYCLE, new Stop(0, Color.color(color2.getRed(), color2.getGreen(), color2.getBlue(), 0.55)),
					new Stop(0.6, Color.color(color2.getRed(), color2.getGreen(), color2.getBlue(), 0.16)), new Stop(1, Color.color(color2.getRed(), color2.getGreen(), color2.getBlue(), 0)));
			this.rayPaintSize = reach;
		}
		graphicsContext.setGlobalBlendMode(BlendMode.ADD);
		graphicsContext.setGlobalAlpha(Math.min(1, 0.75 + this.boost));
		graphicsContext.setFill(this.rayPaint);
		int rays = 16;
		double turn = seconds * this.raySpeed;
		for (int index = 0; index < rays; index++) {
			// Wide and narrow rays alternate and breathe
			double angle = turn + Math.PI * 2 * index / rays;
			double half = Math.PI / rays * (0.35 + 0.2 * Math.sin(seconds * 1.7 + index * 1.3) + (index % 2 == 0 ? 0.15 : 0));
			graphicsContext.fillPolygon(new double[] { cxValue, cxValue + Math.cos(angle - half) * reach, cxValue + Math.cos(angle + half) * reach }, new double[] { cyValue, cyValue + Math.sin(angle - half) * reach,
					cyValue + Math.sin(angle + half) * reach }, 3);
		}
		graphicsContext.setGlobalAlpha(1);
		graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);
	}
}
