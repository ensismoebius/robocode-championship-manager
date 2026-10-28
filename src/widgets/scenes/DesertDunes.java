package widgets.scenes;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The ground of the desert arena beyond its courtyard: a plain of sand, flat where the creatures walk, and beyond it the
 * dunes, rising slowly to the horizon with the ripples of the wind on them. y grows downwards, so a dune goes to
 * negative y.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertDunes {

	/** The plain lies this far below the mosaic of the courtyard */
	static final double PLAIN = 6;
	/** The plain goes this far beyond the courtyard, then the dunes rise over the next distance */
	private static final double FLAT = 420;
	private static final double RISE = 380;
	/** The ground is cut in pieces: fine near the arena, coarser far away, out to the horizon */
	private static final double NEAR = 1500;
	private static final double FAR = 6000;
	private static final double FINE = 80;
	private static final double COARSE = 250;
	/** The sand repeats its picture every this many units */
	private static final double SAND_TILE = 240;

	/** The ground is static: this class is not instantiated. */
	private DesertDunes() {
	}

	/** The sites of the monuments, a mosque or a gate on each: {x, z, radius of the levelled ground} */
	static final double[][] SITES = { { -1250, 780, 300 }, { 1350, -850, 260 }, { -250, -1500, 300 }, { 900, 1450, 260 } };
	/** The levelled ground of a site goes back to the dunes over this distance */
	private static final double TERRACE = 220;

	/** How high the ground is at a point, in units: nothing on the plain and in the courtyard, the dunes beyond them, and level where a monument stands. */
	static double height(double posX, double posZ, double width, double depth) {
		double ground = dunes(posX, posZ, width, depth);
		for (double[] site : SITES) {
			double away = Math.hypot(posX - site[0], posZ - site[1]);
			if (away < site[2] + TERRACE) {
				double share = Math.max(0, (away - site[2]) / TERRACE);
				double level = dunes(site[0], site[1], width, depth);
				ground = level + (ground - level) * share * share * (3 - 2 * share);
			}
		}
		return ground;
	}

	/** How high the dunes alone are at a point of the ground, in units: nothing on the plain and in the courtyard. */
	private static double dunes(double posX, double posZ, double width, double depth) {
		double outside = Math.max(Math.abs(posX) - width / 2, Math.abs(posZ) - depth / 2);
		double rise = Math.max(0, Math.min(1, (outside - FLAT) / RISE));
		double eased = rise * rise * (3 - 2 * rise);
		double waves = 0.5 + 0.3 * Math.sin(posX * 0.0041 + posZ * 0.0023 + 0.7) + 0.2 * Math.sin(posZ * 0.0057 - posX * 0.0013 + 2.1);
		double ripples = 0.5 + 0.5 * Math.sin(posX * 0.021 + posZ * 0.013);
		return eased * (260 * waves + 8 * ripples);
	}

	/** The positions of the cuts of the ground along an axis: fine near the arena, coarser far away. */
	private static double[] cuts() {
		List<Double> positions = new ArrayList<>();
		for (double position = -FAR; position < -NEAR; position += COARSE) {
			positions.add(position);
		}
		for (double position = -NEAR; position <= NEAR; position += FINE) {
			positions.add(position);
		}
		for (double position = NEAR + COARSE; position <= FAR; position += COARSE) {
			positions.add(position);
		}
		return positions.stream().mapToDouble(Double::doubleValue).toArray();
	}

	/** The corner of the ground above a point of the plane, at its height */
	private static double[] corner(double posX, double posZ, double width, double depth) {
		return new double[] { posX, PLAIN - height(posX, posZ, width, depth), posZ };
	}

	/** Builds the plain and the dunes around the courtyard, in the host scene. */
	static void build(BattleArena3D host, double width, double depth) {
		double[] cuts = cuts();
		SurfaceMesh ground = new SurfaceMesh();
		for (int column = 0; column < cuts.length - 1; column++) {
			for (int row = 0; row < cuts.length - 1; row++) {
				double firstX = cuts[column];
				double secondX = cuts[column + 1];
				double firstZ = cuts[row];
				double secondZ = cuts[row + 1];
				double[] northwest = corner(firstX, firstZ, width, depth);
				double[] northeast = corner(secondX, firstZ, width, depth);
				double[] southeast = corner(secondX, secondZ, width, depth);
				double[] southwest = corner(firstX, secondZ, width, depth);
				float[] coords = { (float) (firstX / SAND_TILE), (float) (firstZ / SAND_TILE), (float) (secondX / SAND_TILE), (float) (firstZ / SAND_TILE), (float) (secondX / SAND_TILE), (float) (secondZ / SAND_TILE),
						(float) (firstX / SAND_TILE), (float) (secondZ / SAND_TILE) };
				ground.quad(northwest, northeast, southeast, southwest, coords, slope((firstX + secondX) / 2, (firstZ + secondZ) / 2, width, depth));
			}
		}
		host.arena.getChildren().add(ground.view(ArenaMaterials.textured(DesertTexture.SAND, Color.gray(0.05))));
	}

	/**
	 * The way a piece of the ground faces, up and out of the slope, for the light: the surface goes down where the dunes
	 * rise, so the normal leans against the rise
	 */
	private static double[] slope(double centreX, double centreZ, double width, double depth) {
		double step = 10;
		double slopeX = (height(centreX + step, centreZ, width, depth) - height(centreX - step, centreZ, width, depth)) / (2 * step);
		double slopeZ = (height(centreX, centreZ + step, width, depth) - height(centreX, centreZ - step, width, depth)) / (2 * step);
		return new double[] { -slopeX, -1, -slopeZ };
	}
}
