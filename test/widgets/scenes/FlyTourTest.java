package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import support.FxSupport;

/** The tour at the start of the fly by: the baobab, three birds and the region in the Eden Solar, a sweep and a critter elsewhere, and the city first in the desert. */
class FlyTourTest {

	/** The tour of an arena of the style, as planned for a fly by that looks from the first side of both axes. */
	private static FlyTour tourOf(String style) {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), style);
		FlyTour tour = arena.flyTour;
		FxSupport.runVoid(() -> tour.plan(arena, 1, 1));
		return tour;
	}

	@Test
	void theEdenTourGoesToTheBaobabThreeBirdsAndTheRegion() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");
		FlyTour tour = arena.flyTour;
		FxSupport.runVoid(() -> tour.plan(arena, 1, 1));

		// Three seconds on the baobab, two on each of three birds, two and a half on the whole region
		assertEquals(3 + 3 * 2 + 2.5, tour.duration(), 1e-9);

		double[] baobab = arena.eden.baobab(1);
		FxSupport.runVoid(() -> tour.update(1));
		assertEquals(baobab[0], tour.look[0], 1e-9);
		assertTrue(tour.look[1] < -200, "looks up at the baobab, not at the ground");

		FxSupport.runVoid(() -> tour.update(tour.duration()));
		assertEquals(0, tour.look[0], 1e-9);
		assertTrue(tour.eye[1] < -1500, "the view over the region is from high above");
	}

	@Test
	void theBirdsOfTheTourAreTheOnesNearTheBaobab() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");
		FlyTour tour = arena.flyTour;
		FxSupport.runVoid(() -> tour.plan(arena, 1, 1));
		double[] baobab = arena.eden.baobab(1);

		// The second stop is the first bird: the camera is near the tree, and looking at something near it too
		FxSupport.runVoid(() -> tour.update(3.5));

		assertTrue(Math.hypot(tour.look[0] - baobab[0], tour.look[2] - baobab[2]) < 600);
	}

	@Test
	void otherArenasSweepAroundAndStopOnOneCritter() {
		FlyTour tour = tourOf("JUNGLE");

		assertFalse(tour.isEmpty());
		assertEquals(4.5 + 2, tour.duration(), 1e-9);

		// The sweep is from above, looking at the middle of the arena
		FxSupport.runVoid(() -> tour.update(1));
		assertTrue(tour.eye[1] < -500, "the sweep is from above");
		assertEquals(0, tour.look[0], 1e-9);
	}

	@Test
	void theDesertTourGoesToASkyscraperAndDownAnAvenueBeforeTheSweep() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "MIDDLE_EAST");
		FlyTour tour = arena.flyTour;
		FxSupport.runVoid(() -> tour.plan(arena, 1, 1));

		assertEquals(3 + 5 + 4.5 + 2, tour.duration(), 1e-9);

		// First the camera looks up at the foot of a skyscraper, from above the palms
		FxSupport.runVoid(() -> tour.update(1));
		double[] seen = { tour.look[0], tour.look[2] };
		assertTrue(arena.desert.towers().stream().anyMatch(tower -> tower[0] == seen[0] && tower[1] == seen[1]), "looks at a skyscraper");
		assertTrue(tour.eye[1] < -300, "above the palms");

		// Then it is low on a street, and by its end it is out beyond the dunes, high
		FxSupport.runVoid(() -> tour.update(3 + 2.5));
		assertTrue(tour.eye[1] > -150, "low in the avenue");
		FxSupport.runVoid(() -> tour.update(3 + 5 - 1e-6));
		assertTrue(DesertDunes.outside(tour.eye[0], tour.eye[2], arena.fieldWidth, arena.fieldHeight) > DesertDunes.END, "ends beyond the dunes");
	}
}
