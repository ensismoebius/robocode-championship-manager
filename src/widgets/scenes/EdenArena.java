package widgets.scenes;

import static widgets.scenes.ArenaMaterials.skyDome;

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

	private final BattleArena3D host;
	private final EdenFlags flags;

	/** The Eden of the arena, drawn in the host scene. */
	EdenArena(BattleArena3D host) {
		this.host = host;
		this.flags = new EdenFlags(host);
	}

	/** The breeze: the flags wave. */
	void update(double deltaTime) {
		this.flags.update(deltaTime);
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
		this.flags.build(stadium.concourse() + EdenStadium.CONCOURSE / 2, EdenStadium.concourseHeight(), wild);
		// The trees of life, at the mouths of the corridors, after the floor: their light adds to it
		EdenTree tree = new EdenTree(host);
		for (int side = -1; side <= 1; side += 2) {
			tree.build(side * (front + 190), wild);
		}
	}
}
