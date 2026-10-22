package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import support.FxSupport;

/** The director of the battle: the cuts to the victim of a strong hit, the camera kept clear of the robots, and the choice of the shots. */
class ArenaCameraTest {

	@Test
	void aStrongHitCutsToItsVictimAndTheCameraKeepsClearOfTheRobots() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "NEON");
		FxSupport.show(arena);
		ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 60, 0.05);

		FxSupport.runVoid(() -> {
			arena.viewMode = ViewMode.CINEMATIC;
			arena.phase = Phase.PLAYING;
			arena.shotTime = 2;
			// A hit with a power that always cuts, to the first robot of the battle
			arena.director.impactCut(arena.robots.keySet().iterator().next(), 10);
		});
		assertEquals(Shot.IMPACT, FxSupport.run(() -> arena.shot));

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.values().iterator().next();
			double[] position = robot.position();
			// The eye of the camera right on the robot: it is pushed out to the minimum distance
			arena.eyeTarget[0] = position[0];
			arena.eyeTarget[1] = -50;
			arena.eyeTarget[2] = position[2];
			arena.director.keepClear();

			double distance = Math.hypot(arena.eyeTarget[0] - position[0], arena.eyeTarget[2] - position[2]);
			assertEquals(65, distance, 1e-6);
		});
	}

	@Test
	void theShotsOfTheBattleAreChosenEvenUnderTension() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "NEON");
		FxSupport.show(arena);
		ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 60, 0.05);

		FxSupport.runVoid(() -> {
			arena.viewMode = ViewMode.FREE;
			arena.yawSpeed = 100;
		});
		ArenaSimulation.frames(arena, 5, 0.05);
		FxSupport.runVoid(() -> {
			arena.hitTension = 1;
			// Under tension the wide shot is replaced by the close one, every time
			for (int pick = 0; pick < 200; pick++) {
				arena.director.pickShot(pick % 2 == 0);
				assertNotEquals(Shot.WIDE, arena.shot);
			}
			arena.phase = Phase.PLAYING;
			arena.director.placeBalloons();
		});
	}
}
