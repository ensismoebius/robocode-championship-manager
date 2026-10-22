package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.util.Duration;
import widgets.scenes.ArenaFire.ParticleKind;

/**
 * The pieces a robot loses as it loses energy: the hexagonal plates of its
 * armor and the parts it can fight without (antennas, lamps and lights, the
 * visor, the vents, the sensors, the weapon station and the sight on the roof,
 * and the skirts over the tracks, once their plates are gone). A piece that
 * comes off is thrown away burning and smoking for half a second, keeps its
 * colors, and stays on the floor of the arena until the end of the round; the
 * big ones are pushed around by the robots that run into them. The pieces never
 * grow back during a round, even when a hit gives energy back; the robot is
 * whole again only for a new round or the replay.
 */
final class RobotArmor {

	/** The energy of a robot with all of its pieces */
	static final double FULL_ENERGY = 100;
	/** How long a thrown piece burns and smokes */
	static final double FIRE_SECONDS = 0.5;
	/** A piece this long, at least, is big enough to be pushed by the robots */
	static final double BIG_SIZE = 12;
	/** The robots, as the floor sees them: a circle around their center */
	static final double ROBOT_RADIUS = 22;

	/** A piece thrown off the robot: where it is, how long it still burns, and how it slides on the floor */
	static final class Thrown {
		final Group flying;
		final List<MeshView> shapes;
		/** Its center in the arena before the throw; the throw moves it by the translation of the flying group */
		final Point3D center;
		/** Its turn on the floor, around its center */
		final Rotate turn;
		/** Its long side on the floor before any turn, and half its length and width */
		final double axisX;
		final double axisZ;
		final double halfLength;
		final double halfWidth;
		final boolean big;
		boolean landed;
		double fire = FIRE_SECONDS;
		double flameDebt;
		double smokeDebt;
		double speedX;
		double speedZ;
		/** Radians a second */
		double spin;

		Thrown(Group flying, List<MeshView> shapes, Point3D center, Point3D axis, double halfLength, double halfWidth) {
			this.flying = flying;
			this.shapes = shapes;
			this.center = center;
			this.turn = new Rotate(0, center.getX(), center.getY(), center.getZ(), Rotate.Y_AXIS);
			this.axisX = axis.getX();
			this.axisZ = axis.getZ();
			this.halfLength = halfLength;
			this.halfWidth = halfWidth;
			this.big = 2 * Math.max(halfLength, halfWidth) >= BIG_SIZE;
		}

		/** Where the piece is now in the arena */
		double[] position() {
			return new double[] { this.center.getX() + this.flying.getTranslateX(), this.center.getY() + this.flying.getTranslateY(), this.center.getZ() + this.flying.getTranslateZ() };
		}

		/** Its long side on the floor now, turned with it: x and z */
		double[] axis() {
			double angle = Math.toRadians(this.turn.getAngle());
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			return new double[] { this.axisX * cos + this.axisZ * sin, -this.axisX * sin + this.axisZ * cos };
		}
	}

	private final RobotView view;
	private final Random random;
	/** Every piece, in the order they stay on: the last ones come off first */
	final List<Group> pieces = new ArrayList<Group>();
	/** The pieces thrown off this round, flying, burning or lying on the floor of the arena */
	final List<Thrown> thrown = new ArrayList<Thrown>();
	/** The lowest energy since the robot was whole */
	private double lowest = FULL_ENERGY;
	/** The first energy after the robot is whole is set at once: the replay starts with the pieces already gone */
	private boolean settled;

	/** Creates the pieces of the robot; the seed sets the order they come off, the same in every replay. */
	RobotArmor(RobotView view, long seed) {
		this.view = view;
		this.random = new Random(seed);
	}

	/** Adds a hexagonal plate to a part of the robot, as a piece of its own. */
	Group plate(Group part, PhongMaterial material, double posZ, double posY, double radiusZ, double radiusY, double fromX, double toX) {
		LowPoly piece = new LowPoly();
		piece.hexPlate(posZ, posY, radiusZ, radiusY, fromX, toX);
		return this.piece(part, piece.view(material));
	}

