package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Group;
import widgets.scenes.RobotView.Materials;

/**
 * The parts of a robot it can fight without: the plates, the lights, the
 * stowage and the gear on the turret. Each one is a piece of its armor, which
 * comes off as the robot loses energy. The gear on the gun stays on it.
 */
final class RobotFittings {

	/** Only builds the parts of a robot, it has no instances. */
	private RobotFittings() {
	}

	/** The lamps, the rear lights, the visor on the front and the vents on the engine deck: all of them can come off */
	static void addFittings(RobotView view, Materials materials) {
		for (int side = -1; side <= 1; side += 2) {
			LowPoly lamp = new LowPoly();
			lamp.box(side * 8.5, -10.4, 22.3, 3, 1.2, 1);
			view.armor.piece(view.body, lamp.view(materials.lamp()));
			LowPoly rearLight = new LowPoly();
			rearLight.box(side * 9, -10, -22.6, 3, 1.2, 1);
			view.armor.piece(view.body, rearLight.view(materials.warning()));
		}
		LowPoly visor = new LowPoly();
		visor.box(0, -9, 22.9, 14, 0.9, 0.6);
		view.armor.piece(view.body, visor.view(materials.cyan()));
		// The engine deck comes off only after the vents on it
		List<Group> vents = new ArrayList<Group>();
		for (double posX = -4; posX <= 4; posX += 4) {
			LowPoly vent = new LowPoly();
			vent.box(posX, -14.9, -19.5, 2, 0.5, 2.4);
			vents.add(view.armor.piece(view.body, vent.view(materials.cyan())));
		}
		LowPoly engineDeck = new LowPoly();
		engineDeck.box(0, -14.5, -17.5, 14, 2.2, 6);
		view.armor.holder(view.body, vents, engineDeck.view(materials.dark()));
	}

	/** Spare track links on the front slope, tow hooks under the visor and jerry cans on the back: all of them can come off */
	static void addStowage(RobotView view, Materials materials) {
		for (double posZ = 15.5; posZ <= 18; posZ += 2.5) {
			// On the slope of the front, which goes down from the deck to the nose
			double surface = -13.5 + (posZ - 14) * 3.5 / 8;
			for (double posX = -8; posX <= 8; posX += 4) {
				LowPoly link = new LowPoly();
				link.box(posX, surface - 0.2, posZ, 3, 1.2, 2);
				view.armor.piece(view.body, link.view(materials.steel()));
			}
		}
		for (int side = -1; side <= 1; side += 2) {
			LowPoly hook = new LowPoly();
			hook.box(side * 5, -7.8, 23.3, 1.6, 1.2, 1.4);
			view.armor.piece(view.body, hook.view(materials.dark()));
			LowPoly can = new LowPoly();
			can.box(side * 4.5, -8.8, -22.6, 3.5, 3, 2.4);
			view.armor.piece(view.body, can.view(materials.plate()));
		}
	}

	/**
	 * The skirt over one track: it leans out at the top, with two rows of
	 * hexagonal plates on it and the hanging plates under it. It comes off too,
	 * but only after the last of them, so no plate is ever left in the air.
	 */
	static void addSkirt(RobotView view, int side, Materials materials, List<Group> hanging) {
		List<Group> plates = new ArrayList<Group>(hanging);
		for (int row = 0; row < 2; row++) {
			double posY = row == 0 ? -10 : -7.2;
			double face = 17.5 + (-5 - posY) / 7.5 * 2;
			for (double posZ2 = row == 0 ? -15 : -12; posZ2 <= 16; posZ2 += 6) {
				plates.add(view.armor.plate(view.body, materials.plate(), posZ2, posY, 2.9, 2.5, Math.min(side * (face - 0.2), side * (face + 0.5)), Math.max(side * (face - 0.2), side * (face + 0.5))));
			}
		}
		LowPoly skirt = new LowPoly();
		skirt.prismXY(new double[][] { { side * 13, -5 }, { side * 17.5, -5 }, { side * 19.5, -12.5 }, { side * 13, -12.5 } }, -19, 19);
		// The strip of light in the color of the player runs along the top of the skirt, and goes with it
		LowPoly strip = new LowPoly();
		strip.box(side * 19.7, -11.2, -13, 0.7, 0.9, 9);
		view.armor.holder(view.body, plates, skirt.view(view.bodyMaterial), strip.view(materials.identity()));
	}

