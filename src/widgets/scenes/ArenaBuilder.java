package widgets.scenes;

import javafx.scene.AmbientLight;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.effect.Bloom;
import javafx.scene.layout.Region;
import javafx.scene.transform.Translate;
import utils.configuration.Configuration;

/**
 * The building of the arena scene: the camera, the sub scene that shows the 3D world, the layers over it, the lights and the arena itself
 */
final class ArenaBuilder {

	private final BattleArena3D host;

	/** Creates the builder of the scene of the given arena. */
	ArenaBuilder(BattleArena3D host) {
		this.host = host;
	}

	/** The camera that looks at the arena, with its clip planes, part of the world */
	void buildCamera() {
		host.camera.setNearClip(5);
		host.camera.setFarClip(30000);
		host.camera.getTransforms().setAll(host.cameraLook, host.cameraYaw, host.cameraPitch, host.cameraRoll, host.cameraDistance);
		host.world.getChildren().add(host.camera);
	}

	/** The 3D scene that fills the window, filled with the sky of the arena */
	SubScene buildSubScene() {
		SubScene scene = new SubScene(host.world, 100, 100, true, Configuration.isAntialiasing() ? SceneAntialiasing.BALANCED : SceneAntialiasing.DISABLED);
		scene.setCamera(host.camera);
		// A solid color: with a gradient the 3D frames are not cleared and leave
		// trails
		scene.setFill(host.style.sky);
		if (host.style == ArenaStyle.VOLCANO) {
			// The hottest lava bleeds light around it
			scene.setEffect(new Bloom(0.6));
		}
		scene.widthProperty().bind(host.widthProperty());
		scene.heightProperty().bind(host.heightProperty());
		return scene;
	}

	/** The layers over the 3D scene: the balloons, the letterbox, the showcase, the HUD, the scanlines and the flash */
	void buildOverlay() {
		host.balloons.setMouseTransparent(true);

		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		scanlines.setMouseTransparent(true);

		host.whiteFlash.setStyle("-fx-background-color: white;");
		host.whiteFlash.setOpacity(0);
		host.whiteFlash.setMouseTransparent(true);

		host.getStyleClass().add("arena-3d");
		host.showcaseLayer.setMouseTransparent(true);
		host.showcaseLayer.setPickOnBounds(false);

		host.getChildren().setAll(host.subScene, host.balloons, host.hud.buildLetterbox(), host.showcaseLayer, host.announcer.build(), host.hud.buildHud(), host.whiteFlash, scanlines);
	}

	/** Lights the arena: the ambient light, a key light and the glow of the fire. */
	void buildLights() {
		AmbientLight ambient = new AmbientLight(host.style.ambient);
		PointLight key = new PointLight(host.style.key);
		key.getTransforms().add(new Translate(-300, -900, -500));
		host.fireGlow.getScope().addAll(host.fireLayer, host.glowing);
		host.world.getChildren().addAll(ambient, key, host.flashLight, host.fireGlow);
	}

	/**
	 * The floor and the scenery of the style, and the force field
	 */
	void buildArena() {
		host.fence.getChildren().clear();
		host.glowing.getChildren().clear();
		host.lava = null;
		host.flows.clear();
		double width = host.fieldWidth;
		double height = host.fieldHeight;
		switch (host.style) {
		case NEON -> host.neon.build(width, height);
		case VOLCANO -> host.volcano.build(width, height);
		case ICE -> host.ice.build(width, height);
		case JUNGLE -> host.jungle.build(width, height);
		}
		host.arena.getChildren().add(host.glowing);
		host.forceField.buildFence();
		host.critters = new Critters(host.style, width, height, host.random);
		host.arena.getChildren().add(host.critters.root());
		if (host.style == ArenaStyle.ICE && host.ice.seaRadius() > 0) {
			double centreX = host.ice.seaX();
			double centreZ = host.ice.seaZ();
			double radius = host.ice.seaRadius();
			host.critters.setWater((posX, posZ) -> Math.hypot(posX - centreX, posZ - centreZ) < IceArena.shore(Math.atan2(posZ - centreZ, posX - centreX), radius) * 0.97);
		}
	}
}