	/** Adds a piece made of the shapes to a part of the robot, at a random place in the order of loss. */
	Group piece(Group part, MeshView... shapes) {
		Group piece = new Group(shapes);
		part.getChildren().add(piece);
		this.pieces.add(this.random.nextInt(this.pieces.size() + 1), piece);
		return piece;
	}

	/**
	 * Adds a piece other pieces are fixed to: it comes off only after the last
	 * of them, so none is left in the air. Pieces added later do not change
	 * that, they may only go in between.
	 */
	Group holder(Group part, List<Group> held, MeshView... shapes) {
		Group piece = new Group(shapes);
		part.getChildren().add(piece);
		int first = held.stream().mapToInt(this.pieces::indexOf).min().orElse(this.pieces.size());
		this.pieces.add(first, piece);
		return piece;
	}

	/** How many pieces the energy still holds: all of them at full energy, none when it is gone. */
	int piecesFor(double energy) {
		double fraction = Math.max(0, Math.min(1, energy / FULL_ENERGY));
		return (int) Math.ceil(this.pieces.size() * fraction);
	}

	/** How many pieces are still on the robot. */
	int remaining() {
		return (int) this.pieces.stream().filter(Node::isVisible).count();
	}

	/** Wears the robot down to the energy: the pieces it no longer holds come off, thrown away during the battle. */
	void wear(double energy) {
		this.lowest = Math.min(this.lowest, energy);
		for (int index = this.piecesFor(this.lowest); index < this.pieces.size(); index++) {
			Group piece = this.pieces.get(index);
			if (piece.isVisible()) {
				piece.setVisible(false);
				if (this.settled) {
					this.fling(piece);
				}
			}
		}
		this.settled = true;
	}

	/** The whole robot again, for a new round or the replay: the pieces back on it, the ones on the floor gone. */
	void restore() {
		this.lowest = FULL_ENERGY;
		this.settled = false;
		for (Group piece : this.pieces) {
			piece.setVisible(true);
		}
		for (Thrown piece : this.thrown) {
			this.view.host.effects.getChildren().remove(piece.flying);
		}
		this.thrown.clear();
	}

	/**
	 * A frame of the thrown pieces: they burn and smoke for half a second, and
	 * the big ones on the floor are pushed by the robots that run into them.
	 *
	 * @param deltaTime
	 *            seconds, slower in the replay
	 */
	void update(double deltaTime) {
		for (Thrown piece : this.thrown) {
			if (piece.fire > 0) {
				this.burn(piece, deltaTime);
			}
			if (piece.big && piece.landed && deltaTime > 0) {
				this.collide(piece, deltaTime);
				this.slide(piece, deltaTime);
			}
		}
	}

	/** Flames and smoke from the piece, less and less, until its half second is over */
	private void burn(Thrown piece, double deltaTime) {
		double[] where = piece.position();
		double strength = Math.min(1, piece.fire / FIRE_SECONDS);
		piece.flameDebt += deltaTime * 60 * strength;
		while (piece.flameDebt >= 1) {
			piece.flameDebt--;
			this.view.host.fire.spawn(ParticleKind.FLAME, where[0] + this.jitter(2), where[1] - 1, where[2] + this.jitter(2), 0.25 + 0.5 * strength);
		}
		piece.smokeDebt += deltaTime * 24 * strength;
		while (piece.smokeDebt >= 1) {
			piece.smokeDebt--;
			this.view.host.fire.spawn(ParticleKind.SMOKE, where[0] + this.jitter(2), where[1] - 3, where[2] + this.jitter(2), 0.2 + 0.4 * strength);
		}
		piece.fire -= deltaTime;
	}

