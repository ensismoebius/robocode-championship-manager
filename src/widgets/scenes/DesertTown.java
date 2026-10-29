package widgets.scenes;

import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The town of the desert arena, built as the Islamic cities were but from the future: low houses of pearl white ceramic,
 * with flat roofs, parapets edged in light, a frieze of glowing tiles under them and windows with pointed arches outlined
 * in light, some with a dome of glass on a drum and some with a wind tower; two mosques, each with its great dome of glass
 * under a halo of light, the domes of its corners, the portal with its pointed arch closed by a veil of light and two
 * minarets topped by needles of light; and two gates in the wall of the town, between round towers, their arches also
 * veiled. The mosques and the gates stand on levelled ground, far beyond the courtyard. The windows glow at dusk.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertTown {

	/** The ceramic repeats its picture every this many units, and the windows every this many */
	private static final double WALL = 150;
	private static final double WINDOW = 120;
	/** The windows stand this far out of the walls, so they never flicker into them */
	private static final double OUT = 0.6;
	/** The walls go this far under the ground, so a slope never leaves a gap under them */
	private static final double SINK = 30;
	/** The frieze of tiles under the roof, where the star and cross of the courtyard goes on in a small size: how tall it is, how far under the roof it starts, and how far it juts out of the wall */
	private static final double FRIEZE = 16;
	private static final double FRIEZE_TOP = 3;
	private static final double JUT = 1.5;

	private final BattleArena3D host;
	private final SurfaceMesh walls = new SurfaceMesh();
	private final SurfaceMesh windows = new SurfaceMesh();
	private final SurfaceMesh friezes = new SurfaceMesh();
	private final IslamicForms forms = new IslamicForms();

	/** The town, drawn in the host scene. */
	DesertTown(BattleArena3D host) {
		this.host = host;
	}

	/** Builds the monuments on their sites and the houses on the lots of the city, far from the courtyard, and puts the meshes in the host scene. */
	void build(double width, double depth, Random wild, DesertCity city) {
		double[][] sites = DesertDunes.SITES;
		this.mosque(sites[0], 1.3, width, depth);
		this.mosque(sites[1], 1, width, depth);
		this.gate(sites[2], 1, width, depth);
		this.gate(sites[3], 0.8, width, depth);
		for (double[] spot : city.houses(wild)) {
			this.house(spot[0], spot[1], width, depth, wild);
		}
		PhongMaterial ceramic = ArenaMaterials.textured(DesertTexture.PANELS, Color.gray(0.4));
		PhongMaterial arches = ArenaMaterials.textured(DesertTexture.ARCHES, Color.gray(0.4));
		arches.setSelfIlluminationMap(DesertTexture.WINDOWS);
		PhongMaterial tiles = ArenaMaterials.textured(DesertTexture.MOSAIC, Color.gray(0.1));
		tiles.setSelfIlluminationMap(DesertTexture.MOSAIC_LIGHT);
		host.arena.getChildren().addAll(this.walls.view(ceramic), this.windows.view(arches), this.friezes.view(tiles));
		host.arena.getChildren().addAll(this.forms.views());
	}

	/** One house, its base on the dunes where it stands, with its roof and what is over it: a dome on a drum or a wind tower. */
	private void house(double posX, double posZ, double width, double depth, Random wild) {
		double wide = 80 + wild.nextDouble() * 70;
		double deep = 80 + wild.nextDouble() * 70;
		double high = 70 + wild.nextDouble() * wild.nextDouble() * 230;
		double ground = DesertDunes.PLAIN - DesertDunes.height(posX, posZ, width, depth);
		this.block(posX, posZ, wide, deep, high, ground);
		double top = ground - high;
		this.parapet(posX, posZ, wide, deep, top);
		double feature = wild.nextDouble();
		if (feature < 0.4) {
			this.houseDome(posX, posZ, Math.min(wide, deep) * 0.34, top);
		} else if (feature < 0.65) {
			this.windTower(posX + wide * 0.3, posZ - deep * 0.3, top);
		}
	}

	/** The four walls of a block of masonry, with their windows and the frieze of tiles under its flat roof, and the roof. */
	private void block(double posX, double posZ, double wide, double deep, double high, double ground) {
		double[][] corners = { { posX - wide / 2, posZ - deep / 2 }, { posX + wide / 2, posZ - deep / 2 }, { posX + wide / 2, posZ + deep / 2 }, { posX - wide / 2, posZ + deep / 2 } };
		for (int side = 0; side < 4; side++) {
			this.face(corners[side], corners[(side + 1) % 4], posX, posZ, ground, high);
		}
		this.roof(corners, ground - high);
	}

	/** One wall of a block, from its first corner to its second, with its windows a little out of it */
	private void face(double[] first, double[] second, double centreX, double centreZ, double ground, double high) {
		double[] facing = outward(first, second, centreX, centreZ);
		double length = Math.hypot(second[0] - first[0], second[1] - first[1]);
		double top = ground - high;
		double foot = ground + SINK;
		this.walls.quad(pointAt(first, foot, facing, 0), pointAt(second, foot, facing, 0), pointAt(second, top, facing, 0), pointAt(first, top, facing, 0), coords(length, high + SINK, WALL), facing);
		// The frieze of tiles runs under the roof
		double lower = top + FRIEZE_TOP + FRIEZE;
		this.friezes.quad(pointAt(first, lower, facing, JUT), pointAt(second, lower, facing, JUT), pointAt(second, top + FRIEZE_TOP, facing, JUT), pointAt(first, top + FRIEZE_TOP, facing, JUT), coords(length, FRIEZE, FRIEZE), facing);
		// The windows fill the wall between a ledge above the ground and the frieze
		double bottom = ground - Math.min(40, high / 3);
		double windowTop = lower + 4;
		if (bottom > windowTop) {
			this.windows.quad(pointAt(first, bottom, facing, OUT), pointAt(second, bottom, facing, OUT), pointAt(second, windowTop, facing, OUT), pointAt(first, windowTop, facing, OUT), coords(length, bottom - windowTop, WINDOW),
					facing);
		}
	}

	/** The flat roof of a block, facing up, with its edge at the top of the walls */
	private void roof(double[][] corners, double top) {
		double[] upward = { 0, -1, 0 };
		double[][] points = new double[4][];
		for (int index = 0; index < 4; index++) {
			points[index] = new double[] { corners[index][0], top, corners[index][1] };
		}
		this.walls.quad(points[0], points[1], points[2], points[3], new float[] { 0, 0, 1, 0, 1, 1, 0, 1 }, upward);
	}

	/** A plain parapet of ceramic along the edge of a roof, with a line of light along its top */
	private void parapet(double posX, double posZ, double wide, double deep, double top) {
		this.forms.ceramic.box(posX, top - 4, posZ - deep / 2, wide, 8, 4);
		this.forms.ceramic.box(posX, top - 4, posZ + deep / 2, wide, 8, 4);
		this.forms.ceramic.box(posX - wide / 2, top - 4, posZ, 4, 8, deep);
		this.forms.ceramic.box(posX + wide / 2, top - 4, posZ, 4, 8, deep);
		this.forms.light.box(posX, top - 8.3, posZ - deep / 2, wide, 1.2, 4.6);
		this.forms.light.box(posX, top - 8.3, posZ + deep / 2, wide, 1.2, 4.6);
		this.forms.light.box(posX - wide / 2, top - 8.3, posZ, 4.6, 1.2, deep);
		this.forms.light.box(posX + wide / 2, top - 8.3, posZ, 4.6, 1.2, deep);
	}

	/** Merlons along the parapet of a roof, with their tops at the height given */
	private void merlons(double posX, double posZ, double wide, double deep, double topY) {
		this.forms.merlons(true, posZ - deep / 2, posX - wide / 2, posX + wide / 2, topY, 4);
		this.forms.merlons(true, posZ + deep / 2, posX - wide / 2, posX + wide / 2, topY, 4);
		this.forms.merlons(false, posX - wide / 2, posZ - deep / 2, posZ + deep / 2, topY, 4);
		this.forms.merlons(false, posX + wide / 2, posZ - deep / 2, posZ + deep / 2, topY, 4);
	}

	/** A dome of glass on a short drum over a roof, with a band of light around the drum and its spire */
	private void houseDome(double posX, double posZ, double radius, double top) {
		this.forms.ceramic.cylinderY(posX, posZ, radius * 0.9, top, top - 10, 10);
		this.forms.light.cylinderY(posX, posZ, radius * 0.93, top - 5.5, top - 3, 10);
		this.forms.dome(this.forms.gold, posX, posZ, top - 10, radius, radius * 1.1, 10);
		this.forms.spire(posX, posZ, top - 10 - radius * 1.05, radius * 0.5, 1.6);
	}

	/** A wind tower over a roof, a tall shaft with a band of light under its cap, to bring the breeze down into the house */
	private void windTower(double posX, double posZ, double top) {
		this.forms.ceramic.box(posX, top - 50, posZ, 24, 100, 24);
		this.forms.light.box(posX, top - 96, posZ, 25.5, 3, 25.5);
		this.forms.ceramic.box(posX, top - 102, posZ, 34, 6, 34);
	}

	/**
	 * A mosque on its site: a hall with a great dome on a drum and a small dome on each corner, a portal with a pointed
	 * arch on the side that looks at the courtyard, and a minaret on each corner of that side.
	 */
	private void mosque(double[] site, double scale, double width, double depth) {
		double posX = site[0];
		double posZ = site[1];
		double wide = 200 * scale;
		double ground = DesertDunes.PLAIN - DesertDunes.height(posX, posZ, width, depth);
		double top = ground - 100 * scale;
		this.block(posX, posZ, wide, wide, 100 * scale, ground);
		this.parapet(posX, posZ, wide, wide, top);
		this.merlons(posX, posZ, wide, wide, top - 8);
		double drum = 36 * scale;
		this.forms.ceramic.cylinderY(posX, posZ, 66 * scale, top, top - drum, 8);
		this.forms.light.cylinderY(posX, posZ, 68 * scale, top - drum + 10 * scale, top - drum + 10 * scale + 3, 8);
		this.forms.dome(this.forms.gold, posX, posZ, top - drum, 72 * scale, 84 * scale, 16);
		this.forms.spire(posX, posZ, top - drum - 80 * scale, 50 * scale, 3 * scale);
		this.forms.halo(posX, posZ, top - drum - 84 * scale, 96 * scale);
		this.forms.halo(posX, posZ, top - drum - 118 * scale, 62 * scale);
		for (int sideX = -1; sideX <= 1; sideX += 2) {
			for (int sideZ = -1; sideZ <= 1; sideZ += 2) {
				double cornerX = posX + sideX * wide * 0.36;
				double cornerZ = posZ + sideZ * wide * 0.36;
				this.forms.dome(this.forms.gold, cornerX, cornerZ, top, 20 * scale, 26 * scale, 10);
				this.forms.spire(cornerX, cornerZ, top - 25 * scale, 14 * scale, 1.4 * scale);
			}
		}
		boolean frontOnX = Math.abs(posX) > Math.abs(posZ);
		double toward = -Math.signum(frontOnX ? posX : posZ);
		double portal = wide / 2 + 8 * scale;
		double portalX = frontOnX ? posX + toward * portal : posX;
		double portalZ = frontOnX ? posZ : posZ + toward * portal;
		this.forms.archway(portalX, portalZ, !frontOnX, 96 * scale, 16 * scale, ground, 150 * scale, 24 * scale, 40 * scale);
		this.forms.veil(portalX, portalZ, !frontOnX, ground, 24 * scale, 40 * scale);
		double corner = wide / 2 + 4 * scale;
		for (int side = -1; side <= 1; side += 2) {
			this.forms.minaret(frontOnX ? posX + toward * corner : posX + side * corner, frontOnX ? posZ + side * corner : posZ + toward * corner, ground, 240 * scale, 11 * scale);
		}
	}

	/**
	 * A gate in the wall of the town: a wall across the way to the courtyard with a pointed arch through it, a round tower
	 * at each end, the wall going on beyond the towers, and merlons along the top.
	 */
	private void gate(double[] site, double scale, double width, double depth) {
		double posX = site[0];
		double posZ = site[1];
		double ground = DesertDunes.PLAIN - DesertDunes.height(posX, posZ, width, depth);
		double high = 120 * scale;
		double length = 340 * scale;
		double thick = 24 * scale;
		double beyond = 120 * scale;
		boolean alongX = Math.abs(posX) <= Math.abs(posZ);
		this.forms.archway(posX, posZ, alongX, length, thick, ground, high, 30 * scale, 36 * scale);
		this.forms.veil(posX, posZ, alongX, ground, 30 * scale, 36 * scale);
		for (int side = -1; side <= 1; side += 2) {
			double endX = alongX ? posX + side * length / 2 : posX;
			double endZ = alongX ? posZ : posZ + side * length / 2;
			this.forms.tower(endX, endZ, ground, 160 * scale, 34 * scale);
			double wallX = alongX ? posX + side * (length / 2 + beyond / 2) : posX;
			double wallZ = alongX ? posZ : posZ + side * (length / 2 + beyond / 2);
			this.forms.ceramic.box(wallX, ground - high / 2 + SINK / 2, wallZ, alongX ? beyond : thick, high + SINK, alongX ? thick : beyond);
		}
		double reach = length / 2 + beyond;
		if (alongX) {
			this.forms.merlons(true, posZ, posX - reach, posX + reach, ground - high, thick);
		} else {
			this.forms.merlons(false, posX, posZ - reach, posZ + reach, ground - high, thick);
		}
	}

	/** The way a wall faces: away from the middle of the block. */
	static double[] outward(double[] first, double[] second, double centreX, double centreZ) {
		double normalX = -(second[1] - first[1]);
		double normalZ = second[0] - first[0];
		double middleX = (first[0] + second[0]) / 2 - centreX;
		double middleZ = (first[1] + second[1]) / 2 - centreZ;
		if (middleX * normalX + middleZ * normalZ < 0) {
			normalX = -normalX;
			normalZ = -normalZ;
		}
		double length = Math.hypot(normalX, normalZ);
		return new double[] { normalX / length, 0, normalZ / length };
	}

	/** A point of a wall at a height, moved out of the wall along its facing */
	static double[] pointAt(double[] point, double height, double[] facing, double out) {
		return new double[] { point[0] + facing[0] * out, height, point[1] + facing[2] * out };
	}

	/** The texture coordinates of a wall of the given size, the picture repeating every tile units */
	private static float[] coords(double length, double height, double tile) {
		return new float[] { 0, 0, (float) (length / tile), 0, (float) (length / tile), (float) (height / tile), 0, (float) (height / tile) };
	}
}
