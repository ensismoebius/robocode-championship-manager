package widgets.scenes;

import static widgets.scenes.BattleArena3D.FENCE_BASE;
import static widgets.scenes.ArenaMaterials.material;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The force field of the battle arena: invisible until a bullet or a robot hits
 * it, then the small hexagons around the place of the hit light up.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaFence {

	/** Rings of hexagons around the place of the hit: how many, and how wide each one is */
	private static final int RINGS = 8;
	private static final double RING_WIDTH = 18;
	/** Height of the shots over the floor, where the field is struck */
	private static final double AIM_HEIGHT = 17;
	/** More flashes than this at once and the oldest ones go away at once */
	private static final int MAX_FLASHES = 24;

	private final BattleArena3D host;
	private final List<Flash> flashes = new ArrayList<Flash>();

	/** Creates the fence of the arena, drawn in the host scene. */
	ArenaFence(BattleArena3D host) {
		this.host = host;
	}

	/**
	 * The field of the arena has nothing to show until it is hit: its hexagons are made where a shot hits it.
	 */
	void buildFence() {
		this.flashes.clear();
	}

	/**
	 * The force field lights up where a robot or a bullet hit it: the hexagons around the point, in rings that
	 * light one after the other, going outwards like a shock wave, each ring weaker than the one before
	 */
	void flashFence(double posX, double posY) {
		double width = host.fieldWidth;
		double height = host.fieldHeight;
		double cxValue = Math.max(0, Math.min(width, posX));
		double cyValue = Math.max(0, Math.min(height, posY));

		double[] distances = { cyValue, height - cyValue, cxValue, width - cxValue };
		int wall = 0;
		for (int index = 1; index < distances.length; index++) {
			if (distances[index] < distances[wall]) {
				wall = index;
			}
		}
		double span = wall <= 1 ? width : height;
		double along = (wall <= 1 ? cxValue : cyValue) - span / 2;

		Group plane = this.wallPlane(wall);
		host.fence.getChildren().add(plane);
		if (this.flashes.size() >= MAX_FLASHES) {
			this.flashes.remove(0).stop();
		}
		Flash flash = new Flash(plane);
		this.flashes.add(flash);

		Timeline timeline = new Timeline();
		for (int ring = 0; ring < RINGS; ring++) {
			PhongMaterial edge = material(FENCE_BASE);
			PhongMaterial cell = material(FENCE_BASE);
			plane.getChildren().add(HexPanel.band(span, along, AIM_HEIGHT, ring * RING_WIDTH, (ring + 1) * RING_WIDTH, edge, cell));
			this.light(timeline, edge, cell, 1.0 - ring * 0.11, Duration.millis(ring * 45));
		}
		timeline.setOnFinished(event -> flash.stop());
		flash.timeline = timeline;
		timeline.play();
	}

	/** The plane of a wall, where the hexagons of a hit on it are drawn: x runs along the wall, 0 at its middle */
	private Group wallPlane(int wall) {
		Group plane = new Group();
		double normal = wall <= 1 ? host.fieldHeight / 2 : host.fieldWidth / 2;
		if (wall <= 1) {
			plane.setTranslateZ(wall == 0 ? -normal : normal);
		} else {
			plane.setTranslateX(wall == 2 ? -normal : normal);
			plane.getTransforms().add(new Rotate(-90, Rotate.Y_AXIS));
		}
		return plane;
	}

	/** A ring of hexagons lights up after the delay and fades out: the edges clear, the cells barely there */
	private void light(Timeline timeline, PhongMaterial edge, PhongMaterial cell, double intensity, Duration delay) {
		Color tint = host.style.fence;
		double strength = 0.55 + 0.45 * Math.min(1.0, intensity);
		timeline.getKeyFrames().addAll(
				new KeyFrame(delay, new KeyValue(edge.diffuseColorProperty(), glow(tint, 1, 1, 1, 0.85 * strength)), new KeyValue(cell.diffuseColorProperty(), glow(tint, 1, 1, 1, 0.12 * strength))),
				new KeyFrame(delay.add(Duration.millis(90)), new KeyValue(edge.diffuseColorProperty(), glow(tint, 0.6, 0.8, 1, 0.45 * strength))),
				new KeyFrame(delay.add(Duration.millis(320)), new KeyValue(edge.diffuseColorProperty(), glow(tint, 0.4, 0.8, 1, 0)), new KeyValue(cell.diffuseColorProperty(), glow(tint, 1, 1, 1, 0))));
	}

	/**
	 * The tint of the force field at an opacity. JavaFX 3D blends with colors already multiplied by their opacity, so
	 * a translucent color that is not premultiplied shines as if it were solid.
	 */
	private static Color glow(Color tint, double red, double green, double blue, double opacity) {
		return Color.color(tint.getRed() * red * opacity, tint.getGreen() * green * opacity, tint.getBlue() * blue * opacity, opacity);
	}

	/** Back to nothing at all when a new round starts */
	void resetFence() {
		for (Flash flash : new ArrayList<Flash>(this.flashes)) {
			flash.stop();
		}
		this.flashes.clear();
	}

	/** The hexagons lit by one hit, and the animation that lights them. */
	private final class Flash {
		final Group plane;
		Timeline timeline;

		/** A flash on its plane of the wall. */
		Flash(Group plane) {
			this.plane = plane;
		}

		/** Ends the flash: the animation stops and its hexagons leave the scene. */
		void stop() {
			if (this.timeline != null) {
				this.timeline.stop();
			}
			host.fence.getChildren().remove(this.plane);
			ArenaFence.this.flashes.remove(this);
		}
	}
}
