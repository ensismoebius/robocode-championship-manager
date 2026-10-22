package utils.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import model.Opponent;

class BattleControlTest {

	private static Opponent opponent(String team, String name) {
		Opponent o = new Opponent();
		o.setTeam(team);
		o.setName(name);
		return o;
	}

	private static List<Opponent> ranked(List<BattleControl.BattleResult> results) {
		return results.stream().map(BattleControl.BattleResult::opponent).toList();
	}

	@Test
	void ranksByReportedNameNotBySelectionOrder() {
		Opponent first = opponent("alpha", "Bot");
		Opponent second = opponent("beta", "Bot");

		// The second selected robot won
		List<String> output = List.of("1\tbeta.Bot\t1500", "2\talpha.Bot\t300");

		assertEquals(List.of(second, first), ranked(BattleControl.parseRanking(output, List.of(first, second), BattleControl.WinnerCriterion.RANK)));
	}

	@Test
	void ignoresNoiseAndVersionSuffixes() {
		Opponent first = opponent("alpha", "Bot");
		Opponent second = opponent("beta", "sub.Bot");

		List<String> output = List.of("Err> something happened", "", "1\tbeta.sub.Bot 1.0*\t1500", "2\talpha.Bot (1)\t300");

		List<BattleControl.BattleResult> results = BattleControl.parseRanking(output, List.of(first, second), BattleControl.WinnerCriterion.RANK);
		assertEquals(List.of(second, first), ranked(results));
		assertEquals(1500, results.get(0).score());
		assertEquals(300, results.get(1).score());
	}

	@Test
	void linesThatAreNotAPlaceAreLeftOut() {
		Opponent first = opponent("alpha", "Bot");

		// A line with one field, and a place that is not a number: neither is a robot of the ranking
		List<String> output = List.of("garbage", "x\tbeta.Bot\t5", "1\talpha.Bot\t300");

		assertEquals(List.of(first), ranked(BattleControl.parseRanking(output, List.of(first), BattleControl.WinnerCriterion.RANK)));
	}

	@Test
	void winnerByRankOrByScore() {
		Opponent corners = opponent("alpha", "Corners");
		Opponent crazy = opponent("bravo", "Crazy");

		// Real robocode 1.11 output: sorted by score, but Corners won more rounds
		List<String> output = List.of("2\tbravo.Crazy 1.0\t305", "1\talpha.Corners 1.0\t260");

		assertEquals(List.of(corners, crazy), ranked(BattleControl.parseRanking(output, List.of(corners, crazy), BattleControl.WinnerCriterion.RANK)));
		assertEquals(List.of(crazy, corners), ranked(BattleControl.parseRanking(output, List.of(corners, crazy), BattleControl.WinnerCriterion.SCORE)));
	}

	@Test
	void roundsWonDecideNotTheReportedRank() {
		Opponent ramFire = opponent("foxtrot", "RamFire");
		Opponent walls = opponent("charlie", "Walls");

		// Real robocode 1.11 output: RamFire lost the only round with 0 points,
		// and robocode still reported it as rank 1
		List<String> output = List.of("2\tcharlie.Walls 1.0\t180\t1", "1\tfoxtrot.RamFire 1.0\t0\t0");

		List<BattleControl.BattleResult> results = BattleControl.parseRanking(output, List.of(ramFire, walls), BattleControl.WinnerCriterion.RANK);
		assertEquals(List.of(walls, ramFire), ranked(results));
		assertEquals(1, results.get(0).roundsWon());
		assertEquals(List.of(walls, ramFire), ranked(BattleControl.parseRanking(output, List.of(ramFire, walls), BattleControl.WinnerCriterion.SCORE)));

		// More rounds won beats more points
		List<String> moreRounds = List.of("1\tcharlie.Walls\t500\t1", "2\tfoxtrot.RamFire\t300\t2");
		assertEquals(List.of(ramFire, walls), ranked(BattleControl.parseRanking(moreRounds, List.of(ramFire, walls), BattleControl.WinnerCriterion.RANK)));
	}

	@Test
	void tiesAreBrokenByTheOtherCriterion() {
		Opponent a = opponent("alpha", "A");
		Opponent b = opponent("bravo", "B");

		List<String> sameRank = List.of("1\talpha.A\t100", "1\tbravo.B\t200");
		assertEquals(List.of(b, a), ranked(BattleControl.parseRanking(sameRank, List.of(a, b), BattleControl.WinnerCriterion.RANK)));

		List<String> sameScore = List.of("2\talpha.A\t100", "1\tbravo.B\t100");
		assertEquals(List.of(b, a), ranked(BattleControl.parseRanking(sameScore, List.of(a, b), BattleControl.WinnerCriterion.SCORE)));
	}

	@Test
	void bestOfTakesMostOfTheRounds() {
		// Best of 5: three rounds are enough
		assertEquals(3, BattleControl.roundsToWin(5));
		assertEquals(2, BattleControl.roundsToWin(3));
		assertEquals(1, BattleControl.roundsToWin(1));
		// Best of 4 and of 2: more than half of them
		assertEquals(3, BattleControl.roundsToWin(4));
		assertEquals(2, BattleControl.roundsToWin(2));
		// Never zero
		assertEquals(1, BattleControl.roundsToWin(0));
	}

	@Test
	void onlyTheKnockoutEndsWhenSomeoneHasWon() {
		assertTrue(BattleControl.WinnerCriterion.RANK.isKnockout());
		assertFalse(BattleControl.WinnerCriterion.SCORE.isKnockout());

		assertEquals(List.of("5", "-s", "-k"), BattleControl.battleArguments(5, BattleControl.WinnerCriterion.RANK, true));
		assertEquals(List.of("5", "-k"), BattleControl.battleArguments(5, BattleControl.WinnerCriterion.RANK, false));
		// By points every round is fought, there is nothing to tell the starter
		assertEquals(List.of("7", "-s"), BattleControl.battleArguments(7, BattleControl.WinnerCriterion.SCORE, true));
	}

	@Test
	void knockoutWithTheRoundsTiedIsDecidedByPoints() {
		Opponent a = opponent("alpha", "A");
		Opponent b = opponent("bravo", "B");

		// Best of 5 that ended 2 x 2 with a double knockout in the last round: nobody won 3
		List<String> tied = List.of("1\talpha.A\t810\t2", "2\tbravo.B\t905\t2");
		assertEquals(List.of(b, a), ranked(BattleControl.parseRanking(tied, List.of(a, b), BattleControl.WinnerCriterion.RANK)));
	}

	@Test
	void knockoutWinnerWinsEvenWithFewerPoints() {
		Opponent a = opponent("alpha", "A");
		Opponent b = opponent("bravo", "B");

		// B has more points, but A won 3 rounds to 1: the knockout is A's, the points would give B
		List<String> output = List.of("1\tbravo.B\t700\t1", "2\talpha.A\t640\t3");
		assertEquals(List.of(a, b), ranked(BattleControl.parseRanking(output, List.of(a, b), BattleControl.WinnerCriterion.RANK)));
		assertEquals(List.of(b, a), ranked(BattleControl.parseRanking(output, List.of(a, b), BattleControl.WinnerCriterion.SCORE)));
	}
}
