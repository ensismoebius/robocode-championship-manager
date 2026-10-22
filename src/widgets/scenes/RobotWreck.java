package widgets.scenes;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.util.Duration;
import widgets.scenes.ArenaFire.ParticleKind;
import static widgets.scenes.ArenaMaterials.CRYSTAL;
import static widgets.scenes.ArenaMaterials.energy;
import static widgets.scenes.ArenaMaterials.grow;
import static widgets.scenes.ArenaMaterials.shape;
import static widgets.scenes.ArenaMaterials.shrink;

/**
 * The wreck of a robot that is destroyed: the blast, the smoke, the debris, and the turret, the radar and the hull thrown across the arena
 */
final class RobotWreck {

	/**
	 * Blows up in its own colors: fireballs, shock waves seen as bubbles, smoke,
	 * debris, the hull, the turret and the radar thrown across the arena
	 */
	static void destroy(RobotView view) {
		if (view.dead) {
			return;
		}
		view.dead = true;
		double posX = view.root.getTranslateX();
		double posZ = view.root.getTranslateZ();
		Group blast = new Group();
		blast.setTranslateX(posX);
		blast.setTranslateZ(posZ);
		ParallelTransition all = new ParallelTransition();
		Color tint = view.playerColor;
		burst(view, blast, all, tint);
		smoke(view, posX, posZ);
		throwDebris(view, all);
		throwTurretAndRadar(view, all);
		throwHull(view, all);

		view.host.effects.getChildren().add(blast);
		view.host.fx.effect(all).setOnFinished(event -> view.host.effects.getChildren().remove(blast));
		view.running.add(all);
		all.play();
		view.host.fx.lightFlash(posX, posZ, tint, 1.4);

		view.nameLabel.setText("☠ " + view.displayName);
		view.balloon.setOpacity(0.6);
	}

	/** The flash of the blast, a bolt from the sky, the shock waves, the discharges of lightning and the sparks */
	private static void burst(RobotView view, Group blast, ParallelTransition all, Color tint) {
		Color hot = tint.interpolate(Color.WHITE, 0.85);
		MeshView core = shape(CRYSTAL, energy(Color.WHITE, 0.85));
		core.setTranslateY(-20);
		blast.getChildren().add(core);
		all.getChildren().addAll(grow(core, 2, 9, 140), shrink(core, 140, 180));
		double[] center = { 0, -20, 0 };
		// The bolt from the sky strikes first
		LowPoly strikeGlow = new LowPoly();
		LowPoly strikeCore = new LowPoly();
		view.host.fx.bolt(strikeGlow, strikeCore, new double[] { 0, -420, 0 }, center, 0.6, 0);
		Group strike = new Group(shape(strikeGlow.mesh, energy(tint.interpolate(Color.web("#8fe9ff"), 0.5), 0.45)), shape(strikeCore.mesh, energy(Color.WHITE, 1)));
		blast.getChildren().add(strike);
		all.getChildren().add(new Timeline(new KeyFrame(Duration.millis(120), event -> strike.setVisible(false))));
		// The shock wave: rings spreading on the floor from the wreck
		Color air = tint.interpolate(Color.WHITE, 0.7);
		view.host.fx.shockwave(blast, all, -1, 300, 0, 900, air);
		view.host.fx.shockwave(blast, all, -1, 210, 160, 750, Color.web("#8fe9ff"));
		view.host.fx.shockwave(blast, all, -1, 130, 0, 500, Color.WHITE);
		view.host.fx.discharge(blast, all, center, -1, 9, 110, 0.4, tint, 60);
		view.host.fx.discharge(blast, all, center, -1, 7, 90, 0.35, tint, 400);
		view.host.fx.discharge(blast, all, center, -1, 6, 120, 0.38, tint, 800);
		view.host.fx.sparks(blast, all, center, -1, 40, 120, 1.1, Color.web("#fff3b0"), 1100);
		view.host.fx.sparks(blast, all, center, -1, 18, 80, 0.9, hot, 1500);
	}

	/** The smoke rising from the wreck: particles, like the fire of the burning robots, thick and dark since it is destroyed */
	private static void smoke(RobotView view, double posX, double posZ) {
		// The smoke rising from the wreck: particles, like the fire of the burning robots,
		// thick and dark since it is destroyed
		Timeline smoke = new Timeline();
		for (int index = 0; index < 40; index++) {
			smoke.getKeyFrames().add(new KeyFrame(Duration.millis(index * 90), event -> {
				for (int puff = 0; puff < 2; puff++) {
					view.host.fire.spawn(ParticleKind.SMOKE, posX + (view.host.random.nextDouble() - 0.5) * 40, -20, posZ + (view.host.random.nextDouble() - 0.5) * 40, 0.8);
				}
			}));
		}
		view.running.add(smoke);
		smoke.play();
	}

