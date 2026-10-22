package widgets.scenes;

import static widgets.scenes.BattleArena3D.PLAYER_COLORS;
import static widgets.scenes.ArenaMaterials.argb;
import static widgets.scenes.ArenaMaterials.energy;
import static widgets.scenes.ArenaMaterials.material;
import static widgets.scenes.ArenaMaterials.paint;
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
import javafx.animation.PauseTransition;
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
	/** The hexagonal plates, which come off as the robot loses energy */
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

	/** The materials of the tank model: the dark parts, the plates, the steel, the cyan lights, the identity colour, the lamp and the warning. */
	private record Materials(PhongMaterial dark, PhongMaterial plate, PhongMaterial steel, PhongMaterial cyan, PhongMaterial identity, PhongMaterial lamp, PhongMaterial warning) {
	}

	/** Creates the model of a robot in the arena, in the colour of its place in the order, with its name above it. */
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
		// The same robot loses the same plates in every replay
		this.armor = new RobotArmor(this, Objects.hash(robot.getName(), order));
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
	private void buildBody(Materials materials) {
		// A modern tank of angular armor, pointing to +Z (north when the heading
		// is 0): a low wedge hull, tracks hidden by skirts of hexagonal plates, a
		// long flat turret and a very long gun. Flat facets, a few meshes.
		this.body.getTransforms().add(this.bodyRotate);
		LowPoly hull = new LowPoly();
		LowPoly tracks = new LowPoly();
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
			this.addTreadLinks(side < 0 ? 0 : 1, materials);
			// Hexagonal armor under the skirt, fixed to the hull
			for (double posZ = -15; posZ <= 15; posZ += 6) {
				this.armor.plate(this.body, materials.steel(), posZ, -3, 2.6, 2.6, Math.min(side * (BELT_OUTER + 0.2), side * (BELT_OUTER + 0.9)), Math.max(side * (BELT_OUTER + 0.2), side * (BELT_OUTER + 0.9)));
			}
			this.addSkirt(side, hull, materials.plate());
			// A strip of light in the color of the player, lamps and rear lights
			glowIdentity.box(side * 19.7, -11.2, -13, 0.7, 0.9, 9);
			glowLamp.box(side * 8.5, -10.4, 22.3, 3, 1.2, 1);
			glowWarning.box(side * 9, -10, -22.6, 3, 1.2, 1);
		}
		glowCyan.box(0, -9, 22.9, 14, 0.9, 0.6); // visor on the front
		for (double posX = -4; posX <= 4; posX += 4) {
			glowCyan.box(posX, -14.9, -19.5, 2, 0.5, 2.4); // vents on the engine deck
		}
		this.body.getChildren().addAll(hull.view(this.bodyMaterial), tracks.view(materials.dark()), glowCyan.view(materials.cyan()), glowIdentity.view(materials.identity()),
				glowWarning.view(materials.warning()), glowLamp.view(materials.lamp()));
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

	/** The skirt over one track: it leans out at the top, with two rows of hexagonal plates on it */
	private void addSkirt(int side, LowPoly hull, PhongMaterial plateMaterial) {
		hull.prismXY(new double[][] { { side * 13, -5 }, { side * 17.5, -5 }, { side * 19.5, -12.5 }, { side * 13, -12.5 } }, -19, 19);
		for (int row = 0; row < 2; row++) {
			double posY = row == 0 ? -10 : -7.2;
			double face = 17.5 + (-5 - posY) / 7.5 * 2;
			for (double posZ2 = row == 0 ? -15 : -12; posZ2 <= 16; posZ2 += 6) {
				this.armor.plate(this.body, plateMaterial, posZ2, posY, 2.9, 2.5,Math.min(side * (face - 0.2), side * (face + 0.5)), Math.max(side * (face - 0.2), side * (face + 0.5)));
			}
		}
	}

	/** The turret and the gun, which turn on their own: a long wedge, a flat wing over the gun, a remote weapon station and antennas */
	private void buildTurret(Materials materials) {
		// Turret and the gun turn on their own: a long wedge, a flat wing over the
		// gun, a remote weapon station and antennas
		this.turret.getTransforms().add(this.gunRotate);
		LowPoly shell = new LowPoly();
		LowPoly barrel = new LowPoly();
		LowPoly turretCyan = new LowPoly();
		LowPoly turretWarning = new LowPoly();
		shell.frustum(new double[][] { { -9, -12 }, { 9, -12 }, { 12, -3 }, { 10, 6 }, { 4, 12 }, { -4, 12 }, { -10, 6 }, { -12, -3 } }, -14.8, -19.5, 0.72);
		shell.prismZY(new double[][] { { -9, -19.5 }, { -9, -21 }, { 2, -21.4 }, { 11, -19.5 } }, -5, 5);
		shell.frustum(new double[][] { { -3.6, 6 }, { 3.6, 6 }, { 0.9, 36 }, { -0.9, 36 } }, -20.2, -21.2, 1); // wing over the gun
		shell.box(0, -17.4, 12, 7, 4.6, 5); // gun mantlet
		// The flanks, with plates like the skirts
		for (int side = -1; side <= 1; side += 2) {
			for (int index = 0; index < 3; index++) {
				double posZ = -4.5 + index * 4.5;
				double posY = index == 1 ? -16.2 : -17.9;
				double time = (posY + 14.8) / -4.7;
				double edge = posZ < -3 ? 9 + (posZ + 12) / 3 : 12 - (posZ + 3) * 2 / 9;
				double face = edge * (1 - 0.28 * time);
				this.armor.plate(this.turret, materials.plate(), posZ, posY, 2.3, 1.9,Math.min(side * (face - 0.4), side * (face + 0.45)), Math.max(side * (face - 0.4), side * (face + 0.45)));
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
		this.turret.getChildren().addAll(shell.view(this.gunMaterial), barrel.view(materials.dark()), turretCyan.view(materials.cyan()), turretWarning.view(materials.warning()));
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
		mast.cylinderY(3, 0, 0.25, -31, -27.8, 5);
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
	 * Below half of the energy the robot catches fire. The less energy, the more
	 * burning spots, the stronger the flames and the thicker the smoke.
	 *
	 * @param deltaTime
	 *            seconds, slower in the replay
	 */
	void burn(double deltaTime) {
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
		PauseTransition back = host.fx.effect(new PauseTransition(Duration.millis(110)));
		back.setOnFinished(event -> this.bodyMaterial.setDiffuseColor(this.bodyColor));
		back.play();
	}

	/**
	 * Blows up in its own colors: fireballs, shock waves seen as bubbles, smoke,
	 * debris, the hull, the turret and the radar thrown across the arena
	 */
	void destroy() {
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
	 * Back to a whole tank, for a new round or the replay
	 */
	void reset() {
		for (Animation animation : this.running) {
			animation.stop();
		}
		this.running.clear();
		this.armor.restore();
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
