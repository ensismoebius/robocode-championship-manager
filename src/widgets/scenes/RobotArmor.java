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
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.util.Duration;

/**
 * The armor of a robot: hexagonal plates, each one its own piece, that come off
 * as the robot loses energy. The armor never grows back during a round, even
 * when a hit gives energy back; it is whole again only for a new round or the
 * replay.
 */
final class RobotArmor {

	/** The energy of a robot with all of its armor */
	static final double FULL_ENERGY = 100;

	private final RobotView view;
	private final Random random;
	/** Every plate, in the order they stay on: the last ones come off first */
	final List<MeshView> plates = new ArrayList<MeshView>();
	/** The plates thrown off and still flying or lying on the floor of the arena */
	final List<Group> thrown = new ArrayList<Group>();
	/** The lowest energy since the armor was whole */
	private double lowest = FULL_ENERGY;
	/** The first energy after the armor is whole is set at once: the replay starts with the plates already gone */
	private boolean settled;

	/** Creates the armor of the robot; the seed sets the order the plates come off, the same in every replay. */
	RobotArmor(RobotView view, long seed) {
		this.view = view;
		this.random = new Random(seed);
	}

	/** Adds a hexagonal plate to a part of the robot, as its own piece, at a random place in the order of loss. */
	void plate(Group part, PhongMaterial material, double posZ, double posY, double radiusZ, double radiusY, double fromX, double toX) {
		LowPoly piece = new LowPoly();
		piece.hexPlate(posZ, posY, radiusZ, radiusY, fromX, toX);
		MeshView plate = piece.view(material);
		part.getChildren().add(plate);
		this.plates.add(this.random.nextInt(this.plates.size() + 1), plate);
	}

	/** How many plates the energy still holds: all of them at full energy, none when it is gone. */
	int platesFor(double energy) {
		double fraction = Math.max(0, Math.min(1, energy / FULL_ENERGY));
		return (int) Math.ceil(this.plates.size() * fraction);
	}

	/** How many plates are still on the robot. */
	int remaining() {
		return (int) this.plates.stream().filter(MeshView::isVisible).count();
	}

	/** Wears the armor down to the energy: the plates it no longer holds come off, thrown away during the battle. */
	void wear(double energy) {
		this.lowest = Math.min(this.lowest, energy);
		for (int index = this.platesFor(this.lowest); index < this.plates.size(); index++) {
			MeshView plate = this.plates.get(index);
			if (plate.isVisible()) {
				plate.setVisible(false);
				if (this.settled) {
					this.fling(plate);
				}
			}
		}
		this.settled = true;
	}

	/** The whole armor again, for a new round or the replay. */
	void restore() {
		this.lowest = FULL_ENERGY;
		this.settled = false;
		for (MeshView plate : this.plates) {
			plate.setVisible(true);
		}
		this.view.host.effects.getChildren().removeAll(this.thrown);
		this.thrown.clear();
	}

	/** A copy of the plate flies off the side of the robot, spinning, falls on the floor of the arena and fades away. */
	private void fling(MeshView plate) {
		Group arena = this.view.host.effects;
		Transform place;
		try {
			place = arena.getLocalToSceneTransform().createInverse().createConcatenation(plate.getLocalToSceneTransform());
		} catch (NonInvertibleTransformException exception) {
			return;
		}
		Bounds bounds = plate.getBoundsInLocal();
		Point3D center = place.transform(bounds.getCenterX(), bounds.getCenterY(), bounds.getCenterZ());
		// Away from the side of the robot the plate was on, along the floor
		Point3D side = place.deltaTransform(Math.signum(bounds.getCenterX()), 0, 0);
		Point3D out = new Point3D(side.getX(), 0, side.getZ());
		out = out.magnitude() < 1e-6 ? new Point3D(1, 0, 0) : out.normalize();
		double distance = 10 + this.random.nextDouble() * 12;

		MeshView copy = new MeshView(plate.getMesh());
		copy.setMaterial(plate.getMaterial());
		copy.getTransforms().add(place);
		Point3D axis = new Point3D(this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5);
		Rotate spin = new Rotate(0, center.getX(), center.getY(), center.getZ(), axis.magnitude() < 1e-6 ? Rotate.X_AXIS : axis);
		Group flying = new Group(copy);
		flying.getTransforms().add(spin);
		arena.getChildren().add(flying);
		this.thrown.add(flying);

		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(250), new KeyValue(flying.translateYProperty(), -9, Interpolator.EASE_OUT)),
				new KeyFrame(Duration.millis(700), new KeyValue(flying.translateXProperty(), out.getX() * distance), new KeyValue(flying.translateZProperty(), out.getZ() * distance),
						new KeyValue(flying.translateYProperty(), -center.getY(), Interpolator.EASE_IN), new KeyValue(spin.angleProperty(), (this.random.nextBoolean() ? 1 : -1) * 540)),
				new KeyFrame(Duration.millis(1700), new KeyValue(flying.scaleXProperty(), 1), new KeyValue(flying.scaleYProperty(), 1), new KeyValue(flying.scaleZProperty(), 1)),
				new KeyFrame(Duration.millis(2200), new KeyValue(flying.scaleXProperty(), 0), new KeyValue(flying.scaleYProperty(), 0), new KeyValue(flying.scaleZProperty(), 0)));
		this.view.host.fx.effect(timeline).setOnFinished(event -> {
			arena.getChildren().remove(flying);
			this.thrown.remove(flying);
			this.view.running.remove(timeline);
		});
		this.view.running.add(timeline);
		timeline.play();
	}
}
