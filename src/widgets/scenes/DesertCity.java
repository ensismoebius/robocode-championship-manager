package widgets.scenes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The city of the desert arena, beyond the ring of dunes: a grid of streets, with pavements, kerbs and dashes of light
 * down the middle, and between them the blocks. A block is a lot, paved, with its houses; a garden of the "four gardens"
 * kind of the Islamic tradition, with its channels of glowing water crossing at a fountain, its cypresses and its palms;
 * or a plaza of zellij tiles, under a mosque, a gate or a skyscraper. Street lamps stand at the crossings, and far away,
 * where the city fades into the horizon, the blocks have glass towers of their own. Streets run all the way to the horizon
 * under the ring of dunes, which hides their beginning.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertCity {

	/** What stands on a block of the city */
	enum Kind {
		LOT, GARDEN, PLAZA
	}

	/** The streets are this far apart, centre to centre, and this wide */
	static final double PITCH = 500;
	static final double STREET = 80;
	/** How many streets there are on each side of the middle: the city goes this many blocks out */
	private static final int LINES = 12;
	/** The floor of the city lies this far over the plain, so the dunes that rise from the plain cover it and it shows where they are gone */
	private static final double LIFT = 1.5;
	/** The floor starts this far beyond the courtyard, under the swell of the dunes, and buildings are put only beyond the dunes, a little farther than their end */
	private static final double START = DesertDunes.FLAT + 0.2 * (DesertDunes.END - DesertDunes.FLAT);
	private static final double BUILT = DesertDunes.END + 220;
	/** How many skyscrapers there are, and how far apart at least */
	private static final int TOWERS = 10;
	private static final double APART = 700;
	/** How far from the middle the skyscrapers, the houses, the palms of the gardens and the street lamps go, at most */
	private static final double TOWER_REACH = 2800;
	private static final double HOUSE_REACH = 2800;
	private static final double PALM_REACH = 2400;
	private static final double LAMP_REACH = 2600;
	/** How far out the glass towers of the horizon go */
	private static final double HORIZON_REACH = 5600;
	/** The share of the blocks that are gardens, in a hundred */
	private static final int GARDENS = 28;
	/** The paving repeats its picture every this many units, a street every this many along its length */
	private static final double PAVING = 105;
	private static final double ROAD = 160;

	private final double width;
	private final double depth;
	private final List<double[]> towers;
	private final IslamicForms forms = new IslamicForms();
	private final SurfaceMesh horizon = new SurfaceMesh();

	/** The city around a courtyard of the given size, with its skyscrapers placed. */
	DesertCity(double width, double depth) {
		this.width = width;
		this.depth = depth;
		this.towers = this.chooseTowers();
	}

	/** The index of the block that contains a coordinate: the block between street i and street i + 1 has index i. */
	static int block(double coordinate) {
		return (int) Math.floor(coordinate / PITCH);
	}

	/** The middle {x, z} of a block. */
	static double[] centre(int column, int row) {
		return new double[] { (column + 0.5) * PITCH, (row + 0.5) * PITCH };
	}

	/** The middles {x, z} of the plazas of the skyscrapers, one for each. */
	List<double[]> towers() {
		return this.towers;
	}

	/** Whether the block has a monument on it, a mosque or a gate. */
	private static boolean monument(int column, int row) {
		for (double[] site : DesertDunes.SITES) {
			if (block(site[0]) == column && block(site[1]) == row) {
				return true;
			}
		}
		return false;
	}

	/** Whether the block is beyond the dunes, where things are built. */
	boolean built(int column, int row) {
		double[] middle = centre(column, row);
		return DesertDunes.outside(middle[0], middle[1], this.width, this.depth) >= BUILT;
	}

	/** One skyscraper in turn for each block chosen among the blocks that are built, not too close to the middle, spread so no two are near each other. */
	private List<double[]> chooseTowers() {
		List<double[]> pool = new ArrayList<double[]>();
		for (int column = -6; column < 6; column++) {
			for (int row = -6; row < 6; row++) {
				double[] middle = centre(column, row);
				if (!monument(column, row) && this.built(column, row) && Math.hypot(middle[0], middle[1]) <= TOWER_REACH) {
					pool.add(middle);
				}
			}
		}
		Collections.shuffle(pool, new Random(2002));
		List<double[]> chosen = new ArrayList<double[]>();
		for (double[] candidate : pool) {
			if (chosen.size() < TOWERS && chosen.stream().allMatch(other -> Math.hypot(candidate[0] - other[0], candidate[1] - other[1]) >= APART)) {
				chosen.add(candidate);
			}
		}
		return chosen;
	}

	/** What stands on a block: a plaza where there is a monument or a skyscraper, a garden for about a quarter of the rest of the blocks beyond the dunes, a lot for the others. */
	Kind kind(int column, int row) {
		double[] middle = centre(column, row);
		if (monument(column, row) || this.towers.stream().anyMatch(tower -> tower[0] == middle[0] && tower[1] == middle[1])) {
			return Kind.PLAZA;
		}
		boolean garden = this.built(column, row) && Math.floorMod(column * 73856093 ^ row * 19349663, 100) < GARDENS;
		return garden ? Kind.GARDEN : Kind.LOT;
	}

	/** The places of the houses: up to four on every lot, one in each quarter of it, so they keep clear of the streets and of one another. */
	List<double[]> houses(Random wild) {
		List<double[]> spots = new ArrayList<double[]>();
		for (int column = -6; column < 6; column++) {
			for (int row = -6; row < 6; row++) {
				double[] middle = centre(column, row);
				if (this.built(column, row) && this.kind(column, row) == Kind.LOT && Math.hypot(middle[0], middle[1]) <= HOUSE_REACH) {
					for (int quarter = 0; quarter < 4; quarter++) {
						if (wild.nextDouble() < 0.6) {
							spots.add(new double[] { middle[0] + (quarter % 2 * 2 - 1) * 105, middle[1] + (quarter / 2 * 2 - 1) * 105 });
						}
					}
				}
			}
		}
		return spots;
	}

	/** Builds the floor of the city, the gardens, the lamps and the towers of the horizon, and puts their meshes in the host scene. */
	void build(BattleArena3D host) {
		SurfaceMesh roads = new SurfaceMesh();
		SurfaceMesh crossings = new SurfaceMesh();
		SurfaceMesh lots = new SurfaceMesh();
		SurfaceMesh gardens = new SurfaceMesh();
		SurfaceMesh plazas = new SurfaceMesh();
		List<double[]> axis = axis();
		for (double[] across : axis) {
			for (double[] down : axis) {
				double nearX = Math.max(across[0], Math.min(0, across[1]));
				double nearZ = Math.max(down[0], Math.min(0, down[1]));
				if (DesertDunes.outside(nearX, nearZ, this.width, this.depth) >= START) {
					this.piece(across, down, roads, crossings, lots, gardens, plazas);
				}
			}
		}
		this.gardens(host);
		this.lamps();
		this.horizon();
		PhongMaterial road = ArenaMaterials.textured(DesertTexture.ROAD, Color.gray(0.3));
		road.setSelfIlluminationMap(DesertTexture.ROAD_LIGHT);
		PhongMaterial garden = ArenaMaterials.textured(DesertTexture.GARDEN, Color.gray(0.1));
		garden.setSelfIlluminationMap(DesertTexture.GARDEN_LIGHT);
		PhongMaterial plaza = ArenaMaterials.textured(DesertTexture.MOSAIC, Color.gray(0.2));
		plaza.setSelfIlluminationMap(DesertTexture.FLOOR_LIGHT);
		host.arena.getChildren().addAll(roads.view(road), crossings.view(ArenaMaterials.textured(DesertTexture.ASPHALT, Color.gray(0.3))), lots.view(ArenaMaterials.textured(DesertTexture.PAVING, Color.gray(0.1))), gardens.view(garden),
				plazas.view(plaza), this.horizon.view(DesertSkyline.skin()));
		host.arena.getChildren().addAll(this.forms.views());
	}

	/** The pieces of one axis: a street, the block after it, the next street and so on, each as {from, to, 1 if it is a street, its index}. */
	private static List<double[]> axis() {
		List<double[]> pieces = new ArrayList<double[]>();
		for (int line = -LINES; line <= LINES; line++) {
			pieces.add(new double[] { line * PITCH - STREET / 2, line * PITCH + STREET / 2, 1, line });
			if (line < LINES) {
				pieces.add(new double[] { line * PITCH + STREET / 2, (line + 1) * PITCH - STREET / 2, 0, line });
			}
		}
		return pieces;
	}

	/** One piece of the floor, where a piece of the x axis crosses a piece of the z axis: a crossing, a stretch of street or a block. */
	private void piece(double[] across, double[] down, SurfaceMesh roads, SurfaceMesh crossings, SurfaceMesh lots, SurfaceMesh gardens, SurfaceMesh plazas) {
		boolean streetX = across[2] == 1;
		boolean streetZ = down[2] == 1;
		if (streetX && streetZ) {
			lay(crossings, across, down, new float[] { 0, 0, 1, 0, 1, 1, 0, 1 });
		} else if (streetX) {
			float run = (float) ((down[1] - down[0]) / ROAD);
			lay(roads, across, down, new float[] { 0, 0, 1, 0, 1, run, 0, run });
		} else if (streetZ) {
			float run = (float) ((across[1] - across[0]) / ROAD);
			lay(roads, across, down, new float[] { 0, 0, 0, run, 1, run, 1, 0 });
		} else {
			this.block(across, down, lots, gardens, plazas);
		}
	}

	/** A block of the floor, with the picture of what stands on it. */
	private void block(double[] across, double[] down, SurfaceMesh lots, SurfaceMesh gardens, SurfaceMesh plazas) {
		Kind kind = this.kind((int) across[3], (int) down[3]);
		if (kind == Kind.GARDEN) {
			lay(gardens, across, down, new float[] { 0, 0, 1, 0, 1, 1, 0, 1 });
		} else {
			double tile = kind == Kind.PLAZA ? DesertArena.TILE : PAVING;
			float[] coords = { (float) (across[0] / tile), (float) (down[0] / tile), (float) (across[1] / tile), (float) (down[0] / tile), (float) (across[1] / tile), (float) (down[1] / tile), (float) (across[0] / tile),
					(float) (down[1] / tile) };
			lay(kind == Kind.PLAZA ? plazas : lots, across, down, coords);
		}
	}

	/** A rectangle of the floor, from the x range and the z range of two pieces, facing up. */
	private static void lay(SurfaceMesh mesh, double[] across, double[] down, float[] coords) {
		double heightY = DesertDunes.PLAIN - LIFT;
		mesh.quad(new double[] { across[0], heightY, down[0] }, new double[] { across[1], heightY, down[0] }, new double[] { across[1], heightY, down[1] }, new double[] { across[0], heightY, down[1] }, coords, new double[] { 0, -1, 0 });
	}

	/** The fountain, the cypresses and the palms of every garden beyond the dunes, put in the host scene. */
	private void gardens(BattleArena3D host) {
		Random wild = new Random(3003);
		LowPoly cypresses = new LowPoly();
		DesertPalms palms = new DesertPalms(host);
		for (int column = -6; column < 6; column++) {
			for (int row = -6; row < 6; row++) {
				double[] middle = centre(column, row);
				if (this.kind(column, row) == Kind.GARDEN && Math.hypot(middle[0], middle[1]) <= HOUSE_REACH + 200) {
					this.fountain(middle[0], middle[1]);
					this.cypresses(cypresses, middle, wild);
					if (Math.hypot(middle[0], middle[1]) <= PALM_REACH) {
						this.palms(palms, middle, wild);
					}
				}
			}
		}
		host.arena.getChildren().add(cypresses.view(ArenaMaterials.material(Color.web("#1d5b2e"))));
		palms.show();
	}

	/** The fountain in the middle of a garden: a short column with a lamp, and a jet of light over the pool. */
	private void fountain(double posX, double posZ) {
		double floor = DesertDunes.PLAIN - LIFT;
		this.forms.ceramic.cylinderY(posX, posZ, 6, floor, floor - 14, 8);
		this.forms.light.cylinderY(posX, posZ, 9, floor - 14, floor - 17, 8);
		this.forms.needle(posX, posZ, floor - 17, 46);
		this.forms.halo(posX, posZ, floor - 30, 14);
	}

	/** Eight cypresses along the path around a garden, two on each side, of different heights. */
	private void cypresses(LowPoly foliage, double[] middle, Random wild) {
		double[][] places = { { -195, -100 }, { -195, 100 }, { 195, -100 }, { 195, 100 }, { -100, -195 }, { 100, -195 }, { -100, 195 }, { 100, 195 } };
		for (double[] place : places) {
			double high = 55 + wild.nextDouble() * 30;
			foliage.frustum(DesertSkyline.polygon(middle[0] + place[0], middle[1] + place[1], 10, 6, 0), DesertDunes.PLAIN, DesertDunes.PLAIN - high, 0.1);
		}
	}

	/** A palm on each corner of the path around a garden. */
	private void palms(DesertPalms palms, double[] middle, Random wild) {
		for (int corner = 0; corner < 4; corner++) {
			palms.plant(middle[0] + (corner % 2 * 2 - 1) * 195, DesertDunes.PLAIN, middle[1] + (corner / 2 * 2 - 1) * 195, wild);
		}
	}

	/** A street lamp at a corner of every crossing beyond the dunes: a slim post with a lamp of light on it. */
	private void lamps() {
		double floor = DesertDunes.PLAIN - LIFT;
		for (int column = -5; column <= 5; column++) {
			for (int row = -5; row <= 5; row++) {
				double posX = column * PITCH + STREET / 2 + 4;
				double posZ = row * PITCH + STREET / 2 + 4;
				if (Math.hypot(posX, posZ) <= LAMP_REACH && DesertDunes.outside(posX, posZ, this.width, this.depth) >= BUILT) {
					this.forms.ceramic.cylinderY(posX, posZ, 3, floor, floor - 70, 6);
					this.forms.light.cylinderY(posX, posZ, 6, floor - 70, floor - 78, 6);
				}
			}
		}
	}

	/** Glass towers of their own on the lots of the far blocks, so the city goes on to the horizon. */
	private void horizon() {
		Random wild = new Random(4004);
		for (int column = -LINES; column < LINES; column++) {
			for (int row = -LINES; row < LINES; row++) {
				double[] middle = centre(column, row);
				double far = Math.hypot(middle[0], middle[1]);
				if (far > HOUSE_REACH && far <= HORIZON_REACH && this.kind(column, row) == Kind.LOT && wild.nextDouble() < 0.85) {
					double[][] outline = DesertSkyline.rectangle(middle[0], middle[1], 0, 160 + wild.nextDouble() * 120, 160 + wild.nextDouble() * 120);
					double top = DesertDunes.PLAIN - 90 - wild.nextDouble() * 270;
					DesertSkyline.shaft(this.horizon, outline, DesertDunes.PLAIN + 30, top, DesertSkyline.CURTAIN);
					DesertSkyline.cap(this.forms, outline, top);
				}
			}
		}
	}
}
