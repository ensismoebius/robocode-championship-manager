package widgets.scenes;

import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The forms of Islamic architecture, for the desert arena: the pointed arch, the dome on its drum with a spire over it, the
 * minaret with its balcony on corbels, the round tower and the merlons of a parapet. Every form is built into one of three
 * meshes: the stone, the tiles of turquoise and the gold.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class IslamicForms {

	/** The pointed arch of a wall is cut in this many slices, from the ground to its tip */
	private static final int SLICES = 16;
	/** The size of a merlon, the block of a parapet: the gap between two of them is as wide */
	private static final double MERLON = 9;
	/** How many rings a dome has: the more of them, the rounder it is */
	private static final int RINGS = 7;

	final LowPoly stone = new LowPoly();
	final LowPoly tiles = new LowPoly();
	final LowPoly gold = new LowPoly();

	/**
	 * Half the width of a pointed arch at a height over its springline, and 0 over its tip. The arch is the equilateral one,
	 * two arcs of a radius twice its half width, so its tip stands the square root of 3 half widths over the springline.
	 * Under the springline the jambs are straight.
	 */
	static double archHalfWidth(double height, double half) {
		if (height <= 0) {
			return half;
		}
		if (height >= Math.sqrt(3) * half) {
			return 0;
		}
		return Math.sqrt(4 * half * half - height * height) - half;
	}

	/** The meshes that have something in them, each with its material: sandstone, turquoise tiles and gold. */
	List<MeshView> views() {
		return List.of(this.stone.view(ArenaMaterials.textured(DesertTexture.SANDSTONE, Color.gray(0.05))), this.tiles.view(ArenaMaterials.material(Color.web("#2aa7a0"))), this.gold.view(ArenaMaterials.material(Color.web("#e0b040"))))
				.stream().filter(view -> ((TriangleMesh) view.getMesh()).getFaces().size() > 0).toList();
	}

	/** A ring of points around a vertical axis, at a height */
	private static double[][] ring(double posX, double posZ, double heightY, double radius, int sides) {
		double[][] points = new double[sides][];
		for (int index = 0; index < sides; index++) {
			double angle = Math.PI * 2 * index / sides;
			points[index] = new double[] { posX + Math.cos(angle) * radius, heightY, posZ + Math.sin(angle) * radius };
		}
		return points;
	}

	/**
	 * A dome on a round base: rings that narrow as it rises, a little wider than the base at the bulge, as the domes of the
	 * old mosques. It stops short of its crown, where the spire goes.
	 */
	void dome(LowPoly shape, double posX, double posZ, double baseY, double radius, double high, int sides) {
		double[][] below = ring(posX, posZ, baseY, radius, sides);
		for (int level = 1; level <= RINGS; level++) {
			double share = 0.95 * level / RINGS;
			double[][] above = ring(posX, posZ, baseY - high * share, radius * (1 + 0.12 * Math.sin(Math.PI * share)) * Math.sqrt(1 - share * share), sides);
			shape.solid(below, above);
			below = above;
		}
	}

	/** A spire of gold over a dome or a minaret: a thin shaft with a bead on it. */
	void spire(double posX, double posZ, double baseY, double high, double radius) {
		this.gold.tube(new double[] { posX, baseY, posZ }, new double[] { posX, baseY - high, posZ }, radius, radius * 0.3, 6);
		this.gold.cylinderY(posX, posZ, radius * 2.4, baseY - high * 0.45, baseY - high * 0.45 - radius * 2.4, 8);
	}

	/**
	 * A minaret: a plinth, a round shaft, a balcony on three stepped corbels, a thinner shaft over the balcony and a dome
	 * with a spire.
	 */
	void minaret(double posX, double posZ, double ground, double high, double radius) {
		double balcony = ground - high * 0.7;
		double cap = ground - high * 0.9;
		this.stone.box(posX, ground - high * 0.06, posZ, radius * 2.8, high * 0.12, radius * 2.8);
		this.stone.cylinderY(posX, posZ, radius, ground - high * 0.1, balcony, 12);
		for (int step = 0; step < 3; step++) {
			this.stone.cylinderY(posX, posZ, radius * (1.15 + 0.2 * step), balcony - 4 * step, balcony - 4 * (step + 1), 12);
		}
		this.stone.cylinderY(posX, posZ, radius * 1.7, balcony - 12, balcony - 18, 12);
		this.stone.cylinderY(posX, posZ, radius * 0.8, balcony - 18, cap, 12);
		this.dome(this.tiles, posX, posZ, cap, radius, high * 0.1, 10);
		this.spire(posX, posZ, cap - high * 0.095, high * 0.08, radius * 0.12);
	}

	/** A round tower: a shaft, a ring that juts out under its top, and a dome of tiles over it. */
	void tower(double posX, double posZ, double ground, double high, double radius) {
		this.stone.cylinderY(posX, posZ, radius, ground + 20, ground - high, 12);
		this.stone.cylinderY(posX, posZ, radius * 1.18, ground - high, ground - high - 10, 12);
		this.dome(this.tiles, posX, posZ, ground - high - 10, radius * 1.1, radius * 0.9, 12);
		this.spire(posX, posZ, ground - high - 10 - radius * 0.86, radius * 0.7, radius * 0.1);
	}

	/**
	 * A wall with a pointed arch through it, along x or along z: on each side of the opening, slices of wall that narrow
	 * toward the tip of the arch, and a block over the tip.
	 *
	 * @param half
	 *            half the width of the opening
	 * @param springline
	 *            the height over the ground where the arch starts to bend
	 */
	void archway(double posX, double posZ, boolean alongX, double length, double thickness, double ground, double high, double half, double springline) {
		double tip = springline + Math.sqrt(3) * half;
		for (int slice = 0; slice < SLICES; slice++) {
			double low = tip * slice / SLICES;
			double above = tip * (slice + 1) / SLICES;
			double opening = archHalfWidth((low + above) / 2 - springline, half);
			double side = length / 2 - opening;
			double offset = (length / 2 + opening) / 2;
			this.piece(posX, posZ, alongX, -offset, side, ground - (low + above) / 2, above - low, thickness);
			this.piece(posX, posZ, alongX, offset, side, ground - (low + above) / 2, above - low, thickness);
		}
		this.piece(posX, posZ, alongX, 0, length, ground - (tip + high) / 2, high - tip, thickness);
	}

	/** One block of a wall: its offset along the wall from the middle, its length, its height and its thickness. */
	private void piece(double posX, double posZ, boolean alongX, double offset, double length, double centreY, double height, double thickness) {
		if (alongX) {
			this.stone.box(posX + offset, centreY, posZ, length, height, thickness);
		} else {
			this.stone.box(posX, centreY, posZ + offset, thickness, height, length);
		}
	}

	/** The merlons of a parapet along a wall that runs along x (at the z given) or along z (at the x given): blocks with gaps between them. */
	void merlons(boolean alongX, double fixed, double from, double toValue, double topY, double thickness) {
		for (double along = from + MERLON; along < toValue - MERLON / 2; along += 2 * MERLON) {
			if (alongX) {
				this.stone.box(along, topY - MERLON / 2, fixed, MERLON, MERLON, thickness);
			} else {
				this.stone.box(fixed, topY - MERLON / 2, along, thickness, MERLON, MERLON);
			}
		}
	}
}
