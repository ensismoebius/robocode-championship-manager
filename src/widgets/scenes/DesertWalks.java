package widgets.scenes;

import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * What is on the pedestrian streets of the city of the desert arena, where no car goes: trees on one side and a planter of
 * flowers on the other, in turns, a bench and a lamp between them, people walking in robes of white and black, and at
 * the ends, where a street for cars crosses, the bollards that close it to them.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertWalks {

	/** The pedestrian streets are looked for between the streets of the grid, this many to each side of the middle */
	private static final int LINES = 5;
	/** The trees, the benches and the lamps of a street are this far apart along it, and this far from its middle */
	private static final double STEP = 105;
	private static final double TREE_SIDE = 26;
	private static final double BENCH_SIDE = 30;
	private static final double LAMP_SIDE = 34;
	/** How far from the middle of the arena the pedestrian streets are dressed, at most */
	private static final double REACH = 2400;
	private static final int PEOPLE = 3;
	/** How tall a person is, and how far a bollard stands from the crossing, and from the middle of its street */
	private static final double PERSON = 19;
	private static final double BOLLARD_ALONG = 46;
	private static final double BOLLARD_SIDE = 24;

	private final DesertCity city;
	private final IslamicForms forms;
	private final DesertGreenery green;
	private final Random wild = new Random(5005);
	private final LowPoly pale = new LowPoly();
	private final LowPoly dark = new LowPoly();
	private final LowPoly skin = new LowPoly();

	/** The dressing of the pedestrian streets of the city, with its forms of ceramic and light and its plants. */
	DesertWalks(DesertCity city, IslamicForms forms, DesertGreenery green) {
		this.city = city;
		this.forms = forms;
		this.green = green;
	}

	/** Dresses every pedestrian street beyond the dunes, and closes its ends where a street for cars crosses. */
	void build() {
		for (int line = -LINES; line <= LINES; line++) {
			for (int row = -LINES; row < LINES; row++) {
				if (DesertCity.pedestrian(line)) {
					this.walk(line * DesertCity.PITCH, true, row);
					this.walk(line * DesertCity.PITCH, false, row);
				}
				this.bollards(line, row);
			}
		}
	}

	/** The meshes of the people that have something in them, each with its colour. */
	List<MeshView> views() {
		return List.of(this.pale.view(ArenaMaterials.material(Color.web("#f2efe6"))), this.dark.view(ArenaMaterials.material(Color.web("#23232b"))), this.skin.view(ArenaMaterials.material(Color.web("#b27d52")))).stream()
				.filter(view -> ((TriangleMesh) view.getMesh()).getFaces().size() > 0).toList();
	}

	/** The point {x, z} at a position along a street that runs along z at the x given, or along x at the z given, and at a distance from its middle. */
	private static double[] spot(double fixed, boolean alongZ, double position, double side) {
		return alongZ ? new double[] { fixed + side, position } : new double[] { position, fixed + side };
	}

	/** Whether a point is beyond the dunes and near enough to be dressed. */
	private boolean usable(double[] spot) {
		return this.city.beyondDunes(spot[0], spot[1]) && Math.hypot(spot[0], spot[1]) <= REACH;
	}

	/** One stretch of a pedestrian street, between two crossings: the trees and the planters, the benches and the lamps, and the people. */
	private void walk(double fixed, boolean alongZ, int row) {
		double from = row * DesertCity.PITCH + DesertCity.STREET / 2;
		for (int step = 0; step < 4; step++) {
			double position = from + STEP / 2 + step * STEP;
			int side = step % 2 == 0 ? -1 : 1;
			double[] tree = spot(fixed, alongZ, position, side * TREE_SIDE);
			if (this.usable(tree)) {
				this.green.tree(this.wild.nextInt(DesertGreenery.KINDS), tree[0], DesertDunes.PLAIN, tree[1], this.wild);
				double[] bed = spot(fixed, alongZ, position, -side * TREE_SIDE);
				this.planter(bed, step == 1);
			}
			double[] bench = spot(fixed, alongZ, position + STEP / 2, side * BENCH_SIDE);
			if (step < 3 && this.usable(bench)) {
				this.bench(bench, alongZ, side);
				this.lamp(spot(fixed, alongZ, position + STEP / 2, -side * LAMP_SIDE));
			}
		}
		for (int person = 0; person < PEOPLE; person++) {
			double[] where = spot(fixed, alongZ, from + 20 + this.wild.nextDouble() * (DesertCity.PITCH - DesertCity.STREET - 40), (this.wild.nextDouble() - 0.5) * 28);
			if (this.usable(where)) {
				this.person(where[0], where[1], this.wild.nextBoolean());
			}
		}
	}

	/** A low bed of stone with a shrub of flowers in it: bougainvillea, or oleander. */
	private void planter(double[] where, boolean oleander) {
		this.forms.ceramic.box(where[0], DesertCity.FLOOR - 3, where[1], 24, 6, 24);
		if (oleander) {
			this.green.oleander(where[0], DesertCity.FLOOR - 6, where[1], this.wild);
		} else {
			this.green.bougainvillea(where[0], DesertCity.FLOOR - 6, where[1], this.wild);
		}
	}

	/** A bench along the street, with its back to the side it stands on. */
	private void bench(double[] where, boolean alongZ, int side) {
		double along = 22;
		double across = 8;
		this.forms.ceramic.box(where[0], DesertCity.FLOOR - 4, where[1], alongZ ? across : along, 8, alongZ ? along : across);
		double back = side * (across / 2 + 1);
		this.forms.ceramic.box(where[0] + (alongZ ? back : 0), DesertCity.FLOOR - 12, where[1] + (alongZ ? 0 : back), alongZ ? 2 : along, 8, alongZ ? along : 2);
	}

	/** A lamp of a pedestrian street: a post with a head of light, lower than the lamps for cars. */
	private void lamp(double[] where) {
		this.forms.ceramic.cylinderY(where[0], where[1], 2.4, DesertCity.FLOOR, DesertCity.FLOOR - 46, 6);
		this.forms.light.cylinderY(where[0], where[1], 5, DesertCity.FLOOR - 46, DesertCity.FLOOR - 52, 6);
	}

	/** A person standing in the street: a robe, white or black, a head and a covering for it. */
	private void person(double posX, double posZ, boolean white) {
		LowPoly robe = white ? this.pale : this.dark;
		robe.frustum(DesertSkyline.polygon(posX, posZ, 4.5, 6, 0), DesertCity.FLOOR, DesertCity.FLOOR - PERSON * 0.78, 0.5);
		this.skin.cylinderY(posX, posZ, 2.2, DesertCity.FLOOR - PERSON * 0.78, DesertCity.FLOOR - PERSON * 0.94, 6);
		robe.cylinderY(posX, posZ, 2.7, DesertCity.FLOOR - PERSON * 0.92, DesertCity.FLOOR - PERSON, 6);
	}

	/** Where a street for cars crosses a pedestrian street, four bollards, two at each end of the crossing, close the way to cars. */
	private void bollards(int column, int row) {
		boolean walkAlongZ = DesertCity.pedestrian(column) && !DesertCity.pedestrian(row);
		boolean walkAlongX = DesertCity.pedestrian(row) && !DesertCity.pedestrian(column);
		if (!walkAlongZ && !walkAlongX) {
			return;
		}
		double crossX = column * DesertCity.PITCH;
		double crossZ = row * DesertCity.PITCH;
		for (int end = -1; end <= 1; end += 2) {
			for (int side = -1; side <= 1; side += 2) {
				double[] where = walkAlongZ ? new double[] { crossX + side * BOLLARD_SIDE, crossZ + end * BOLLARD_ALONG } : new double[] { crossX + end * BOLLARD_ALONG, crossZ + side * BOLLARD_SIDE };
				if (this.usable(where)) {
					this.forms.ceramic.cylinderY(where[0], where[1], 3.2, DesertCity.FLOOR, DesertCity.FLOOR - 12, 6);
					this.forms.light.cylinderY(where[0], where[1], 3.4, DesertCity.FLOOR - 12, DesertCity.FLOOR - 14, 6);
				}
			}
		}
	}
}
