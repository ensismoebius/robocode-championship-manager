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
import javafx.animation.Timeline;
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
import javafx.scene.control.Label;
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
import javafx.util.Duration;
import model.Opponent;
import utils.robocode.BattleControl;
import utils.robocode.BattleRecording;
import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;
import widgets.BattleAudio;
import widgets.Sfx;
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
	private static final double[] SPEEDS = { 0.5, 1, 2, 4, 8 };
	private static final int NORMAL_SPEED = 1;

	private static final double INTRO_SECONDS = 2.4;
	private static final double ROUND_END_SECONDS = 2.8;
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
		LOADING, INTRO, PLAYING, FREEZE, REPLAY, ROUND_END, FINISHED
	}

	private final List<Opponent> fighters;
	private Runnable onFinished;
	private final BattleAudio audio = new BattleAudio();
	private final Random random = new Random();

	// 3D
	private final Group world = new Group();
	private final Group arena = new Group();
	private final Group effects = new Group();
	private final SubScene subScene;
	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final Translate cameraLook = new Translate();
	private final Rotate cameraYaw = new Rotate(0, Rotate.Y_AXIS);
	private final Rotate cameraPitch = new Rotate(-38, Rotate.X_AXIS);
	private final Rotate cameraRoll = new Rotate(0, Rotate.Z_AXIS);
	private final Translate cameraDistance = new Translate();
	/** Lights up the explosions */
	private final PointLight flashLight = new PointLight(Color.BLACK);

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
	private int speed = NORMAL_SPEED;
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

		this.world.getChildren().addAll(this.arena, this.effects);
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
		this.subScene.setFill(Color.web("#0a0b18"));
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
		this.getChildren().setAll(this.subScene, this.balloons, this.buildLetterbox(), this.buildHud(), this.whiteFlash, scanlines);

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
			this.world.getChildren().add(view.root);
			this.balloons.getChildren().add(view.balloon);
			this.roundWins.put(robot.getIndex(), 0);
		}
		this.buildHudPlayers();
		this.loading.setVisible(false);

		this.audio.start();
		this.startRound(0);
	}

	private Opponent opponentOf(BattleRecording.Robot robot) {
		String name = BattleControl.robotClassName(robot.getName());
		return this.fighters.stream().filter(o -> o.getFullName().equals(name)).findFirst().orElse(null);
	}

	// ------------------------------------------------------------------ scene

	private void buildLights() {
		AmbientLight ambient = new AmbientLight(Color.gray(0.5));
		PointLight key = new PointLight(Color.gray(0.8));
		key.getTransforms().add(new Translate(-300, -900, -500));
		this.world.getChildren().addAll(ambient, key, this.flashLight);
	}

	/**
	 * Floor with a neon grid and a cage of lines as walls
	 */
	private void buildArena() {
		double w = this.fieldWidth;
		double h = this.fieldHeight;

		Box floor = new Box(w + 40, 4, h + 40);
		floor.setMaterial(material(Color.web("#0d1020")));
		floor.setTranslateY(2);

		Box ground = new Box(w * 6, 2, h * 6);
		ground.setMaterial(material(Color.web("#07080f")));
		ground.setTranslateY(6);
		this.arena.getChildren().addAll(ground, floor);

		PhongMaterial gridMaterial = material(Color.web("#2ec4ff"));
		for (double x = 0; x <= w; x += 50) {
			this.arena.getChildren().add(box(1.5, 0.5, h, x - w / 2, -0.3, 0, gridMaterial));
		}
		for (double y = 0; y <= h; y += 50) {
			this.arena.getChildren().add(box(w, 0.5, 1.5, 0, -0.3, y - h / 2, gridMaterial));
		}

		// Walls: posts and rails, like a cage
		PhongMaterial wallMaterial = material(Color.web("#ff3d6e"));
		double wallHeight = 60;
		for (double x = 0; x <= w; x += 100) {
			this.arena.getChildren().add(box(3, wallHeight, 3, x - w / 2, -wallHeight / 2, -h / 2, wallMaterial));
			this.arena.getChildren().add(box(3, wallHeight, 3, x - w / 2, -wallHeight / 2, h / 2, wallMaterial));
		}
		for (double y = 100; y < h; y += 100) {
			this.arena.getChildren().add(box(3, wallHeight, 3, -w / 2, -wallHeight / 2, y - h / 2, wallMaterial));
			this.arena.getChildren().add(box(3, wallHeight, 3, w / 2, -wallHeight / 2, y - h / 2, wallMaterial));
		}
		for (double level : new double[] { 20, 40, 60 }) {
			this.arena.getChildren().add(box(w, 2, 2, 0, -level, -h / 2, wallMaterial));
			this.arena.getChildren().add(box(w, 2, 2, 0, -level, h / 2, wallMaterial));
			this.arena.getChildren().add(box(2, 2, h, -w / 2, -level, 0, wallMaterial));
			this.arena.getChildren().add(box(2, 2, h, w / 2, -level, 0, wallMaterial));
		}
	}

	private static PhongMaterial material(Color color) {
		PhongMaterial material = new PhongMaterial(color);
		material.setSpecularColor(color.brighter());
		material.setSpecularPower(24);
		return material;
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
		this.replayTag.relocate(36, 26);
		this.replayTag.setManaged(false);
		this.replayTag.setVisible(false);

		Pane frame = new Pane(this.letterboxTop, this.letterboxBottom, this.replayTag);
		frame.setMouseTransparent(true);
		return frame;
	}

	private void showLetterbox(boolean show) {
		double height = show ? this.getHeight() * 0.12 : 0;
		Timeline bars = new Timeline(new KeyFrame(Duration.millis(450), new KeyValue(this.letterboxTop.heightProperty(), height, Interpolator.EASE_BOTH),
				new KeyValue(this.letterboxBottom.heightProperty(), height, Interpolator.EASE_BOTH)));
		bars.play();
		this.replayTag.setVisible(show);
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
			case ENTER, ESCAPE -> this.skip();
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
	 * Enter: ends the replay, or the whole battle
	 */
	private void skip() {
		if (this.phase == Phase.FREEZE || this.phase == Phase.REPLAY) {
			this.endReplay();
		} else if (this.phase != Phase.LOADING && this.phase != Phase.FINISHED) {
			this.finish();
		}
	}

	private void finish() {
		this.phase = Phase.FINISHED;
		this.timer.stop();
		this.audio.stop();
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
		case INTRO -> {
			if (this.phaseTime > INTRO_SECONDS * 0.55 && !"FIGHT!".equals(this.banner.getText())) {
				this.showBanner("FIGHT!", null);
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
			if (this.phaseTime > ROUND_END_SECONDS) {
				if (this.round + 1 < this.recording.getRounds().size()) {
					this.startRound(this.round + 1);
				} else {
					this.finish();
				}
			}
		}
		case FINISHED -> {
		}
		}

		this.updateTension(dt);
		this.updateCamera(dt);
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
		this.showBanner("ROUND " + (round + 1), round + 1 == this.recording.getRounds().size() && round > 0 ? "ROUND DECISIVO" : null);
		this.audio.roundCall();
		this.audio.setMusicOn(true);
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
		if (alive.size() == 1) {
			int winner = alive.get(0).index();
			this.roundWins.merge(winner, 1, Integer::sum);
			RobotView view = this.robots.get(winner);
			this.showBanner("K.O.!", (view == null ? "" : view.displayName) + " vence o round");
			this.audio.knockOut();
			// The winner, from close
			this.shot = Shot.CLOSE;
			this.shotSubject = winner;
			this.shotTime = 0;
			this.shotDuration = ROUND_END_SECONDS + 1;
			this.shotSide = this.random.nextBoolean() ? 1 : -1;
			this.closeDistance = 110;
			this.cut();
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
		if (this.phase == Phase.INTRO) {
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
				this.world.getChildren().remove(entry.getValue());
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
		this.world.getChildren().add(sphere);
		return sphere;
	}

	private void clearBullets() {
		for (Sphere bullet : this.bullets.values()) {
			this.world.getChildren().remove(bullet);
		}
		this.bullets.clear();
	}

	private void whiteFlash() {
		FadeTransition flash = new FadeTransition(Duration.millis(380), this.whiteFlash);
		flash.setFromValue(0.75);
		flash.setToValue(0);
		flash.play();
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
		this.cut();
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
		this.shotSide = this.random.nextBoolean() ? 1 : -1;
		this.cut();
	}

	private void updateCamera(double dt) {
		this.shotTime += dt;
		if (this.viewMode == ViewMode.CINEMATIC || this.phase == Phase.REPLAY || this.phase == Phase.FREEZE) {
			if (this.shotTime > this.shotDuration && this.phase == Phase.PLAYING) {
				this.pickShot(true);
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
		double[] mid = { (a[0] + b[0]) / 2, 0, (a[2] + b[2]) / 2 };
		double dx = b[0] - a[0];
		double dz = b[2] - a[2];
		double separation = Math.max(1, Math.hypot(dx, dz));
		double ux = dx / separation;
		double uz = dz / separation;
		// Perpendicular to the line between them
		double px = -uz * this.shotSide;
		double pz = ux * this.shotSide;
		double progress = Math.min(1, this.shotTime / Math.max(0.1, this.shotDuration));
		this.rollTarget = 0;

		switch (this.shot) {
		case WIDE -> {
			double angle = this.shotSeed + this.shotTime * 0.12;
			double radius = Math.max(this.fieldWidth, this.fieldHeight) * 0.85;
			this.setShot(Math.cos(angle) * radius, -radius * 0.55, Math.sin(angle) * radius, mid[0] * 0.6, -10, mid[2] * 0.6, 40, 2.5);
		}
		case CHASE -> {
			// Over the shoulder of the subject, looking at the other
			this.setShot(a[0] - ux * 95 + px * 38, -42, a[2] - uz * 95 + pz * 38, b[0] * 0.65 + a[0] * 0.35, -18, b[2] * 0.65 + a[2] * 0.35, 52, 5);
		}
		case LOW_TRACK -> {
			// From the floor, sideways, sliding along: the hero shot
			double reach = separation * 0.55 + 150;
			double slide = (progress - 0.5) * 160;
			this.setShot(mid[0] + px * reach + ux * slide, -9, mid[2] + pz * reach + uz * slide, mid[0], -26, mid[2], 46, 3);
		}
		case CLOSE, IMPACT -> {
			// From the front, low and tilted
			double heading = subject == null ? 0 : Math.toRadians(subject.bodyRotate.getAngle());
			double hx = Math.sin(heading);
			double hz = Math.cos(heading);
			double distance = this.closeDistance * (1 - 0.12 * progress);
			double height = -22 - this.closeDistance * 0.08;
			this.setShot(a[0] + hx * distance + hz * distance * 0.4 * this.shotSide, height, a[2] + hz * distance - hx * distance * 0.4 * this.shotSide, a[0], -17, a[2], this.shot == Shot.IMPACT ? 36 : 32, 7);
			this.rollTarget = 7 * this.shotSide;
		}
		case CRANE -> {
			// From high above down to the fight, turning around it
			double eased = progress * progress * (3 - 2 * progress);
			double angle = this.shotSeed + eased * 0.9;
			double radius = 420 - 190 * eased;
			double height = -560 + 430 * eased;
			this.setShot(mid[0] + Math.cos(angle) * radius, height, mid[2] + Math.sin(angle) * radius, mid[0], -12, mid[2], 44, 3.5);
		}
		case FATAL -> this.fatalShot(a);
		}
		this.keepClear();
		// Never under the floor
		this.eyeTarget[1] = Math.min(this.eyeTarget[1], -5);
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
		boolean show = this.phase != Phase.LOADING && this.phase != Phase.REPLAY;
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

			// Turret and gun turn on their own
			this.turret.getTransforms().add(this.gunRotate);
			Cylinder dome = new Cylinder(10, 8, 8);
			dome.setMaterial(this.gunMaterial);
			dome.setTranslateY(-19);
			this.turret.getChildren().addAll(dome, box(4, 4, 24, 0, -20, 14, this.gunMaterial));

			this.radar.getTransforms().add(this.radarRotate);
			this.radar.getChildren().addAll(box(2, 8, 2, 0, -27, 0, radarMaterial), box(16, 5, 2, 0, -32, 2, radarMaterial));

			this.tank.getChildren().addAll(this.body, this.turret, this.radar);

			// Shown when it is destroyed
			this.tombstone = new Group();
			PhongMaterial stone = material(Color.web("#8a8fa3"));
			Cylinder top = new Cylinder(11, 6, 12);
			top.setMaterial(stone);
			top.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
			top.setTranslateY(-26);
			this.tombstone.getChildren().addAll(box(22, 26, 6, 0, -13, 0, stone), top, box(30, 4, 14, 0, -2, 0, stone), box(2, 12, 1, 0, -18, -3.5, playerMaterial),
					box(8, 2, 1, 0, -20, -3.5, playerMaterial));
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
