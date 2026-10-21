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
		int count = bottom.length;
		double[] center = new double[3];
		for (double[][] polygon : new double[][][] { bottom, top }) {
			for (double[] point : polygon) {
				for (int deepIndex = 0; deepIndex < 3; deepIndex++) {
					center[deepIndex] += point[deepIndex] / (2 * count);
				}
			}
		}
		for (int index = 1; index < count - 1; index++) {
			this.triangle(bottom[0], bottom[index], bottom[index + 1], center);
			this.triangle(top[0], top[index], top[index + 1], center);
		}
		for (int index2 = 0; index2 < count; index2++) {
			int innerIndex = (index2 + 1) % count;
			this.triangle(bottom[index2], bottom[innerIndex], top[innerIndex], center);
			this.triangle(bottom[index2], top[innerIndex], top[index2], center);
		}
	}

	/**
	 * A triangle facing away from the center (JavaFX takes the counter clockwise
	 * side as the front)
	 */
	private void triangle(double[] first, double[] second, double[] column, double[] center) {
		double[] uCoord = { second[0] - first[0], second[1] - first[1], second[2] - first[2] };
		double[] vCoord = { column[0] - first[0], column[1] - first[1], column[2] - first[2] };
		double[] normal = { uCoord[1] * vCoord[2] - uCoord[2] * vCoord[1], uCoord[2] * vCoord[0] - uCoord[0] * vCoord[2], uCoord[0] * vCoord[1] - uCoord[1] * vCoord[0] };
		double dot = 0;
		for (int deepIndex = 0; deepIndex < 3; deepIndex++) {
			dot += normal[deepIndex] * ((first[deepIndex] + second[deepIndex] + column[deepIndex]) / 3 - center[deepIndex]);
		}
		if (dot < 0) {
			double[] swap = second;
			second = column;
			column = swap;
		}
		int base = this.mesh.getPoints().size() / 3;
		this.mesh.getPoints().addAll((float) first[0], (float) first[1], (float) first[2], (float) second[0], (float) second[1], (float) second[2], (float) column[0], (float) column[1], (float) column[2]);
		this.mesh.getFaces().addAll(base, 0, base + 1, 0, base + 2, 0);
	}

	void box(double cxValue, double cyValue, double czValue, double sxValue, double syValue, double szValue) {
		this.frustum(new double[][] { { cxValue - sxValue / 2, czValue - szValue / 2 }, { cxValue + sxValue / 2, czValue - szValue / 2 }, { cxValue + sxValue / 2, czValue + szValue / 2 }, { cxValue - sxValue / 2, czValue + szValue / 2 } }, cyValue + syValue / 2, cyValue - syValue / 2, 1);
	}

	/**
	 * A polygon seen from above {x, z}, from y0 up to y1, the top scaled around
	 * the middle: sloped sides
	 */
	void frustum(double[][] outline, double y0Value, double y1Value, double topScale) {
		int count = outline.length;
		double cxValue = 0;
		double czValue = 0;
		for (double[] point : outline) {
			cxValue += point[0] / count;
			czValue += point[1] / count;
		}
		double[][] bottom = new double[count][];
		double[][] top = new double[count][];
		for (int index = 0; index < count; index++) {
			bottom[index] = new double[] { outline[index][0], y0Value, outline[index][1] };
			top[index] = new double[] { cxValue + (outline[index][0] - cxValue) * topScale, y1Value, czValue + (outline[index][1] - czValue) * topScale };
		}
		this.solid(bottom, top);
	}

	/**
	 * A profile {z, y} pushed along x
	 */
	void prismZY(double[][] profile, double x0Value, double x1Value) {
		int count = profile.length;
		double[][] first = new double[count][];
		double[][] second = new double[count][];
		for (int index = 0; index < count; index++) {
			first[index] = new double[] { x0Value, profile[index][1], profile[index][0] };
			second[index] = new double[] { x1Value, profile[index][1], profile[index][0] };
		}
		this.solid(first, second);
	}

	/**
	 * A profile {x, y} pushed along z
	 */
	void prismXY(double[][] profile, double z0Value, double z1Value) {
		int count = profile.length;
		double[][] first = new double[count][];
		double[][] second = new double[count][];
		for (int index = 0; index < count; index++) {
			first[index] = new double[] { profile[index][0], profile[index][1], z0Value };
			second[index] = new double[] { profile[index][0], profile[index][1], z1Value };
		}
		this.solid(first, second);
	}

	/**
	 * A hexagon in the z, y plane, pushed along x: a plate of armor
	 */
	void hexPlate(double posZ, double posY, double radiusZ, double radiusY, double x0Value, double x1Value) {
		double[][] hexagon = new double[6][];
		for (int index = 0; index < 6; index++) {
			hexagon[index] = new double[] { posZ + radiusZ * Math.cos(Math.PI / 3 * index), posY + radiusY * Math.sin(Math.PI / 3 * index) };
		}
		this.prismZY(hexagon, x0Value, x1Value);
	}

	/**
	 * A vertical cylinder
	 */
	void cylinderY(double cxValue, double czValue, double radius, double y0Value, double y1Value, int sides) {
		double[][] bottom = new double[sides][];
		double[][] top = new double[sides][];
		for (int index = 0; index < sides; index++) {
			double posX = cxValue + radius * Math.cos(Math.PI * 2 * index / sides);
			double posZ = czValue + radius * Math.sin(Math.PI * 2 * index / sides);
			bottom[index] = new double[] { posX, y0Value, posZ };
			top[index] = new double[] { posX, y1Value, posZ };
		}
		this.solid(bottom, top);
	}

	/**
	 * A cylinder along z, like a gun barrel
	 */
	void cylinderZ(double cxValue, double cyValue, double radius, double z0Value, double z1Value, int sides) {
		double[][] profile = new double[sides][];
		for (int index = 0; index < sides; index++) {
			profile[index] = new double[] { cxValue + radius * Math.cos(Math.PI * 2 * index / sides), cyValue + radius * Math.sin(Math.PI * 2 * index / sides) };
		}
		this.prismXY(profile, z0Value, z1Value);
	}

	/**
	 * A spindle along z, pointed at both ends: a hexagon ring in the middle (at z)
	 * and a tip in front and another behind
	 */
	void spindle(double cxValue, double cyValue, double posZ, double radius, double front, double back, int sides) {
		double[][] ring = new double[sides][];
		double[][] nose = new double[sides][];
		double[][] tail = new double[sides][];
		for (int index = 0; index < sides; index++) {
			ring[index] = new double[] { cxValue + radius * Math.cos(Math.PI * 2 * index / sides), cyValue + radius * Math.sin(Math.PI * 2 * index / sides), posZ };
			nose[index] = new double[] { cxValue, cyValue, posZ + front };
			tail[index] = new double[] { cxValue, cyValue, posZ - back };
		}
		this.solid(ring, nose);
		this.solid(ring, tail);
	}

	/**
	 * A thin tube from a to b, like a segment of a lightning bolt
	 */
	void tube(double[] first, double[] second, double radius, int sides) {
		double[] direction = { second[0] - first[0], second[1] - first[1], second[2] - first[2] };
		double length = Math.sqrt(direction[0] * direction[0] + direction[1] * direction[1] + direction[2] * direction[2]);
		if (length < 1e-6) {
			return;
		}
		for (int deepIndex = 0; deepIndex < 3; deepIndex++) {
			direction[deepIndex] /= length;
		}
		double[] reference = Math.abs(direction[1]) < 0.9 ? new double[] { 0, 1, 0 } : new double[] { 1, 0, 0 };
		double[] uCoord = { direction[1] * reference[2] - direction[2] * reference[1], direction[2] * reference[0] - direction[0] * reference[2], direction[0] * reference[1] - direction[1] * reference[0] };
		double size = Math.sqrt(uCoord[0] * uCoord[0] + uCoord[1] * uCoord[1] + uCoord[2] * uCoord[2]);
		for (int deepIndex2 = 0; deepIndex2 < 3; deepIndex2++) {
			uCoord[deepIndex2] /= size;
		}
		double[] vCoord = { direction[1] * uCoord[2] - direction[2] * uCoord[1], direction[2] * uCoord[0] - direction[0] * uCoord[2], direction[0] * uCoord[1] - direction[1] * uCoord[0] };
		double[][] bottom = new double[sides][3];
		double[][] top = new double[sides][3];
		for (int index = 0; index < sides; index++) {
			double cos = Math.cos(Math.PI * 2 * index / sides) * radius;
			double sin = Math.sin(Math.PI * 2 * index / sides) * radius;
			for (int deepIndex3 = 0; deepIndex3 < 3; deepIndex3++) {
				double offset = uCoord[deepIndex3] * cos + vCoord[deepIndex3] * sin;
				bottom[index][deepIndex3] = first[deepIndex3] + offset;
				top[index][deepIndex3] = second[deepIndex3] + offset;
			}
		}
		this.solid(bottom, top);
	}

	/**
	 * A flat ring on the floor plane, from y0 to y1
	 */
	void ring(double inner, double outer, double y0Value, double y1Value, int segments) {
		for (int index = 0; index < segments; index++) {
			double first = Math.PI * 2 * index / segments;
			double second = Math.PI * 2 * (index + 1) / segments;
			this.frustum(new double[][] { { inner * Math.cos(first), inner * Math.sin(first) }, { outer * Math.cos(first), outer * Math.sin(first) }, { outer * Math.cos(second), outer * Math.sin(second) },
					{ inner * Math.cos(second), inner * Math.sin(second) } }, y0Value, y1Value, 1);
		}
	}

	/**
	 * A boulder: an irregular, faceted stone. The base and the top are rings of
	 * points that are moved at random, so no two are alike.
	 *
	 * @param topScale
	 *            how much smaller the top is than the base (0.5 is a cone-ish
	 *            stone, 0.9 a block)
	 * @param roughness
	 *            how far the points move, 0.3 to 0.7
	 */
	void rock(double cxValue, double czValue, double baseY, double height, double radiusX, double radiusZ, int sides, double topScale, double roughness, java.util.Random random) {
		double[][] bottom = new double[sides][];
		double[][] top = new double[sides][];
		double shiftX = (random.nextDouble() - 0.5) * radiusX * 0.5;
		double shiftZ = (random.nextDouble() - 0.5) * radiusZ * 0.5;
		for (int index = 0; index < sides; index++) {
			double angle = Math.PI * 2 * (index + (random.nextDouble() - 0.5) * 0.5) / sides;
			double row = 1 - roughness * 0.5 + random.nextDouble() * roughness;
			double scale = topScale * (0.8 + random.nextDouble() * 0.4);
			bottom[index] = new double[] { cxValue + Math.cos(angle) * radiusX * row, baseY, czValue + Math.sin(angle) * radiusZ * row };
			top[index] = new double[] { cxValue + shiftX + Math.cos(angle) * radiusX * row * scale, baseY - height * (0.8 + random.nextDouble() * 0.3), czValue + shiftZ + Math.sin(angle) * radiusZ * row * scale };
		}
		this.solid(bottom, top);
	}
}
