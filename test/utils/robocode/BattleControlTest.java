package utils.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
	void winnerByRankOrByScore() {
		Opponent corners = opponent("alpha", "Corners");
		Opponent crazy = opponent("bravo", "Crazy");

		// Real robocode 1.11 output: sorted by score, but Corners won more rounds
		List<String> output = List.of("2\tbravo.Crazy 1.0\t305", "1\talpha.Corners 1.0\t260");

		assertEquals(List.of(corners, crazy), ranked(BattleControl.parseRanking(output, List.of(corners, crazy), BattleControl.WinnerCriterion.RANK)));
		assertEquals(List.of(crazy, corners), ranked(BattleControl.parseRanking(output, List.of(corners, crazy), BattleControl.WinnerCriterion.SCORE)));
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
}
