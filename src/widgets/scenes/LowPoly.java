package widgets.scenes;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Builds flat shaded, low poly solids (convex ones) into one mesh: prisms
 * extruded from a profile, tapered slabs, boxes and cylinders. Every face has its
 * own points, so the light shows each facet. Many parts, one node.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class LowPoly {

	final TriangleMesh mesh = new TriangleMesh();

	LowPoly() {
		this.mesh.getTexCoords().addAll(0, 0);
	}

	/**
	 * The mesh as a node with the material
	 */
	MeshView view(PhongMaterial material) {
		MeshView view = new MeshView(this.mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.BACK);
		return view;
	}

	/**
	 * A solid between two polygons with the same number of points: sides between
	 * them and a cap on each
	 */
	void solid(double[][] bottom, double[][] top) {
		int n = bottom.length;
		double[] center = new double[3];
		for (double[][] polygon : new double[][][] { bottom, top }) {
			for (double[] p : polygon) {
				for (int k = 0; k < 3; k++) {
					center[k] += p[k] / (2 * n);
				}
			}
		}
		for (int i = 1; i < n - 1; i++) {
			this.triangle(bottom[0], bottom[i], bottom[i + 1], center);
			this.triangle(top[0], top[i], top[i + 1], center);
		}
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			this.triangle(bottom[i], bottom[j], top[j], center);
			this.triangle(bottom[i], top[j], top[i], center);
		}
	}

	/**
	 * A triangle facing away from the center (JavaFX takes the counter clockwise
	 * side as the front)
	 */
	private void triangle(double[] a, double[] b, double[] c, double[] center) {
		double[] u = { b[0] - a[0], b[1] - a[1], b[2] - a[2] };
		double[] v = { c[0] - a[0], c[1] - a[1], c[2] - a[2] };
		double[] normal = { u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0] };
		double dot = 0;
		for (int k = 0; k < 3; k++) {
			dot += normal[k] * ((a[k] + b[k] + c[k]) / 3 - center[k]);
		}
		if (dot < 0) {
			double[] swap = b;
			b = c;
			c = swap;
		}
		int base = this.mesh.getPoints().size() / 3;
		this.mesh.getPoints().addAll((float) a[0], (float) a[1], (float) a[2], (float) b[0], (float) b[1], (float) b[2], (float) c[0], (float) c[1], (float) c[2]);
		this.mesh.getFaces().addAll(base, 0, base + 1, 0, base + 2, 0);
	}

	void box(double cx, double cy, double cz, double sx, double sy, double sz) {
		this.frustum(new double[][] { { cx - sx / 2, cz - sz / 2 }, { cx + sx / 2, cz - sz / 2 }, { cx + sx / 2, cz + sz / 2 }, { cx - sx / 2, cz + sz / 2 } }, cy + sy / 2, cy - sy / 2, 1);
	}

	/**
	 * A polygon seen from above {x, z}, from y0 up to y1, the top scaled around
	 * the middle: sloped sides
	 */
	void frustum(double[][] outline, double y0, double y1, double topScale) {
		int n = outline.length;
		double cx = 0;
		double cz = 0;
		for (double[] p : outline) {
			cx += p[0] / n;
			cz += p[1] / n;
		}
		double[][] bottom = new double[n][];
		double[][] top = new double[n][];
		for (int i = 0; i < n; i++) {
			bottom[i] = new double[] { outline[i][0], y0, outline[i][1] };
			top[i] = new double[] { cx + (outline[i][0] - cx) * topScale, y1, cz + (outline[i][1] - cz) * topScale };
		}
		this.solid(bottom, top);
	}

	/**
	 * A profile {z, y} pushed along x
	 */
	void prismZY(double[][] profile, double x0, double x1) {
		int n = profile.length;
		double[][] a = new double[n][];
		double[][] b = new double[n][];
		for (int i = 0; i < n; i++) {
			a[i] = new double[] { x0, profile[i][1], profile[i][0] };
			b[i] = new double[] { x1, profile[i][1], profile[i][0] };
		}
		this.solid(a, b);
	}

	/**
	 * A profile {x, y} pushed along z
	 */
	void prismXY(double[][] profile, double z0, double z1) {
		int n = profile.length;
		double[][] a = new double[n][];
		double[][] b = new double[n][];
		for (int i = 0; i < n; i++) {
			a[i] = new double[] { profile[i][0], profile[i][1], z0 };
			b[i] = new double[] { profile[i][0], profile[i][1], z1 };
		}
		this.solid(a, b);
	}

	/**
	 * A hexagon in the z, y plane, pushed along x: a plate of armor
	 */
	void hexPlate(double z, double y, double radiusZ, double radiusY, double x0, double x1) {
		double[][] hexagon = new double[6][];
		for (int i = 0; i < 6; i++) {
			hexagon[i] = new double[] { z + radiusZ * Math.cos(Math.PI / 3 * i), y + radiusY * Math.sin(Math.PI / 3 * i) };
		}
		this.prismZY(hexagon, x0, x1);
	}

	/**
	 * A vertical cylinder
	 */
	void cylinderY(double cx, double cz, double radius, double y0, double y1, int sides) {
		double[][] bottom = new double[sides][];
		double[][] top = new double[sides][];
		for (int i = 0; i < sides; i++) {
			double x = cx + radius * Math.cos(Math.PI * 2 * i / sides);
			double z = cz + radius * Math.sin(Math.PI * 2 * i / sides);
			bottom[i] = new double[] { x, y0, z };
			top[i] = new double[] { x, y1, z };
		}
		this.solid(bottom, top);
	}

	/**
	 * A cylinder along z, like a gun barrel
	 */
	void cylinderZ(double cx, double cy, double radius, double z0, double z1, int sides) {
		double[][] profile = new double[sides][];
		for (int i = 0; i < sides; i++) {
			profile[i] = new double[] { cx + radius * Math.cos(Math.PI * 2 * i / sides), cy + radius * Math.sin(Math.PI * 2 * i / sides) };
		}
		this.prismXY(profile, z0, z1);
	}

	/**
	 * A spindle along z, pointed at both ends: a hexagon ring in the middle (at z)
	 * and a tip in front and another behind
	 */
	void spindle(double cx, double cy, double z, double radius, double front, double back, int sides) {
		double[][] ring = new double[sides][];
		double[][] nose = new double[sides][];
		double[][] tail = new double[sides][];
		for (int i = 0; i < sides; i++) {
			ring[i] = new double[] { cx + radius * Math.cos(Math.PI * 2 * i / sides), cy + radius * Math.sin(Math.PI * 2 * i / sides), z };
			nose[i] = new double[] { cx, cy, z + front };
			tail[i] = new double[] { cx, cy, z - back };
		}
		this.solid(ring, nose);
		this.solid(ring, tail);
	}

	/**
	 * A thin tube from a to b, like a segment of a lightning bolt
	 */
	void tube(double[] a, double[] b, double radius, int sides) {
		double[] direction = { b[0] - a[0], b[1] - a[1], b[2] - a[2] };
		double length = Math.sqrt(direction[0] * direction[0] + direction[1] * direction[1] + direction[2] * direction[2]);
		if (length < 1e-6) {
			return;
		}
		for (int k = 0; k < 3; k++) {
			direction[k] /= length;
		}
		double[] reference = Math.abs(direction[1]) < 0.9 ? new double[] { 0, 1, 0 } : new double[] { 1, 0, 0 };
		double[] u = { direction[1] * reference[2] - direction[2] * reference[1], direction[2] * reference[0] - direction[0] * reference[2], direction[0] * reference[1] - direction[1] * reference[0] };
		double size = Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
		for (int k = 0; k < 3; k++) {
			u[k] /= size;
		}
		double[] v = { direction[1] * u[2] - direction[2] * u[1], direction[2] * u[0] - direction[0] * u[2], direction[0] * u[1] - direction[1] * u[0] };
		double[][] bottom = new double[sides][3];
		double[][] top = new double[sides][3];
		for (int i = 0; i < sides; i++) {
			double cos = Math.cos(Math.PI * 2 * i / sides) * radius;
			double sin = Math.sin(Math.PI * 2 * i / sides) * radius;
			for (int k = 0; k < 3; k++) {
				double offset = u[k] * cos + v[k] * sin;
				bottom[i][k] = a[k] + offset;
				top[i][k] = b[k] + offset;
			}
		}
		this.solid(bottom, top);
	}

	/**
	 * A flat ring on the floor plane, from y0 to y1
	 */
	void ring(double inner, double outer, double y0, double y1, int segments) {
		for (int i = 0; i < segments; i++) {
			double a = Math.PI * 2 * i / segments;
			double b = Math.PI * 2 * (i + 1) / segments;
			this.frustum(new double[][] { { inner * Math.cos(a), inner * Math.sin(a) }, { outer * Math.cos(a), outer * Math.sin(a) }, { outer * Math.cos(b), outer * Math.sin(b) },
					{ inner * Math.cos(b), inner * Math.sin(b) } }, y0, y1, 1);
		}
	}
}
