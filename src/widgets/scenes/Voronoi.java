package widgets.scenes;

import java.util.ArrayList;
import java.util.List;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Voronoi cells in a rectangle: the region closer to each seed than to any
 * other. Each cell is shrunk by a gap, so there is space between them, like
 * the cracks between basalt columns.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class Voronoi {

	private Voronoi() {
	}

	/**
	 * @param seeds
	 *            points {x, z}
	 * @param gaps
	 *            how far each cell stays from the border with its neighbors and
	 *            from the rectangle
	 * @return the polygon {x, z} of each seed, in order around it (convex), maybe
	 *         empty if the gap eats the whole cell
	 */
	static List<double[][]> cells(double[][] seeds, double[] gaps, double minX, double minZ, double maxX, double maxZ) {
		List<double[][]> cells = new ArrayList<double[][]>(seeds.length);
		for (int i = 0; i < seeds.length; i++) {
			double gap = gaps[i];
			List<double[]> polygon = new ArrayList<double[]>(List.of( //
					new double[] { minX + gap, minZ + gap }, new double[] { maxX - gap, minZ + gap }, //
					new double[] { maxX - gap, maxZ - gap }, new double[] { minX + gap, maxZ - gap }));
			double[] seed = seeds[i];
			for (int j = 0; j < seeds.length && !polygon.isEmpty(); j++) {
				if (j == i) {
					continue;
				}
				// Keeps the side of the bisector closer to this seed, moved back by the gap
				double dx = seeds[j][0] - seed[0];
				double dz = seeds[j][1] - seed[1];
				double length = Math.hypot(dx, dz);
				if (length == 0) {
					continue;
				}
				double midX = (seed[0] + seeds[j][0]) / 2;
				double midZ = (seed[1] + seeds[j][1]) / 2;
				polygon = clip(polygon, dx, dz, midX * dx + midZ * dz - gap * length);
			}
			cells.add(polygon.toArray(new double[0][]));
		}
		return cells;
	}

	/**
	 * Sutherland–Hodgman: the part of a convex polygon where x·dx + z·dz <= limit
	 */
	private static List<double[]> clip(List<double[]> polygon, double dx, double dz, double limit) {
		List<double[]> result = new ArrayList<double[]>(polygon.size() + 1);
		for (int k = 0; k < polygon.size(); k++) {
			double[] a = polygon.get(k);
			double[] b = polygon.get((k + 1) % polygon.size());
			double da = a[0] * dx + a[1] * dz - limit;
			double db = b[0] * dx + b[1] * dz - limit;
			if (da <= 0) {
				result.add(a);
			}
			if ((da <= 0) != (db <= 0)) {
				double t = da / (da - db);
				result.add(new double[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t });
			}
		}
		return result;
	}
}
