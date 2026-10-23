package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

import static widgets.scenes.CritterModels.creature;
import static widgets.scenes.CritterModels.joint;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The critters of each arena: small animals that live around the field, outside
 * the battle. Walkers swing their legs, flyers flap their wings. They never enter
 * the battle area, and when something happens in the battle (a shot, an explosion,
 * a robot destroyed) the ones nearby run or fly away from it.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class Critters {

	/** How big the critters are drawn: small next to the tanks */
	static final double SCALE = 0.3;
	/** Where the ground is: the feet of the walkers touch it */
	private static final double GROUND = 5;
	private static final int COUNT = 9;
	/** The battle area, and this much around it, is never entered */
	private static final double MARGIN = 60;
	/** How far from the field the critters roam */
	private static final double REACH = 320;
	/** The surface of the water, where swimmers float */
	private static final double WATER = 4.6;
	/** Swimmers can go this much farther than walkers, out into the water */
	private static final double SWIM_REACH = 500;

	private final Group root = new Group();
	private final List<Critter> critters = new ArrayList<>();
	private final double halfWidth;
	private final double halfHeight;
	private final ArenaStyle style;
	private final Random random;
	/** Where there is water that penguins swim in, or null */
	private BiPredicate<Double, Double> water;

	/** Creates the critters of an arena style, placed on the field of the given size. */
	Critters(ArenaStyle style, double width, double height, Random random) {
		this.style = style;
		this.halfWidth = width / 2;
		this.halfHeight = height / 2;
		this.random = random;
		for (int index = 0; index < COUNT; index++) {
			Critter critter = new Critter(kind(style, index));
			this.critters.add(critter);
			this.root.getChildren().add(critter.root);
		}
	}

	/** The group with every critter, to put in the arena. */
	Group root() {
		return this.root;
	}

	/** Where the water is: penguins in it swim instead of walking */
	void setWater(BiPredicate<Double, Double> water) {
		this.water = water;
	}

	/** Moves every critter by the time that passed. */
	void update(double deltaTime) {
		for (Critter critter : this.critters) {
			critter.update(deltaTime);
		}
	}

	/**
	 * Something happens in the battle at (x, z): the critters within the radius are
	 * frightened, the nearer the more, and run or fly away from it
	 */
	void alarm(double posX, double posZ, double radius, double strength) {
		for (Critter critter : this.critters) {
			double distance = Math.hypot(critter.x - posX, critter.z - posZ);
			if (distance < radius) {
				critter.frighten(posX, posZ, strength * (1 - distance / radius));
			}
		}
	}

	/** Where the critter nearest to the point is, aimed at its body, or null if there is none */
	double[] nearest(double posX, double posZ) {
		Critter best = this.closest(posX, posZ);
		return best == null ? null : this.body(best);
	}

	/**
	 * Follows the critter nearest to the point: every call tells where it is now,
	 * aimed at its body. Null if there are no critters.
	 */
	Supplier<double[]> track(double posX, double posZ) {
		Critter best = this.closest(posX, posZ);
		return best == null ? null : () -> this.body(best);
	}

	/** The critter nearest to the given position, or null when there are none. */
	private Critter closest(double posX, double posZ) {
		Critter best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Critter critter : this.critters) {
			double distance = Math.hypot(critter.x - posX, critter.z - posZ);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = critter;
			}
		}
		return best;
	}

	/** The position of the body of a critter, above its feet. */
	private double[] body(Critter critter) {
		return new double[] { critter.x, critter.y - 12 * SCALE, critter.z };
	}

	/** The kinds of the arenas: a mix of walkers and flyers where there are flyers */
	enum Kind {
		MONKEY(false), BIRD(true), PENGUIN(false), BEETLE(false), SALAMANDER(false), BAT(true);

		final boolean flies;

		/** The kind of a creature: whether it flies or walks. */
		Kind(boolean flies) {
			this.flies = flies;
		}
	}

	/** The kind of the creature at the given index of an arena style. */
	private static Kind kind(ArenaStyle style, int index) {
		return switch (style) {
		case JUNGLE -> index % 3 == 2 ? Kind.BIRD : Kind.MONKEY;
		case EDEN_SOLAR -> index % 2 == 0 ? Kind.BIRD : Kind.MONKEY;
		case ICE -> Kind.PENGUIN;
		case NEON -> Kind.BEETLE;
		case VOLCANO -> index % 3 == 2 ? Kind.BAT : Kind.SALAMANDER;
		};
	}

	/** Whether the point is in the battle area, with its margin */
	private boolean inField(double posX, double posZ) {
		return Math.abs(posX) < this.halfWidth + MARGIN && Math.abs(posZ) < this.halfHeight + MARGIN;
	}

	/** Whether a critter can be at the point: near the field, not in the battle, not in the river for walkers */
	private boolean walkable(double posX, double posZ, boolean flies) {
		if (this.inField(posX, posZ)) {
			return false;
		}
		boolean near = Math.abs(posX) <= this.halfWidth + REACH && Math.abs(posZ) <= this.halfHeight + REACH;
		boolean wet = this.water != null && this.water.test(posX, posZ);
		if (!near && !(wet && Math.abs(posX) <= this.halfWidth + SWIM_REACH && Math.abs(posZ) <= this.halfHeight + SWIM_REACH)) {
			return false;
		}
		return flies || this.style != ArenaStyle.JUNGLE || Math.abs(Math.abs(posZ) - (this.halfHeight + 160)) >= 90;
	}

	/** Whether the straight way from one point to another never enters the battle area */
	private boolean clearPath(double fromX, double fromZ, double toX, double toZ) {
		// Dense enough that not even a corner of the field can be skipped between two points
		int samples = (int) Math.ceil(Math.hypot(toX - fromX, toZ - fromZ) / 4) + 1;
		for (int index = 1; index <= samples; index++) {
			double time2 = (double) index / samples;
			// Checked a little wider than the margin, so a corner grazed between two samples is still caught
			double posX = fromX + (toX - fromX) * time2;
			double posZ = fromZ + (toZ - fromZ) * time2;
			if (Math.abs(posX) < this.halfWidth + MARGIN + 8 && Math.abs(posZ) < this.halfHeight + MARGIN + 8) {
				return false;
			}
		}
		return true;
	}

	/** A point where a critter can start: anywhere it can walk or fly around the field */
	private double[] spawnPoint(boolean flies) {
		// Where there is water, half of the places are in it, so the swimmers do swim
		boolean wantWater = this.water != null && this.random.nextBoolean();
		for (int attempt = 0; attempt < 1000; attempt++) {
			double reach = this.water == null ? REACH : SWIM_REACH;
			double posX = (this.random.nextDouble() * 2 - 1) * (this.halfWidth + reach);
			double posZ = (this.random.nextDouble() * 2 - 1) * (this.halfHeight + reach);
			if (this.walkable(posX, posZ, flies) && (!wantWater || this.water.test(posX, posZ))) {
				return new double[] { posX, posZ };
			}
		}
		// No water around: any place will do
		while (true) {
			double posX2 = (this.random.nextDouble() * 2 - 1) * (this.halfWidth + REACH);
			double posZ2 = (this.random.nextDouble() * 2 - 1) * (this.halfHeight + REACH);
			if (this.walkable(posX2, posZ2, flies)) {
				return new double[] { posX2, posZ2 };
			}
		}
	}

	/** A place to wander to from here, reached without entering the battle area; stays where it is if none is found */
	private double[] wander(boolean flies, double fromX, double fromZ) {
		for (int attempt = 0; attempt < 200; attempt++) {
			double[] goal = this.spawnPoint(flies);
			if (this.clearPath(fromX, fromZ, goal[0], goal[1])) {
				return goal;
			}
		}
		return new double[] { fromX, fromZ };
	}

	/** A place to run to, away from the threat, reached without entering the battle area */
	private double[] escape(boolean flies, double fromX, double fromZ, double threatX, double threatZ) {
		double away = Math.atan2(fromZ - threatZ, fromX - threatX);
		if (Math.hypot(fromX - threatX, fromZ - threatZ) < 1) {
			away = this.random.nextDouble() * Math.PI * 2;
		}
		for (int attempt = 0; attempt < 60; attempt++) {
			double direction = away + (this.random.nextDouble() - 0.5) * 1.4;
			double distance = 120 + this.random.nextDouble() * 160;
			double posX = fromX + Math.cos(direction) * distance;
			double posZ = fromZ + Math.sin(direction) * distance;
			if (this.walkable(posX, posZ, flies) && this.clearPath(fromX, fromZ, posX, posZ)) {
				return new double[] { posX, posZ };
			}
		}
		return this.wander(flies, fromX, fromZ);
	}

	/** A part of a creature that turns around its hip: the legs or the wings */
	record Joint(Rotate rotate, double amplitude, double phase) {
	}

	/** One critter: its body, where it is and where it goes */
	private final class Critter {
		private final Kind kind;
		private final Group root = new Group();
		private final Rotate turn = new Rotate(0, Rotate.Y_AXIS);
		/** Leans the body forward around its middle, lying on the water to swim */
		private final Rotate pose = new Rotate(0, 0, -12, 0, Rotate.X_AXIS);
		/** 0 on land, 1 swimming, in between while getting in or out of the water */
		private double swim;
		private final List<Joint> joints = new ArrayList<>();
		private final double altitude;
		private double x;
		private double y;
		private double z;
		private double heading;
		private double targetX;
		private double targetZ;
		/** 0 calm, 1 terrified: it runs faster, flies higher, its body jumps */
		private double fear;
		private double threatX;
		private double threatZ;
		private double time;

		/** Creates a critter of the given kind, with its model. */
		Critter(Kind kind) {
			this.kind = kind;
			this.root.getTransforms().add(this.turn);
			Group model = creature(kind, this.joints);
			model.getTransforms().addAll(new Scale(SCALE, SCALE, SCALE), this.pose);
			this.root.getChildren().add(model);
			this.altitude = -120 - Critters.this.random.nextDouble() * 70;
			double[] start = spawnPoint(kind.flies);
			this.x = start[0];
			this.z = start[1];
			this.heading = Critters.this.random.nextDouble() * Math.PI * 2;
			double[] goal = wander(kind.flies, this.x, this.z);
			this.targetX = goal[0];
			this.targetZ = goal[1];
		}

		/** Runs or flies away from a threat; the nearer it is, the more afraid */
		void frighten(double posX, double posZ, double amount) {
			this.threatX = posX;
			this.threatZ = posZ;
			this.fear = Math.min(1, Math.max(this.fear, amount * 1.5));
			double[] goal = escape(this.kind.flies, this.x, this.z, posX, posZ);
			this.targetX = goal[0];
			this.targetZ = goal[1];
		}

		/** Moves the critter towards its target and cools down its fear. */
		void update(double deltaTime) {
			this.time += deltaTime;
			this.fear = Math.max(0, this.fear - deltaTime * 0.3);
			double deltaX = this.targetX - this.x;
			double deltaZ = this.targetZ - this.z;
			if (Math.hypot(deltaX, deltaZ) < 14) {
				double[] goal = this.fear > 0.05 ? escape(this.kind.flies, this.x, this.z, this.threatX, this.threatZ) : wander(this.kind.flies, this.x, this.z);
				this.targetX = goal[0];
				this.targetZ = goal[1];
				deltaX = this.targetX - this.x;
				deltaZ = this.targetZ - this.z;
			}
			double desired = Math.atan2(deltaX, deltaZ);
			double turnBy = Math.atan2(Math.sin(desired - this.heading), Math.cos(desired - this.heading));
			this.heading += turnBy * Math.min(1, deltaTime * (2.5 + 4 * this.fear));

			// Walks straight to the goal, the way that was checked to stay out of the battle area;
			// the body turns towards it on its own pace
			boolean inWater = this.kind == Kind.PENGUIN && Critters.this.water != null && Critters.this.water.test(this.x, this.z);
			this.swim += ((inWater ? 1 : 0) - this.swim) * Math.min(1, deltaTime * 4);
			double speed = (this.kind.flies ? 90 : 34 + 30 * this.swim) * (1 + 1.6 * this.fear);
			double distance = Math.hypot(deltaX, deltaZ);
			double step = Math.min(distance, speed * deltaTime);
			if (distance > 0) {
				this.x += deltaX / distance * step;
				this.z += deltaZ / distance * step;
			}

			// The legs swing with the stride, the wings flap quicker when afraid
			double cycle = this.kind.flies ? this.time * 16 * (1 + this.fear) : this.time * speed * 0.22;
			for (Joint joint : this.joints) {
				// Swimming, the flippers stroke hard and the feet trail behind
				double stroke = joint.rotate().getAxis() == Rotate.Z_AXIS ? 1 + 3 * this.swim : 1 - 0.8 * this.swim;
				joint.rotate().setAngle(Math.toDegrees(stroke * joint.amplitude() * Math.sin(cycle + joint.phase())));
			}
			this.pose.setAngle(-62 * this.swim);
			if (this.kind.flies) {
				// Flyers climb away when afraid
				this.y = this.altitude - this.fear * 70 + Math.sin(this.time * 0.9) * 10;
			} else {
				// Each step lifts the body, a fearful one jumps; a swimmer floats half under the water
				double walking = GROUND - Math.abs(Math.sin(cycle)) * (1.2 + 5 * this.fear) * SCALE;
				double floating = WATER + (8 + Math.sin(this.time * 3) * 0.6) * SCALE;
				this.y = walking + (floating - walking) * this.swim;
			}
			this.turn.setAngle(Math.toDegrees(this.heading));
			this.root.setTranslateX(this.x);
			this.root.setTranslateY(this.y);
			this.root.setTranslateZ(this.z);
		}
	}

}
