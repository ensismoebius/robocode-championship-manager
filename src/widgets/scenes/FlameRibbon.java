package widgets.scenes;

import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The strip a flame is drawn on: it stands on the floor, always facing the camera,
 * and is cut in sections that bend more and more toward the tip, as the tongue of a
 * real fire does under the wind, and sway at the tip. A flat picture turned as a
 * whole would look like a leaning board; this one bends. Its base is always at the
 * origin of the strip, and does not move.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class FlameRibbon {

	/** The sections of the strip */
	static final int SECTIONS = 7;

	/** The strip: two points for each row, the base is the first row */
	final TriangleMesh mesh = new TriangleMesh();
	private final float[] points = new float[(SECTIONS + 1) * 2 * 3];
	/** How far aside each row of the strip is from the vertical of the base, on the floor: the caller sets them before shaping the strip */
	final double[] asideX = new double[SECTIONS + 1];
	final double[] asideZ = new double[SECTIONS + 1];

	/** Makes the strip, straight up, with the picture of the flame on it: its tip at the top. */
	FlameRibbon() {
		for (int row = 0; row <= SECTIONS; row++) {
			float down = 1 - (float) row / SECTIONS;
			this.mesh.getTexCoords().addAll(0, down, 1, down);
		}
		for (int section = 0; section < SECTIONS; section++) {
			int left = 2 * section;
			int right = left + 1;
			int farLeft = left + 2;
			int farRight = left + 3;
			this.mesh.getFaces().addAll(left, left, right, right, farRight, farRight, left, left, farRight, farRight, farLeft, farLeft);
		}
		this.mesh.getPoints().resize(this.points.length);
		this.shape(1, 1, 1, 0, 0);
	}

	/**
	 * Bends the strip: up from the base, and aside as it goes up, as far as asked for
	 * each row in {@link #asideX} and {@link #asideZ}, which the caller fills before.
	 *
	 * @param width
	 *            of the strip
	 * @param height
	 *            of the strip, from its base to its tip
	 * @param rightX
	 *            where the right of the camera is, on the floor
	 * @param rightZ
	 *            same
	 * @param sway
	 *            how far the tip swings to the side of the camera, as a share of the height
	 */
	void shape(double width, double height, double rightX, double rightZ, double sway) {
		for (int row = 0; row <= SECTIONS; row++) {
			double rise = (double) row / SECTIONS;
			double swing = sway * height * rise * rise;
			double centerX = this.asideX[row] + rightX * swing;
			double centerZ = this.asideZ[row] + rightZ * swing;
			double altitude = -rise * height;
			this.put(2 * row, centerX - rightX * width / 2, altitude, centerZ - rightZ * width / 2);
			this.put(2 * row + 1, centerX + rightX * width / 2, altitude, centerZ + rightZ * width / 2);
		}
		this.mesh.getPoints().set(0, this.points, 0, this.points.length);
	}

	/** Writes a point of the strip. */
	private void put(int point, double posX, double posY, double posZ) {
		this.points[3 * point] = (float) posX;
		this.points[3 * point + 1] = (float) posY;
		this.points[3 * point + 2] = (float) posZ;
	}

	/** Where the base of the strip is, the middle of its first row, on the floor: x and z. */
	double[] base() {
		return new double[] { (this.points[0] + this.points[3]) / 2, (this.points[2] + this.points[5]) / 2 };
	}

	/** Where the tip of the strip is, the middle of its last row: x, y and z. */
	double[] tip() {
		int last = 2 * SECTIONS * 3;
		return new double[] { (this.points[last] + this.points[last + 3]) / 2, (this.points[last + 1] + this.points[last + 4]) / 2, (this.points[last + 2] + this.points[last + 5]) / 2 };
	}
}