	/**
	 * Every robot that runs into the piece pushes it out of its way: the piece is
	 * moved out, takes the speed of the push and turns around the point it was
	 * hit, the more the farther from its middle.
	 */
	private void collide(Thrown piece, double deltaTime) {
		for (RobotView robot : this.view.host.robots.values()) {
			if (robot.dead || !robot.root.isVisible()) {
				continue;
			}
			double[] where = piece.position();
			double[] along = piece.axis();
			double[] across = { -along[1], along[0] };
			double offsetX = robot.root.getTranslateX() - where[0];
			double offsetZ = robot.root.getTranslateZ() - where[2];
			double onLength = Math.max(-piece.halfLength, Math.min(piece.halfLength, offsetX * along[0] + offsetZ * along[1]));
			double onWidth = Math.max(-piece.halfWidth, Math.min(piece.halfWidth, offsetX * across[0] + offsetZ * across[1]));
			// The point of the piece nearest to the robot, and how far the robot is from it
			double nearX = onLength * along[0] + onWidth * across[0];
			double nearZ = onLength * along[1] + onWidth * across[1];
			double gapX = offsetX - nearX;
			double gapZ = offsetZ - nearZ;
			double gap = Math.hypot(gapX, gapZ);
			if (gap >= ROBOT_RADIUS) {
				continue;
			}
			// The robot over the piece pushes it out across its width
			double sign = offsetX * across[0] + offsetZ * across[1] >= 0 ? 1 : -1;
			double normalX = gap > 1e-6 ? gapX / gap : across[0] * sign;
			double normalZ = gap > 1e-6 ? gapZ / gap : across[1] * sign;
			this.push(piece, -normalX * (ROBOT_RADIUS - gap), -normalZ * (ROBOT_RADIUS - gap), nearX, nearZ, deltaTime);
		}
	}

	/** Moves the piece by the push, gives it the speed of the push, and turns it around the point that was hit */
	private void push(Thrown piece, double pushX, double pushZ, double hitX, double hitZ, double deltaTime) {
		piece.flying.setTranslateX(piece.flying.getTranslateX() + pushX);
		piece.flying.setTranslateZ(piece.flying.getTranslateZ() + pushZ);
		piece.speedX = clamp(piece.speedX + pushX / deltaTime * 0.6, 220);
		piece.speedZ = clamp(piece.speedZ + pushZ / deltaTime * 0.6, 220);
		// A point at (x, z) turned by the angle moves along (z, -x)
		double lever = pushX * hitZ - pushZ * hitX;
		piece.spin = clamp(piece.spin + lever / (piece.halfLength * piece.halfLength + piece.halfWidth * piece.halfWidth) / deltaTime * 0.6, 6);
	}

	/** The piece slides and turns with its speed, slower and slower, without leaving the arena */
	private void slide(Thrown piece, double deltaTime) {
		piece.flying.setTranslateX(piece.flying.getTranslateX() + piece.speedX * deltaTime);
		piece.flying.setTranslateZ(piece.flying.getTranslateZ() + piece.speedZ * deltaTime);
		piece.turn.setAngle(piece.turn.getAngle() + Math.toDegrees(piece.spin * deltaTime));
		double friction = Math.exp(-4 * deltaTime);
		piece.speedX *= friction;
		piece.speedZ *= friction;
		piece.spin *= friction;
		double[] where = piece.position();
		double limitX = this.view.host.fieldWidth / 2;
		double limitZ = this.view.host.fieldHeight / 2;
		if (Math.abs(where[0]) > limitX) {
			piece.flying.setTranslateX(piece.flying.getTranslateX() - (where[0] - Math.signum(where[0]) * limitX));
			piece.speedX = 0;
		}
		if (Math.abs(where[2]) > limitZ) {
			piece.flying.setTranslateZ(piece.flying.getTranslateZ() - (where[2] - Math.signum(where[2]) * limitZ));
			piece.speedZ = 0;
		}
	}

	/** The value, no farther from zero than the limit */
	private static double clamp(double value, double limit) {
		return Math.max(-limit, Math.min(limit, value));
	}

	/** A random offset up to half the size, either way */
	private double jitter(double size) {
		return (this.random.nextDouble() - 0.5) * size;
	}

	/**
	 * The shapes of the piece, copied where the place puts them in the arena,
	 * each with a material of its own in the colors it has on the robot: the
	 * robot blinks white when hit, the pieces on the floor do not
	 */
	private List<MeshView> copyOf(Group piece, Transform place) {
		List<MeshView> shapes = new ArrayList<MeshView>();
		for (Node node : piece.getChildren()) {
			MeshView original = (MeshView) node;
			MeshView copy = new MeshView(original.getMesh());
			copy.setMaterial(this.materialOf((PhongMaterial) original.getMaterial()));
			copy.getTransforms().add(place);
			shapes.add(copy);
		}
		return shapes;
	}

