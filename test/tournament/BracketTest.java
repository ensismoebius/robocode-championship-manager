package tournament;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import tournament.Bracket.Match;
import tournament.Bracket.Status;

class BracketTest {

	@Test
	void importanceFlagsSemifinalsAndFinals() {
		Bracket bracket = new Bracket(ids(8), false, false);
		for (Match m : bracket.getMatches()) {
			Bracket.Importance expected = switch (bracket.roundName(m)) {
			case "Semifinal" -> Bracket.Importance.SEMIFINAL;
			case "Final" -> Bracket.Importance.FINAL;
			default -> Bracket.Importance.REGULAR;
			};
			assertEquals(expected, bracket.importance(m), m.getCode());
		}
		assertEquals(2, bracket.getMatches().stream().filter(m -> bracket.importance(m) == Bracket.Importance.SEMIFINAL).count());
		assertEquals(1, bracket.getMatches().stream().filter(m -> bracket.importance(m) == Bracket.Importance.FINAL).count());
	}

	@Test
	void importanceCountsRepechageFinalsAndGrandFinal() {
		Bracket bracket = new Bracket(ids(8), true, true);
		for (Match m : bracket.getMatches()) {
			boolean big = bracket.roundName(m).startsWith("Final") || bracket.roundName(m).startsWith("Grande");
			assertEquals(big, bracket.importance(m) == Bracket.Importance.FINAL, bracket.roundName(m));
		}
		assertTrue(bracket.getMatches().stream().anyMatch(m -> bracket.importance(m) == Bracket.Importance.SEMIFINAL));
	}

	private static List<Long> ids(int n) {
		return LongStream.rangeClosed(1, n).boxed().toList();
	}

	/**
	 * Plays the whole championship choosing winners at random, checking the
	 * bracket rules on the way
	 *
	 * @return how many matches each participant lost
	 */
	private static Map<Long, Integer> playAll(Bracket bracket, Random random) {
		Map<Long, Integer> losses = new HashMap<Long, Integer>();
		int guard = 0;

		while (bracket.getNextMatch().isPresent()) {
			assertTrue(guard++ < 1000, "Championship never ends");

			// Nobody is in two playable matches at the same time
			Set<Long> busy = new HashSet<Long>();
			for (Match m : bracket.getMatches()) {
				if (m.getStatus() == Status.READY) {
					assertNotEquals(m.getA().getParticipant(), m.getB().getParticipant());
					assertTrue(busy.add(m.getA().getParticipant()), "Participant in two matches: " + m);
					assertTrue(busy.add(m.getB().getParticipant()), "Participant in two matches: " + m);
				}
			}

			Match next = bracket.getNextMatch().get();
			long winner = random.nextBoolean() ? next.getA().getParticipant() : next.getB().getParticipant();
			bracket.record(next.getCode(), winner, 100, 50);
			losses.merge(next.getLoser(), 1, Integer::sum);
		}
		return losses;
	}

	@Test
	void seedingOrderSpreadsTheBestSeeds() {
		assertArrayEquals(new int[] { 1, 2 }, Bracket.seedingOrder(2));
		assertArrayEquals(new int[] { 1, 4, 2, 3 }, Bracket.seedingOrder(4));
		assertArrayEquals(new int[] { 1, 8, 4, 5, 2, 7, 3, 6 }, Bracket.seedingOrder(8));
	}

	@Test
	void singleEliminationForAnyNumberOfParticipants() {
		Random random = new Random(1);
		for (int n = 2; n <= 40; n++) {
			Bracket bracket = new Bracket(ids(n), false, false);
			Map<Long, Integer> losses = playAll(bracket, random);

			assertTrue(bracket.isFinished(), "n=" + n);
			long played = bracket.getMatches().stream().filter(m -> m.getStatus() == Status.FINISHED).count();
			assertEquals(n - 1, played, "n=" + n);

			Long champion = bracket.getChampion();
			assertNull(losses.get(champion));
			for (long p = 1; p <= n; p++) {
				if (p != champion) {
					assertEquals(1, losses.get(p), "n=" + n + " p=" + p);
				}
			}
			assertEquals(n, bracket.getPlacements().size());
			assertEquals(1, bracket.getPlacements().get(champion));
			assertTrue(bracket.getLosersRounds().isEmpty());
			assertTrue(bracket.getFinals().isEmpty());
		}
	}

