package utils.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The progress of a battle that Robocode simulates, measured by the health the robots lose. */
class BattleProgressTest {

	/** A turn of the round with the energies of two robots */
	private static String turn(int round, int turn, double first, double second) {
		return "#T " + round + " " + turn + "|0 10 20 0 0 0 " + first + " 0;1 30 40 0 0 0 " + second + " 0|";
	}

	@Test
	void theProgressOfARoundIsTheHealthTheWeakestRobotLost() {
		BattleProgress progress = new BattleProgress();
		progress.accept("#F 800 600 1");

		assertEquals(0, progress.accept(turn(0, 0, 100, 100)), 1e-9);
		assertEquals(0.4, progress.accept(turn(0, 1, 100, 60)), 1e-9);
		// The weakest counts, whichever robot it is
		assertEquals(0.7, progress.accept(turn(0, 2, 30, 80)), 1e-9);
		assertEquals(1, progress.accept(turn(0, 3, 30, 0)), 1e-9);
	}

	@Test
	void theRoundsOverCountAndTheNextOneStartsFromItsOwnHealth() {
		BattleProgress progress = new BattleProgress();
		progress.accept("#F 800 600 4");

		assertEquals(0.25, progress.accept(turn(0, 9, 100, 0)), 1e-9);
		// A new round, the robots are whole again: it adds to the first one, it does not undo it
		assertEquals(0.25, progress.accept(turn(1, 0, 100, 100)), 1e-9);
		assertEquals(0.375, progress.accept(turn(1, 5, 50, 100)), 1e-9);
	}

	@Test
	void theProgressNeverGoesBack() {
		BattleProgress progress = new BattleProgress();
		progress.accept("#F 800 600 1");

		assertEquals(0.5, progress.accept(turn(0, 0, 50, 100)), 1e-9);
		// A robot that is healed by a hit does not undo what the battle did
		assertEquals(0.5, progress.accept(turn(0, 1, 80, 100)), 1e-9);
	}

	@Test
	void theLinesThatAreNotTurnsAreIgnored() {
		BattleProgress progress = new BattleProgress();

		assertEquals(0, progress.accept("#N 0 sample.Walls"), 1e-9);
		assertEquals(0, progress.accept("#T broken line"), 1e-9);
		assertEquals(0, progress.accept("#T 0 0|"), 1e-9);
		assertEquals(0, progress.accept("1\tsample.Walls\t216"), 1e-9);
		assertEquals(0, progress.accept("#F wide"), 1e-9);
	}

	@Test
	void theBattleThatEndsIsAllDone() {
		BattleProgress progress = new BattleProgress();
		progress.accept("#F 800 600 3");
		progress.accept(turn(0, 0, 90, 100));

		assertEquals(1, progress.finish(), 1e-9);
		assertTrue(progress.accept(turn(0, 1, 80, 100)) >= 1);
	}
}
