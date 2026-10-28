package widgets.scenes;

import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The skyline of the desert arena, in the style of the contemporary architecture of the Arab cities: skyscrapers far
 * beyond the courtyard, of five kinds. The tripod, like the Burj Khalifa, whose three wings step back in a spiral around
 * a core that ends in a spire. The lattice tower, a glass cylinder inside a skin of diamonds like the mashrabiya, as in
 * the Doha Tower and the Al Bahar Towers. The portal tower, like the Kingdom Centre of Riyadh, with a parabolic opening
 * at the top crossed by a skybridge. The twisting tower, whose floors turn and lean as in the Capital Gate of Abu Dhabi
 * and the Cayan Tower. And the tower with a crown in the shape of the pointed arch of the old mosques. Their glass skin
 * glows at dusk, and they carry the halos and needles of light of the rest of the town. Each stands on the plaza of a block of the city.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertSkyline {

	/** The walls go this far under the ground, so a slope never leaves a gap under them */
	private static final double SINK = 70;
	/** The glass skin repeats its picture every this many units, and the lattice every this many */
	static final double CURTAIN = 208;
	private static final double DIAMONDS = 120;
	private static final int TIERS = 6;
	private static final int FLOORS = 16;
	private static final int SIDES = 20;
	private static final int SLICES = 14;

	private final BattleArena3D host;
	private final SurfaceMesh curtain = new SurfaceMesh();
	private final SurfaceMesh lattice = new SurfaceMesh();
	private final IslamicForms forms = new IslamicForms();

	/** The skyline, drawn in the host scene. */
	DesertSkyline(BattleArena3D host) {
		this.host = host;
	}

	/** Builds the towers, one of each kind in turn, each on the plaza of a block of the city, and puts their meshes in the host scene. */
	void build(double width, double depth, List<double[]> spots) {
		for (int index = 0; index < spots.size(); index++) {
			double posX = spots.get(index)[0];
			double posZ = spots.get(index)[1];
			double ground = DesertDunes.PLAIN - DesertDunes.height(posX, posZ, width, depth);
			Random wild = new Random(2002L + index);
			double height = 440 + wild.nextDouble() * 320;
			double turn = wild.nextDouble() * Math.PI;
			switch (index % 5) {
			case 0 -> this.tripod(posX, posZ, ground, height, turn);
			case 1 -> this.latticeTower(posX, posZ, ground, height);
			case 2 -> this.portalTower(posX, posZ, ground, height, turn);
			case 3 -> this.twistingTower(posX, posZ, ground, height, turn);
			default -> this.crownedTower(posX, posZ, ground, height, turn);
			}
		}
		PhongMaterial diamonds = ArenaMaterials.textured(DesertTexture.MASHRABIYA, Color.gray(0.6));
		diamonds.setSelfIlluminationMap(DesertTexture.MASHRABIYA_LIGHT);
		this.host.arena.getChildren().addAll(this.curtain.view(skin()), this.lattice.view(diamonds));
		this.host.arena.getChildren().addAll(this.forms.views());
	}

	/** The material of the glass skin of the skyscrapers: glass in frames of ceramic, with the lit floors shining. */
	static PhongMaterial skin() {
		PhongMaterial glass = ArenaMaterials.textured(DesertTexture.CURTAIN, Color.gray(0.6));
		glass.setSelfIlluminationMap(DesertTexture.CURTAIN_LIGHT);
		return glass;
	}

	/**
	 * The Burj Khalifa: a round core and three wings, each in tiers that get shorter as they rise, the tiers of the wings
	 * ending one after the other so the setbacks spiral up around the core, to a dome and a long spire of gold and light.
	 */
	private void tripod(double posX, double posZ, double ground, double height, double turn) {
		double core = height * 0.075;
		double coreTop = ground - height * 0.9;
		this.forms.ceramic.cylinderY(posX, posZ, core, ground + SINK, coreTop, 12);
		for (int wing = 0; wing < 3; wing++) {
			double angle = turn + Math.PI * 2 * wing / 3;
			for (int tier = 0; tier < TIERS; tier++) {
				double length = height * 0.22 * (1 - tier / (TIERS + 0.5));
				double[][] outline = rectangle(posX + Math.cos(angle) * length / 2, posZ + Math.sin(angle) * length / 2, angle, length, height * 0.1);
				double start = tier == 0 ? ground + SINK : ground - tierTop(height, tier - 1, wing);
				shaft(this.curtain, outline, start, ground - tierTop(height, tier, wing), CURTAIN);
				cap(this.forms, outline, ground - tierTop(height, tier, wing));
			}
		}
		this.forms.dome(this.forms.glass, posX, posZ, coreTop, core, core * 1.4, 12);
		this.forms.spire(posX, posZ, coreTop - core * 1.3, height * 0.1, 3);
		this.forms.needle(posX, posZ, coreTop - core * 1.3 - height * 0.1, height * 0.3);
		this.forms.halo(posX, posZ, coreTop - core * 0.6, core * 2.4);
	}

	/** How high the given tier of a wing of the tripod ends: the wings end a third of a tier after one another. */
	private static double tierTop(double height, int tier, int wing) {
		return height * 0.88 * (tier + 1 - wing / 3.0) / TIERS;
	}

	/**
	 * The Doha Tower: a cylinder inside a skin of diamonds that glow like the mashrabiya, a glass dome on its flat top, bands
	 * and a halo of light.
	 */
	private void latticeTower(double posX, double posZ, double ground, double height) {
		double radius = 40 + height * 0.04;
		double[][] outline = polygon(posX, posZ, radius, SIDES, 0);
		shaft(this.lattice, outline, ground + SINK, ground - height, DIAMONDS);
		cap(this.forms, outline, ground - height);
		this.forms.ceramic.cylinderY(posX, posZ, radius * 1.1, ground + SINK, ground - 14, SIDES);
		this.forms.band(posX, posZ, radius * 1.08, ground - height * 0.5);
		this.forms.dome(this.forms.glass, posX, posZ, ground - height - 3, radius * 0.8, radius * 0.7, 12);
		this.forms.spire(posX, posZ, ground - height - 3 - radius * 0.66, radius * 0.9, 2);
		this.forms.halo(posX, posZ, ground - height - radius * 0.9, radius * 1.8);
	}

	/**
	 * The Kingdom Centre: a slab of glass with a parabolic opening in its upper third, narrow at the bottom and wide at the
	 * top, so what is left of the slab is two legs, and across the opening a skybridge with a line of light under it.
	 */
	private void portalTower(double posX, double posZ, double ground, double height, double turn) {
		double wide = height * 0.26;
		double deep = wide * 0.38;
		double notch = height * 0.34;
		double bodyTop = ground - (height - notch);
		shaft(this.curtain, rectangle(posX, posZ, turn, wide, deep), ground + SINK, bodyTop, CURTAIN);
		for (int slice = 0; slice < SLICES; slice++) {
			double opening = wide * 0.3 * Math.sqrt((slice + 0.5) / SLICES);
			double leg = wide / 2 - opening;
			double offset = (wide / 2 + opening) / 2;
			for (int side = -1; side <= 1; side += 2) {
				double[][] outline = rectangle(posX + Math.cos(turn) * side * offset, posZ + Math.sin(turn) * side * offset, turn, leg, deep);
				shaft(this.curtain, outline, bodyTop - notch * slice / SLICES, bodyTop - notch * (slice + 1) / SLICES, CURTAIN);
				if (slice == SLICES - 1) {
					cap(this.forms, outline, bodyTop - notch);
				}
			}
		}
		double bridge = bodyTop - notch * 0.45;
		double reach = wide * 0.6 * Math.sqrt(0.45);
		this.slab(this.forms.ceramic, posX, posZ, turn, reach, deep * 0.6, bridge + 6, bridge - 6);
		this.slab(this.forms.light, posX, posZ, turn, reach, deep * 0.62, bridge + 8, bridge + 6);
		for (int side = -1; side <= 1; side += 2) {
			this.forms.needle(posX + Math.cos(turn) * side * wide * 0.4, posZ + Math.sin(turn) * side * wide * 0.4, ground - height, height * 0.25);
		}
	}

	/**
	 * The Capital Gate and the Cayan Tower: floors that turn a little more at every tier and lean farther and farther out,
	 * each tier a little smaller, so the ledge of every tier shows as a line of light, and a halo and a needle on the top.
	 */
	private void twistingTower(double posX, double posZ, double ground, double height, double turn) {
		double[][] outline = null;
		for (int floor = 0; floor < FLOORS; floor++) {
			double low = (double) floor / FLOORS;
			double high = (double) (floor + 1) / FLOORS;
			double lean = height * 0.2 * low * low;
			double side = height * 0.2 * (1 - 0.3 * low);
			outline = rectangle(posX + Math.cos(turn) * lean, posZ + Math.sin(turn) * lean, turn + 0.9 * low, side, side * 0.8);
			shaft(this.curtain, outline, floor == 0 ? ground + SINK : ground - height * low, ground - height * high, CURTAIN);
			cap(this.forms, outline, ground - height * high);
		}
		double[] top = centre(outline);
		this.forms.halo(top[0], top[1], ground - height - 20, height * 0.12);
		this.forms.needle(top[0], top[1], ground - height - 3, height * 0.25);
	}

	/**
	 * A tower with a crown of the pointed arch of the old mosques, turned into a pointed spire of glass: the crown is cut in
	 * slices that narrow as the arch does, and over it a spire of gold, a halo and a needle of light.
	 */
	private void crownedTower(double posX, double posZ, double ground, double height, double turn) {
		double wide = height * 0.2;
		double crown = height * 0.22;
		double bodyTop = ground - (height - crown);
		double[][] outline = rectangle(posX, posZ, turn, wide, wide);
		shaft(this.curtain, outline, ground + SINK, bodyTop, CURTAIN);
		cap(this.forms, outline, bodyTop);
		for (int slice = 0; slice < SLICES; slice++) {
			double share = (slice + 0.5) / SLICES;
			double half = IslamicForms.archHalfWidth(share * Math.sqrt(3) * wide / 2, wide / 2);
			this.slab(this.forms.glass, posX, posZ, turn, 2 * half, 2 * half, bodyTop - 3 - crown * slice / SLICES, bodyTop - 3 - crown * (slice + 1) / SLICES);
		}
		this.forms.spire(posX, posZ, bodyTop - 3 - crown, crown * 0.35, 3);
		this.forms.halo(posX, posZ, bodyTop - crown * 0.6, wide * 0.8);
		this.forms.needle(posX, posZ, bodyTop - 3 - crown * 1.35, height * 0.3);
	}

	/** The walls around an outline, from a foot to a top, with the picture repeating every tile units, going on from one wall to the next and from one height to the next. */
	static void shaft(SurfaceMesh mesh, double[][] outline, double foot, double top, double tile) {
		double[] middle = centre(outline);
		float along = 0;
		for (int side = 0; side < outline.length; side++) {
			double[] first = outline[side];
			double[] second = outline[(side + 1) % outline.length];
			double[] facing = DesertTown.outward(first, second, middle[0], middle[1]);
			float until = along + (float) (Math.hypot(second[0] - first[0], second[1] - first[1]) / tile);
			float lower = (float) (foot / tile);
			float upper = (float) (top / tile);
			mesh.quad(DesertTown.pointAt(first, foot, facing, 0), DesertTown.pointAt(second, foot, facing, 0), DesertTown.pointAt(second, top, facing, 0), DesertTown.pointAt(first, top, facing, 0),
					new float[] { along, lower, until, lower, until, upper, along, upper }, facing);
			along = until;
		}
	}

	/** The roof of a tier: a lip of ceramic a little wider than the wall, with a line of light under it. */
	static void cap(IslamicForms forms, double[][] outline, double top) {
		forms.ceramic.solid(level(outline, 1.06, top), level(outline, 1.06, top - 3));
		forms.light.solid(level(outline, 1.04, top + 1.6), level(outline, 1.04, top));
	}

	/** A turned box of a shape: its centre, the way its length points, its size, and the heights of its two faces. */
	private void slab(LowPoly shape, double posX, double posZ, double angle, double length, double thick, double lower, double upper) {
		double[][] outline = rectangle(posX, posZ, angle, length, thick);
		shape.solid(level(outline, 1, lower), level(outline, 1, upper));
	}

	/** The corners {x, z} of a rectangle turned by an angle, its length along the angle. */
	static double[][] rectangle(double posX, double posZ, double angle, double length, double thick) {
		double[][] corners = new double[4][];
		double[][] signs = { { -1, -1 }, { 1, -1 }, { 1, 1 }, { -1, 1 } };
		for (int index = 0; index < 4; index++) {
			double along = signs[index][0] * length / 2;
			double across = signs[index][1] * thick / 2;
			corners[index] = new double[] { posX + Math.cos(angle) * along - Math.sin(angle) * across, posZ + Math.sin(angle) * along + Math.cos(angle) * across };
		}
		return corners;
	}

	/** The corners {x, z} of a regular polygon. */
	static double[][] polygon(double posX, double posZ, double radius, int sides, double turn) {
		double[][] corners = new double[sides][];
		for (int index = 0; index < sides; index++) {
			double angle = turn + Math.PI * 2 * index / sides;
			corners[index] = new double[] { posX + Math.cos(angle) * radius, posZ + Math.sin(angle) * radius };
		}
		return corners;
	}

	/** The middle {x, z} of an outline. */
	private static double[] centre(double[][] outline) {
		double posX = 0;
		double posZ = 0;
		for (double[] corner : outline) {
			posX += corner[0] / outline.length;
			posZ += corner[1] / outline.length;
		}
		return new double[] { posX, posZ };
	}

	/** An outline {x, z} as points {x, y, z} at a height, grown around its middle by a factor. */
	private static double[][] level(double[][] outline, double grow, double heightY) {
		double[] middle = centre(outline);
		double[][] points = new double[outline.length][];
		for (int index = 0; index < outline.length; index++) {
			points[index] = new double[] { middle[0] + (outline[index][0] - middle[0]) * grow, heightY, middle[1] + (outline[index][1] - middle[1]) * grow };
		}
		return points;
	}
}
