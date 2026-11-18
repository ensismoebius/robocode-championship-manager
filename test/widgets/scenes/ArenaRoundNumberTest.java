package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import support.FxSupport;
import support.TestConfiguration;
import utils.configuration.Configuration;

/** The number of the round: in a knockout a double knockout does not count, the round is run again. */
class ArenaRoundNumberTest {

	/** Three rounds: both robots dead in the first, a winner in the second (and the redone first) and in the third. */
	private static final List<String> OUTPUT = List.of( //
			"#F 800 600 3", "#N 0 sample.Walls", "#N 1 sample.SpinBot", //
			"#R 0", "#T 0 0|0 603 477.9 0.627 0.627 0.627 100 0;1 358.1 273.8 2.972 2.972 2.972 100 0|", "#T 0 90|0 600 470 0.6 0.6 0.6 0 3;1 360 270 3 3 3 0 3|", "#E 0 90", //
			"#R 1", "#T 1 0|0 603 477.9 0.627 0.627 0.627 100 0;1 358.1 273.8 2.972 2.972 2.972 100 0|", "#T 1 90|0 600 470 0.6 0.6 0.6 50 0;1 360 270 3 3 3 0 3|", "#E 1 90", //
			"#R 2", "#T 2 0|0 603 477.9 0.627 0.627 0.627 100 0;1 358.1 273.8 2.972 2.972 2.972 100 0|", "#T 2 90|0 600 470 0.6 0.6 0.6 0 3;1 360 270 3 3 3 50 0|", "#E 2 90");

	@BeforeAll
	static void openTheWindow() {
		FxSupport.gameWindow();
	}

	@AfterAll
	static void backToPoints() {
		TestConfiguration.winnerCriterion = "SCORE";
	}

	/** The numbers of the three rounds, with the given way of choosing the winner. */
	private static List<Integer> numbers(String criterion) {
		TestConfiguration.winnerCriterion = criterion;
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		return FxSupport.run(() -> {
			arena.play(ArenaSimulation.recording(OUTPUT), () -> {
			});
			java.util.ArrayList<Integer> result = new java.util.ArrayList<Integer>();
			for (int round = 0; round < 3; round++) {
				arena.round = round;
				result.add(arena.rounds.roundNumber());
			}
			return result;
		});
	}

	@Test
	void inAKnockoutADoubleKnockoutIsRunAgainWithTheSameNumber() {
		assertEquals(List.of(1, 1, 2), numbers("RANK"));
	}

	@Test
	void inABattleByPointsEveryRoundCounts() {
		assertEquals(List.of(1, 2, 3), numbers("SCORE"));
	}
}
