package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
 * tombstones, a fighting game HUD, an adaptive soundtrack and a camera that
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
		LOADING, SHOWCASE, INTRO, PLAYING, FREEZE, REPLAY, ROUND_END, FINISHED
	}

	private final List<Opponent> fighters;
	private Runnable onFinished;
	private final BattleAudio audio = new BattleAudio();
	private final Random random = new Random();

	// 3D
	private final Group world = new Group();
	private final Group arena = new Group();
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
	private final ArenaStyle style;
	/** Parts of the arena that glow, like lava: lit by the fire glow */
	private final Group glowing = new Group();
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
	private final List<PhongMaterial> pulseMaterials = new ArrayList<PhongMaterial>();
	private final List<Color> pulseColors = new ArrayList<Color>();
	private final List<Double> pulsePhases = new ArrayList<Double>();
	private double neonClock;
	private Box scanner;
	private static final double BEAM_ALPHA = 0.07;
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
	private double fieldWidth = 800;
	private double fieldHeight = 600;
	private final Map<Integer, RobotView> robots = new HashMap<Integer, RobotView>();
	private final Map<Integer, Sphere> bullets = new HashMap<Integer, Sphere>();
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

		this.subScene = new SubScene(this.world, 100, 100, true, SceneAntialiasing.BALANCED);
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
		case NEON -> this.buildNeon(w, h);
		case VOLCANO -> this.buildVolcano(w, h);
		case ICE -> this.buildIce(w, h);
		case JUNGLE -> this.buildJungle(w, h);
		}
		this.arena.getChildren().add(this.glowing);
		this.buildFence(w, h);
	}

	/**
	 * A dome around everything, seen from inside, that shines by itself
	 */
	private static Sphere skyDome(Image picture) {
		Sphere dome = new Sphere(14000, 48);
		dome.setCullFace(CullFace.FRONT);
		PhongMaterial sky = new PhongMaterial(Color.BLACK);
		sky.setSelfIlluminationMap(picture);
		dome.setMaterial(sky);
		return dome;
	}

	/** A material that is the color, lit by the glow light of the arena */
	private PhongMaterial neonGlow(Color color, boolean pulse, Random random) {
		PhongMaterial material = new PhongMaterial(color);
		if (pulse) {
			this.pulseMaterials.add(material);
			this.pulseColors.add(color);
			this.pulsePhases.add(random.nextDouble() * Math.PI * 2);
		}
		return material;
	}

	private MeshView meshView(TriangleMesh mesh, PhongMaterial material) {
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		return view;
	}

	/**
	 * Night city: a circuit floor with a grid, pads in the corners and pylons; around it, towers with lit windows, a grid to the horizon
	 * and a setting sun under a violet sky
	 */
	private void buildNeon(double w, double h) {
		Random random = new Random(8);
		this.pulseMaterials.clear();
		this.pulseColors.clear();
		this.pulsePhases.clear();

		this.arena.getChildren().add(skyDome(NeonTexture.SKY));

		// The sun, far away, facing the middle
		double sunX = -0.78 * 7200;
		double sunZ = -0.62 * 7200;
		double sunSize = 1250;
		double rightX = 0.62;
		double rightZ = -0.78;
		PhongMaterial sun = new PhongMaterial(Color.BLACK);
		sun.setDiffuseMap(NeonTexture.SUN);
		sun.setSelfIlluminationMap(NeonTexture.SUN);
		TriangleMesh sunMesh = new TriangleMesh();
		sunMesh.getPoints().addAll((float) (sunX - rightX * sunSize), (float) (-950 - sunSize), (float) (sunZ - rightZ * sunSize), (float) (sunX + rightX * sunSize),
				(float) (-950 - sunSize), (float) (sunZ + rightZ * sunSize), (float) (sunX + rightX * sunSize), (float) (-950 + sunSize), (float) (sunZ + rightZ * sunSize),
				(float) (sunX - rightX * sunSize), (float) (-950 + sunSize), (float) (sunZ - rightZ * sunSize));
		sunMesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
		sunMesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
		this.arena.getChildren().add(this.meshView(sunMesh, sun));

		Box ground = new Box(9000, 2, 9000);
		ground.setMaterial(material(Color.web("#07080f")));
		ground.setTranslateY(6);
		this.arena.getChildren().add(ground);

		// The floor: a slab with a panel texture over it
		Box floor = new Box(w + 40, 4.4, h + 40);
		floor.setMaterial(material(Color.web("#0d1020")));
		floor.setTranslateY(2.8);
		this.arena.getChildren().add(floor);
		PhongMaterial panels = textured(NeonTexture.FLOOR, Color.gray(0.3));
		panels.setSpecularPower(34);
		this.arena.getChildren().add(this.tiledQuad(new double[][] { { -w / 2 - 20, 0, -h / 2 - 20 }, { w / 2 + 20, 0, -h / 2 - 20 }, { w / 2 + 20, 0, h / 2 + 20 },
				{ -w / 2 - 20, 0, h / 2 + 20 } }, panels, NeonTexture.FLOOR_TILE));

		// The grid: thin lines every 50, brighter every 200
		TriangleMesh minor = new TriangleMesh();
		TriangleMesh major = new TriangleMesh();
		for (double x = 0; x <= w; x += 50) {
			NeonCity.box(x % 200 == 0 ? major : minor, x - w / 2, -0.3, 0, x % 200 == 0 ? 2.6 : 1.2, 0.5, h);
		}
		for (double y = 0; y <= h; y += 50) {
			NeonCity.box(y % 200 == 0 ? major : minor, 0, -0.3, y - h / 2, w, 0.5, y % 200 == 0 ? 2.6 : 1.2);
		}
		this.glowing.getChildren().add(this.meshView(minor, this.neonGlow(Color.web("#157fae"), false, random)));
		this.glowing.getChildren().add(this.meshView(major, this.neonGlow(Color.web("#2ec4ff"), true, random)));

		// The edge of the field and a strip of light just outside it
		TriangleMesh edge = new TriangleMesh();
		NeonCity.box(edge, 0, -0.4, -h / 2, w + 6, 0.6, 5);
		NeonCity.box(edge, 0, -0.4, h / 2, w + 6, 0.6, 5);
		NeonCity.box(edge, -w / 2, -0.4, 0, 5, 0.6, h + 6);
		NeonCity.box(edge, w / 2, -0.4, 0, 5, 0.6, h + 6);
		this.glowing.getChildren().add(this.meshView(edge, this.neonGlow(Color.web("#bff3ff"), true, random)));
		TriangleMesh curb = new TriangleMesh();
		NeonCity.box(curb, 0, -1.5, -h / 2 - 14, w + 40, 3, 4);
		NeonCity.box(curb, 0, -1.5, h / 2 + 14, w + 40, 3, 4);
		NeonCity.box(curb, -w / 2 - 14, -1.5, 0, 4, 3, h + 40);
		NeonCity.box(curb, w / 2 + 14, -1.5, 0, 4, 3, h + 40);
		this.glowing.getChildren().add(this.meshView(curb, this.neonGlow(Color.web("#ff3fa4"), true, random)));

		// Pads in the corners: a square and a corner mark inside
		TriangleMesh pads = new TriangleMesh();
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				double cx = sx * (w / 2 - 55);
				double cz = sz * (h / 2 - 55);
				NeonCity.box(pads, cx, -0.5, cz - 36, 72, 0.6, 2);
				NeonCity.box(pads, cx, -0.5, cz + 36, 72, 0.6, 2);
				NeonCity.box(pads, cx - 36, -0.5, cz, 2, 0.6, 72);
				NeonCity.box(pads, cx + 36, -0.5, cz, 2, 0.6, 72);
				NeonCity.box(pads, cx - sx * 14, -0.5, cz - sz * 14, 24, 0.6, 3);
				NeonCity.box(pads, cx - sx * 14, -0.5, cz - sz * 14, 3, 0.6, 24);
			}
		}
		this.glowing.getChildren().add(this.meshView(pads, this.neonGlow(Color.web("#2ec4ff"), true, random)));

		// A scanner: a line of light that sweeps the field
		this.scanner = box(5, 0.6, h, -w / 2, -0.6, 0, this.neonGlow(Color.web("#ff3fa4"), false, random));
		this.glowing.getChildren().add(this.scanner);

		// Pylons in the corners, with a light on top and a vertical strip
		PhongMaterial metal = material(Color.web("#171c32"));
		TriangleMesh pylonLights = new TriangleMesh();
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				double px = sx * (w / 2 + 16);
				double pz = sz * (h / 2 + 16);
				this.arena.getChildren().add(box(18, 74, 18, px, -37, pz, metal));
				NeonCity.box(pylonLights, px, -77, pz, 24, 6, 24);
				NeonCity.box(pylonLights, px - sx * 9.5, -37, pz - sz * 9.5, 3, 60, 3);
			}
		}
		this.glowing.getChildren().add(this.meshView(pylonLights, this.neonGlow(Color.web("#ff3fa4"), true, random)));

		// A dim grid out to the horizon
		TriangleMesh far = new TriangleMesh();
		double reach = 3600;
		for (int j = 1; j <= 35; j++) {
			for (int sign = -1; sign <= 1; sign += 2) {
				NeonCity.box(far, sign * (w / 2 + 100 * j), 4.6, 0, 3, 0.8, reach * 2);
				NeonCity.box(far, 0, 4.6, sign * (h / 2 + 100 * j), reach * 2, 0.8, 3);
			}
		}
		double continuation = reach - h / 2;
		for (double x = -w / 2; x <= w / 2 + 1; x += 100) {
			NeonCity.box(far, x, 4.6, -(h / 2 + continuation / 2), 3, 0.8, continuation);
			NeonCity.box(far, x, 4.6, h / 2 + continuation / 2, 3, 0.8, continuation);
		}
		double continuationX = reach - w / 2;
		for (double z = -h / 2; z <= h / 2 + 1; z += 100) {
			NeonCity.box(far, -(w / 2 + continuationX / 2), 4.6, z, continuationX, 0.8, 3);
			NeonCity.box(far, w / 2 + continuationX / 2, 4.6, z, continuationX, 0.8, 3);
		}
		this.glowing.getChildren().add(this.meshView(far, this.neonGlow(Color.web("#0d4f73"), false, random)));

		// The skyline: towers with lit windows and neon crowns
		NeonCity city = new NeonCity();
		city.skyline(48, random);
		PhongMaterial facade = new PhongMaterial(Color.gray(0.6));
		facade.setDiffuseMap(NeonTexture.FACADE);
		facade.setSelfIlluminationMap(NeonTexture.FACADE_GLOW);
		this.arena.getChildren().add(this.meshView(city.facades, facade));
		this.glowing.getChildren().add(this.meshView(city.cyan, this.neonGlow(Color.web("#2ec4ff"), false, random)));
		this.glowing.getChildren().add(this.meshView(city.magenta, this.neonGlow(Color.web("#ff3fa4"), false, random)));
	}

	/**
	 * The lights of the neon arena breathe, out of step, and the scanner sweeps
	 */
	private void updateNeon(double dt) {
		if (this.pulseMaterials.isEmpty()) {
			return;
		}
		this.neonClock += dt;
		for (int i = 0; i < this.pulseMaterials.size(); i++) {
			double breath = 0.5 + 0.5 * Math.sin(this.neonClock * 1.3 + this.pulsePhases.get(i));
			this.pulseMaterials.get(i).setDiffuseColor(this.pulseColors.get(i).deriveColor(0, 1, 0.45 + 0.55 * breath, 1));
		}
		if (this.scanner != null) {
			this.scanner.setTranslateX(((this.neonClock / 7) % 1 - 0.5) * (this.fieldWidth + 20));
		}
	}

	/** The lava breathes between these tints of its texture */
	private static final Color LAVA_DIM = Color.gray(0.72);
	private static final Color LAVA_FULL = Color.WHITE;
	private static final Image LAVA_TEXTURE = LavaTexture.create(0);
	/** Far away the lava has cooled, only its veins glow */
	private static final Image CRUST_TEXTURE = LavaTexture.create(0.45);
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

	private static double distance2(double[] a, double[] b) {
		return (a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]);
	}

	private void updateFlows() {
		float[] coordinates = new float[8];
		for (Flow flow : this.flows) {
			// Kept small: the texture repeats every 1
			float u = (float) ((this.clock * flow.du()) % 1);
			float v = (float) ((this.clock * flow.dv()) % 1);
			for (int i = 0; i < 4; i++) {
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

		PhongMaterial basalt = new PhongMaterial(Color.web("#2a1c18"));
		basalt.setSpecularColor(Color.web("#4a3028"));
		basalt.setSpecularPower(10);
		Box floor = new Box(w + 40, 4, h + 40);
		floor.setMaterial(basalt);
		floor.setTranslateY(2);
		this.arena.getChildren().addAll(ground, floor);

		// Lava under the whole field, slowly moving, it shows in the cracks
		this.flow(new double[][] { { -w / 2, -0.3, -h / 2 }, { w / 2, -0.3, -h / 2 }, { w / 2, -0.3, h / 2 }, { -w / 2, -0.3, h / 2 } }, 0.015, 0.025);

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
		String[] tones = { "#2a1c18", "#221613", "#33231e" };
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
			basaltTone.setSpecularColor(Color.web("#4a3028"));
			basaltTone.setSpecularPower(10);
			MeshView view = new MeshView(plates[i]);
			view.setMaterial(basaltTone);
			view.setCullFace(CullFace.NONE);
			this.arena.getChildren().add(view);
		}

		PhongMaterial crack = new PhongMaterial(Color.web("#ff5a14"));
		crack.setSpecularColor(Color.web("#ffb070"));

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
	 * Plates of ice with bright seams, and crystals of ice on the horizon
	 */
	private void buildIce(double w, double h) {
		Random crystals = new Random(23);
		Box ground = new Box(w * 6, 2, h * 6);
		ground.setMaterial(material(Color.web("#8ea4b8")));
		ground.setTranslateY(6);

		// The seams: a light floor under the plates
		Box seams = new Box(w + 40, 4, h + 40);
		seams.setMaterial(material(Color.web("#e4f3ff")));
		seams.setTranslateY(2.6);
		this.arena.getChildren().addAll(ground, seams);

		PhongMaterial[] ice = new PhongMaterial[3];
		String[] tones = { "#5b83a6", "#6990b3", "#5276a0" };
		for (int i = 0; i < ice.length; i++) {
			ice[i] = new PhongMaterial(Color.web(tones[i]));
			ice[i].setSpecularColor(Color.WHITE);
			ice[i].setSpecularPower(90);
		}
		for (double x = 0; x < w; x += 100) {
			for (double z = 0; z < h; z += 100) {
				Box plate = box(Math.min(100, w - x) - 2, 4, Math.min(100, h - z) - 2, x + Math.min(100, w - x) / 2 - w / 2, 2, z + Math.min(100, h - z) / 2 - h / 2,
						ice[crystals.nextInt(ice.length)]);
				this.arena.getChildren().add(plate);
			}
		}

		// Clusters of crystals far away
		PhongMaterial crystal = new PhongMaterial(Color.web("#a9dcf5"));
		crystal.setSpecularColor(Color.WHITE);
		crystal.setSpecularPower(100);
		for (int i = 0; i < 12; i++) {
			double angle = i * Math.PI * 2 / 12 + crystals.nextDouble() * 0.3;
			double distance = 900 + crystals.nextDouble() * 800;
			double cx = Math.cos(angle) * distance;
			double cz = Math.sin(angle) * distance;
			int count = 3 + crystals.nextInt(3);
			for (int k = 0; k < count; k++) {
				double height = 90 + crystals.nextDouble() * 240;
				Cylinder spike = new Cylinder(18 + crystals.nextDouble() * 28, height, 6);
				spike.setMaterial(crystal);
				spike.setTranslateX(cx + (crystals.nextDouble() - 0.5) * 120);
				spike.setTranslateZ(cz + (crystals.nextDouble() - 0.5) * 120);
				spike.setTranslateY(5 - height / 2 + 10);
				spike.getTransforms().addAll(new Rotate((crystals.nextDouble() - 0.5) * 50, Rotate.Z_AXIS), new Rotate((crystals.nextDouble() - 0.5) * 50, Rotate.X_AXIS));
				this.arena.getChildren().add(spike);
			}
		}
	}

	/**
	 * A textured material
	 */
	private static PhongMaterial textured(Image texture, Color specular) {
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
	private MeshView tiledQuad(double[][] corners, PhongMaterial material, double tile) {
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
		double riverZ = h / 2 + 120;
		double riverWidth = 96;
		this.flowStrip(-3000, -riverZ, 3000, -riverZ, riverWidth, 3, 0.16, water, 120);
		this.flowStrip(3000, riverZ, -3000, riverZ, riverWidth, 3, 0.16, water, 120);

		// Waterfalls far at both ends, falling from a cliff to a pool
		PhongMaterial rock = textured(JungleTexture.BARK, Color.gray(0.1));
		for (int side = -1; side <= 1; side += 2) {
			double x = side * (w / 2 + 1000);
			double z = (wild.nextDouble() - 0.5) * 300;
			// The cliff, its face to the field at x
			this.arena.getChildren().add(box(90, 460, 420, x + side * 45, 6 - 230, z, rock));
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
			} while (Math.abs(x) < w / 2 + 25 && Math.abs(z) < h / 2 + 25 || Math.abs(Math.abs(z) - riverZ) < riverWidth / 2 + 14);
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

	private static PhongMaterial material(Color color) {
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

	private static Box box(double w, double h, double d, double x, double y, double z, PhongMaterial material) {
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

	private record HudPlayer(int index, Region energy, Label energyText, HBox pips) {
	}

	private Node buildHud() {
		this.banner.getStyleClass().add("arena-banner");
		this.subBanner.getStyleClass().add("arena-sub-banner");
		VBox center = new VBox(10, this.loading, this.banner, this.subBanner);
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

			Region energy = new Region();
			energy.getStyleClass().add("arena-energy");
			energy.setStyle("-fx-background-color: " + toCss(color) + ";");
			StackPane bar = new StackPane(energy);
			bar.getStyleClass().add("arena-energy-bar");
			bar.setPrefWidth(420);
			StackPane.setAlignment(energy, i == 0 ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
			Label energyText = new Label("100");
			energyText.getStyleClass().add("arena-energy-text");
			HBox pips = new HBox(6);

			HBox nameRow = i == 0 ? new HBox(10, name, team) : new HBox(10, team, name);
			nameRow.setAlignment(i == 0 ? Pos.BOTTOM_LEFT : Pos.BOTTOM_RIGHT);
			HBox barRow = i == 0 ? new HBox(10, energyText, bar) : new HBox(10, bar, energyText);
			barRow.setAlignment(Pos.CENTER);
			VBox panel = new VBox(6, nameRow, barRow, pips);
			panel.setAlignment(i == 0 ? Pos.TOP_LEFT : Pos.TOP_RIGHT);

			this.hudPlayers.add(new HudPlayer(robot.getIndex(), energy, energyText, pips));
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

	private void updatePips() {
		int rounds = this.recording == null ? 0 : this.recording.getRounds().size();
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
			player.energy().setMaxWidth(420 * Math.min(1, energy / 100));
			player.energyText().setText(String.valueOf((int) Math.ceil(energy)));
		}
	}

	private void updateInfo() {
		this.speedLabel.setText(this.paused ? "❚❚ PAUSA" : "▶ " + formatSpeed(SPEEDS[this.speed]) + "x");
		this.viewLabel.setText("◉ " + this.viewMode.label);
		if (this.recording != null) {
			this.roundLabel.setText("ROUND " + (this.round + 1) + "/" + this.recording.getRounds().size());
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
		} else if (this.phase == Phase.FREEZE || this.phase == Phase.REPLAY) {
			this.endReplay();
		} else if (this.phase != Phase.LOADING && this.phase != Phase.FINISHED) {
			this.finish();
		}
	}

	private void finish() {
		this.phase = Phase.FINISHED;
		this.timer.stop();
		this.audio.stop();
		this.stopShowcaseMusic();
		if (this.onFinished != null) {
			Runnable callback = this.onFinished;
			this.onFinished = null;
			callback.run();
		}
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
		this.updateFire(dt * this.effectRate);
		this.updateSunbeams(dt);
		this.updateNeon(dt);
		this.placeBalloons();
	}

	private List<Turn> turns() {
		return this.recording.getRounds().get(this.round);
	}

	private void startRound(int round) {
		this.round = round;
		this.turnPosition = 0;
		this.lastTurnProcessed = -1;
		this.replayed = false;
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
		this.phase = Phase.INTRO;
		this.phaseTime = 0;
		this.fightShown = false;
		this.countdown = 0;
		this.roundTitle = "ROUND " + (round + 1);
		this.roundSubtitle = round + 1 == this.recording.getRounds().size() && round > 0 ? "ROUND DECISIVO" : null;
		this.showBanner(this.roundTitle, this.roundSubtitle);
		this.audio.roundCall();
		this.audio.setMusicOn(true);
		boolean decisive = round + 1 == this.recording.getRounds().size() && round > 0;
		this.announce(decisive ? "Round decisivo! Quem vencer leva a batalha!" : "Round " + (round + 1) + "! Preparem os motores!", 5);
		this.updateInfo();

		// Opens with a crane shot down to the arena
		this.shot = Shot.CRANE;
		this.shotTime = 0;
		this.shotDuration = INTRO_SECONDS + 2.5;
		this.shotSeed = this.random.nextDouble() * Math.PI * 2;
		this.cut();
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
		if (this.phase == Phase.INTRO || this.phase == Phase.SHOWCASE) {
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

		// Bullets in flight
		Set<Integer> flying = new HashSet<Integer>();
		for (Bullet bullet : current.bullets()) {
			if (bullet.isHit() || bullet.state() > BattleRecording.BULLET_HIT_WALL) {
				continue;
			}
			flying.add(bullet.id());
			Bullet after = next.bullets().stream().filter(b -> b.id() == bullet.id()).findFirst().orElse(bullet);
			Sphere sphere = this.bullets.computeIfAbsent(bullet.id(), id -> this.newBullet(bullet));
			sphere.setTranslateX(this.worldX(lerp(bullet.x(), after.x(), fraction)));
			sphere.setTranslateZ(this.worldZ(lerp(bullet.y(), after.y(), fraction)));
		}
		this.bullets.entrySet().removeIf(entry -> {
			if (!flying.contains(entry.getKey())) {
				this.bulletLayer.getChildren().remove(entry.getValue());
				return true;
			}
			return false;
		});
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
			if (this.bulletsSeen.add(bullet.id()) && !bullet.isHit()) {
				this.audio.shot(bullet.power());
				this.muzzleFlash(this.worldX(bullet.x()), this.worldZ(bullet.y()));
			}
			if (bullet.isHit() && this.bulletsExploded.add(bullet.id())) {
				this.explosion(this.worldX(bullet.x()), this.worldZ(bullet.y()), 8 + bullet.power() * 6, Color.web("#ffb000"));
				RobotView victim = this.robots.get(bullet.victim());
				if (bullet.state() == BattleRecording.BULLET_HIT_VICTIM && victim != null) {
					victim.flash();
					this.audio.hit(bullet.power());
					this.hitTension = Math.min(0.25, this.hitTension + 0.08 + bullet.power() * 0.03);
					this.shake += 3 + bullet.power() * 3;
					this.impactCut(bullet.victim(), bullet.power());
				} else {
					this.audio.spark();
					if (bullet.state() == BattleRecording.BULLET_HIT_WALL) {
						this.flashFence(bullet.x(), bullet.y());
					}
				}
			}
		}
		for (RobotState state : turn.robots()) {
			RobotState before = previous == null ? null : find(previous.robots(), state.index(), null);
			RobotView view = this.robots.get(state.index());
			if (view == null) {
				continue;
			}
			if ((state.state() == BattleRecording.ROBOT_HIT_WALL || state.state() == BattleRecording.ROBOT_HIT_ROBOT) && before != null && before.state() != state.state()) {
				this.audio.thud();
				this.shake += 1.5;
				if (state.state() == BattleRecording.ROBOT_HIT_WALL) {
					this.flashFence(state.x(), state.y());
				}
			}
			if (!state.isDead() && state.energy() < 20 && this.lowEnergyWarned.add(state.index())) {
				this.audio.lowEnergy();
			}
			if (state.isDead() && before != null && !before.isDead() && !view.dead) {
				view.destroy();
				this.audio.destroyed();
				this.shake += 22;
				this.whiteFlash();
				if (this.phase == Phase.PLAYING && !this.replayed) {
					this.freezeForReplay(state.index(), turnIndex);
				}
			}
		}
	}

	private Sphere newBullet(Bullet bullet) {
		Sphere sphere = new Sphere(2.5 + bullet.power() * 1.2, 8);
		PhongMaterial glow = new PhongMaterial(Color.web("#fff3a0"));
		glow.setSpecularColor(Color.WHITE);
		sphere.setMaterial(glow);
		sphere.setTranslateY(-22);
		this.bulletLayer.getChildren().add(sphere);
		return sphere;
	}

	private void clearBullets() {
		for (Sphere bullet : this.bullets.values()) {
			this.bulletLayer.getChildren().remove(bullet);
		}
		this.bullets.clear();
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

	private void muzzleFlash(double x, double z) {
		Sphere flash = new Sphere(7, 8);
		flash.setMaterial(new PhongMaterial(Color.web("#fff6c0")));
		flash.setTranslateX(x);
		flash.setTranslateZ(z);
		flash.setTranslateY(-21);
		this.effects.getChildren().add(flash);
		ScaleTransition out = this.effect(new ScaleTransition(Duration.millis(140), flash));
		out.setFromX(1.4);
		out.setFromY(1.4);
		out.setFromZ(1.4);
		out.setToX(0);
		out.setToY(0);
		out.setToZ(0);
		out.setOnFinished(event -> this.effects.getChildren().remove(flash));
		out.play();
	}

	/**
	 * A fireball that grows and vanishes, with flying debris
	 */
	private void explosion(double x, double z, double size, Color color) {
		Group blast = new Group();
		blast.setTranslateX(x);
		blast.setTranslateZ(z);
		blast.setTranslateY(-18);

		Sphere fire = new Sphere(size / 2, 10);
		fire.setMaterial(new PhongMaterial(color));
		blast.getChildren().add(fire);
		ParallelTransition all = new ParallelTransition(grow(fire, 0.2, 1.4, 420), shrink(fire, 420, 260));

		PhongMaterial debrisMaterial = new PhongMaterial(Color.web("#ffe766"));
		int pieces = size > 30 ? 14 : 6;
		for (int i = 0; i < pieces; i++) {
			double angle = Math.PI * 2 * i / pieces;
			Box debris = new Box(3, 3, 3);
			debris.setMaterial(debrisMaterial);
			blast.getChildren().add(debris);
			all.getChildren().add(this.fly(debris, Math.cos(angle) * size * (0.9 + Math.random() * 0.8), Math.sin(angle) * size * (0.9 + Math.random() * 0.8), size * 0.6 * Math.random(), 650));
		}
		this.effects.getChildren().add(blast);
		this.effect(all).setOnFinished(event -> this.effects.getChildren().remove(blast));
		all.play();
		this.lightFlash(x, z, Color.web("#ffb000"), 0.6);
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
		this.announce("Frente a frente: " + names.replace("  ×  ", " contra ") + "!", 5);
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
		for (int t = Math.max(0, turn - 3); t <= Math.min(turns.size() - 1, turn + 1) && this.fatalBullet < 0; t++) {
			for (Bullet bullet : turns.get(t).bullets()) {
				if (bullet.state() == BattleRecording.BULLET_HIT_VICTIM && bullet.victim() == robot) {
					this.fatalBullet = bullet.id();
					this.impactTurn = t;
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
		// Bullets already flying when the replay starts are not new shots
		if (this.replayFrom > 0) {
			for (Bullet bullet : this.turns().get(this.replayFrom - 1).bullets()) {
				this.bulletsSeen.add(bullet.id());
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
		if (this.viewMode == ViewMode.CINEMATIC || this.phase == Phase.REPLAY || this.phase == Phase.FREEZE || this.phase == Phase.SHOWCASE) {
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
		double[] mid = { (a[0] + b[0]) / 2, -16, (a[2] + b[2]) / 2 };
		double dx = b[0] - a[0];
		double dz = b[2] - a[2];
		double separation = Math.max(10, Math.hypot(dx, dz));
		double ux = dx / separation;
		double uz = dz / separation;
		// Perpendicular to the line between them
		double px = -uz * this.shotSide;
		double pz = ux * this.shotSide;
		this.rollTarget = 0;

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
		if (this.robots.size() >= 2 && livingRobots.size() == 1) {
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
		Sphere bullet = this.bullets.get(this.fatalBullet);
		double turn = this.turnPosition;
		if (bullet != null && this.impactTurn >= 0 && turn < this.impactTurn) {
			double bx = bullet.getTranslateX();
			double bz = bullet.getTranslateZ();
			double dx = victim[0] - bx;
			double dz = victim[2] - bz;
			double length = Math.max(1, Math.hypot(dx, dz));
			double ux = dx / length;
			double uz = dz / length;
			this.setShot(bx - ux * 60 - uz * 22 * this.shotSide, -30, bz - uz * 60 + ux * 22 * this.shotSide, bx + ux * 140, -20, bz + uz * 140, 38, 9);
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
		boolean show = this.phase != Phase.LOADING && this.phase != Phase.REPLAY && this.phase != Phase.SHOWCASE;
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
		final Color bodyColor;
		final Color gunColor;
		final Color playerColor;
		final Group tombstone;
		final VBox balloon = new VBox(4);
		final Region energyFill = new Region();
		final Label nameLabel;
		final String displayName;
		final List<Animation> running = new ArrayList<Animation>();
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

		RobotView(BattleRecording.Robot robot, Opponent opponent, int order) {
			this.playerColor = PLAYER_COLORS[order % PLAYER_COLORS.length];
			this.bodyColor = argb(robot.getBodyColor(), this.playerColor);
			this.gunColor = argb(robot.getGunColor(), this.bodyColor);
			this.bodyMaterial = material(this.bodyColor);
			this.gunMaterial = material(this.gunColor);
			PhongMaterial radarMaterial = material(argb(robot.getRadarColor(), Color.web("#ffd400")));
			PhongMaterial treadMaterial = material(Color.web("#22252e"));
			PhongMaterial playerMaterial = material(this.playerColor);

			// Body, pointing to +Z (north when the heading is 0)
			this.body.getTransforms().add(this.bodyRotate);
			this.body.getChildren().addAll( //
					box(7, 12, 38, -15, -6, 0, treadMaterial), //
					box(7, 12, 38, 15, -6, 0, treadMaterial), //
					box(24, 10, 32, 0, -10, 0, this.bodyMaterial), //
					box(18, 3, 6, 0, -16, 13, playerMaterial)); // front marker in the player color

			PhongMaterial metal = material(Color.web("#2c303b"));
			PhongMaterial steel = material(Color.web("#7a8296"));
			PhongMaterial lamp = new PhongMaterial(Color.web("#fff0a8"));
			lamp.setSpecularColor(Color.WHITE);
			PhongMaterial tail = new PhongMaterial(Color.web("#ff2d2d"));
			tail.setSpecularColor(Color.web("#ff9090"));
			for (int side = -1; side <= 1; side += 2) {
				// Tread: wheels on the outside and ridges on the top
				for (double z = -15; z <= 15; z += 7.5) {
					this.body.getChildren().add(cylinder(4.8, 1.4, 10, side * 18.6, -6, z, Rotate.Z_AXIS, 90, steel));
					this.body.getChildren().add(cylinder(2, 1.9, 6, side * 18.7, -6, z, Rotate.Z_AXIS, 90, metal));
				}
				for (double z = -16; z <= 16; z += 4) {
					this.body.getChildren().add(box(7.6, 1, 2.2, side * 15, -12.3, z, metal));
				}
				// Side skirt and air intakes
				this.body.getChildren().add(box(1.4, 6.5, 26, side * 12.8, -8.5, 0, this.gunMaterial));
				for (double z = -9; z <= -3; z += 3) {
					this.body.getChildren().add(box(0.5, 3.4, 1.2, side * 12.2, -11, z, metal));
				}
				// Lights and exhaust
				this.body.getChildren().add(box(3.2, 2.2, 1.2, side * 8, -10, 16.3, lamp));
				this.body.getChildren().add(box(2.6, 1.8, 1, side * 9, -9, -16.4, tail));
				this.body.getChildren().add(cylinder(1.7, 7, 8, side * 6, -11.5, -18.5, Rotate.X_AXIS, 90, metal));
				this.body.getChildren().add(cylinder(2.1, 1, 8, side * 6, -11.5, -22.2, Rotate.X_AXIS, 90, steel));
			}
			// Sloped front plate, top plate and a sensor
			Box glacis = box(22, 2.2, 9, 0, -14.6, 15, this.bodyMaterial);
			glacis.setRotationAxis(Rotate.X_AXIS);
			glacis.setRotate(-20);
			this.body.getChildren().addAll(glacis, box(15, 1, 13, 0, -15.4, -5, metal), box(4, 1.6, 4, 5, -16.6, -7, steel));

			// Turret and gun turn on their own
			this.turret.getTransforms().add(this.gunRotate);
			Cylinder dome = new Cylinder(10, 8, 8);
			dome.setMaterial(this.gunMaterial);
			dome.setTranslateY(-19);
			this.turret.getChildren().addAll(dome, box(4, 4, 24, 0, -20, 14, this.gunMaterial), //
					cylinder(11.2, 2, 12, 0, -15.5, 0, Rotate.X_AXIS, 0, metal), // ring of the turret
					box(8, 7, 8, 0, -20, 8, this.gunMaterial), // mantle of the gun
					cylinder(2.6, 1.3, 8, 0, -20, 19, Rotate.X_AXIS, 90, steel), // ring on the barrel
					cylinder(2.9, 5, 8, 0, -20, 27.5, Rotate.X_AXIS, 90, metal), // muzzle brake
					box(2, 2, 10, 5.8, -18, 12, metal), // coaxial gun
					box(6, 1.4, 6, -3.2, -23.6, -2, metal), // hatch
					cylinder(0.8, 4, 6, -3.2, -24.8, -2, Rotate.Z_AXIS, 90, steel)); // its handle

			this.radar.getTransforms().add(this.radarRotate);
			Cylinder dish = cylinder(7.5, 1.6, 14, 0, -33, 3, Rotate.X_AXIS, 80, radarMaterial);
			this.radar.getChildren().addAll(box(2, 8, 2, 0, -27, 0, radarMaterial), dish, //
					cylinder(1.8, 3, 6, 0, -32.6, 4.6, Rotate.X_AXIS, 80, lamp), // the feed in front of the dish
					cylinder(0.6, 14, 6, 4, -35, -2, Rotate.X_AXIS, 0, steel)); // antenna

			this.tank.getChildren().addAll(this.body, this.turret, this.radar);

			// Shown when it is destroyed
			this.tombstone = new Group();
			PhongMaterial stone = material(Color.web("#8a8fa3"));
			Cylinder top = new Cylinder(11, 6, 12);
			top.setMaterial(stone);
			top.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
			top.setTranslateY(-26);
			this.tombstone.getChildren().addAll(box(22, 26, 6, 0, -13, 0, stone), top, box(30, 4, 14, 0, -2, 0, stone));
			this.tombstone.setVisible(false);

			this.root.getChildren().addAll(this.tank, this.tombstone);

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
		 * Blinks white when hit
		 */
		void flash() {
			if (this.dead) {
				return;
			}
			this.bodyMaterial.setDiffuseColor(Color.WHITE);
			PauseTransition back = BattleArena3D.this.effect(new PauseTransition(Duration.millis(110)));
			back.setOnFinished(event -> this.bodyMaterial.setDiffuseColor(this.dead ? Color.web("#262626") : this.bodyColor));
			back.play();
		}

		/**
		 * Blows up: fireballs, a shockwave, smoke, debris, the turret thrown in the
		 * air and a burnt wreck, that becomes a tombstone
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

			// Core, fireball and a second blast
			Sphere core = new Sphere(16, 12);
			core.setMaterial(new PhongMaterial(Color.web("#fffbe0")));
			core.setTranslateY(-20);
			Sphere fire = new Sphere(34, 14);
			fire.setMaterial(new PhongMaterial(Color.web("#ff7a1a")));
			fire.setTranslateY(-26);
			Sphere second = new Sphere(26, 12);
			second.setMaterial(new PhongMaterial(Color.web("#ff3d2e")));
			second.setTranslateY(-40);
			second.setScaleX(0);
			second.setScaleY(0);
			second.setScaleZ(0);
			blast.getChildren().addAll(fire, second, core);
			all.getChildren().addAll(grow(core, 0.1, 1.6, 180), shrink(core, 180, 200), grow(fire, 0.2, 1.8, 600), shrink(fire, 650, 500));
			ScaleTransition secondGrow = grow(second, 0, 1.7, 450);
			secondGrow.setDelay(Duration.millis(260));
			all.getChildren().addAll(secondGrow, shrink(second, 750, 450));

			// Shockwave on the floor
			Cylinder ring = new Cylinder(12, 1.5, 32);
			ring.setMaterial(new PhongMaterial(Color.web("#ffe766")));
			ring.setTranslateY(-1);
			blast.getChildren().add(ring);
			ScaleTransition wave = new ScaleTransition(Duration.millis(700), ring);
			wave.setToX(14);
			wave.setToZ(14);
			wave.setInterpolator(Interpolator.EASE_OUT);
			all.getChildren().addAll(wave, shrink(ring, 600, 250));

			// Smoke rising
			PhongMaterial smoke = new PhongMaterial(Color.web("#2b2b33"));
			for (int i = 0; i < 7; i++) {
				Sphere puff = new Sphere(10 + BattleArena3D.this.random.nextDouble() * 8, 8);
				puff.setMaterial(smoke);
				puff.setTranslateX((BattleArena3D.this.random.nextDouble() - 0.5) * 30);
				puff.setTranslateZ((BattleArena3D.this.random.nextDouble() - 0.5) * 30);
				puff.setTranslateY(-20);
				puff.setScaleX(0.1);
				puff.setScaleY(0.1);
				puff.setScaleZ(0.1);
				blast.getChildren().add(0, puff);
				Timeline rise = new Timeline(new KeyFrame(Duration.millis(2200), //
						new KeyValue(puff.translateYProperty(), -90 - BattleArena3D.this.random.nextDouble() * 60, Interpolator.EASE_OUT), //
						new KeyValue(puff.scaleXProperty(), 2.2), new KeyValue(puff.scaleYProperty(), 2.2), new KeyValue(puff.scaleZProperty(), 2.2)));
				rise.setDelay(Duration.millis(200 + i * 70));
				all.getChildren().addAll(rise, shrink(puff, 2400, 600));
			}

			// Debris in the colors of the robot
			PhongMaterial[] colors = { this.bodyMaterial, this.gunMaterial, new PhongMaterial(Color.web("#22252e")), new PhongMaterial(Color.web("#ffb000")) };
			for (int i = 0; i < 18; i++) {
				double angle = Math.PI * 2 * i / 18 + BattleArena3D.this.random.nextDouble() * 0.3;
				double size = 3 + BattleArena3D.this.random.nextDouble() * 5;
				Box piece = new Box(size, size, size);
				piece.setMaterial(colors[i % colors.length]);
				piece.setTranslateY(-15);
				blast.getChildren().add(piece);
				double reach = 60 + BattleArena3D.this.random.nextDouble() * 90;
				all.getChildren().add(BattleArena3D.this.fly(piece, Math.cos(angle) * reach, Math.sin(angle) * reach, 40 + BattleArena3D.this.random.nextDouble() * 90, 1300));
			}

			// The turret and the radar are thrown in the air
			double throwAngle = BattleArena3D.this.random.nextDouble() * Math.PI * 2;
			this.turret.setRotationAxis(new Point3D(1, 0, 0.4));
			this.radar.setRotationAxis(new Point3D(0.3, 0, 1));
			Timeline thrown = new Timeline( //
					new KeyFrame(Duration.millis(550), new KeyValue(this.turret.translateYProperty(), -140, Interpolator.EASE_OUT), new KeyValue(this.radar.translateYProperty(), -190, Interpolator.EASE_OUT)), //
					new KeyFrame(Duration.millis(1400), //
							new KeyValue(this.turret.translateYProperty(), 14, Interpolator.EASE_IN), //
							new KeyValue(this.turret.translateXProperty(), Math.cos(throwAngle) * 70, Interpolator.EASE_OUT), //
							new KeyValue(this.turret.translateZProperty(), Math.sin(throwAngle) * 70, Interpolator.EASE_OUT), //
							new KeyValue(this.turret.rotateProperty(), 720, Interpolator.LINEAR), //
							new KeyValue(this.radar.translateYProperty(), 24, Interpolator.EASE_IN), //
							new KeyValue(this.radar.translateXProperty(), -Math.cos(throwAngle) * 90, Interpolator.EASE_OUT), //
							new KeyValue(this.radar.translateZProperty(), -Math.sin(throwAngle) * 90, Interpolator.EASE_OUT), //
							new KeyValue(this.radar.rotateProperty(), -900, Interpolator.LINEAR)));
			all.getChildren().add(thrown);

			// Burnt wreck, a bit tilted
			this.bodyMaterial.setDiffuseColor(Color.web("#262626"));
			this.gunMaterial.setDiffuseColor(Color.web("#303030"));
			this.body.setRotationAxis(Rotate.Z_AXIS);
			all.getChildren().add(new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(this.body.rotateProperty(), 9, Interpolator.EASE_OUT))));

			BattleArena3D.this.effects.getChildren().add(blast);
			BattleArena3D.this.effect(all).setOnFinished(event -> BattleArena3D.this.effects.getChildren().remove(blast));
			this.running.add(all);
			all.play();
			BattleArena3D.this.lightFlash(x, z, Color.web("#ff8a2a"), 1.4);

			// Later the wreck gives place to the tombstone
			this.tombstone.setVisible(false);
			this.tombstone.setTranslateY(34);
			Timeline grave = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(this.tombstone.translateYProperty(), 34)), new KeyFrame(Duration.millis(1), event -> this.tombstone.setVisible(true)),
					new KeyFrame(Duration.millis(900), new KeyValue(this.tombstone.translateYProperty(), 0, Interpolator.EASE_OUT)));
			grave.setDelay(Duration.millis(2300));
			grave.setOnFinished(event -> this.tank.setVisible(false));
			BattleArena3D.this.effect(grave);
			this.running.add(grave);
			grave.play();

			this.nameLabel.setText("☠ " + this.displayName);
			this.balloon.setOpacity(0.6);
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
			this.tombstone.setVisible(false);
			this.bodyMaterial.setDiffuseColor(this.bodyColor);
			this.gunMaterial.setDiffuseColor(this.gunColor);
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
				if (t < 0.3) {
					color = Color.web("#fff6c8").interpolate(Color.web("#ffb12a"), t / 0.3);
				} else {
					color = Color.web("#ffb12a").interpolate(Color.web("#e8380d"), (t - 0.3) / 0.7);
				}
				color = Color.color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(1, (1 - t) * 1.6) * flicker);
			}
			case SMOKE -> {
				// Born in the fire, grows and fades
				width = p.size * (0.6 + 2.6 * Math.sqrt(t));
				height = width;
				double alpha = Math.min(1, t * 6) * Math.pow(1 - t, 1.4) * 0.7;
				Color ember = Color.web("#ff7a2a");
				Color gray = Color.gray(p.shade);
				color = t < 0.12 ? ember.interpolate(gray, t / 0.12) : gray.interpolate(Color.gray(Math.min(0.4, p.shade + 0.12)), (t - 0.12) / 0.88);
				color = Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
			}
			case EMBER -> {
				double flicker = 0.7 + 0.3 * this.random.nextDouble();
				width = p.size * (1 - 0.3 * t) * flicker;
				height = width;
				double alpha = (t < 0.1 ? t * 10 : 1 - (t - 0.1) / 0.9) * 0.95;
				color = Color.color(1, 0.62 - 0.4 * t, 0.15 - 0.1 * t, Math.max(0, alpha));
			}
			case FIREFLY -> {
				width = p.size;
				height = width;
				// Fades in and out and blinks
				double blink = 0.5 + 0.5 * Math.sin(p.age * 4.2 + p.shade * 20);
				double envelope = Math.min(1, Math.min(t * 6, (1 - t) * 6));
				color = Color.color(0.78, 1, 0.35, Math.max(0, envelope * (0.35 + 0.65 * blink)));
			}
			case SNOW -> {
				width = p.size;
				height = width;
				color = Color.color(0.94, 0.97, 1, Math.min(0.9, p.age * 1.5));
			}
			default -> {
				width = p.size * (1 - 0.5 * t);
				height = width;
				color = Color.color(1, 0.85 - 0.4 * t, 0.4 - 0.3 * t, 1 - t * t);
			}
			}
			p.material.setDiffuseColor(color);
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
		this.particles.sort((a, b) -> Double.compare(b.distance, a.distance));
		List<Node> views = new ArrayList<Node>(this.particles.size());
		for (Particle p : this.particles) {
			views.add(p.view);
		}
		this.fireLayer.getChildren().setAll(views);
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
	 * A robocode ARGB color, or the fallback if the robot did not choose one (or
	 * chose black, invisible on the dark arena)
	 */
	private static Color argb(int argb, Color fallback) {
		int r = (argb >> 16) & 0xff;
		int g = (argb >> 8) & 0xff;
		int b = argb & 0xff;
		if (r + g + b < 60) {
			return fallback.deriveColor(0, 0.8, 0.7, 1);
		}
		return Color.rgb(r, g, b);
	}
}
