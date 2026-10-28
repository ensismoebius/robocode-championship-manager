package widgets.scenes;

import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The oases of the desert arena: palms with slender, leaning trunks and a crown of long fronds that droop over the sand,
 * with clusters of dates under them. They stand on the plain, beyond the reach of the creatures.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertPalms {

	/** How many palms there are around the courtyard */
	private static final int PALMS = 16;
	/** How far beyond the edges of the courtyard the palms stand, at least */
	private static final double REACH = 500;
	private static final int FRONDS = 11;
	private static final int SEGMENTS = 7;

	private final BattleArena3D host;

	/** The oases, drawn in the host scene. */
	DesertPalms(BattleArena3D host) {
		this.host = host;
	}

	/** Places the palms around the courtyard, on the plain, and puts their meshes in the host scene. */
	void build(double width, double depth, Random wild) {
		LowPoly trunks = new LowPoly();
		LowPoly fronds = new LowPoly();
		for (int index = 0; index < PALMS; index++) {
			double angle = Math.PI * 2 * index / PALMS + (wild.nextDouble() - 0.5) * 0.2;
			double posX = Math.cos(angle) * (width / 2 + REACH + wild.nextDouble() * 60);
			double posZ = Math.sin(angle) * (depth / 2 + REACH + wild.nextDouble() * 60);
			double ground = DesertDunes.PLAIN - DesertDunes.height(posX, posZ, width, depth);
			this.palm(posX, ground, posZ, trunks, fronds, wild);
		}
		host.arena.getChildren().addAll(trunks.view(ArenaMaterials.material(Color.web("#7a5636"))), fronds.view(frondMaterial()));
	}

	/** The fronds: green leaves with their lights, a little lighter at the tips. */
	private static PhongMaterial frondMaterial() {
		PhongMaterial leaves = ArenaMaterials.textured(JungleTexture.LEAVES, Color.gray(0.1));
		leaves.setDiffuseColor(Color.web("#6f9e3f"));
		return leaves;
	}

	/** One palm: a trunk that leans and thins toward the crown, the fronds that droop from it, and the dates under them. */
	private void palm(double posX, double ground, double posZ, LowPoly trunks, LowPoly fronds, Random wild) {
		double height = 220 + wild.nextDouble() * 80;
		double lean = (wild.nextDouble() - 0.5) * 60;
		double[] crown = { posX + lean, ground - height, posZ + lean / 2 };
		double[] previous = { posX, ground, posZ };
		for (int segment = 1; segment <= SEGMENTS; segment++) {
			double share = (double) segment / SEGMENTS;
			double[] next = { posX + lean * share * share, ground - height * share, posZ + lean / 2 * share * share };
			trunks.tube(previous, next, 9 - 3 * (share - 1.0 / SEGMENTS), 9 - 3 * share, 7);
			previous = next;
		}
		for (int frond = 0; frond < FRONDS; frond++) {
			double angle = Math.PI * 2 * frond / FRONDS + wild.nextDouble() * 0.3;
			double length = 150 + wild.nextDouble() * 60;
			this.frond(crown, angle, length, fronds);
		}
		fronds.rock(crown[0], crown[2], crown[1] + 10, 14, 9, 9, 6, 0.6, 0.2, wild);
	}

	/** A frond: it leaves the crown up, arches out and droops down to its tip, thinning as it goes. */
	private void frond(double[] crown, double angle, double length, LowPoly fronds) {
		double[] previous = crown;
		int links = 5;
		for (int link = 1; link <= links; link++) {
			double share = (double) link / links;
			double[] next = { crown[0] + Math.cos(angle) * length * share, crown[1] - 70 * share + 160 * share * share, crown[2] + Math.sin(angle) * length * share };
			fronds.tube(previous, next, 7 - 5.5 * (share - 1.0 / links), 7 - 5.5 * share, 6);
			previous = next;
		}
	}
}