	@Test
	void repechageGivesEveryoneASecondChance() {
		Random random = new Random(2);
		for (int n = 2; n <= 40; n++) {
			for (int repeat = 0; repeat < 5; repeat++) {
				Bracket bracket = new Bracket(ids(n), true, true);
				Map<Long, Integer> losses = playAll(bracket, random);

				assertTrue(bracket.isFinished(), "n=" + n);
				Long champion = bracket.getChampion();

				// Everybody but the champion is out after exactly two losses
				for (long p = 1; p <= n; p++) {
					if (p == champion) {
						assertTrue(losses.getOrDefault(p, 0) <= 1);
					} else {
						assertEquals(2, losses.get(p), "n=" + n + " p=" + p);
					}
				}

				long played = bracket.getMatches().stream().filter(m -> m.getStatus() == Status.FINISHED).count();
				assertTrue(played == 2L * n - 2 || played == 2L * n - 1, "n=" + n + " played=" + played);

				Map<Long, Integer> placements = bracket.getPlacements();
				assertEquals(n, placements.size(), "n=" + n);
				assertEquals(1, placements.get(champion));
				assertEquals(1, placements.values().stream().filter(v -> v == 2).count(), "one runner up, n=" + n);
				if (n >= 3) {
					assertEquals(1, placements.values().stream().filter(v -> v == 3).count(), "one third place, n=" + n);
				}
			}
		}
	}

	@Test
	void repechageWithoutFinalReset() {
		Random random = new Random(3);
		for (int n = 2; n <= 20; n++) {
			Bracket bracket = new Bracket(ids(n), true, false);
			playAll(bracket, random);
			assertTrue(bracket.isFinished());
			assertEquals(1, bracket.getFinals().size());
			assertEquals(n, bracket.getPlacements().size());
		}
	}

	@Test
	void byesGoToTheFirstSeedsAndAreDecidedAutomatically() {
		// 5 participants: bracket of 8, seeds 1, 2 and 3 get byes
		Bracket bracket = new Bracket(ids(5), true, true);

		List<Match> firstRound = bracket.getWinnersRounds().get(0);
		assertEquals(4, firstRound.size());
		assertEquals(3, firstRound.stream().filter(m -> m.getStatus() == Status.BYE).count());

		Match onlyPlayable = bracket.getNextMatch().orElseThrow();
		assertEquals(Set.of(4L, 5L), Set.of(onlyPlayable.getA().getParticipant(), onlyPlayable.getB().getParticipant()));

		// Seeds 2 and 3 already meet in the second round
		Match semifinal = bracket.getWinnersRounds().get(1).get(1);
		assertEquals(Status.READY, semifinal.getStatus());
		assertEquals(Set.of(2L, 3L), Set.of(semifinal.getA().getParticipant(), semifinal.getB().getParticipant()));
	}

	@Test
	void finalResetOnlyWhenTheRepechageChampionWins() {
		// Two participants: 1 beats 2, 2 comes back through the repechage
		Bracket bracket = new Bracket(ids(2), true, true);
		bracket.record("V1-1", 1, null, null);
		assertEquals(Status.READY, bracket.getMatch("F1").getStatus());

		// Main bracket champion wins the final: no reset
		bracket.record("F1", 1, null, null);
		assertEquals(Status.SKIPPED, bracket.getMatch("F2").getStatus());
		assertEquals(1L, bracket.getChampion());
		assertEquals(Map.of(1L, 1, 2L, 2), bracket.getPlacements());

		// Now the repechage champion wins the final: it is played again
		bracket = new Bracket(ids(2), true, true);
		bracket.record("V1-1", 1, null, null);
		bracket.record("F1", 2, null, null);
		assertFalse(bracket.isFinished());
		assertEquals(Status.READY, bracket.getMatch("F2").getStatus());
		bracket.record("F2", 2, null, null);
		assertEquals(2L, bracket.getChampion());
	}

	@Test
	void placementsWhilePlaying() {
		Bracket bracket = new Bracket(ids(4), false, false);
		bracket.record("V1-1", 1, null, null);
		bracket.record("V1-2", 2, null, null);

		// Two eliminated in the first round share the 3rd place
		assertEquals(Map.of(4L, 3, 3L, 3), bracket.getPlacements());
	}

	@Test
	void rejectsInvalidResults() {
		Bracket bracket = new Bracket(ids(4), true, true);
		assertThrows(IllegalArgumentException.class, () -> bracket.record("X9", 1, null, null));
		assertThrows(IllegalStateException.class, () -> bracket.record("V2-1", 1, null, null));
		assertThrows(IllegalArgumentException.class, () -> bracket.record("V1-1", 2, null, null));
		assertThrows(IllegalArgumentException.class, () -> new Bracket(ids(1), true, true));
		assertThrows(IllegalArgumentException.class, () -> new Bracket(new ArrayList<Long>(List.of(1L, 1L)), true, true));
	}

	@Test
	void podiumHasTheChampionThenTheSecondAndThirdPlaces() {
		for (int n = 3; n <= 12; n++) {
			Bracket bracket = new Bracket(ids(n), true, true);
			assertTrue(bracket.getPodium().isEmpty());
			playAll(bracket, new Random(n));

			List<Long> podium = bracket.getPodium();
			assertEquals(3, podium.size(), "Robots: " + n);
			assertEquals(bracket.getChampion(), podium.get(0));
			assertEquals(3, new HashSet<Long>(podium).size());
			Map<Long, Integer> placements = bracket.getPlacements();
			assertEquals(1, placements.get(podium.get(0)));
			assertEquals(2, placements.get(podium.get(1)));
			assertEquals(3, placements.get(podium.get(2)));
		}
	}
}
