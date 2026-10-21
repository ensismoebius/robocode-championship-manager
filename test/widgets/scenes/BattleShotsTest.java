package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import utils.robocode.BattleRecording;

/** A battle with shots, hits of every kind and robots that die, so the bullets and the collisions of the arena run. */
class BattleShotsTest {

	private static final List<String> SHOTS = List.of( //
			"#F 800 600 2", //
			"#N 0 sample.Walls", //
			"#N 1 sample.SpinBot", //
			"#R 0", //
			"#T 0 0|0 100 100 0 0 0 100 0;1 500 400 3.14 0 0 100 0|1 0 120 120 2 0 -1;", //
			"#T 0 1|0 100 100 0 0 0 100 0;1 480 380 3.14 0 0 80 0|1 0 200 200 2 0 -1;2 1 450 300 3 0 -1", //
			"#T 0 2|0 100 100 0 0 0 100 0;1 470 370 3.14 0 0 50 0|1 2 400 320 2 2 1;2 1 300 300 3 3 -1;3 0 60 60 1 4 -1", //
			"#T 0 3|0 100 100 0 0 0 100 1;1 470 370 3.14 0 0 0 3|", //
			"#R 1", //
			"#T 1 0|0 10 20 0 0 0 100 0;1 30 40 0 0 0 100 0|4 0 15 15 1 4 -1", //
			"#T 1 1|0 10 20 0 0 0 100 2;1 30 40 0 0 0 100 2|", //
			"#E 1 2");

	@Test
	void aBattleWithShotsAndCollisionsRunsToItsEnd() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");
		BattleRecording recording = ArenaSimulation.recording(SHOTS);

		assertFalse(recording.isEmpty());
		assertTrue(ArenaSimulation.battle(arena, recording, 4000, 0.05));
	}
}
