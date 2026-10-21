package widgets.scenes;

import static widgets.scenes.BattleArena3D.BURN_ENERGY;
import static widgets.scenes.BattleArena3D.BURN_SPOTS;
import static widgets.scenes.ArenaMaterials.CRYSTAL;
import static widgets.scenes.BattleArena3D.PLAYER_COLORS;
import static widgets.scenes.ArenaMaterials.argb;
import static widgets.scenes.ArenaMaterials.energy;
import static widgets.scenes.ArenaMaterials.grow;
import static widgets.scenes.ArenaMaterials.material;
import static widgets.scenes.ArenaMaterials.paint;
import static widgets.scenes.ArenaMaterials.shape;
import static widgets.scenes.ArenaMaterials.shrink;
import static widgets.scenes.BattleArena3D.toCss;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import model.Opponent;
import utils.robocode.BattleRecording;
import widgets.scenes.ArenaFire.Particle;
import widgets.scenes.ArenaFire.ParticleKind;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * A low poly tank: treads, body, turret with the gun and the radar, each part
 * with the color the robot chose. It lives in the {@link BattleArena3D}, which
 * it reads for the fire, the effects and the shared random.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class RobotView {

	private final BattleArena3D host;

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

	private record Materials(PhongMaterial dark, PhongMaterial plate, PhongMaterial steel, PhongMaterial cyan, PhongMaterial identity, PhongMaterial lamp, PhongMaterial warning) {
	}

	RobotView(BattleArena3D host, BattleRecording.Robot robot, Opponent opponent, int order) {
		this.host = host;
		this.opponent = opponent;
		this.playerColor = PLAYER_COLORS[order % PLAYER_COLORS.length];
		this.bodyColor = argb(robot.getBodyColor(), this.playerColor);
		this.gunColor = argb(robot.getGunColor(), this.bodyColor);
		this.bodyMaterial = paint(this.bodyColor);
		this.gunMaterial = paint(this.gunColor);
		PhongMaterial radarMaterial = paint(argb(robot.getRadarColor(), Color.web("#ffd400")));
		PhongMaterial steel = material(Color.web("#6c7488"));
		steel.setSpecularPower(50);
		Materials materials = new Materials(material(Color.web("#171b26")), material(Color.web("#2d3345")), steel, this.glow(Color.web("#38e6ff")), this.glow(this.playerColor), this.glow(Color.web("#fff6c8")), this.glow(Color.web("#ff3b3b")));
		this.buildBody(materials);
		this.buildTurret(materials);
		this.buildRadar(materials, radarMaterial);
		this.tank.getChildren().addAll(this.body, this.turret, this.radar);

		// The pieces thrown by the blast stay here, beside the wreck
		this.debris = new Group();
		this.root.getChildren().addAll(this.tank, this.debris);

		this.displayName = opponent == null ? robot.getName() : opponent.getName();
		this.nameLabel = new Label(this.displayName);
		this.buildBalloon(robot, opponent);
	}

	/** The hull, the tracks with their links, the armor and the lights: a modern tank of angular armor */
	private void buildBody(Materials m) {
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
		tracks.box(0, -14.5, -17.5, 14, 2.2, 6); // engine deck, m.dark()
		for (int side = -1; side <= 1; side += 2) {
			// Track with chamfered ends, hidden above by the skirt
			tracks.prismZY(BELT, Math.min(side * BELT_INNER, side * BELT_OUTER), Math.max(side * BELT_INNER, side * BELT_OUTER));
			int index = side < 0 ? 0 : 1;
			// The links of the belt, each one turned to the belt where it is
			for (int link = 0; link < TREAD_LINKS; link++) {
				LowPoly piece = new LowPoly();
				piece.box(0, 0, 0, BELT_WIDTH, 1.2, 2.4);
				MeshView linkView = piece.view(m.dark());
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
		this.body.getChildren().addAll(hull.view(this.bodyMaterial), tracks.view(m.dark()), armor.view(m.steel()), plates.view(m.plate()), glowCyan.view(m.cyan()), glowIdentity.view(m.identity()),
				glowWarning.view(m.warning()), glowLamp.view(m.lamp()));
	}

	/** The turret and the gun, which turn on their own: a long wedge, a flat wing over the gun, a remote weapon station and antennas */
	private void buildTurret(Materials m) {
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
		this.turret.getChildren().addAll(shell.view(this.gunMaterial), barrel.view(m.dark()), turretPlates.view(m.plate()), turretCyan.view(m.cyan()), turretWarning.view(m.warning()));
	}

	/** The radar: a flat panel on a mast, turning over the roof */
	private void buildRadar(Materials m, PhongMaterial radarMaterial) {
		// The radar: a flat panel on a mast, turning over the roof
		this.radar.getTransforms().add(this.radarRotate);
		LowPoly panel = new LowPoly();
		LowPoly panelCyan = new LowPoly();
		LowPoly mast = new LowPoly();
		mast.cylinderY(0, 0, 0.8, -24, -21, 6);
		panel.box(0, -26, 0, 8, 3.6, 1.2);
		panelCyan.box(0, -26, 0.7, 6, 0.6, 0.3);
		mast.cylinderY(3, 0, 0.25, -31, -27.8, 5);
		this.radar.getChildren().addAll(panel.view(radarMaterial), mast.view(m.dark()), panelCyan.view(m.cyan()));
	}

	/** Balloon over the robot: its name and its energy */
	private void buildBalloon(BattleRecording.Robot robot, Opponent opponent) {
		// Balloon over the robot
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

		Random random = host.random;
		int spots = damage < 0.3 ? 1 : damage < 0.6 ? 2 : 3;
		double heading = Math.toRadians(this.bodyRotate.getAngle());
		double cos = Math.cos(heading);
		double sin = Math.sin(heading);

		this.flameDebt += dt * (40 + 70 * damage);
		this.smokeDebt += dt * (5 + 16 * damage);
		this.sparkDebt += dt * (1.5 + 7 * damage);
		while (this.flameDebt >= 1 || this.smokeDebt >= 1 || this.sparkDebt >= 1) {
			// A burning spot of the hull, turning with the body
			double[] spot = BURN_SPOTS[host.random.nextInt(spots)];
			double lx = spot[0] + (host.random.nextDouble() - 0.5) * 6;
			double lz = spot[2] + (host.random.nextDouble() - 0.5) * 6;
			double wx = x + lx * cos + lz * sin;
			double wz = z - lx * sin + lz * cos;
			if (this.flameDebt >= 1) {
				this.flameDebt--;
				// Anchored to the robot, its position is relative to it
				Particle flame = host.fire.spawn(ParticleKind.FLAME, wx - x, spot[1], wz - z, damage);
				flame.anchor = this;
			} else if (this.smokeDebt >= 1) {
				this.smokeDebt--;
				host.fire.spawn(ParticleKind.SMOKE, wx, spot[1] - 6, wz, damage).inherit(this.speedX * 0.15, this.speedZ * 0.15);
			} else {
				this.sparkDebt--;
				host.fire.spawn(ParticleKind.SPARK, wx, spot[1], wz, damage).inherit(this.speedX * 0.3, this.speedZ * 0.3);
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
		PauseTransition back = host.fx.effect(new PauseTransition(Duration.millis(110)));
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
		Color tint = this.playerColor;
		this.burst(blast, all, tint);
		this.smoke(x, z);
		this.throwDebris(all);
		this.throwTurretAndRadar(all);
		this.throwHull(all);

		host.effects.getChildren().add(blast);
		host.fx.effect(all).setOnFinished(event -> host.effects.getChildren().remove(blast));
		this.running.add(all);
		all.play();
		host.fx.lightFlash(x, z, tint, 1.4);

		this.nameLabel.setText("☠ " + this.displayName);
		this.balloon.setOpacity(0.6);
	}

	/** The flash of the blast, a bolt from the sky, the shock waves, the discharges of lightning and the sparks */
	private void burst(Group blast, ParallelTransition all, Color tint) {
		Color hot = tint.interpolate(Color.WHITE, 0.85);
		MeshView core = shape(CRYSTAL, energy(Color.WHITE, 0.85));
		core.setTranslateY(-20);
		blast.getChildren().add(core);
		all.getChildren().addAll(grow(core, 2, 9, 140), shrink(core, 140, 180));
		double[] center = { 0, -20, 0 };
		// The bolt from the sky strikes first
		LowPoly strikeGlow = new LowPoly();
		LowPoly strikeCore = new LowPoly();
		host.fx.bolt(strikeGlow, strikeCore, new double[] { 0, -420, 0 }, center, 0.6, 0);
		Group strike = new Group(shape(strikeGlow.mesh, energy(tint.interpolate(Color.web("#8fe9ff"), 0.5), 0.45)), shape(strikeCore.mesh, energy(Color.WHITE, 1)));
		blast.getChildren().add(strike);
		all.getChildren().add(new Timeline(new KeyFrame(Duration.millis(120), event -> strike.setVisible(false))));
		// The shock wave: rings spreading on the floor from the wreck
		Color air = tint.interpolate(Color.WHITE, 0.7);
		host.fx.shockwave(blast, all, -1, 300, 0, 900, air);
		host.fx.shockwave(blast, all, -1, 210, 160, 750, Color.web("#8fe9ff"));
		host.fx.shockwave(blast, all, -1, 130, 0, 500, Color.WHITE);
		host.fx.discharge(blast, all, center, -1, 9, 110, 0.4, tint, 60);
		host.fx.discharge(blast, all, center, -1, 7, 90, 0.35, tint, 400);
		host.fx.discharge(blast, all, center, -1, 6, 120, 0.38, tint, 800);
		host.fx.sparks(blast, all, center, -1, 40, 120, 1.1, Color.web("#fff3b0"), 1100);
		host.fx.sparks(blast, all, center, -1, 18, 80, 0.9, hot, 1500);
	}

	/** The smoke rising from the wreck: particles, like the fire of the burning robots, thick and dark since it is destroyed */
	private void smoke(double x, double z) {
		// The smoke rising from the wreck: particles, like the fire of the burning robots,
		// thick and dark since it is destroyed
		Timeline smoke = new Timeline();
		for (int i = 0; i < 40; i++) {
			smoke.getKeyFrames().add(new KeyFrame(Duration.millis(i * 90), event -> {
				for (int puff = 0; puff < 2; puff++) {
					host.fire.spawn(ParticleKind.SMOKE, x + (host.random.nextDouble() - 0.5) * 40, -20, z + (host.random.nextDouble() - 0.5) * 40, 0.8);
				}
			}));
		}
		this.running.add(smoke);
		smoke.play();
	}

	/** Debris in the colors of the robot, thrown across the arena */
	private void throwDebris(ParallelTransition all) {
		PhongMaterial[] colors = { this.bodyMaterial, this.gunMaterial, new PhongMaterial(Color.web("#22252e")), new PhongMaterial(Color.web("#ffb000")) };
		for (int i = 0; i < 26; i++) {
			double angle = Math.PI * 2 * i / 26 + host.random.nextDouble() * 0.3;
			double size = 3 + host.random.nextDouble() * 5;
			Box piece = new Box(size, size, size);
			piece.setMaterial(colors[i % colors.length]);
			piece.setTranslateY(-15);
			this.debris.getChildren().add(piece);
			double reach = 120 + host.random.nextDouble() * 180;
			all.getChildren().add(host.fx.fly(piece, Math.cos(angle) * reach, Math.sin(angle) * reach, 70 + host.random.nextDouble() * 120, 1500));
		}
	}

	/** The turret and the radar are thrown in the air */
	private void throwTurretAndRadar(ParallelTransition all) {
		// The turret and the radar are thrown in the air
		double throwAngle = host.random.nextDouble() * Math.PI * 2;
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
	}

	/** The hull is thrown too: it flies over the arena, turns over and lands tilted */
	private void throwHull(ParallelTransition all) {
		// The hull is thrown too: it flies over the arena, turns over and lands tilted
		double hullAngle = host.random.nextDouble() * Math.PI * 2;
		double hullReach = 130 + host.random.nextDouble() * 80;
		double hullX = Math.cos(hullAngle) * hullReach;
		double hullZ = Math.sin(hullAngle) * hullReach;
		this.body.setRotationAxis(new Point3D(-Math.sin(hullAngle), 0, Math.cos(hullAngle)));
		all.getChildren().add(new Timeline( //
				new KeyFrame(Duration.millis(450), new KeyValue(this.body.translateYProperty(), -80, Interpolator.EASE_OUT), new KeyValue(this.body.translateXProperty(), hullX * 0.5, Interpolator.EASE_OUT),
						new KeyValue(this.body.translateZProperty(), hullZ * 0.5, Interpolator.EASE_OUT), new KeyValue(this.body.rotateProperty(), 60, Interpolator.LINEAR)), //
				new KeyFrame(Duration.millis(1300), new KeyValue(this.body.translateYProperty(), 0, Interpolator.EASE_IN), new KeyValue(this.body.translateXProperty(), hullX, Interpolator.EASE_OUT),
						new KeyValue(this.body.translateZProperty(), hullZ, Interpolator.EASE_OUT), new KeyValue(this.body.rotateProperty(), 35, Interpolator.EASE_OUT))));
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
