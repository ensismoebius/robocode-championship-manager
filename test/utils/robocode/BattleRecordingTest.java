package utils.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;

/**
 * The turns printed by RobocodeStarter/BattleStarter -s (lines copied from a
 * real battle)
 */
class BattleRecordingTest {

	private static final List<String> OUTPUT = List.of( //
			"#F 800 600 2", //
			"#N 0 sample.Walls", //
			"#N 1 sample.SpinBot", //
			"#R 0", //
			"#C 0 -16777216 -16777216 -14336", //
			"#C 1 -16776961 -16776961 -16777216", //
			"#T 0 0|0 603 477.9 0.627 0.627 0.627 100 0;1 358.1 273.8 2.972 2.972 2.972 100 0|", //
			"some robocode log line", //
			"#T 0 199|0 450 18 4.712 6.283 6.283 94 0;1 270.3 277.8 6.026 6.026 6.026 91 0|4 0 163.3 374.3 2 1 -1;10004 1 654 93.3 3 2 0", //
			"#E 0 1495", //
			"#R 1", //
			"#T 1 0|0 10 20 0 0 0 100 0;1 30 40 0 0 0 0 3|", //
			"#T broken line", //
			"1\tsample.Walls\t216", //
			"2\tsample.SpinBot\t0");

	@Test
	void readsTheTurns() {
		BattleRecording recording = BattleRecording.parse(OUTPUT);

		assertEquals(800, recording.getWidth());
		assertEquals(600, recording.getHeight());
		assertEquals(2, recording.getRobots().size());
		assertEquals("sample.SpinBot", recording.getRobot(1).getName());
		// Blue body, as the robot chose
		assertEquals(0xff0000ff, recording.getRobot(1).getBodyColor());

		assertEquals(2, recording.getRounds().size());
		List<Turn> first = recording.getRounds().get(0);
		assertEquals(2, first.size());

		Turn turn = first.get(1);
		assertEquals(199, turn.turn());
		RobotState walls = turn.robots().get(0);
		assertEquals(450, walls.x(), 1e-9);
		assertEquals(18, walls.y(), 1e-9);
		assertEquals(4.712, walls.bodyHeading(), 1e-9);
		assertEquals(94, walls.energy(), 1e-9);

		assertEquals(2, turn.bullets().size());
		Bullet hit = turn.bullets().get(1);
		assertEquals(10004, hit.id());
		assertEquals(1, hit.owner());
		assertTrue(hit.isHit());
		assertEquals(0, hit.victim());
		assertFalse(turn.bullets().get(0).isHit());

		// The broken line is skipped, the rest of the round is kept
		RobotState dead = recording.getRounds().get(1).get(0).robots().get(1);
		assertTrue(dead.isDead());
	}

	@Test
	void classicBattleHasNoTurns() {
		BattleRecording recording = BattleRecording.parse(List.of("1\tsample.Walls\t216", "2\tsample.SpinBot\t0"));
		assertTrue(recording.isEmpty());
	}
}
