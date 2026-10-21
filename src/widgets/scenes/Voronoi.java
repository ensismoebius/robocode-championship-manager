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

	/** The diagram is static: this class is not instantiated. */
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
		for (int index = 0; index < seeds.length; index++) {
			double gap = gaps[index];
			List<double[]> polygon = new ArrayList<double[]>(List.of( //
					new double[] { minX + gap, minZ + gap }, new double[] { maxX - gap, minZ + gap }, //
					new double[] { maxX - gap, maxZ - gap }, new double[] { minX + gap, maxZ - gap }));
			double[] seed = seeds[index];
			for (int innerIndex = 0; innerIndex < seeds.length && !polygon.isEmpty(); innerIndex++) {
				if (innerIndex == index) {
					continue;
				}
				// Keeps the side of the bisector closer to this seed, moved back by the gap
				double deltaX = seeds[innerIndex][0] - seed[0];
				double deltaZ = seeds[innerIndex][1] - seed[1];
				double length = Math.hypot(deltaX, deltaZ);
				if (length == 0) {
					continue;
				}
				double midX = (seed[0] + seeds[innerIndex][0]) / 2;
				double midZ = (seed[1] + seeds[innerIndex][1]) / 2;
				polygon = clip(polygon, deltaX, deltaZ, midX * deltaX + midZ * deltaZ - gap * length);
			}
			cells.add(polygon.toArray(new double[0][]));
		}
		return cells;
	}

	/**
	 * Sutherland–Hodgman: the part of a convex polygon where x·dx + z·dz <= limit
	 */
	private static List<double[]> clip(List<double[]> polygon, double deltaX, double deltaZ, double limit) {
		List<double[]> result = new ArrayList<double[]>(polygon.size() + 1);
		for (int deepIndex = 0; deepIndex < polygon.size(); deepIndex++) {
			double[] first = polygon.get(deepIndex);
			double[] second = polygon.get((deepIndex + 1) % polygon.size());
			double daValue = first[0] * deltaX + first[1] * deltaZ - limit;
			double dbValue = second[0] * deltaX + second[1] * deltaZ - limit;
			if (daValue <= 0) {
				result.add(first);
			}
			if ((daValue <= 0) != (dbValue <= 0)) {
				double time = daValue / (daValue - dbValue);
				result.add(new double[] { first[0] + (second[0] - first[0]) * time, first[1] + (second[1] - first[1]) * time });
			}
		}
		return result;
	}
}