	/** A copy of the material, in the color it has when the robot is not blinking */
	private PhongMaterial materialOf(PhongMaterial material) {
		PhongMaterial copy = new PhongMaterial(material.getDiffuseColor(), material.getDiffuseMap(), material.getSpecularMap(), material.getBumpMap(), material.getSelfIlluminationMap());
		copy.setSpecularColor(material.getSpecularColor());
		copy.setSpecularPower(material.getSpecularPower());
		if (material == this.view.bodyMaterial) {
			copy.setDiffuseColor(this.view.bodyColor);
		} else if (material == this.view.gunMaterial) {
			copy.setDiffuseColor(this.view.gunColor);
		}
		return copy;
	}

	/** A copy of the piece flies off the side of the robot, spinning, burning, and lands on the floor of the arena. */
	private void fling(Group piece) {
		Group arena = this.view.host.effects;
		Transform place;
		try {
			place = arena.getLocalToSceneTransform().createInverse().createConcatenation(piece.getLocalToSceneTransform());
		} catch (NonInvertibleTransformException exception) {
			return;
		}
		Bounds bounds = piece.getBoundsInLocal();
		Point3D center = place.transform(bounds.getCenterX(), bounds.getCenterY(), bounds.getCenterZ());
		// Away from the side of the robot the piece was on, along the floor
		Point3D side = place.deltaTransform(Math.signum(bounds.getCenterX()), 0, 0);
		Point3D out = new Point3D(side.getX(), 0, side.getZ());
		out = out.magnitude() < 1e-6 ? new Point3D(1, 0, 0) : out.normalize();
		double distance = 10 + this.random.nextDouble() * 12;

		// It tumbles away from the robot; a piece that stands (a plate of the skirt, an antenna) lands lying down
		Rotate spin = new Rotate(0, center.getX(), center.getY(), center.getZ(), new Point3D(-out.getZ(), 0, out.getX()));
		boolean standing = bounds.getWidth() < bounds.getHeight();
		double turn = standing ? 450 : 360;
		// It rests on the floor, not in it: its center half its thickness up
		double rest = -center.getY() - (standing ? bounds.getWidth() : bounds.getHeight()) / 2 - 0.2;
		// Its long side keeps the direction it had on the robot: the tumble turns around it
		Point3D length = place.deltaTransform(0, 0, 1);
		Point3D axis = new Point3D(length.getX(), 0, length.getZ());
		axis = axis.magnitude() < 1e-6 ? new Point3D(0, 0, 1) : axis.normalize();
		List<MeshView> shapes = this.copyOf(piece, place);
		Thrown thrown = new Thrown(new Group(shapes.toArray(new MeshView[0])), shapes, center, axis, bounds.getDepth() / 2, (standing ? bounds.getHeight() : bounds.getWidth()) / 2);
		thrown.flying.getTransforms().addAll(thrown.turn, spin);
		arena.getChildren().add(thrown.flying);
		this.thrown.add(thrown);
		this.toss(thrown, spin, out.multiply(distance), turn, rest);
	}

	/** Up, tumbling, then down onto the floor at the landing point, where it stays */
	private void toss(Thrown thrown, Rotate spin, Point3D landing, double turn, double rest) {
		Group flying = thrown.flying;
		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(250), new KeyValue(flying.translateYProperty(), -9, Interpolator.EASE_OUT)),
				new KeyFrame(Duration.millis(700), new KeyValue(flying.translateXProperty(), landing.getX()), new KeyValue(flying.translateZProperty(), landing.getZ()),
						new KeyValue(flying.translateYProperty(), rest, Interpolator.EASE_IN), new KeyValue(spin.angleProperty(), turn)));
		this.view.host.fx.effect(timeline).setOnFinished(event -> {
			thrown.landed = true;
			this.view.running.remove(timeline);
		});
		this.view.running.add(timeline);
		timeline.play();
	}
}
