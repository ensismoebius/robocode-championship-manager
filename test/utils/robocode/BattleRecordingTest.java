package utils.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
	void aRobotCanChangeItsColorsDuringTheBattle() {
		BattleRecording recording = BattleRecording.parse(List.of( //
				"#N 0 sample.Crazy", //
				"#R 0", //
				"#C 0 -16777216 -16776961 -14336", //
				"#T 0 0|0 10 20 0 0 0 100 0|", //
				"#T 0 1|0 11 20 0 0 0 100 0|", //
				// Red body from the third turn on
				"#C 0 -65536 -16776961 -14336", //
				"#T 0 2|0 12 20 0 0 0 100 0|"));

		List<Turn> turns = recording.getRounds().get(0);
		assertEquals(0xff000000, turns.get(0).robots().get(0).bodyColor());
		assertEquals(0xff000000, turns.get(1).robots().get(0).bodyColor());
		assertEquals(0xffff0000, turns.get(2).robots().get(0).bodyColor());
		assertEquals(0xff0000ff, turns.get(2).robots().get(0).gunColor());
		assertEquals(0xffffc800, turns.get(2).robots().get(0).radarColor());
		// The robot keeps the colors it started with, for the presentation before the battle
		assertEquals(0xff000000, recording.getRobot(0).getBodyColor());
	}

	@Test
	void aRobotThatNeverReportsItsColorsHasItsOwnInEveryTurn() {
		BattleRecording recording = BattleRecording.parse(List.of("#N 0 sample.Walls", "#R 0", "#T 0 0|0 10 20 0 0 0 100 0|"));

		RobotState state = recording.getRounds().get(0).get(0).robots().get(0);
		assertEquals(recording.getRobot(0).getBodyColor(), state.bodyColor());
		assertEquals(recording.getRobot(0).getRadarColor(), state.radarColor());
	}

	@Test
	void classicBattleHasNoTurns() {
		BattleRecording recording = BattleRecording.parse(List.of("1\tsample.Walls\t216", "2\tsample.SpinBot\t0"));
		assertTrue(recording.isEmpty());
	}

	@Test
	void aRobotThatIsNotInTheRecordingIsNotFound() {
		BattleRecording recording = BattleRecording.parse(List.of("#N 0 sample.Walls"));

		assertNull(recording.getRobot(99));
	}

	@Test
	void aTurnOfALaterRoundCreatesTheRoundsBeforeIt() {
		BattleRecording recording = BattleRecording.parse(List.of("#T 2 0|0 1 2 3 4 5 6 7|"));

		assertEquals(3, recording.getRounds().size());
		assertTrue(recording.getRounds().get(0).isEmpty());
		assertEquals(1, recording.getRounds().get(2).size());
	}
}
