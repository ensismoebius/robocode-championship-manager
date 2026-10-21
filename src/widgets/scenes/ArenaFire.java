package widgets.scenes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The fire of the battle arena: the flames, sparks and smoke of the robots that
 * burn, and the embers, snow and fireflies of each style. The particles are
 * pooled and drawn as sprites that face the camera.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaFire {

	private final BattleArena3D host;
	/** Embers or snow still owed */
	private double ambientDebt = 0;
	private final List<Particle> particles = new ArrayList<Particle>();
	private final List<Particle> particlePool = new ArrayList<Particle>();

	ArenaFire(BattleArena3D host) {
		this.host = host;
	}

	enum ParticleKind {
		FLAME, SMOKE, SPARK, EMBER, SNOW, FIREFLY
	}

	/** A quad of size 1 facing -Z, shared by all the sprites */
	private static final TriangleMesh SPRITE = sprite();
	private static final Image FLAME_TEXTURE = flameTexture();
	private static final Image SMOKE_TEXTURE = smokeTexture();
	private static final Image SOFT_TEXTURE = softTexture();

	/**
	 * A soft white dot, for embers and snow
	 */
	private static Image softTexture() {
		int size = 32;
		WritableImage image = new WritableImage(size, size);
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double radius = Math.hypot((px + 0.5) / size * 2 - 1, (py + 0.5) / size * 2 - 1);
				double alpha = Math.max(0, 1 - radius);
				image.getPixelWriter().setColor(px, py, Color.color(1, 1, 1, Math.min(1, alpha * alpha * 1.6)));
			}
		}
		return image;
	}

	private static TriangleMesh sprite() {
		TriangleMesh mesh = new TriangleMesh();
		mesh.getPoints().addAll(-0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f, 0);
		mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
		mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
		return mesh;
	}

	/**
	 * A tongue of fire: white hot at the base, round at the bottom and pointed at
	 * the top, soft borders
	 */
	private static Image flameTexture() {
		int size = 64;
		WritableImage image = new WritableImage(size, size);
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double u = (px + 0.5) / size * 2 - 1;
				// 1 at the top, -1 at the bottom
				double v = 1 - (py + 0.5) / size * 2;
				// Narrower going up
				double width = 0.62 * Math.pow(Math.max(0, (1 - v) / 2), 0.55) + 0.04;
				double center = -0.25;
				double dx = u / width;
				double dy = v < center ? (v - center) / 0.75 : (v - center) / 1.2;
				double distance = Math.sqrt(dx * dx + dy * dy);
				double alpha = Math.max(0, 1 - distance);
				alpha = alpha * alpha * (3 - 2 * alpha);
				double heat = Math.max(0, 1 - distance * 1.4);
				image.getPixelWriter().setColor(px, py, Color.color(1, 0.75 + 0.25 * heat, 0.45 + 0.55 * heat, Math.min(1, alpha)));
			}
		}
		return image;
	}

	/**
	 * A puff: soft round blob with some noise, so it does not look like a ball
	 */
	private static Image smokeTexture() {
		int size = 64;
		WritableImage image = new WritableImage(size, size);
		Random random = new Random(7);
		double[] phase = new double[6];
		for (int i = 0; i < phase.length; i++) {
			phase[i] = random.nextDouble() * Math.PI * 2;
		}
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double u = (px + 0.5) / size * 2 - 1;
				double v = (py + 0.5) / size * 2 - 1;
				double angle = Math.atan2(v, u);
				double radius = Math.hypot(u, v);
				// Bumpy border
				double border = 0.78 + 0.08 * Math.sin(3 * angle + phase[0]) + 0.06 * Math.sin(5 * angle + phase[1]) + 0.04 * Math.sin(9 * angle + phase[2]);
				double alpha = Math.max(0, 1 - radius / border);
				alpha = Math.pow(alpha, 0.8);
				// Darker and lighter spots inside
				double grain = 0.85 + 0.15 * Math.sin(u * 7 + phase[3]) * Math.sin(v * 6 + phase[4]) + 0.08 * Math.sin((u + v) * 11 + phase[5]);
				double gray = Math.max(0, Math.min(1, grain));
				image.getPixelWriter().setColor(px, py, Color.color(gray, gray, gray, Math.min(1, alpha * (0.75 + 0.25 * grain))));
			}
		}
		return image;
	}

	/** Farthest first */
	private static final Comparator<Particle> FARTHEST_FIRST = (a, b) -> Double.compare(b.distance, a.distance);
	/** Reused every frame, so it does not make garbage */
	private final List<Node> fireViews = new ArrayList<Node>();

	// The colors of the particles are made once: every one is a step of its life
	// and a level of opacity, and the particles take them from these tables
	// instead of making new colors in every frame
	private static final int LIFE_STEPS = 48;
	private static final int OPACITY_LEVELS = 16;
	private static final int SMOKE_SHADES = 8;
	/** The darkest gray of the smoke, the shades go from 0 to this */
	private static final double SMOKE_SHADE_MAX = 0.25;
	private static final Color[] FLAME_COLORS = new Color[2 * LIFE_STEPS * OPACITY_LEVELS];
	private static final Color[] SMOKE_COLORS = new Color[SMOKE_SHADES * LIFE_STEPS * OPACITY_LEVELS];
	private static final Color[] EMBER_COLORS = new Color[LIFE_STEPS];
	private static final Color[] SPARK_COLORS = new Color[LIFE_STEPS];
	private static final Color[] FIREFLY_COLORS = new Color[OPACITY_LEVELS];
	private static final Color[] SNOW_COLORS = new Color[OPACITY_LEVELS];

	private static int step(double t) {
		return Math.max(0, Math.min(LIFE_STEPS - 1, (int) (t * LIFE_STEPS)));
	}

	private static int level(double opacity) {
		return Math.max(0, Math.min(OPACITY_LEVELS - 1, (int) Math.round(opacity * (OPACITY_LEVELS - 1))));
	}

	private static double middle(int step) {
		return (step + 0.5) / LIFE_STEPS;
	}

	private static double opacity(int level) {
		return (double) level / (OPACITY_LEVELS - 1);
	}

	private static Color withOpacity(Color color, double opacity) {
		return Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
	}

	private static Color flameColor(boolean blue, int step, int level) {
		int index = ((blue ? 1 : 0) * LIFE_STEPS + step) * OPACITY_LEVELS + level;
		Color color = FLAME_COLORS[index];
		if (color == null) {
			double t = middle(step);
			Color base;
			if (blue) {
				// A blue tongue: pale at the base, deep blue at the tip
				base = t < 0.25 ? Color.web("#d6f1ff").interpolate(Color.web("#4aa8ff"), t / 0.25) : Color.web("#4aa8ff").interpolate(Color.web("#2a46ff"), (t - 0.25) / 0.75);
			} else if (t < 0.14) {
				// Every flame is blue where it is born
				base = Color.web("#5aa9ff").interpolate(Color.web("#fff0b8"), t / 0.14);
			} else if (t < 0.3) {
				base = Color.web("#fff0b8").interpolate(Color.web("#ffb12a"), (t - 0.14) / 0.16);
			} else {
				base = Color.web("#ffb12a").interpolate(Color.web("#e8380d"), (t - 0.3) / 0.7);
			}
			color = FLAME_COLORS[index] = withOpacity(base, opacity(level));
		}
		return color;
	}

	private static Color smokeColor(int shade, int step, int level) {
		int index = (shade * LIFE_STEPS + step) * OPACITY_LEVELS + level;
		Color color = SMOKE_COLORS[index];
		if (color == null) {
			double t = middle(step);
			double gray = (shade + 0.5) / SMOKE_SHADES * SMOKE_SHADE_MAX;
			Color base = t < 0.12 ? Color.web("#ff7a2a").interpolate(Color.gray(gray), t / 0.12) : Color.gray(gray).interpolate(Color.gray(Math.min(0.4, gray + 0.12)), (t - 0.12) / 0.88);
			color = SMOKE_COLORS[index] = withOpacity(base, opacity(level));
		}
		return color;
	}

	private static Color emberColor(int step) {
		Color color = EMBER_COLORS[step];
		if (color == null) {
			double t = middle(step);
			double alpha = (t < 0.1 ? t * 10 : 1 - (t - 0.1) / 0.9) * 0.95;
			color = EMBER_COLORS[step] = Color.color(1, 0.62 - 0.4 * t, 0.15 - 0.1 * t, Math.max(0, alpha));
		}
		return color;
	}

	private static Color sparkColor(int step) {
		Color color = SPARK_COLORS[step];
		if (color == null) {
			double t = middle(step);
			color = SPARK_COLORS[step] = Color.color(1, 0.85 - 0.4 * t, 0.4 - 0.3 * t, 1 - t * t);
		}
		return color;
	}

	private static Color fireflyColor(int level) {
		return cachedColor(FIREFLY_COLORS, level, 0.78, 1, 0.35);
	}

	private static Color snowColor(int level) {
		return cachedColor(SNOW_COLORS, level, 0.94, 0.97, 1);
	}

	/** The color of a level of opacity, made the first time it is needed */
	private static Color cachedColor(Color[] cache, int level, double red, double green, double blue) {
		Color color = cache[level];
		if (color == null) {
			color = cache[level] = Color.color(red, green, blue, opacity(level));
		}
		return color;
	}

	/**
	 * A flame, a spark or a puff of smoke: a sprite that always faces the camera.
	 * It lives in the world, so a moving robot leaves a trail of smoke.
	 */
	static final class Particle {
		final MeshView view = new MeshView(SPRITE);
		final PhongMaterial material = new PhongMaterial();
		final Rotate yaw = new Rotate(0, Rotate.Y_AXIS);
		final Rotate pitch = new Rotate(0, Rotate.X_AXIS);
		final Rotate spin = new Rotate(0, Rotate.Z_AXIS);
		final Scale scale = new Scale(1, 1, 1);
		ParticleKind kind;
		double x, y, z;
		double vx, vy, vz;
		double age, life;
		double size, spinSpeed, shade;
		double distance;
		/** The color on the material now */
		Color shown;
		/** Flames go with the robot: x and z are relative to it */
		RobotView anchor;

		/**
		 * Moves along with what emitted it
		 */
		void inherit(double speedX, double speedZ) {
			this.vx += speedX;
			this.vz += speedZ;
		}

		Particle() {
			this.view.setMaterial(this.material);
			this.view.setCullFace(CullFace.NONE);
			this.view.getTransforms().addAll(this.yaw, this.pitch, this.spin, this.scale);
		}
	}

	/** How a particle of each kind starts: its life, size, speed, texture; damage makes the flames bigger and the smoke darker */
	private void setUp(Particle p, ParticleKind kind, double damage) {
	switch (kind) {
	case FLAME -> {
		p.life = 0.35 + host.random.nextDouble() * 0.35;
		// Some tongues burn blue all the way, the others only at the base
		p.shade = host.random.nextDouble();
		p.size = 9 + host.random.nextDouble() * 6 + damage * 8;
		p.vx = (host.random.nextDouble() - 0.5) * 10;
		p.vz = (host.random.nextDouble() - 0.5) * 10;
		p.vy = -(32 + host.random.nextDouble() * 22 + damage * 25);
		// Flames stay upright, only a little sway
		p.spin.setAngle((host.random.nextDouble() - 0.5) * 16);
		p.spinSpeed = (host.random.nextDouble() - 0.5) * 30;
		p.material.setDiffuseMap(FLAME_TEXTURE);
	}
	case SMOKE -> {
		p.life = 1.6 + host.random.nextDouble() * 1.4;
		p.size = 9 + host.random.nextDouble() * 6;
		p.vx = (host.random.nextDouble() - 0.5) * 8 + 6;
		p.vz = (host.random.nextDouble() - 0.5) * 8 + 3;
		p.vy = -(20 + host.random.nextDouble() * 14);
		p.spinSpeed = (host.random.nextDouble() - 0.5) * 70;
		// Thicker and blacker when almost destroyed
		p.shade = 0.16 - 0.1 * damage + host.random.nextDouble() * 0.06;
		p.material.setDiffuseMap(SMOKE_TEXTURE);
	}
	case EMBER -> {
		p.life = 2.5 + host.random.nextDouble() * 2;
		p.size = 1.3 + host.random.nextDouble() * 1.2;
		p.vx = (host.random.nextDouble() - 0.5) * 12;
		p.vz = (host.random.nextDouble() - 0.5) * 12;
		p.vy = -(18 + host.random.nextDouble() * 24);
		p.spinSpeed = 0;
		p.shade = host.random.nextDouble();
		p.material.setDiffuseMap(SOFT_TEXTURE);
	}
	case FIREFLY -> {
		p.life = 5 + host.random.nextDouble() * 4;
		p.size = 2.6 + host.random.nextDouble() * 1.6;
		p.vx = (host.random.nextDouble() - 0.5) * 14;
		p.vz = (host.random.nextDouble() - 0.5) * 14;
		p.vy = (host.random.nextDouble() - 0.5) * 8;
		p.spinSpeed = 0;
		p.shade = host.random.nextDouble();
		p.material.setDiffuseMap(SOFT_TEXTURE);
	}
	case SNOW -> {
		p.life = 12;
		p.size = 2.6 + host.random.nextDouble() * 2.2;
		p.vx = 5 + host.random.nextDouble() * 5;
		p.vz = (host.random.nextDouble() - 0.5) * 6;
		p.vy = 36 + host.random.nextDouble() * 26;
		p.spinSpeed = 0;
		p.shade = host.random.nextDouble();
		p.material.setDiffuseMap(SOFT_TEXTURE);
	}
	case SPARK -> {
		p.life = 0.5 + host.random.nextDouble() * 0.5;
		p.size = 1.6 + host.random.nextDouble() * 1.4;
		double angle = host.random.nextDouble() * Math.PI * 2;
		double speed = 30 + host.random.nextDouble() * 50;
		p.vx = Math.cos(angle) * speed;
		p.vz = Math.sin(angle) * speed;
		p.vy = -(60 + host.random.nextDouble() * 70);
		p.spinSpeed = 0;
		p.material.setDiffuseMap(FLAME_TEXTURE);
	}
	}
	}

	Particle spawn(ParticleKind kind, double x, double y, double z, double damage) {
		Particle p = this.particlePool.isEmpty() ? new Particle() : this.particlePool.remove(this.particlePool.size() - 1);
		p.kind = kind;
		p.anchor = null;
		p.x = x;
		p.y = y;
		p.z = z;
		p.age = 0;
		p.spin.setAngle(host.random.nextDouble() * 360);
		this.setUp(p, kind, damage);
		// Too many: it is made, but not shown
		if (this.particles.size() < 900) {
			this.particles.add(p);
		} else {
			this.particlePool.add(p);
		}
		return p;
	}

	/** The size and the color of a particle at a moment */
	record Look(double width, double height, Color color) {
	}

	/**
	 * Moves, fades and sorts the particles, and lets the robots emit new ones
	 */
	void update(double dt) {
		for (RobotView view : host.robots.values()) {
			view.burn(dt);
		}

		this.spawnAmbient(dt);

		double eyeX = host.eye[0];
		double eyeY = host.eye[1];
		double eyeZ = host.eye[2];
		double yaw = host.cameraYaw.getAngle();
		double pitch = host.cameraPitch.getAngle();

		for (int i = this.particles.size() - 1; i >= 0; i--) {
			Particle p = this.particles.get(i);
			p.age += dt;
			if (p.age >= p.life || p.anchor != null && (p.anchor.dead || !p.anchor.root.isVisible())) {
				this.particles.remove(i);
				this.particlePool.add(p);
				continue;
			}
			double t = p.age / p.life;
			this.moveParticle(p, dt);
			Look look = this.lookOf(p, t);
			this.placeParticle(p, look, yaw, pitch, eyeX, eyeY, eyeZ);
		}

		// No depth sorting in JavaFX: the farthest first, so the nearest blend
		// over them
		this.particles.sort(FARTHEST_FIRST);
		this.fireViews.clear();
		for (Particle p : this.particles) {
			this.fireViews.add(p.view);
		}
		if (!host.fireLayer.getChildren().equals(this.fireViews)) {
			host.fireLayer.getChildren().setAll(this.fireViews);
		}
	}

	/** Embers rise from the lava, snow falls on the ice, fireflies drift in the jungle: below a limit, so the fire of the robots is never left out */
	private void spawnAmbient(double dt) {
	// Embers rise from the lava, snow falls on the ice. Below a limit, so the
	// fire of the robots is never left out.
	double rate = host.style == ArenaStyle.VOLCANO ? 26 : host.style == ArenaStyle.ICE ? 95 : host.style == ArenaStyle.JUNGLE ? 16 : 0;
	this.ambientDebt += dt * rate;
	while (this.ambientDebt >= 1) {
		this.ambientDebt--;
		if (this.particles.size() > 550) {
			continue;
		}
		if (host.style == ArenaStyle.JUNGLE) {
			double x = (host.random.nextDouble() - 0.5) * (host.fieldWidth + 700);
			double z = (host.random.nextDouble() - 0.5) * (host.fieldHeight + 700);
			this.spawn(ParticleKind.FIREFLY, x, -8 - host.random.nextDouble() * 60, z, 0);
		} else if (host.style == ArenaStyle.VOLCANO) {
			this.spawn(ParticleKind.EMBER, (host.random.nextDouble() - 0.5) * (host.fieldWidth + 260), -2, (host.random.nextDouble() - 0.5) * (host.fieldHeight + 260), 0);
		} else {
			this.spawn(ParticleKind.SNOW, (host.random.nextDouble() - 0.5) * (host.fieldWidth + 800), -240 - host.random.nextDouble() * 120,
					(host.random.nextDouble() - 0.5) * (host.fieldHeight + 800), 0);
		}
	}
	}

	/** The motion of a particle: drag, gravity of its kind, and its sway */
	private void moveParticle(Particle p, double dt) {
		double drag = p.kind == ParticleKind.EMBER || p.kind == ParticleKind.SNOW ? 1 : Math.exp(-(p.kind == ParticleKind.SMOKE ? 0.6 : 1.5) * dt);
		p.vx *= drag;
		p.vz *= drag;
		if (p.kind == ParticleKind.SPARK) {
			// Thrown up, then falls
			p.vy += 160 * dt;
		} else if (p.kind == ParticleKind.FLAME) {
			// Flames speed up going up
			p.vy -= 30 * dt;
		}
		p.x += p.vx * dt;
		p.y = Math.min(-1, p.y + p.vy * dt);
		// Sparks go out on the floor
		if ((p.kind == ParticleKind.SPARK || p.kind == ParticleKind.SNOW) && p.y >= -1 && p.vy > 0) {
			p.age = p.life;
		}
		// Embers, snow and fireflies sway
		if (p.kind == ParticleKind.EMBER || p.kind == ParticleKind.SNOW || p.kind == ParticleKind.FIREFLY) {
			p.x += Math.sin(p.age * (p.kind == ParticleKind.EMBER ? 3 : p.kind == ParticleKind.FIREFLY ? 1.3 : 1.7) + p.shade * 6) * (p.kind == ParticleKind.FIREFLY ? 14 : 9) * dt;
			if (p.kind == ParticleKind.FIREFLY) {
				p.z += Math.cos(p.age * 1.1 + p.shade * 9) * 12 * dt;
			}
		}
		p.z += p.vz * dt;
		p.spin.setAngle(p.spin.getAngle() + p.spinSpeed * dt);
	}

	/** How a particle looks at its age, as its size and color */
	private Look lookOf(Particle p, double t) {
		double width;
		double height;
		Color color;
		switch (p.kind) {
		case FLAME -> {
			// Grows fast, then thins out to a tip
			double grow = Math.min(1, t * 5);
			width = p.size * grow * (1 - 0.5 * t);
			height = width * 1.7;
			double flicker = 0.85 + 0.15 * host.random.nextDouble();
			color = flameColor(p.shade < 0.35, step(t), level(Math.min(1, (1 - t) * 1.6) * flicker));
		}
		case SMOKE -> {
			// Born in the fire, grows and fades
			width = p.size * (0.6 + 2.6 * Math.sqrt(t));
			height = width;
			color = smokeColor(Math.max(0, Math.min(SMOKE_SHADES - 1, (int) (p.shade / SMOKE_SHADE_MAX * SMOKE_SHADES))), step(t), level(Math.min(1, t * 6) * Math.pow(1 - t, 1.4) * 0.7));
		}
		case EMBER -> {
			double flicker = 0.7 + 0.3 * host.random.nextDouble();
			width = p.size * (1 - 0.3 * t) * flicker;
			height = width;
			color = emberColor(step(t));
		}
		case FIREFLY -> {
			width = p.size;
			height = width;
			// Fades in and out and blinks
			double blink = 0.5 + 0.5 * Math.sin(p.age * 4.2 + p.shade * 20);
			double envelope = Math.min(1, Math.min(t * 6, (1 - t) * 6));
			color = fireflyColor(level(Math.max(0, envelope * (0.35 + 0.65 * blink))));
		}
		case SNOW -> {
			width = p.size;
			height = width;
			color = snowColor(level(Math.min(0.9, p.age * 1.5)));
		}
		default -> {
			width = p.size * (1 - 0.5 * t);
			height = width;
			color = sparkColor(step(t));
		}
		}
		return new Look(width, height, color);
	}

	/** Places the particle in the world, its material and its orientation towards the camera; keeps its distance to the eye */
	private void placeParticle(Particle p, Look look, double yaw, double pitch, double eyeX, double eyeY, double eyeZ) {
		double width = look.width();
		double height = look.height();
		Color color = look.color();
		// The colors come from tables, the same object again if it did not change
		if (p.shown != color) {
			p.material.setDiffuseColor(color);
			p.shown = color;
		}
		double px = p.x;
		double pz = p.z;
		if (p.anchor != null) {
			px += p.anchor.root.getTranslateX();
			pz += p.anchor.root.getTranslateZ();
		}
		p.view.setTranslateX(px);
		p.view.setTranslateY(p.y);
		p.view.setTranslateZ(pz);
		p.yaw.setAngle(yaw);
		p.pitch.setAngle(pitch);
		p.scale.setX(Math.max(0.01, width));
		p.scale.setY(Math.max(0.01, height));
		// Flames are drawn from their base
		if (p.kind == ParticleKind.FLAME) {
			p.view.setTranslateY(p.y - height * 0.3);
		}
		double dx = px - eyeX;
		double dy = p.y - eyeY;
		double dz = pz - eyeZ;
		p.distance = dx * dx + dy * dy + dz * dz;
	}

	void clear() {
		this.particlePool.addAll(this.particles);
		this.particles.clear();
		host.fireLayer.getChildren().clear();
		for (RobotView view : host.robots.values()) {
			view.flameDebt = view.smokeDebt = view.sparkDebt = 0;
		}
	}
}
