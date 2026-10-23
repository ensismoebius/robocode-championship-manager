package widgets.scenes;

import static widgets.scenes.ArenaMaterials.skyDome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The Eden Solar arena, Africa, 2187: a great stadium with an opening in its roof to a stormy sky, a round garden
 * in the middle where the robots fight, lush plants and round trees up to the stands, the stands full of people
 * from every continent with their flags, warm lights everywhere, and a glowing tree of life at the mouth of each
 * of the two corridors.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenArena {

	/** How far the garden goes beyond the corners of the field, and the stands beyond the garden */
	private static final double GARDEN_MARGIN = 260;
	private static final double BELT = 340;

	/** How far beyond the front wall of the stands, at the corridors, the baobabs stand */
	private static final double TREE_OFFSET = 190;

	private final BattleArena3D host;
	private final EdenFlags flags;
	private EdenScreens screens;
	/** How far from the middle of the arena the baobabs are, along the x axis */
	private double treeX;
	/** The spots where birds can perch on each baobab, the one on the negative side first */
	private final List<List<double[]>> perches = new ArrayList<>();

	/** The Eden of the arena, drawn in the host scene. */
	EdenArena(BattleArena3D host) {
		this.host = host;
		this.flags = new EdenFlags(host);
	}

	/** Where the baobab at the end of the field on that side is, -1 or 1: on the ground. */
	double[] baobab(int side) {
		return new double[] { side * this.treeX, 0, 0 };
	}

	/** Where the two baobabs are, on the ground, the one on the negative side first. */
	List<double[]> baobabs() {
		return List.of(this.baobab(-1), this.baobab(1));
	}

	/** The spots where birds can perch on each of the baobabs, in the same order. */
	List<List<double[]>> perches() {
		return this.perches;
	}

	/** The breeze: the flags wave, and the screens show the battle. */
	void update(double deltaTime) {
		this.flags.update(deltaTime);
		if (this.screens != null) {
			this.screens.update(deltaTime);
		}
	}

	/** The great screens of the stadium, once it is built. */
	EdenScreens screens() {
		return this.screens;
	}

	/** Builds the whole Eden around a field of the given size. */
	void build(double width, double height) {
		Random wild = new Random(2187);
		double garden = Math.hypot(width, height) / 2 + GARDEN_MARGIN;
		double front = garden + BELT;
		EdenStadium stadium = new EdenStadium(host, front);
		host.arena.getChildren().add(skyDome(EdenTexture.SKY));
		new EdenGarden(host, garden, front).build(width, height, stadium.corridorEnd(), wild);
		stadium.build(wild);
		this.screens = stadium.screens();
		this.flags.build(stadium.concourse() + EdenStadium.CONCOURSE / 2, EdenStadium.concourseHeight(), wild);
		// The trees of life, at the mouths of the corridors, after the floor: their light adds to it
		EdenTree tree = new EdenTree(host);
		this.treeX = front * (1 + EdenStadium.swell(0)) + TREE_OFFSET;
		this.perches.clear();
		for (int side = -1; side <= 1; side += 2) {
			this.perches.add(tree.build(side * this.treeX, wild));
		}
	}
}
