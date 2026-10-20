package widgets.scenes;

import java.util.Random;

import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Geometry of the neon arena, many pieces merged in few meshes: the towers of
 * the skyline with their neon trims, and lines on the ground. y grows
 * downwards, so a tower goes to negative y.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class NeonCity {

	/** Walls of the towers, with the texture of the windows */
	final TriangleMesh facades = new TriangleMesh();
	/** Neon trims on the towers, in two colors */
	final TriangleMesh cyan = new TriangleMesh();
	final TriangleMesh magenta = new TriangleMesh();

	NeonCity() {
		this.facades.getTexCoords().addAll(0, 0);
	}

	/**
	 * A box in a mesh without a texture (it uses only the first texture point),
	 * the mesh must start with a texture point
	 */
	static void box(TriangleMesh mesh, double cx, double cy, double cz, double sx, double sy, double sz) {
		if (mesh.getTexCoords().size() == 0) {
			mesh.getTexCoords().addAll(0, 0);
		}
		int base = mesh.getPoints().size() / 3;
		for (int i = 0; i < 8; i++) {
			mesh.getPoints().addAll((float) (cx + ((i & 1) == 0 ? -sx : sx) / 2), (float) (cy + ((i & 2) == 0 ? -sy : sy) / 2), (float) (cz + ((i & 4) == 0 ? -sz : sz) / 2));
		}
		int[][] quads = { { 0, 1, 3, 2 }, { 4, 6, 7, 5 }, { 0, 2, 6, 4 }, { 1, 5, 7, 3 }, { 0, 4, 5, 1 }, { 2, 3, 7, 6 } };
		for (int[] q : quads) {
			mesh.getFaces().addAll(base + q[0], 0, base + q[1], 0, base + q[2], 0, base + q[0], 0, base + q[2], 0, base + q[3], 0);
		}
	}

	/**
	 * A tower with its base on the ground at x, z
	 */
	void addTower(double x, double z, double width, double depth, double height, Random random) {
		double ground = 6;
		double[][] corners = { { x - width / 2, z - depth / 2 }, { x + width / 2, z - depth / 2 }, { x + width / 2, z + depth / 2 }, { x - width / 2, z + depth / 2 } };
		float offsetU = (float) random.nextDouble();
		float offsetV = (float) random.nextDouble();
		for (int side = 0; side < 4; side++) {
			double[] a = corners[side];
			double[] b = corners[(side + 1) % 4];
			double length = Math.hypot(b[0] - a[0], b[1] - a[1]);
			float u = (float) (length / NeonTexture.FACADE_WIDTH);
			float v = (float) (height / NeonTexture.FACADE_HEIGHT);
			int base = this.facades.getPoints().size() / 3;
			this.facades.getPoints().addAll((float) a[0], (float) ground, (float) a[1], (float) b[0], (float) ground, (float) b[1], (float) b[0], (float) (ground - height), (float) b[1],
					(float) a[0], (float) (ground - height), (float) a[1]);
			int tex = this.facades.getTexCoords().size() / 2;
			// The top of the texture is at the top of the tower
			this.facades.getTexCoords().addAll(offsetU, offsetV + v, offsetU + u, offsetV + v, offsetU + u, offsetV, offsetU, offsetV);
			this.facades.getFaces().addAll(base, tex, base + 1, tex + 1, base + 2, tex + 2, base, tex, base + 2, tex + 2, base + 3, tex + 3);
		}
		TriangleMesh trim = random.nextBoolean() ? this.cyan : this.magenta;
		double top = ground - height;
		// A neon crown around the top
		box(trim, x, top, z - depth / 2, width + 6, 7, 6);
		box(trim, x, top, z + depth / 2, width + 6, 7, 6);
		box(trim, x - width / 2, top, z, 6, 7, depth + 6);
		box(trim, x + width / 2, top, z, 6, 7, depth + 6);
		// Tall towers also have glowing edges, and some an antenna
		if (height > 700) {
			for (double[] corner : corners) {
				box(trim, corner[0], ground - height / 2, corner[1], 5, height, 5);
			}
		}
		if (random.nextDouble() < 0.35) {
			double antenna = 80 + random.nextDouble() * 160;
			box(trim, x, top - antenna / 2, z, 6, antenna, 6);
		}
	}

	/**
	 * Towers around the field, far away
	 */
	void skyline(int count, Random random) {
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = 2300 + random.nextDouble() * 2000;
			double height = 250 + Math.pow(random.nextDouble(), 2.2) * 1700;
			this.addTower(Math.cos(angle) * distance * 1.2, Math.sin(angle) * distance, 150 + random.nextDouble() * 260, 150 + random.nextDouble() * 260, height, random);
		}
	}
}
