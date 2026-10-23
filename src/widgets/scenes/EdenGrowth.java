package widgets.scenes;

import static widgets.scenes.ArenaMaterials.material;

import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * What grows on the stadium of the Eden Solar arena, so it looks like a part of nature and not a building: ribs
 * of wood that arch over the crowd from the concourse up to the roof like the limbs of a tree, vines hanging from
 * the opening of the roof and from its edge, a fringe of leaves around the opening, and hedges in flower along
 * the front wall and the concourse.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenGrowth {

	private static final int RIBS = 16;
	private static final int STEPS = 8;
	private static final String[] TONES = { "#2f7a2a", "#3a8f35", "#25662a" };
	private static final String[] BLOSSOMS = { "#ff6fa3", "#f2efe6", "#f2c14e", "#e8505b" };

	private final BattleArena3D host;
	private final EdenStadium stadium;
	/** The leaves and the flowers of all the growth, in their tones, drawn together at the end */
	private final LowPoly[] leaves = new LowPoly[TONES.length];
	private final LowPoly[] flowers = new LowPoly[BLOSSOMS.length];

	/** The growth on a stadium, drawn in the host scene. */
	EdenGrowth(BattleArena3D host, EdenStadium stadium) {
		this.host = host;
		this.stadium = stadium;
		for (int index = 0; index < leaves.length; index++) {
			leaves[index] = new LowPoly();
		}
		for (int index = 0; index < flowers.length; index++) {
			flowers[index] = new LowPoly();
		}
	}

	/** Grows everything. */
	void build(Random wild) {
		this.ribs(wild);
		this.vines(wild);
		this.fringe(wild);
		this.hedges(wild);
		for (int index = 0; index < leaves.length; index++) {
			PhongMaterial green = material(Color.web(TONES[index]));
			green.setSpecularColor(Color.gray(0.1));
			host.arena.getChildren().add(leaves[index].view(green));
		}
		for (int index = 0; index < flowers.length; index++) {
			host.arena.getChildren().add(flowers[index].view(material(Color.web(BLOSSOMS[index]))));
		}
	}

	/** A clump of leaves of the given size on the point, which is where its lower middle is. */
	private void leaf(Random wild, double[] point, double size) {
		leaves[wild.nextInt(leaves.length)].rock(point[0], point[2], point[1] + size * 0.3, size * 0.7, size, size, 6, 0.6, 0.45, wild);
	}

	/** A few small flowers on the top of a clump of leaves of the given size. */
	private void bloom(Random wild, double[] point, double size) {
		for (int flower = 0; flower < 4; flower++) {
			double angle = wild.nextDouble() * Math.PI * 2;
			double reach = wild.nextDouble() * size * 0.6;
			flowers[wild.nextInt(flowers.length)].rock(point[0] + Math.cos(angle) * reach, point[2] + Math.sin(angle) * reach, point[1] - size * 0.5, 3.5, 3, 3, 5, 0.5, 0.4, wild);
		}
	}

	/** How thick a rib is at that share of its length: stout where it grows from the floor, thinner on the roof. */
	private static double thickness(double share) {
		return 18 - 10 * share;
	}

	/**
	 * Ribs of wood that grow from the back of the concourse and arch over the crowd up to the edge of the roof, with
	 * clumps of leaves along them. They rise fast at first and then lean over the seats, always above the heads, and
	 * there are none in front of the corridors.
	 */
	private void ribs(Random wild) {
		LowPoly wood = new LowPoly();
		double start = stadium.concourse() + EdenStadium.CONCOURSE - 10;
		double floor = EdenStadium.concourseHeight();
		double lift = EdenStadium.roofBase() + 10 - floor;
		double reach = stadium.outerEdge() - 10 - start;
		for (int rib = 0; rib < RIBS; rib++) {
			double angle = Math.PI * 2 * (rib + 0.5) / RIBS;
			if (Math.abs(Math.sin(angle)) < 0.3) {
				continue;
			}
			double[] previous = EdenStadium.point(angle, start, floor);
			for (int step = 1; step <= STEPS; step++) {
				double share = (double) step / STEPS;
				// Each rib turns a little as it climbs
				double turn = angle + 0.05 * Math.sin(share * Math.PI * 2);
				double[] next = EdenStadium.point(turn, start + reach * share, floor + lift * Math.pow(share, 0.55));
				wood.tube(previous, next, thickness(share - 1.0 / STEPS), thickness(share), 6);
				this.leaf(wild, next, 14 + wild.nextDouble() * 12);
				previous = next;
			}
		}
		host.arena.getChildren().add(wood.view(material(Color.web("#6b4a32"))));
	}

	/** A vine hanging from a point of the roof, swaying a little, with a leaf at each of its links. */
	private void vine(LowPoly stems, Random wild, double[] top, double length) {
		int links = 3;
		double[] previous = top;
		for (int link = 1; link <= links; link++) {
			double[] next = { previous[0] + (wild.nextDouble() - 0.5) * 12, top[1] + length * link / links, previous[2] + (wild.nextDouble() - 0.5) * 12 };
			stems.tube(previous, next, 1.4, 1.0, 4);
			this.leaf(wild, next, 5 + wild.nextDouble() * 4);
			previous = next;
		}
	}

	/** Long vines hang inside the opening of the roof, and short ones from its outer edge, over the panels. */
	private void vines(Random wild) {
		LowPoly stems = new LowPoly();
		for (int index = 0; index < 260; index++) {
			double[] top = EdenStadium.point(wild.nextDouble() * Math.PI * 2, stadium.opening() - 4 - wild.nextDouble() * 6, EdenStadium.rim() + 2);
			this.vine(stems, wild, top, 60 + wild.nextDouble() * 150);
		}
		for (int index = 0; index < 320; index++) {
			double[] top = EdenStadium.point(wild.nextDouble() * Math.PI * 2, stadium.outerEdge() - 2.5, EdenStadium.roofBase() + 2);
			this.vine(stems, wild, top, 20 + wild.nextDouble() * 60);
		}
		host.arena.getChildren().add(stems.view(material(Color.web("#2c5a26"))));
	}

	/** A fringe of leaves around the opening of the roof. */
	private void fringe(Random wild) {
		int clumps = 72;
		for (int index = 0; index < clumps; index++) {
			double angle = Math.PI * 2 * (index + wild.nextDouble() * 0.4) / clumps;
			this.leaf(wild, EdenStadium.point(angle, stadium.opening() - 2, EdenStadium.rim() + 6), 16 + wild.nextDouble() * 10);
		}
	}

	/** Hedges, some in flower, on the edge of the first row and on the edge of the concourse, but not at the corridors. */
	private void hedges(Random wild) {
		for (int segment = 0; segment < EdenStadium.SEGMENTS; segment += 2) {
			if (EdenStadium.inCorridor(segment)) {
				continue;
			}
			double angle = EdenStadium.angle(segment) + Math.PI / EdenStadium.SEGMENTS;
			double[] row = EdenStadium.point(angle, stadium.front() + 6, -30);
			double[] rim = EdenStadium.point(angle, stadium.concourse() - 7, EdenStadium.concourseHeight());
			this.leaf(wild, row, 20);
			this.leaf(wild, rim, 14);
			if (wild.nextInt(4) == 0) {
				this.bloom(wild, row, 20);
			}
			if (wild.nextInt(4) == 0) {
				this.bloom(wild, rim, 14);
			}
		}
	}
}
