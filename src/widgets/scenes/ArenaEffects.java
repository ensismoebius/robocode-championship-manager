package widgets.scenes;

import static widgets.scenes.ArenaMaterials.CRYSTAL;
import static widgets.scenes.ArenaMaterials.bubble;
import static widgets.scenes.ArenaMaterials.energy;
import static widgets.scenes.ArenaMaterials.grow;
import static widgets.scenes.ArenaMaterials.shape;
import static widgets.scenes.ArenaMaterials.shrink;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.util.Duration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The effects of the battle arena: the lightning of the discharges, the sparks,
 * the shockwaves, the flashes and the explosions, and the flights of the pieces.
 * They are added to the effects layer of the {@link BattleArena3D}.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaEffects {

	private final BattleArena3D host;

	ArenaEffects(BattleArena3D host) {
		this.host = host;
	}

	/**
	 * Animations of the effects follow the slow motion
	 */
	<T extends Animation> T effect(T animation) {
		animation.setRate(host.effectRate);
		return animation;
	}

	/**
	 * A jagged bolt of lightning from a to b, with now and then a branch, as thin
	 * tubes: a wide one that glows and a thin white core
	 */
	void bolt(LowPoly glow, LowPoly core, double[] from, double[] toValue, double radius, int depth) {
		int steps = 7;
		double length = Math.sqrt(Math.pow(toValue[0] - from[0], 2) + Math.pow(toValue[1] - from[1], 2) + Math.pow(toValue[2] - from[2], 2));
		double[] previous = from;
		for (int index = 1; index <= steps; index++) {
			double time = (double) index / steps;
			double jitter = index == steps ? 0 : length * 0.11;
			double[] point = new double[3];
			for (int deepIndex = 0; deepIndex < 3; deepIndex++) {
				point[deepIndex] = from[deepIndex] + (toValue[deepIndex] - from[deepIndex]) * time + (host.random.nextDouble() - 0.5) * 2 * jitter;
			}
			glow.tube(previous, point, radius * 3, 4);
			core.tube(previous, point, radius, 4);
			if (depth < 1 && index > 1 && index < steps && host.random.nextDouble() < 0.3) {
				double[] branch = new double[3];
				for (int deepIndex2 = 0; deepIndex2 < 3; deepIndex2++) {
					branch[deepIndex2] = point[deepIndex2] + (toValue[deepIndex2] - from[deepIndex2]) * 0.3 + (host.random.nextDouble() - 0.5) * length * 0.5;
				}
				this.bolt(glow, core, point, branch, radius * 0.7, depth + 1);
			}
			previous = point;
		}
	}

	/**
	 * A discharge: bolts from the center in every direction (some run along the
	 * floor), three different shapes shown one after the other, quickly, so it
	 * crackles
	 *
	 * @param floorY
	 *            where the floor is, in the coordinates of the blast
	 */
	void discharge(Group blast, ParallelTransition all, double[] center, double floorY, int bolts, double reach, double radius, Color tint, double delay) {
		Color arc = tint.interpolate(Color.web("#8fe9ff"), 0.35);
		PhongMaterial glowMaterial = energy(arc, 0.25);
		PhongMaterial coreMaterial = energy(Color.WHITE, 1);
		List<Node> frames = new ArrayList<Node>();
		for (int frame = 0; frame < 3; frame++) {
			LowPoly glow = new LowPoly();
			LowPoly core = new LowPoly();
			for (int index = 0; index < bolts; index++) {
				double angle = host.random.nextDouble() * Math.PI * 2;
				double rise = index % 3 == 0 ? 0.05 : host.random.nextDouble() * 1.4 - 0.3;
				double flat = Math.sqrt(Math.max(0.05, 1 - Math.min(0.9, rise * rise * 0.5)));
				double length = reach * (0.55 + host.random.nextDouble() * 0.6);
				double[] end = { center[0] + Math.cos(angle) * length * flat, Math.min(floorY - 1, center[1] - rise * length * 0.8), center[2] + Math.sin(angle) * length * flat };
				this.bolt(glow, core, center, end, radius, 0);
			}
			Group group = new Group(shape(glow.mesh, glowMaterial), shape(core.mesh, coreMaterial));
			group.setVisible(false);
			blast.getChildren().add(group);
			frames.add(group);
		}
		// Rapid flicker between the shapes, then gone
		Timeline flicker = new Timeline();
		int[] order = { 0, 1, 2, 0, 2, 1, 0 };
		for (int index2 = 0; index2 < order.length; index2++) {
			int shown = order[index2];
			flicker.getKeyFrames().add(new KeyFrame(Duration.millis(index2 * 45), event -> {
				for (int deepIndex = 0; deepIndex < frames.size(); deepIndex++) {
					frames.get(deepIndex).setVisible(deepIndex == shown);
				}
			}));
		}
		flicker.getKeyFrames().add(new KeyFrame(Duration.millis(order.length * 45), event -> frames.forEach(node -> node.setVisible(false))));
		flicker.setDelay(Duration.millis(delay));
		all.getChildren().add(flicker);
	}

	/**
	 * Sparks thrown up and out, that fall in an arc and go out
	 */
	void sparks(Group blast, ParallelTransition all, double[] center, double floorY, int count, double reach, double size, Color color, double millis) {
		PhongMaterial material = energy(color, 1);
		for (int index = 0; index < count; index++) {
			MeshView spark = shape(CRYSTAL, material);
			double scale = size * (0.5 + host.random.nextDouble() * 0.8);
			spark.setScaleX(scale);
			spark.setScaleY(scale);
			spark.setScaleZ(scale * 1.6);
			spark.setTranslateX(center[0]);
			spark.setTranslateY(center[1]);
			spark.setTranslateZ(center[2]);
			blast.getChildren().add(spark);
			double angle = host.random.nextDouble() * Math.PI * 2;
			double distance = reach * (0.3 + host.random.nextDouble() * 0.9);
			double height = reach * (0.3 + host.random.nextDouble() * 0.8);
			double time = millis * (0.7 + host.random.nextDouble() * 0.5);
			all.getChildren().add(new Timeline( //
					new KeyFrame(Duration.millis(time * 0.4), new KeyValue(spark.translateYProperty(), center[1] - height, Interpolator.EASE_OUT)), //
					new KeyFrame(Duration.millis(time), //
							new KeyValue(spark.translateXProperty(), center[0] + Math.cos(angle) * distance, Interpolator.EASE_OUT), //
							new KeyValue(spark.translateZProperty(), center[2] + Math.sin(angle) * distance, Interpolator.EASE_OUT), //
							new KeyValue(spark.translateYProperty(), floorY - 1, Interpolator.EASE_IN), //
							new KeyValue(spark.scaleXProperty(), 0.05), new KeyValue(spark.scaleYProperty(), 0.05), new KeyValue(spark.scaleZProperty(), 0.05))));
		}
	}

	/** A thin flat ring, one unit across, that spreads as a shock wave */
	private static final TriangleMesh RING = ringMesh();

	static TriangleMesh ringMesh() {
		LowPoly shape = new LowPoly();
		shape.ring(0.9, 1, -0.5, 0.5, 40);
		return shape.mesh;
	}

	/**
	 * A shock wave: a ring that spreads on the floor and fades
	 *
	 * @param floorY
	 *            where the floor is, in the coordinates of the blast
	 */
	void shockwave(Group blast, ParallelTransition all, double floorY, double radius, double delay, double millis, Color color) {
		MeshView ring = shape(RING, energy(color, 0.8));
		ring.setTranslateY(floorY - 0.5);
		ring.setScaleY(1.5);
		ring.setVisible(false);
		blast.getChildren().add(ring);
		ParallelTransition spread = new ParallelTransition();
		ScaleTransition ringSpread = new ScaleTransition(Duration.millis(millis), ring);
		ringSpread.setFromX(radius * 0.05);
		ringSpread.setFromZ(radius * 0.05);
		ringSpread.setToX(radius);
		ringSpread.setToZ(radius);
		ringSpread.setInterpolator(Interpolator.EASE_OUT);
		FadeTransition ringFade = new FadeTransition(Duration.millis(millis), ring);
		ringFade.setFromValue(1);
		ringFade.setToValue(0);
		ringFade.setInterpolator(Interpolator.EASE_IN);
		// The bubble of the same wave: a see through dome that grows as the ring does
		Sphere bubble = new Sphere(1, 32);
		bubble.setMaterial(bubble(color));
		bubble.setCullFace(CullFace.NONE);
		bubble.setTranslateY(floorY - 0.5);
		bubble.setVisible(false);
		blast.getChildren().add(bubble);
		ScaleTransition bubbleSpread = new ScaleTransition(Duration.millis(millis), bubble);
		bubbleSpread.setFromX(radius * 0.05);
		bubbleSpread.setFromY(radius * 0.05);
		bubbleSpread.setFromZ(radius * 0.05);
		bubbleSpread.setToX(radius);
		bubbleSpread.setToY(radius);
		bubbleSpread.setToZ(radius);
		bubbleSpread.setInterpolator(Interpolator.EASE_OUT);
		FadeTransition bubbleFade = new FadeTransition(Duration.millis(millis), bubble);
		bubbleFade.setFromValue(1);
		bubbleFade.setToValue(0);
		bubbleFade.setInterpolator(Interpolator.EASE_IN);
		spread.getChildren().addAll(ringSpread, ringFade, bubbleSpread, bubbleFade);
		spread.setDelay(Duration.millis(delay));
		spread.statusProperty().addListener((observable, before, now) -> {
			ring.setVisible(now == Animation.Status.RUNNING);
			bubble.setVisible(now == Animation.Status.RUNNING);
		});
		all.getChildren().add(spread);
	}

	void muzzleFlash(double posX, double posZ, Color tint) {
		MeshView flash = shape(CRYSTAL, energy(tint.interpolate(Color.WHITE, 0.8), 1));
		flash.setTranslateX(posX);
		flash.setTranslateZ(posZ);
		flash.setTranslateY(-21);
		host.effects.getChildren().add(flash);
		ScaleTransition out = this.effect(new ScaleTransition(Duration.millis(140), flash));
		out.setFromX(6);
		out.setFromY(6);
		out.setFromZ(6);
		out.setToX(0);
		out.setToY(0);
		out.setToZ(0);
		out.setOnFinished(event -> host.effects.getChildren().remove(flash));
		out.play();
	}

	/**
	 * An electric blast: a flash, bolts of lightning crackling out and sparks
	 * thrown in an arc, in the color of the shooter
	 */
	void explosion(double posX, double posZ, double size, Color tint) {
		Group blast = new Group();
		blast.setTranslateX(posX);
		blast.setTranslateZ(posZ);
		blast.setTranslateY(-18);

		MeshView flash = shape(CRYSTAL, energy(tint.interpolate(Color.WHITE, 0.9), 0.6));
		blast.getChildren().add(flash);
		double half = size / 2;
		ParallelTransition all = new ParallelTransition(grow(flash, 0.2 * half, 0.7 * half, 100), shrink(flash, 100, 140));
		double[] center = { 0, 0, 0 };
		// The floor is 18 below the center of a blast
		this.discharge(blast, all, center, 18, size > 30 ? 8 : 5, size * 2.2, Math.max(0.3, size / 60), tint, 0);
		this.sparks(blast, all, center, 18, size > 30 ? 26 : 14, size * 2, Math.max(0.5, size / 30), Color.web("#fff3b0"), 650);
		host.effects.getChildren().add(blast);
		this.effect(all).setOnFinished(event -> host.effects.getChildren().remove(blast));
		all.play();
		this.lightFlash(posX, posZ, tint.interpolate(Color.web("#8fe9ff"), 0.5), 0.5);
	}

	/**
	 * A piece flying in an arc: up, then down to the floor
	 */
	Timeline fly(Node piece, double deltaX, double deltaZ, double height, double millis) {
		double baseY = piece.getTranslateY();
		return new Timeline( //
				new KeyFrame(Duration.millis(millis * 0.4), new KeyValue(piece.translateYProperty(), baseY - height, Interpolator.EASE_OUT)), //
				new KeyFrame(Duration.millis(millis), //
						new KeyValue(piece.translateXProperty(), piece.getTranslateX() + deltaX, Interpolator.EASE_OUT), //
						new KeyValue(piece.translateZProperty(), piece.getTranslateZ() + deltaZ, Interpolator.EASE_OUT), //
						new KeyValue(piece.translateYProperty(), Math.max(baseY, 16), Interpolator.EASE_IN), //
						new KeyValue(piece.rotateProperty(), 540 + Math.random() * 360, Interpolator.LINEAR), //
						new KeyValue(piece.scaleXProperty(), 0.3), new KeyValue(piece.scaleYProperty(), 0.3), new KeyValue(piece.scaleZProperty(), 0.3)));
	}

	/**
	 * The explosions light up the arena for a moment
	 */
	void lightFlash(double posX, double posZ, Color color, double seconds) {
		host.flashLight.setTranslateX(posX);
		host.flashLight.setTranslateZ(posZ);
		host.flashLight.setTranslateY(-60);
		host.flashLight.setColor(color);
		this.effect(new Timeline(new KeyFrame(Duration.seconds(seconds), new KeyValue(host.flashLight.colorProperty(), Color.BLACK)))).play();
	}
}