	/** Debris in the colors of the robot, thrown across the arena */
	private static void throwDebris(RobotView view, ParallelTransition all) {
		PhongMaterial[] colors = { view.bodyMaterial, view.gunMaterial, new PhongMaterial(Color.web("#22252e")), new PhongMaterial(Color.web("#ffb000")) };
		for (int index = 0; index < 26; index++) {
			double angle = Math.PI * 2 * index / 26 + view.host.random.nextDouble() * 0.3;
			double size = 3 + view.host.random.nextDouble() * 5;
			Box piece = new Box(size, size, size);
			piece.setMaterial(colors[index % colors.length]);
			piece.setTranslateY(-15);
			view.debris.getChildren().add(piece);
			double reach = 120 + view.host.random.nextDouble() * 180;
			all.getChildren().add(view.host.fx.fly(piece, Math.cos(angle) * reach, Math.sin(angle) * reach, 70 + view.host.random.nextDouble() * 120, 1500));
		}
	}

	/** The turret and the radar are thrown in the air */
	private static void throwTurretAndRadar(RobotView view, ParallelTransition all) {
		// The turret and the radar are thrown in the air
		double throwAngle = view.host.random.nextDouble() * Math.PI * 2;
		view.turret.setRotationAxis(new Point3D(1, 0, 0.4));
		view.radar.setRotationAxis(new Point3D(0.3, 0, 1));
		Timeline thrown = new Timeline( //
				new KeyFrame(Duration.millis(550), new KeyValue(view.turret.translateYProperty(), -140, Interpolator.EASE_OUT), new KeyValue(view.radar.translateYProperty(), -190, Interpolator.EASE_OUT)), //
				new KeyFrame(Duration.millis(1400), //
						new KeyValue(view.turret.translateYProperty(), 14, Interpolator.EASE_IN), //
						new KeyValue(view.turret.translateXProperty(), Math.cos(throwAngle) * 170, Interpolator.EASE_OUT), //
						new KeyValue(view.turret.translateZProperty(), Math.sin(throwAngle) * 170, Interpolator.EASE_OUT), //
						new KeyValue(view.turret.rotateProperty(), 720, Interpolator.LINEAR), //
						new KeyValue(view.radar.translateYProperty(), 24, Interpolator.EASE_IN), //
						new KeyValue(view.radar.translateXProperty(), -Math.cos(throwAngle) * 200, Interpolator.EASE_OUT), //
						new KeyValue(view.radar.translateZProperty(), -Math.sin(throwAngle) * 200, Interpolator.EASE_OUT), //
						new KeyValue(view.radar.rotateProperty(), -900, Interpolator.LINEAR)));
		all.getChildren().add(thrown);
	}

	/** The hull is thrown too: it flies over the arena, turns over and lands tilted */
	private static void throwHull(RobotView view, ParallelTransition all) {
		// The hull is thrown too: it flies over the arena, turns over and lands tilted
		double hullAngle = view.host.random.nextDouble() * Math.PI * 2;
		double hullReach = 130 + view.host.random.nextDouble() * 80;
		double hullX = Math.cos(hullAngle) * hullReach;
		double hullZ = Math.sin(hullAngle) * hullReach;
		view.body.setRotationAxis(new Point3D(-Math.sin(hullAngle), 0, Math.cos(hullAngle)));
		all.getChildren().add(new Timeline( //
				new KeyFrame(Duration.millis(450), new KeyValue(view.body.translateYProperty(), -80, Interpolator.EASE_OUT), new KeyValue(view.body.translateXProperty(), hullX * 0.5, Interpolator.EASE_OUT),
						new KeyValue(view.body.translateZProperty(), hullZ * 0.5, Interpolator.EASE_OUT), new KeyValue(view.body.rotateProperty(), 60, Interpolator.LINEAR)), //
				new KeyFrame(Duration.millis(1300), new KeyValue(view.body.translateYProperty(), 0, Interpolator.EASE_IN), new KeyValue(view.body.translateXProperty(), hullX, Interpolator.EASE_OUT),
						new KeyValue(view.body.translateZProperty(), hullZ, Interpolator.EASE_OUT), new KeyValue(view.body.rotateProperty(), 35, Interpolator.EASE_OUT))));
	}
}
