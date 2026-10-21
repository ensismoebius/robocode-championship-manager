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

	/** Creates the city of the neon arena, with the texture of its facades. */
	NeonCity() {
		this.facades.getTexCoords().addAll(0, 0);
	}

	/**
	 * A box in a mesh without a texture (it uses only the first texture point),
	 * the mesh must start with a texture point
	 */
	static void box(TriangleMesh mesh, double cxValue, double cyValue, double czValue, double sxValue, double syValue, double szValue) {
		if (mesh.getTexCoords().size() == 0) {
			mesh.getTexCoords().addAll(0, 0);
		}
		int base = mesh.getPoints().size() / 3;
		for (int index = 0; index < 8; index++) {
			mesh.getPoints().addAll((float) (cxValue + ((index & 1) == 0 ? -sxValue : sxValue) / 2), (float) (cyValue + ((index & 2) == 0 ? -syValue : syValue) / 2), (float) (czValue + ((index & 4) == 0 ? -szValue : szValue) / 2));
		}
		int[][] quads = { { 0, 1, 3, 2 }, { 4, 6, 7, 5 }, { 0, 2, 6, 4 }, { 1, 5, 7, 3 }, { 0, 4, 5, 1 }, { 2, 3, 7, 6 } };
		for (int[] other : quads) {
			mesh.getFaces().addAll(base + other[0], 0, base + other[1], 0, base + other[2], 0, base + other[0], 0, base + other[2], 0, base + other[3], 0);
		}
	}

	/**
	 * A tower with its base on the ground at x, z
	 */
	void addTower(double posX, double posZ, double width, double depth, double height, Random random) {
		double ground = 6;
		double[][] corners = { { posX - width / 2, posZ - depth / 2 }, { posX + width / 2, posZ - depth / 2 }, { posX + width / 2, posZ + depth / 2 }, { posX - width / 2, posZ + depth / 2 } };
		float offsetU = (float) random.nextDouble();
		float offsetV = (float) random.nextDouble();
		for (int side = 0; side < 4; side++) {
			double[] first = corners[side];
			double[] second = corners[(side + 1) % 4];
			double length = Math.hypot(second[0] - first[0], second[1] - first[1]);
			float uCoord = (float) (length / NeonTexture.FACADE_WIDTH);
			float vCoord = (float) (height / NeonTexture.FACADE_HEIGHT);
			int base = this.facades.getPoints().size() / 3;
			this.facades.getPoints().addAll((float) first[0], (float) ground, (float) first[1], (float) second[0], (float) ground, (float) second[1], (float) second[0], (float) (ground - height), (float) second[1],
					(float) first[0], (float) (ground - height), (float) first[1]);
			int tex = this.facades.getTexCoords().size() / 2;
			// The top of the texture is at the top of the tower
			this.facades.getTexCoords().addAll(offsetU, offsetV + vCoord, offsetU + uCoord, offsetV + vCoord, offsetU + uCoord, offsetV, offsetU, offsetV);
			this.facades.getFaces().addAll(base, tex, base + 1, tex + 1, base + 2, tex + 2, base, tex, base + 2, tex + 2, base + 3, tex + 3);
		}
		TriangleMesh trim = random.nextBoolean() ? this.cyan : this.magenta;
		double top = ground - height;
		// A neon crown around the top
		box(trim, posX, top, posZ - depth / 2, width + 6, 7, 6);
		box(trim, posX, top, posZ + depth / 2, width + 6, 7, 6);
		box(trim, posX - width / 2, top, posZ, 6, 7, depth + 6);
		box(trim, posX + width / 2, top, posZ, 6, 7, depth + 6);
		// Tall towers also have glowing edges, and some an antenna
		if (height > 700) {
			for (double[] corner : corners) {
				box(trim, corner[0], ground - height / 2, corner[1], 5, height, 5);
			}
		}
		if (random.nextDouble() < 0.35) {
			double antenna = 80 + random.nextDouble() * 160;
			box(trim, posX, top - antenna / 2, posZ, 6, antenna, 6);
		}
	}

	/** The streets of the city run along both axes, this far from each other */
	static final double ROAD_SPACING = 560;
	static final double ROAD_WIDTH = 70;
	/** The streets go this far from the middle, both ways */
	static final double ROAD_REACH = 3600;

	/**
	 * Where the streets are along an axis, that is the coordinates of the streets
	 * that run across it: all but those too near the field, which is half its
	 * size from the middle
	 */
	static double[] roads(double half) {
		java.util.List<Double> positions = new java.util.ArrayList<>();
		for (int deepIndex = -7; deepIndex <= 6; deepIndex++) {
			double position = (deepIndex + 0.5) * ROAD_SPACING;
			if (Math.abs(position) >= half + 100) {
				positions.add(position);
			}
		}
		return positions.stream().mapToDouble(Double::doubleValue).toArray();
	}

	/**
	 * Towers around the field, far away and between the streets: each one stands
	 * in a block, never over a street
	 *
	 * @param roadsX
	 *            the x of the streets that run along z
	 * @param roadsZ
	 *            the z of the streets that run along x
	 */
	void skyline(int count, Random random, double[] roadsX, double[] roadsZ) {
		for (int index = 0; index < count; index++) {
			// Some places fall on a street: the tower is tried somewhere else
			for (int attempt = 0; attempt < 40; attempt++) {
				double angle = random.nextDouble() * Math.PI * 2;
				double distance = 1500 + random.nextDouble() * 1100;
				double height = 250 + Math.pow(random.nextDouble(), 2.2) * 1700;
				double posX = Math.cos(angle) * distance * 1.2;
				double posZ = Math.sin(angle) * distance;
				// A block between two streets is about 490 wide: the towers fit in it
				double width = 150 + random.nextDouble() * 140;
				double depth = 150 + random.nextDouble() * 140;
				if (!onStreet(posX, width / 2, roadsX) && !onStreet(posZ, depth / 2, roadsZ)) {
					this.addTower(posX, posZ, width, depth, height, random);
					break;
				}
			}
		}
	}

	/** Whether a footprint that goes this far from the coordinate touches a street */
	private static boolean onStreet(double coordinate, double reach, double[] roads) {
		for (double road : roads) {
			if (Math.abs(coordinate - road) < reach + ROAD_WIDTH / 2 + 12) {
				return true;
			}
		}
		return false;
	}
}
