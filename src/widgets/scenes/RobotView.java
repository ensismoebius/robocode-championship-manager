package widgets.scenes;

import static widgets.scenes.BattleArena3D.PLAYER_COLORS;
import static widgets.scenes.ArenaMaterials.argb;
import static widgets.scenes.ArenaMaterials.energy;
import static widgets.scenes.ArenaMaterials.material;
import static widgets.scenes.ArenaMaterials.paint;
import static widgets.scenes.ArenaMaterials.repaint;
import static widgets.scenes.ArenaHelpers.toCss;
import static widgets.scenes.RobotTreads.TREAD_LINKS;
import static widgets.scenes.RobotTreads.BELT;
import static widgets.scenes.RobotTreads.BELT_LENGTH;
import static widgets.scenes.RobotTreads.BELT_INNER;
import static widgets.scenes.RobotTreads.BELT_OUTER;
import static widgets.scenes.RobotTreads.BELT_CENTER;
import static widgets.scenes.RobotTreads.BELT_WIDTH;
import static widgets.scenes.RobotTreads.beltPoint;
import static widgets.scenes.RobotTreads.sidePoint;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
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
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import model.Opponent;
import utils.robocode.BattleRecording;

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

	final BattleArena3D host;

	final Group root = new Group();
	final Group tank = new Group();
	final Group body = new Group();
	final Group turret = new Group();
	/** The barrel with its fittings: it slides back along the turret when the gun fires */
	final Group gun = new Group();
	private final Translate recoilShift = new Translate();
	Timeline recoiling;
	final Group radar = new Group();
	final Rotate bodyRotate = new Rotate(0, Rotate.Y_AXIS);
	final Rotate gunRotate = new Rotate(0, Rotate.Y_AXIS);
	final Rotate radarRotate = new Rotate(0, Rotate.Y_AXIS);
	/** The whole tank tilts on the ground after a hit, around the axis the hit gives it */
	final Rotate tilt = new Rotate(0, Rotate.X_AXIS);
	Timeline rocking;
	final PhongMaterial bodyMaterial;
	final PhongMaterial gunMaterial;
	final PhongMaterial radarMaterial;
	final Opponent opponent;
	/** The colors the robot chose, which it can change during the battle */
	Color bodyColor;
	Color gunColor;
	Color radarColor;
	final Color playerColor;
	/** Blinking white after a hit: the body takes its color back when it ends */
	boolean flashing;
	final Group debris;
	final VBox balloon = new VBox(4);
	final Region energyFill = new Region();
	final Label nameLabel;
	final String displayName;
	final List<Animation> running = new ArrayList<Animation>();
	/** The armor plates and the parts it can fight without, which come off as the robot loses energy */
	final RobotArmor armor;
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

	/** The radar of a robot that did not choose its color */
	static final Color RADAR_COLOR = Color.web("#ffd400");

	/** The materials of the tank model: the dark parts, the plates, the steel, the cyan lights, the identity colour, the lamp and the warning. */
	record Materials(PhongMaterial dark, PhongMaterial plate, PhongMaterial steel, PhongMaterial cyan, PhongMaterial identity, PhongMaterial lamp, PhongMaterial warning) {
	}

	/** Creates the model of a robot in the arena, in the colour of its place in the order, with its name above it. */
	RobotView(BattleArena3D host, BattleRecording.Robot robot, Opponent opponent, int order) {
		this.host = host;
		this.opponent = opponent;
		this.playerColor = PLAYER_COLORS[order % PLAYER_COLORS.length];
		this.bodyColor = argb(robot.getBodyColor(), this.playerColor);
		this.gunColor = argb(robot.getGunColor(), this.bodyColor);
		this.radarColor = argb(robot.getRadarColor(), RADAR_COLOR);
		this.bodyMaterial = paint(this.bodyColor);
		this.gunMaterial = paint(this.gunColor);
		this.radarMaterial = paint(this.radarColor);
		PhongMaterial steel = material(Color.web("#6c7488"));
		steel.setSpecularPower(50);
		Materials materials = new Materials(material(Color.web("#171b26")), material(Color.web("#2d3345")), steel, this.glow(Color.web("#38e6ff")), this.glow(this.playerColor), this.glow(Color.web("#fff6c8")), this.glow(Color.web("#ff3b3b")));
		// The same robot loses the same plates in every replay
		this.armor = new RobotArmor(this, Objects.hash(robot.getName(), order));
		this.buildBody(materials);
		this.buildTurret(materials);
		this.buildRadar(materials, this.radarMaterial);
		this.tank.getChildren().addAll(this.body, this.turret, this.radar);
		this.tank.getTransforms().add(this.tilt);
		this.liftTracks();
		this.tilt.angleProperty().addListener((observable, oldAngle, newAngle) -> this.liftTracks());

		// The pieces thrown by the blast stay here, beside the wreck
		this.debris = new Group();
		this.root.getChildren().addAll(this.tank, this.debris);

		this.displayName = opponent == null ? robot.getName() : opponent.getName();
		this.nameLabel = new Label(this.displayName);
		this.buildBalloon(robot, opponent);
	}

	/** The hull, the tracks with their links, the armor and the lights: a modern tank of angular armor */
	private void buildBody(Materials materials) {
		// A modern tank of angular armor, pointing to +Z (north when the heading
		// is 0): a low wedge hull, tracks hidden by skirts of hexagonal plates, a
		// long flat turret and a very long gun. Flat facets, a few meshes.
		this.body.getTransforms().add(this.bodyRotate);
		LowPoly hull = new LowPoly();
		LowPoly tracks = new LowPoly();
		// The hull: a wedge, sloped in front and behind, with a deck over it
		hull.prismZY(new double[][] { { -21, -7 }, { -22.5, -9.5 }, { -20, -13 }, { 14, -13.5 }, { 22, -10 }, { 23, -7.5 } }, -13.5, 13.5);
		hull.frustum(new double[][] { { -11.5, -17 }, { 11.5, -17 }, { 11.5, 9 }, { -11.5, 9 } }, -13.4, -15.2, 0.9);
		for (int side = -1; side <= 1; side += 2) {
			// Track with chamfered ends, hidden above by the skirt
			tracks.prismZY(BELT, Math.min(side * BELT_INNER, side * BELT_OUTER), Math.max(side * BELT_INNER, side * BELT_OUTER));
			this.addTreadLinks(side < 0 ? 0 : 1, materials);
			// Hexagonal armor hanging under the skirt, outside the track
			List<Group> hanging = new ArrayList<Group>();
			for (double posZ = -15; posZ <= 15; posZ += 6) {
				hanging.add(this.armor.plate(this.body, materials.steel(), posZ, -3, 2.6, 2.6, Math.min(side * (BELT_OUTER + 0.2), side * (BELT_OUTER + 0.9)), Math.max(side * (BELT_OUTER + 0.2), side * (BELT_OUTER + 0.9))));
			}
			RobotFittings.addSkirt(this, side, materials, hanging);
		}
		RobotFittings.addFittings(this, materials);
		RobotFittings.addStowage(this, materials);
		this.body.getChildren().addAll(hull.view(this.bodyMaterial), tracks.view(materials.dark()));
	}

	/** The links of one belt, each one a short box turned to the belt where it is */
	private void addTreadLinks(int belt, Materials materials) {
		for (int link = 0; link < TREAD_LINKS; link++) {
			LowPoly piece = new LowPoly();
			piece.box(0, 0, 0, BELT_WIDTH, 1.2, 2.4);
			MeshView linkView = piece.view(materials.dark());
			Rotate turn = new Rotate(0, Rotate.X_AXIS);
			linkView.getTransforms().add(turn);
			this.body.getChildren().add(linkView);
			this.treadLinks[belt][link] = linkView;
			this.treadTurns[belt][link] = turn;
		}
	}

	/** The turret and the gun, which turn on their own: a long wedge, a flat wing over the gun, a remote weapon station and antennas */
	private void buildTurret(Materials materials) {
		// Turret and the gun turn on their own: a long wedge, a flat wing over the
		// gun, a remote weapon station and antennas
		this.turret.getTransforms().add(this.gunRotate);
		LowPoly shell = new LowPoly();
		LowPoly barrel = new LowPoly();
		shell.frustum(new double[][] { { -9, -12 }, { 9, -12 }, { 12, -3 }, { 10, 6 }, { 4, 12 }, { -4, 12 }, { -10, 6 }, { -12, -3 } }, -14.8, -19.5, 0.72);
		shell.prismZY(new double[][] { { -9, -19.5 }, { -9, -21 }, { 2, -21.4 }, { 11, -19.5 } }, -5, 5);
		shell.frustum(new double[][] { { -3.6, 6 }, { 3.6, 6 }, { 0.9, 36 }, { -0.9, 36 } }, -20.2, -21.2, 1); // wing over the gun
		shell.box(0, -17.4, 12, 7, 4.6, 5); // gun mantlet
		RobotFittings.addTurretSides(this, materials);
		// The gun: a very long barrel, with a fume extractor and a muzzle ring, which stay on it
		barrel.cylinderZ(0, -17.2, 1.35, 12, 43, 6);
		RobotFittings.addGunGear(this, materials);
		RobotFittings.addRoofGear(this, materials);
		this.gun.getTransforms().add(this.recoilShift);
		this.gun.getChildren().add(barrel.view(materials.dark()));
		this.turret.getChildren().addAll(shell.view(this.gunMaterial), this.gun);
	}

	/** The radar: a flat panel on a mast, turning over the roof */
	private void buildRadar(Materials materials, PhongMaterial radarMaterial) {
		// The radar: a flat panel on a mast, turning over the roof
		this.radar.getTransforms().add(this.radarRotate);
		LowPoly panel = new LowPoly();
		LowPoly panelCyan = new LowPoly();
		LowPoly mast = new LowPoly();
		mast.cylinderY(0, 0, 0.8, -24, -21, 6);
		panel.box(0, -26, 0, 8, 3.6, 1.2);
		panelCyan.box(0, -26, 0.7, 6, 0.6, 0.3);
		// The thin antenna on the panel can come off
		LowPoly whip = new LowPoly();
		whip.cylinderY(3, 0, 0.25, -31, -27.8, 5);
		this.armor.piece(this.radar, whip.view(materials.dark()));
		this.radar.getChildren().addAll(panel.view(radarMaterial), mast.view(materials.dark()), panelCyan.view(materials.cyan()));
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

	/** The position of the robot on the arena floor, as x, zero and z. */
	double[] position() {
		return new double[] { this.root.getTranslateX(), 0, this.root.getTranslateZ() };
	}

	/** Places the robot at the given position, turning its body, its turret and its radar to the given angles. */
	void place(double posX, double posZ, double body, double gun, double radar) {
		this.root.setTranslateX(posX);
		this.root.setTranslateZ(posZ);
		this.bodyRotate.setAngle(Math.toDegrees(body));
		if (!this.dead) {
			this.gunRotate.setAngle(Math.toDegrees(gun));
			this.radarRotate.setAngle(Math.toDegrees(radar));
		}
	}

	/** Sets the energy: the bar over the robot, and the armor, which comes off as the energy goes. */
	void setEnergy(double energy) {
		this.energy = energy;
		this.energyFill.setMaxWidth(90 * Math.max(0, Math.min(1, energy / 100)));
		this.armor.wear(energy);
	}

	/**
	 * Paints the robot in the colors it has in this turn (ARGB, 0 for the default
	 * one): a robot can change them at any time. A destroyed robot keeps the
	 * colors it had, and a blinking one takes the new color of the body when the
	 * blink ends.
	 */
	void setColors(int body, int gun, int radar) {
		if (this.dead) {
			return;
		}
		Color newBody = argb(body, this.playerColor);
		Color newGun = argb(gun, newBody);
		Color newRadar = argb(radar, RADAR_COLOR);
		if (!newBody.equals(this.bodyColor)) {
			this.bodyColor = newBody;
			repaint(this.bodyMaterial, newBody);
			if (this.flashing) {
				this.bodyMaterial.setDiffuseColor(Color.WHITE);
			}
		}
		if (!newGun.equals(this.gunColor)) {
			this.gunColor = newGun;
			repaint(this.gunMaterial, newGun);
		}
		if (!newRadar.equals(this.radarColor)) {
			this.radarColor = newRadar;
			repaint(this.radarMaterial, newRadar);
		}
	}

	/**
	 * Below half of the energy the robot catches fire. The less energy, the more
	 * burning spots, the stronger the flames and the thicker the smoke.
	 *
	 * @param deltaTime
	 *            seconds, slower in the replay
	 */
	void burn(double deltaTime) {
		this.armor.update(deltaTime);
		RobotFire.burn(this, deltaTime);
	}

	/**
	 * The links go round the belts, which move with the ground that each side of
	 * the hull covered since the last frame, so a robot turning in place runs its
	 * two belts the opposite way. Dead robots stop.
	 */
	void roll(double deltaTime) {
		// The turret against the hull: a robot turning in place with its tracks carries the
		// turret along, but that is not the turret turning
		double gun = this.gunRotate.getAngle() - this.bodyRotate.getAngle();
		this.treadSpeed = 0;
		this.turretSpeed = 0;
		double posX = this.root.getTranslateX();
		double posZ = this.root.getTranslateZ();
		double heading = Math.toRadians(this.bodyRotate.getAngle());
		this.shiftTreads(deltaTime, posX, posZ, heading);
		// The turret: how far it turned on the hull since the last frame, round the short way
		if (!Double.isNaN(this.lastGun) && !this.dead && deltaTime > 0) {
			double turn = Math.IEEEremainder(gun - this.lastGun, 360);
			if (Math.abs(turn) < 90) {
				this.turretSpeed = Math.abs(turn) / deltaTime;
			}
		}
		this.lastGun = gun;
		this.rollX = posX;
		this.rollZ = posZ;
		this.rollHeading = heading;
		this.placeLinks();
	}

	/** Moves each belt with the ground that its side of the hull went over since the last frame */
	private void shiftTreads(double deltaTime, double posX, double posZ, double heading) {
		// A jump (new round, replay) is not a movement
		if (!Double.isNaN(this.rollX) && !this.dead && Math.hypot(posX - this.rollX, posZ - this.rollZ) < 60) {
			double fastest = 0;
			for (int side = 0; side < 2; side++) {
				double[] from = sidePoint(this.rollX, this.rollZ, this.rollHeading, side);
				double[] toValue = sidePoint(posX, posZ, heading, side);
				// How far that side went forward, along the hull
				double forward = (toValue[0] - from[0]) * Math.sin(heading) + (toValue[1] - from[1]) * Math.cos(heading);
				this.treadShift[side] = (this.treadShift[side] + forward) % BELT_LENGTH;
				fastest = Math.max(fastest, Math.abs(forward));
			}
			if (deltaTime > 0) {
				this.treadSpeed = fastest / deltaTime;
			}
		}
	}

	/** Puts every link of both belts where its point of the belt is, turned to follow the belt */
	private void placeLinks() {
		for (int side = 0; side < 2; side++) {
			for (int link = 0; link < TREAD_LINKS; link++) {
				double text = link * BELT_LENGTH / TREAD_LINKS + this.treadShift[side];
				// Round the belt: a link that leaves one end comes back at the other
				text -= BELT_LENGTH * Math.floor(text / BELT_LENGTH);
				double[] point = beltPoint(text);
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
		this.flashing = true;
		PauseTransition back = host.fx.effect(new PauseTransition(Duration.millis(110)));
		back.setOnFinished(event -> {
			this.flashing = false;
			this.bodyMaterial.setDiffuseColor(this.bodyColor);
		});
		back.play();
	}

	/**
	 * Rocks with the hit: the tank tilts away from where the bullet struck, the
	 * more the stronger the bullet, and sways back to rest in a moment. A new hit
	 * starts the rocking again.
	 *
	 * @param hitX
	 *            where the bullet struck, in the arena
	 * @param power
	 *            of the bullet, 0.1 to 3
	 */
	void rock(double hitX, double hitZ, double power) {
		if (this.dead) {
			return;
		}
		double towardX = hitX - this.root.getTranslateX();
		double towardZ = hitZ - this.root.getTranslateZ();
		double length = Math.hypot(towardX, towardZ);
		if (length < 1e-6) {
			towardX = 0;
			towardZ = 1;
			length = 1;
		}
		this.settle();
		// Turning around this axis, the top of the tank goes away from the hit
		this.tilt.setAxis(new Point3D(towardZ / length, 0, -towardX / length));
		this.liftTracks();
		// A hard hit throws the tank much further over, and it swings back and forth longer
		double peak = Math.min(18, 6 + power * 4);
		this.rocking = new Timeline(new KeyFrame(Duration.millis(60), new KeyValue(this.tilt.angleProperty(), peak, Interpolator.EASE_OUT)),
				new KeyFrame(Duration.millis(180), new KeyValue(this.tilt.angleProperty(), -peak * 0.6, Interpolator.EASE_BOTH)),
				new KeyFrame(Duration.millis(300), new KeyValue(this.tilt.angleProperty(), peak * 0.35, Interpolator.EASE_BOTH)),
				new KeyFrame(Duration.millis(400), new KeyValue(this.tilt.angleProperty(), -peak * 0.12, Interpolator.EASE_BOTH)),
				new KeyFrame(Duration.millis(520), new KeyValue(this.tilt.angleProperty(), 0, Interpolator.EASE_BOTH)));
		host.fx.effect(this.rocking).play();
	}

	/**
	 * The shot pushes the barrel back along the turret, the more the stronger the bullet, and a spring brings it
	 * forward again. A new shot starts the recoil again.
	 *
	 * @param power
	 *            of the bullet, 0.1 to 3
	 */
	void recoil(double power) {
		if (this.dead) {
			return;
		}
		if (this.recoiling != null) {
			this.recoiling.stop();
		}
		double back = -(2.5 + power * 2);
		this.recoiling = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(this.recoilShift.zProperty(), 0)),
				new KeyFrame(Duration.millis(45), new KeyValue(this.recoilShift.zProperty(), back, Interpolator.EASE_OUT)),
				new KeyFrame(Duration.millis(380), new KeyValue(this.recoilShift.zProperty(), 0, Interpolator.EASE_BOTH)));
		host.fx.effect(this.recoiling).play();
	}

	/** Stops the rocking, the tank standing straight */
	void settle() {
		if (this.rocking != null) {
			this.rocking.stop();
			this.rocking = null;
		}
		this.tilt.setAngle(0);
	}

	/**
	 * The tilt turns the belts into the floor on the side that goes down, so the tank is raised by as much as
	 * its lowest belt point goes below the place it has standing still. Standing still, the tracks rest on the floor at y 0:
	 * the tank is lifted by the drop of its lowest track link, the chamfered ends of the belts.
	 */
	private void liftTracks() {
		double rest = lowestBelt(0, this.tilt.getAxis());
		double lowest = lowestBelt(Math.toRadians(this.tilt.getAngle()), this.tilt.getAxis());
		// The y axis points down, so a belt in the floor has a larger y than at rest
		this.tank.setTranslateY(-TRACKS_DROP + Math.min(0, rest - lowest));
	}

	/** The lowest point of the track links stands this far under the belts, where the floor is */
	static final double TRACKS_DROP = 0.6;

	/** The largest y of the belts, the lowest point of them, after turning them round the axis */
	private static double lowestBelt(double angle, Point3D axis) {
		double lowest = Double.NEGATIVE_INFINITY;
		for (double[] point : RobotTreads.BELT) {
			for (double across : new double[] { -RobotTreads.BELT_OUTER, -RobotTreads.BELT_INNER, RobotTreads.BELT_INNER, RobotTreads.BELT_OUTER }) {
				Point3D turned = turn(new Point3D(across, point[1], point[0]), axis, angle);
				lowest = Math.max(lowest, turned.getY());
			}
		}
		return lowest;
	}

	/** Turns a point round an axis through the origin, by the angle in radians (Rodrigues' rotation formula). */
	private static Point3D turn(Point3D point, Point3D axis, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		Point3D cross = axis.crossProduct(point);
		double along = axis.dotProduct(point) * (1 - cos);
		return point.multiply(cos).add(cross.multiply(sin)).add(axis.multiply(along));
	}

	/**
	 * Blows up in its own colors: fireballs, shock waves seen as bubbles, smoke,
	 * debris, the hull, the turret and the radar thrown across the arena
	 */
	void destroy() {
		this.settle();
		RobotWreck.destroy(this);
	}

	/**
	 * A material that shines in the color by itself
	 */
	PhongMaterial glow(Color color) {
		WritableImage map = new WritableImage(2, 2);
		for (int posY = 0; posY < 2; posY++) {
			for (int posX = 0; posX < 2; posX++) {
				map.getPixelWriter().setColor(posX, posY, color);
			}
		}
		PhongMaterial material = new PhongMaterial(color.darker().darker());
		material.setSelfIlluminationMap(map);
		this.glows.add(material);
		this.glowMaps.add(map);
		return material;
	}

	/** Turns the glow of the robot on or off. */
	void setGlowing(boolean glowing) {
		for (int index = 0; index < this.glows.size(); index++) {
			this.glows.get(index).setSelfIlluminationMap(glowing ? this.glowMaps.get(index) : null);
		}
	}

	/**
	 * Back to a whole tank, for a new round
	 */
	void reset() {
		if (this.recoiling != null) {
			this.recoiling.stop();
		}
		this.recoilShift.setZ(0);
		this.standAgain();
		this.armor.restore();
	}

	/**
	 * Back to the moment the replay starts: standing again, without the pieces
	 * it had lost by then, which stay on the floor where they fell
	 *
	 * @param lowest
	 *            the lowest energy of the robot up to that moment
	 */
	void rewind(double lowest) {
		this.standAgain();
		this.armor.rewind(lowest);
	}

	/** Standing again, alive, in its colors, with the parts the blast threw back in place */
	private void standAgain() {
		for (Animation animation : this.running) {
			animation.stop();
		}
		this.running.clear();
		this.dead = false;
		this.flashing = false;
		this.settle();
		this.tank.setVisible(true);
		this.debris.getChildren().clear();
		this.bodyMaterial.setDiffuseColor(this.bodyColor);
		this.gunMaterial.setDiffuseColor(this.gunColor);
		this.setGlowing(true);
		for (Node part : List.of(this.turret, this.radar, this.body)) {
			// The hull and the turret of a wreck were taken as pieces of the floor
			part.setVisible(true);
			part.setTranslateX(0);
			part.setTranslateY(0);
			part.setTranslateZ(0);
			part.setRotate(0);
		}
		this.nameLabel.setText(this.displayName);
		this.balloon.setOpacity(1);
	}
}
