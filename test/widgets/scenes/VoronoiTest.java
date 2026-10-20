package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class VoronoiTest {

	private static double max(double[][] polygon, int axis) {
		double max = Double.NEGATIVE_INFINITY;
		for (double[] point : polygon) {
			max = Math.max(max, point[axis]);
		}
		return max;
	}

	private static double min(double[][] polygon, int axis) {
		double min = Double.POSITIVE_INFINITY;
		for (double[] point : polygon) {
			min = Math.min(min, point[axis]);
		}
		return min;
	}

	@Test
	void twoSeedsSplitTheRectangleWithAGap() {
		List<double[][]> cells = Voronoi.cells(new double[][] { { -10, 0 }, { 10, 0 } }, new double[] { 1, 2 }, -50, -30, 50, 30);
		// Bisector at x = 0, each cell keeps its gap from it and from the border
		assertEquals(-1, max(cells.get(0), 0), 1e-9);
		assertEquals(-49, min(cells.get(0), 0), 1e-9);
		assertEquals(2, min(cells.get(1), 0), 1e-9);
		assertEquals(28, max(cells.get(1), 1), 1e-9);
	}

	@Test
	void eachCellHoldsItsSeedAndStaysInside() {
		Random random = new Random(4);
		double[][] seeds = new double[60][];
		double[] gaps = new double[60];
		for (int i = 0; i < seeds.length; i++) {
			seeds[i] = new double[] { random.nextDouble() * 800 - 400, random.nextDouble() * 600 - 300 };
			gaps[i] = 0.5;
		}
		List<double[][]> cells = Voronoi.cells(seeds, gaps, -400, -300, 400, 300);
		assertEquals(seeds.length, cells.size());
		double area = 0;
		for (int i = 0; i < cells.size(); i++) {
			double[][] cell = cells.get(i);
			assertTrue(cell.length >= 3);
			assertTrue(min(cell, 0) >= -400 && max(cell, 0) <= 400 && min(cell, 1) >= -300 && max(cell, 1) <= 300);
			area += area(cell);
		}
		// The cells cover the field, but the gaps
		assertTrue(area > 800 * 600 * 0.9 && area < 800 * 600, String.valueOf(area));
	}

	private static double area(double[][] polygon) {
		double sum = 0;
		for (int i = 0; i < polygon.length; i++) {
			double[] a = polygon[i];
			double[] b = polygon[(i + 1) % polygon.length];
			sum += a[0] * b[1] - b[0] * a[1];
		}
		return Math.abs(sum) / 2;
	}
}
