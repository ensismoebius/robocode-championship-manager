package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import javafx.scene.Node;

import org.junit.jupiter.api.Test;

import support.FxSupport;

/** The critters of each arena: they walk around, run away from the battle, swim in the water and can be followed. */
class CrittersTest {

	/** The critters of the style, built on the JavaFX thread: the callers already run there or wrap it. */
	private static Critters critters(ArenaStyle style, long seed) {
		return new Critters(style, 800, 600, new Random(seed));
	}

	/** Where every critter is on the field, along the x axis and the z axis. */
	private static List<Double> positions(Critters critters) {
		return critters.root().getChildren().stream().flatMap(node -> List.of(node.getTranslateX(), node.getTranslateZ()).stream()).toList();
	}

	/** Moves the critters through the time given, one step at a time. */
	private static void walk(Critters critters, int steps) {
		for (int step = 0; step < steps; step++) {
			critters.update(0.05);
		}
	}

	@Test
	void everyArenaHasItsCreatures() {
		for (ArenaStyle style : ArenaStyle.values()) {
			int count = FxSupport.run(() -> critters(style, 1).root().getChildren().size());

			assertEquals(9, count, style.name());
		}
	}

	@Test
	void theCreaturesWalkAroundTheField() {
		FxSupport.runVoid(() -> {
			Critters critters = critters(ArenaStyle.JUNGLE, 2);
			List<Double> start = positions(critters);

			walk(critters, 200);

			assertNotEquals(start, positions(critters));
		});
	}

	@Test
	void theWaterIsSwumInByThePenguinsOfTheIce() {
		FxSupport.runVoid(() -> {
			Critters critters = critters(ArenaStyle.ICE, 3);
			// The half of the field on the right is a lake
			critters.setWater((posX, posZ) -> posX > 0);
			List<Double> start = positions(critters);

			walk(critters, 300);

			assertNotEquals(start, positions(critters));
		});
	}

	@Test
	void aPlaceToWalkIsFoundEvenWhenTheWaterIsNowhere() {
		FxSupport.runVoid(() -> {
			Critters critters = critters(ArenaStyle.ICE, 7);
			// The penguins want the water, and there is none to find: they walk on the land
			critters.setWater((posX, posZ) -> false);
			List<Double> start = positions(critters);

			walk(critters, 300);

			assertNotEquals(start, positions(critters));
		});
	}

	@Test
	void theCreaturesRunAwayFromAnAlarmOfTheBattle() {
		FxSupport.runVoid(() -> {
			Critters critters = critters(ArenaStyle.VOLCANO, 4);
			// Something happens where the nearest critter is: every one close to it is frightened
			double[] nearest = critters.nearest(0, 0);
			List<Double> start = positions(critters);

			critters.alarm(nearest[0], nearest[2], 400, 1.0);
			walk(critters, 60);

			assertNotEquals(start, positions(critters));
		});
	}

	@Test
	void anAlarmFarFromTheCreaturesFrightensNone() {
		FxSupport.runVoid(() -> {
			Critters critters = critters(ArenaStyle.NEON, 5);
			List<Double> start = positions(critters);

			critters.alarm(100000, 100000, 10, 1.0);
			walk(critters, 1);

			// Only the walking is left, the critters go on to where they were going
			assertEquals(start.size(), positions(critters).size());
		});
	}

	@Test
	void aCritterCanBeFollowedWhereItIsNow() {
		FxSupport.runVoid(() -> {
			Critters critters = critters(ArenaStyle.JUNGLE, 6);

			Supplier<double[]> follow = critters.track(0, 0);
			double[] first = follow.get();
			walk(critters, 100);
			double[] now = follow.get();

			assertNotNull(first);
			assertEquals(3, now.length);
		});
	}

	@Test
	void theBirdsOfTheEdenPerchOnTheBaobabsAndFlyAroundThem() {
		FxSupport.runVoid(() -> {
			Critters critters = critters(ArenaStyle.EDEN_SOLAR, 7);
			List<double[]> trees = List.of(new double[] { -1800, 0, 0 }, new double[] { 1800, 0, 0 });
			critters.setRoosts(trees, List.of(List.of(new double[] { -1800, -560, 40 }), List.of(new double[] { 1800, -560, 40 })));
			List<Node> birds = critters.root().getChildren();

			walk(critters, 1);
			long perched = birds.stream().filter(bird -> Math.abs(Math.abs(bird.getTranslateX()) - 1800) < 1e-9 && Math.abs(bird.getTranslateZ() - 40) < 1e-9).count();

			// Some are perched, still, and the others are flying
			assertTrue(perched >= 1 && perched < birds.size(), "perched: " + perched);

			walk(critters, 400);
			for (Node bird : birds) {
				double nearest = Math.min(Math.hypot(bird.getTranslateX() + 1800, bird.getTranslateZ()), Math.hypot(bird.getTranslateX() - 1800, bird.getTranslateZ()));
				assertTrue(nearest < 700, "a bird went off from the baobabs: " + nearest);
			}
		});
	}
}
