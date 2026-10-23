package widgets.scenes;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The floor of the field of the Eden Solar arena: patterns from African art, as in mudcloth, Kente cloth and the
 * painted houses of the Ndebele (bands of triangles and zigzags, and diamonds inside diamonds, in the colors of
 * the earth), crossed by thin lines of light, like the traces of a circuit, with a node at every corner. In the
 * middle, a sun. There are two pictures of the same floor: its colors and its lights.
 *
 * <p>
 * All the measures are in units of the field, and the points are given from the middle of the field.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenFloor {

	private static final int DARK = 0x2b1a12;
	private static final int BRICK = 0x7a2e1c;
	private static final int TERRACOTTA = 0xa4452a;
	private static final int OCHRE = 0xe0a63a;
	private static final int CREAM = 0xecd9ae;
	private static final int TEAL = 0x1f7f7c;
	/** The rings of a diamond from its center out, for the two kinds of diamond that alternate */
	private static final int[][] DIAMONDS = { { DARK, OCHRE, TERRACOTTA, CREAM, TEAL, TERRACOTTA, OCHRE, DARK }, { TERRACOTTA, CREAM, TEAL, OCHRE, DARK, TEAL, CREAM, DARK } };

	private static final int CYAN = 0x2cb8c8;
	private static final int AMBER = 0xd0902f;

	/** The border of the field, from its edge in: the width of all its bands */
	private static final double BORDER = 45;
	/** The diamonds are this wide from tip to tip, with a ring every RING units from the center to the tip */
	private static final double CELL = 100;
	private static final double RING = CELL / 16;
	/** The sun in the middle, to this radius */
	private static final double SUN = 91;
	private static final double TRIANGLE = 14;
	private static final double ZIGZAG = 24;

	/** The floor is only a set of functions: this class is not instantiated. */
	private EdenFloor() {
	}

	/** How far inside the edge of the field a point is. */
	private static double edge(double posX, double posZ, double width, double height) {
		return Math.min(width / 2 - Math.abs(posX), height / 2 - Math.abs(posZ));
	}

	/** The position along the nearest edge of the field, for the bands of the border. */
	private static double along(double posX, double posZ, double width, double height) {
		return width / 2 - Math.abs(posX) < height / 2 - Math.abs(posZ) ? posZ : posX;
	}

	/** A number between 0 and 1: how far a position is along a repeating piece of the given size. */
	private static double share(double position, double size) {
		double turns = position / size;
		return turns - Math.floor(turns);
	}

	/** The color of the floor at a point inside the field. */
	static int color(double posX, double posZ, double width, double height) {
		double edge = edge(posX, posZ, width, height);
		if (edge < BORDER) {
			return border(edge, along(posX, posZ, width, height));
		}
		double radius = Math.hypot(posX, posZ);
		// The inside is darkened, so the robots and the lights stand out from the border
		if (radius < SUN) {
			return TextureMath.mix(sun(radius, Math.atan2(posZ, posX)), DARK, 0.2);
		}
		return TextureMath.mix(diamond(posX, posZ), DARK, 0.5);
	}

	/** The light of the floor at a point inside the field: black, or the color of a trace or a node. */
	static int glow(double posX, double posZ, double width, double height) {
		double edge = edge(posX, posZ, width, height);
		if (edge < BORDER) {
			return borderGlow(edge, along(posX, posZ, width, height));
		}
		double radius = Math.hypot(posX, posZ);
		return radius < SUN ? sunGlow(radius, Math.atan2(posZ, posX)) : diamondGlow(posX, posZ);
	}

	/** The bands of the border: the line of the field, triangles, a zigzag, and their lines between */
	private static int border(double edge, double along) {
		if (edge < 3 || edge >= 19 && edge < 21 || edge >= 41 && edge < 43) {
			return CREAM;
		}
		if (edge < 5 || edge >= 21 && edge < 23 || edge >= 43) {
			return DARK;
		}
		return edge < 19 ? triangles(edge - 5, along) : zigzag(edge - 23, along);
	}

	/** Triangles with their base on the outer side, in terracotta and teal, on ochre */
	private static int triangles(double depth, double along) {
		double across = share(along, TRIANGLE);
		boolean filled = depth / TRIANGLE < 1 - Math.abs(2 * across - 1);
		if (!filled) {
			return OCHRE;
		}
		return Math.floorMod((int) Math.floor(along / TRIANGLE), 2) == 0 ? TERRACOTTA : TEAL;
	}

	/** How far from the outer line of its band the middle of the zigzag is at a position. */
	private static double zigzagMiddle(double along) {
		return 3 + 12 * Math.abs(2 * share(along, ZIGZAG) - 1);
	}

	/** A thick ochre zigzag between thin cream lines, on a deep red */
	private static int zigzag(double depth, double along) {
		double away = Math.abs(depth - zigzagMiddle(along));
		if (away < 3) {
			return OCHRE;
		}
		return away < 4.4 ? CREAM : BRICK;
	}

	/** The lights of the border: the limit of the field in amber, and a thread of signal inside the zigzag */
	private static int borderGlow(double edge, double along) {
		if (edge >= 3 && edge < 5) {
			return AMBER;
		}
		boolean inZigzag = edge >= 23 && edge < 41;
		return inZigzag && Math.abs(edge - 23 - zigzagMiddle(along)) < 0.9 ? CYAN : 0;
	}

	/**
	 * Where a point is in its diamond: how far from the middle along each of the two diagonals, and the number of the
	 * diamond, odd or even. The diamonds are the squares of a net turned a quarter of a turn, with a diamond at the
	 * middle of the field.
	 */
	private static double[] inDiamond(double posX, double posZ) {
		double sum = posX + posZ + CELL / 2;
		double diff = posX - posZ + CELL / 2;
		double sumCell = Math.floor(sum / CELL);
		double diffCell = Math.floor(diff / CELL);
		return new double[] { Math.abs(sum - (sumCell + 0.5) * CELL), Math.abs(diff - (diffCell + 0.5) * CELL), sumCell + diffCell };
	}

	/** The diamond at a point: rings of color from its middle to its tips, the kinds alternating */
	private static int diamond(double posX, double posZ) {
		double[] offsets = inDiamond(posX, posZ);
		int ring = Math.min(DIAMONDS[0].length - 1, (int) (Math.max(offsets[0], offsets[1]) / RING));
		return DIAMONDS[Math.floorMod((int) offsets[2], 2)][ring];
	}

	/** The traces along the outline of every diamond, a node at each tip and one at its middle */
	private static int diamondGlow(double posX, double posZ) {
		double[] offsets = inDiamond(posX, posZ);
		double across = Math.max(offsets[0], offsets[1]);
		double fromTip = Math.hypot(offsets[0] - CELL / 2, offsets[1] - CELL / 2) / Math.sqrt(2);
		if (fromTip < 3.4 || across < 2.2) {
			return AMBER;
		}
		return Math.abs(across - (CELL / 2 - 3)) < 0.9 ? CYAN : 0;
	}

	/** The sun: a disc, a ring of sixteen rays, a ring of beads, and rings between them */
	private static int sun(double radius, double angle) {
		if (radius < 6) {
			return OCHRE;
		}
		if (radius < 12 || radius >= 55 && radius < 58 || radius >= 81 && radius < 84) {
			return DARK;
		}
		if (radius < 18 || radius >= 52 && radius < 55 || radius >= 78 && radius < 81 || radius >= 88) {
			return CREAM;
		}
		if (radius < 26) {
			return TEAL;
		}
		if (radius < 52) {
			return ray(radius - 26, angle) ? OCHRE : TERRACOTTA;
		}
		return radius < 78 ? bead(radius, angle) < 6 ? CREAM : BRICK : TERRACOTTA;
	}

	/** Whether a point of the ring of rays, at the depth from its inner edge, is inside a ray. */
	private static boolean ray(double depth, double angle) {
		double across = share(angle / (Math.PI * 2) * 16, 1);
		return depth / 26 < 1 - Math.abs(2 * across - 1);
	}

	/** How far a point is from the nearest of the twenty-four beads of the ring. */
	private static double bead(double radius, double angle) {
		double step = Math.PI * 2 / 24;
		double around = Math.IEEEremainder(angle, step) * 68;
		return Math.hypot(around, radius - 68);
	}

	/** The lights of the sun: its middle, two threads around it, and the beads */
	private static int sunGlow(double radius, double angle) {
		if (radius < 3 || radius >= 58 && radius < 78 && bead(radius, angle) < 2.2) {
			return AMBER;
		}
		return Math.abs(radius - 56.5) < 0.7 || Math.abs(radius - 82.5) < 0.7 ? CYAN : 0;
	}
}
