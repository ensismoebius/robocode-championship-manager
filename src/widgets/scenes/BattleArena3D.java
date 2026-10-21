package widgets.scenes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;


import javafx.animation.AnimationTimer;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.CullFace;
import javafx.scene.image.WritableImage;
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
import widgets.scenes.ArenaFence.FenceSegment;
import static widgets.scenes.ArenaMaterials.distance2;
import static widgets.scenes.ArenaMaterials.energy;
import static widgets.scenes.ArenaMaterials.material;

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

		/** A camera view: its label, the pitch of the camera and its field of view. */
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
	Runnable onFinished;
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
	final Group fence = new Group();
	/** The looks of the arena */
	final ArenaStyle style;
	/** Parts of the arena that glow, like lava: lit by the fire glow */
	final Group glowing = new Group();
	/** The lava of the volcano arena, pulsing */
	PhongMaterial lava;
	final List<Flow> flows = new ArrayList<Flow>();
	double clock = 0;
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
	final ArenaControls controls = new ArenaControls(this);
	final LavaFlows lavaFlows = new LavaFlows(this);
	final ArenaPhotos photographer = new ArenaPhotos(this);
	final ArenaFence forceField = new ArenaFence(this);
	/** Translucent panels of the cage, each one with its own material to flash */
	final List<FenceSegment> fenceSegments = new ArrayList<FenceSegment>();
	final SubScene subScene;
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
	final List<BattlePhoto> photos = new ArrayList<BattlePhoto>();
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

	final AnimationTimer timer = new AnimationTimer() {
		private long last = -1;

		@Override
		/** Advances the battle by the time since the last frame. */
		public void handle(long now) {
			double deltaTime = this.last < 0 ? 0 : Math.min(0.1, (now - this.last) / 1e9);
			this.last = now;
			BattleArena3D.this.update(deltaTime);
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

	/** Creates the arena of a battle between the fighters. */
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

		this.controls.installControls();
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

	/** The fighter that drives the given robot of the recording, or null when it is not one of them. */
	Opponent opponentOf(BattleRecording.Robot robot) {
		String name = BattleControl.robotClassName(robot.getName());
		return this.fighters.stream().filter(other -> other.getFullName().equals(name)).findFirst().orElse(null);
	}

	// ------------------------------------------------------------------ scene

	/** Lights the arena: the ambient light, a key light and the glow of the fire. */
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
		double width = this.fieldWidth;
		double height = this.fieldHeight;
		switch (this.style) {
		case NEON -> this.neon.build(width, height);
		case VOLCANO -> this.volcano.build(width, height);
		case ICE -> this.ice.build(width, height);
		case JUNGLE -> this.jungle.build(width, height);
		}
		this.arena.getChildren().add(this.glowing);
		this.forceField.buildFence(width, height);
		this.critters = new Critters(this.style, width, height, this.random);
		this.arena.getChildren().add(this.critters.root());
		if (this.style == ArenaStyle.ICE && this.ice.seaRadius() > 0) {
			double centreX = this.ice.seaX();
			double centreZ = this.ice.seaZ();
			double radius = this.ice.seaRadius();
			this.critters.setWater((posX, posZ) -> Math.hypot(posX - centreX, posZ - centreZ) < IceArena.shore(Math.atan2(posZ - centreZ, posX - centreX), radius) * 0.97);
		}
	}

	/** A mesh view with the given material, visible from both sides. */
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
	 * Robocode position to the world: the field is centered, north is +Z, up is
	 * -Y
	 */
	double worldX(double posX) {
		return posX - this.fieldWidth / 2;
	}

	/** The z coordinate of the arena for a y coordinate of the recording. */
	double worldZ(double posY) {
		return posY - this.fieldHeight / 2;
	}

	// ------------------------------------------------------------------- HUD

	/** The CSS colour of a colour, as #rrggbb. */
	static String toCss(Color color) {
		return String.format("#%02x%02x%02x", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
	}

	/** Switches to the next camera view. */
	void cycleView() {
		this.controls.setView(ViewMode.values()[(this.viewMode.ordinal() + 1) % ViewMode.values().length]);
		Sfx.blip();
	}

	/**
	 * The sounds of the battle fade to silence
	 */
	public void fadeOutAudio(double seconds) {
		this.audio.fadeOut(seconds);
	}

	// ----------------------------------------------------------------- update

	/** Advances the battle state machine by the time since the last frame. */
	private void update(double deltaTime) {
		this.phaseTime += deltaTime;

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
		case PLAYING -> this.rounds.updatePlaying(deltaTime);
		case FREEZE -> {
			if (this.phaseTime > FREEZE_SECONDS) {
				this.showcase.startReplay();
			}
		}
		case REPLAY -> this.rounds.updateReplay(deltaTime);
		case ROUND_END -> this.rounds.updateRoundEnd();
		case FINISHED -> {
		}
		}

		this.rounds.updateTension(deltaTime);
		this.announcer.tick(deltaTime);
		this.director.updateCamera(deltaTime);
		this.clock += deltaTime * this.effectRate;
		this.updateSurfaces();
		this.updateAudio(deltaTime);
		this.updateEffects(deltaTime);
	}

	/** The lava breathes and its flows slide */
	private void updateSurfaces() {
	if (this.lava != null) {
		this.lava.setDiffuseColor(LAVA_DIM.interpolate(LAVA_FULL, 0.5 + 0.5 * Math.sin(this.clock * 2.1) * Math.sin(this.clock * 0.7 + 1)));
	}
	if (!this.flows.isEmpty()) {
		this.lavaFlows.updateFlows();
	}
	}

	/** The ears of the sound are the camera; each robot makes its machine sound */
	private void updateAudio(double deltaTime) {
	// The ears are the camera: the sounds come from the side of the screen they are on
	this.audio.setListener(this.eye[0], this.eye[2], this.look[0] - this.eye[0], this.look[2] - this.eye[2]);
	for (Map.Entry<Integer, RobotView> entry : this.robots.entrySet()) {
		RobotView robotView = entry.getValue();
		robotView.roll(deltaTime);
		double[] atValue = robotView.position();
		this.audio.setMachine(entry.getKey(), robotView.treadSpeed, robotView.turretSpeed, atValue[0], atValue[2]);
	}
	}

	/** The fire, the sunbeams, the neon and the ice, the critters, the balloons and the photos */
	private void updateEffects(double deltaTime) {
	this.fire.update(deltaTime * this.effectRate);
	this.jungle.updateSunbeams(deltaTime);
	this.neon.update(deltaTime);
	this.ice.update(deltaTime);
	if (this.critters != null) {
		this.critters.update(deltaTime);
	}
	this.director.placeBalloons();
	this.photographer.flushPhotos(deltaTime);
	}

	/**
	 * The photos of the key moments taken so far, in order
	 */
	public List<BattlePhoto> photos() {
		return List.copyOf(this.photos);
	}

	/** The turns of the current round of the recording. */
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

	/** The bullet with the given identifier in a turn, or null when there is none. */
	static Bullet findBullet(Turn turn, int identifier) {
		for (Bullet bullet : turn.bullets()) {
			if (bullet.id() == identifier) {
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

	/** The state of the robot with the given index, or the fallback when it is not in the list. */
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
		for (int posY = 0; posY < 2; posY++) {
			for (int posX = 0; posX < 2; posX++) {
				map.getPixelWriter().setColor(posX, posY, color);
			}
		}
		return map;
	}

	/** Stops the music of the showcase, if it is playing. */
	void stopShowcaseMusic() {
		if (this.showcaseMusic != null) {
			this.showcaseMusic.stop();
			this.showcaseMusic = null;
		}
	}

	/** Linear interpolation between two values. */
	static double lerp(double first, double second, double time) {
		return first + (second - first) * time;
	}

	/**
	 * Interpolates angles (radians) the short way
	 */
	static double lerpAngle(double first, double second, double time) {
		double difference = Math.IEEEremainder(second - first, Math.PI * 2);
		return first + difference * time;
	}

	// ------------------------------------------------------------------ robot

	/** Robots catch fire below this energy */
	static final double BURN_ENERGY = 50;
	/** Where the hull burns, in the body coordinates: engine, side, turret */
	static final double[][] BURN_SPOTS = { { 0, -15, -11 }, { 8, -15, 3 }, { -5, -23, 1 } };

}
