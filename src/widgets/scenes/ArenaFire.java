package widgets.scenes;

import static widgets.scenes.FireTextures.SPRITE;
import static widgets.scenes.FireTextures.FLAME_TEXTURE;
import static widgets.scenes.FireTextures.SMOKE_TEXTURE;
import static widgets.scenes.FireTextures.SOFT_TEXTURE;
import static widgets.scenes.FireColors.SMOKE_SHADES;
import static widgets.scenes.FireColors.SMOKE_SHADE_MAX;
import static widgets.scenes.FireColors.step;
import static widgets.scenes.FireColors.level;
import static widgets.scenes.FireColors.flameColor;
import static widgets.scenes.FireColors.smokeColor;
import static widgets.scenes.FireColors.emberColor;
import static widgets.scenes.FireColors.sparkColor;
import static widgets.scenes.FireColors.fireflyColor;
import static widgets.scenes.FireColors.snowColor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
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

	/** Farthest first */
	private static final Comparator<Particle> FARTHEST_FIRST = (first, second) -> Double.compare(second.distance, first.distance);
	/** Reused every frame, so it does not make garbage */
	private final List<Node> fireViews = new ArrayList<Node>();

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
		case FLAME -> this.startFlame(particle, damage);
		case SMOKE -> this.startSmoke(particle, damage);
		case EMBER -> this.startEmber(particle);
		case FIREFLY -> this.startFirefly(particle);
		case SNOW -> this.startSnow(particle);
		case SPARK -> this.startSpark(particle);
		}
	}

	/** A flame: a tongue of fire that goes up, and some of them burn blue all the way */
	private void startFlame(Particle particle, double damage) {
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

	/** A puff of smoke, thicker and blacker when the robot is almost destroyed */
	private void startSmoke(Particle particle, double damage) {
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

	/** A glowing ember, drifting up from the fire */
	private void startEmber(Particle particle) {
		particle.life = 2.5 + host.random.nextDouble() * 2;
		particle.size = 1.3 + host.random.nextDouble() * 1.2;
		particle.vx = (host.random.nextDouble() - 0.5) * 12;
		particle.vz = (host.random.nextDouble() - 0.5) * 12;
		particle.vy = -(18 + host.random.nextDouble() * 24);
		particle.spinSpeed = 0;
		particle.shade = host.random.nextDouble();
		particle.material.setDiffuseMap(SOFT_TEXTURE);
	}

	/** A firefly, floating about slowly */
	private void startFirefly(Particle particle) {
		particle.life = 5 + host.random.nextDouble() * 4;
		particle.size = 2.6 + host.random.nextDouble() * 1.6;
		particle.vx = (host.random.nextDouble() - 0.5) * 14;
		particle.vz = (host.random.nextDouble() - 0.5) * 14;
		particle.vy = (host.random.nextDouble() - 0.5) * 8;
		particle.spinSpeed = 0;
		particle.shade = host.random.nextDouble();
		particle.material.setDiffuseMap(SOFT_TEXTURE);
	}

	/** A flake of snow, falling and drifting with the wind */
	private void startSnow(Particle particle) {
		particle.life = 12;
		particle.size = 2.6 + host.random.nextDouble() * 2.2;
		particle.vx = 5 + host.random.nextDouble() * 5;
		particle.vz = (host.random.nextDouble() - 0.5) * 6;
		particle.vy = 36 + host.random.nextDouble() * 26;
		particle.spinSpeed = 0;
		particle.shade = host.random.nextDouble();
		particle.material.setDiffuseMap(SOFT_TEXTURE);
	}

	/** A spark, thrown out in a ring from the hit */
	private void startSpark(Particle particle) {
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
	double rate = host.style == ArenaStyle.VOLCANO ? 26 : host.style == ArenaStyle.ICE ? 95 : host.style == ArenaStyle.JUNGLE ? 16 : host.style == ArenaStyle.EDEN_SOLAR ? 22 : 0;
	this.ambientDebt += deltaTime * rate;
	while (this.ambientDebt >= 1) {
		this.ambientDebt--;
		if (this.particles.size() > 550) {
			continue;
		}
		if (host.style == ArenaStyle.JUNGLE || host.style == ArenaStyle.EDEN_SOLAR) {
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
		return switch (particle.kind) {
		case FLAME -> this.flameLook(particle, time);
		case SMOKE -> this.smokeLook(particle, time);
		case EMBER -> this.emberLook(particle, time);
		case FIREFLY -> this.fireflyLook(particle, time);
		case SNOW -> this.snowLook(particle);
		default -> this.sparkLook(particle, time);
		};
	}

	/** A flame at its age: it grows fast, thins out to a tip and flickers */
	private Look flameLook(Particle particle, double time) {
		// Grows fast, then thins out to a tip
		double grow = Math.min(1, time * 5);
		double width = particle.size * grow * (1 - 0.5 * time);
		double flicker = 0.85 + 0.15 * host.random.nextDouble();
		Color color = flameColor(particle.shade < 0.35, step(time), level(Math.min(1, (1 - time) * 1.6) * flicker));
		return new Look(width, width * 1.7, color);
	}

	/** A puff of smoke at its age: born in the fire, it grows and fades */
	private Look smokeLook(Particle particle, double time) {
		double width = particle.size * (0.6 + 2.6 * Math.sqrt(time));
		Color color = smokeColor(Math.max(0, Math.min(SMOKE_SHADES - 1, (int) (particle.shade / SMOKE_SHADE_MAX * SMOKE_SHADES))), step(time), level(Math.min(1, time * 6) * Math.pow(1 - time, 1.4) * 0.7));
		return new Look(width, width, color);
	}

	/** An ember at its age: it shrinks a little and flickers */
	private Look emberLook(Particle particle, double time) {
		double flicker = 0.7 + 0.3 * host.random.nextDouble();
		double width = particle.size * (1 - 0.3 * time) * flicker;
		return new Look(width, width, emberColor(step(time)));
	}

	/** A firefly at its age: it fades in and out and blinks */
	private Look fireflyLook(Particle particle, double time) {
		// Fades in and out and blinks
		double blink = 0.5 + 0.5 * Math.sin(particle.age * 4.2 + particle.shade * 20);
		double envelope = Math.min(1, Math.min(time * 6, (1 - time) * 6));
		return new Look(particle.size, particle.size, fireflyColor(level(Math.max(0, envelope * (0.35 + 0.65 * blink)))));
	}

	/** A flake of snow, which whitens as it falls */
	private Look snowLook(Particle particle) {
		return new Look(particle.size, particle.size, snowColor(level(Math.min(0.9, particle.age * 1.5))));
	}

	/** A spark at its age: it shrinks as it goes out */
	private Look sparkLook(Particle particle, double time) {
		double width = particle.size * (1 - 0.5 * time);
		return new Look(width, width, sparkColor(step(time)));
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
