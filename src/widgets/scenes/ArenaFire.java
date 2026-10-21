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

	/** Creates the fire of the arena, drawn in the host scene. */
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
		for (int posY = 0; posY < size; posY++) {
			for (int posX = 0; posX < size; posX++) {
				double radius = Math.hypot((posX + 0.5) / size * 2 - 1, (posY + 0.5) / size * 2 - 1);
				double alpha = Math.max(0, 1 - radius);
				image.getPixelWriter().setColor(posX, posY, Color.color(1, 1, 1, Math.min(1, alpha * alpha * 1.6)));
			}
		}
		return image;
	}

	/** A square sprite, two triangles, with its texture coordinates. */
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
		for (int posY = 0; posY < size; posY++) {
			for (int posX = 0; posX < size; posX++) {
				double uCoord = (posX + 0.5) / size * 2 - 1;
				// 1 at the top, -1 at the bottom
				double vCoord = 1 - (posY + 0.5) / size * 2;
				// Narrower going up
				double width = 0.62 * Math.pow(Math.max(0, (1 - vCoord) / 2), 0.55) + 0.04;
				double center = -0.25;
				double deltaX = uCoord / width;
				double deltaY = vCoord < center ? (vCoord - center) / 0.75 : (vCoord - center) / 1.2;
				double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
				double alpha = Math.max(0, 1 - distance);
				alpha = alpha * alpha * (3 - 2 * alpha);
				double heat = Math.max(0, 1 - distance * 1.4);
				image.getPixelWriter().setColor(posX, posY, Color.color(1, 0.75 + 0.25 * heat, 0.45 + 0.55 * heat, Math.min(1, alpha)));
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
		for (int index = 0; index < phase.length; index++) {
			phase[index] = random.nextDouble() * Math.PI * 2;
		}
		for (int posY = 0; posY < size; posY++) {
			for (int posX = 0; posX < size; posX++) {
				double uCoord = (posX + 0.5) / size * 2 - 1;
				double vCoord = (posY + 0.5) / size * 2 - 1;
				double angle = Math.atan2(vCoord, uCoord);
				double radius = Math.hypot(uCoord, vCoord);
				// Bumpy border
				double border = 0.78 + 0.08 * Math.sin(3 * angle + phase[0]) + 0.06 * Math.sin(5 * angle + phase[1]) + 0.04 * Math.sin(9 * angle + phase[2]);
				double alpha = Math.max(0, 1 - radius / border);
				alpha = Math.pow(alpha, 0.8);
				// Darker and lighter spots inside
				double grain = 0.85 + 0.15 * Math.sin(uCoord * 7 + phase[3]) * Math.sin(vCoord * 6 + phase[4]) + 0.08 * Math.sin((uCoord + vCoord) * 11 + phase[5]);
				double gray = Math.max(0, Math.min(1, grain));
				image.getPixelWriter().setColor(posX, posY, Color.color(gray, gray, gray, Math.min(1, alpha * (0.75 + 0.25 * grain))));
			}
		}
		return image;
	}

	/** Farthest first */
	private static final Comparator<Particle> FARTHEST_FIRST = (first, second) -> Double.compare(second.distance, first.distance);
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

	/** The index of the life step of a particle, from its life between zero and one. */
	private static int step(double time) {
		return Math.max(0, Math.min(LIFE_STEPS - 1, (int) (time * LIFE_STEPS)));
	}

	/** The index of the opacity level closest to the given opacity. */
	private static int level(double opacity) {
		return Math.max(0, Math.min(OPACITY_LEVELS - 1, (int) Math.round(opacity * (OPACITY_LEVELS - 1))));
	}

	/** The middle of a life step, as a fraction of the life. */
	private static double middle(int step) {
		return (step + 0.5) / LIFE_STEPS;
	}

	/** The opacity of an opacity level. */
	private static double opacity(int level) {
		return (double) level / (OPACITY_LEVELS - 1);
	}

	/** The colour with the given opacity. */
	private static Color withOpacity(Color color, double opacity) {
		return Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
	}

	/** The colour of a flame, cached by its life step and opacity level. */
	private static Color flameColor(boolean blue, int step, int level) {
		int index = ((blue ? 1 : 0) * LIFE_STEPS + step) * OPACITY_LEVELS + level;
		Color color = FLAME_COLORS[index];
		if (color == null) {
			double time = middle(step);
			Color base;
			if (blue) {
				// A blue tongue: pale at the base, deep blue at the tip
				base = time < 0.25 ? Color.web("#d6f1ff").interpolate(Color.web("#4aa8ff"), time / 0.25) : Color.web("#4aa8ff").interpolate(Color.web("#2a46ff"), (time - 0.25) / 0.75);
			} else if (time < 0.14) {
				// Every flame is blue where it is born
				base = Color.web("#5aa9ff").interpolate(Color.web("#fff0b8"), time / 0.14);
			} else if (time < 0.3) {
				base = Color.web("#fff0b8").interpolate(Color.web("#ffb12a"), (time - 0.14) / 0.16);
			} else {
				base = Color.web("#ffb12a").interpolate(Color.web("#e8380d"), (time - 0.3) / 0.7);
			}
			color = FLAME_COLORS[index] = withOpacity(base, opacity(level));
		}
		return color;
	}

	/** The colour of a smoke particle, cached by its shade, life step and opacity level. */
	private static Color smokeColor(int shade, int step, int level) {
		int index = (shade * LIFE_STEPS + step) * OPACITY_LEVELS + level;
		Color color = SMOKE_COLORS[index];
		if (color == null) {
			double time = middle(step);
			double gray = (shade + 0.5) / SMOKE_SHADES * SMOKE_SHADE_MAX;
			Color base = time < 0.12 ? Color.web("#ff7a2a").interpolate(Color.gray(gray), time / 0.12) : Color.gray(gray).interpolate(Color.gray(Math.min(0.4, gray + 0.12)), (time - 0.12) / 0.88);
			color = SMOKE_COLORS[index] = withOpacity(base, opacity(level));
		}
		return color;
	}

	/** The colour of an ember, cached by its life step. */
	private static Color emberColor(int step) {
		Color color = EMBER_COLORS[step];
		if (color == null) {
			double time = middle(step);
			double alpha = (time < 0.1 ? time * 10 : 1 - (time - 0.1) / 0.9) * 0.95;
			color = EMBER_COLORS[step] = Color.color(1, 0.62 - 0.4 * time, 0.15 - 0.1 * time, Math.max(0, alpha));
		}
		return color;
	}

	/** The colour of a spark, cached by its life step. */
	private static Color sparkColor(int step) {
		Color color = SPARK_COLORS[step];
		if (color == null) {
			double time = middle(step);
			color = SPARK_COLORS[step] = Color.color(1, 0.85 - 0.4 * time, 0.4 - 0.3 * time, 1 - time * time);
		}
		return color;
	}

	/** The colour of a firefly for the given opacity level. */
	private static Color fireflyColor(int level) {
		return cachedColor(FIREFLY_COLORS, level, 0.78, 1, 0.35);
	}

	/** The colour of a snowflake for the given opacity level. */
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

		/** Creates a particle with its view, material and transforms. */
		Particle() {
			this.view.setMaterial(this.material);
			this.view.setCullFace(CullFace.NONE);
			this.view.getTransforms().addAll(this.yaw, this.pitch, this.spin, this.scale);
		}
	}

	/** How a particle of each kind starts: its life, size, speed, texture; damage makes the flames bigger and the smoke darker */
	private void setUp(Particle particle, ParticleKind kind, double damage) {
	switch (kind) {
	case FLAME -> {
		particle.life = 0.35 + host.random.nextDouble() * 0.35;
		// Some tongues burn blue all the way, the others only at the base
		particle.shade = host.random.nextDouble();
		particle.size = 9 + host.random.nextDouble() * 6 + damage * 8;
		particle.vx = (host.random.nextDouble() - 0.5) * 10;
		particle.vz = (host.random.nextDouble() - 0.5) * 10;
		particle.vy = -(32 + host.random.nextDouble() * 22 + damage * 25);
		// Flames stay upright, only a little sway
		particle.spin.setAngle((host.random.nextDouble() - 0.5) * 16);
		particle.spinSpeed = (host.random.nextDouble() - 0.5) * 30;
		particle.material.setDiffuseMap(FLAME_TEXTURE);
	}
	case SMOKE -> {
		particle.life = 1.6 + host.random.nextDouble() * 1.4;
		particle.size = 9 + host.random.nextDouble() * 6;
		particle.vx = (host.random.nextDouble() - 0.5) * 8 + 6;
		particle.vz = (host.random.nextDouble() - 0.5) * 8 + 3;
		particle.vy = -(20 + host.random.nextDouble() * 14);
		particle.spinSpeed = (host.random.nextDouble() - 0.5) * 70;
		// Thicker and blacker when almost destroyed
		particle.shade = 0.16 - 0.1 * damage + host.random.nextDouble() * 0.06;
		particle.material.setDiffuseMap(SMOKE_TEXTURE);
	}
	case EMBER -> {
		particle.life = 2.5 + host.random.nextDouble() * 2;
		particle.size = 1.3 + host.random.nextDouble() * 1.2;
		particle.vx = (host.random.nextDouble() - 0.5) * 12;
		particle.vz = (host.random.nextDouble() - 0.5) * 12;
		particle.vy = -(18 + host.random.nextDouble() * 24);
		particle.spinSpeed = 0;
		particle.shade = host.random.nextDouble();
		particle.material.setDiffuseMap(SOFT_TEXTURE);
	}
	case FIREFLY -> {
		particle.life = 5 + host.random.nextDouble() * 4;
		particle.size = 2.6 + host.random.nextDouble() * 1.6;
		particle.vx = (host.random.nextDouble() - 0.5) * 14;
		particle.vz = (host.random.nextDouble() - 0.5) * 14;
		particle.vy = (host.random.nextDouble() - 0.5) * 8;
		particle.spinSpeed = 0;
		particle.shade = host.random.nextDouble();
		particle.material.setDiffuseMap(SOFT_TEXTURE);
	}
	case SNOW -> {
		particle.life = 12;
		particle.size = 2.6 + host.random.nextDouble() * 2.2;
		particle.vx = 5 + host.random.nextDouble() * 5;
		particle.vz = (host.random.nextDouble() - 0.5) * 6;
		particle.vy = 36 + host.random.nextDouble() * 26;
		particle.spinSpeed = 0;
		particle.shade = host.random.nextDouble();
		particle.material.setDiffuseMap(SOFT_TEXTURE);
	}
	case SPARK -> {
		particle.life = 0.5 + host.random.nextDouble() * 0.5;
		particle.size = 1.6 + host.random.nextDouble() * 1.4;
		double angle = host.random.nextDouble() * Math.PI * 2;
		double speed = 30 + host.random.nextDouble() * 50;
		particle.vx = Math.cos(angle) * speed;
		particle.vz = Math.sin(angle) * speed;
		particle.vy = -(60 + host.random.nextDouble() * 70);
		particle.spinSpeed = 0;
		particle.material.setDiffuseMap(FLAME_TEXTURE);
	}
	}
	}

	/** Spawns a particle of the given kind at the given position, with the damage it carries. */
	Particle spawn(ParticleKind kind, double posX, double posY, double posZ, double damage) {
		Particle particle = this.particlePool.isEmpty() ? new Particle() : this.particlePool.remove(this.particlePool.size() - 1);
		particle.kind = kind;
		particle.anchor = null;
		particle.x = posX;
		particle.y = posY;
		particle.z = posZ;
		particle.age = 0;
		particle.spin.setAngle(host.random.nextDouble() * 360);
		this.setUp(particle, kind, damage);
		// Too many: it is made, but not shown
		if (this.particles.size() < 900) {
			this.particles.add(particle);
		} else {
			this.particlePool.add(particle);
		}
		return particle;
	}

	/** The size and the color of a particle at a moment */
	record Look(double width, double height, Color color) {
	}

	/**
	 * Moves, fades and sorts the particles, and lets the robots emit new ones
	 */
	void update(double deltaTime) {
		for (RobotView view : host.robots.values()) {
			view.burn(deltaTime);
		}

		this.spawnAmbient(deltaTime);

		double eyeX = host.eye[0];
		double eyeY = host.eye[1];
		double eyeZ = host.eye[2];
		double yaw = host.cameraYaw.getAngle();
		double pitch = host.cameraPitch.getAngle();

		for (int index = this.particles.size() - 1; index >= 0; index--) {
			Particle particle = this.particles.get(index);
			particle.age += deltaTime;
			if (particle.age >= particle.life || particle.anchor != null && (particle.anchor.dead || !particle.anchor.root.isVisible())) {
				this.particles.remove(index);
				this.particlePool.add(particle);
				continue;
			}
			double time = particle.age / particle.life;
			this.moveParticle(particle, deltaTime);
			Look look = this.lookOf(particle, time);
			this.placeParticle(particle, look, yaw, pitch, eyeX, eyeY, eyeZ);
		}

		// No depth sorting in JavaFX: the farthest first, so the nearest blend
		// over them
		this.particles.sort(FARTHEST_FIRST);
		this.fireViews.clear();
		for (Particle particle2 : this.particles) {
			this.fireViews.add(particle2.view);
		}
		if (!host.fireLayer.getChildren().equals(this.fireViews)) {
			host.fireLayer.getChildren().setAll(this.fireViews);
		}
	}

	/** Embers rise from the lava, snow falls on the ice, fireflies drift in the jungle: below a limit, so the fire of the robots is never left out */
	private void spawnAmbient(double deltaTime) {
	// Embers rise from the lava, snow falls on the ice. Below a limit, so the
	// fire of the robots is never left out.
	double rate = host.style == ArenaStyle.VOLCANO ? 26 : host.style == ArenaStyle.ICE ? 95 : host.style == ArenaStyle.JUNGLE ? 16 : 0;
	this.ambientDebt += deltaTime * rate;
	while (this.ambientDebt >= 1) {
		this.ambientDebt--;
		if (this.particles.size() > 550) {
			continue;
		}
		if (host.style == ArenaStyle.JUNGLE) {
			double posX = (host.random.nextDouble() - 0.5) * (host.fieldWidth + 700);
			double posZ = (host.random.nextDouble() - 0.5) * (host.fieldHeight + 700);
			this.spawn(ParticleKind.FIREFLY, posX, -8 - host.random.nextDouble() * 60, posZ, 0);
		} else if (host.style == ArenaStyle.VOLCANO) {
			this.spawn(ParticleKind.EMBER, (host.random.nextDouble() - 0.5) * (host.fieldWidth + 260), -2, (host.random.nextDouble() - 0.5) * (host.fieldHeight + 260), 0);
		} else {
			this.spawn(ParticleKind.SNOW, (host.random.nextDouble() - 0.5) * (host.fieldWidth + 800), -240 - host.random.nextDouble() * 120,
					(host.random.nextDouble() - 0.5) * (host.fieldHeight + 800), 0);
		}
	}
	}

	/** The motion of a particle: drag, gravity of its kind, and its sway */
	private void moveParticle(Particle particle, double deltaTime) {
		double drag = particle.kind == ParticleKind.EMBER || particle.kind == ParticleKind.SNOW ? 1 : Math.exp(-(particle.kind == ParticleKind.SMOKE ? 0.6 : 1.5) * deltaTime);
		particle.vx *= drag;
		particle.vz *= drag;
		if (particle.kind == ParticleKind.SPARK) {
			// Thrown up, then falls
			particle.vy += 160 * deltaTime;
		} else if (particle.kind == ParticleKind.FLAME) {
			// Flames speed up going up
			particle.vy -= 30 * deltaTime;
		}
		particle.x += particle.vx * deltaTime;
		particle.y = Math.min(-1, particle.y + particle.vy * deltaTime);
		// Sparks go out on the floor
		if ((particle.kind == ParticleKind.SPARK || particle.kind == ParticleKind.SNOW) && particle.y >= -1 && particle.vy > 0) {
			particle.age = particle.life;
		}
		// Embers, snow and fireflies sway
		if (particle.kind == ParticleKind.EMBER || particle.kind == ParticleKind.SNOW || particle.kind == ParticleKind.FIREFLY) {
			particle.x += Math.sin(particle.age * (particle.kind == ParticleKind.EMBER ? 3 : particle.kind == ParticleKind.FIREFLY ? 1.3 : 1.7) + particle.shade * 6) * (particle.kind == ParticleKind.FIREFLY ? 14 : 9) * deltaTime;
			if (particle.kind == ParticleKind.FIREFLY) {
				particle.z += Math.cos(particle.age * 1.1 + particle.shade * 9) * 12 * deltaTime;
			}
		}
		particle.z += particle.vz * deltaTime;
		particle.spin.setAngle(particle.spin.getAngle() + particle.spinSpeed * deltaTime);
	}

	/** How a particle looks at its age, as its size and color */
	private Look lookOf(Particle particle, double time) {
		double width;
		double height;
		Color color;
		switch (particle.kind) {
		case FLAME -> {
			// Grows fast, then thins out to a tip
			double grow = Math.min(1, time * 5);
			width = particle.size * grow * (1 - 0.5 * time);
			height = width * 1.7;
			double flicker = 0.85 + 0.15 * host.random.nextDouble();
			color = flameColor(particle.shade < 0.35, step(time), level(Math.min(1, (1 - time) * 1.6) * flicker));
		}
		case SMOKE -> {
			// Born in the fire, grows and fades
			width = particle.size * (0.6 + 2.6 * Math.sqrt(time));
			height = width;
			color = smokeColor(Math.max(0, Math.min(SMOKE_SHADES - 1, (int) (particle.shade / SMOKE_SHADE_MAX * SMOKE_SHADES))), step(time), level(Math.min(1, time * 6) * Math.pow(1 - time, 1.4) * 0.7));
		}
		case EMBER -> {
			double flicker = 0.7 + 0.3 * host.random.nextDouble();
			width = particle.size * (1 - 0.3 * time) * flicker;
			height = width;
			color = emberColor(step(time));
		}
		case FIREFLY -> {
			width = particle.size;
			height = width;
			// Fades in and out and blinks
			double blink = 0.5 + 0.5 * Math.sin(particle.age * 4.2 + particle.shade * 20);
			double envelope = Math.min(1, Math.min(time * 6, (1 - time) * 6));
			color = fireflyColor(level(Math.max(0, envelope * (0.35 + 0.65 * blink))));
		}
		case SNOW -> {
			width = particle.size;
			height = width;
			color = snowColor(level(Math.min(0.9, particle.age * 1.5)));
		}
		default -> {
			width = particle.size * (1 - 0.5 * time);
			height = width;
			color = sparkColor(step(time));
		}
		}
		return new Look(width, height, color);
	}

	/** Places the particle in the world, its material and its orientation towards the camera; keeps its distance to the eye */
	private void placeParticle(Particle particle, Look look, double yaw, double pitch, double eyeX, double eyeY, double eyeZ) {
		double width = look.width();
		double height = look.height();
		Color color = look.color();
		// The colors come from tables, the same object again if it did not change
		if (particle.shown != color) {
			particle.material.setDiffuseColor(color);
			particle.shown = color;
		}
		double posX = particle.x;
		double posZ = particle.z;
		if (particle.anchor != null) {
			posX += particle.anchor.root.getTranslateX();
			posZ += particle.anchor.root.getTranslateZ();
		}
		particle.view.setTranslateX(posX);
		particle.view.setTranslateY(particle.y);
		particle.view.setTranslateZ(posZ);
		particle.yaw.setAngle(yaw);
		particle.pitch.setAngle(pitch);
		particle.scale.setX(Math.max(0.01, width));
		particle.scale.setY(Math.max(0.01, height));
		// Flames are drawn from their base
		if (particle.kind == ParticleKind.FLAME) {
			particle.view.setTranslateY(particle.y - height * 0.3);
		}
		double deltaX = posX - eyeX;
		double deltaY = particle.y - eyeY;
		double deltaZ = posZ - eyeZ;
		particle.distance = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
	}

	/** Removes every particle and returns them to the pool. */
	void clear() {
		this.particlePool.addAll(this.particles);
		this.particles.clear();
		host.fireLayer.getChildren().clear();
		for (RobotView view : host.robots.values()) {
			view.flameDebt = view.smokeDebt = view.sparkDebt = 0;
		}
	}
}
