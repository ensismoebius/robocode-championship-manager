package widgets.scenes;

import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The plants of the city of the desert arena, besides the date palms: the trees and shrubs that grow in the Arabian
 * Peninsula and its cities. The ghaf (Prosopis cineraria), the national tree of the United Arab Emirates, has a slim trunk and
 * a wide crown like an umbrella, of a pale blue green. The umbrella thorn acacia (Vachellia tortilis) has a flat crown in
 * layers over a tall trunk that forks. The sidr (Ziziphus spina-christi) is a dense tree of thick branches and a round crown, dark
 * green. And the shrubs of the gardens and the streets: bougainvillea, magenta, oleander, pink, and a grey green desert shrub.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertGreenery {

	/** The kinds of tree, to choose one by a number */
	static final int KINDS = 3;
	private static final int SIDES = 8;
	/** The trees are this many times the size they were drawn for, against the houses and the palms around them */
	private static final double GROWTH = 1.35;

	private final LowPoly bark = new LowPoly();
	private final LowPoly ghaf = new LowPoly();
	private final LowPoly acacia = new LowPoly();
	private final LowPoly sidr = new LowPoly();
	private final LowPoly bougainvillea = new LowPoly();
	private final LowPoly oleander = new LowPoly();
	private final LowPoly scrub = new LowPoly();

	/** A tree of the kind (the number is taken modulo the kinds) with its foot at the point, on the ground at the height given. */
	void tree(int kind, double posX, double ground, double posZ, Random wild) {
		switch (Math.floorMod(kind, KINDS)) {
		case 0 -> this.ghaf(posX, ground, posZ, wild);
		case 1 -> this.acacia(posX, ground, posZ, wild);
		default -> this.sidr(posX, ground, posZ, wild);
		}
	}

	/** A ghaf: a slim trunk that leans a little, under a crown wide as an umbrella, made of three blobs of pale blue green. */
	private void ghaf(double posX, double ground, double posZ, Random wild) {
		double high = (46 + wild.nextDouble() * 16) * GROWTH;
		double lean = (wild.nextDouble() - 0.5) * 8;
		this.bark.tube(new double[] { posX, ground, posZ }, new double[] { posX + lean, ground - high, posZ }, 3.4 * GROWTH, 2.2 * GROWTH, 6);
		this.crown(this.ghaf, posX + lean, posZ, ground - high + 4, 15, 34, 0.6, wild);
		this.crown(this.ghaf, posX + lean - 22 * GROWTH, posZ + 8 * GROWTH, ground - high * 0.8, 12, 23, 0.6, wild);
		this.crown(this.ghaf, posX + lean + 20 * GROWTH, posZ - 10 * GROWTH, ground - high * 0.78, 11, 22, 0.6, wild);
	}

	/** An umbrella thorn acacia: a tall trunk that forks in two, under two flat layers of crown, the lower one the wider. */
	private void acacia(double posX, double ground, double posZ, Random wild) {
		double high = (62 + wild.nextDouble() * 18) * GROWTH;
		double fork = ground - high * 0.55;
		double lean = (wild.nextDouble() - 0.5) * 14;
		this.bark.tube(new double[] { posX, ground, posZ }, new double[] { posX, fork, posZ }, 4 * GROWTH, 3 * GROWTH, 6);
		this.bark.tube(new double[] { posX, fork, posZ }, new double[] { posX + lean - 10 * GROWTH, ground - high, posZ + 6 * GROWTH }, 2.6 * GROWTH, 1.8 * GROWTH, 6);
		this.bark.tube(new double[] { posX, fork, posZ }, new double[] { posX + lean + 10 * GROWTH, ground - high * 0.95, posZ - 6 * GROWTH }, 2.6 * GROWTH, 1.8 * GROWTH, 6);
		this.crown(this.acacia, posX + lean, posZ, ground - high * 0.86, 9, 52, 0.75, wild);
		this.crown(this.acacia, posX + lean + 4, posZ - 3, ground - high * 0.86 - 8 * GROWTH, 8, 36, 0.7, wild);
	}

	/** A sidr: a short twisted trunk with thick branches, under a dense round crown, dark green, of a big blob and two smaller ones. */
	private void sidr(double posX, double ground, double posZ, Random wild) {
		double high = (30 + wild.nextDouble() * 10) * GROWTH;
		double twist = (wild.nextDouble() - 0.5) * 10;
		this.bark.tube(new double[] { posX, ground, posZ }, new double[] { posX + twist, ground - high, posZ }, 5.5 * GROWTH, 3.5 * GROWTH, 6);
		this.crown(this.sidr, posX + twist, posZ, ground - high * 0.7, 34, 38, 0.55, wild);
		this.crown(this.sidr, posX + twist + 18 * GROWTH, posZ + 12 * GROWTH, ground - high * 0.5, 22, 22, 0.55, wild);
		this.crown(this.sidr, posX + twist - 16 * GROWTH, posZ - 12 * GROWTH, ground - high * 0.5, 20, 20, 0.55, wild);
	}

	/** A blob of leaves with its base at the height given, going up, as wide as the radius, and as high as given, both grown like the tree. */
	private void crown(LowPoly leaves, double posX, double posZ, double baseY, double high, double radius, double top, Random wild) {
		leaves.rock(posX, posZ, baseY, high * GROWTH, radius * GROWTH, radius * GROWTH, SIDES, top, 0.35, wild);
	}

	/** A bougainvillea: a mound of magenta, of two blobs, on a bed. */
	void bougainvillea(double posX, double ground, double posZ, Random wild) {
		this.mound(this.bougainvillea, posX, ground, posZ, 16 + wild.nextDouble() * 5, 20, wild);
	}

	/** An oleander: a mound of pink flowers over green. */
	void oleander(double posX, double ground, double posZ, Random wild) {
		this.mound(this.scrub, posX, ground, posZ, 15, 14, wild);
		this.mound(this.oleander, posX, ground - 10, posZ, 12 + wild.nextDouble() * 4, 12, wild);
	}

	/** A shrub of the desert, grey green. */
	void scrub(double posX, double ground, double posZ, Random wild) {
		this.mound(this.scrub, posX, ground, posZ, 10 + wild.nextDouble() * 5, 11, wild);
	}

	/** Two blobs side by side that make a mound of the radius and the height given. */
	private void mound(LowPoly leaves, double posX, double ground, double posZ, double radius, double high, Random wild) {
		leaves.rock(posX - radius * 0.35, posZ, ground, high * 1.3, radius * 1.1, radius * 1.1, 7, 0.6, 0.35, wild);
		leaves.rock(posX + radius * 0.4, posZ + radius * 0.2, ground, high * 1.05, radius * 0.95, radius * 0.95, 7, 0.6, 0.35, wild);
	}

	/** The meshes of the plants that have something in them, each with its colour. */
	List<MeshView> views() {
		return List.of(this.bark.view(ArenaMaterials.material(Color.web("#6b4a2b"))), this.ghaf.view(ArenaMaterials.material(Color.web("#6f9f86"))), this.acacia.view(ArenaMaterials.material(Color.web("#93a24a"))),
				this.sidr.view(ArenaMaterials.material(Color.web("#3d7a3a"))), this.bougainvillea.view(ArenaMaterials.material(Color.web("#d2246b"))), this.oleander.view(ArenaMaterials.material(Color.web("#ef7fa8"))),
				this.scrub.view(ArenaMaterials.material(Color.web("#7f9a68")))).stream().filter(view -> ((TriangleMesh) view.getMesh()).getFaces().size() > 0).toList();
	}
}