	/** The plates on the flanks of the turret, like the skirts, the sensors on its cheeks and the antennas, with lights on top */
	static void addTurretSides(RobotView view, Materials materials) {
		for (int side = -1; side <= 1; side += 2) {
			for (int index = 0; index < 3; index++) {
				double posZ = -4.5 + index * 4.5;
				double posY = index == 1 ? -16.2 : -17.9;
				double time = (posY + 14.8) / -4.7;
				double edge = posZ < -3 ? 9 + (posZ + 12) / 3 : 12 - (posZ + 3) * 2 / 9;
				double face = edge * (1 - 0.28 * time);
				view.armor.plate(view.turret, materials.plate(), posZ, posY, 2.3, 1.9, Math.min(side * (face - 0.4), side * (face + 0.45)), Math.max(side * (face - 0.4), side * (face + 0.45)));
			}
			LowPoly sensor = new LowPoly();
			sensor.box(side * 5.2, -17.6, 11.5, 1.8, 1.2, 1);
			view.armor.piece(view.turret, sensor.view(materials.cyan()));
			LowPoly antenna = new LowPoly();
			antenna.cylinderY(side * 7, -9, 0.3, -19, -26, 5);
			LowPoly antennaLight = new LowPoly();
			antennaLight.cylinderY(side * 7, -9, 0.9, -26, -26.9, 5);
			view.armor.piece(view.turret, antenna.view(materials.dark()), antennaLight.view(materials.warning()));
		}
	}

	/** The fume extractor and the muzzle ring of the gun, with its light: they stay on the gun, they never come off, and they recoil with it */
	static void addGunGear(RobotView view, Materials materials) {
		LowPoly extractor = new LowPoly();
		extractor.cylinderZ(0, -17.2, 1.95, 21, 27, 6);
		LowPoly muzzle = new LowPoly();
		muzzle.cylinderZ(0, -17.2, 1.8, 40, 43.6, 6);
		LowPoly turretCyan = new LowPoly();
		turretCyan.cylinderZ(0, -17.2, 2, 43.6, 44.1, 6);
		view.gun.getChildren().addAll(extractor.view(materials.dark()), muzzle.view(materials.dark()), turretCyan.view(materials.cyan()));
	}

	/** The remote weapon station on the roof of the turret and the commander sight, which can come off */
	static void addRoofGear(RobotView view, Materials materials) {
		LowPoly station = new LowPoly();
		station.box(-4.6, -22.4, -4, 4.4, 2.2, 4.4);
		LowPoly stationGun = new LowPoly();
		stationGun.cylinderZ(-4.6, -22.6, 0.5, -1.5, 3.5, 5);
		LowPoly stationLens = new LowPoly();
		stationLens.box(-4.6, -23.2, -1.6, 1.4, 0.9, 0.6);
		view.armor.piece(view.turret, station.view(view.gunMaterial), stationGun.view(materials.dark()), stationLens.view(materials.cyan()));
		LowPoly sight = new LowPoly();
		sight.frustum(new double[][] { { 3, -4 }, { 6, -4 }, { 6, -1 }, { 3, -1 } }, -21, -22.6, 0.7);
		LowPoly sightLens = new LowPoly();
		sightLens.box(4.5, -22, -0.8, 1.6, 0.8, 0.4);
		view.armor.piece(view.turret, sight.view(view.gunMaterial), sightLens.view(materials.cyan()));
	}
}
