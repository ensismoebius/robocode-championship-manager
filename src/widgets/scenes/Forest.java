package widgets.scenes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Trees grown by a recursive (fractal) rule: a crooked trunk that forks into
 * branches, each branch forking again into smaller ones, a bit shorter and
 * thinner each time, with a clump of leaves at the tips. Everything goes into
 * two meshes (bark and leaves), however many trees there are.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class Forest {

	private static final int SIDES = 6;
	/** Units of the world one tile of the bark texture covers */
	private static final double BARK_TILE = 90;

	/** How many times a branch forks: trunk, branches, twigs, tips */
	static final int DEPTH = 3;

	final TriangleMesh bark = new TriangleMesh();
	final TriangleMesh leaves = new TriangleMesh();

	// A unit sphere with few faces, the shape of a clump of leaves
	private static final double[][] BLOB_POINTS;
	private static final int[][] BLOB_FACES;
	static {
		double t = (1 + Math.sqrt(5)) / 2;
		List<double[]> points = new ArrayList<double[]>(List.of(new double[] { -1, t, 0 }, new double[] { 1, t, 0 }, new double[] { -1, -t, 0 }, new double[] { 1, -t, 0 },
				new double[] { 0, -1, t }, new double[] { 0, 1, t }, new double[] { 0, -1, -t }, new double[] { 0, 1, -t }, new double[] { t, 0, -1 }, new double[] { t, 0, 1 },
				new double[] { -t, 0, -1 }, new double[] { -t, 0, 1 }));
		points.replaceAll(Forest::normalize);
		int[][] faces = { { 0, 11, 5 }, { 0, 5, 1 }, { 0, 1, 7 }, { 0, 7, 10 }, { 0, 10, 11 }, { 1, 5, 9 }, { 5, 11, 4 }, { 11, 10, 2 }, { 10, 7, 6 }, { 7, 1, 8 }, { 3, 9, 4 },
				{ 3, 4, 2 }, { 3, 2, 6 }, { 3, 6, 8 }, { 3, 8, 9 }, { 4, 9, 5 }, { 2, 4, 11 }, { 6, 2, 10 }, { 8, 6, 7 }, { 9, 8, 1 } };
		// One subdivision: every face in four
		Map<Long, Integer> middles = new HashMap<Long, Integer>();
		List<int[]> divided = new ArrayList<int[]>();
		for (int[] face : faces) {
			int a = middle(points, middles, face[0], face[1]);
			int b = middle(points, middles, face[1], face[2]);
			int c = middle(points, middles, face[2], face[0]);
			divided.add(new int[] { face[0], a, c });
			divided.add(new int[] { face[1], b, a });
			divided.add(new int[] { face[2], c, b });
			divided.add(new int[] { a, b, c });
		}
		BLOB_POINTS = points.toArray(new double[0][]);
		BLOB_FACES = divided.toArray(new int[0][]);
	}

	private static int middle(List<double[]> points, Map<Long, Integer> cache, int a, int b) {
		long key = ((long) Math.min(a, b) << 32) | Math.max(a, b);
		Integer found = cache.get(key);
		if (found != null) {
			return found;
		}
		double[] p = points.get(a);
		double[] q = points.get(b);
		points.add(normalize(new double[] { (p[0] + q[0]) / 2, (p[1] + q[1]) / 2, (p[2] + q[2]) / 2 }));
		cache.put(key, points.size() - 1);
		return points.size() - 1;
	}

	/**
	 * Grows a tree with its base at x, ground, z
	 *
	 * @param ground
	 *            the y of the ground (it grows to negative y)
	 * @param size
	 *            1 for a tree of about 400 units
	 */
	void addTree(double x, double ground, double z, double size, Random random) {
		double lean = 0.22;
		double[] direction = normalize(new double[] { (random.nextDouble() - 0.5) * lean * 2, -1, (random.nextDouble() - 0.5) * lean * 2 });
		double radius = (17 + random.nextDouble() * 9) * size;
		grow(new double[] { x, ground + 10, z }, direction, radius, (330 + random.nextDouble() * 140) * size, 0, random);
		// Roots spreading over the ground and going into it
		int roots = 4 + random.nextInt(2);
		double turn = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < roots; i++) {
			double angle = turn + i * Math.PI * 2 / roots + (random.nextDouble() - 0.5) * 0.6;
			double dx = Math.cos(angle);
			double dz = Math.sin(angle);
			double reach = (80 + random.nextDouble() * 60) * size;
			List<double[]> path = List.of(new double[] { x, ground - 50 * size, z }, new double[] { x + dx * reach * 0.3, ground - 30 * size, z + dz * reach * 0.3 },
					new double[] { x + dx * reach * 0.65, ground - 11 * size, z + dz * reach * 0.65 }, new double[] { x + dx * reach, ground + 6, z + dz * reach });
			tube(path, new double[] { radius * 0.55, radius * 0.38, radius * 0.24, radius * 0.1 });
		}
	}

	/**
	 * One branch and, recursively, its branches
	 */
	private void grow(double[] start, double[] startDirection, double radius, double length, int depth, Random random) {
		int steps = Math.max(3, (int) (length / 42));
		List<double[]> path = new ArrayList<double[]>();
		List<double[]> directions = new ArrayList<double[]>();
		double[] radii = new double[steps + 1];
		double[] position = start.clone();
		double[] direction = startDirection.clone();
		// The trunk bends a lot and always looks for the light; the thin branches
		// bend less and, the thinner, the more they droop
		double crookedness = depth == 0 ? 0.30 : 0.18;
		double up = depth == 0 ? 0.20 : depth == 1 ? 0.10 : 0.02;
		double droop = depth >= 2 ? 0.08 : 0;
		double taper = depth == 0 ? 0.6 : 0.75;
		for (int i = 0; i <= steps; i++) {
			double t = (double) i / steps;
			// The foot of the trunk flares
			radii[i] = radius * (1 - taper * t) * (depth == 0 ? 1 + 0.8 * Math.exp(-8 * t) : 1);
			path.add(position.clone());
			directions.add(direction.clone());
			if (i < steps) {
				direction = normalize(new double[] { direction[0] + (random.nextDouble() - 0.5) * 2 * crookedness, direction[1] - up + droop + (random.nextDouble() - 0.5) * crookedness * 0.6,
						direction[2] + (random.nextDouble() - 0.5) * 2 * crookedness });
				double step = length / steps;
				position = new double[] { position[0] + direction[0] * step, position[1] + direction[1] * step, position[2] + direction[2] * step };
			}
		}
		tube(path, radii);

		if (depth >= DEPTH) {
			// A clump of leaves at the tip, and now and then a smaller one hanging from
			// the middle (every clump is many triangles, and there are many trees)
			double[] tip = path.get(steps);
			blob(tip, (65 + random.nextDouble() * 45) * Math.min(1.4, 0.45 + length / 120), random);
			if (random.nextDouble() < 0.35) {
				double[] middle = path.get(steps / 2);
				blob(new double[] { middle[0], middle[1] + 8, middle[2] }, 30 + random.nextDouble() * 22, random);
			}
			return;
		}
		int forks = depth == 0 ? 3 + random.nextInt(2) : 2 + random.nextInt(2);
		for (int k = 0; k < forks; k++) {
			// The branches start along the upper part of this one
			double t = 0.38 + 0.62 * (k + random.nextDouble() * 0.6) / forks;
			int index = Math.max(1, Math.min(steps, (int) Math.round(t * steps)));
			double[] along = directions.get(index);
			double[] axis = normalize(cross(along, new double[] { random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5 }));
			double angle = Math.toRadians(28 + random.nextDouble() * 30);
			double[] child = rotate(along, axis, angle);
			// And they look up a bit
			child = normalize(new double[] { child[0], child[1] - 0.18, child[2] });
			grow(path.get(index), child, radii[index] * (0.55 + random.nextDouble() * 0.15), length * (0.55 + random.nextDouble() * 0.2), depth + 1, random);
		}
	}

	/**
	 * A tube along the points, with the radius of each one
	 */
	private void tube(List<double[]> path, double[] radii) {
		int base = bark.getPoints().size() / 3;
		double length = 0;
		int count = path.size();
		for (int i = 0; i < count; i++) {
			double[] before = path.get(Math.max(0, i - 1));
			double[] after = path.get(Math.min(count - 1, i + 1));
			double[] direction = normalize(new double[] { after[0] - before[0], after[1] - before[1], after[2] - before[2] });
			double[] reference = Math.abs(direction[1]) < 0.9 ? new double[] { 0, 1, 0 } : new double[] { 1, 0, 0 };
			double[] u = normalize(cross(direction, reference));
			double[] v = cross(direction, u);
			if (i > 0) {
				double[] previous = path.get(i - 1);
				length += Math.sqrt(Math.pow(path.get(i)[0] - previous[0], 2) + Math.pow(path.get(i)[1] - previous[1], 2) + Math.pow(path.get(i)[2] - previous[2], 2));
			}
			double[] center = path.get(i);
			for (int k = 0; k <= SIDES; k++) {
				double angle = Math.PI * 2 * k / SIDES;
				double cos = Math.cos(angle) * radii[i];
				double sin = Math.sin(angle) * radii[i];
				bark.getPoints().addAll((float) (center[0] + u[0] * cos + v[0] * sin), (float) (center[1] + u[1] * cos + v[1] * sin), (float) (center[2] + u[2] * cos + v[2] * sin));
				bark.getTexCoords().addAll((float) k / SIDES * 2, (float) (length / BARK_TILE));
			}
		}
		for (int i = 0; i < count - 1; i++) {
			for (int k = 0; k < SIDES; k++) {
				int a = base + i * (SIDES + 1) + k;
				int b = a + 1;
				int c = a + SIDES + 2;
				int d = a + SIDES + 1;
				bark.getFaces().addAll(a, a, b, b, c, c, a, a, c, c, d, d);
			}
		}
	}

	/**
	 * A clump of leaves: a flattened lumpy sphere
	 */
	private void blob(double[] center, double radius, Random random) {
		int base = leaves.getPoints().size() / 3;
		for (double[] point : BLOB_POINTS) {
			double bump = 0.82 + random.nextDouble() * 0.36;
			leaves.getPoints().addAll((float) (center[0] + point[0] * radius * bump), (float) (center[1] + point[1] * radius * 0.62 * bump), (float) (center[2] + point[2] * radius * bump));
			leaves.getTexCoords().addAll((float) (0.5 + 0.5 * point[0]) * 2, (float) (0.5 + 0.5 * point[2]) * 2);
		}
		for (int[] face : BLOB_FACES) {
			leaves.getFaces().addAll(base + face[0], base + face[0], base + face[1], base + face[1], base + face[2], base + face[2]);
		}
	}

	private static double[] normalize(double[] v) {
		double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
		return length == 0 ? new double[] { 0, -1, 0 } : new double[] { v[0] / length, v[1] / length, v[2] / length };
	}

	private static double[] cross(double[] a, double[] b) {
		return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
	}

	/**
	 * Rodrigues: v turned around the axis (unit) by the angle
	 */
	private static double[] rotate(double[] v, double[] axis, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double dot = v[0] * axis[0] + v[1] * axis[1] + v[2] * axis[2];
		double[] cross = cross(axis, v);
		return new double[] { v[0] * cos + cross[0] * sin + axis[0] * dot * (1 - cos), v[1] * cos + cross[1] * sin + axis[1] * dot * (1 - cos), v[2] * cos + cross[2] * sin + axis[2] * dot * (1 - cos) };
	}
}
