package tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import model.BattleMoment;

class FlashbacksTest {

	private static BattleMoment moment(String matchCode, long first, long second) {
		BattleMoment moment = new BattleMoment();
		moment.setChampionshipId(1);
		moment.setMatchCode(matchCode);
		moment.setFirstId(first);
		moment.setSecondId(second);
		moment.setImage(new byte[] { 1 });
		return moment;
	}

	private static List<String> codes(List<BattleMoment> moments) {
		List<String> codes = new ArrayList<String>();
		for (BattleMoment moment : moments) {
			codes.add(moment.getMatchCode());
		}
		return codes;
	}

	@Test
	void aRobotSeesOnlyTheBattlesItFought() {
		List<BattleMoment> moments = List.of(moment("V1-1", 1, 2), moment("V1-2", 3, 4), moment("F1", 2, 4));
		assertEquals(List.of("V1-1", "F1"), codes(Flashbacks.of(moments, 2, Flashbacks.LIMIT)));
		assertEquals(List.of(), codes(Flashbacks.of(moments, 9, Flashbacks.LIMIT)));
	}

	@Test
	void everyBattleKeepsItsPhotosWhenThereAreMany() {
		List<BattleMoment> moments = new ArrayList<BattleMoment>();
		for (String match : List.of("V1-1", "V1-2", "V2-1")) {
			for (int i = 0; i < 6; i++) {
				moments.add(moment(match, 1, 2));
			}
		}
		List<String> chosen = codes(Flashbacks.of(moments, 1, 12));
		assertEquals(12, chosen.size());
		for (String match : List.of("V1-1", "V1-2", "V2-1")) {
			assertEquals(4, chosen.stream().filter(match::equals).count(), match);
		}
	}

	@Test
	void photosWithoutPictureAreLeftOut() {
		BattleMoment blank = moment("V1-1", 1, 2);
		blank.setImage(null);
		assertEquals(List.of("V1-2"), codes(Flashbacks.of(List.of(blank, moment("V1-2", 1, 2)), 1, Flashbacks.LIMIT)));
	}

	@Test
	void theOpponentIsTheOtherRobotOfTheBattle() {
		BattleMoment battle = moment("V1-1", 5, 7);
		assertEquals(7, Flashbacks.opponentOf(battle, 5));
		assertEquals(5, Flashbacks.opponentOf(battle, 7));
	}

	@Test
	void spreadKeepsOrderAndTheEnds() {
		List<Integer> items = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
		assertEquals(List.of(0, 5, 9), Flashbacks.spread(items, 3));
		assertEquals(List.of(9), Flashbacks.spread(items, 1));
		assertEquals(List.of(0, 1), Flashbacks.spread(List.of(0, 1), 5));
	}
}
