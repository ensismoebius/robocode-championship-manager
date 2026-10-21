package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

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

	Critters(ArenaStyle style, double width, double height, Random random) {
		this.style = style;
		this.halfWidth = width / 2;
		this.halfHeight = height / 2;
		this.random = random;
		for (int i = 0; i < COUNT; i++) {
			Critter critter = new Critter(kind(style, i));
			this.critters.add(critter);
			this.root.getChildren().add(critter.root);
		}
	}

	Group root() {
		return this.root;
	}

	/** Where the water is: penguins in it swim instead of walking */
	void setWater(BiPredicate<Double, Double> water) {
		this.water = water;
	}

	void update(double dt) {
		for (Critter critter : this.critters) {
			critter.update(dt);
		}
	}

	/**
	 * Something happens in the battle at (x, z): the critters within the radius are
	 * frightened, the nearer the more, and run or fly away from it
	 */
	void alarm(double x, double z, double radius, double strength) {
		for (Critter critter : this.critters) {
			double distance = Math.hypot(critter.x - x, critter.z - z);
			if (distance < radius) {
				critter.frighten(x, z, strength * (1 - distance / radius));
			}
		}
	}

	/** Where the critter nearest to the point is, aimed at its body, or null if there is none */
	double[] nearest(double x, double z) {
		Critter best = this.closest(x, z);
		return best == null ? null : this.body(best);
	}

	/**
	 * Follows the critter nearest to the point: every call tells where it is now,
	 * aimed at its body. Null if there are no critters.
	 */
	Supplier<double[]> track(double x, double z) {
		Critter best = this.closest(x, z);
		return best == null ? null : () -> this.body(best);
	}

	private Critter closest(double x, double z) {
		Critter best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Critter critter : this.critters) {
			double distance = Math.hypot(critter.x - x, critter.z - z);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = critter;
			}
		}
		return best;
	}

	private double[] body(Critter critter) {
		return new double[] { critter.x, critter.y - 12 * SCALE, critter.z };
	}

	/** The kinds of the arenas: a mix of walkers and flyers where there are flyers */
	private enum Kind {
		MONKEY(false), BIRD(true), PENGUIN(false), BEETLE(false), SALAMANDER(false), BAT(true);

		final boolean flies;

		Kind(boolean flies) {
			this.flies = flies;
		}
	}

	private static Kind kind(ArenaStyle style, int index) {
		return switch (style) {
		case JUNGLE -> index % 3 == 2 ? Kind.BIRD : Kind.MONKEY;
		case ICE -> Kind.PENGUIN;
		case NEON -> Kind.BEETLE;
		case VOLCANO -> index % 3 == 2 ? Kind.BAT : Kind.SALAMANDER;
		};
	}

	/** Whether the point is in the battle area, with its margin */
	private boolean inField(double x, double z) {
		return Math.abs(x) < this.halfWidth + MARGIN && Math.abs(z) < this.halfHeight + MARGIN;
	}

	/** Whether a critter can be at the point: near the field, not in the battle, not in the river for walkers */
	private boolean walkable(double x, double z, boolean flies) {
		if (this.inField(x, z)) {
			return false;
		}
		boolean near = Math.abs(x) <= this.halfWidth + REACH && Math.abs(z) <= this.halfHeight + REACH;
		boolean wet = this.water != null && this.water.test(x, z);
		if (!near && !(wet && Math.abs(x) <= this.halfWidth + SWIM_REACH && Math.abs(z) <= this.halfHeight + SWIM_REACH)) {
			return false;
		}
		return flies || this.style != ArenaStyle.JUNGLE || Math.abs(Math.abs(z) - (this.halfHeight + 160)) >= 90;
	}

	/** Whether the straight way from one point to another never enters the battle area */
	private boolean clearPath(double fromX, double fromZ, double toX, double toZ) {
		// Dense enough that not even a corner of the field can be skipped between two points
		int samples = (int) Math.ceil(Math.hypot(toX - fromX, toZ - fromZ) / 4) + 1;
		for (int i = 1; i <= samples; i++) {
			double t = (double) i / samples;
			// Checked a little wider than the margin, so a corner grazed between two samples is still caught
			double x = fromX + (toX - fromX) * t;
			double z = fromZ + (toZ - fromZ) * t;
			if (Math.abs(x) < this.halfWidth + MARGIN + 8 && Math.abs(z) < this.halfHeight + MARGIN + 8) {
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
			double x = (this.random.nextDouble() * 2 - 1) * (this.halfWidth + reach);
			double z = (this.random.nextDouble() * 2 - 1) * (this.halfHeight + reach);
			if (this.walkable(x, z, flies) && (!wantWater || this.water.test(x, z))) {
				return new double[] { x, z };
			}
		}
		// No water around: any place will do
		while (true) {
			double x = (this.random.nextDouble() * 2 - 1) * (this.halfWidth + REACH);
			double z = (this.random.nextDouble() * 2 - 1) * (this.halfHeight + REACH);
			if (this.walkable(x, z, flies)) {
				return new double[] { x, z };
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
			double x = fromX + Math.cos(direction) * distance;
			double z = fromZ + Math.sin(direction) * distance;
			if (this.walkable(x, z, flies) && this.clearPath(fromX, fromZ, x, z)) {
				return new double[] { x, z };
			}
		}
		return this.wander(flies, fromX, fromZ);
	}

	/** A part of a creature that turns around its hip: the legs or the wings */
	private record Joint(Rotate rotate, double amplitude, double phase) {
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
		void frighten(double x, double z, double amount) {
			this.threatX = x;
			this.threatZ = z;
			this.fear = Math.min(1, Math.max(this.fear, amount * 1.5));
			double[] goal = escape(this.kind.flies, this.x, this.z, x, z);
			this.targetX = goal[0];
			this.targetZ = goal[1];
		}

		void update(double dt) {
			this.time += dt;
			this.fear = Math.max(0, this.fear - dt * 0.3);
			double dx = this.targetX - this.x;
			double dz = this.targetZ - this.z;
			if (Math.hypot(dx, dz) < 14) {
				double[] goal = this.fear > 0.05 ? escape(this.kind.flies, this.x, this.z, this.threatX, this.threatZ) : wander(this.kind.flies, this.x, this.z);
				this.targetX = goal[0];
				this.targetZ = goal[1];
				dx = this.targetX - this.x;
				dz = this.targetZ - this.z;
			}
			double desired = Math.atan2(dx, dz);
			double turnBy = Math.atan2(Math.sin(desired - this.heading), Math.cos(desired - this.heading));
			this.heading += turnBy * Math.min(1, dt * (2.5 + 4 * this.fear));

			// Walks straight to the goal, the way that was checked to stay out of the battle area;
			// the body turns towards it on its own pace
			boolean inWater = this.kind == Kind.PENGUIN && Critters.this.water != null && Critters.this.water.test(this.x, this.z);
			this.swim += ((inWater ? 1 : 0) - this.swim) * Math.min(1, dt * 4);
			double speed = (this.kind.flies ? 90 : 34 + 30 * this.swim) * (1 + 1.6 * this.fear);
			double distance = Math.hypot(dx, dz);
			double step = Math.min(distance, speed * dt);
			if (distance > 0) {
				this.x += dx / distance * step;
				this.z += dz / distance * step;
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

	/** The body of a creature, its joints added to the list to be animated */
	private static Group creature(Kind kind, List<Joint> joints) {
		Group body = new Group();
		switch (kind) {
		case MONKEY -> monkey(body, joints);
		case PENGUIN -> penguin(body, joints);
		case BEETLE -> beetle(body, joints);
		case SALAMANDER -> salamander(body, joints);
		case BIRD -> bird(body, joints);
		case BAT -> bat(body, joints);
		}
		return body;
	}

	/** The monkey: its shape, and the joints that move it */
	private static void monkey(Group body, List<Joint> joints) {
		PhongMaterial fur = paint(Color.web("#7a4a22"), 12);
		PhongMaterial belly = paint(Color.web("#b8834f"), 12);
		PhongMaterial face = paint(Color.web("#d9b38c"), 12);
		PhongMaterial dark = paint(Color.web("#140c07"), 30);
		body.getChildren().addAll(part(new Sphere(9, 12), fur, 0, -14, 0, 1, 1.15, 1), part(new Sphere(5.5, 10), belly, 0, -12, 4, 0.9, 1.1, 0.6),
				part(new Sphere(6.5, 12), fur, 0, -29, 3, 1, 1, 1), part(new Sphere(2.2, 8), face, -6, -30, 2, 1, 1, 0.6),
				part(new Sphere(2.2, 8), face, 6, -30, 2, 1, 1, 0.6), part(new Sphere(3.8, 10), face, 0, -26.5, 8, 1, 0.9, 0.7),
				part(new Sphere(0.9, 6), dark, 0, -25, 11.3, 1.4, 0.8, 1));
		body.getChildren().addAll(eyes(2, -28.5, 9.6, 1.1));
		// The tail leaves the back and curves like an S, wagging a little when it walks
		body.getChildren().add(joint(joints, Rotate.Y_AXIS, 0, -12, -7, curledTail(fur), 0.3, 0.8));
		for (int side = -1; side <= 1; side += 2) {
			Group arm = new Group(box(3.2, 10, 3.2, fur, 0, 5, 0), part(new Sphere(2, 8), face, 0, 11, 0.5, 1, 1, 1));
			body.getChildren().add(joint(joints, Rotate.X_AXIS, side * 7.5, -23, 0, arm, 0.4, side > 0 ? Math.PI : 0));
			body.getChildren().add(joint(joints, Rotate.X_AXIS, side * 4.5, -11, 0, box(3.5, 11, 3.5, fur, 0, 5.5, 0), 0.5, side > 0 ? 0 : Math.PI));
		}
	
	}

	/** The penguin: its shape, and the joints that move it */
	private static void penguin(Group body, List<Joint> joints) {
		PhongMaterial black = paint(Color.web("#151a22"), 40);
		PhongMaterial belly = paint(Color.web("#f4f7fa"), 20);
		PhongMaterial beak = paint(Color.web("#f0a020"), 30);
		body.getChildren().addAll(part(new Sphere(8, 12), black, 0, -13, 0, 1, 1.25, 0.9), part(new Sphere(6, 10), belly, 0, -12, 4.5, 0.9, 1.2, 0.6),
				beak(beak, 0, -17, 8.5));
		body.getChildren().addAll(eyes(2.8, -20, 7, 1.6));
		for (int side = -1; side <= 1; side += 2) {
			body.getChildren().add(joint(joints, Rotate.Z_AXIS, side * 7, -13, 0, box(2, 10, 5, black, 0, 5, 0), 0.25 * side, 0));
			body.getChildren().add(joint(joints, Rotate.X_AXIS, side * 3, 0, 1, box(4, 1, 6, beak, 0, 0, 2.5), 0.45, side > 0 ? 0 : Math.PI));
		}
	
	}

	/** The beetle: its shape, and the joints that move it */
	private static void beetle(Group body, List<Joint> joints) {
		PhongMaterial shell = paint(Color.web("#2a1b3d"), 60);
		PhongMaterial seam = paint(Color.web("#3b2a52"), 40);
		PhongMaterial glow = paint(Color.web("#3ff2ff"), 60);
		PhongMaterial pink = paint(Color.web("#ff3fa4"), 60);
		body.getChildren().addAll(part(new Box(12, 5, 15), shell, 0, -4, 0, 1, 1, 1), part(new Box(6, 3.5, 4), shell, 0, -4, 9.5, 1, 1, 1),
				part(new Box(3, 1, 15), glow, 0, -7, 0, 1, 1, 1), part(new Box(0.5, 0.6, 14), seam, -2.6, -6.6, 0, 1, 1, 1),
				part(new Box(0.5, 0.6, 14), seam, 2.6, -6.6, 0, 1, 1, 1));
		for (int side = -1; side <= 1; side += 2) {
			for (double z = -4; z <= 3; z += 7) {
				body.getChildren().add(part(new Sphere(0.9, 6), glow, side * 3.2, -6.6, z, 1, 0.5, 1));
			}
			body.getChildren().add(cylinder(0.3, 9, shell, side * 2.2, -11, 12, -side * 20));
			body.getChildren().add(part(new Sphere(0.9, 6), glow, side * 4.2, -15.5, 12.5, 1, 1, 1));
		}
		body.getChildren().addAll(eyes(2.2, -4.5, 12, 1));
		for (int side = -1; side <= 1; side += 2) {
			for (int row = -1; row <= 1; row++) {
				body.getChildren().add(joint(joints, Rotate.Y_AXIS, side * 5.5, -3, row * 5, box(7, 1.2, 1.2, shell, side * 3.5, 0, 0), 0.4, row * 1.7 + (side > 0 ? 0 : Math.PI)));
			}
		}
		body.getChildren().add(part(new Sphere(1.2, 6), pink, 0, -5.5, 14, 1, 1, 1));
	
	}

	/** The salamander: its shape, and the joints that move it */
	private static void salamander(Group body, List<Joint> joints) {
		PhongMaterial skin = paint(Color.web("#5a1d12"), 25);
		PhongMaterial belly = paint(Color.web("#ff7a1a"), 15);
		PhongMaterial spot = paint(Color.web("#ffb020"), 30);
		PhongMaterial gill = paint(Color.web("#e0301a"), 20);
		body.getChildren().addAll(part(new Box(7, 4, 17), skin, 0, -3, 0, 1, 1, 1), part(new Box(5, 1, 15), belly, 0, -1, 0, 1, 1, 1),
				part(new Sphere(4, 10), skin, 0, -4, 10, 1, 0.8, 1.1), tail(skin, 0, -3, -15));
		for (int side = -1; side <= 1; side += 2) {
			body.getChildren().add(part(new Box(0.4, 3, 2.5), gill, side * 4.3, -5, 8, 1, 1, 1));
			for (double z = -6; z <= 6; z += 4) {
				body.getChildren().add(part(new Sphere(0.9, 6), spot, side * 1.6, -5.4, z, 1, 0.5, 1));
			}
		}
		body.getChildren().addAll(eyes(2, -6, 13, 0.9));
		for (int side = -1; side <= 1; side += 2) {
			for (int row = -1; row <= 1; row += 2) {
				body.getChildren().add(joint(joints, Rotate.Y_AXIS, side * 4.5, -2, row * 5, box(6, 1, 2, skin, side * 3, 0, 0), 0.35, row + (side > 0 ? 0 : Math.PI)));
			}
		}
	
	}

	/** The bird: its shape, and the joints that move it */
	private static void bird(Group body, List<Joint> joints) {
		PhongMaterial red = paint(Color.web("#e0452b"), 20);
		PhongMaterial yellow = paint(Color.web("#f2c230"), 20);
		PhongMaterial blue = paint(Color.web("#2a7fd1"), 20);
		PhongMaterial tip = paint(Color.web("#7fc4ff"), 20);
		PhongMaterial beak = paint(Color.web("#f0a020"), 20);
		body.getChildren().addAll(part(new Sphere(5, 10), red, 0, 0, 0, 0.9, 0.9, 1.4), part(new Sphere(3.2, 8), yellow, 0, -2, 5.5, 1, 1, 1),
				part(new Box(0.8, 3, 2), yellow, 0, -6.5, 4.5, 1, 1, 1), part(new Box(0.8, 2.5, 2), yellow, 0, -6, 6.5, 1, 1, 1), beak(beak, 0, -1.5, 9));
		body.getChildren().addAll(eyes(1.8, -2.6, 7, 1.1));
		for (int side = -1; side <= 1; side += 2) {
			body.getChildren().add(part(rotated(new Box(2.5, 0.3, 6), side * 12, Rotate.Y_AXIS), red, side * 1.5, 0, -9.5, 1, 1, 1));
			body.getChildren().add(cylinder(0.5, 5, beak, side * 1.5, 6.5, 0.5, 0));
			body.getChildren().add(part(new Box(3, 0.4, 2), beak, side * 1.5, 9, 1.5, 1, 1, 1));
			Group wing = new Group(box(18, 0.6, 6, blue, side * 9, 0, 0), part(new Box(4, 0.6, 6), tip, side * 17, 0, 0, 1, 1, 1));
			body.getChildren().add(joint(joints, Rotate.Z_AXIS, side * 3, -1, 0, wing, 0.8 * side, 0));
		}
	
	}

	/** The bat: its shape, and the joints that move it */
	private static void bat(Group body, List<Joint> joints) {
		PhongMaterial fur = paint(Color.web("#1d1424"), 10);
		PhongMaterial membrane = paint(Color.web("#2b1d33"), 10);
		PhongMaterial rib = paint(Color.web("#4a3a5c"), 10);
		PhongMaterial red = paint(Color.web("#ff3030"), 40);
		body.getChildren().addAll(part(new Sphere(4, 10), fur, 0, 0, 0, 1, 0.9, 1.3), part(new Sphere(2.6, 8), fur, 0, -1, 4.5, 1, 1, 1));
		for (int side = -1; side <= 1; side += 2) {
			body.getChildren().add(cylinder(0.7, 3.5, fur, side * 1.6, -4.5, 4.5, 0));
			body.getChildren().add(part(new Sphere(0.7, 6), red, side * 1.1, -1.2, 6.6, 1, 1, 1));
			Group wing = new Group(box(16, 0.4, 8, membrane, side * 8, 0, 0));
			for (double x = 4; x <= 13; x += 4.5) {
				wing.getChildren().add(box(0.4, 0.6, 8.5, rib, side * x, -0.2, 0));
			}
			body.getChildren().add(joint(joints, Rotate.Z_AXIS, side * 2, 0, 0, wing, 0.9 * side, 0));
		}
	
	}

	/** Two eyes at x = ±x: a white with a black pupil in front of it */
	private static List<Node> eyes(double x, double y, double z, double size) {
		PhongMaterial white = paint(Color.web("#f4f4ee"), 60);
		PhongMaterial pupil = paint(Color.web("#0a0a0a"), 60);
		List<Node> parts = new ArrayList<>();
		for (int side = -1; side <= 1; side += 2) {
			parts.add(part(new Sphere(size, 8), white, side * x, y, z, 1, 1, 1));
			parts.add(part(new Sphere(size * 0.55, 6), pupil, side * x, y, z + size * 0.6, 1, 1, 1));
		}
		return parts;
	}

	/** A cylinder standing on its middle, tilted sideways by degrees around Z */
	private static Node cylinder(double radius, double height, PhongMaterial material, double x, double y, double z, double tilt) {
		Cylinder cylinder = new Cylinder(radius, height, 6);
		cylinder.setMaterial(material);
		if (tilt != 0) {
			cylinder.getTransforms().add(new Rotate(tilt, Rotate.Z_AXIS));
		}
		cylinder.setTranslateX(x);
		cylinder.setTranslateY(y);
		cylinder.setTranslateZ(z);
		return cylinder;
	}

	/** A box turned around an axis, before it is placed */
	private static Box rotated(Box box, double angle, Point3D axis) {
		box.getTransforms().add(new Rotate(angle, axis));
		return box;
	}

	private static PhongMaterial paint(Color color, double shine) {
		PhongMaterial material = new PhongMaterial(color);
		material.setSpecularColor(Color.gray(0.3));
		material.setSpecularPower(shine);
		return material;
	}

	private static <T extends Shape3D> T part(T shape, PhongMaterial material, double x, double y, double z, double sx, double sy, double sz) {
		shape.setMaterial(material);
		shape.setTranslateX(x);
		shape.setTranslateY(y);
		shape.setTranslateZ(z);
		shape.setScaleX(sx);
		shape.setScaleY(sy);
		shape.setScaleZ(sz);
		return shape;
	}

	private static Box box(double sx, double sy, double sz, PhongMaterial material, double x, double y, double z) {
		Box box = new Box(sx, sy, sz);
		return (Box) part(box, material, x, y, z, 1, 1, 1);
	}

	/** A joint: a hip at (x, y, z) that turns around the axis, with the child hanging from it */
	private static Node joint(List<Joint> joints, Point3D axis, double x, double y, double z, Node child, double amplitude, double phase) {
		Rotate rotate = new Rotate(0, axis);
		Group hip = new Group(child);
		hip.setTranslateX(x);
		hip.setTranslateY(y);
		hip.setTranslateZ(z);
		hip.getTransforms().add(rotate);
		joints.add(new Joint(rotate, amplitude, phase));
		return hip;
	}

	/**
	 * A tail that stands up curved like a loose S seen from the side: it rises from
	 * the rump, swings back, forward and back again. Made of cylinders, one between each two points of
	 * the curve, that get thinner towards the tip, around its root at the origin
	 * (the back of the animal is at -z, up is -y).
	 */
	private static Node curledTail(PhongMaterial material) {
		Group tail = new Group();
		int links = 16;
		Point3D from = null;
		for (int i = 0; i <= links; i++) {
			double t = (double) i / links;
			// Up along y, standing; it swings back, forward and back again along z, a
			// loose S: the swings are uneven and the tail wanders a little to the side
			Point3D to = new Point3D(2 * Math.sin(4.1 * t + 0.6), -20 * t, -3 * t - 3.6 * Math.sin(2 * Math.PI * Math.pow(t, 1.15)));
			if (from != null) {
				tail.getChildren().add(link(material, from, to, 1.9 - 0.7 * t));
			}
			from = to;
		}
		return tail;
	}

	/**
	 * A cylinder from a point to another, a little longer so the links of a chain
	 * overlap at the bends
	 */
	private static Node link(PhongMaterial material, Point3D from, Point3D to, double radius) {
		Point3D direction = to.subtract(from);
		Cylinder cylinder = new Cylinder(radius, direction.magnitude() * 1.15, 8);
		cylinder.setMaterial(material);
		Point3D middle = from.midpoint(to);
		// A cylinder stands along y: turned around the axis perpendicular to y and the direction
		cylinder.getTransforms().addAll(new Translate(middle.getX(), middle.getY(), middle.getZ()), new Rotate(Rotate.Y_AXIS.angle(direction), Rotate.Y_AXIS.crossProduct(direction)));
		return cylinder;
	}

	private static Node tail(PhongMaterial material, double x, double y, double z) {
		return pointing(1.6, 16, material, x, y, z);
	}

	private static Node beak(PhongMaterial material, double x, double y, double z) {
		return pointing(1.2, 4, material, x, y, z);
	}

	/** A cylinder lying along z, its center at x, y, z */
	private static Node pointing(double radius, double length, PhongMaterial material, double x, double y, double z) {
		Cylinder cylinder = new Cylinder(radius, length, 6);
		cylinder.setMaterial(material);
		cylinder.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
		cylinder.setTranslateX(x);
		cylinder.setTranslateY(y);
		cylinder.setTranslateZ(z);
		return cylinder;
	}
}
