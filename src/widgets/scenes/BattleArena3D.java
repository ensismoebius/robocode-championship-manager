package widgets.scenes;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.CullFace;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import model.Opponent;
import tournament.Bracket;
import utils.configuration.Configuration;
import utils.robocode.BattleControl;
import utils.robocode.BattleRecording;
import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;
import widgets.BattleAudio;
import widgets.Song;
import widgets.Sfx;
import widgets.Sound;
import widgets.Theme;
import widgets.scenes.ArenaHud.HudPlayer;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The battle in 3D, replayed from the turns recorded by robocode: a neon arena,
 * low poly tanks with the colors the robots chose, bullets, explosions and
 * burnt wrecks, a fighting game HUD, an adaptive soundtrack and a camera that
 * films it like an action movie. When a robot dies, the last second is
 * replayed in slow motion.
 *
 * Robocode runs much faster than real time without its window, so the battle
 * is recorded first and then played here at robocode's normal speed.
 *
 * Keys: V changes the camera, space pauses, left/right change the speed, Enter
 * skips the replay or the battle. The mouse orbits (drag) and zooms (wheel).
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class BattleArena3D extends StackPane {

	static final double[] SPEEDS = Configuration.BATTLE_SPEEDS;

	/** The 3, 2, 1 before the fight, then the fight banner for a moment */
	/** The camera flies over the arena before the first round and ends where the battle starts */
	static final double FLYBY_SECONDS = 6.5;
	static final int COUNTDOWN_SECONDS = 3;
	/** Each robot is presented alone before the battle, then the versus */
	private static final double SHOWCASE_SECONDS = 7;
	private static final double VERSUS_SECONDS = 2.6;
	/** The action stops for a moment when a robot dies, then the replay */
	static final double FREEZE_SECONDS = 1.3;
	/** The photos of the key moments are kept small, as JPEG */
	private static final int PHOTO_WIDTH = 640;
	private static final int PHOTO_HEIGHT = 360;
	/** A hit this strong (power) makes a photo: the first one of each round */
	static final double PHOTO_HIT_POWER = 2.5;
	/** Seconds from a hit to its photo, so the flash of the impact has grown */
	static final double PHOTO_HIT_DELAY = 0.2;
	/** Seconds from a destruction to its photo: the tank is falling, the battle is held (see freezeForReplay) */
	static final double PHOTO_DESTRUCTION_DELAY = 1.0;
	static final Color[] PLAYER_COLORS = { Theme.PLAYER_1, Theme.PLAYER_2, Color.web("#5af08f"), Color.web("#c86bff"), Color.web("#ff9f1c") };
	static final Color FENCE_BASE = Color.color(0, 0, 0, 0);

	enum ViewMode {
		CINEMATIC("CINEMATOGRÁFICA", -30, 45), FREE("LIVRE", -38, 38), ISOMETRIC("ISOMÉTRICA", -35.26, 12), TOP_DOWN("VISTA DE CIMA", -89.5, 12);

		final String label;
		final double pitch;
		final double fov;

		ViewMode(String label, double pitch, double fov) {
			this.label = label;
			this.pitch = pitch;
			this.fov = fov;
		}
	}

	/** The shots of the cinematic camera */
	enum Shot {
		WIDE, CHASE, LOW_TRACK, CLOSE, CRANE, IMPACT, FATAL
	}

	enum Phase {
		LOADING, SHOWCASE, FLYBY, INTRO, PLAYING, FREEZE, REPLAY, ROUND_END, FINISHED
	}

	private final List<Opponent> fighters;
	private Runnable onFinished;
	final BattleAudio audio = new BattleAudio();
	final Random random = new Random();

	// 3D
	private final Group world = new Group();
	final Group arena = new Group();
	final Group effects = new Group();
	private final Group robotLayer = new Group();
	final Group bulletLayer = new Group();
	/**
	 * The cage panels. JavaFX does not sort translucent shapes: they are drawn in
	 * the order of the scene graph and hide what is drawn after them, so the
	 * panels must come last for the robots to be seen through them
	 */
	private final Group fence = new Group();
	/** The looks of the arena */
	final ArenaStyle style;
	/** Parts of the arena that glow, like lava: lit by the fire glow */
	final Group glowing = new Group();
	/** The lava of the volcano arena, pulsing */
	PhongMaterial lava;
	private final List<Flow> flows = new ArrayList<Flow>();
	private double clock = 0;
	/** Flame, spark and smoke sprites, sorted back to front every frame */
	final Group fireLayer = new Group();
	/** Shafts of light through the canopy (jungle), over everything but the fire */
	final Group sunbeams = new Group();
	/** The city of the neon arena, its lights and cars */
	private final NeonArena neon = new NeonArena(this);
	private final IceArena ice = new IceArena(this);
	final ArenaFire fire = new ArenaFire(this);
	final ArenaEffects fx = new ArenaEffects(this);
	final ArenaHud hud = new ArenaHud(this);
	final VolcanoArena volcano = new VolcanoArena(this);
	final JungleArena jungle = new JungleArena(this);
	final ArenaBullets shots = new ArenaBullets(this);
	final ArenaCamera director = new ArenaCamera(this);
	final ArenaShowcase showcase = new ArenaShowcase(this);
	final ArenaRounds rounds = new ArenaRounds(this);
	/** Translucent panels of the cage, each one with its own material to flash */
	final List<FenceSegment> fenceSegments = new ArrayList<FenceSegment>();
	private final SubScene subScene;
	final PerspectiveCamera camera = new PerspectiveCamera(true);
	final Translate cameraLook = new Translate();
	final Rotate cameraYaw = new Rotate(0, Rotate.Y_AXIS);
	final Rotate cameraPitch = new Rotate(-38, Rotate.X_AXIS);
	final Rotate cameraRoll = new Rotate(0, Rotate.Z_AXIS);
	final Translate cameraDistance = new Translate();
	/** Lights up the explosions */
	final PointLight flashLight = new PointLight(Color.BLACK);
	/**
	 * Lights only the flames, so they glow. Its scope never gets empty, an empty
	 * scope would light everything.
	 */
	final AmbientLight fireGlow = new AmbientLight(Color.WHITE);

	// Camera: what it sees now and what it wants to see, eased every frame
	ViewMode viewMode = ViewMode.CINEMATIC;
	final double[] eye = { 0, -700, -900 };
	final double[] look = { 0, -10, 0 };
	final double[] eyeTarget = { 0, -700, -900 };
	final double[] lookTarget = { 0, -10, 0 };
	double fov = 45;
	double fovTarget = 45;
	double roll = 0;
	double rollTarget = 0;
	double follow = 4;
	double shake = 0;
	double cinematicOrbitAngle = 0;

	// Free, isometric and top views
	double yaw = 0;
	double targetYaw = 0;
	double freePitch = -38;
	double pitch = -38;
	double zoom = 1;
	double targetZoom = 1;
	double yawSpeed = 0;
	private double lastDragX;
	private double lastDragY;

	// Cinematic director
	Shot shot = Shot.WIDE;
	double shotTime = 0;
	double shotDuration = 1000;
	int shotSubject = 0;
	double shotSide = 1;
	double shotSeed = 0;
	/** How far the close shots are, the death is filmed from farther */
	double closeDistance = 100;

	// 2D on top: balloons, HUD, banners and the replay frame
	final Pane balloons = new Pane();
	final Label banner = new Label();
	final Label subBanner = new Label();
	final Label loading = new Label("PREPARANDO A BATALHA");
	final Label roundLabel = new Label();
	/** The stage of the championship, over the loading and the versus */
	final Label stageLabel = new Label();
	String stageName = "";
	Bracket.Importance stageImportance = Bracket.Importance.REGULAR;
	final Label speedLabel = new Label();
	final Label viewLabel = new Label();
	final HBox hudTop = new HBox();
	final List<HudPlayer> hudPlayers = new ArrayList<HudPlayer>();
	final Rectangle letterboxTop = new Rectangle();
	final Rectangle letterboxBottom = new Rectangle();
	final HBox replayTag = new HBox(14);
	/** The card of the robot being presented */
	final StackPane showcaseLayer = new StackPane();
	final Region whiteFlash = new Region();

	// Playback
	BattleRecording recording;
	double fieldWidth = 800;
	double fieldHeight = 600;
	final Map<Integer, RobotView> robots = new HashMap<Integer, RobotView>();
	final Map<Integer, Group> bullets = new HashMap<Integer, Group>();
	final Set<Integer> bulletsSeen = new HashSet<Integer>();
	final Set<Integer> bulletsExploded = new HashSet<Integer>();
	final Set<Integer> lowEnergyWarned = new HashSet<Integer>();
	final Map<Integer, Integer> roundWins = new HashMap<Integer, Integer>();
	Phase phase = Phase.LOADING;
	double phaseTime = 0;
	int round = 0;
	double turnPosition = 0;
	int lastTurnProcessed = -1;
	/** Index in SPEEDS, starts with the configured one */
	int speed;
	/** The critter the fly by starts looking at, where it is when the fly by begins, or null */
	double[] flyFocus;
	/** Where the critter the fly by starts on is now, while the camera stays on it */
	Supplier<double[]> flyFocusSource;
	/** The animals that live around the field */
	Critters critters;
	boolean paused = false;
	double hitTension = 0;
	/** Animations of effects run slower in the replay */
	double effectRate = 1;

	// The death being replayed
	int deathTurn = -1;
	int replayTo;
	int fatalBullet = -1;
	int impactTurn = -1;
	boolean replayed = false;
	// The photos of the key moments of the battle, see takePhoto
	private final List<BattlePhoto> photos = new ArrayList<BattlePhoto>();
	final List<PendingPhoto> pendingPhotos = new ArrayList<PendingPhoto>();
	boolean hitPhotoTaken = false;

	// Callouts and comments
	BattleCommentary commentary;
	final ArenaCommentary announcer = new ArenaCommentary(this);
	BattleCommentary.Headline headline;

	// The presentation of the robots
	int showcaseOrder = 0;
	boolean versus = false;
	Sound showcaseMusic;

	private final AnimationTimer timer = new AnimationTimer() {
		private long last = -1;

		@Override
		public void handle(long now) {
			double dt = this.last < 0 ? 0 : Math.min(0.1, (now - this.last) / 1e9);
			this.last = now;
			BattleArena3D.this.update(dt);
		}
	};

	/**
	 * @param fighters
	 *            the robots, to show their names, teams and logos
	 */
	/**
	 * Warns which stage of the championship this battle is: a semifinal and a
	 * final are highlighted. Call it before the battle starts.
	 */
	public void setStage(String name, Bracket.Importance importance) {
		this.stageName = name == null ? "" : name.toUpperCase();
		this.stageImportance = importance == null ? Bracket.Importance.REGULAR : importance;
		boolean big = this.stageImportance != Bracket.Importance.REGULAR;
		this.stageLabel.setText(big ? "★ " + this.stageName + " ★" : this.stageName);
		this.stageLabel.getStyleClass().removeAll("arena-stage", "arena-stage-big");
		this.stageLabel.getStyleClass().add(big ? "arena-stage-big" : "arena-stage");
		this.stageLabel.setVisible(!this.stageName.isEmpty());
		this.stageLabel.setManaged(!this.stageName.isEmpty());
		this.roundLabel.getStyleClass().remove("arena-round-big");
		if (big) {
			this.roundLabel.getStyleClass().add("arena-round-big");
		}
	}

	public BattleArena3D(List<Opponent> fighters) {
		this.fighters = fighters;
		this.speed = Math.max(0, java.util.Arrays.binarySearch(SPEEDS, Configuration.getBattleSpeed()));
		this.style = ArenaStyle.resolve(Configuration.getArena(), this.random);
		this.audio.setSong(Configuration.isArenaMusic() ? this.style.song : Song.resolve(Configuration.getBattleMusic(), this.random));
		this.audio.setMusicLevel(Configuration.getMusicVolume() / 100.0);

		this.world.getChildren().addAll(this.arena, this.robotLayer, this.bulletLayer, this.effects, this.sunbeams, this.fireLayer, this.fence);
		this.buildLights();
		this.buildArena();

		this.camera.setNearClip(5);
		this.camera.setFarClip(30000);
		this.camera.getTransforms().setAll(this.cameraLook, this.cameraYaw, this.cameraPitch, this.cameraRoll, this.cameraDistance);
		this.world.getChildren().add(this.camera);

		this.subScene = new SubScene(this.world, 100, 100, true, Configuration.isAntialiasing() ? SceneAntialiasing.BALANCED : SceneAntialiasing.DISABLED);
		this.subScene.setCamera(this.camera);
		// A solid color: with a gradient the 3D frames are not cleared and leave
		// trails
		this.subScene.setFill(this.style.sky);
		if (this.style == ArenaStyle.VOLCANO) {
			// The hottest lava bleeds light around it
			this.subScene.setEffect(new Bloom(0.6));
		}
		this.subScene.widthProperty().bind(this.widthProperty());
		this.subScene.heightProperty().bind(this.heightProperty());

		this.balloons.setMouseTransparent(true);

		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		scanlines.setMouseTransparent(true);

		this.whiteFlash.setStyle("-fx-background-color: white;");
		this.whiteFlash.setOpacity(0);
		this.whiteFlash.setMouseTransparent(true);

		this.getStyleClass().add("arena-3d");
		this.showcaseLayer.setMouseTransparent(true);
		this.showcaseLayer.setPickOnBounds(false);

		this.getChildren().setAll(this.subScene, this.balloons, this.hud.buildLetterbox(), this.showcaseLayer, this.announcer.build(), this.hud.buildHud(), this.whiteFlash, scanlines);

		this.installControls();
		this.hud.showBanner("", null);
		this.loading.getStyleClass().add("arena-banner");
		this.loading.setStyle("-fx-font-size: 30px;");
		Theme.blink(this.loading);

		// Runs only while on the screen
		this.sceneProperty().addListener((observable, oldScene, newScene) -> {
			if (newScene == null) {
				this.timer.stop();
				this.audio.stop();
				this.stopShowcaseMusic();
			} else {
				this.timer.start();
			}
		});
	}

	/**
	 * Starts showing the battle
	 *
	 * @param onFinished
	 *            called when the last round ends or is skipped
	 */
	public void play(BattleRecording recording, Runnable onFinished) {
		this.recording = recording;
		this.onFinished = onFinished;
		this.fieldWidth = recording.getWidth();
		this.fieldHeight = recording.getHeight();
		this.arena.getChildren().clear();
		this.buildArena();

		for (BattleRecording.Robot robot : recording.getRobots()) {
			RobotView view = new RobotView(this, robot, this.opponentOf(robot), this.robots.size());
			this.robots.put(robot.getIndex(), view);
			this.robotLayer.getChildren().add(view.root);
			this.balloons.getChildren().add(view.balloon);
			this.roundWins.put(robot.getIndex(), 0);
		}
		this.hud.buildHudPlayers();
		this.loading.setVisible(false);

		Map<Integer, String> names = new HashMap<Integer, String>();
		for (BattleRecording.Robot robot : recording.getRobots()) {
			names.put(robot.getIndex(), this.robots.get(robot.getIndex()).displayName);
		}
		this.commentary = new BattleCommentary(names, this.random);

		this.audio.start();
		this.showcase.startShowcase();
	}

	Opponent opponentOf(BattleRecording.Robot robot) {
		String name = BattleControl.robotClassName(robot.getName());
		return this.fighters.stream().filter(o -> o.getFullName().equals(name)).findFirst().orElse(null);
	}

	// ------------------------------------------------------------------ scene

	private void buildLights() {
		AmbientLight ambient = new AmbientLight(this.style.ambient);
		PointLight key = new PointLight(this.style.key);
		key.getTransforms().add(new Translate(-300, -900, -500));
		this.fireGlow.getScope().addAll(this.fireLayer, this.glowing);
		this.world.getChildren().addAll(ambient, key, this.flashLight, this.fireGlow);
	}

	/**
	 * The floor and the scenery of the style, and the force field
	 */
	private void buildArena() {
		this.fenceSegments.clear();
		this.fence.getChildren().clear();
		this.glowing.getChildren().clear();
		this.lava = null;
		this.flows.clear();
		double w = this.fieldWidth;
		double h = this.fieldHeight;
		switch (this.style) {
		case NEON -> this.neon.build(w, h);
		case VOLCANO -> this.volcano.build(w, h);
		case ICE -> this.ice.build(w, h);
		case JUNGLE -> this.jungle.build(w, h);
		}
		this.arena.getChildren().add(this.glowing);
		this.buildFence(w, h);
		this.critters = new Critters(this.style, w, h, this.random);
		this.arena.getChildren().add(this.critters.root());
		if (this.style == ArenaStyle.ICE && this.ice.seaRadius() > 0) {
			double centreX = this.ice.seaX();
			double centreZ = this.ice.seaZ();
			double radius = this.ice.seaRadius();
			this.critters.setWater((x, z) -> Math.hypot(x - centreX, z - centreZ) < IceArena.shore(Math.atan2(z - centreZ, x - centreX), radius) * 0.97);
		}
	}

	/**
	 * A dome around everything, seen from inside, that shines by itself
	 */
	static Sphere skyDome(Image picture) {
		Sphere dome = new Sphere(14000, 48);
		dome.setCullFace(CullFace.FRONT);
		PhongMaterial sky = new PhongMaterial(Color.BLACK);
		sky.setSelfIlluminationMap(picture);
		dome.setMaterial(sky);
		return dome;
	}

	MeshView meshView(TriangleMesh mesh, PhongMaterial material) {
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		return view;
	}

	/** The lava breathes between these tints of its texture */
	static final Color LAVA_DIM = Color.gray(0.72);
	static final Color LAVA_FULL = Color.WHITE;
	/** Units of the world one tile of the lava texture covers */
	static final double LAVA_TILE = 150;
	/**
	 * A surface of lava whose texture slides: its texture coordinates move every
	 * frame
	 */
	record Flow(TriangleMesh mesh, float[] base, double du, double dv) {
	}

	/**
	 * A quad of flowing lava
	 *
	 * @param corners
	 *            four {x, y, z}, in order around the quad: the texture goes from
	 *            the first to the second (u) and from the first to the fourth (v)
	 * @param du
	 *            tiles per second the texture slides along u, negative goes from
	 *            the first corner to the second
	 */
	void flow(double[][] corners, double du, double dv) {
		this.flow(corners, du, dv, this.lava, LAVA_TILE);
	}

	/**
	 * @param tile
	 *            units of the world one tile of the texture covers
	 */
	void flow(double[][] corners, double du, double dv, PhongMaterial material, double tile) {
		double uLength = Math.sqrt(distance2(corners[0], corners[1])) / tile;
		double vLength = Math.sqrt(distance2(corners[0], corners[3])) / tile;
		TriangleMesh mesh = new TriangleMesh();
		for (double[] corner : corners) {
			mesh.getPoints().addAll((float) corner[0], (float) corner[1], (float) corner[2]);
		}
		float[] base = { 0, 0, (float) uLength, 0, (float) uLength, (float) vLength, 0, (float) vLength };
		mesh.getTexCoords().addAll(base);
		mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		// Only the lava glows, water is lit like the rest
		(this.style == ArenaStyle.VOLCANO ? this.glowing : this.arena).getChildren().add(view);
		this.flows.add(new Flow(mesh, base, du, dv));
	}

	/**
	 * A flat strip of lava from one point to another, flowing that way
	 */
	void flowStrip(double x1, double z1, double x2, double z2, double width, double y, double speed) {
		this.flowStrip(x1, z1, x2, z2, width, y, speed, this.lava, LAVA_TILE);
	}

	void flowStrip(double x1, double z1, double x2, double z2, double width, double y, double speed, PhongMaterial material, double tile) {
		double length = Math.hypot(x2 - x1, z2 - z1);
		double px = -(z2 - z1) / length * width / 2;
		double pz = (x2 - x1) / length * width / 2;
		this.flow(new double[][] { { x1 - px, y, z1 - pz }, { x2 - px, y, z2 - pz }, { x2 + px, y, z2 + pz }, { x1 + px, y, z1 + pz } }, -speed, 0, material, tile);
	}

	/**
	 * A flat ribbon along a winding line, with its own half width at each point: a
	 * single mesh, so the texture runs on without a seam at the joints
	 */
	private void flowRibbon(double[] xs, double[] zs, double[] halfWidths, double y, double du, double dv, PhongMaterial material, double tile) {
		int count = xs.length;
		TriangleMesh mesh = new TriangleMesh();
		float[] texture = new float[4 * count];
		double along = 0;
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				along += Math.hypot(xs[i] - xs[i - 1], zs[i] - zs[i - 1]);
			}
			// The side of the ribbon: across the direction of the line
			double dx = xs[Math.min(i + 1, count - 1)] - xs[Math.max(i - 1, 0)];
			double dz = zs[Math.min(i + 1, count - 1)] - zs[Math.max(i - 1, 0)];
			double length = Math.hypot(dx, dz);
			double sx = -dz / length * halfWidths[i];
			double sz = dx / length * halfWidths[i];
			mesh.getPoints().addAll((float) (xs[i] + sx), (float) y, (float) (zs[i] + sz), (float) (xs[i] - sx), (float) y, (float) (zs[i] - sz));
			// Along the line the texture grows with the length, across it with the width
			float u = (float) (along / tile);
			float v = (float) (2 * halfWidths[i] / tile);
			texture[4 * i] = u;
			texture[4 * i + 1] = 0;
			texture[4 * i + 2] = u;
			texture[4 * i + 3] = v;
		}
		mesh.getTexCoords().addAll(texture);
		for (int i = 0; i < count - 1; i++) {
			int left = 2 * i;
			int right = left + 1;
			int nextLeft = left + 2;
			int nextRight = left + 3;
			mesh.getFaces().addAll(left, left, nextLeft, nextLeft, nextRight, nextRight);
			mesh.getFaces().addAll(left, left, nextRight, nextRight, right, right);
		}
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		this.arena.getChildren().add(view);
		this.flows.add(new Flow(mesh, texture, du, dv));
	}

	/**
	 * A river from one side of the field to the other, meandering around a line at
	 * distance centre from the middle, its banks wider and narrower in turn
	 */
	void river(double fromX, double toX, double sign, double centre, double phase, double width, double speed, PhongMaterial material, double tile) {
		int count = 150;
		double[] xs = new double[count];
		double[] zs = new double[count];
		double[] halfWidths = new double[count];
		for (int i = 0; i < count; i++) {
			double x = fromX + (toX - fromX) * i / (count - 1);
			double bend = 45 * Math.sin(x / 330 + phase) + 22 * Math.sin(x / 120 + 1.7 + 2 * phase) + 10 * Math.sin(x / 57 + 0.3 * phase);
			xs[i] = x;
			zs[i] = sign * (centre + bend);
			halfWidths[i] = width / 2 * (0.8 + 0.3 * (0.5 + 0.5 * Math.sin(x / 190 + 0.9 + phase)));
		}
		this.flowRibbon(xs, zs, halfWidths, 3, -speed, 0, material, tile);
	}

	static double distance2(double[] a, double[] b) {
		return (a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]);
	}

	private void updateFlows() {
		for (Flow flow : this.flows) {
			float[] coordinates = new float[flow.base().length];
			// Kept small: the texture repeats every 1
			float u = (float) ((this.clock * flow.du()) % 1);
			float v = (float) ((this.clock * flow.dv()) % 1);
			for (int i = 0; i < coordinates.length / 2; i++) {
				coordinates[2 * i] = flow.base()[2 * i] + u;
				coordinates[2 * i + 1] = flow.base()[2 * i + 1] + v;
			}
			flow.mesh().getTexCoords().setAll(coordinates);
		}
	}


	/**
	 * A textured material
	 */
	static PhongMaterial textured(Image texture, Color specular) {
		PhongMaterial material = new PhongMaterial(Color.WHITE);
		material.setDiffuseMap(texture);
		material.setSpecularColor(specular);
		material.setSpecularPower(40);
		return material;
	}

	/**
	 * A flat quad with the texture repeating, not moving
	 *
	 * @param corners
	 *            four {x, y, z} in order around it
	 * @param tile
	 *            units of the world one tile of the texture covers
	 */
	MeshView tiledQuad(double[][] corners, PhongMaterial material, double tile) {
		TriangleMesh mesh = new TriangleMesh();
		for (double[] corner : corners) {
			mesh.getPoints().addAll((float) corner[0], (float) corner[1], (float) corner[2]);
		}
		float uLength = (float) (Math.sqrt(distance2(corners[0], corners[1])) / tile);
		float vLength = (float) (Math.sqrt(distance2(corners[0], corners[3])) / tile);
		mesh.getTexCoords().addAll(0, 0, uLength, 0, uLength, vLength, 0, vLength);
		mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		return view;
	}

	/**
	 * Invisible panels around the field, they flash when hit
	 */
	private void buildFence(double w, double h) {
		// Force field panels: 100% transparent/invisible until hit
		double wallHeight = 80;
		for (int wall = 0; wall < 4; wall++) {
			double span = wall <= 1 ? w : h;
			int count = (int) Math.round(span / 100);
			for (int index = 0; index < count; index++) {
				double along = index * 100 + 50 - span / 2;
				double normal = wall <= 1 ? h / 2 : w / 2;
				PhongMaterial panelMaterial = material(FENCE_BASE);
				Box panel = wall <= 1 ? box(98, wallHeight, 1.0, along, -wallHeight / 2, wall == 0 ? -normal : normal, panelMaterial)
						: box(1.0, wallHeight, 98, wall == 2 ? -normal : normal, -wallHeight / 2, along, panelMaterial);
				panel.setVisible(false);
				this.fence.getChildren().add(panel);
				this.fenceSegments.add(new FenceSegment(wall, index, panel, panelMaterial));
			}
		}
	}

	static PhongMaterial material(Color color) {
		PhongMaterial material = new PhongMaterial(color);
		material.setSpecularColor(color.brighter());
		material.setSpecularPower(24);
		return material;
	}

	private static Cylinder cylinder(double radius, double height, int divisions, double x, double y, double z, Point3D axis, double angle, PhongMaterial material) {
		Cylinder cylinder = new Cylinder(radius, height, divisions);
		cylinder.setTranslateX(x);
		cylinder.setTranslateY(y);
		cylinder.setTranslateZ(z);
		cylinder.setRotationAxis(axis);
		cylinder.setRotate(angle);
		cylinder.setMaterial(material);
		return cylinder;
	}

	static Box box(double w, double h, double d, double x, double y, double z, PhongMaterial material) {
		Box box = new Box(w, h, d);
		box.setTranslateX(x);
		box.setTranslateY(y);
		box.setTranslateZ(z);
		box.setMaterial(material);
		return box;
	}

	/**
	 * One segment of the invisible force field. It only appears when struck.
	 */
	static class FenceSegment {
		final int wall;
		final int index;
		final Box box;
		final PhongMaterial material;
		Timeline animation;

		FenceSegment(int wall, int index, Box box, PhongMaterial material) {
			this.wall = wall;
			this.index = index;
			this.box = box;
			this.material = material;
		}

		int wall() {
			return wall;
		}

		int index() {
			return index;
		}

		Box box() {
			return box;
		}

		PhongMaterial material() {
			return material;
		}
	}

	/**
	 * Robocode position to the world: the field is centered, north is +Z, up is
	 * -Y
	 */
	double worldX(double x) {
		return x - this.fieldWidth / 2;
	}

	double worldZ(double y) {
		return y - this.fieldHeight / 2;
	}

	// ------------------------------------------------------------------- HUD

	static String toCss(Color color) {
		return String.format("#%02x%02x%02x", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
	}

	// ---------------------------------------------------------------- controls

	private void installControls() {
		this.setFocusTraversable(true);
		this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			// Quitting still works, the other shortcuts wait for the battle
			if (event.isControlDown() && event.getCode() == KeyCode.Q) {
				return;
			}
			switch (event.getCode()) {
			case V -> this.cycleView();
			case SPACE -> {
				this.paused = !this.paused;
				Sfx.blip();
			}
			case RIGHT, PLUS, EQUALS, ADD -> this.speed = Math.min(SPEEDS.length - 1, this.speed + 1);
			case LEFT, MINUS, SUBTRACT -> this.speed = Math.max(0, this.speed - 1);
			case ENTER -> this.skip(false);
			case ESCAPE -> this.skip(true);
			default -> {
			}
			}
			this.hud.updateInfo();
			// Nothing else happens during the battle (like declaring a winner by
			// accident)
			event.consume();
		});

		this.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			this.lastDragX = event.getX();
			this.lastDragY = event.getY();
			this.yawSpeed = 0;
			this.requestFocus();
		});
		this.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
			// Dragging takes the camera from the director
			if (this.viewMode == ViewMode.CINEMATIC) {
				this.setView(ViewMode.FREE);
			}
			if (this.viewMode != ViewMode.FREE) {
				return;
			}
			double dx = event.getX() - this.lastDragX;
			double dy = event.getY() - this.lastDragY;
			this.lastDragX = event.getX();
			this.lastDragY = event.getY();
			this.targetYaw += dx * 0.3;
			this.yawSpeed = dx * 0.3 * 60;
			this.freePitch = Math.max(-85, Math.min(-8, this.freePitch + dy * 0.2));
		});
		this.addEventFilter(ScrollEvent.SCROLL, event -> {
			this.targetZoom = Math.max(0.45, Math.min(2.5, this.targetZoom * (event.getDeltaY() > 0 ? 0.9 : 1.1)));
		});
	}

	private void cycleView() {
		this.setView(ViewMode.values()[(this.viewMode.ordinal() + 1) % ViewMode.values().length]);
		Sfx.blip();
	}

	private void setView(ViewMode mode) {
		if (mode == ViewMode.FREE && this.viewMode == ViewMode.CINEMATIC) {
			// Starts from where the director was
			this.targetYaw = this.yaw = Math.toDegrees(Math.atan2(this.look[0] - this.eye[0], this.look[2] - this.eye[2]));
		}
		this.viewMode = mode;
		this.targetYaw = switch (mode) {
		case ISOMETRIC -> Math.round((this.targetYaw - 45) / 90) * 90 + 45;
		case TOP_DOWN -> Math.round(this.targetYaw / 360) * 360;
		default -> this.targetYaw;
		};
		this.yawSpeed = 0;
		if (mode == ViewMode.CINEMATIC) {
			this.director.pickShot(true);
		}
		this.hud.updateInfo();
	}

	/**
	 * Enter: the next robot of the presentation, ends the replay, or the whole
	 * battle. Escape skips the whole presentation.
	 *
	 * @param all
	 *            skips the whole presentation
	 */
	private void skip(boolean all) {
		if (this.phase == Phase.SHOWCASE) {
			if (all || this.versus) {
				this.showcase.endShowcase();
			} else {
				this.showcase.showcaseRobot(this.showcaseOrder + 1);
			}
		} else if (this.phase == Phase.FLYBY) {
			this.rounds.endFlyBy();
		} else if (this.phase == Phase.FREEZE || this.phase == Phase.REPLAY) {
			this.showcase.endReplay();
		} else if (this.phase != Phase.LOADING && this.phase != Phase.FINISHED) {
			this.finish();
		}
	}

	void finish() {
		this.phase = Phase.FINISHED;
		this.timer.stop();
		this.stopShowcaseMusic();
		if (this.onFinished != null) {
			// The screen decides what comes next: the sound goes on until the arena
			// leaves the screen, or fades out with {@link #fadeOutAudio}
			Runnable callback = this.onFinished;
			this.onFinished = null;
			callback.run();
		} else {
			this.audio.stop();
		}
	}

	/**
	 * The sounds of the battle fade to silence
	 */
	public void fadeOutAudio(double seconds) {
		this.audio.fadeOut(seconds);
	}

	// ----------------------------------------------------------------- update

	private void update(double dt) {
		this.phaseTime += dt;

		switch (this.phase) {
		case LOADING -> {
		}
		case SHOWCASE -> {
			if (this.versus) {
				if (this.phaseTime > VERSUS_SECONDS) {
					this.showcase.endShowcase();
				}
			} else {
				this.showcase.turntable();
				if (this.phaseTime > SHOWCASE_SECONDS) {
					this.showcase.showcaseRobot(this.showcaseOrder + 1);
				}
			}
		}
		case FLYBY -> {
			if (this.phaseTime > this.rounds.flyByHold() + FLYBY_SECONDS) {
				this.rounds.endFlyBy();
			}
		}
		case INTRO -> this.rounds.updateIntro();
		case PLAYING -> this.rounds.updatePlaying(dt);
		case FREEZE -> {
			if (this.phaseTime > FREEZE_SECONDS) {
				this.showcase.startReplay();
			}
		}
		case REPLAY -> this.rounds.updateReplay(dt);
		case ROUND_END -> this.rounds.updateRoundEnd();
		case FINISHED -> {
		}
		}

		this.rounds.updateTension(dt);
		this.announcer.tick(dt);
		this.director.updateCamera(dt);
		this.clock += dt * this.effectRate;
		this.updateSurfaces();
		this.updateAudio(dt);
		this.updateEffects(dt);
	}

	/** The lava breathes and its flows slide */
	private void updateSurfaces() {
	if (this.lava != null) {
		this.lava.setDiffuseColor(LAVA_DIM.interpolate(LAVA_FULL, 0.5 + 0.5 * Math.sin(this.clock * 2.1) * Math.sin(this.clock * 0.7 + 1)));
	}
	if (!this.flows.isEmpty()) {
		this.updateFlows();
	}
	}

	/** The ears of the sound are the camera; each robot makes its machine sound */
	private void updateAudio(double dt) {
	// The ears are the camera: the sounds come from the side of the screen they are on
	this.audio.setListener(this.eye[0], this.eye[2], this.look[0] - this.eye[0], this.look[2] - this.eye[2]);
	for (Map.Entry<Integer, RobotView> entry : this.robots.entrySet()) {
		RobotView robotView = entry.getValue();
		robotView.roll(dt);
		double[] at = robotView.position();
		this.audio.setMachine(entry.getKey(), robotView.treadSpeed, robotView.turretSpeed, at[0], at[2]);
	}
	}

	/** The fire, the sunbeams, the neon and the ice, the critters, the balloons and the photos */
	private void updateEffects(double dt) {
	this.fire.update(dt * this.effectRate);
	this.jungle.updateSunbeams(dt);
	this.neon.update(dt);
	this.ice.update(dt);
	if (this.critters != null) {
		this.critters.update(dt);
	}
	this.director.placeBalloons();
	this.flushPhotos(dt);
	}

	/**
	 * The photos of the key moments taken so far, in order
	 */
	public List<BattlePhoto> photos() {
		return List.copyOf(this.photos);
	}

	/**
	 * Takes the photos that are due, at the end of the frame: the robots and the
	 * camera are then where they are drawn
	 */
	private void flushPhotos(double dt) {
		if (this.pendingPhotos.isEmpty()) {
			return;
		}
		List<PendingPhoto> waiting = new ArrayList<PendingPhoto>();
		for (PendingPhoto pending : this.pendingPhotos) {
			double remaining = pending.remaining() - dt;
			if (remaining > 0) {
				waiting.add(new PendingPhoto(remaining, pending.round(), pending.destruction(), pending.subject(), pending.power()));
			} else if (this.phase != Phase.FINISHED && this.phase != Phase.LOADING) {
				this.takePhoto(pending);
			}
		}
		this.pendingPhotos.clear();
		this.pendingPhotos.addAll(waiting);
	}

	private void takePhoto(PendingPhoto pending) {
		if (pending.subject() == null) {
			return;
		}
		try {
			this.photos.add(new BattlePhoto(pending.round(), pending.destruction(), pending.subject(), pending.power(), encodePhoto(this.subScene.snapshot(null, null))));
		} catch (IOException | RuntimeException e) {
			// A photo is a bonus: the battle goes on without it
			e.printStackTrace();
		}
	}

	/**
	 * The picture as a small JPEG: each of its pixels is the average of the ones it
	 * covers
	 */
	private static byte[] encodePhoto(WritableImage view) throws IOException {
		int width = (int) view.getWidth();
		int height = (int) view.getHeight();
		int[] pixels = new int[width * height];
		view.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
		int[] small = new int[PHOTO_WIDTH * PHOTO_HEIGHT];
		for (int y = 0; y < PHOTO_HEIGHT; y++) {
			int top = y * height / PHOTO_HEIGHT;
			int bottom = Math.max(top + 1, (y + 1) * height / PHOTO_HEIGHT);
			for (int x = 0; x < PHOTO_WIDTH; x++) {
				int left = x * width / PHOTO_WIDTH;
				int right = Math.max(left + 1, (x + 1) * width / PHOTO_WIDTH);
				int red = 0;
				int green = 0;
				int blue = 0;
				int count = 0;
				for (int sy = top; sy < bottom; sy++) {
					for (int sx = left; sx < right; sx++) {
						int argb = pixels[sy * width + sx];
						red += argb >> 16 & 0xff;
						green += argb >> 8 & 0xff;
						blue += argb & 0xff;
						count++;
					}
				}
				small[y * PHOTO_WIDTH + x] = (red / count) << 16 | (green / count) << 8 | blue / count;
			}
		}
		BufferedImage photo = new BufferedImage(PHOTO_WIDTH, PHOTO_HEIGHT, BufferedImage.TYPE_INT_RGB);
		photo.setRGB(0, 0, PHOTO_WIDTH, PHOTO_HEIGHT, small, 0, PHOTO_WIDTH);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (!ImageIO.write(photo, "jpg", out)) {
			throw new IOException("No JPEG writer");
		}
		return out.toByteArray();
	}

	List<Turn> turns() {
		return this.recording.getRounds().get(this.round);
	}

	/**
	 * Where a robot is drawn at a moment (in turns, with the fraction) and its
	 * heading, as showTurn places it
	 */
	static double[] pose(List<Turn> turns, int robot, double time) {
		int index = Math.max(0, Math.min((int) Math.floor(time), turns.size() - 1));
		double fraction = Math.max(0, Math.min(1, time - index));
		RobotState state = find(turns.get(index).robots(), robot, null);
		if (state == null) {
			return null;
		}
		RobotState after = find(turns.get(Math.min(index + 1, turns.size() - 1)).robots(), robot, state);
		return new double[] { lerp(state.x(), after.x(), fraction), lerp(state.y(), after.y(), fraction), lerpAngle(state.bodyHeading(), after.bodyHeading(), fraction) };
	}

	static Bullet findBullet(Turn turn, int id) {
		for (Bullet bullet : turn.bullets()) {
			if (bullet.id() == id) {
				return bullet;
			}
		}
		return null;
	}

	/**
	 * The whole flight of a bullet, from the muzzle of the gun to where it hits.
	 *
	 * @param born
	 *            the turn (and fraction) when it left the shooter
	 * @param duration
	 *            turns until it arrives
	 * @param hit
	 *            the record of the hit, null if it flies until the records end
	 */
	/**
	 * A photo of a key moment of the battle, as JPEG: a hit that hurts or a robot
	 * destroyed
	 *
	 * @param round
	 *            the round of the battle, from 1
	 * @param destruction
	 *            true when the subject was destroyed, false when it hit
	 * @param subject
	 *            the robot that fired the hit, or the robot destroyed
	 * @param power
	 *            the power of the hit, zero for a destruction
	 * @param jpeg
	 *            the picture of the 3D view
	 */
	public record BattlePhoto(int round, boolean destruction, Opponent subject, double power, byte[] jpeg) {
	}

	/** A photo waiting for its moment: the seconds left, then it is taken */
	record PendingPhoto(double remaining, int round, boolean destruction, Opponent subject, double power) {
	}

	static RobotState find(List<RobotState> states, int index, RobotState fallback) {
		for (RobotState state : states) {
			if (state.index() == index) {
				return state;
			}
		}
		return fallback;
	}

	/**
	 * A small picture of one color, for materials that shine by themselves
	 */
	static WritableImage solidMap(Color color) {
		WritableImage map = new WritableImage(2, 2);
		for (int y = 0; y < 2; y++) {
			for (int x = 0; x < 2; x++) {
				map.getPixelWriter().setColor(x, y, color);
			}
		}
		return map;
	}

	// ---------------------------------------------------------------- effects

	/** A crystal of light, built once: the flash of a shot, a spark */
	static final TriangleMesh CRYSTAL = spindleMesh(1, 1, 1, 4);

	private static TriangleMesh spindleMesh(double radius, double front, double back, int sides) {
		LowPoly shape = new LowPoly();
		shape.spindle(0, 0, 0, radius, front, back, sides);
		return shape.mesh;
	}

	/**
	 * A see through bubble: the color with a low opacity and no light of its own, so
	 * the arena shows through it. A light map would make it opaque.
	 */
	static PhongMaterial bubble(Color color) {
		Color light = color.interpolate(Color.WHITE, 0.5);
		PhongMaterial material = new PhongMaterial(Color.color(light.getRed(), light.getGreen(), light.getBlue(), 0.3));
		material.setSpecularColor(Color.color(1, 1, 1, 0.6));
		material.setSpecularPower(64);
		return material;
	}

	/**
	 * A see through material that shines in the color by itself
	 */
	static PhongMaterial energy(Color color, double opacity) {
		PhongMaterial material = new PhongMaterial(Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity));
		material.setSelfIlluminationMap(solidMap(color));
		return material;
	}

	static MeshView shape(TriangleMesh mesh, PhongMaterial material) {
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		return view;
	}

	static ScaleTransition grow(Node node, double from, double to, double millis) {
		ScaleTransition grow = new ScaleTransition(Duration.millis(millis), node);
		grow.setFromX(from);
		grow.setFromY(from);
		grow.setFromZ(from);
		grow.setToX(to);
		grow.setToY(to);
		grow.setToZ(to);
		grow.setInterpolator(Interpolator.EASE_OUT);
		return grow;
	}

	static ScaleTransition shrink(Node node, double delay, double millis) {
		ScaleTransition vanish = new ScaleTransition(Duration.millis(millis), node);
		vanish.setDelay(Duration.millis(delay));
		vanish.setToX(0);
		vanish.setToY(0);
		vanish.setToZ(0);
		return vanish;
	}

	void stopShowcaseMusic() {
		if (this.showcaseMusic != null) {
			this.showcaseMusic.stop();
			this.showcaseMusic = null;
		}
	}

	static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	/**
	 * Interpolates angles (radians) the short way
	 */
	static double lerpAngle(double a, double b, double t) {
		double difference = Math.IEEEremainder(b - a, Math.PI * 2);
		return a + difference * t;
	}

	// ------------------------------------------------------------------ robot

	/** Robots catch fire below this energy */
	static final double BURN_ENERGY = 50;
	/** Where the hull burns, in the body coordinates: engine, side, turret */
	static final double[][] BURN_SPOTS = { { 0, -15, -11 }, { 8, -15, 3 }, { -5, -23, 1 } };

	/**
	 * A robocode ARGB color, exactly as the robot defined it (black included), or
	 * the fallback if there is none
	 */
	static Color argb(int argb, Color fallback) {
		if (argb == 0) {
			return fallback;
		}
		return Color.rgb((argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff);
	}

	/**
	 * The material of a part painted in a color chosen by the robot. It is the
	 * color itself, but a dark paint (a black tank) still shows its shape: the
	 * light glints on it
	 */
	static PhongMaterial paint(Color color) {
		PhongMaterial material = new PhongMaterial(color);
		double floor = 0.38;
		material.setSpecularColor(Color.color(Math.max(floor, Math.min(1, color.getRed() * 1.3)), Math.max(floor, Math.min(1, color.getGreen() * 1.3)), Math.max(floor, Math.min(1, color.getBlue() * 1.3))));
		material.setSpecularPower(36);
		return material;
	}
}
