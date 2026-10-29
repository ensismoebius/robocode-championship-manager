package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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

		// Three seconds on the baobab, two on each of three birds, two and a half on the whole region, and longer where the crossfades need it
		assertTrue(tour.duration() >= 3 + 3 * 2 + 2.5);

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
		assertTrue(tour.duration() >= 4.5 + 2);

		// The sweep is from above, looking at the middle of the arena
		FxSupport.runVoid(() -> tour.update(0));
		assertTrue(tour.eye[1] < -500, "the sweep is from above");
		assertEquals(0, tour.look[0], 1e-9);
	}

	@Test
	void theDesertTourGoesToASkyscraperAndDownAnAvenueBeforeTheSweep() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "MIDDLE_EAST");
		FlyTour tour = arena.flyTour;
		FxSupport.runVoid(() -> tour.plan(arena, 1, 1));

		assertTrue(tour.duration() >= 3 + 5 + 4.5 + 2);

		// First the camera looks up at the foot of a skyscraper, from above the palms
		FxSupport.runVoid(() -> tour.update(0));
		double[] seen = { tour.look[0], tour.look[2] };
		assertTrue(arena.desert.towers().stream().anyMatch(tower -> tower[0] == seen[0] && tower[1] == seen[1]), "looks at a skyscraper");
		assertTrue(tour.eye[1] < -300, "above the palms");

		// Then it is low on a street, out beyond the dunes at the end of it
		boolean[] lowOutside = { false };
		FxSupport.runVoid(() -> {
			for (double time = 0; time < tour.duration(); time += 0.1) {
				tour.update(time);
				lowOutside[0] |= tour.eye[1] > -100 && DesertDunes.outside(tour.eye[0], tour.eye[2], arena.fieldWidth, arena.fieldHeight) > DesertDunes.END;
			}
		});
		assertTrue(lowOutside[0], "low in the avenue");
	}

	@Test
	void theRobotsArePresentedWhileTheBattleLoadsAndTheCameraToursWhenTheyAreAllPresentedBeforeItIsReady() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");

		FxSupport.runVoid(() -> {
			assertEquals(Phase.LOADING, arena.phase);
			arena.showcase.startPreview();
			// The versus comes first, with both robots on the screen, though there is no recording yet
			assertEquals(Phase.SHOWCASE, arena.phase);
			assertTrue(arena.versus);
			assertEquals(2, arena.robots.size());
			assertEquals(2, arena.robots.values().stream().filter(view -> view.root.isVisible()).count());
			assertTrue(arena.robots.get(0).position()[0] < 0 && arena.robots.get(1).position()[0] > 0, "one on each side");

			// Then the first robot is presented, alone on the screen
			arena.showcase.endVersus();
			assertFalse(arena.versus);
			assertEquals(1, arena.robots.values().stream().filter(view -> view.root.isVisible()).count());

			// All were presented and the battle is not ready: the camera tours, with no robot on the screen
			arena.showcase.showcaseRobot(2);
			assertEquals(Phase.LOADING, arena.phase);
			assertEquals(0, arena.robots.values().stream().filter(view -> view.root.isVisible()).count());
			double[] start = arena.eye.clone();
			arena.phaseTime = 4;
			arena.director.updateCamera(0.05);
			arena.director.updateCamera(5);
			assertTrue(Math.abs(arena.eye[0] - start[0]) + Math.abs(arena.eye[2] - start[2]) > 100, "the camera moved");

			// The battle is ready: the fly by has the rest of the tour
			arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
			});
			assertEquals(Phase.FLYBY, arena.phase);
			assertEquals(arena.flyTour.passEnd() - 4, arena.rounds.flyByHold(), 1e-9);
		});
	}

	@Test
	void whenTheBattleIsReadyTheCameraGoesOnFromWhereItIsWithNoCut() {
		for (double waited : new double[] { 4, 20, 31.7 }) {
			BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");

			FxSupport.runVoid(() -> {
				arena.showcase.startPreview();
				arena.showcase.showcaseRobot(2);
				// It may have gone around the tour more than once
				arena.phaseTime = waited;
				arena.director.updateCamera(0.05);
				double[] before = arena.eyeTarget.clone();
				double[] lookBefore = arena.lookTarget.clone();
				double[] camera = arena.eye.clone();
				arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
				});

				assertEquals(Phase.FLYBY, arena.phase);
				// The tour goes on, to the end of its pass: never longer than a whole one
				assertTrue(arena.rounds.flyByHold() >= 0 && arena.rounds.flyByHold() < arena.flyTour.duration(), "waited " + waited);
				// Where the camera aims at is where it aimed at, and the camera itself was not moved by a cut
				for (int axis = 0; axis < 3; axis++) {
					assertEquals(camera[axis], arena.eye[axis], 1e-6, "no cut " + waited);
				}
				for (int axis = 0; axis < 3; axis++) {
					assertEquals(before[axis], arena.eyeTarget[axis], 1e-6, "eye " + waited);
					assertEquals(lookBefore[axis], arena.lookTarget[axis], 1e-6, "look " + waited);
				}
			});
		}
	}

	@Test
	void aBattleReadyBeforeThePresentationEndsDoesNotCutItAndTheFlyByHasTheWholeTour() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");

		FxSupport.runVoid(() -> {
			arena.showcase.startPreview();
			RobotView presented = arena.robots.get(0);
			arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
			});

			// Still in the versus, whose robots are the ones of the recording, and then the first one is presented
			assertEquals(Phase.SHOWCASE, arena.phase);
			assertTrue(arena.versus);
			arena.showcase.endVersus();
			assertFalse(arena.versus);
			assertEquals(0, arena.showcaseOrder);
			assertSame(presented, arena.robots.get(arena.recording.getRobots().get(0).getIndex()));
			assertEquals(arena.flyTour.duration(), arena.rounds.flyByHold(), 1e-9);
		});
	}

	@Test
	void aBattleReadyBeforeTheArenaIsShownStartsThePresentationAsBefore() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");

		FxSupport.runVoid(() -> {
			arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
			});
			assertEquals(Phase.SHOWCASE, arena.phase);
			assertTrue(arena.versus);
			assertEquals(arena.recording.getRobots().size(), arena.robots.size());
		});
	}

	/** The most the camera moves between two frames of 20 milliseconds, or the point it looks at, over a few turns of the tour that goes around. */
	private static double biggestStep(FlyTour tour) {
		double[] before = new double[6];
		double biggest = 0;
		for (double time = 0; time < tour.duration() * 2.5; time += 0.02) {
			tour.update(time, true);
			double step = Math.max(Math.hypot(tour.eye[0] - before[0], Math.hypot(tour.eye[1] - before[1], tour.eye[2] - before[2])),
					Math.hypot(tour.look[0] - before[3], Math.hypot(tour.look[1] - before[4], tour.look[2] - before[5])));
			biggest = time == 0 ? 0 : Math.max(biggest, step);
			System.arraycopy(tour.eye, 0, before, 0, 3);
			System.arraycopy(tour.look, 0, before, 3, 3);
		}
		return biggest;
	}

	@Test
	void theTourIsSmoothFromStopToStopAndWhenItGoesAround() {
		for (String style : new String[] { "MIDDLE_EAST", "JUNGLE", "EDEN_SOLAR" }) {
			FlyTour tour = tourOf(style);
			double[] biggest = new double[1];
			FxSupport.runVoid(() -> biggest[0] = biggestStep(tour));
			assertTrue(biggest[0] < 40, style + " moves " + biggest[0] + " in a frame");
		}
	}

	@Test
	void theEffectsOfThePresentationAreHeardOnlyOnceTheBattleIsReady() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");

		FxSupport.runVoid(() -> {
			arena.showcase.startPreview();
			assertFalse(arena.showcase.effectsOn(), "silent while the battle loads");

			arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
			});
			assertTrue(arena.showcase.effectsOn());
		});
	}
}
