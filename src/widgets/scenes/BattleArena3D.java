package widgets.scenes;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.DepthTest;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.CullFace;
import javafx.scene.transform.Scale;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import model.Author;
import model.Opponent;
import tournament.Bracket;
import utils.configuration.Configuration;
import utils.robocode.BattleControl;
import utils.robocode.BattleRecording;
import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;
import widgets.BattleAudio;
import widgets.Sfx;
import widgets.Sound;
import widgets.Theme;

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

	/** Robocode's default speed, turns per second */
	private static final double TPS = 30;
	private static final double[] SPEEDS = Configuration.BATTLE_SPEEDS;

	/** The 3, 2, 1 before the fight, then the fight banner for a moment */
	/** The camera flies over the arena before the first round and ends where the battle starts */
	private static final double FLYBY_SECONDS = 6.5;
	/** The fly by starts looking at a critter for this long before it goes on */
	private static final double FLYBY_FOCUS_SECONDS = 2;
	private static final int COUNTDOWN_SECONDS = 3;
	private static final double INTRO_SECONDS = COUNTDOWN_SECONDS + 1.1;
	/** Each robot is presented alone before the battle, then the versus */
	private static final double SHOWCASE_SECONDS = 7;
	private static final double VERSUS_SECONDS = 2.6;
	private static final double ROUND_END_SECONDS = 2.8;
	/** The headline of a special battle, after the last round */
	private static final double HEADLINE_SECONDS = 3.4;
	/** How long a comment stays, and how long it may wait to be shown */
	private static final double COMMENT_SECONDS = 3.2;
	/**
	 * The commentary only shows up now and then, always with the presenter of the
	 * event: the more important a comment, the sooner after the last one it may
	 * come and the surer it is to come. The ones that do not pass are not told.
	 */
	private static final double[] COMMENT_COOLDOWN = { 15, 15, 12, 12, 8, 8, 8 };
	private static final double[] COMMENT_CHANCE = { 0, 0.5, 0.7, 0.7, 1, 1, 1 };
	private static final double COMMENT_MAX_WAIT = 4;
	/** Callouts shown at the same time on each side */
	private static final int MAX_CALLOUTS = 3;
	/** The action stops for a moment when a robot dies, then the replay */
	private static final double FREEZE_SECONDS = 1.3;
	/** The photos of the key moments are kept small, as JPEG */
	private static final int PHOTO_WIDTH = 640;
	private static final int PHOTO_HEIGHT = 360;
	/** A hit this strong (power) makes a photo: the first one of each round */
	private static final double PHOTO_HIT_POWER = 2.5;
	/** Seconds from a hit to its photo, so the flash of the impact has grown */
	private static final double PHOTO_HIT_DELAY = 0.2;
	/** Seconds from a destruction to its photo: the tank is falling, the battle is held (see freezeForReplay) */
	private static final double PHOTO_DESTRUCTION_DELAY = 1.0;
	/** Turns of the replay before and after the death */
	private static final int REPLAY_BEFORE = 30;
	private static final int REPLAY_AFTER = 10;
	/** Slow motion speeds: before the impact, close to it, after it */
	private static final double REPLAY_SPEED = 0.28;
	private static final double REPLAY_IMPACT_SPEED = 0.07;
	private static final double REPLAY_AFTER_SPEED = 0.22;

	private static final Color[] PLAYER_COLORS = { Theme.PLAYER_1, Theme.PLAYER_2, Color.web("#5af08f"), Color.web("#c86bff"), Color.web("#ff9f1c") };
	private static final Color FENCE_BASE = Color.color(0, 0, 0, 0);

	private enum ViewMode {
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
	private enum Shot {
		WIDE, CHASE, LOW_TRACK, CLOSE, CRANE, IMPACT, FATAL
	}

	private enum Phase {
		LOADING, SHOWCASE, FLYBY, INTRO, PLAYING, FREEZE, REPLAY, ROUND_END, FINISHED
	}

	private final List<Opponent> fighters;
	private Runnable onFinished;
	private final BattleAudio audio = new BattleAudio();
	private final Random random = new Random();

	// 3D
	private final Group world = new Group();
	final Group arena = new Group();
	private final Group effects = new Group();
	private final Group robotLayer = new Group();
	private final Group bulletLayer = new Group();
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
	private PhongMaterial lava;
	private final List<Flow> flows = new ArrayList<Flow>();
	private double clock = 0;
	/** Embers or snow still owed */
	private double ambientDebt = 0;
	/** Flame, spark and smoke sprites, sorted back to front every frame */
	private final Group fireLayer = new Group();
	/** Shafts of light through the canopy (jungle), over everything but the fire */
	private final Group sunbeams = new Group();
	private final List<PhongMaterial> beamMaterials = new ArrayList<PhongMaterial>();
	private final List<Double> beamPhases = new ArrayList<Double>();
	private final List<TriangleMesh> beamMeshes = new ArrayList<TriangleMesh>();
	/** x, z and width of each */
	private final List<double[]> beamSpots = new ArrayList<double[]>();
	private double beamYaw;
	private double beamClock;
	/** Lights of the neon arena that breathe, with their colors and phases */
	private static final double BEAM_ALPHA = 0.07;
	/** The city of the neon arena, its lights and cars */
	private final NeonArena neon = new NeonArena(this);
	private final IceArena ice = new IceArena(this);
	private final List<Particle> particles = new ArrayList<Particle>();
	private final List<Particle> particlePool = new ArrayList<Particle>();
	/** Translucent panels of the cage, each one with its own material to flash */
	private final List<FenceSegment> fenceSegments = new ArrayList<FenceSegment>();
	private final SubScene subScene;
	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final Translate cameraLook = new Translate();
	private final Rotate cameraYaw = new Rotate(0, Rotate.Y_AXIS);
	private final Rotate cameraPitch = new Rotate(-38, Rotate.X_AXIS);
	private final Rotate cameraRoll = new Rotate(0, Rotate.Z_AXIS);
	private final Translate cameraDistance = new Translate();
	/** Lights up the explosions */
	private final PointLight flashLight = new PointLight(Color.BLACK);
	/**
	 * Lights only the flames, so they glow. Its scope never gets empty, an empty
	 * scope would light everything.
	 */
	private final AmbientLight fireGlow = new AmbientLight(Color.WHITE);

	// Camera: what it sees now and what it wants to see, eased every frame
	private ViewMode viewMode = ViewMode.CINEMATIC;
	private final double[] eye = { 0, -700, -900 };
	private final double[] look = { 0, -10, 0 };
	private final double[] eyeTarget = { 0, -700, -900 };
	private final double[] lookTarget = { 0, -10, 0 };
	private double fov = 45;
	private double fovTarget = 45;
	private double roll = 0;
	private double rollTarget = 0;
	private double follow = 4;
	private double shake = 0;
	private double cinematicOrbitAngle = 0;

	// Free, isometric and top views
	private double yaw = 0;
	private double targetYaw = 0;
	private double freePitch = -38;
	private double pitch = -38;
	private double zoom = 1;
	private double targetZoom = 1;
	private double yawSpeed = 0;
	private double lastDragX;
	private double lastDragY;

	// Cinematic director
	private Shot shot = Shot.WIDE;
	private double shotTime = 0;
	private double shotDuration = 1000;
	private int shotSubject = 0;
	private double shotSide = 1;
	private double shotSeed = 0;
	/** How far the close shots are, the death is filmed from farther */
	private double closeDistance = 100;

	// 2D on top: balloons, HUD, banners and the replay frame
	private final Pane balloons = new Pane();
	private final Label banner = new Label();
	private final Label subBanner = new Label();
	private final Label loading = new Label("PREPARANDO A BATALHA");
	private final Label roundLabel = new Label();
	/** The stage of the championship, over the loading and the versus */
	private final Label stageLabel = new Label();
	private String stageName = "";
	private Bracket.Importance stageImportance = Bracket.Importance.REGULAR;
	private final Label speedLabel = new Label();
	private final Label viewLabel = new Label();
	private final HBox hudTop = new HBox();
	private final List<HudPlayer> hudPlayers = new ArrayList<HudPlayer>();
	private final Rectangle letterboxTop = new Rectangle();
	private final Rectangle letterboxBottom = new Rectangle();
	private final HBox replayTag = new HBox(14);
	/** The card of the robot being presented */
	private final StackPane showcaseLayer = new StackPane();
	private final Region whiteFlash = new Region();

	// Playback
	private BattleRecording recording;
	double fieldWidth = 800;
	private double fieldHeight = 600;
	private final Map<Integer, RobotView> robots = new HashMap<Integer, RobotView>();
	private final Map<Integer, Group> bullets = new HashMap<Integer, Group>();
	private final Set<Integer> bulletsSeen = new HashSet<Integer>();
	private final Set<Integer> bulletsExploded = new HashSet<Integer>();
	private final Set<Integer> lowEnergyWarned = new HashSet<Integer>();
	private final Map<Integer, Integer> roundWins = new HashMap<Integer, Integer>();
	private Phase phase = Phase.LOADING;
	private double phaseTime = 0;
	private int round = 0;
	private double turnPosition = 0;
	private int lastTurnProcessed = -1;
	/** Index in SPEEDS, starts with the configured one */
	private int speed;
	/** Shown when the round starts, configurable */
	private final String fightText = Configuration.getFightText();
	private boolean fightShown = false;
	/** Where the fly by ends: the camera at the start of the battle */
	private final double[] flyEyeEnd = new double[3];
	/** The critter the fly by starts looking at, where it is when the fly by begins, or null */
	private double[] flyFocus;
	/** Where the critter the fly by starts on is now, while the camera stays on it */
	private Supplier<double[]> flyFocusSource;
	/** The animals that live around the field */
	private Critters critters;
	/** The glacial sea of the ice arena: its middle and its radius, 0 when there is none */
	private final double[] flyLookEnd = new double[3];
	private double flyFovEnd;
	private double flyMirrorX = 1;
	private double flyMirrorZ = 1;
	/** The number of the countdown on the screen, 0 if none yet */
	private int countdown;
	private String roundTitle = "";
	private String roundSubtitle;
	private boolean paused = false;
	private double hitTension = 0;
	/** Animations of effects run slower in the replay */
	private double effectRate = 1;

	// The death being replayed
	private int deathTurn = -1;
	private int deadRobot = -1;
	private int replayFrom;
	private int replayTo;
	private int fatalBullet = -1;
	private int impactTurn = -1;
	private boolean replayed = false;
	// The photos of the key moments of the battle, see takePhoto
	private final List<BattlePhoto> photos = new ArrayList<BattlePhoto>();
	private final List<PendingPhoto> pendingPhotos = new ArrayList<PendingPhoto>();
	private boolean hitPhotoTaken = false;

	// Callouts and comments
	private BattleCommentary commentary;
	/** The presenter of the event being commented */
	private final Presenter presenter = new Presenter(190);
	private final Label liveTag = new Label("●");
	private final StackPane commentaryLayer = new StackPane();
	private final VBox calloutsLeft = new VBox(8);
	private final VBox calloutsRight = new VBox(8);
	private final VBox calloutsCenter = new VBox(8);
	private final HBox ticker = new HBox(14);
	private final Label commentText = new Label();
	private BattleCommentary.Message comment;
	private double commentAge;
	private double commentClock;
	/** When a presenter showed up for the last time */
	private double lastPresented = -1000;
	private final List<QueuedComment> commentQueue = new ArrayList<QueuedComment>();
	private BattleCommentary.Headline headline;
	private boolean headlineShown = false;

	private record QueuedComment(BattleCommentary.Message message, double since) {
	}

	// The presentation of the robots
	private int showcaseOrder = 0;
	private boolean versus = false;
	private Sound showcaseMusic;

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
		this.audio.setSong(Configuration.isArenaMusic() ? this.style.song : BattleAudio.Song.resolve(Configuration.getBattleMusic(), this.random));
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

		this.getChildren().setAll(this.subScene, this.balloons, this.buildLetterbox(), this.showcaseLayer, this.buildCommentary(), this.buildHud(), this.whiteFlash, scanlines);

		this.installControls();
		this.showBanner("", null);
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
			RobotView view = new RobotView(robot, this.opponentOf(robot), this.robots.size());
			this.robots.put(robot.getIndex(), view);
			this.robotLayer.getChildren().add(view.root);
			this.balloons.getChildren().add(view.balloon);
			this.roundWins.put(robot.getIndex(), 0);
		}
		this.buildHudPlayers();
		this.loading.setVisible(false);

		Map<Integer, String> names = new HashMap<Integer, String>();
		for (BattleRecording.Robot robot : recording.getRobots()) {
			names.put(robot.getIndex(), this.robots.get(robot.getIndex()).displayName);
		}
		this.commentary = new BattleCommentary(names, this.random);

		this.audio.start();
		this.startShowcase();
	}

	private Opponent opponentOf(BattleRecording.Robot robot) {
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
		case VOLCANO -> this.buildVolcano(w, h);
		case ICE -> this.ice.build(w, h);
		case JUNGLE -> this.buildJungle(w, h);
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
	private static final Color LAVA_DIM = Color.gray(0.72);
	private static final Color LAVA_FULL = Color.WHITE;
	private static final Image LAVA_TEXTURE = LavaTexture.create(0);
	/** Far away the lava has cooled, only its veins glow */
	private static final Image CRUST_TEXTURE = LavaTexture.create(0.45);
	/** Under the field of the battle the lava glows little, it shows in the cracks of the gray plates: the robots are what should stand out */
	private static final Image FIELD_TEXTURE = LavaTexture.floor(0.6, 0.5, 0x120403);
	/** Units of the world one tile of the lava texture covers */
	private static final double LAVA_TILE = 150;

	/**
	 * A surface of lava whose texture slides: its texture coordinates move every
	 * frame
	 */
	private record Flow(TriangleMesh mesh, float[] base, double du, double dv) {
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
	private void flow(double[][] corners, double du, double dv) {
		this.flow(corners, du, dv, this.lava, LAVA_TILE);
	}

	/**
	 * @param tile
	 *            units of the world one tile of the texture covers
	 */
	private void flow(double[][] corners, double du, double dv, PhongMaterial material, double tile) {
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
	private void flowStrip(double x1, double z1, double x2, double z2, double width, double y, double speed) {
		this.flowStrip(x1, z1, x2, z2, width, y, speed, this.lava, LAVA_TILE);
	}

	private void flowStrip(double x1, double z1, double x2, double z2, double width, double y, double speed, PhongMaterial material, double tile) {
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
	private void river(double fromX, double toX, double sign, double centre, double phase, double width, double speed, PhongMaterial material, double tile) {
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

	private static double distance2(double[] a, double[] b) {
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
	 * Basalt plates (Voronoi cells) with glowing lava in the cracks, a moat of
	 * lava around it and
	 * volcanoes on the horizon
	 */
	private void buildVolcano(double w, double h) {
		Random rocks = new Random(17);
		// A plateau of rock in a sea of lava
		double plateau = 380;
		Box ground = new Box(w + 2 * plateau, 2, h + 2 * plateau);
		ground.setMaterial(material(Color.web("#100706")));
		ground.setTranslateY(6);

		// Lava: its texture glows by itself (self illumination), with no light
		this.lava = new PhongMaterial(LAVA_FULL);
		this.lava.setDiffuseMap(LAVA_TEXTURE);
		this.lava.setSelfIlluminationMap(LAVA_TEXTURE);
		this.lava.setSpecularColor(Color.BLACK);

		PhongMaterial basalt = new PhongMaterial(Color.web("#4a4a52"));
		basalt.setSpecularColor(Color.web("#6a6a72"));
		basalt.setSpecularPower(10);
		Box floor = new Box(w + 40, 4, h + 40);
		floor.setMaterial(basalt);
		floor.setTranslateY(2);
		this.arena.getChildren().addAll(ground, floor);

		// Lava under the whole field, slowly moving, it shows in the cracks. It glows little, much less
		// than the moat and the sea, so the robots stand out against the field
		PhongMaterial fieldLava = new PhongMaterial(Color.WHITE);
		fieldLava.setDiffuseMap(FIELD_TEXTURE);
		fieldLava.setSelfIlluminationMap(FIELD_TEXTURE);
		fieldLava.setSpecularColor(Color.BLACK);
		this.flow(new double[][] { { -w / 2, -0.3, -h / 2 }, { w / 2, -0.3, -h / 2 }, { w / 2, -0.3, h / 2 }, { -w / 2, -0.3, h / 2 } }, 0.015, 0.025, fieldLava, LAVA_TILE);

		// Basalt plates over it: Voronoi cells of jittered seeds, each shrunk a
		// bit so the lava glows between them, in cracks of different widths
		double cell = 68;
		List<double[]> seeds = new ArrayList<double[]>();
		for (double x = cell / 2; x < w; x += cell) {
			for (double z = cell / 2; z < h; z += cell) {
				seeds.add(new double[] { x - w / 2 + (rocks.nextDouble() - 0.5) * cell * 0.9, z - h / 2 + (rocks.nextDouble() - 0.5) * cell * 0.9 });
			}
		}
		double[] gaps = new double[seeds.size()];
		for (int i = 0; i < gaps.length; i++) {
			gaps[i] = 0.9 + rocks.nextDouble() * rocks.nextDouble() * 2.6;
		}
		List<double[][]> cells = Voronoi.cells(seeds.toArray(new double[0][]), gaps, -w / 2, -h / 2, w / 2, h / 2);

		// One mesh for each tone of basalt
		String[] tones = { "#4a4a52", "#41414a", "#55555d" };
		TriangleMesh[] plates = new TriangleMesh[tones.length];
		for (int i = 0; i < plates.length; i++) {
			plates[i] = new TriangleMesh();
			plates[i].getTexCoords().addAll(0, 0);
		}
		for (double[][] polygon : cells) {
			if (polygon.length < 3) {
				continue;
			}
			TriangleMesh mesh = plates[rocks.nextInt(plates.length)];
			int first = mesh.getPoints().size() / 3;
			for (double[] point : polygon) {
				mesh.getPoints().addAll((float) point[0], -0.6f, (float) point[1]);
			}
			// A fan, so the normal points up (to -Y)
			for (int k = 1; k + 1 < polygon.length; k++) {
				mesh.getFaces().addAll(first, 0, first + k + 1, 0, first + k, 0);
			}
		}
		for (int i = 0; i < plates.length; i++) {
			PhongMaterial basaltTone = new PhongMaterial(Color.web(tones[i]));
			basaltTone.setSpecularColor(Color.web("#6a6a72"));
			basaltTone.setSpecularPower(10);
			MeshView view = new MeshView(plates[i]);
			view.setMaterial(basaltTone);
			view.setCullFace(CullFace.NONE);
			this.arena.getChildren().add(view);
		}

		PhongMaterial crack = new PhongMaterial(Color.web("#a8380e"));
		crack.setSpecularColor(Color.web("#7a4a30"));

		// The field border
		this.glowing.getChildren().addAll(box(w, 0.5, 2, 0, -0.3, -h / 2, crack), box(w, 0.5, 2, 0, -0.3, h / 2, crack), box(2, 0.5, h, -w / 2, -0.3, 0, crack),
				box(2, 0.5, h, w / 2, -0.3, 0, crack));

		// A moat of lava, a bit lower than the floor
		double edge = 20;
		double moat = 70;
		// It flows around the field, clockwise seen from above
		double outerX = w / 2 + edge + moat;
		double outerZ = h / 2 + edge + moat;
		double middleX = w / 2 + edge + moat / 2;
		double middleZ = h / 2 + edge + moat / 2;
		this.flowStrip(-outerX, -middleZ, outerX, -middleZ, moat, 3, 0.18);
		this.flowStrip(middleX, -outerZ, middleX, outerZ, moat, 3, 0.18);
		this.flowStrip(outerX, middleZ, -outerX, middleZ, moat, 3, 0.18);
		this.flowStrip(-middleX, outerZ, -middleX, -outerZ, moat, 3, 0.18);

		// Rivers from the moat to the sea, with a bend
		double[][] rivers = { { -w / 4, -1, 0 }, { w / 3, 1, 0 }, { h / 5, 1, 1 }, { -h / 4, -1, 1 } };
		for (double[] river : rivers) {
			boolean alongX = river[2] == 1;
			double side = river[1];
			double start = (alongX ? outerX : outerZ) - 4;
			double end = (alongX ? w : h) / 2 + plateau + 20;
			double middle = (start + end) / 2;
			double bend = river[0] + (rocks.nextDouble() - 0.5) * 140;
			double[][] points = { { river[0], start }, { bend, middle }, { bend + (rocks.nextDouble() - 0.5) * 120, end } };
			for (int k = 0; k + 1 < points.length; k++) {
				double[] a = points[k];
				double[] b = points[k + 1];
				if (alongX) {
					this.flowStrip(side * a[1], a[0], side * b[1], b[0], 34, 4.6, 0.35);
				} else {
					this.flowStrip(a[0], side * a[1], b[0], side * b[1], 34, 4.6, 0.35);
				}
			}
		}

		// The sea of lava, drifting, with a cooler crust: bright, it would steal the
		// show and shimmer far away
		PhongMaterial crust = new PhongMaterial(Color.WHITE);
		crust.setDiffuseMap(CRUST_TEXTURE);
		crust.setSelfIlluminationMap(CRUST_TEXTURE);
		crust.setSpecularColor(Color.BLACK);
		double sea = 5000;
		this.flow(new double[][] { { -sea, 7, -sea }, { sea, 7, -sea }, { sea, 7, sea }, { -sea, 7, sea } }, 0.012, 0.007, crust, 420);

		// Volcanoes on the horizon: stacked tiers make a cone, glowing on the top
		PhongMaterial rock = new PhongMaterial(Color.web("#1c110e"));
		rock.setSpecularColor(Color.web("#2c1a14"));
		for (int i = 0; i < 9; i++) {
			double angle = i * Math.PI * 2 / 9 + rocks.nextDouble() * 0.4;
			double distance = 1400 + rocks.nextDouble() * 700;
			double height = 220 + rocks.nextDouble() * 420;
			double radius = 110 + rocks.nextDouble() * 130;
			double x = Math.cos(angle) * distance;
			double z = Math.sin(angle) * distance;
			int tiers = 8;
			for (int tier = 0; tier < tiers; tier++) {
				Cylinder part = new Cylinder(radius * (1 - tier * 0.1), height / tiers, 9);
				part.setMaterial(rock);
				part.setTranslateX(x);
				part.setTranslateZ(z);
				part.setTranslateY(5 - height / tiers * (tier + 0.5));
				this.arena.getChildren().add(part);
			}
			// Lava falls down the slopes, a bit off them so the tiers do not cover them
			for (int fall = 0; fall < 3; fall++) {
				double side = (fall + rocks.nextDouble() * 0.6) * Math.PI * 2 / 3;
				double dx = Math.cos(side);
				double dz = Math.sin(side);
				double top = radius * 0.3 * 1.12;
				double bottom = radius * 1.12;
				double topWidth = radius * 0.15;
				double bottomWidth = radius * 0.42;
				this.flow(new double[][] { //
						{ x + dx * top - dz * topWidth, 5 - height, z + dz * top + dx * topWidth }, //
						{ x + dx * top + dz * topWidth, 5 - height, z + dz * top - dx * topWidth }, //
						{ x + dx * bottom + dz * bottomWidth, 5, z + dz * bottom - dx * bottomWidth }, //
						{ x + dx * bottom - dz * bottomWidth, 5, z + dz * bottom + dx * bottomWidth } }, 0, -0.4);
			}
			Cylinder crater = new Cylinder(radius * 0.3, 4, 7);
			crater.setMaterial(this.lava);
			crater.setTranslateX(x);
			crater.setTranslateZ(z);
			crater.setTranslateY(5 - height - 1);
			this.glowing.getChildren().add(crater);
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
	 * Crepuscular rays: pale shafts of light slanting through the canopy. Each is
	 * one soft strip that turns around its own axis to face the camera, so they
	 * have no edges and cost little. They do not touch the depth buffer (they are
	 * only a faint haze), and breathe slowly.
	 */
	private void buildSunbeams(double w, double h, Random random) {
		this.sunbeams.setDepthTest(DepthTest.DISABLE);
		this.sunbeams.setMouseTransparent(true);
		for (int i = 0; i < 9; i++) {
			TriangleMesh mesh = new TriangleMesh();
			mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
			mesh.getPoints().addAll(new float[12]);
			mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
			PhongMaterial material = new PhongMaterial(Color.color(1, 1, 1, BEAM_ALPHA));
			material.setDiffuseMap(JungleTexture.BEAM);
			MeshView view = new MeshView(mesh);
			view.setMaterial(material);
			view.setCullFace(CullFace.NONE);
			this.sunbeams.getChildren().add(view);
			this.beamMeshes.add(mesh);
			this.beamMaterials.add(material);
			this.beamSpots.add(new double[] { (random.nextDouble() - 0.5) * (w + 1400), (random.nextDouble() - 0.5) * (h + 1400), 110 + random.nextDouble() * 150 });
			this.beamPhases.add(random.nextDouble() * Math.PI * 2);
		}
		this.beamYaw = Double.NaN;
		this.fireGlow.getScope().add(this.sunbeams);
	}

	/**
	 * The shafts of light slowly get stronger and weaker, out of step, and turn to
	 * the camera
	 */
	private void updateSunbeams(double dt) {
		if (this.beamMaterials.isEmpty()) {
			return;
		}
		this.beamClock += dt;
		// A strip that fills the screen is slow to draw, and a haze you are inside of
		// is not seen as a shaft anyway: they fade away as the camera comes close
		Point3D eye = this.camera.localToScene(Point3D.ZERO);
		double top = -1150;
		double bottom = 6;
		double height = bottom - top;
		double along = Math.max(0, Math.min(1, (eye.getY() - top) / height));
		for (int i = 0; i < this.beamMaterials.size(); i++) {
			double[] spot = this.beamSpots.get(i);
			double near = Math.hypot(eye.getX() - (spot[0] + height * 0.38 * along), eye.getZ() - (spot[1] + height * 0.22 * along));
			double away = Math.max(0, Math.min(1, (near - 250) / 700));
			away = away * away * (3 - 2 * away);
			this.sunbeams.getChildren().get(i).setVisible(away > 0.01);
			double breath = 0.5 + 0.5 * Math.sin(this.beamClock * 0.35 + this.beamPhases.get(i));
			this.beamMaterials.get(i).setDiffuseColor(Color.color(1, 1, 1, BEAM_ALPHA * (0.45 + 0.55 * breath) * away));
		}
		double yaw = this.cameraYaw.getAngle();
		if (yaw == this.beamYaw) {
			return;
		}
		this.beamYaw = yaw;
		// The light comes down slanted, from where the key light is
		double dx = height * 0.38;
		double dz = height * 0.22;
		double right = Math.toRadians(yaw);
		for (int i = 0; i < this.beamMeshes.size(); i++) {
			double[] spot = this.beamSpots.get(i);
			double ex = Math.cos(right) * spot[2] / 2;
			double ez = -Math.sin(right) * spot[2] / 2;
			this.beamMeshes.get(i).getPoints().setAll((float) (spot[0] - ex), (float) top, (float) (spot[1] - ez), (float) (spot[0] + ex), (float) top, (float) (spot[1] + ez),
					(float) (spot[0] + ex + dx), (float) bottom, (float) (spot[1] + ez + dz), (float) (spot[0] - ex + dx), (float) bottom, (float) (spot[1] - ez + dz));
		}
	}

	/**
	 * Mossy ground, rivers on both sides, huge trees in the distance, ferns,
	 * rocks and waterfalls. Fireflies float in the air and shafts of light cross the canopy.
	 */
	private void buildJungle(double w, double h) {
		Random wild = new Random(31);
		PhongMaterial moss = textured(JungleTexture.MOSS, Color.gray(0.05));
		// The relief of the water makes the sun glint on it
		PhongMaterial water = textured(JungleTexture.WATER, Color.web("#d8fff4"));
		water.setBumpMap(JungleTexture.WATER_RELIEF);
		water.setSpecularPower(70);
		PhongMaterial waterfall = textured(JungleTexture.WATERFALL, Color.web("#d8fff4"));
		waterfall.setBumpMap(JungleTexture.WATERFALL_RELIEF);
		waterfall.setSpecularPower(50);
		PhongMaterial bark = textured(JungleTexture.BARK, Color.gray(0.05));
		PhongMaterial leaves = textured(JungleTexture.LEAVES, Color.gray(0.1));

		// The sky: a green dome seen from inside
		this.arena.getChildren().add(skyDome(JungleTexture.SKY));
		// The dense forest all around, painted once
		this.arena.getChildren().add(ForestBackdrop.ring());
		this.buildSunbeams(w, h, wild);

		// Ground: moss everywhere, a bit darker far away (it is the same texture,
		// bigger tiles)
		double far = 5000;
		this.arena.getChildren().add(this.tiledQuad(new double[][] { { -far, 6, -far }, { far, 6, -far }, { far, 6, far }, { -far, 6, far } }, moss, 260));
		this.arena.getChildren().add(this.tiledQuad(new double[][] { { -w / 2 - 20, 0.2, -h / 2 - 20 }, { w / 2 + 20, 0.2, -h / 2 - 20 }, { w / 2 + 20, 0.2, h / 2 + 20 }, { -w / 2 - 20, 0.2, h / 2 + 20 } }, moss, 110));

		// A faint grid of lighter moss and a sandy border, so the field reads
		PhongMaterial line = material(Color.web("#4f8a3c"));
		for (double x = 100; x < w; x += 100) {
			this.arena.getChildren().add(box(1.2, 0.4, h, x - w / 2, -0.2, 0, line));
		}
		for (double y = 100; y < h; y += 100) {
			this.arena.getChildren().add(box(w, 0.4, 1.2, 0, -0.2, y - h / 2, line));
		}
		PhongMaterial sand = material(Color.web("#d6c58a"));
		this.arena.getChildren().addAll(box(w + 4, 0.6, 3, 0, -0.3, -h / 2, sand), box(w + 4, 0.6, 3, 0, -0.3, h / 2, sand), box(3, 0.6, h, -w / 2, -0.3, 0, sand),
				box(3, 0.6, h, w / 2, -0.3, 0, sand));

		// Rivers along the long sides, flowing in opposite directions
		double riverZ = h / 2 + 160;
		double riverWidth = 96;
		this.river(-3000, 3000, -1, riverZ, 0, riverWidth, 0.16, water, 120);
		this.river(3000, -3000, 1, riverZ, 2.3, riverWidth, 0.16, water, 120);

		// Waterfalls far at both ends, falling from a cliff to a pool
		PhongMaterial rock = textured(JungleTexture.BARK, Color.gray(0.1));
		for (int side = -1; side <= 1; side += 2) {
			double x = side * (w / 2 + 1000);
			double z = (wild.nextDouble() - 0.5) * 300;
			// The cliff, its face to the field at x
			this.rockMountain(x, z, side, wild);
			// The water in front of the face, not on it (two surfaces in the same
			// place flicker)
			double front = x - side * 2.5;
			this.flow(new double[][] { { front, 6 - 450, z - 70 }, { front, 6 - 450, z + 70 }, { front, 5, z + 70 }, { front, 5, z - 70 } }, 0, -0.9, waterfall, 60);
			// The pool at its foot, toward the field
			double poolEnd = x - side * 240;
			double[][] pool = { { x - side * 2.5, 3, z - 150 }, { poolEnd, 3, z - 150 }, { poolEnd, 3, z + 150 }, { x - side * 2.5, 3, z + 150 } };
			this.flow(pool, 0.025, 0.018, water, 120);
		}

		// Rocks covered with moss between the field and the rivers
		for (int i = 0; i < 16; i++) {
			double radius = 16 + wild.nextDouble() * 24;
			double x = (wild.nextBoolean() ? 1 : -1) * (w / 2 + 40 + wild.nextDouble() * 160);
			double z = (wild.nextDouble() - 0.5) * (h + 100);
			Sphere stone = new Sphere(radius, 8);
			stone.setMaterial(i % 3 == 0 ? moss : rock);
			stone.setTranslateX(x);
			stone.setTranslateZ(z);
			stone.setTranslateY(6 - radius * 0.35);
			stone.setScaleY(0.6);
			this.arena.getChildren().add(stone);
		}

		// Ferns: all the blades in a few meshes, three tones of green
		String[] tones = { "#2f8f2a", "#3da83a", "#25782a" };
		TriangleMesh[] ferns = new TriangleMesh[tones.length];
		for (int i = 0; i < ferns.length; i++) {
			ferns[i] = new TriangleMesh();
			ferns[i].getTexCoords().addAll(0, 0);
		}
		for (int cluster = 0; cluster < 170; cluster++) {
			double x;
			double z;
			do {
				x = (wild.nextDouble() - 0.5) * (w + 1700);
				z = (wild.nextDouble() - 0.5) * (h + 1300);
				// Not in the field, not in the rivers
			} while (Math.abs(x) < w / 2 + 25 && Math.abs(z) < h / 2 + 25 || Math.abs(Math.abs(z) - riverZ) < riverWidth / 2 + 14 + 80);
			this.fern(ferns[wild.nextInt(ferns.length)], x, z, wild);
		}
		for (int i = 0; i < ferns.length; i++) {
			PhongMaterial tone = new PhongMaterial(Color.web(tones[i]));
			tone.setSpecularColor(Color.gray(0.15));
			MeshView view = new MeshView(ferns[i]);
			view.setMaterial(tone);
			view.setCullFace(CullFace.NONE);
			this.arena.getChildren().add(view);
		}

		// Huge trees far away, grown by a fractal rule: a crooked trunk that forks
		// into branches that fork again, with clumps of leaves at the tips
		Forest forest = new Forest();
		for (int i = 0; i < 32; i++) {
			double x;
			double z;
			do {
				double angle = wild.nextDouble() * Math.PI * 2;
				double distance = 950 + wild.nextDouble() * 1100;
				x = Math.cos(angle) * distance * 1.15;
				z = Math.sin(angle) * distance;
				// Not over the rivers
			} while (Math.abs(Math.abs(z) - riverZ) < riverWidth / 2 + 90);
			forest.addTree(x, 6, z, 0.85 + wild.nextDouble() * 0.5, wild);
		}
		MeshView trunks = new MeshView(forest.bark);
		trunks.setMaterial(bark);
		trunks.setCullFace(CullFace.NONE);
		MeshView crowns = new MeshView(forest.leaves);
		crowns.setMaterial(leaves);
		crowns.setCullFace(CullFace.BACK);
		this.arena.getChildren().addAll(trunks, crowns);
	}

	/**
	 * The mountain the waterfall falls from: tiers of irregular rocks, higher in
	 * the middle, with buttresses and ledges on both sides of the water, a rock
	 * lip over it, boulders at its foot and moss on the tops. The water is a flat
	 * strip in front of x, so the rocks behind it stay behind x.
	 *
	 * @param side
	 *            1 or -1: which end of the arena, the mountain goes away from the
	 *            field
	 */
	private void rockMountain(double x, double z, int side, Random wild) {
		Color[] tones = { Color.web("#4b4a45"), Color.web("#5d5850"), Color.web("#3f4640"), Color.web("#6b6358") };
		LowPoly[] stones = new LowPoly[tones.length];
		for (int i = 0; i < stones.length; i++) {
			stones[i] = new LowPoly();
		}
		LowPoly moss = new LowPoly();
		double toward = -side;

		// The body: tiers side by side, tall behind the water and lower at the sides
		for (int i = 0; i < 10; i++) {
			double dz = -250 + i * 55 + (wild.nextDouble() - 0.5) * 30;
			boolean channel = Math.abs(dz) < 105;
			double rx = 55 + wild.nextDouble() * 35;
			double rz = 55 + wild.nextDouble() * 30;
			double height = channel ? 440 + wild.nextDouble() * 70 : 360 - Math.abs(dz) * 0.5 + wild.nextDouble() * 120;
			// Behind the water in the middle, sticking out toward the field at the sides
			double jut = channel ? 0 : wild.nextDouble() * 45;
			double cx = x + side * (rx * 1.25 + 4) + toward * jut;
			stones[wild.nextInt(stones.length)].rock(cx, z + dz, 6, height, rx, rz, 7, 0.45 + wild.nextDouble() * 0.3, 0.6, wild);
			if (!channel) {
				// A ledge on top of it
				double upperHeight = 90 + wild.nextDouble() * 110;
				stones[wild.nextInt(stones.length)].rock(cx + side * 10, z + dz, 6 - height * 0.72, upperHeight, rx * 0.65, rz * 0.65, 6, 0.55, 0.55, wild);
				moss.rock(cx, z + dz, 6 - height * 0.88, 7, rx * 0.5, rz * 0.5, 6, 0.8, 0.4, wild);
			}
		}

		// Buttresses: rocks coming out of the mountain to the sides of the water
		for (int i = 0; i < 8; i++) {
			double dz = (i % 2 == 0 ? 1 : -1) * (100 + wild.nextDouble() * 130);
			double rx = 30 + wild.nextDouble() * 30;
			double rz = 28 + wild.nextDouble() * 24;
			double height = 90 + wild.nextDouble() * 200;
			double cx = x + toward * (12 + wild.nextDouble() * 50);
			stones[wild.nextInt(stones.length)].rock(cx, z + dz, 6, height, rx, rz, 7, 0.4 + wild.nextDouble() * 0.3, 0.6, wild);
			if (wild.nextDouble() < 0.5) {
				moss.rock(cx, z + dz, 6 - height * 0.85, 6, rx * 0.5, rz * 0.5, 6, 0.8, 0.4, wild);
			}
		}

		// A lip of rock over the water: it comes out from under it
		stones[1].rock(x + toward * 16, z, 6 - 400, 80, 70, 100, 8, 0.75, 0.45, wild);
		stones[3].rock(x + toward * 6, z + 55, 6 - 380, 70, 40, 45, 6, 0.7, 0.5, wild);
		stones[0].rock(x + toward * 6, z - 55, 6 - 385, 70, 40, 45, 6, 0.7, 0.5, wild);

		// Boulders at the foot, on both sides of the pool
		for (int i = 0; i < 12; i++) {
			double dz = (wild.nextBoolean() ? 1 : -1) * (95 + wild.nextDouble() * 140);
			double radius = 16 + wild.nextDouble() * 28;
			double cx = x + toward * (20 + wild.nextDouble() * 230);
			stones[wild.nextInt(stones.length)].rock(cx, z + dz, 6, radius * (0.8 + wild.nextDouble() * 0.8), radius, radius * 0.9, 6, 0.6, 0.55, wild);
		}
		// Stones in the water, at the edge of the pool
		for (int i = 0; i < 4; i++) {
			double radius = 10 + wild.nextDouble() * 12;
			stones[wild.nextInt(stones.length)].rock(x + toward * (60 + wild.nextDouble() * 170), z + (wild.nextDouble() - 0.5) * 220, 6, radius, radius, radius, 6, 0.6, 0.5, wild);
		}

		for (int i = 0; i < stones.length; i++) {
			PhongMaterial material = new PhongMaterial(tones[i]);
			material.setSpecularColor(Color.gray(0.08));
			material.setSpecularPower(14);
			this.arena.getChildren().add(stones[i].view(material));
		}
		PhongMaterial mossy = new PhongMaterial(Color.web("#2f5a1f"));
		mossy.setSpecularColor(Color.gray(0.05));
		this.arena.getChildren().add(moss.view(mossy));
	}

	/**
	 * A fern: blades that rise from the ground and bend outwards, added to a mesh
	 */
	private void fern(TriangleMesh mesh, double x, double z, Random random) {
		int blades = 8 + random.nextInt(4);
		int segments = 4;
		double scale = 0.8 + random.nextDouble() * 0.8;
		for (int blade = 0; blade < blades; blade++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double length = (30 + random.nextDouble() * 22) * scale;
			double lift = (18 + random.nextDouble() * 14) * scale;
			double width = (3.5 + random.nextDouble() * 2) * scale;
			double sideX = -Math.sin(angle);
			double sideZ = Math.cos(angle);
			int first = mesh.getPoints().size() / 3;
			for (int segment = 0; segment <= segments; segment++) {
				double t = (double) segment / segments;
				double reach = length * t;
				// Rises, then bends down at the tip
				double y = 6 - lift * Math.sin(t * Math.PI * 0.8);
				double half = width * (1 - 0.85 * t);
				double cx = x + Math.cos(angle) * reach;
				double cz = z + Math.sin(angle) * reach;
				mesh.getPoints().addAll((float) (cx - sideX * half), (float) y, (float) (cz - sideZ * half), (float) (cx + sideX * half), (float) y, (float) (cz + sideZ * half));
			}
			for (int segment = 0; segment < segments; segment++) {
				int a = first + 2 * segment;
				mesh.getFaces().addAll(a, 0, a + 1, 0, a + 3, 0, a, 0, a + 3, 0, a + 2, 0);
			}
		}
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
	private static class FenceSegment {
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
	private double worldX(double x) {
		return x - this.fieldWidth / 2;
	}

	private double worldZ(double y) {
		return y - this.fieldHeight / 2;
	}

	// ------------------------------------------------------------------- HUD

	/** A health bar: the energy, the light trail of the damage just taken, the number and the won rounds */
	private record HudPlayer(int index, Region energy, Region lag, Label energyText, HBox pips) {
	}

	/** Width of the health bars */
	private static final double HUD_BAR_WIDTH = 420;
	/** The damage trails draining, one per player */
	private final Map<Integer, Timeline> lagDrains = new HashMap<>();

	private Node buildHud() {
		this.banner.getStyleClass().add("arena-banner");
		this.subBanner.getStyleClass().add("arena-sub-banner");
		this.stageLabel.setVisible(false);
		this.stageLabel.setManaged(false);
		VBox center = new VBox(10, this.stageLabel, this.loading, this.banner, this.subBanner);
		center.setAlignment(Pos.CENTER);
		center.setMouseTransparent(true);

		this.roundLabel.getStyleClass().add("arena-round");
		this.hudTop.getChildren().add(this.roundLabel);
		this.hudTop.setAlignment(Pos.TOP_CENTER);
		this.hudTop.setPadding(new Insets(18, 24, 0, 24));

		this.speedLabel.getStyleClass().add("arena-info");
		this.viewLabel.getStyleClass().add("arena-info");
		Label help = new Label("V: CÂMERA • ESPAÇO: PAUSA • ← →: VELOCIDADE • ENTER: PULAR • ARRASTE: GIRAR • RODA: ZOOM");
		help.getStyleClass().add("arena-help");
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox bottom = new HBox(18, help, spacer, this.viewLabel, this.speedLabel);
		bottom.setAlignment(Pos.CENTER_LEFT);
		bottom.setPadding(new Insets(0, 24, 16, 24));

		BorderPane hud = new BorderPane(center, this.hudTop, null, bottom, null);
		hud.setPickOnBounds(false);
		this.updateInfo();
		return hud;
	}

	/**
	 * Black bars of the replay, like a movie, with its tag
	 */
	private Node buildLetterbox() {
		for (Rectangle bar : List.of(this.letterboxTop, this.letterboxBottom)) {
			bar.setFill(Color.BLACK);
			bar.widthProperty().bind(this.widthProperty());
			bar.setHeight(0);
			bar.setManaged(false);
		}
		this.letterboxBottom.layoutYProperty().bind(this.heightProperty().subtract(this.letterboxBottom.heightProperty()));

		Label dot = new Label("●");
		dot.getStyleClass().add("arena-replay-dot");
		Theme.blink(dot);
		Label replay = new Label("REPLAY");
		replay.getStyleClass().add("arena-replay");
		Label slow = new Label("CÂMERA LENTA");
		slow.getStyleClass().add("arena-replay-slow");
		this.replayTag.getChildren().addAll(dot, replay, slow);
		this.replayTag.setAlignment(Pos.CENTER_LEFT);
		// Managed, so the pane gives it its size (unmanaged, the texts were cut)
		this.replayTag.relocate(36, 26);
		this.replayTag.setVisible(false);

		Pane frame = new Pane(this.letterboxTop, this.letterboxBottom, this.replayTag);
		frame.setMouseTransparent(true);
		return frame;
	}

	private void showLetterbox(boolean show) {
		this.showLetterbox(show, show);
	}

	/**
	 * @param tag
	 *            shows the replay tag
	 */
	private void showLetterbox(boolean show, boolean tag) {
		double height = show ? this.getHeight() * 0.12 : 0;
		Timeline bars = new Timeline(new KeyFrame(Duration.millis(450), new KeyValue(this.letterboxTop.heightProperty(), height, Interpolator.EASE_BOTH),
				new KeyValue(this.letterboxBottom.heightProperty(), height, Interpolator.EASE_BOTH)));
		bars.play();
		this.replayTag.setVisible(tag);
		this.hudTop.setVisible(!show);
	}

	/**
	 * Fighting game health bars, P1 on the left and P2 on the right
	 */
	private void buildHudPlayers() {
		this.hudTop.getChildren().clear();

		List<BattleRecording.Robot> list = this.recording.getRobots();
		for (int i = 0; i < list.size() && i < 2; i++) {
			BattleRecording.Robot robot = list.get(i);
			Opponent opponent = this.opponentOf(robot);
			Color color = PLAYER_COLORS[i];

			Label name = new Label(opponent == null ? robot.getName() : opponent.getName().toUpperCase());
			name.getStyleClass().add("arena-player-name");
			name.setTextFill(color);
			Label team = new Label(opponent == null ? "" : opponent.getTeam());
			team.getStyleClass().add("arena-player-team");

			// A flat futuristic bar: slanted at its outer end, solid colors, a thin outline, cells
			// every 5 %, and a light trail that shows the damage just taken before it drains away
			double height = 14;
			double slant = 8;
			String css = toCss(color);
			Region back = new Region();
			back.setStyle("-fx-background-color: derive(" + css + ", -80%);");
			Region lag = new Region();
			lag.getStyleClass().add("arena-energy-lag");
			Region energy = new Region();
			energy.getStyleClass().add("arena-energy");
			energy.setStyle("-fx-background-color: " + css + ";");
			Region cells = new Region();
			cells.getStyleClass().add("arena-energy-cells");
			StackPane bar = new StackPane(back, lag, energy, cells);
			bar.setMinSize(HUD_BAR_WIDTH, height);
			bar.setPrefSize(HUD_BAR_WIDTH, height);
			bar.setMaxSize(HUD_BAR_WIDTH, height);
			Pos anchor = i == 0 ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;
			StackPane.setAlignment(lag, anchor);
			StackPane.setAlignment(energy, anchor);
			double[] corners = i == 0 ? new double[] { slant, 0, HUD_BAR_WIDTH, 0, HUD_BAR_WIDTH, height, 0, height }
					: new double[] { 0, 0, HUD_BAR_WIDTH - slant, 0, HUD_BAR_WIDTH, height, 0, height };
			bar.setClip(new Polygon(corners));
			Polygon outline = new Polygon(corners);
			outline.setFill(Color.TRANSPARENT);
			outline.setStroke(color);
			outline.setStrokeWidth(1.5);
			outline.setMouseTransparent(true);
			StackPane frame = new StackPane(bar, outline);
			Label energyText = new Label("100");
			energyText.getStyleClass().add("arena-energy-text");
			energyText.setStyle("-fx-text-fill: " + css + "; -fx-border-color: " + css + ";");
			HBox pips = new HBox(6);

			HBox nameRow = i == 0 ? new HBox(10, name, team) : new HBox(10, team, name);
			nameRow.setAlignment(i == 0 ? Pos.BOTTOM_LEFT : Pos.BOTTOM_RIGHT);
			HBox barRow = i == 0 ? new HBox(10, energyText, frame) : new HBox(10, frame, energyText);
			barRow.setAlignment(Pos.CENTER);
			VBox panel = new VBox(6, nameRow, barRow, pips);
			panel.setAlignment(i == 0 ? Pos.TOP_LEFT : Pos.TOP_RIGHT);

			this.hudPlayers.add(new HudPlayer(robot.getIndex(), energy, lag, energyText, pips));
			if (i == 1) {
				this.hudTop.getChildren().addAll(spacer(), this.roundLabel, spacer());
			}
			this.hudTop.getChildren().add(panel);
		}
		if (list.size() < 2) {
			this.hudTop.getChildren().add(this.roundLabel);
		}
		this.updatePips();
	}

	private static Region spacer() {
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		return spacer;
	}

	/**
	 * How many rounds the robots play for: all of the recording in a battle by
	 * points, the rounds to win in a knockout (best of 5, 3), which may end earlier
	 */
	private int roundsToWin() {
		int rounds = this.recording == null ? 0 : this.recording.getRounds().size();
		if (!Configuration.getWinnerCriterion().isKnockout()) {
			return rounds;
		}
		return Math.max(1, BattleControl.roundsToWin(Configuration.getRobocodeRounds()));
	}

	private void updatePips() {
		int rounds = this.roundsToWin();
		for (HudPlayer player : this.hudPlayers) {
			player.pips().getChildren().clear();
			int wins = this.roundWins.getOrDefault(player.index(), 0);
			for (int i = 0; i < rounds; i++) {
				Label pip = new Label("◆");
				pip.getStyleClass().add(i < wins ? "arena-pip-won" : "arena-pip");
				player.pips().getChildren().add(pip);
			}
		}
	}

	private void updateHud(Map<Integer, Double> energies) {
		for (HudPlayer player : this.hudPlayers) {
			double energy = Math.max(0, energies.getOrDefault(player.index(), 0.0));
			double width = HUD_BAR_WIDTH * Math.min(1, energy / 100);
			double before = player.energy().getMaxWidth();
			player.energy().setMaxWidth(width);
			if (width >= player.lag().getMaxWidth()) {
				// Healed, or a replay went back: the trail follows at once
				Timeline drain = this.lagDrains.remove(player.index());
				if (drain != null) {
					drain.stop();
				}
				player.lag().setMaxWidth(width);
			} else if (width < before) {
				// The damage stays lit for a moment, then drains down to the new energy
				Timeline old = this.lagDrains.get(player.index());
				if (old != null) {
					old.stop();
				}
				Timeline drain = new Timeline(new KeyFrame(Duration.seconds(0.45), new KeyValue(player.lag().maxWidthProperty(), player.lag().getMaxWidth())),
						new KeyFrame(Duration.seconds(1.0), new KeyValue(player.lag().maxWidthProperty(), width, Interpolator.EASE_IN)));
				this.lagDrains.put(player.index(), drain);
				drain.play();
			}
			// Low energy: the bar pulses
			player.energy().setOpacity(energy < 25 ? 0.6 + 0.4 * Math.abs(Math.sin(System.nanoTime() / 1e9 * 5)) : 1);
			player.energyText().setText(String.valueOf((int) Math.ceil(energy)));
		}
	}

	private void updateInfo() {
		this.speedLabel.setText(this.paused ? "❚❚ PAUSA" : "▶ " + formatSpeed(SPEEDS[this.speed]) + "x");
		this.viewLabel.setText("◉ " + this.viewMode.label);
		if (this.recording != null) {
			boolean knockout = Configuration.getWinnerCriterion().isKnockout();
			String stage = this.stageImportance == Bracket.Importance.REGULAR ? "" : this.stageName + " · ";
			this.roundLabel.setText(stage + "ROUND " + (this.round + 1) + (knockout ? " · MELHOR DE " + Configuration.getRobocodeRounds() : "/" + this.recording.getRounds().size()));
		}
	}

	private static String formatSpeed(double speed) {
		return speed == Math.rint(speed) ? String.valueOf((int) speed) : String.valueOf(speed);
	}

	private static String toCss(Color color) {
		return String.format("#%02x%02x%02x", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
	}

	private void showBanner(String text, String sub) {
		this.banner.setStyle("");
		this.banner.setText(text);
		this.subBanner.setText(sub == null ? "" : sub);
		this.banner.setVisible(!text.isEmpty());
		this.subBanner.setVisible(sub != null);
		ScaleTransition pop = new ScaleTransition(Duration.millis(350), this.banner);
		pop.setFromX(2.5);
		pop.setFromY(2.5);
		pop.setToX(1);
		pop.setToY(1);
		pop.setInterpolator(Theme.OVERSHOOT);
		pop.play();
	}

	private void hideBanner() {
		this.banner.setVisible(false);
		this.subBanner.setVisible(false);
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
			this.updateInfo();
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
			this.pickShot(true);
		}
		this.updateInfo();
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
				this.endShowcase();
			} else {
				this.showcaseRobot(this.showcaseOrder + 1);
			}
		} else if (this.phase == Phase.FLYBY) {
			this.endFlyBy();
		} else if (this.phase == Phase.FREEZE || this.phase == Phase.REPLAY) {
			this.endReplay();
		} else if (this.phase != Phase.LOADING && this.phase != Phase.FINISHED) {
			this.finish();
		}
	}

	private void finish() {
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
					this.endShowcase();
				}
			} else {
				this.turntable();
				if (this.phaseTime > SHOWCASE_SECONDS) {
					this.showcaseRobot(this.showcaseOrder + 1);
				}
			}
		}
		case FLYBY -> {
			if (this.phaseTime > this.flyByHold() + FLYBY_SECONDS) {
				this.endFlyBy();
			}
		}
		case INTRO -> {
			int remaining = (int) Math.ceil(COUNTDOWN_SECONDS - this.phaseTime);
			if (remaining >= 1 && remaining != this.countdown) {
				this.countdown = remaining;
				this.showBanner(String.valueOf(remaining), this.roundTitle + (this.roundSubtitle == null ? "" : " - " + this.roundSubtitle));
				this.audio.countdown(remaining == 1);
			}
			if (this.phaseTime > COUNTDOWN_SECONDS && !this.fightShown) {
				this.fightShown = true;
				this.showBanner(this.fightText, null);
				this.audio.fight();
				this.audio.setMusicOn(true);
			}
			if (this.phaseTime > INTRO_SECONDS) {
				this.hideBanner();
				this.phase = Phase.PLAYING;
			}
		}
		case PLAYING -> {
			if (!this.paused) {
				this.turnPosition += dt * TPS * SPEEDS[this.speed];
			}
			List<Turn> turns = this.turns();
			if (this.turnPosition >= turns.size() - 1) {
				this.turnPosition = turns.size() - 1;
				this.showTurn(turns);
				if (this.phase == Phase.PLAYING) {
					this.endRound(turns);
				}
			} else {
				this.showTurn(turns);
			}
		}
		case FREEZE -> {
			if (this.phaseTime > FREEZE_SECONDS) {
				this.startReplay();
			}
		}
		case REPLAY -> {
			int turn = (int) this.turnPosition;
			double rate = turn >= this.deathTurn ? REPLAY_AFTER_SPEED : this.impactTurn >= 0 && this.impactTurn - turn <= 3 ? REPLAY_IMPACT_SPEED : REPLAY_SPEED;
			this.turnPosition += dt * TPS * rate;
			if (this.turnPosition >= this.replayTo) {
				this.turnPosition = this.replayTo;
				this.showTurn(this.turns());
				this.endReplay();
			} else {
				this.showTurn(this.turns());
			}
		}
		case ROUND_END -> {
			if (this.phaseTime > (this.headlineShown ? HEADLINE_SECONDS : ROUND_END_SECONDS)) {
				if (this.round + 1 < this.recording.getRounds().size()) {
					this.startRound(this.round + 1);
				} else if (this.headline != null && !this.headlineShown) {
					// A special battle gets its headline
					this.headlineShown = true;
					this.phaseTime = 0;
					this.showBanner(this.headline.title(), this.headline.subtitle());
					this.announce(this.headline.subtitle(), 6);
					this.banner.setStyle("-fx-font-size: 46px;");
					Sfx.fanfare();
				} else {
					this.finish();
				}
			}
		}
		case FINISHED -> {
		}
		}

		this.updateTension(dt);
		this.tickComments(dt);
		this.updateCamera(dt);
		this.clock += dt * this.effectRate;
		if (this.lava != null) {
			this.lava.setDiffuseColor(LAVA_DIM.interpolate(LAVA_FULL, 0.5 + 0.5 * Math.sin(this.clock * 2.1) * Math.sin(this.clock * 0.7 + 1)));
		}
		if (!this.flows.isEmpty()) {
			this.updateFlows();
		}
		// The ears are the camera: the sounds come from the side of the screen they are on
		this.audio.setListener(this.eye[0], this.eye[2], this.look[0] - this.eye[0], this.look[2] - this.eye[2]);
		for (Map.Entry<Integer, RobotView> entry : this.robots.entrySet()) {
			RobotView robotView = entry.getValue();
			robotView.roll(dt);
			double[] at = robotView.position();
			this.audio.setMachine(entry.getKey(), robotView.treadSpeed, robotView.turretSpeed, at[0], at[2]);
		}
		this.updateFire(dt * this.effectRate);
		this.updateSunbeams(dt);
		this.neon.update(dt);
		this.ice.update(dt);
		if (this.critters != null) {
			this.critters.update(dt);
		}
		this.placeBalloons();
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

	private List<Turn> turns() {
		return this.recording.getRounds().get(this.round);
	}

	private void startRound(int round) {
		this.round = round;
		this.turnPosition = 0;
		this.lastTurnProcessed = -1;
		this.replayed = false;
		this.hitPhotoTaken = false;
		this.bulletsSeen.clear();
		this.bulletsExploded.clear();
		this.lowEnergyWarned.clear();
		this.clearBullets();
		this.effects.getChildren().clear();
		this.clearFire();
		this.resetFence();
		if (this.commentary != null) {
			this.commentary.startRound();
			if (round > 0 && round + 1 == this.recording.getRounds().size()) {
				this.commentary.lastRoundStarts();
			}
		}
		this.commentQueue.clear();
		for (RobotView view : this.robots.values()) {
			view.reset();
		}

		// Skips empty rounds
		List<Turn> turns = this.turns();
		if (turns.isEmpty()) {
			this.phase = Phase.ROUND_END;
			this.phaseTime = ROUND_END_SECONDS;
			return;
		}
		this.showTurn(turns);

		// The camera at the start of the battle: a crane shot down to the arena
		this.shot = Shot.CRANE;
		this.shotTime = 0;
		this.shotDuration = INTRO_SECONDS + 2.5;
		this.shotSeed = this.random.nextDouble() * Math.PI * 2;
		this.cut();
		if (round == 0) {
			// Before the first battle the camera flies over the arena first
			this.beginFlyBy();
		} else {
			this.beginIntro();
		}
	}

	/**
	 * The round title, the countdown and the fight
	 */
	private void beginIntro() {
		this.phase = Phase.INTRO;
		this.phaseTime = 0;
		this.fightShown = false;
		this.countdown = 0;
		this.roundTitle = "ROUND " + (this.round + 1);
		boolean decisive = this.round + 1 == this.recording.getRounds().size() && this.round > 0;
		this.roundSubtitle = decisive ? "ROUND DECISIVO" : null;
		this.showBanner(this.roundTitle, this.roundSubtitle);
		this.audio.roundCall();
		this.audio.setMusicOn(true);
		this.announce(decisive ? "Round decisivo! Quem vencer leva a batalha!" : "Round " + (this.round + 1) + "! Preparem os motores!", 5);
		this.updateInfo();
	}

	/**
	 * The camera flies over the arena, low and fast past the walls, and ends
	 * where the battle starts
	 */
	private void beginFlyBy() {
		this.flyMirrorX = this.random.nextBoolean() ? 1 : -1;
		this.flyMirrorZ = this.random.nextBoolean() ? 1 : -1;
		this.phase = Phase.FLYBY;
		this.phaseTime = 0;
		this.hideBanner();
		this.showLetterbox(true, false);
		this.audio.setMusicOn(true);
		// The fly by ends framing both robots, where the battle starts
		RobotView subject = this.robots.get(this.shotSubject);
		RobotView other = this.robots.values().stream().filter(v -> v != subject).findFirst().orElse(subject);
		double[] a = subject == null ? new double[] { 0, 0, 0 } : subject.position();
		this.frameFighters(0, a, other == null ? a : other.position());
		System.arraycopy(this.eyeTarget, 0, this.flyEyeEnd, 0, 3);
		System.arraycopy(this.lookTarget, 0, this.flyLookEnd, 0, 3);
		this.flyFovEnd = this.fovTarget;
		// The first look is at a critter, from outside the field: the fly by starts on it
		this.flyFocusSource = this.critters == null ? null : this.critters.track(-0.8 * this.fieldWidth * this.flyMirrorX, -this.fieldHeight * this.flyMirrorZ);
		this.flyFocus = this.flyFocusSource == null ? null : this.flyFocusSource.get();
		// Starts at the first point of the path, no sliding to it
		this.flyByPose(0);
		System.arraycopy(this.eyeTarget, 0, this.eye, 0, 3);
		System.arraycopy(this.lookTarget, 0, this.look, 0, 3);
		this.fov = this.fovTarget;
		this.updateInfo();
	}

	/** How long the fly by stays on the critter before the trip: nothing if there is no critter */
	private double flyByHold() {
		return this.flyFocusSource == null ? 0 : FLYBY_FOCUS_SECONDS;
	}

	private void endFlyBy() {
		this.showLetterbox(false);
		this.beginIntro();
	}

	/**
	 * Puts the camera where the fly by is at the moment: through points around
	 * the arena, low, that end framing both robots, where the battle starts
	 */
	private void flyByPose(double progress) {
		double w = this.fieldWidth;
		double h = this.fieldHeight;
		double mx = this.flyMirrorX;
		double mz = this.flyMirrorZ;
		double[][] eyes = { { -0.8 * w * mx, -120, -1.0 * h * mz }, { -0.05 * w * mx, -50, -0.85 * h * mz }, { 0.85 * w * mx, -75, -0.1 * h * mz }, this.flyEyeEnd };
		double[][] looks = { { 0.1 * w * mx, -40, 0.2 * h * mz }, { 0.25 * w * mx, -25, 0.1 * h * mz }, { -0.2 * w * mx, -20, 0 }, this.flyLookEnd };
		if (this.flyFocus != null) {
			// Starts looking at the critter, from its outer side, a little above it
			double fx = this.flyFocus[0];
			double fz = this.flyFocus[2];
			double length = Math.max(1, Math.hypot(fx, fz));
			eyes[0] = new double[] { fx + fx / length * 45, this.flyFocus[1] - 22, fz + fz / length * 45 };
			looks[0] = this.flyFocus;
		}
		// Slow at the start and at the end, quick in the middle
		double smooth = progress * progress * (3 - 2 * progress);
		double u = 0.3 * progress + 0.7 * smooth;
		catmull(eyes, u, this.eyeTarget);
		catmull(looks, u, this.lookTarget);
		this.fovTarget = 62 + (this.flyFovEnd - 62) * smooth;
		this.rollTarget = 3 * Math.sin(progress * Math.PI * 2) * (1 - smooth);
		this.follow = 40;
	}

	/**
	 * A point along a smooth curve that goes through all the points, u from 0 to 1
	 */
	private static void catmull(double[][] points, double u, double[] out) {
		int segments = points.length - 1;
		double position = Math.max(0, Math.min(1, u)) * segments;
		int i = Math.min(segments - 1, (int) position);
		double t = position - i;
		double[] p0 = points[Math.max(0, i - 1)];
		double[] p1 = points[i];
		double[] p2 = points[i + 1];
		double[] p3 = points[Math.min(segments, i + 2)];
		for (int k = 0; k < 3; k++) {
			out[k] = 0.5 * (2 * p1[k] + (p2[k] - p0[k]) * t + (2 * p0[k] - 5 * p1[k] + 4 * p2[k] - p3[k]) * t * t + (3 * p1[k] - p0[k] - 3 * p2[k] + p3[k]) * t * t * t);
		}
	}

	private void endRound(List<Turn> turns) {
		this.phase = Phase.ROUND_END;
		this.phaseTime = 0;

		List<RobotState> alive = turns.get(turns.size() - 1).robots().stream().filter(r -> !r.isDead()).toList();
		this.say(this.commentary.roundEnd(alive.size() == 1 ? alive.get(0).index() : -1));
		if (this.round + 1 == this.recording.getRounds().size()) {
			this.headline = this.commentary.battleEnd();
		}
		if (alive.size() == 1) {
			int winner = alive.get(0).index();
			this.roundWins.merge(winner, 1, Integer::sum);
			RobotView view = this.robots.get(winner);
			this.showBanner("K.O.!", (view == null ? "" : view.displayName) + " vence o round");
			this.announce("K.O.! " + (view == null ? "" : view.displayName) + " vence o round!", 5);
			this.audio.knockOut();
			// The winner, from close
			this.shot = Shot.CLOSE;
			this.shotSubject = winner;
			this.shotTime = 0;
			this.shotDuration = ROUND_END_SECONDS + 1;
			this.shotSide = this.random.nextBoolean() ? 1 : -1;
			this.closeDistance = 110;
		} else {
			this.showBanner("FIM DO ROUND", null);
		}
		this.audio.setMusicOn(false);
		this.updatePips();
	}

	/**
	 * How tense the fight is: low energy, the end of the round, the deciding
	 * round and recent hits make the music grow
	 */
	private void updateTension(double dt) {
		this.hitTension *= Math.exp(-0.5 * dt);
		if (this.recording == null || this.phase == Phase.LOADING) {
			this.audio.setIntensity(0);
			return;
		}
		double lowest = 100;
		for (RobotView view : this.robots.values()) {
			if (!view.dead) {
				lowest = Math.min(lowest, view.energy);
			}
		}
		double progress = Math.min(1, this.turnPosition / Math.max(1, this.turns().size()));
		boolean deciding = this.round + 1 == this.recording.getRounds().size() && this.recording.getRounds().size() > 1;
		double tension = 0.12 + 0.55 * (1 - Math.max(0, Math.min(100, lowest)) / 100) + 0.15 * progress + (deciding ? 0.15 : 0) + this.hitTension;
		if (this.phase == Phase.INTRO || this.phase == Phase.SHOWCASE || this.phase == Phase.FLYBY) {
			tension = 0.1;
		}
		this.audio.setIntensity(tension);
	}

	/**
	 * Puts everything where it is at the current (fractional) turn and fires the
	 * events of the turns just reached
	 */
	private void showTurn(List<Turn> turns) {
		int index = Math.min((int) Math.floor(this.turnPosition), turns.size() - 1);
		double fraction = this.turnPosition - index;
		Turn current = turns.get(index);
		Turn next = turns.get(Math.min(index + 1, turns.size() - 1));

		for (int t = this.lastTurnProcessed + 1; t <= index; t++) {
			this.turnEvents(turns.get(t), t == 0 ? null : turns.get(t - 1), t);
		}
		this.lastTurnProcessed = Math.max(this.lastTurnProcessed, index);

		Map<Integer, Double> energies = new HashMap<Integer, Double>();
		for (RobotState state : current.robots()) {
			RobotState after = find(next.robots(), state.index(), state);
			RobotView view = this.robots.get(state.index());
			if (view == null) {
				continue;
			}
			view.place(this.worldX(lerp(state.x(), after.x(), fraction)), this.worldZ(lerp(state.y(), after.y(), fraction)), lerpAngle(state.bodyHeading(), after.bodyHeading(), fraction),
					lerpAngle(state.gunHeading(), after.gunHeading(), fraction), lerpAngle(state.radarHeading(), after.radarHeading(), fraction));
			double energy = lerp(state.energy(), after.energy(), fraction);
			view.setEnergy(state.isDead() ? 0 : energy);
			energies.put(state.index(), state.isDead() ? 0 : energy);
		}
		this.updateHud(energies);

		this.showBullets(turns, index, fraction);
	}

	/**
	 * Where a robot is drawn at a moment (in turns, with the fraction) and its
	 * heading, as showTurn places it
	 */
	private static double[] pose(List<Turn> turns, int robot, double time) {
		int index = Math.max(0, Math.min((int) Math.floor(time), turns.size() - 1));
		double fraction = Math.max(0, Math.min(1, time - index));
		RobotState state = find(turns.get(index).robots(), robot, null);
		if (state == null) {
			return null;
		}
		RobotState after = find(turns.get(Math.min(index + 1, turns.size() - 1)).robots(), robot, state);
		return new double[] { lerp(state.x(), after.x(), fraction), lerp(state.y(), after.y(), fraction), lerpAngle(state.bodyHeading(), after.bodyHeading(), fraction) };
	}

	private static Bullet findBullet(Turn turn, int id) {
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
	private record PendingPhoto(double remaining, int round, boolean destruction, Opponent subject, double power) {
	}

	private record Flight(double sx, double sy, double ux, double uy, double total, double born, double duration, Bullet hit, double side, double ahead) {
	}

	/**
	 * Robocode hits with a square of 36 that does not turn with the robot, the
	 * tank is narrower (and longer): a bullet that grazes a corner of the square
	 * would pass by the side of the tank. The point of the hit is brought well
	 * inside the hull (13.5 to each side, 22 to the front and to the back), so
	 * the bolt always goes into it.
	 */
	private static final double HULL_SIDE = 8;
	private static final double HULL_AHEAD = 15;
	/** The height of the gun, where the bolts leave */
	private static final double BARREL_HEIGHT = -17.2;
	/**
	 * The height of the middle of the hull (from -7 to -13.5): the tank is low,
	 * at the height of the gun a bolt would pass over it
	 */
	private static final double HULL_HEIGHT = -10.5;

	/** How far the end of the gun is from the center of the tank */
	private static final double MUZZLE = 44;

	private final Map<Integer, Flight> flights = new HashMap<Integer, Flight>();

	/**
	 * The records only say where a bullet is at the end of each turn, one step
	 * after it left the center of the shooter, and the last one is the point of
	 * the hit (that follows the victim). So the flight is made from the whole
	 * path: the bolt comes out of the muzzle of the gun and goes straight to the
	 * point of the hit, always at the same pace, and explodes when it gets there.
	 */
	private Flight flight(List<Turn> turns, int index, Bullet seen) {
		Flight known = this.flights.get(seen.id());
		if (known != null) {
			return known;
		}
		int first = index;
		while (first > 0 && findBullet(turns.get(first - 1), seen.id()) != null) {
			first--;
		}
		Bullet start = findBullet(turns.get(first), seen.id());
		if (start == null) {
			first = index + 1;
			start = seen;
		}
		Bullet last = start;
		Bullet previous = start;
		Bullet hit = null;
		int end = first;
		for (int k = first + 1; k < turns.size(); k++) {
			Bullet now = findBullet(turns.get(k), seen.id());
			if (now == null) {
				break;
			}
			end = k;
			if (now.isHit()) {
				hit = now;
				last = now;
				break;
			}
			previous = now;
			last = now;
		}
		double speed = 20 - 3 * start.power();
		double dx = last.x() - start.x();
		double dy = last.y() - start.y();
		double length = Math.hypot(dx, dy);
		Flight flight = null;
		if (length > 1e-6 && !start.isHit()) {
			dx /= length;
			dy /= length;
			double born = first - 1;
			// Gets there when the last step is done
			double duration = hit != null ? end - 1 - born + Math.hypot(hit.x() - previous.x(), hit.y() - previous.y()) / speed : end - born;
			duration = Math.max(0.01, duration);
			double sx = start.x() - dx * speed;
			double sy = start.y() - dy * speed;
			double ex = last.x();
			double ey = last.y();
			// Where it hits the victim, in the frame of the tank (across and ahead),
			// and where that point of the hull is drawn when the bolt gets there: the
			// bolt flies straight to it
			double side = 0;
			double ahead = 0;
			if (hit != null && hit.state() == BattleRecording.BULLET_HIT_VICTIM) {
				RobotState victim = find(turns.get(end).robots(), hit.victim(), null);
				double[] pose = pose(turns, hit.victim(), born + duration);
				if (victim != null && pose != null) {
					double ox = hit.x() - victim.x();
					double oy = hit.y() - victim.y();
					double heading = victim.bodyHeading();
					side = Math.max(-HULL_SIDE, Math.min(HULL_SIDE, ox * Math.cos(heading) - oy * Math.sin(heading)));
					ahead = Math.max(-HULL_AHEAD, Math.min(HULL_AHEAD, ox * Math.sin(heading) + oy * Math.cos(heading)));
					ex = pose[0] + side * Math.cos(pose[2]) + ahead * Math.sin(pose[2]);
					ey = pose[1] - side * Math.sin(pose[2]) + ahead * Math.cos(pose[2]);
				}
			}
			double total = Math.hypot(ex - sx, ey - sy);
			if (total > 1e-6) {
				flight = new Flight(sx, sy, (ex - sx) / total, (ey - sy) / total, total, born, duration, hit, side, ahead);
			}
		}
		// A bullet with no flight is never drawn: it was born hitting
		this.flights.put(seen.id(), flight == null ? new Flight(0, 0, 0, 0, 0, 0, 0, null, 0, 0) : flight);
		return this.flights.get(seen.id());
	}

	private void showBullets(List<Turn> turns, int index, double fraction) {
		Turn current = turns.get(index);
		Turn next = turns.get(Math.min(index + 1, turns.size() - 1));
		Set<Integer> flying = new HashSet<Integer>();
		for (Turn turn : next == current ? List.of(current) : List.of(current, next)) {
			for (Bullet bullet : turn.bullets()) {
				if (bullet.isHit() || bullet.state() > BattleRecording.BULLET_HIT_WALL || flying.contains(bullet.id())) {
					continue;
				}
				Flight flight = this.flight(turns, turn == current ? index : index + 1, bullet);
				if (flight.total() <= 0) {
					continue;
				}
				double progress = (this.turnPosition - flight.born()) / flight.duration();
				if (progress < 0) {
					continue;
				}
				if (progress >= 1) {
					if (flight.hit() != null) {
						double[] target = this.target(flight);
						this.bulletHit(flight.hit(), target[0], target[1]);
						continue;
					}
					progress = 1;
				}
				// From the muzzle to the end, the speed is the same all the way
				double distance = flight.total() > MUZZLE ? MUZZLE + (flight.total() - MUZZLE) * progress : flight.total() * progress;
				double x = flight.sx() + flight.ux() * distance;
				double y = flight.sy() + flight.uy() * distance;
				// The ones that hit a tank come down, little by little, from the height of
				// the gun to the middle of the hull
				double height = BARREL_HEIGHT;
				if (flight.hit() != null && flight.hit().state() == BattleRecording.BULLET_HIT_VICTIM) {
					double along = flight.total() > MUZZLE ? (distance - MUZZLE) / (flight.total() - MUZZLE) : progress;
					height += (HULL_HEIGHT - BARREL_HEIGHT) * Math.max(0, Math.min(1, along));
				}
				if (distance >= MUZZLE) {
					this.bulletFired(bullet, flight.sx() + flight.ux() * MUZZLE, flight.sy() + flight.uy() * MUZZLE);
				} else if (this.bulletsSeen.contains(bullet.id()) == false) {
					this.bulletFired(bullet, flight.sx() + flight.ux() * Math.min(MUZZLE, flight.total()), flight.sy() + flight.uy() * Math.min(MUZZLE, flight.total()));
				}
				this.placeBullet(bullet, flying, x, y, flight.ux(), flight.uy(), height);
			}
		}
		this.bullets.entrySet().removeIf(entry -> {
			if (!flying.contains(entry.getKey())) {
				this.bulletLayer.getChildren().remove(entry.getValue());
				return true;
			}
			return false;
		});
	}

	private void placeBullet(Bullet bullet, Set<Integer> flying, double x, double y, double dx, double dy, double height) {
		flying.add(bullet.id());
		Group shot = this.bullets.computeIfAbsent(bullet.id(), id -> this.newBullet(bullet));
		shot.setTranslateX(this.worldX(x));
		shot.setTranslateZ(this.worldZ(y));
		shot.setTranslateY(height);
		// The bolt points to where it goes, turned around its head
		((Rotate) shot.getTransforms().get(0)).setAngle(Math.toDegrees(Math.atan2(dx, dy)));
	}

	/**
	 * Where the flight ends now: the point of the hit on the victim as it is drawn
	 * at this moment, or the end of the path
	 */
	private double[] target(Flight flight) {
		Bullet hit = flight.hit();
		RobotView victim = hit == null || hit.state() != BattleRecording.BULLET_HIT_VICTIM ? null : this.robots.get(hit.victim());
		if (victim == null) {
			return new double[] { flight.sx() + flight.ux() * flight.total(), flight.sy() + flight.uy() * flight.total() };
		}
		double x = victim.root.getTranslateX() + this.fieldWidth / 2;
		double y = victim.root.getTranslateZ() + this.fieldHeight / 2;
		double heading = Math.toRadians(victim.bodyRotate.getAngle());
		return new double[] { x + flight.side() * Math.cos(heading) + flight.ahead() * Math.sin(heading), y - flight.side() * Math.sin(heading) + flight.ahead() * Math.cos(heading) };
	}

	/** Something happens in the battle at this world point: the critters nearby react to it */
	private void startle(double worldX, double worldZ, double radius, double strength) {
		if (this.critters != null) {
			this.critters.alarm(worldX, worldZ, radius, strength);
		}
	}

	/**
	 * The shot, once: the sound and the flash at the gun
	 */
	private void bulletFired(Bullet bullet, double x, double y) {
		if (this.bulletsSeen.add(bullet.id())) {
			this.audio.shotAt(bullet.power(), this.worldX(x), this.worldZ(y));
			this.muzzleFlash(this.worldX(x), this.worldZ(y), this.shooterColor(bullet));
			this.startle(this.worldX(x), this.worldZ(y), 150, 0.4);
		}
	}

	/**
	 * The hit, once: the explosion where the bullet stopped and what the victim
	 * feels
	 */
	private void bulletHit(Bullet bullet, double x, double y) {
		if (!this.bulletsExploded.add(bullet.id())) {
			return;
		}
		this.explosion(this.worldX(x), this.worldZ(y), 8 + bullet.power() * 6, this.shooterColor(bullet));
		this.startle(this.worldX(x), this.worldZ(y), 220, 0.7);
		RobotView victim = this.robots.get(bullet.victim());
		if (bullet.state() == BattleRecording.BULLET_HIT_VICTIM && victim != null) {
			victim.flash();
			this.audio.hitAt(bullet.power(), this.worldX(x), this.worldZ(y));
			this.hitTension = Math.min(0.25, this.hitTension + 0.08 + bullet.power() * 0.03);
			this.shake += 3 + bullet.power() * 3;
			this.impactCut(bullet.victim(), bullet.power());
			// The first hit that hurts of the round is kept as a photo
			RobotView shooter = this.robots.get(bullet.owner());
			if (this.phase == Phase.PLAYING && !this.hitPhotoTaken && bullet.power() >= PHOTO_HIT_POWER && shooter != null) {
				this.hitPhotoTaken = true;
				this.pendingPhotos.add(new PendingPhoto(PHOTO_HIT_DELAY, this.round + 1, false, shooter.opponent, bullet.power()));
			}
		} else {
			this.audio.sparkAt(this.worldX(x), this.worldZ(y));
			if (bullet.state() == BattleRecording.BULLET_HIT_WALL) {
				this.flashFence(bullet.x(), bullet.y());
			}
		}
	}

	private static RobotState find(List<RobotState> states, int index, RobotState fallback) {
		for (RobotState state : states) {
			if (state.index() == index) {
				return state;
			}
		}
		return fallback;
	}

	/**
	 * Shots, hits and deaths happen once, when their turn is reached
	 */
	private void turnEvents(Turn turn, Turn previous, int turnIndex) {
		this.say(this.commentary.turn(turn, previous));
		for (Bullet bullet : turn.bullets()) {
			if (!bullet.isHit()) {
				this.bulletFired(bullet, bullet.x(), bullet.y());
			} else {
				this.bulletsSeen.add(bullet.id());
				this.bulletHit(bullet, bullet.x(), bullet.y());
			}
		}
		for (RobotState state : turn.robots()) {
			RobotState before = previous == null ? null : find(previous.robots(), state.index(), null);
			RobotView view = this.robots.get(state.index());
			if (view == null) {
				continue;
			}
			if ((state.state() == BattleRecording.ROBOT_HIT_WALL || state.state() == BattleRecording.ROBOT_HIT_ROBOT) && before != null && before.state() != state.state()) {
				this.audio.thudAt(this.worldX(state.x()), this.worldZ(state.y()));
				this.startle(this.worldX(state.x()), this.worldZ(state.y()), 120, 0.3);
				this.shake += 1.5;
				if (state.state() == BattleRecording.ROBOT_HIT_WALL) {
					this.flashFence(state.x(), state.y());
				}
			}
			if (!state.isDead() && state.energy() < 20 && this.lowEnergyWarned.add(state.index())) {
				this.audio.lowEnergyAt(view.position()[0], view.position()[2]);
			}
			if (state.isDead() && before != null && !before.isDead() && !view.dead) {
				view.destroy();
				this.audio.destroyedAt(view.position()[0], view.position()[2]);
				this.startle(view.position()[0], view.position()[2], 340, 1);
				this.shake += 22;
				this.whiteFlash();
				if (this.phase == Phase.PLAYING) {
					this.pendingPhotos.add(new PendingPhoto(PHOTO_DESTRUCTION_DELAY, this.round + 1, true, view.opponent, 0));
				}
				if (this.phase == Phase.PLAYING && !this.replayed) {
					this.freezeForReplay(state.index(), turnIndex);
				}
			}
		}
	}

	/**
	 * The color of the robot that fired the bullet
	 */
	private Color shooterColor(Bullet bullet) {
		RobotView owner = this.robots.get(bullet.owner());
		return owner == null ? Color.web("#38e6ff") : owner.playerColor;
	}

	/**
	 * A plasma bolt: a bright spindle of facets, a halo around it and a long tail
	 * behind, in the color of the robot that fired it. It points to +Z and is
	 * turned to where it goes.
	 */
	private Group newBullet(Bullet bullet) {
		Color tint = this.shooterColor(bullet);
		double r = 2.2 + bullet.power() * 0.7;
		LowPoly core = new LowPoly();
		core.spindle(0, 0, 0, r, r * 3.4, r * 2.2, 6);
		LowPoly halo = new LowPoly();
		halo.spindle(0, 0, 0, r * 1.9, r * 3.8, r * 2.8, 6);
		LowPoly tail = new LowPoly();
		tail.spindle(0, 0, -r * 2, r * 0.9, 0, r * 12, 6);

		Color hot = tint.interpolate(Color.WHITE, 0.75);
		PhongMaterial coreMaterial = new PhongMaterial(hot);
		coreMaterial.setSelfIlluminationMap(solidMap(hot));
		// The halo and the tail are see through and shine by themselves
		PhongMaterial haloMaterial = new PhongMaterial(Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.28));
		haloMaterial.setSelfIlluminationMap(solidMap(tint));
		PhongMaterial tailMaterial = new PhongMaterial(Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.4));
		tailMaterial.setSelfIlluminationMap(solidMap(tint));
		Group shot = new Group(tail.view(tailMaterial), halo.view(haloMaterial), core.view(coreMaterial));
		// Turned around the head, where the bullet is. Not with setRotate: it turns
		// around the middle of the bolt, the long tail moves it there, and the head
		// would be drawn up to 40 units away from the bullet, to the side of the
		// robot it hits.
		shot.getTransforms().add(new Rotate(0, Rotate.Y_AXIS));
		shot.setTranslateY(BARREL_HEIGHT);
		this.bulletLayer.getChildren().add(shot);
		return shot;
	}

	/**
	 * A small picture of one color, for materials that shine by themselves
	 */
	private static WritableImage solidMap(Color color) {
		WritableImage map = new WritableImage(2, 2);
		for (int y = 0; y < 2; y++) {
			for (int x = 0; x < 2; x++) {
				map.getPixelWriter().setColor(x, y, color);
			}
		}
		return map;
	}

	private void clearBullets() {
		for (Group bullet : this.bullets.values()) {
			this.bulletLayer.getChildren().remove(bullet);
		}
		this.bullets.clear();
		this.flights.clear();
	}

	private void whiteFlash() {
		FadeTransition flash = new FadeTransition(Duration.millis(380), this.whiteFlash);
		flash.setFromValue(0.75);
		flash.setToValue(0);
		flash.play();
	}

	/**
	 * The cage lights up where a robot or a bullet hit it, the glow spreads to
	 * the neighboring panels like a shockwave
	 */
	private void flashFence(double x, double y) {
		double w = this.fieldWidth;
		double h = this.fieldHeight;
		double cx = Math.max(0, Math.min(w, x));
		double cy = Math.max(0, Math.min(h, y));

		double[] distances = { cy, h - cy, cx, w - cx };
		int wall = 0;
		for (int i = 1; i < distances.length; i++) {
			if (distances[i] < distances[wall]) {
				wall = i;
			}
		}

		double along = wall <= 1 ? cx : cy;
		int count = (int) Math.round((wall <= 1 ? w : h) / 100);
		int index = Math.max(0, Math.min(count - 1, (int) Math.floor(along / 100)));

		this.flashSegment(wall, index, 1.0, Duration.ZERO);
		this.flashSegment(wall, index - 1, 0.6, Duration.millis(60));
		this.flashSegment(wall, index + 1, 0.6, Duration.millis(60));
		this.flashSegment(wall, index - 2, 0.3, Duration.millis(120));
		this.flashSegment(wall, index + 2, 0.3, Duration.millis(120));
	}

	private void flashSegment(int wall, int index, double intensity, Duration delay) {
		for (FenceSegment segment : this.fenceSegments) {
			if (segment.wall() != wall || segment.index() != index) {
				continue;
			}
			if (segment.animation != null) {
				segment.animation.stop();
				segment.animation = null;
			}

			segment.box().setVisible(true);

			// Electric force field energy flare: transparent translucent barrier
			double peakAlpha = 0.20 + 0.08 * Math.min(1.0, intensity);
			Color tint = this.style.fence;
			Color shieldBright = Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), peakAlpha);
			Color shieldMid = Color.color(tint.getRed() * 0.6, tint.getGreen() * 0.8, tint.getBlue(), peakAlpha * 0.45);
			Color shieldFade = Color.color(tint.getRed() * 0.4, tint.getGreen() * 0.8, tint.getBlue(), 0.0);

			segment.material().setSpecularColor(Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.20));

			Timeline flash = new Timeline();
			flash.getKeyFrames().addAll(
					new KeyFrame(delay, new KeyValue(segment.material().diffuseColorProperty(), shieldBright)),
					new KeyFrame(delay.add(Duration.millis(90)), new KeyValue(segment.material().diffuseColorProperty(), shieldMid)),
					new KeyFrame(delay.add(Duration.millis(320)), new KeyValue(segment.material().diffuseColorProperty(), shieldFade)));
			flash.setOnFinished(e -> {
				segment.box().setVisible(false);
				segment.material().setDiffuseColor(FENCE_BASE);
				segment.material().setSpecularColor(Color.TRANSPARENT);
				segment.animation = null;
			});
			segment.animation = flash;
			flash.play();

			if (intensity >= 0.8) {
				ScaleTransition pulse = new ScaleTransition(Duration.millis(90), segment.box());
				pulse.setFromX(1);
				pulse.setToX(1.04);
				pulse.setFromY(1);
				pulse.setToY(1.04);
				pulse.setFromZ(1);
				pulse.setToZ(1.04);
				pulse.setCycleCount(2);
				pulse.setAutoReverse(true);
				pulse.play();
			}
			return;
		}
	}

	/** Back to the base look when a new round starts */
	private void resetFence() {
		for (FenceSegment segment : this.fenceSegments) {
			if (segment.animation != null) {
				segment.animation.stop();
				segment.animation = null;
			}
			segment.box().setVisible(false);
			segment.material().setDiffuseColor(FENCE_BASE);
			segment.material().setSpecularColor(Color.TRANSPARENT);
			segment.box().setScaleX(1);
			segment.box().setScaleY(1);
			segment.box().setScaleZ(1);
		}
	}

	// ---------------------------------------------------------------- effects

	/**
	 * Animations of the effects follow the slow motion
	 */
	private <T extends Animation> T effect(T animation) {
		animation.setRate(this.effectRate);
		return animation;
	}

	/** A crystal of light, built once: the flash of a shot, a spark */
	private static final TriangleMesh CRYSTAL = spindleMesh(1, 1, 1, 4);

	private static TriangleMesh spindleMesh(double radius, double front, double back, int sides) {
		LowPoly shape = new LowPoly();
		shape.spindle(0, 0, 0, radius, front, back, sides);
		return shape.mesh;
	}

	/**
	 * A see through bubble: the color with a low opacity and no light of its own, so
	 * the arena shows through it. A light map would make it opaque.
	 */
	private static PhongMaterial bubble(Color color) {
		Color light = color.interpolate(Color.WHITE, 0.5);
		PhongMaterial material = new PhongMaterial(Color.color(light.getRed(), light.getGreen(), light.getBlue(), 0.3));
		material.setSpecularColor(Color.color(1, 1, 1, 0.6));
		material.setSpecularPower(64);
		return material;
	}

	/**
	 * A see through material that shines in the color by itself
	 */
	private static PhongMaterial energy(Color color, double opacity) {
		PhongMaterial material = new PhongMaterial(Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity));
		material.setSelfIlluminationMap(solidMap(color));
		return material;
	}

	private static MeshView shape(TriangleMesh mesh, PhongMaterial material) {
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		return view;
	}

	/**
	 * A jagged bolt of lightning from a to b, with now and then a branch, as thin
	 * tubes: a wide one that glows and a thin white core
	 */
	private void bolt(LowPoly glow, LowPoly core, double[] from, double[] to, double radius, int depth) {
		int steps = 7;
		double length = Math.sqrt(Math.pow(to[0] - from[0], 2) + Math.pow(to[1] - from[1], 2) + Math.pow(to[2] - from[2], 2));
		double[] previous = from;
		for (int i = 1; i <= steps; i++) {
			double t = (double) i / steps;
			double jitter = i == steps ? 0 : length * 0.11;
			double[] point = new double[3];
			for (int k = 0; k < 3; k++) {
				point[k] = from[k] + (to[k] - from[k]) * t + (this.random.nextDouble() - 0.5) * 2 * jitter;
			}
			glow.tube(previous, point, radius * 3, 4);
			core.tube(previous, point, radius, 4);
			if (depth < 1 && i > 1 && i < steps && this.random.nextDouble() < 0.3) {
				double[] branch = new double[3];
				for (int k = 0; k < 3; k++) {
					branch[k] = point[k] + (to[k] - from[k]) * 0.3 + (this.random.nextDouble() - 0.5) * length * 0.5;
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
	private void discharge(Group blast, ParallelTransition all, double[] center, double floorY, int bolts, double reach, double radius, Color tint, double delay) {
		Color arc = tint.interpolate(Color.web("#8fe9ff"), 0.35);
		PhongMaterial glowMaterial = energy(arc, 0.25);
		PhongMaterial coreMaterial = energy(Color.WHITE, 1);
		List<Node> frames = new ArrayList<Node>();
		for (int frame = 0; frame < 3; frame++) {
			LowPoly glow = new LowPoly();
			LowPoly core = new LowPoly();
			for (int i = 0; i < bolts; i++) {
				double angle = this.random.nextDouble() * Math.PI * 2;
				double rise = i % 3 == 0 ? 0.05 : this.random.nextDouble() * 1.4 - 0.3;
				double flat = Math.sqrt(Math.max(0.05, 1 - Math.min(0.9, rise * rise * 0.5)));
				double length = reach * (0.55 + this.random.nextDouble() * 0.6);
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
		for (int i = 0; i < order.length; i++) {
			int shown = order[i];
			flicker.getKeyFrames().add(new KeyFrame(Duration.millis(i * 45), event -> {
				for (int k = 0; k < frames.size(); k++) {
					frames.get(k).setVisible(k == shown);
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
	private void sparks(Group blast, ParallelTransition all, double[] center, double floorY, int count, double reach, double size, Color color, double millis) {
		PhongMaterial material = energy(color, 1);
		for (int i = 0; i < count; i++) {
			MeshView spark = shape(CRYSTAL, material);
			double scale = size * (0.5 + this.random.nextDouble() * 0.8);
			spark.setScaleX(scale);
			spark.setScaleY(scale);
			spark.setScaleZ(scale * 1.6);
			spark.setTranslateX(center[0]);
			spark.setTranslateY(center[1]);
			spark.setTranslateZ(center[2]);
			blast.getChildren().add(spark);
			double angle = this.random.nextDouble() * Math.PI * 2;
			double distance = reach * (0.3 + this.random.nextDouble() * 0.9);
			double height = reach * (0.3 + this.random.nextDouble() * 0.8);
			double time = millis * (0.7 + this.random.nextDouble() * 0.5);
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

	private static TriangleMesh ringMesh() {
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
	private void shockwave(Group blast, ParallelTransition all, double floorY, double radius, double delay, double millis, Color color) {
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

	private void muzzleFlash(double x, double z, Color tint) {
		MeshView flash = shape(CRYSTAL, energy(tint.interpolate(Color.WHITE, 0.8), 1));
		flash.setTranslateX(x);
		flash.setTranslateZ(z);
		flash.setTranslateY(-21);
		this.effects.getChildren().add(flash);
		ScaleTransition out = this.effect(new ScaleTransition(Duration.millis(140), flash));
		out.setFromX(6);
		out.setFromY(6);
		out.setFromZ(6);
		out.setToX(0);
		out.setToY(0);
		out.setToZ(0);
		out.setOnFinished(event -> this.effects.getChildren().remove(flash));
		out.play();
	}

	/**
	 * An electric blast: a flash, bolts of lightning crackling out and sparks
	 * thrown in an arc, in the color of the shooter
	 */
	private void explosion(double x, double z, double size, Color tint) {
		Group blast = new Group();
		blast.setTranslateX(x);
		blast.setTranslateZ(z);
		blast.setTranslateY(-18);

		MeshView flash = shape(CRYSTAL, energy(tint.interpolate(Color.WHITE, 0.9), 0.6));
		blast.getChildren().add(flash);
		double half = size / 2;
		ParallelTransition all = new ParallelTransition(grow(flash, 0.2 * half, 0.7 * half, 100), shrink(flash, 100, 140));
		double[] center = { 0, 0, 0 };
		// The floor is 18 below the center of a blast
		this.discharge(blast, all, center, 18, size > 30 ? 8 : 5, size * 2.2, Math.max(0.3, size / 60), tint, 0);
		this.sparks(blast, all, center, 18, size > 30 ? 26 : 14, size * 2, Math.max(0.5, size / 30), Color.web("#fff3b0"), 650);
		this.effects.getChildren().add(blast);
		this.effect(all).setOnFinished(event -> this.effects.getChildren().remove(blast));
		all.play();
		this.lightFlash(x, z, tint.interpolate(Color.web("#8fe9ff"), 0.5), 0.5);
	}

	private static ScaleTransition grow(Node node, double from, double to, double millis) {
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

	private static ScaleTransition shrink(Node node, double delay, double millis) {
		ScaleTransition vanish = new ScaleTransition(Duration.millis(millis), node);
		vanish.setDelay(Duration.millis(delay));
		vanish.setToX(0);
		vanish.setToY(0);
		vanish.setToZ(0);
		return vanish;
	}

	/**
	 * A piece flying in an arc: up, then down to the floor
	 */
	private Timeline fly(Node piece, double dx, double dz, double height, double millis) {
		double baseY = piece.getTranslateY();
		return new Timeline( //
				new KeyFrame(Duration.millis(millis * 0.4), new KeyValue(piece.translateYProperty(), baseY - height, Interpolator.EASE_OUT)), //
				new KeyFrame(Duration.millis(millis), //
						new KeyValue(piece.translateXProperty(), piece.getTranslateX() + dx, Interpolator.EASE_OUT), //
						new KeyValue(piece.translateZProperty(), piece.getTranslateZ() + dz, Interpolator.EASE_OUT), //
						new KeyValue(piece.translateYProperty(), Math.max(baseY, 16), Interpolator.EASE_IN), //
						new KeyValue(piece.rotateProperty(), 540 + Math.random() * 360, Interpolator.LINEAR), //
						new KeyValue(piece.scaleXProperty(), 0.3), new KeyValue(piece.scaleYProperty(), 0.3), new KeyValue(piece.scaleZProperty(), 0.3)));
	}

	/**
	 * The explosions light up the arena for a moment
	 */
	private void lightFlash(double x, double z, Color color, double seconds) {
		this.flashLight.setTranslateX(x);
		this.flashLight.setTranslateZ(z);
		this.flashLight.setTranslateY(-60);
		this.flashLight.setColor(color);
		this.effect(new Timeline(new KeyFrame(Duration.seconds(seconds), new KeyValue(this.flashLight.colorProperty(), Color.BLACK)))).play();
	}

	// ------------------------------------------------------------- commentary

	/**
	 * Callouts on the side of each player, under the energy bars, and the
	 * commentator at the bottom
	 */
	private Node buildCommentary() {
		this.calloutsLeft.setAlignment(Pos.TOP_LEFT);
		this.calloutsRight.setAlignment(Pos.TOP_RIGHT);
		this.calloutsCenter.setAlignment(Pos.TOP_CENTER);
		for (VBox column : List.of(this.calloutsLeft, this.calloutsRight, this.calloutsCenter)) {
			column.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		}
		StackPane.setAlignment(this.calloutsLeft, Pos.TOP_LEFT);
		StackPane.setMargin(this.calloutsLeft, new Insets(130, 0, 0, 40));
		StackPane.setAlignment(this.calloutsRight, Pos.TOP_RIGHT);
		StackPane.setMargin(this.calloutsRight, new Insets(130, 40, 0, 0));
		StackPane.setAlignment(this.calloutsCenter, Pos.TOP_CENTER);
		StackPane.setMargin(this.calloutsCenter, new Insets(130, 0, 0, 0));

		Label tag = this.liveTag;
		tag.getStyleClass().add("arena-comment-tag");
		this.commentText.getStyleClass().add("arena-comment-text");
		this.commentText.setWrapText(true);
		this.ticker.getChildren().addAll(tag, this.commentText);
		this.ticker.setAlignment(Pos.CENTER_LEFT);
		this.ticker.getStyleClass().add("arena-comment");
		this.ticker.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		this.ticker.setOpacity(0);
		// The presenter stands at the left, the comment beside it
		StackPane.setAlignment(this.presenter, Pos.BOTTOM_LEFT);
		StackPane.setMargin(this.presenter, new Insets(0, 0, 40, 28));
		StackPane.setAlignment(this.ticker, Pos.BOTTOM_LEFT);
		StackPane.setMargin(this.ticker, new Insets(0, 0, 64, 250));
		this.ticker.maxWidthProperty().bind(this.widthProperty().subtract(300).multiply(0.9));

		this.commentaryLayer.getChildren().addAll(this.presenter, this.calloutsLeft, this.calloutsRight, this.calloutsCenter, this.ticker);
		this.commentaryLayer.setMouseTransparent(true);
		this.commentaryLayer.setPickOnBounds(false);
		return this.commentaryLayer;
	}

	/**
	 * Shows what the commentary said, nothing during the replay: it was said
	 * already
	 */
	private void say(List<BattleCommentary.Message> messages) {
		if (this.phase == Phase.REPLAY) {
			return;
		}
		for (BattleCommentary.Message message : messages) {
			if (message.callout()) {
				this.callout(message);
			} else {
				this.queueComment(message);
			}
		}
	}

	/**
	 * Position of the robot in the battle: 0 is on the left, 1 on the right
	 */
	private int orderOf(int robot) {
		List<BattleRecording.Robot> list = this.recording.getRobots();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getIndex() == robot) {
				return i;
			}
		}
		return -1;
	}

	private Color toneColor(BattleCommentary.Message message) {
		return switch (message.tone()) {
		case DANGER -> Color.web("#ff2a2a");
		case GOLD -> Theme.ACCENT;
		case NEUTRAL -> Color.web("#eef2fa");
		case PLAYER -> {
			int order = this.orderOf(message.robot());
			yield order < 0 ? Theme.ACCENT : PLAYER_COLORS[order % PLAYER_COLORS.length];
		}
		};
	}

	/**
	 * A big word that pops on the side of the robot and goes away
	 */
	private void callout(BattleCommentary.Message message) {
		int order = this.orderOf(message.robot());
		VBox column = order == 0 ? this.calloutsLeft : order == 1 ? this.calloutsRight : this.calloutsCenter;
		Color color = this.toneColor(message);

		Label label = new Label(message.text());
		label.getStyleClass().add("arena-callout");
		label.setTextFill(color);
		label.setEffect(Theme.glow(color, 16));
		label.setRotate(order == 0 ? -4 : order == 1 ? 4 : 0);
		// The same callout again pops again, it is not repeated
		column.getChildren().removeIf(node -> node instanceof Label other && other.getText().equals(message.text()));
		if (column.getChildren().size() >= MAX_CALLOUTS) {
			column.getChildren().remove(0);
		}
		column.getChildren().add(label);

		ScaleTransition pop = new ScaleTransition(Duration.millis(280), label);
		pop.setFromX(2.2);
		pop.setFromY(2.2);
		pop.setToX(1);
		pop.setToY(1);
		pop.setInterpolator(Theme.OVERSHOOT);
		FadeTransition fade = new FadeTransition(Duration.millis(400), label);
		fade.setToValue(0);
		TranslateTransition rise = new TranslateTransition(Duration.millis(400), label);
		rise.setByY(-18);
		SequentialTransition life = new SequentialTransition(pop, new PauseTransition(Duration.millis(message.tone() == BattleCommentary.Tone.DANGER ? 2200 : 1500)),
				new ParallelTransition(fade, rise));
		life.setOnFinished(event -> column.getChildren().remove(label));
		life.play();
		if (message.tone() == BattleCommentary.Tone.DANGER) {
			Timeline blink = new Timeline(new KeyFrame(Duration.millis(160), event -> label.setVisible(!label.isVisible())));
			blink.setCycleCount(12);
			blink.setOnFinished(event -> label.setVisible(true));
			blink.play();
		}
	}

	/**
	 * A more important comment cuts the current one, the others wait their turn
	 */
	private void queueComment(BattleCommentary.Message message) {
		if (this.comment == null || message.priority() > this.comment.priority() && this.commentAge > 0.6) {
			// If it is not time for it, it is dropped
			this.showComment(message);
			return;
		}
		this.commentQueue.add(new QueuedComment(message, this.commentClock));
		this.commentQueue.sort((a, b) -> Integer.compare(b.message().priority(), a.message().priority()));
		while (this.commentQueue.size() > 3) {
			this.commentQueue.remove(this.commentQueue.size() - 1);
		}
	}

	/**
	 * The presenter of a moment of the game (a round that begins, a knock out)
	 * says a line
	 */
	private void announce(String text, int priority) {
		this.queueComment(new BattleCommentary.Message(false, text, -1, BattleCommentary.Tone.NEUTRAL, priority, Topic.SHOW));
	}

	/**
	 * Whether it is time for this comment, by its importance and the time since
	 * the last one
	 */
	private boolean commentAllowed(BattleCommentary.Message message) {
		int level = Math.max(0, Math.min(COMMENT_COOLDOWN.length - 1, message.priority()));
		return this.commentClock - this.lastPresented >= COMMENT_COOLDOWN[level] && this.random.nextDouble() < COMMENT_CHANCE[level];
	}

	/**
	 * Shows the comment with the presenter of its topic
	 *
	 * @return false if it was not time for it and it was dropped
	 */
	private boolean showComment(BattleCommentary.Message message) {
		if (!this.commentAllowed(message)) {
			return false;
		}
		this.lastPresented = this.commentClock;
		this.presenter.present(message.topic(), COMMENT_SECONDS + 0.4);
		this.liveTag.setText("● " + message.topic().presenter.toUpperCase());
		this.comment = message;
		this.commentAge = 0;
		this.commentText.setText(message.text());
		Color color = message.robot() < 0 ? Theme.ACCENT : this.toneColor(message);
		this.ticker.setStyle("-fx-border-color: " + toCss(color) + ";");
		this.ticker.setOpacity(1);
		TranslateTransition slide = new TranslateTransition(Duration.millis(220), this.ticker);
		slide.setFromX(-60);
		slide.setToX(0);
		slide.setInterpolator(Interpolator.EASE_OUT);
		slide.play();
		return true;
	}

	private void tickComments(double dt) {
		this.commentClock += dt;
		this.presenter.tick(dt);
		if (this.comment != null) {
			this.commentAge += dt;
			if (this.commentAge > COMMENT_SECONDS) {
				this.comment = null;
				FadeTransition fade = new FadeTransition(Duration.millis(250), this.ticker);
				fade.setToValue(0);
				fade.play();
			}
		}
		// Old news are not told
		this.commentQueue.removeIf(queued -> this.commentClock - queued.since() > COMMENT_MAX_WAIT);
		if (this.comment == null && !this.commentQueue.isEmpty() && this.phase != Phase.REPLAY) {
			// Not time for it: dropped, the next one may have its turn
			this.showComment(this.commentQueue.remove(0).message());
		}
	}

	// --------------------------------------------------------------- showcase

	/**
	 * Before the first round each robot is presented alone in the arena, like
	 * the fighters of a fighting game, then both face each other
	 */
	private void startShowcase() {
		this.phase = Phase.SHOWCASE;
		this.hudTop.setVisible(false);
		this.showLetterbox(true, false);
		this.audio.setMusicOn(false);
		this.showcaseRobot(0);
	}

	/**
	 * Presents the robot at this position of the battle, or the versus after the
	 * last one
	 */
	private void showcaseRobot(int order) {
		this.stopShowcaseMusic();
		List<BattleRecording.Robot> list = this.recording.getRobots();
		if (order >= list.size()) {
			this.startVersus();
			return;
		}
		this.showcaseOrder = order;
		this.phaseTime = 0;

		BattleRecording.Robot robot = list.get(order);
		RobotView view = this.robots.get(robot.getIndex());
		for (RobotView other : this.robots.values()) {
			other.root.setVisible(other == view);
		}
		this.shotSubject = robot.getIndex();
		this.shotSeed = this.random.nextDouble() * Math.PI * 2;
		this.turntable();

		// A spotlight in the color of the player
		this.flashLight.setTranslateX(0);
		this.flashLight.setTranslateY(-160);
		this.flashLight.setTranslateZ(0);
		this.flashLight.setColor(view.playerColor.deriveColor(0, 0.8, 0.9, 1));

		Opponent opponent = this.opponentOf(robot);
		Node card = this.showcaseCard(robot, opponent, view.playerColor, order);
		this.showcaseLayer.getChildren().setAll(card);
		TranslateTransition slide = new TranslateTransition(Duration.millis(550), card);
		double side = order % 2 == 0 ? -1 : 1;
		slide.setFromX(side * Math.max(1200, this.getWidth()));
		slide.setToX(0);
		slide.setDelay(Duration.millis(700));
		slide.setInterpolator(Interpolator.EASE_OUT);
		slide.setOnFinished(event -> Sfx.slam());
		card.setTranslateX(slide.getFromX());
		slide.play();

		this.audio.whoosh();
		this.showcaseMusic = opponent == null ? null : Sound.fromBytes(opponent.getMusic(), opponent.getFullName());
		if (this.showcaseMusic != null) {
			this.showcaseMusic.play();
		}
		this.cut();
	}

	/**
	 * Both robots where the battle starts, and the VS
	 */
	private void startVersus() {
		this.versus = true;
		this.phaseTime = 0;
		this.showcaseLayer.getChildren().clear();
		this.flashLight.setColor(Color.BLACK);
		for (RobotView view : this.robots.values()) {
			view.root.setVisible(true);
		}
		this.turnPosition = 0;
		this.lastTurnProcessed = -1;
		this.showTurn(this.turns());

		String names = String.join("  ×  ", this.recording.getRobots().stream().map(r -> this.robots.get(r.getIndex()).displayName.toUpperCase()).toList());
		this.showBanner("VS", names + "\n\nARENA: " + this.style.label.toUpperCase());
		String versusLine = "Frente a frente: " + names.replace("  ×  ", " contra ") + "!";
		this.announce(switch (this.stageImportance) {
		case SEMIFINAL -> "Atenção, é uma SEMIFINAL! " + versusLine + " Quem vence vai para a final!";
		case FINAL -> "Atenção, é a FINAL! " + versusLine + " Quem vencer leva a glória!";
		default -> versusLine;
		}, 5);
		this.audio.whoosh();
		Sfx.slam();
		this.shot = Shot.WIDE;
		this.shotTime = 0;
		this.cut();
	}

	private void endShowcase() {
		this.stopShowcaseMusic();
		this.versus = false;
		this.showcaseLayer.getChildren().clear();
		this.flashLight.setColor(Color.BLACK);
		for (RobotView view : this.robots.values()) {
			view.root.setVisible(true);
		}
		this.hideBanner();
		this.stageLabel.setVisible(false);
		this.stageLabel.setManaged(false);
		this.showLetterbox(false);
		this.startRound(0);
	}

	private void stopShowcaseMusic() {
		if (this.showcaseMusic != null) {
			this.showcaseMusic.stop();
			this.showcaseMusic = null;
		}
	}

	/**
	 * The presented robot turns slowly in the center of the arena, aiming its gun
	 * and spinning the radar
	 */
	private void turntable() {
		RobotView view = this.robots.get(this.shotSubject);
		if (view == null) {
			return;
		}
		double t = this.phaseTime;
		double body = this.shotSeed + t * 0.35;
		view.place(0, 0, body, body + 0.7 * Math.sin(t * 1.4), t * 5);
	}

	/**
	 * Comes down from above to a low hero angle, circling and getting closer. The
	 * robot stays on the side opposite to its card.
	 */
	private void showcaseShot(double[] robot) {
		double t = this.phaseTime;
		double crane = Math.max(0, 1 - t / 2.5);
		crane = crane * crane;
		double angle = this.shotSeed + Math.PI - t * 0.3;
		double radius = 175 - Math.min(30, t * 4.5) + 120 * crane;
		double ex = robot[0] + Math.cos(angle) * radius;
		double ez = robot[2] + Math.sin(angle) * radius;
		double ey = -30 - 150 * crane;

		// Screen right of the camera, the look point moves to the side of the card
		double fx = robot[0] - ex;
		double fz = robot[2] - ez;
		double length = Math.max(1, Math.hypot(fx, fz));
		double rightX = fz / length;
		double rightZ = -fx / length;
		double offset = (this.showcaseOrder % 2 == 0 ? -1 : 1) * 34;
		this.setShot(ex, ey, ez, robot[0] + rightX * offset, -20, robot[2] + rightZ * offset, 40, 6);
		this.rollTarget = (this.showcaseOrder % 2 == 0 ? 1 : -1) * 2.5 * crane;
	}

	/**
	 * Logo, name, team, battle cry and authors of the robot
	 */
	private Node showcaseCard(BattleRecording.Robot robot, Opponent opponent, Color color, int order) {
		boolean left = order % 2 == 0;
		Pos alignment = left ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT;
		VBox card = new VBox(10);
		card.setAlignment(alignment);

		Label tag = new Label("JOGADOR " + (order + 1));
		tag.getStyleClass().add("showcase-section");
		tag.setTextFill(color);
		card.getChildren().add(tag);

		if (opponent != null && opponent.getImage() != null) {
			ImageView logo = new ImageView(new Image(new ByteArrayInputStream(opponent.getImage()), 260, 180, true, true));
			card.getChildren().add(logo);
		}

		Label name = new Label(opponent == null ? robot.getName() : opponent.getName().toUpperCase());
		name.setWrapText(true);
		name.getStyleClass().add("showcase-name");
		name.setTextFill(color);
		name.setEffect(Theme.glow(color, 18));
		card.getChildren().add(name);

		if (opponent != null) {
			Label team = new Label("Time " + opponent.getTeam());
			team.getStyleClass().add("showcase-team");
			card.getChildren().add(team);

			if (opponent.getBattleCry() != null && !opponent.getBattleCry().isBlank()) {
				Label cry = new Label("“" + opponent.getBattleCry() + "”");
				cry.setWrapText(true);
				cry.getStyleClass().add("showcase-cry");
				card.getChildren().add(cry);
			}

			if (!opponent.getAuthors().isEmpty()) {
				Label section = new Label("EQUIPE");
				section.getStyleClass().add("showcase-section");
				card.getChildren().add(section);
				for (Author author : opponent.getAuthors()) {
					Label label = new Label("▸ " + author.getName());
					label.getStyleClass().add("showcase-author");
					card.getChildren().add(label);
				}
			}
		}
		for (Node node : card.getChildren()) {
			if (node instanceof Label label) {
				label.setAlignment(alignment);
				label.setTextAlignment(left ? javafx.scene.text.TextAlignment.LEFT : javafx.scene.text.TextAlignment.RIGHT);
			}
		}

		card.setStyle("-fx-background-color: rgba(8, 10, 20, 0.72); -fx-padding: 28 36 28 36; -fx-border-color: " + toCss(color) + "; -fx-border-width: "
				+ (left ? "0 0 0 6" : "0 6 0 0") + ";");
		card.maxWidthProperty().bind(this.widthProperty().multiply(0.36));
		card.setMaxHeight(Region.USE_PREF_SIZE);
		StackPane.setAlignment(card, alignment);
		StackPane.setMargin(card, new Insets(0, 70, 0, 70));
		return card;
	}

	// ----------------------------------------------------------------- replay

	/**
	 * A robot died: the action stops for a moment, then the last second is
	 * shown again in slow motion
	 */
	private void freezeForReplay(int robot, int turn) {
		this.phase = Phase.FREEZE;
		this.phaseTime = 0;
		this.deadRobot = robot;
		this.deathTurn = turn;
		this.replayFrom = Math.max(0, turn - REPLAY_BEFORE);
		this.replayTo = Math.min(this.turns().size() - 1, turn + REPLAY_AFTER);

		// The bullet that killed it, if one did
		this.fatalBullet = -1;
		this.impactTurn = -1;
		List<Turn> turns = this.turns();
		// (a bullet stays in the records for a while after its hit, exploding: the
		// hit is the turn it changes to HIT_VICTIM, the last one before the death)
		for (int t = Math.min(turns.size() - 1, turn + 1); t >= Math.max(1, turn - 3) && this.fatalBullet < 0; t--) {
			for (Bullet bullet : turns.get(t).bullets()) {
				Bullet before = findBullet(turns.get(t - 1), bullet.id());
				if (bullet.state() == BattleRecording.BULLET_HIT_VICTIM && bullet.victim() == robot && (before == null || !before.isHit())) {
					this.fatalBullet = bullet.id();
					this.impactTurn = t;
					break;
				}
			}
		}

		// Holds on the explosion, far enough to see all of it
		this.shot = Shot.IMPACT;
		this.shotSubject = robot;
		this.shotTime = 0;
		this.shotDuration = FREEZE_SECONDS + 1;
		this.closeDistance = 190;
		this.shotSide = this.random.nextBoolean() ? 1 : -1;
	}

	private void startReplay() {
		this.phase = Phase.REPLAY;
		this.phaseTime = 0;
		this.replayed = true;
		this.effectRate = 0.3;
		this.audio.whoosh();
		this.audio.setSlowMotion(true);
		this.showLetterbox(true);

		// Back to before the death
		this.turnPosition = this.replayFrom;
		this.lastTurnProcessed = this.replayFrom - 1;
		this.bulletsSeen.clear();
		this.bulletsExploded.clear();
		this.clearBullets();
		this.effects.getChildren().clear();
		for (RobotView view : this.robots.values()) {
			view.reset();
		}
		// Bullets already flying when the replay starts are not new shots, and the
		// ones still exploding there hit before it
		if (this.replayFrom > 0) {
			for (Bullet bullet : this.turns().get(this.replayFrom - 1).bullets()) {
				this.bulletsSeen.add(bullet.id());
				if (bullet.isHit()) {
					this.bulletsExploded.add(bullet.id());
				}
			}
		}

		this.shot = Shot.FATAL;
		this.shotSubject = this.deadRobot;
		this.shotTime = 0;
		this.shotDuration = 1000;
		this.shotSide = this.random.nextBoolean() ? 1 : -1;
		this.showTurn(this.turns());
		this.cut();
	}

	private void endReplay() {
		if (this.phase != Phase.REPLAY && this.phase != Phase.FREEZE) {
			return;
		}
		this.replayed = true;
		this.effectRate = 1;
		for (RobotView view : this.robots.values()) {
			view.running.forEach(animation -> animation.setRate(1));
		}
		this.audio.setSlowMotion(false);
		this.showLetterbox(false);

		// Goes on from the end of the replay, with everything in its final state
		this.turnPosition = Math.max(this.turnPosition, this.replayTo);
		List<Turn> turns = this.turns();
		this.showTurn(turns);
		RobotView dead = this.robots.get(this.deadRobot);
		if (dead != null && !dead.dead) {
			dead.destroy();
		}
		this.phase = Phase.PLAYING;
		this.phaseTime = 0;
		this.pickShot(true);
	}

	// ----------------------------------------------------------------- camera

	/**
	 * The camera jumps to the shot, like a cut in a movie
	 */
	private void cut() {
		this.directShot(0);
		System.arraycopy(this.eyeTarget, 0, this.eye, 0, 3);
		System.arraycopy(this.lookTarget, 0, this.look, 0, 3);
		this.fov = this.fovTarget;
		this.roll = this.rollTarget;
	}

	/**
	 * Chooses the next shot: faster cuts and closer shots when the fight is
	 * tense
	 */
	private void pickShot(boolean cut) {
		List<Integer> alive = this.robots.entrySet().stream().filter(e -> !e.getValue().dead).map(Map.Entry::getKey).toList();
		double tension = Math.min(1, 0.55 * this.hitTension * 4 + this.lowestEnergyTension());

		Shot[] options = { Shot.WIDE, Shot.CHASE, Shot.CHASE, Shot.LOW_TRACK, Shot.LOW_TRACK, Shot.CLOSE, Shot.CRANE };
		Shot next;
		do {
			next = options[this.random.nextInt(options.length)];
			if (tension > 0.5 && next == Shot.WIDE) {
				next = Shot.CLOSE;
			}
		} while (next == this.shot && this.random.nextDouble() < 0.8);

		this.shot = next;
		this.shotTime = 0;
		this.shotDuration = 5.5 - 3 * tension + this.random.nextDouble() * 1.5;
		this.shotSubject = alive.isEmpty() ? 0 : alive.get(this.random.nextInt(alive.size()));
		this.shotSide = this.random.nextBoolean() ? 1 : -1;
		this.shotSeed = this.random.nextDouble() * Math.PI * 2;
		this.closeDistance = 100;
		if (cut) {
			this.cut();
		}
	}

	private double lowestEnergyTension() {
		double lowest = 100;
		for (RobotView view : this.robots.values()) {
			if (!view.dead) {
				lowest = Math.min(lowest, view.energy);
			}
		}
		return 1 - Math.max(0, Math.min(100, lowest)) / 100;
	}

	/**
	 * A strong hit may cut to the victim
	 */
	private void impactCut(int victim, double power) {
		if (this.viewMode != ViewMode.CINEMATIC || this.phase != Phase.PLAYING || this.shotTime < 1.2 || this.random.nextDouble() > 0.25 + power * 0.15) {
			return;
		}
		this.shot = Shot.IMPACT;
		this.shotSubject = victim;
		this.shotTime = 0;
		this.shotDuration = 1.6;
		this.closeDistance = 90;
	}

	private void updateCamera(double dt) {
		this.shotTime += dt;
		if (this.viewMode == ViewMode.CINEMATIC || this.phase == Phase.REPLAY || this.phase == Phase.FREEZE || this.phase == Phase.SHOWCASE || this.phase == Phase.FLYBY) {
			if (this.shotTime > this.shotDuration && this.phase == Phase.PLAYING) {
				this.pickShot(false);
			}
			this.directShot(dt);
		} else {
			this.freeCamera(dt);
		}

		double ease = 1 - Math.exp(-this.follow * dt);
		for (int i = 0; i < 3; i++) {
			this.eye[i] += (this.eyeTarget[i] - this.eye[i]) * ease;
			this.look[i] += (this.lookTarget[i] - this.look[i]) * ease;
		}
		this.fov += (this.fovTarget - this.fov) * ease;
		this.roll += (this.rollTarget - this.roll) * ease;

		// Shake of the explosions
		this.shake = Math.min(40, this.shake) * Math.exp(-5 * dt);
		double sx = (this.random.nextDouble() - 0.5) * this.shake;
		double sy = (this.random.nextDouble() - 0.5) * this.shake;
		double sz = (this.random.nextDouble() - 0.5) * this.shake;

		double fx = this.look[0] - this.eye[0];
		double fy = this.look[1] - this.eye[1];
		double fz = this.look[2] - this.eye[2];
		double distance = Math.max(1, Math.sqrt(fx * fx + fy * fy + fz * fz));

		this.camera.setFieldOfView(this.fov);
		this.cameraLook.setX(this.look[0] + sx);
		this.cameraLook.setY(this.look[1] + sy);
		this.cameraLook.setZ(this.look[2] + sz);
		this.cameraYaw.setAngle(Math.toDegrees(Math.atan2(fx, fz)));
		this.cameraPitch.setAngle(Math.toDegrees(Math.asin(-fy / distance)));
		this.cameraRoll.setAngle(this.roll + (this.random.nextDouble() - 0.5) * this.shake * 0.1);
		this.cameraDistance.setZ(-distance);
	}

	/**
	 * Free (orbit), isometric and top views
	 */
	private void freeCamera(double dt) {
		if (this.viewMode == ViewMode.FREE && Math.abs(this.yawSpeed) > 0.1) {
			// Momentum after a drag
			this.targetYaw += this.yawSpeed * dt;
			this.yawSpeed *= Math.exp(-4 * dt);
		}
		double ease = 1 - Math.exp(-5 * dt);
		this.yaw += (this.targetYaw - this.yaw) * ease;
		double wantedPitch = this.viewMode == ViewMode.FREE ? this.freePitch : this.viewMode.pitch;
		this.pitch += (wantedPitch - this.pitch) * ease;
		this.zoom += (this.targetZoom - this.zoom) * ease;

		double aspect = Math.max(0.5, this.getWidth() / Math.max(1, this.getHeight()));
		double tangent = Math.tan(Math.toRadians(this.viewMode.fov / 2));
		double distance;
		if (this.viewMode == ViewMode.TOP_DOWN) {
			distance = Math.max(this.fieldHeight / 2 * 1.3, this.fieldWidth / 2 * 1.08 / aspect) / tangent;
		} else {
			distance = 0.5 * Math.hypot(this.fieldWidth, this.fieldHeight) * 0.78 / tangent / Math.min(1, aspect);
		}
		distance *= this.zoom;

		double yawRad = Math.toRadians(this.yaw);
		double pitchRad = Math.toRadians(this.pitch);
		// Forward of the camera for this yaw and pitch
		double fx = Math.cos(pitchRad) * Math.sin(yawRad);
		double fy = -Math.sin(pitchRad);
		double fz = Math.cos(pitchRad) * Math.cos(yawRad);
		this.lookTarget[0] = 0;
		this.lookTarget[1] = -10;
		this.lookTarget[2] = 0;
		this.eyeTarget[0] = -fx * distance;
		this.eyeTarget[1] = -10 - fy * distance;
		this.eyeTarget[2] = -fz * distance;
		this.fovTarget = this.viewMode.fov;
		this.rollTarget = 0;
		this.follow = 6;
	}

	/**
	 * Where the cinematic camera is and looks, for the current shot
	 */
	private void directShot(double dt) {
		RobotView subject = this.robots.get(this.shotSubject);
		RobotView other = this.robots.values().stream().filter(v -> v != subject).findFirst().orElse(subject);
		double[] a = subject == null ? new double[] { 0, 0, 0 } : subject.position();
		double[] b = other == null ? a : other.position();
		this.rollTarget = 0;

		if (this.phase == Phase.FLYBY) {
			// The camera stays on the critter, following it, and then the trip begins
			double hold = this.flyByHold();
			if (this.phaseTime < hold) {
				this.flyFocus = this.flyFocusSource.get();
			}
			this.flyByPose(Math.max(0, Math.min(1, (this.phaseTime - hold) / FLYBY_SECONDS)));
			return;
		}
		if (this.phase == Phase.SHOWCASE && !this.versus) {
			this.showcaseShot(subject == null ? new double[] { 0, 0, 0 } : subject.position());
			return;
		}

		if (this.shot == Shot.FATAL) {
			this.fatalShot(a);
			this.keepClear();
			this.eyeTarget[1] = Math.min(this.eyeTarget[1], -10);
			return;
		}

		// When a bot wins, focus the camera directly on the victor!
		List<RobotView> livingRobots = this.robots.values().stream().filter(v -> !v.dead).toList();
		boolean roundOver = this.phase == Phase.ROUND_END || this.phase == Phase.FINISHED;
		RobotView winner = null;
		// Not while a death is held on screen: the wreck is framed then, see freezeForReplay
		if (this.robots.size() >= 2 && livingRobots.size() == 1 && this.phase != Phase.FREEZE) {
			winner = livingRobots.get(0);
		} else if (roundOver && this.robots.containsKey(this.shotSubject)) {
			winner = this.robots.get(this.shotSubject);
		}

		if (winner != null) {
			this.cinematicOrbitAngle += dt * 0.35;
			double[] winPos = winner.position();
			double winDist = 175.0; // Heroic victory framing
			double winHeight = -42.0; // Elevated view looking proud at the victor
			double winAngle = this.cinematicOrbitAngle;

			this.setShot(winPos[0] + Math.cos(winAngle) * winDist, winHeight, winPos[2] + Math.sin(winAngle) * winDist,
					winPos[0], -18, winPos[2], 44.0, 3.5);
			this.rollTarget = 0;
			this.keepClear();
			this.eyeTarget[1] = Math.min(this.eyeTarget[1], -10);
			return;
		}

		this.frameFighters(dt, a, b);
	}

	/**
	 * Frames both fighters: the camera circles them on a flank, looking at the
	 * middle between them, far enough away to keep both in view
	 */
	private void frameFighters(double dt, double[] a, double[] b) {
		double[] mid = { (a[0] + b[0]) / 2, -16, (a[2] + b[2]) / 2 };
		double dx = b[0] - a[0];
		double dz = b[2] - a[2];
		double separation = Math.max(10, Math.hypot(dx, dz));
		double ux = dx / separation;
		double uz = dz / separation;
		// Perpendicular to the line between them
		double px = -uz * this.shotSide;
		double pz = ux * this.shotSide;
		this.cinematicOrbitAngle += dt * 0.25;

		// Calculate distance required to guarantee both robots are comfortably inside the field of view
		// Fit the bounding circle encompassing both combatants to the horizontal field of view
		double aspect = Math.max(0.5, this.getWidth() / Math.max(1.0, this.getHeight()));
		double fovVal = 48.0;
		double vFovRad = Math.toRadians(fovVal);
		double hFovRad = 2.0 * Math.atan(Math.tan(vFovRad / 2.0) * aspect);

		// Boxing referee camera: continuously seeks the best angle of the fight,
		// circling the combatants on the flank to keep both clearly in sight.
		double curDx = this.eye[0] - mid[0];
		double curDz = this.eye[2] - mid[2];
		double curAngle = Math.atan2(curDz, curDx);

		// The two flanks perpendicular to the combat line
		double flankAngle1 = Math.atan2(pz, px);
		double flankAngle2 = Math.atan2(-pz, -px);

		// Maintain continuity with the referee's current position around the fighters
		double diff1 = Math.abs(Math.atan2(Math.sin(flankAngle1 - curAngle), Math.cos(flankAngle1 - curAngle)));
		double diff2 = Math.abs(Math.atan2(Math.sin(flankAngle2 - curAngle), Math.cos(flankAngle2 - curAngle)));
		double baseFlank = diff1 <= diff2 ? flankAngle1 : flankAngle2;

		// Dynamic 3/4 angle & referee footwork: gently pacing to keep an unobstructed view
		double pacing = Math.sin(this.cinematicOrbitAngle) * 0.16;
		double bestAngle = baseFlank + pacing;

		// Frame both combatants with generous margins so neither leaves the view nor feels cramped
		double sphereRadius = separation * 0.5 + 80.0;
		double fitDist = sphereRadius / Math.tan(hFovRad * 0.35);

		// Well-balanced ringside distance: neither too zoomed out nor right in the robot treads
		double targetDist = Math.max(420.0, fitDist);
		if (this.shot == Shot.IMPACT || this.hitTension > 0.15) {
			targetDist *= 0.95;
		}

		// Elevation: ringside referee perspective looking slightly down into the arena
		double eyeY = -Math.max(65.0, targetDist * 0.23);

		// Roll target: subtle dynamic tilt on impacts, otherwise level
		this.rollTarget = (this.shot == Shot.IMPACT ? 2.0 : 0.5) * Math.sin(this.cinematicOrbitAngle);

		// Aim directly at the midpoint between the fighters
		this.setShot(mid[0] + Math.cos(bestAngle) * targetDist, eyeY, mid[2] + Math.sin(bestAngle) * targetDist,
				mid[0], -16, mid[2], fovVal, 3.5);
		this.keepClear();
		// Never under the floor
		this.eyeTarget[1] = Math.min(this.eyeTarget[1], -10);
	}

	/**
	 * The camera never goes inside a robot: if one is too close, the camera
	 * moves away from it and up
	 */
	private void keepClear() {
		for (RobotView view : this.robots.values()) {
			double[] position = view.position();
			double dx = this.eyeTarget[0] - position[0];
			double dz = this.eyeTarget[2] - position[2];
			double distance = Math.hypot(dx, dz);
			double minimum = 65;
			if (distance < minimum && this.eyeTarget[1] > -80) {
				if (distance < 1) {
					dx = 1;
					dz = 0;
					distance = 1;
				}
				this.eyeTarget[0] = position[0] + dx / distance * minimum;
				this.eyeTarget[2] = position[2] + dz / distance * minimum;
				this.eyeTarget[1] = Math.min(this.eyeTarget[1], -45);
			}
		}
	}

	/**
	 * The replay: the camera rides behind the fatal bullet and then watches the
	 * robot explode from the side
	 */
	private void fatalShot(double[] victim) {
		Group bullet = this.bullets.get(this.fatalBullet);
		double turn = this.turnPosition;
		if (bullet != null && this.impactTurn >= 0 && turn < this.impactTurn) {
			double bx = bullet.getTranslateX();
			double bz = bullet.getTranslateZ();
			double dx = victim[0] - bx;
			double dz = victim[2] - bz;
			double length = Math.max(1, Math.hypot(dx, dz));
			double ux = dx / length;
			double uz = dz / length;
			this.setShot(bx - ux * 120 - uz * 22 * this.shotSide, -30, bz - uz * 120 + ux * 22 * this.shotSide, bx + ux * 140, -20, bz + uz * 140, 38, 9);
			this.rollTarget = -5 * this.shotSide;
		} else {
			// Side view of the victim, slowly getting closer
			double angle = this.shotSeed + this.phaseTime * 0.15;
			double radius = 175 - Math.min(35, this.phaseTime * 5);
			this.setShot(victim[0] + Math.cos(angle) * radius, -26, victim[2] + Math.sin(angle) * radius, victim[0], -18, victim[2], 36, 4);
			this.rollTarget = 4 * this.shotSide;
		}
	}

	private void setShot(double ex, double ey, double ez, double lx, double ly, double lz, double fov, double follow) {
		this.eyeTarget[0] = ex;
		this.eyeTarget[1] = ey;
		this.eyeTarget[2] = ez;
		this.lookTarget[0] = lx;
		this.lookTarget[1] = ly;
		this.lookTarget[2] = lz;
		this.fovTarget = fov;
		this.follow = follow;
	}

	/**
	 * The name and energy balloons follow their robots on the screen
	 */
	private void placeBalloons() {
		boolean show = this.phase != Phase.LOADING && this.phase != Phase.REPLAY && this.phase != Phase.SHOWCASE && this.phase != Phase.FLYBY;
		for (RobotView view : this.robots.values()) {
			Point3D scene = view.root.localToScene(new Point3D(0, -62, 0), true);
			if (scene == null || !show) {
				view.balloon.setVisible(false);
				continue;
			}
			Point2D local = this.balloons.sceneToLocal(scene.getX(), scene.getY());
			boolean inside = local != null && local.getX() > -100 && local.getY() > -100 && local.getX() < this.getWidth() + 100 && local.getY() < this.getHeight() + 100;
			view.balloon.setVisible(inside);
			if (inside) {
				view.balloon.autosize();
				view.balloon.relocate(local.getX() - view.balloon.getWidth() / 2, local.getY() - view.balloon.getHeight());
			}
		}
	}

	private static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	/**
	 * Interpolates angles (radians) the short way
	 */
	private static double lerpAngle(double a, double b, double t) {
		double difference = Math.IEEEremainder(b - a, Math.PI * 2);
		return a + difference * t;
	}

	// ------------------------------------------------------------------ robot

	/**
	 * A low poly tank: treads, body, turret with the gun and the radar, each part
	 * with the color the robot chose
	 */
	/** The links of a tread, which go round the belt of their side of the hull */
	private static final int TREAD_LINKS = 30;
	/** The belt in the side view, as a closed outline: its back, up, over the top, down and back along the bottom */
	private static final double[][] BELT = { { -20, -1 }, { -21.5, -4 }, { -19.5, -9 }, { 18.5, -9 }, { 21.5, -4 }, { 20, -1 } };
	/** The length of the outline: one turn of the belt */
	private static final double BELT_LENGTH = beltLength();
	/** The width of a belt, sideways, from its inner face to its outer one, where the links are */
	private static final double BELT_INNER = 8;
	private static final double BELT_OUTER = 17.5;
	private static final double BELT_CENTER = (BELT_INNER + BELT_OUTER) / 2;
	private static final double BELT_WIDTH = BELT_OUTER - BELT_INNER;

	private static double beltLength() {
		double length = 0;
		for (int i = 0; i < BELT.length; i++) {
			double[] a = BELT[i];
			double[] b = BELT[(i + 1) % BELT.length];
			length += Math.hypot(b[0] - a[0], b[1] - a[1]);
		}
		return length;
	}

	/**
	 * Where a link is on the belt, s from 0 to BELT_LENGTH along its outline: its z
	 * and its y in the body, and the angle of the outline there (turned round x).
	 * The links follow the shape of the track, its chamfers included.
	 */
	private static double[] beltPoint(double s) {
		double rest = s;
		for (int i = 0; i < BELT.length - 1; i++) {
			double[] a = BELT[i];
			double[] b = BELT[i + 1];
			double dz = b[0] - a[0];
			double dy = b[1] - a[1];
			double length = Math.hypot(dz, dy);
			if (rest <= length) {
				return new double[] { a[0] + dz * rest / length, a[1] + dy * rest / length, Math.atan2(-dy, dz) };
			}
			rest -= length;
		}
		// The last piece of the outline, back to its start
		double[] a = BELT[BELT.length - 1];
		double[] b = BELT[0];
		double dz = b[0] - a[0];
		double dy = b[1] - a[1];
		double length = Math.hypot(dz, dy);
		return new double[] { a[0] + dz * rest / length, a[1] + dy * rest / length, Math.atan2(-dy, dz) };
	}

	/**
	 * A point on one side of the hull, in the arena (side 0 is the left)
	 */
	private static double[] sidePoint(double x, double z, double heading, int side) {
		double sideways = side == 0 ? -BELT_CENTER : BELT_CENTER;
		return new double[] { x + sideways * Math.cos(heading), z - sideways * Math.sin(heading) };
	}

	private final class RobotView {
		final Group root = new Group();
		final Group tank = new Group();
		final Group body = new Group();
		final Group turret = new Group();
		final Group radar = new Group();
		final Rotate bodyRotate = new Rotate(0, Rotate.Y_AXIS);
		final Rotate gunRotate = new Rotate(0, Rotate.Y_AXIS);
		final Rotate radarRotate = new Rotate(0, Rotate.Y_AXIS);
		final PhongMaterial bodyMaterial;
		final PhongMaterial gunMaterial;
		final Opponent opponent;
		final Color bodyColor;
		final Color gunColor;
		final Color playerColor;
		final Group debris;
		final VBox balloon = new VBox(4);
		final Region energyFill = new Region();
		final Label nameLabel;
		final String displayName;
		final List<Animation> running = new ArrayList<Animation>();
		/** Parts that shine by themselves, and the pictures that make them shine */
		final List<PhongMaterial> glows = new ArrayList<PhongMaterial>();
		final List<Image> glowMaps = new ArrayList<Image>();
		/** Particles still owed by the emitters */
		double flameDebt;
		double smokeDebt;
		double sparkDebt;
		/** Where it was in the last frame and how fast it goes, for the flames */
		double lastX = Double.NaN;
		double lastZ;
		double speedX;
		double speedZ;
		boolean dead = false;
		double energy = 100;
		/** The links of each tread, left side first */
		final MeshView[][] treadLinks = new MeshView[2][TREAD_LINKS];
		final Rotate[][] treadTurns = new Rotate[2][TREAD_LINKS];
		/** How far each tread has slid along its belt */
		final double[] treadShift = new double[2];
		/** Where it was at the last frame, for the rolling */
		double rollX = Double.NaN;
		double rollZ;
		double rollHeading;
		/** How fast the tracks run (arena units a second) and the turret turns (degrees a second), for the sound */
		double treadSpeed;
		double turretSpeed;
		double lastGun = Double.NaN;

		RobotView(BattleRecording.Robot robot, Opponent opponent, int order) {
			this.opponent = opponent;
			this.playerColor = PLAYER_COLORS[order % PLAYER_COLORS.length];
			this.bodyColor = argb(robot.getBodyColor(), this.playerColor);
			this.gunColor = argb(robot.getGunColor(), this.bodyColor);
			this.bodyMaterial = paint(this.bodyColor);
			this.gunMaterial = paint(this.gunColor);
			PhongMaterial radarMaterial = paint(argb(robot.getRadarColor(), Color.web("#ffd400")));
			PhongMaterial dark = material(Color.web("#171b26"));
			PhongMaterial plateMaterial = material(Color.web("#2d3345"));
			PhongMaterial steel = material(Color.web("#6c7488"));
			steel.setSpecularPower(50);
			PhongMaterial cyan = this.glow(Color.web("#38e6ff"));
			PhongMaterial identity = this.glow(this.playerColor);
			PhongMaterial lamp = this.glow(Color.web("#fff6c8"));
			PhongMaterial warning = this.glow(Color.web("#ff3b3b"));

			// A modern tank of angular armor, pointing to +Z (north when the heading
			// is 0): a low wedge hull, tracks hidden by skirts of hexagonal plates, a
			// long flat turret and a very long gun. Flat facets, a few meshes.
			this.body.getTransforms().add(this.bodyRotate);
			LowPoly hull = new LowPoly();
			LowPoly tracks = new LowPoly();
			LowPoly armor = new LowPoly();
			LowPoly plates = new LowPoly();
			LowPoly glowCyan = new LowPoly();
			LowPoly glowIdentity = new LowPoly();
			LowPoly glowWarning = new LowPoly();
			LowPoly glowLamp = new LowPoly();
			// The hull: a wedge, sloped in front and behind, with a deck over it
			hull.prismZY(new double[][] { { -21, -7 }, { -22.5, -9.5 }, { -20, -13 }, { 14, -13.5 }, { 22, -10 }, { 23, -7.5 } }, -13.5, 13.5);
			hull.frustum(new double[][] { { -11.5, -17 }, { 11.5, -17 }, { 11.5, 9 }, { -11.5, 9 } }, -13.4, -15.2, 0.9);
			tracks.box(0, -14.5, -17.5, 14, 2.2, 6); // engine deck, dark
			for (int side = -1; side <= 1; side += 2) {
				// Track with chamfered ends, hidden above by the skirt
				tracks.prismZY(BELT, Math.min(side * BELT_INNER, side * BELT_OUTER), Math.max(side * BELT_INNER, side * BELT_OUTER));
				int index = side < 0 ? 0 : 1;
				// The links of the belt, each one turned to the belt where it is
				for (int link = 0; link < TREAD_LINKS; link++) {
					LowPoly piece = new LowPoly();
					piece.box(0, 0, 0, BELT_WIDTH, 1.2, 2.4);
					MeshView linkView = piece.view(dark);
					Rotate turn = new Rotate(0, Rotate.X_AXIS);
					linkView.getTransforms().add(turn);
					this.body.getChildren().add(linkView);
					this.treadLinks[index][link] = linkView;
					this.treadTurns[index][link] = turn;
				}
				// Hexagonal armor under the skirt, fixed to the hull
				for (double z = -15; z <= 15; z += 6) {
					armor.hexPlate(z, -3, 2.6, 2.6, Math.min(side * (BELT_OUTER + 0.2), side * (BELT_OUTER + 0.9)), Math.max(side * (BELT_OUTER + 0.2), side * (BELT_OUTER + 0.9)));
				}
				// The skirt leans out at the top
				hull.prismXY(new double[][] { { side * 13, -5 }, { side * 17.5, -5 }, { side * 19.5, -12.5 }, { side * 13, -12.5 } }, -19, 19);
				// Hexagonal plates on it, in two rows
				for (int row = 0; row < 2; row++) {
					double y = row == 0 ? -10 : -7.2;
					double face = 17.5 + (-5 - y) / 7.5 * 2;
					for (double z = row == 0 ? -15 : -12; z <= 16; z += 6) {
						plates.hexPlate(z, y, 2.9, 2.5, Math.min(side * (face - 0.2), side * (face + 0.5)), Math.max(side * (face - 0.2), side * (face + 0.5)));
					}
				}
				// A strip of light in the color of the player, lamps and rear lights
				glowIdentity.box(side * 19.7, -11.2, -13, 0.7, 0.9, 9);
				glowLamp.box(side * 8.5, -10.4, 22.3, 3, 1.2, 1);
				glowWarning.box(side * 9, -10, -22.6, 3, 1.2, 1);
			}
			glowCyan.box(0, -9, 22.9, 14, 0.9, 0.6); // visor on the front
			for (double x = -4; x <= 4; x += 4) {
				glowCyan.box(x, -14.9, -19.5, 2, 0.5, 2.4); // vents on the engine deck
			}
			this.body.getChildren().addAll(hull.view(this.bodyMaterial), tracks.view(dark), armor.view(steel), plates.view(plateMaterial), glowCyan.view(cyan), glowIdentity.view(identity),
					glowWarning.view(warning), glowLamp.view(lamp));

			// Turret and the gun turn on their own: a long wedge, a flat wing over the
			// gun, a remote weapon station and antennas
			this.turret.getTransforms().add(this.gunRotate);
			LowPoly shell = new LowPoly();
			LowPoly barrel = new LowPoly();
			LowPoly turretPlates = new LowPoly();
			LowPoly turretCyan = new LowPoly();
			LowPoly turretWarning = new LowPoly();
			shell.frustum(new double[][] { { -9, -12 }, { 9, -12 }, { 12, -3 }, { 10, 6 }, { 4, 12 }, { -4, 12 }, { -10, 6 }, { -12, -3 } }, -14.8, -19.5, 0.72);
			shell.prismZY(new double[][] { { -9, -19.5 }, { -9, -21 }, { 2, -21.4 }, { 11, -19.5 } }, -5, 5);
			shell.frustum(new double[][] { { -3.6, 6 }, { 3.6, 6 }, { 0.9, 36 }, { -0.9, 36 } }, -20.2, -21.2, 1); // wing over the gun
			shell.box(0, -17.4, 12, 7, 4.6, 5); // gun mantlet
			// The flanks, with plates like the skirts
			for (int side = -1; side <= 1; side += 2) {
				for (int i = 0; i < 3; i++) {
					double z = -4.5 + i * 4.5;
					double y = i == 1 ? -16.2 : -17.9;
					double t = (y + 14.8) / -4.7;
					double edge = z < -3 ? 9 + (z + 12) / 3 : 12 - (z + 3) * 2 / 9;
					double face = edge * (1 - 0.28 * t);
					turretPlates.hexPlate(z, y, 2.3, 1.9, Math.min(side * (face - 0.4), side * (face + 0.45)), Math.max(side * (face - 0.4), side * (face + 0.45)));
				}
				turretCyan.box(side * 5.2, -17.6, 11.5, 1.8, 1.2, 1); // sensors on the cheeks
				turretWarning.cylinderY(side * 7, -9, 0.9, -26, -26.9, 5); // lights on the antennas
				barrel.cylinderY(side * 7, -9, 0.3, -19, -26, 5);
			}
			// The gun: a very long barrel with a fume extractor and a muzzle ring
			barrel.cylinderZ(0, -17.2, 1.35, 12, 43, 6);
			barrel.cylinderZ(0, -17.2, 1.95, 21, 27, 6);
			barrel.cylinderZ(0, -17.2, 1.8, 40, 43.6, 6);
			turretCyan.cylinderZ(0, -17.2, 2, 43.6, 44.1, 6);
			// Remote weapon station on the roof and a commander sight
			shell.box(-4.6, -22.4, -4, 4.4, 2.2, 4.4);
			barrel.cylinderZ(-4.6, -22.6, 0.5, -1.5, 3.5, 5);
			turretCyan.box(-4.6, -23.2, -1.6, 1.4, 0.9, 0.6);
			shell.frustum(new double[][] { { 3, -4 }, { 6, -4 }, { 6, -1 }, { 3, -1 } }, -21, -22.6, 0.7);
			turretCyan.box(4.5, -22, -0.8, 1.6, 0.8, 0.4);
			this.turret.getChildren().addAll(shell.view(this.gunMaterial), barrel.view(dark), turretPlates.view(plateMaterial), turretCyan.view(cyan), turretWarning.view(warning));

			// The radar: a flat panel on a mast, turning over the roof
			this.radar.getTransforms().add(this.radarRotate);
			LowPoly panel = new LowPoly();
			LowPoly panelCyan = new LowPoly();
			LowPoly mast = new LowPoly();
			mast.cylinderY(0, 0, 0.8, -24, -21, 6);
			panel.box(0, -26, 0, 8, 3.6, 1.2);
			panelCyan.box(0, -26, 0.7, 6, 0.6, 0.3);
			mast.cylinderY(3, 0, 0.25, -31, -27.8, 5);
			this.radar.getChildren().addAll(panel.view(radarMaterial), mast.view(dark), panelCyan.view(cyan));

			this.tank.getChildren().addAll(this.body, this.turret, this.radar);

			// The pieces thrown by the blast stay here, beside the wreck
			this.debris = new Group();
			this.root.getChildren().addAll(this.tank, this.debris);

			// Balloon over the robot
			this.displayName = opponent == null ? robot.getName() : opponent.getName();
			this.nameLabel = new Label(this.displayName);
			this.nameLabel.getStyleClass().add("arena-balloon-name");
			this.nameLabel.setTextFill(this.playerColor);
			this.energyFill.getStyleClass().add("arena-balloon-energy");
			this.energyFill.setStyle("-fx-background-color: " + toCss(this.playerColor) + ";");
			StackPane energyBar = new StackPane(this.energyFill);
			energyBar.getStyleClass().add("arena-balloon-bar");
			StackPane.setAlignment(this.energyFill, Pos.CENTER_LEFT);
			HBox nameRow = new HBox(6, this.nameLabel);
			nameRow.setAlignment(Pos.CENTER);
			if (opponent != null && opponent.getImage() != null) {
				ImageView logo = new ImageView(new Image(new ByteArrayInputStream(opponent.getImage()), 40, 30, true, true));
				nameRow.getChildren().add(0, logo);
			}
			this.balloon.getChildren().addAll(nameRow, energyBar);
			this.balloon.getStyleClass().add("arena-balloon");
			this.balloon.setStyle("-fx-border-color: " + toCss(this.playerColor) + ";");
			this.balloon.setAlignment(Pos.CENTER);
			this.balloon.setVisible(false);
		}

		double[] position() {
			return new double[] { this.root.getTranslateX(), 0, this.root.getTranslateZ() };
		}

		void place(double x, double z, double body, double gun, double radar) {
			this.root.setTranslateX(x);
			this.root.setTranslateZ(z);
			this.bodyRotate.setAngle(Math.toDegrees(body));
			if (!this.dead) {
				this.gunRotate.setAngle(Math.toDegrees(gun));
				this.radarRotate.setAngle(Math.toDegrees(radar));
			}
		}

		void setEnergy(double energy) {
			this.energy = energy;
			this.energyFill.setMaxWidth(90 * Math.max(0, Math.min(1, energy / 100)));
		}

		/**
		 * Below half of the energy the robot catches fire. The less energy, the more
		 * burning spots, the stronger the flames and the thicker the smoke.
		 *
		 * @param dt
		 *            seconds, slower in the replay
		 */
		void burn(double dt) {
			double damage = this.dead ? 0 : Math.max(0, Math.min(1, (BURN_ENERGY - this.energy) / BURN_ENERGY));
			if (damage <= 0 || !this.root.isVisible()) {
				this.flameDebt = this.smokeDebt = this.sparkDebt = 0;
				this.lastX = Double.NaN;
				return;
			}
			// The flames go with the robot, the smoke stays behind as a trail
			double x = this.root.getTranslateX();
			double z = this.root.getTranslateZ();
			if (dt > 0) {
				double sx = Double.isNaN(this.lastX) ? 0 : (x - this.lastX) / dt;
				double sz = Double.isNaN(this.lastX) ? 0 : (z - this.lastZ) / dt;
				// A jump (new round, replay) is not a movement
				if (Math.hypot(sx, sz) * dt > 60) {
					sx = sz = 0;
				}
				double ease = 1 - Math.exp(-12 * dt);
				this.speedX += (sx - this.speedX) * ease;
				this.speedZ += (sz - this.speedZ) * ease;
			}
			this.lastX = x;
			this.lastZ = z;

			Random random = BattleArena3D.this.random;
			int spots = damage < 0.3 ? 1 : damage < 0.6 ? 2 : 3;
			double heading = Math.toRadians(this.bodyRotate.getAngle());
			double cos = Math.cos(heading);
			double sin = Math.sin(heading);

			this.flameDebt += dt * (40 + 70 * damage);
			this.smokeDebt += dt * (5 + 16 * damage);
			this.sparkDebt += dt * (1.5 + 7 * damage);
			while (this.flameDebt >= 1 || this.smokeDebt >= 1 || this.sparkDebt >= 1) {
				// A burning spot of the hull, turning with the body
				double[] spot = BURN_SPOTS[random.nextInt(spots)];
				double lx = spot[0] + (random.nextDouble() - 0.5) * 6;
				double lz = spot[2] + (random.nextDouble() - 0.5) * 6;
				double wx = x + lx * cos + lz * sin;
				double wz = z - lx * sin + lz * cos;
				if (this.flameDebt >= 1) {
					this.flameDebt--;
					// Anchored to the robot, its position is relative to it
					Particle flame = BattleArena3D.this.spawn(ParticleKind.FLAME, wx - x, spot[1], wz - z, damage);
					flame.anchor = this;
				} else if (this.smokeDebt >= 1) {
					this.smokeDebt--;
					BattleArena3D.this.spawn(ParticleKind.SMOKE, wx, spot[1] - 6, wz, damage).inherit(this.speedX * 0.15, this.speedZ * 0.15);
				} else {
					this.sparkDebt--;
					BattleArena3D.this.spawn(ParticleKind.SPARK, wx, spot[1], wz, damage).inherit(this.speedX * 0.3, this.speedZ * 0.3);
				}
			}
		}

		/**
		 * The links go round the belts, which move with the ground that each side of
		 * the hull covered since the last frame, so a robot turning in place runs its
		 * two belts the opposite way. Dead robots stop.
		 */
		void roll(double dt) {
			// The turret against the hull: a robot turning in place with its tracks carries the
			// turret along, but that is not the turret turning
			double gun = this.gunRotate.getAngle() - this.bodyRotate.getAngle();
			this.treadSpeed = 0;
			this.turretSpeed = 0;
			double x = this.root.getTranslateX();
			double z = this.root.getTranslateZ();
			double heading = Math.toRadians(this.bodyRotate.getAngle());
			// A jump (new round, replay) is not a movement
			if (!Double.isNaN(this.rollX) && !this.dead && Math.hypot(x - this.rollX, z - this.rollZ) < 60) {
				double fastest = 0;
				for (int side = 0; side < 2; side++) {
					double[] from = sidePoint(this.rollX, this.rollZ, this.rollHeading, side);
					double[] to = sidePoint(x, z, heading, side);
					// How far that side went forward, along the hull
					double forward = (to[0] - from[0]) * Math.sin(heading) + (to[1] - from[1]) * Math.cos(heading);
					this.treadShift[side] = (this.treadShift[side] + forward) % BELT_LENGTH;
					fastest = Math.max(fastest, Math.abs(forward));
				}
				if (dt > 0) {
					this.treadSpeed = fastest / dt;
				}
			}
			// The turret: how far it turned on the hull since the last frame, round the short way
			if (!Double.isNaN(this.lastGun) && !this.dead && dt > 0) {
				double turn = Math.IEEEremainder(gun - this.lastGun, 360);
				if (Math.abs(turn) < 90) {
					this.turretSpeed = Math.abs(turn) / dt;
				}
			}
			this.lastGun = gun;
			this.rollX = x;
			this.rollZ = z;
			this.rollHeading = heading;

			for (int side = 0; side < 2; side++) {
				for (int link = 0; link < TREAD_LINKS; link++) {
					double s = link * BELT_LENGTH / TREAD_LINKS + this.treadShift[side];
					// Round the belt: a link that leaves one end comes back at the other
					s -= BELT_LENGTH * Math.floor(s / BELT_LENGTH);
					double[] point = beltPoint(s);
					MeshView linkView = this.treadLinks[side][link];
					linkView.setTranslateX(side == 0 ? -BELT_CENTER : BELT_CENTER);
					linkView.setTranslateY(point[1]);
					linkView.setTranslateZ(point[0]);
					this.treadTurns[side][link].setAngle(Math.toDegrees(point[2]));
				}
			}
		}

		/**
		 * Blinks white when hit
		 */
		void flash() {
			if (this.dead) {
				return;
			}
			this.bodyMaterial.setDiffuseColor(Color.WHITE);
			PauseTransition back = BattleArena3D.this.effect(new PauseTransition(Duration.millis(110)));
			back.setOnFinished(event -> this.bodyMaterial.setDiffuseColor(this.bodyColor));
			back.play();
		}

		/**
		 * Blows up in its own colors: fireballs, shock waves seen as bubbles, smoke,
		 * debris, the hull, the turret and the radar thrown across the arena
		 */
		void destroy() {
			if (this.dead) {
				return;
			}
			this.dead = true;
			double x = this.root.getTranslateX();
			double z = this.root.getTranslateZ();
			Group blast = new Group();
			blast.setTranslateX(x);
			blast.setTranslateZ(z);
			ParallelTransition all = new ParallelTransition();

			// An overload: a flash, a bolt from the sky, then discharges of lightning
			// crackling out of the wreck and showers of sparks
			Color tint = this.playerColor;
			Color hot = tint.interpolate(Color.WHITE, 0.85);
			MeshView core = shape(CRYSTAL, energy(Color.WHITE, 0.85));
			core.setTranslateY(-20);
			blast.getChildren().add(core);
			all.getChildren().addAll(grow(core, 2, 9, 140), shrink(core, 140, 180));
			double[] center = { 0, -20, 0 };
			// The bolt from the sky strikes first
			LowPoly strikeGlow = new LowPoly();
			LowPoly strikeCore = new LowPoly();
			BattleArena3D.this.bolt(strikeGlow, strikeCore, new double[] { 0, -420, 0 }, center, 0.6, 0);
			Group strike = new Group(shape(strikeGlow.mesh, energy(tint.interpolate(Color.web("#8fe9ff"), 0.5), 0.45)), shape(strikeCore.mesh, energy(Color.WHITE, 1)));
			blast.getChildren().add(strike);
			all.getChildren().add(new Timeline(new KeyFrame(Duration.millis(120), event -> strike.setVisible(false))));
			// The shock wave: rings spreading on the floor from the wreck
			Color air = tint.interpolate(Color.WHITE, 0.7);
			BattleArena3D.this.shockwave(blast, all, -1, 300, 0, 900, air);
			BattleArena3D.this.shockwave(blast, all, -1, 210, 160, 750, Color.web("#8fe9ff"));
			BattleArena3D.this.shockwave(blast, all, -1, 130, 0, 500, Color.WHITE);
			BattleArena3D.this.discharge(blast, all, center, -1, 9, 110, 0.4, tint, 60);
			BattleArena3D.this.discharge(blast, all, center, -1, 7, 90, 0.35, tint, 400);
			BattleArena3D.this.discharge(blast, all, center, -1, 6, 120, 0.38, tint, 800);
			BattleArena3D.this.sparks(blast, all, center, -1, 40, 120, 1.1, Color.web("#fff3b0"), 1100);
			BattleArena3D.this.sparks(blast, all, center, -1, 18, 80, 0.9, hot, 1500);

			// The smoke rising from the wreck: particles, like the fire of the burning robots,
			// thick and dark since it is destroyed
			Timeline smoke = new Timeline();
			for (int i = 0; i < 40; i++) {
				smoke.getKeyFrames().add(new KeyFrame(Duration.millis(i * 90), event -> {
					for (int puff = 0; puff < 2; puff++) {
						BattleArena3D.this.spawn(ParticleKind.SMOKE, x + (BattleArena3D.this.random.nextDouble() - 0.5) * 40, -20, z + (BattleArena3D.this.random.nextDouble() - 0.5) * 40, 0.8);
					}
				}));
			}
			this.running.add(smoke);
			smoke.play();

			// Debris in the colors of the robot
			PhongMaterial[] colors = { this.bodyMaterial, this.gunMaterial, new PhongMaterial(Color.web("#22252e")), new PhongMaterial(Color.web("#ffb000")) };
			for (int i = 0; i < 26; i++) {
				double angle = Math.PI * 2 * i / 26 + BattleArena3D.this.random.nextDouble() * 0.3;
				double size = 3 + BattleArena3D.this.random.nextDouble() * 5;
				Box piece = new Box(size, size, size);
				piece.setMaterial(colors[i % colors.length]);
				piece.setTranslateY(-15);
				this.debris.getChildren().add(piece);
				double reach = 120 + BattleArena3D.this.random.nextDouble() * 180;
				all.getChildren().add(BattleArena3D.this.fly(piece, Math.cos(angle) * reach, Math.sin(angle) * reach, 70 + BattleArena3D.this.random.nextDouble() * 120, 1500));
			}

			// The turret and the radar are thrown in the air
			double throwAngle = BattleArena3D.this.random.nextDouble() * Math.PI * 2;
			this.turret.setRotationAxis(new Point3D(1, 0, 0.4));
			this.radar.setRotationAxis(new Point3D(0.3, 0, 1));
			Timeline thrown = new Timeline( //
					new KeyFrame(Duration.millis(550), new KeyValue(this.turret.translateYProperty(), -140, Interpolator.EASE_OUT), new KeyValue(this.radar.translateYProperty(), -190, Interpolator.EASE_OUT)), //
					new KeyFrame(Duration.millis(1400), //
							new KeyValue(this.turret.translateYProperty(), 14, Interpolator.EASE_IN), //
							new KeyValue(this.turret.translateXProperty(), Math.cos(throwAngle) * 170, Interpolator.EASE_OUT), //
							new KeyValue(this.turret.translateZProperty(), Math.sin(throwAngle) * 170, Interpolator.EASE_OUT), //
							new KeyValue(this.turret.rotateProperty(), 720, Interpolator.LINEAR), //
							new KeyValue(this.radar.translateYProperty(), 24, Interpolator.EASE_IN), //
							new KeyValue(this.radar.translateXProperty(), -Math.cos(throwAngle) * 200, Interpolator.EASE_OUT), //
							new KeyValue(this.radar.translateZProperty(), -Math.sin(throwAngle) * 200, Interpolator.EASE_OUT), //
							new KeyValue(this.radar.rotateProperty(), -900, Interpolator.LINEAR)));
			all.getChildren().add(thrown);

			// The hull is thrown too: it flies over the arena, turns over and lands tilted
			double hullAngle = BattleArena3D.this.random.nextDouble() * Math.PI * 2;
			double hullReach = 130 + BattleArena3D.this.random.nextDouble() * 80;
			double hullX = Math.cos(hullAngle) * hullReach;
			double hullZ = Math.sin(hullAngle) * hullReach;
			this.body.setRotationAxis(new Point3D(-Math.sin(hullAngle), 0, Math.cos(hullAngle)));
			all.getChildren().add(new Timeline( //
					new KeyFrame(Duration.millis(450), new KeyValue(this.body.translateYProperty(), -80, Interpolator.EASE_OUT), new KeyValue(this.body.translateXProperty(), hullX * 0.5, Interpolator.EASE_OUT),
							new KeyValue(this.body.translateZProperty(), hullZ * 0.5, Interpolator.EASE_OUT), new KeyValue(this.body.rotateProperty(), 60, Interpolator.LINEAR)), //
					new KeyFrame(Duration.millis(1300), new KeyValue(this.body.translateYProperty(), 0, Interpolator.EASE_IN), new KeyValue(this.body.translateXProperty(), hullX, Interpolator.EASE_OUT),
							new KeyValue(this.body.translateZProperty(), hullZ, Interpolator.EASE_OUT), new KeyValue(this.body.rotateProperty(), 35, Interpolator.EASE_OUT))));

			BattleArena3D.this.effects.getChildren().add(blast);
			BattleArena3D.this.effect(all).setOnFinished(event -> BattleArena3D.this.effects.getChildren().remove(blast));
			this.running.add(all);
			all.play();
			BattleArena3D.this.lightFlash(x, z, tint, 1.4);

			this.nameLabel.setText("☠ " + this.displayName);
			this.balloon.setOpacity(0.6);
		}

		/**
		 * A material that shines in the color by itself
		 */
		PhongMaterial glow(Color color) {
			WritableImage map = new WritableImage(2, 2);
			for (int y = 0; y < 2; y++) {
				for (int x = 0; x < 2; x++) {
					map.getPixelWriter().setColor(x, y, color);
				}
			}
			PhongMaterial material = new PhongMaterial(color.darker().darker());
			material.setSelfIlluminationMap(map);
			this.glows.add(material);
			this.glowMaps.add(map);
			return material;
		}

		void setGlowing(boolean on) {
			for (int i = 0; i < this.glows.size(); i++) {
				this.glows.get(i).setSelfIlluminationMap(on ? this.glowMaps.get(i) : null);
			}
		}

		/**
		 * Back to a whole tank, for a new round or the replay
		 */
		void reset() {
			for (Animation animation : this.running) {
				animation.stop();
			}
			this.running.clear();
			this.dead = false;
			this.tank.setVisible(true);
			this.debris.getChildren().clear();
			this.bodyMaterial.setDiffuseColor(this.bodyColor);
			this.gunMaterial.setDiffuseColor(this.gunColor);
			this.setGlowing(true);
			for (Node part : List.of(this.turret, this.radar, this.body)) {
				part.setTranslateX(0);
				part.setTranslateY(0);
				part.setTranslateZ(0);
				part.setRotate(0);
			}
			this.nameLabel.setText(this.displayName);
			this.balloon.setOpacity(1);
		}
	}

	/** Robots catch fire below this energy */
	private static final double BURN_ENERGY = 50;
	/** Where the hull burns, in the body coordinates: engine, side, turret */
	private static final double[][] BURN_SPOTS = { { 0, -15, -11 }, { 8, -15, 3 }, { -5, -23, 1 } };

	private enum ParticleKind {
		FLAME, SMOKE, SPARK, EMBER, SNOW, FIREFLY
	}

	/** A quad of size 1 facing -Z, shared by all the sprites */
	private static final TriangleMesh SPRITE = sprite();
	private static final Image FLAME_TEXTURE = flameTexture();
	private static final Image SMOKE_TEXTURE = smokeTexture();
	private static final Image SOFT_TEXTURE = softTexture();

	/**
	 * A soft white dot, for embers and snow
	 */
	private static Image softTexture() {
		int size = 32;
		WritableImage image = new WritableImage(size, size);
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double radius = Math.hypot((px + 0.5) / size * 2 - 1, (py + 0.5) / size * 2 - 1);
				double alpha = Math.max(0, 1 - radius);
				image.getPixelWriter().setColor(px, py, Color.color(1, 1, 1, Math.min(1, alpha * alpha * 1.6)));
			}
		}
		return image;
	}

	private static TriangleMesh sprite() {
		TriangleMesh mesh = new TriangleMesh();
		mesh.getPoints().addAll(-0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f, 0);
		mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
		mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
		return mesh;
	}

	/**
	 * A tongue of fire: white hot at the base, round at the bottom and pointed at
	 * the top, soft borders
	 */
	private static Image flameTexture() {
		int size = 64;
		WritableImage image = new WritableImage(size, size);
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double u = (px + 0.5) / size * 2 - 1;
				// 1 at the top, -1 at the bottom
				double v = 1 - (py + 0.5) / size * 2;
				// Narrower going up
				double width = 0.62 * Math.pow(Math.max(0, (1 - v) / 2), 0.55) + 0.04;
				double center = -0.25;
				double dx = u / width;
				double dy = v < center ? (v - center) / 0.75 : (v - center) / 1.2;
				double distance = Math.sqrt(dx * dx + dy * dy);
				double alpha = Math.max(0, 1 - distance);
				alpha = alpha * alpha * (3 - 2 * alpha);
				double heat = Math.max(0, 1 - distance * 1.4);
				image.getPixelWriter().setColor(px, py, Color.color(1, 0.75 + 0.25 * heat, 0.45 + 0.55 * heat, Math.min(1, alpha)));
			}
		}
		return image;
	}

	/**
	 * A puff: soft round blob with some noise, so it does not look like a ball
	 */
	private static Image smokeTexture() {
		int size = 64;
		WritableImage image = new WritableImage(size, size);
		Random random = new Random(7);
		double[] phase = new double[6];
		for (int i = 0; i < phase.length; i++) {
			phase[i] = random.nextDouble() * Math.PI * 2;
		}
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double u = (px + 0.5) / size * 2 - 1;
				double v = (py + 0.5) / size * 2 - 1;
				double angle = Math.atan2(v, u);
				double radius = Math.hypot(u, v);
				// Bumpy border
				double border = 0.78 + 0.08 * Math.sin(3 * angle + phase[0]) + 0.06 * Math.sin(5 * angle + phase[1]) + 0.04 * Math.sin(9 * angle + phase[2]);
				double alpha = Math.max(0, 1 - radius / border);
				alpha = Math.pow(alpha, 0.8);
				// Darker and lighter spots inside
				double grain = 0.85 + 0.15 * Math.sin(u * 7 + phase[3]) * Math.sin(v * 6 + phase[4]) + 0.08 * Math.sin((u + v) * 11 + phase[5]);
				double gray = Math.max(0, Math.min(1, grain));
				image.getPixelWriter().setColor(px, py, Color.color(gray, gray, gray, Math.min(1, alpha * (0.75 + 0.25 * grain))));
			}
		}
		return image;
	}

	/** Farthest first */
	private static final Comparator<Particle> FARTHEST_FIRST = (a, b) -> Double.compare(b.distance, a.distance);
	/** Reused every frame, so it does not make garbage */
	private final List<Node> fireViews = new ArrayList<Node>();

	// The colors of the particles are made once: every one is a step of its life
	// and a level of opacity, and the particles take them from these tables
	// instead of making new colors in every frame
	private static final int LIFE_STEPS = 48;
	private static final int OPACITY_LEVELS = 16;
	private static final int SMOKE_SHADES = 8;
	/** The darkest gray of the smoke, the shades go from 0 to this */
	private static final double SMOKE_SHADE_MAX = 0.25;
	private static final Color[] FLAME_COLORS = new Color[2 * LIFE_STEPS * OPACITY_LEVELS];
	private static final Color[] SMOKE_COLORS = new Color[SMOKE_SHADES * LIFE_STEPS * OPACITY_LEVELS];
	private static final Color[] EMBER_COLORS = new Color[LIFE_STEPS];
	private static final Color[] SPARK_COLORS = new Color[LIFE_STEPS];
	private static final Color[] FIREFLY_COLORS = new Color[OPACITY_LEVELS];
	private static final Color[] SNOW_COLORS = new Color[OPACITY_LEVELS];

	private static int step(double t) {
		return Math.max(0, Math.min(LIFE_STEPS - 1, (int) (t * LIFE_STEPS)));
	}

	private static int level(double opacity) {
		return Math.max(0, Math.min(OPACITY_LEVELS - 1, (int) Math.round(opacity * (OPACITY_LEVELS - 1))));
	}

	private static double middle(int step) {
		return (step + 0.5) / LIFE_STEPS;
	}

	private static double opacity(int level) {
		return (double) level / (OPACITY_LEVELS - 1);
	}

	private static Color withOpacity(Color color, double opacity) {
		return Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
	}

	private static Color flameColor(boolean blue, int step, int level) {
		int index = ((blue ? 1 : 0) * LIFE_STEPS + step) * OPACITY_LEVELS + level;
		Color color = FLAME_COLORS[index];
		if (color == null) {
			double t = middle(step);
			Color base;
			if (blue) {
				// A blue tongue: pale at the base, deep blue at the tip
				base = t < 0.25 ? Color.web("#d6f1ff").interpolate(Color.web("#4aa8ff"), t / 0.25) : Color.web("#4aa8ff").interpolate(Color.web("#2a46ff"), (t - 0.25) / 0.75);
			} else if (t < 0.14) {
				// Every flame is blue where it is born
				base = Color.web("#5aa9ff").interpolate(Color.web("#fff0b8"), t / 0.14);
			} else if (t < 0.3) {
				base = Color.web("#fff0b8").interpolate(Color.web("#ffb12a"), (t - 0.14) / 0.16);
			} else {
				base = Color.web("#ffb12a").interpolate(Color.web("#e8380d"), (t - 0.3) / 0.7);
			}
			color = FLAME_COLORS[index] = withOpacity(base, opacity(level));
		}
		return color;
	}

	private static Color smokeColor(int shade, int step, int level) {
		int index = (shade * LIFE_STEPS + step) * OPACITY_LEVELS + level;
		Color color = SMOKE_COLORS[index];
		if (color == null) {
			double t = middle(step);
			double gray = (shade + 0.5) / SMOKE_SHADES * SMOKE_SHADE_MAX;
			Color base = t < 0.12 ? Color.web("#ff7a2a").interpolate(Color.gray(gray), t / 0.12) : Color.gray(gray).interpolate(Color.gray(Math.min(0.4, gray + 0.12)), (t - 0.12) / 0.88);
			color = SMOKE_COLORS[index] = withOpacity(base, opacity(level));
		}
		return color;
	}

	private static Color emberColor(int step) {
		Color color = EMBER_COLORS[step];
		if (color == null) {
			double t = middle(step);
			double alpha = (t < 0.1 ? t * 10 : 1 - (t - 0.1) / 0.9) * 0.95;
			color = EMBER_COLORS[step] = Color.color(1, 0.62 - 0.4 * t, 0.15 - 0.1 * t, Math.max(0, alpha));
		}
		return color;
	}

	private static Color sparkColor(int step) {
		Color color = SPARK_COLORS[step];
		if (color == null) {
			double t = middle(step);
			color = SPARK_COLORS[step] = Color.color(1, 0.85 - 0.4 * t, 0.4 - 0.3 * t, 1 - t * t);
		}
		return color;
	}

	private static Color fireflyColor(int level) {
		return cachedColor(FIREFLY_COLORS, level, 0.78, 1, 0.35);
	}

	private static Color snowColor(int level) {
		return cachedColor(SNOW_COLORS, level, 0.94, 0.97, 1);
	}

	/** The color of a level of opacity, made the first time it is needed */
	private static Color cachedColor(Color[] cache, int level, double red, double green, double blue) {
		Color color = cache[level];
		if (color == null) {
			color = cache[level] = Color.color(red, green, blue, opacity(level));
		}
		return color;
	}

	/**
	 * A flame, a spark or a puff of smoke: a sprite that always faces the camera.
	 * It lives in the world, so a moving robot leaves a trail of smoke.
	 */
	private static final class Particle {
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

		Particle() {
			this.view.setMaterial(this.material);
			this.view.setCullFace(CullFace.NONE);
			this.view.getTransforms().addAll(this.yaw, this.pitch, this.spin, this.scale);
		}
	}

	private Particle spawn(ParticleKind kind, double x, double y, double z, double damage) {
		Particle p = this.particlePool.isEmpty() ? new Particle() : this.particlePool.remove(this.particlePool.size() - 1);
		p.kind = kind;
		p.anchor = null;
		p.x = x;
		p.y = y;
		p.z = z;
		p.age = 0;
		p.spin.setAngle(this.random.nextDouble() * 360);
		switch (kind) {
		case FLAME -> {
			p.life = 0.35 + this.random.nextDouble() * 0.35;
			// Some tongues burn blue all the way, the others only at the base
			p.shade = this.random.nextDouble();
			p.size = 9 + this.random.nextDouble() * 6 + damage * 8;
			p.vx = (this.random.nextDouble() - 0.5) * 10;
			p.vz = (this.random.nextDouble() - 0.5) * 10;
			p.vy = -(32 + this.random.nextDouble() * 22 + damage * 25);
			// Flames stay upright, only a little sway
			p.spin.setAngle((this.random.nextDouble() - 0.5) * 16);
			p.spinSpeed = (this.random.nextDouble() - 0.5) * 30;
			p.material.setDiffuseMap(FLAME_TEXTURE);
		}
		case SMOKE -> {
			p.life = 1.6 + this.random.nextDouble() * 1.4;
			p.size = 9 + this.random.nextDouble() * 6;
			p.vx = (this.random.nextDouble() - 0.5) * 8 + 6;
			p.vz = (this.random.nextDouble() - 0.5) * 8 + 3;
			p.vy = -(20 + this.random.nextDouble() * 14);
			p.spinSpeed = (this.random.nextDouble() - 0.5) * 70;
			// Thicker and blacker when almost destroyed
			p.shade = 0.16 - 0.1 * damage + this.random.nextDouble() * 0.06;
			p.material.setDiffuseMap(SMOKE_TEXTURE);
		}
		case EMBER -> {
			p.life = 2.5 + this.random.nextDouble() * 2;
			p.size = 1.3 + this.random.nextDouble() * 1.2;
			p.vx = (this.random.nextDouble() - 0.5) * 12;
			p.vz = (this.random.nextDouble() - 0.5) * 12;
			p.vy = -(18 + this.random.nextDouble() * 24);
			p.spinSpeed = 0;
			p.shade = this.random.nextDouble();
			p.material.setDiffuseMap(SOFT_TEXTURE);
		}
		case FIREFLY -> {
			p.life = 5 + this.random.nextDouble() * 4;
			p.size = 2.6 + this.random.nextDouble() * 1.6;
			p.vx = (this.random.nextDouble() - 0.5) * 14;
			p.vz = (this.random.nextDouble() - 0.5) * 14;
			p.vy = (this.random.nextDouble() - 0.5) * 8;
			p.spinSpeed = 0;
			p.shade = this.random.nextDouble();
			p.material.setDiffuseMap(SOFT_TEXTURE);
		}
		case SNOW -> {
			p.life = 12;
			p.size = 2.6 + this.random.nextDouble() * 2.2;
			p.vx = 5 + this.random.nextDouble() * 5;
			p.vz = (this.random.nextDouble() - 0.5) * 6;
			p.vy = 36 + this.random.nextDouble() * 26;
			p.spinSpeed = 0;
			p.shade = this.random.nextDouble();
			p.material.setDiffuseMap(SOFT_TEXTURE);
		}
		case SPARK -> {
			p.life = 0.5 + this.random.nextDouble() * 0.5;
			p.size = 1.6 + this.random.nextDouble() * 1.4;
			double angle = this.random.nextDouble() * Math.PI * 2;
			double speed = 30 + this.random.nextDouble() * 50;
			p.vx = Math.cos(angle) * speed;
			p.vz = Math.sin(angle) * speed;
			p.vy = -(60 + this.random.nextDouble() * 70);
			p.spinSpeed = 0;
			p.material.setDiffuseMap(FLAME_TEXTURE);
		}
		}
		// Too many: it is made, but not shown
		if (this.particles.size() < 900) {
			this.particles.add(p);
		} else {
			this.particlePool.add(p);
		}
		return p;
	}

	/**
	 * Moves, fades and sorts the particles, and lets the robots emit new ones
	 */
	private void updateFire(double dt) {
		for (RobotView view : this.robots.values()) {
			view.burn(dt);
		}

		// Embers rise from the lava, snow falls on the ice. Below a limit, so the
		// fire of the robots is never left out.
		double rate = this.style == ArenaStyle.VOLCANO ? 26 : this.style == ArenaStyle.ICE ? 95 : this.style == ArenaStyle.JUNGLE ? 16 : 0;
		this.ambientDebt += dt * rate;
		while (this.ambientDebt >= 1) {
			this.ambientDebt--;
			if (this.particles.size() > 550) {
				continue;
			}
			if (this.style == ArenaStyle.JUNGLE) {
				double x = (this.random.nextDouble() - 0.5) * (this.fieldWidth + 700);
				double z = (this.random.nextDouble() - 0.5) * (this.fieldHeight + 700);
				this.spawn(ParticleKind.FIREFLY, x, -8 - this.random.nextDouble() * 60, z, 0);
			} else if (this.style == ArenaStyle.VOLCANO) {
				this.spawn(ParticleKind.EMBER, (this.random.nextDouble() - 0.5) * (this.fieldWidth + 260), -2, (this.random.nextDouble() - 0.5) * (this.fieldHeight + 260), 0);
			} else {
				this.spawn(ParticleKind.SNOW, (this.random.nextDouble() - 0.5) * (this.fieldWidth + 800), -240 - this.random.nextDouble() * 120,
						(this.random.nextDouble() - 0.5) * (this.fieldHeight + 800), 0);
			}
		}

		double eyeX = this.eye[0];
		double eyeY = this.eye[1];
		double eyeZ = this.eye[2];
		double yaw = this.cameraYaw.getAngle();
		double pitch = this.cameraPitch.getAngle();

		for (int i = this.particles.size() - 1; i >= 0; i--) {
			Particle p = this.particles.get(i);
			p.age += dt;
			if (p.age >= p.life || p.anchor != null && (p.anchor.dead || !p.anchor.root.isVisible())) {
				this.particles.remove(i);
				this.particlePool.add(p);
				continue;
			}
			double t = p.age / p.life;
			double drag = p.kind == ParticleKind.EMBER || p.kind == ParticleKind.SNOW ? 1 : Math.exp(-(p.kind == ParticleKind.SMOKE ? 0.6 : 1.5) * dt);
			p.vx *= drag;
			p.vz *= drag;
			if (p.kind == ParticleKind.SPARK) {
				// Thrown up, then falls
				p.vy += 160 * dt;
			} else if (p.kind == ParticleKind.FLAME) {
				// Flames speed up going up
				p.vy -= 30 * dt;
			}
			p.x += p.vx * dt;
			p.y = Math.min(-1, p.y + p.vy * dt);
			// Sparks go out on the floor
			if ((p.kind == ParticleKind.SPARK || p.kind == ParticleKind.SNOW) && p.y >= -1 && p.vy > 0) {
				p.age = p.life;
			}
			// Embers, snow and fireflies sway
			if (p.kind == ParticleKind.EMBER || p.kind == ParticleKind.SNOW || p.kind == ParticleKind.FIREFLY) {
				p.x += Math.sin(p.age * (p.kind == ParticleKind.EMBER ? 3 : p.kind == ParticleKind.FIREFLY ? 1.3 : 1.7) + p.shade * 6) * (p.kind == ParticleKind.FIREFLY ? 14 : 9) * dt;
				if (p.kind == ParticleKind.FIREFLY) {
					p.z += Math.cos(p.age * 1.1 + p.shade * 9) * 12 * dt;
				}
			}
			p.z += p.vz * dt;
			p.spin.setAngle(p.spin.getAngle() + p.spinSpeed * dt);

			double width;
			double height;
			Color color;
			switch (p.kind) {
			case FLAME -> {
				// Grows fast, then thins out to a tip
				double grow = Math.min(1, t * 5);
				width = p.size * grow * (1 - 0.5 * t);
				height = width * 1.7;
				double flicker = 0.85 + 0.15 * this.random.nextDouble();
				color = flameColor(p.shade < 0.35, step(t), level(Math.min(1, (1 - t) * 1.6) * flicker));
			}
			case SMOKE -> {
				// Born in the fire, grows and fades
				width = p.size * (0.6 + 2.6 * Math.sqrt(t));
				height = width;
				color = smokeColor(Math.max(0, Math.min(SMOKE_SHADES - 1, (int) (p.shade / SMOKE_SHADE_MAX * SMOKE_SHADES))), step(t), level(Math.min(1, t * 6) * Math.pow(1 - t, 1.4) * 0.7));
			}
			case EMBER -> {
				double flicker = 0.7 + 0.3 * this.random.nextDouble();
				width = p.size * (1 - 0.3 * t) * flicker;
				height = width;
				color = emberColor(step(t));
			}
			case FIREFLY -> {
				width = p.size;
				height = width;
				// Fades in and out and blinks
				double blink = 0.5 + 0.5 * Math.sin(p.age * 4.2 + p.shade * 20);
				double envelope = Math.min(1, Math.min(t * 6, (1 - t) * 6));
				color = fireflyColor(level(Math.max(0, envelope * (0.35 + 0.65 * blink))));
			}
			case SNOW -> {
				width = p.size;
				height = width;
				color = snowColor(level(Math.min(0.9, p.age * 1.5)));
			}
			default -> {
				width = p.size * (1 - 0.5 * t);
				height = width;
				color = sparkColor(step(t));
			}
			}
			// The colors come from tables, the same object again if it did not change
			if (p.shown != color) {
				p.material.setDiffuseColor(color);
				p.shown = color;
			}
			double px = p.x;
			double pz = p.z;
			if (p.anchor != null) {
				px += p.anchor.root.getTranslateX();
				pz += p.anchor.root.getTranslateZ();
			}
			p.view.setTranslateX(px);
			p.view.setTranslateY(p.y);
			p.view.setTranslateZ(pz);
			p.yaw.setAngle(yaw);
			p.pitch.setAngle(pitch);
			p.scale.setX(Math.max(0.01, width));
			p.scale.setY(Math.max(0.01, height));
			// Flames are drawn from their base
			if (p.kind == ParticleKind.FLAME) {
				p.view.setTranslateY(p.y - height * 0.3);
			}
			double dx = px - eyeX;
			double dy = p.y - eyeY;
			double dz = pz - eyeZ;
			p.distance = dx * dx + dy * dy + dz * dz;
		}

		// No depth sorting in JavaFX: the farthest first, so the nearest blend
		// over them
		this.particles.sort(FARTHEST_FIRST);
		this.fireViews.clear();
		for (Particle p : this.particles) {
			this.fireViews.add(p.view);
		}
		if (!this.fireLayer.getChildren().equals(this.fireViews)) {
			this.fireLayer.getChildren().setAll(this.fireViews);
		}
	}

	private void clearFire() {
		this.particlePool.addAll(this.particles);
		this.particles.clear();
		this.fireLayer.getChildren().clear();
		for (RobotView view : this.robots.values()) {
			view.flameDebt = view.smokeDebt = view.sparkDebt = 0;
		}
	}

	/**
	 * A robocode ARGB color, exactly as the robot defined it (black included), or
	 * the fallback if there is none
	 */
	private static Color argb(int argb, Color fallback) {
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
	private static PhongMaterial paint(Color color) {
		PhongMaterial material = new PhongMaterial(color);
		double floor = 0.38;
		material.setSpecularColor(Color.color(Math.max(floor, Math.min(1, color.getRed() * 1.3)), Math.max(floor, Math.min(1, color.getGreen() * 1.3)), Math.max(floor, Math.min(1, color.getBlue() * 1.3))));
		material.setSpecularPower(36);
		return material;
	}
}
