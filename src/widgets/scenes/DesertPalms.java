package widgets.scenes;

import java.util.Random;

import javafx.scene.paint.Color;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The oases of the desert arena, with palms in the style of the platform games of the 90s: a tall trunk, thick and straight,
 * made of tan segments with a dark ring between them, a brown knob with coconuts under the crown, and big leaves that
 * arch from it, up and then down, like folded paper, each one of flat facets that are light green and dark green by turns. They stand on the
 * plain, beyond the reach of the creatures.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertPalms {

	/** How many palms there are around the courtyard */
	private static final int PALMS = 16;
	/** How far beyond the edges of the courtyard the palms stand, at least */
	private static final double REACH = 500;
	private static final int FRONDS = 8;
	/** The tan segments of the trunk, and the dark rings between them, in units of height */
	private static final double SEGMENT = 12;
	private static final double RING = 4;
	/** The radius of the trunk: it does not thin toward the crown, each segment only narrows a little under its ring */
	private static final double TRUNK = 9;
	/** Into how many stretches along its arch a leaf is cut, each one of its own facets */
	private static final int STRETCHES = 4;
	/** How thick the plate of a facet of a leaf is: both of its sides are seen */
	private static final double PLATE = 1.6;

	private final BattleArena3D host;
	private final LowPoly bark = new LowPoly();
	private final LowPoly rings = new LowPoly();
	private final LowPoly light = new LowPoly();
	private final LowPoly dark = new LowPoly();

	/** The oases, drawn in the host scene. */
	DesertPalms(BattleArena3D host) {
		this.host = host;
	}

	/** Places the palms around the courtyard, on the plain, and puts their meshes in the host scene. */
	void build(double width, double depth, Random wild) {
		for (int index = 0; index < PALMS; index++) {
			double angle = Math.PI * 2 * index / PALMS + (wild.nextDouble() - 0.5) * 0.2;
			double posX = Math.cos(angle) * (width / 2 + REACH + wild.nextDouble() * 60);
			double posZ = Math.sin(angle) * (depth / 2 + REACH + wild.nextDouble() * 60);
			this.plant(posX, DesertDunes.PLAIN - DesertDunes.height(posX, posZ, width, depth), posZ, wild);
		}
		this.show();
	}

	/** Plants a palm with its foot at the point, on the ground at the height given. */
	void plant(double posX, double ground, double posZ, Random wild) {
		this.palm(posX, ground, posZ, this.bark, this.rings, this.light, this.dark, wild);
	}

	/** Puts the meshes of the palms planted so far in the host scene. */
	void show() {
		host.arena.getChildren().addAll(this.bark.view(ArenaMaterials.material(Color.web("#d9a24f"))), this.rings.view(ArenaMaterials.material(Color.web("#6b3f1c"))), this.light.view(ArenaMaterials.material(Color.web("#66c433"))),
				this.dark.view(ArenaMaterials.material(Color.web("#2c8f2e"))));
	}

	/** One palm: the trunk of segments and rings, the knob with its coconuts, and the leaves that hang from it. */
	private void palm(double posX, double ground, double posZ, LowPoly bark, LowPoly rings, LowPoly light, LowPoly dark, Random wild) {
		double height = 230 + wild.nextDouble() * 70;
		double lean = (wild.nextDouble() - 0.5) * 24;
		this.trunk(posX, ground, posZ, height, lean, bark, rings);
		double[] crown = trunkPoint(posX, ground, posZ, height, lean, 1);
		rings.cylinderY(crown[0], crown[2], TRUNK * 1.35, crown[1] + 4, crown[1] - 10, 8);
		for (int coconut = 0; coconut < 3; coconut++) {
			double angle = Math.PI * 2 * coconut / 3 + wild.nextDouble();
			rings.rock(crown[0] + Math.cos(angle) * 9, crown[2] + Math.sin(angle) * 9, crown[1] + 16, 12, 6, 6, 6, 0.7, 0.2, wild);
		}
		for (int frond = 0; frond < FRONDS; frond++) {
			double angle = Math.PI * 2 * frond / FRONDS + wild.nextDouble() * 0.4;
			this.frond(crown, angle, 150 + wild.nextDouble() * 40, 1 + wild.nextDouble() * 0.4, 0.45 + wild.nextDouble() * 0.25, light, dark);
		}
	}

	/** A point of the axis of the trunk, from the foot (0) to the crown (1): it leans a little more the higher it goes */
	private static double[] trunkPoint(double posX, double ground, double posZ, double height, double lean, double share) {
		return new double[] { posX + lean * share * share, ground - height * share, posZ + lean / 2 * share * share };
	}

	/** The trunk: a tan segment that narrows a little, and over it a dark ring that is a little wider, again and again up to the crown. */
	private void trunk(double posX, double ground, double posZ, double height, double lean, LowPoly bark, LowPoly rings) {
		int cycles = (int) (height / (SEGMENT + RING));
		for (int cycle = 0; cycle < cycles; cycle++) {
			double from = (double) cycle / cycles;
			double ringStart = from + (1.0 / cycles) * SEGMENT / (SEGMENT + RING);
			double until = (double) (cycle + 1) / cycles;
			double[] foot = trunkPoint(posX, ground + (cycle == 0 ? 10 : 0), posZ, height, lean, from);
			double[] under = trunkPoint(posX, ground, posZ, height, lean, ringStart);
			double[] over = trunkPoint(posX, ground, posZ, height, lean, until);
			bark.tube(foot, under, TRUNK, TRUNK * 0.88, 8);
			rings.tube(under, over, TRUNK * 1.08, TRUNK * 1.08, 8);
		}
	}

	/**
	 * A leaf: a ridge from the crown to its tip that arches, going up first and coming down after the peak, with the two sides
	 * of the leaf folded down from it like a tent, wide in the middle and pointed at both ends. It is cut in stretches along
	 * the arch, each one of flat facets, and the facets are light and dark in turns, the two sides the other way around.
	 *
	 * @param climb
	 *            how far the ridge would go up, in lengths of the leaf, if it went on straight: the arch peaks about a third of the way
	 * @param sag
	 *            how far under the crown the tip hangs, in lengths of the leaf
	 */
	private void frond(double[] crown, double angle, double length, double climb, double sag, LowPoly light, LowPoly dark) {
		double[] side = { -Math.sin(angle), 0, Math.cos(angle) };
		double[][] ridge = new double[STRETCHES + 1][];
		for (int station = 0; station <= STRETCHES; station++) {
			double share = (double) station / STRETCHES;
			ridge[station] = new double[] { crown[0] + Math.cos(angle) * length * share, crown[1] - 6 - climb * length * share + (climb + sag) * length * share * share, crown[2] + Math.sin(angle) * length * share };
		}
		double[][] left = new double[STRETCHES + 1][];
		double[][] right = new double[STRETCHES + 1][];
		double[][] sky = new double[STRETCHES + 1][];
		for (int station = 0; station <= STRETCHES; station++) {
			double[] ahead = ridge[Math.min(station + 1, STRETCHES)];
			double[] behind = ridge[Math.max(station - 1, 0)];
			double[] axis = normalize(new double[] { ahead[0] - behind[0], ahead[1] - behind[1], ahead[2] - behind[2] });
			// The way the leaf faces, to the sky: up is negative y
			sky[station] = normalize(new double[] { axis[1] * side[2] - axis[2] * side[1], axis[2] * side[0] - axis[0] * side[2], axis[0] * side[1] - axis[1] * side[0] });
			double wide = length * 0.4 * Math.pow(Math.sin(Math.PI * station / STRETCHES), 0.7);
			left[station] = edge(ridge[station], side, wide / 2, sky[station], wide * 0.2);
			right[station] = edge(ridge[station], side, -wide / 2, sky[station], wide * 0.2);
		}
		for (int stretch = 0; stretch < STRETCHES; stretch++) {
			LowPoly leftShade = stretch % 2 == 0 ? light : dark;
			LowPoly rightShade = stretch % 2 == 0 ? dark : light;
			plate(leftShade, ridge[stretch], left[stretch], left[stretch + 1], sky[stretch]);
			plate(leftShade, ridge[stretch], left[stretch + 1], ridge[stretch + 1], sky[stretch]);
			plate(rightShade, ridge[stretch], right[stretch], right[stretch + 1], sky[stretch]);
			plate(rightShade, ridge[stretch], right[stretch + 1], ridge[stretch + 1], sky[stretch]);
		}
	}

	/** The edge of the leaf at a point of its ridge: out to the side, and down from the way the leaf faces so the leaf is folded like a tent. */
	private static double[] edge(double[] ridge, double[] side, double out, double[] sky, double dip) {
		return new double[] { ridge[0] + side[0] * out - sky[0] * dip, ridge[1] + side[1] * out - sky[1] * dip, ridge[2] + side[2] * out - sky[2] * dip };
	}

	/** A flat triangle with a little thickness along the way the leaf faces, so it is seen from both sides. A triangle with two corners together is nothing and is left out. */
	private static void plate(LowPoly shape, double[] first, double[] second, double[] third, double[] sky) {
		double[][] bottom = { first, second, third };
		if (apart(first, second) && apart(second, third) && apart(first, third)) {
			double[][] top = new double[3][];
			for (int index = 0; index < 3; index++) {
				top[index] = new double[] { bottom[index][0] + sky[0] * PLATE, bottom[index][1] + sky[1] * PLATE, bottom[index][2] + sky[2] * PLATE };
			}
			shape.solid(bottom, top);
		}
	}

	/** Whether two points are not on the same spot. */
	private static boolean apart(double[] first, double[] second) {
		return Math.abs(first[0] - second[0]) + Math.abs(first[1] - second[1]) + Math.abs(first[2] - second[2]) > 0.01;
	}

	/** The vector with the same direction and a length of 1 */
	private static double[] normalize(double[] vector) {
		double length = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
		return new double[] { vector[0] / length, vector[1] / length, vector[2] / length };
	}
}
