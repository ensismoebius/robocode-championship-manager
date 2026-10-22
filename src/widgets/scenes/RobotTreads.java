package widgets.scenes;

/**
 * The treads of a robot: the outline of each belt in the side view, the points along it, and the links that go round it
 */
final class RobotTreads {

	/** The links of a tread, which go round the belt of their side of the hull */
	static final int TREAD_LINKS = 30;

	/** The belt in the side view, as a closed outline: its back, up, over the top, down and back along the bottom */
	static final double[][] BELT = { { -20, -1 }, { -21.5, -4 }, { -19.5, -9 }, { 18.5, -9 }, { 21.5, -4 }, { 20, -1 } };

	/** The length of the outline: one turn of the belt */
	static final double BELT_LENGTH = beltLength();

	/** The width of a belt, sideways, from its inner face to its outer one, where the links are */
	static final double BELT_INNER = 8;

	static final double BELT_OUTER = 17.5;

	static final double BELT_CENTER = (BELT_INNER + BELT_OUTER) / 2;

	static final double BELT_WIDTH = BELT_OUTER - BELT_INNER;

	/** The length of the belt of the tracks, around the wheels. */
	static double beltLength() {
		double length = 0;
		for (int index = 0; index < BELT.length; index++) {
			double[] first = BELT[index];
			double[] second = BELT[(index + 1) % BELT.length];
			length += Math.hypot(second[0] - first[0], second[1] - first[1]);
		}
		return length;
	}

	/**
	 * Where a link is on the belt, s from 0 to BELT_LENGTH along its outline: its z
	 * and its y in the body, and the angle of the outline there (turned round x).
	 * The links follow the shape of the track, its chamfers included.
	 */
	static double[] beltPoint(double text) {
		double rest = text;
		for (int index = 0; index < BELT.length - 1; index++) {
			double[] first = BELT[index];
			double[] second = BELT[index + 1];
			double deltaZ = second[0] - first[0];
			double deltaY = second[1] - first[1];
			double length = Math.hypot(deltaZ, deltaY);
			if (rest <= length) {
				return new double[] { first[0] + deltaZ * rest / length, first[1] + deltaY * rest / length, Math.atan2(-deltaY, deltaZ) };
			}
			rest -= length;
		}
		// The last piece of the outline, back to its start
		double[] first2 = BELT[BELT.length - 1];
		double[] second2 = BELT[0];
		double deltaZ2 = second2[0] - first2[0];
		double deltaY2 = second2[1] - first2[1];
		double length = Math.hypot(deltaZ2, deltaY2);
		return new double[] { first2[0] + deltaZ2 * rest / length, first2[1] + deltaY2 * rest / length, Math.atan2(-deltaY2, deltaZ2) };
	}

	/**
	 * A point on one side of the hull, in the arena (side 0 is the left)
	 */
	static double[] sidePoint(double posX, double posZ, double heading, int side) {
		double sideways = side == 0 ? -BELT_CENTER : BELT_CENTER;
		return new double[] { posX + sideways * Math.cos(heading), posZ - sideways * Math.sin(heading) };
	}
}
