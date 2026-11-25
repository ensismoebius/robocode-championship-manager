package tournament;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

class BracketTest {

	@Test
	void importanceFlagsSemifinalsAndFinals() {
		Bracket bracket = new Bracket(ids(8), false);
		for (Match m : bracket.getMatches()) {
			Importance expected = switch (bracket.roundName(m)) {
			case "Semifinal" -> Importance.SEMIFINAL;
			case "Final" -> Importance.FINAL;
			default -> Importance.REGULAR;
			};
			assertEquals(expected, bracket.importance(m), m.getCode());
		}
		assertEquals(2, bracket.getMatches().stream().filter(m -> bracket.importance(m) == Importance.SEMIFINAL).count());
		assertEquals(1, bracket.getMatches().stream().filter(m -> bracket.importance(m) == Importance.FINAL).count());
	}

	@Test
	void importanceCountsRepechageFinalsAndGrandFinal() {
		Bracket bracket = new Bracket(ids(8), true);
		for (Match m : bracket.getMatches()) {
			boolean big = bracket.roundName(m).startsWith("Fim da") || bracket.roundName(m).startsWith("Grande");
			assertEquals(big, bracket.importance(m) == Importance.FINAL, bracket.roundName(m));
		}
		assertTrue(bracket.getMatches().stream().anyMatch(m -> bracket.importance(m) == Importance.SEMIFINAL));
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

	/** How many challenges the bracket has: one in each round with an odd number of participants. */
	private static long challenges(Bracket bracket) {
		return bracket.getMatches().stream().filter(Match::isChallenge).count();
	}

	/**
	 * Checks, on a finished bracket, that everyone played the first round and that nobody went on without beating someone: whoever
	 * plays a match that is not of the first round won the match before it, or lost the first match of the round and plays its
	 * challenge.
	 */
	private static void nobodyGoesOnWithoutWinning(Bracket bracket) {
		Set<Long> firstRound = new HashSet<Long>();
		for (Match m : bracket.getWinnersRounds().get(0)) {
			if (!m.isChallenge()) {
				firstRound.add(m.getA().getParticipant());
				firstRound.add(m.getB().getParticipant());
			} else {
				firstRound.add(m.getA().getParticipant());
			}
		}
		assertEquals(new HashSet<Long>(bracket.getParticipants()), firstRound, "Everyone plays the first round");
		for (Match m : bracket.getMatches()) {
			for (Slot slot : List.of(m.getA(), m.getB())) {
				Match source = slot.getSource();
				if (source == null) {
					continue;
				}
				if (slot.isSourceWinner()) {
					assertEquals(source.getWinner(), slot.getParticipant(), m.getCode());
				} else {
					// A loser goes on only to the repechage, to the grand final, or to the challenge of its round
					assertTrue(m.getSide() != Side.WINNERS || m.isChallenge(), "Loser going on in the main bracket: " + m.getCode());
				}
			}
		}
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
			Bracket bracket = new Bracket(ids(n), false);
			Map<Long, Integer> losses = playAll(bracket, random);

			assertTrue(bracket.isFinished(), "n=" + n);
			nobodyGoesOnWithoutWinning(bracket);
			// Each match puts one out, but the challenges, which give the loser of the first match of the round another chance
			long played = bracket.getMatches().stream().filter(m -> m.getStatus() == Status.FINISHED).count();
			assertEquals(n - 1 + challenges(bracket), played, "n=" + n);
			assertEquals(n == (Integer.highestOneBit(n)), challenges(bracket) == 0, "n=" + n);

			Long champion = bracket.getChampion();
			assertTrue(losses.getOrDefault(champion, 0) <= 1);
			for (long p = 1; p <= n; p++) {
				if (p != champion) {
					assertTrue(losses.get(p) >= 1, "n=" + n + " p=" + p);
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
				Bracket bracket = new Bracket(ids(n), true);
				Map<Long, Integer> losses = playAll(bracket, random);

				assertTrue(bracket.isFinished(), "n=" + n);
				nobodyGoesOnWithoutWinning(bracket);
				Long champion = bracket.getChampion();

				// Everybody but the finalists is out after two losses (or three, for who lost the first match of a round and then its
				// challenge). The grand final has no rematch: the runner up lost at least once
				Long runnerUp = bracket.getPlacements().entrySet().stream().filter(e -> e.getValue() == 2).map(Map.Entry::getKey).findFirst().orElseThrow();
				for (long p = 1; p <= n; p++) {
					if (p == runnerUp) {
						assertTrue(losses.get(p) >= 1, "n=" + n);
					} else if (p != champion) {
						assertTrue(losses.get(p) >= 2, "n=" + n + " p=" + p);
					}
				}

				// Whoever loses in the main bracket goes to the repechage (or, with two robots, straight to the grand final)
				Set<Long> secondChance = new HashSet<Long>();
				for (Match m : bracket.getMatches()) {
					if (m.getSide() != Side.WINNERS) {
						secondChance.add(m.getA().getParticipant());
						secondChance.add(m.getB().getParticipant());
					}
				}
				for (Match m : bracket.getMatches()) {
					if (m.getSide() == Side.WINNERS) {
						assertTrue(secondChance.contains(m.getLoser()), "n=" + n + " " + m.getCode());
					}
				}

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
	void grandFinalDecidesTheChampionWithoutRematch() {
		Random random = new Random(3);
		for (int n = 2; n <= 20; n++) {
			Bracket bracket = new Bracket(ids(n), true);
			playAll(bracket, random);
			assertTrue(bracket.isFinished());
			assertEquals(1, bracket.getFinals().size());
			assertEquals(n, bracket.getPlacements().size());
		}
	}

	@Test
	void theRoundsAreNamedFromTheFinalBackwards() {
		Bracket sixteen = new Bracket(ids(16), false);

		assertEquals("Oitavas de final", sixteen.winnersRoundName(1));
		assertEquals("Quartas de final", sixteen.winnersRoundName(2));
		assertEquals("Semifinal", sixteen.winnersRoundName(3));
		assertEquals("Final", sixteen.winnersRoundName(4));
		// With the repechage the main bracket and the repechage end in finals of their own
		Bracket repechage = new Bracket(ids(16), true);
		assertEquals("Fim da chave principal", repechage.winnersRoundName(4));
		assertEquals("Fim da repescagem", repechage.losersRoundName(repechage.getLosersRounds().size()));
		// A bigger bracket counts its first rounds by number
		assertEquals("1ª rodada", new Bracket(ids(32), false).winnersRoundName(1));
	}

	@Test
	void theOneWithoutAPairChallengesTheLoserOfTheFirstMatch() {
		// 5 participants: two matches and a challenge in the first round, nobody goes on without playing
		Bracket bracket = new Bracket(ids(5), true);

		List<Match> firstRound = bracket.getWinnersRounds().get(0);
		assertEquals(3, firstRound.size());
		assertTrue(bracket.getMatches().stream().allMatch(m -> m.getStatus() != Status.FINISHED));
		Match challenge = firstRound.get(2);
		assertTrue(challenge.isChallenge());
		assertEquals(Status.WAITING, challenge.getStatus());
		assertEquals(3L, challenge.getA().getParticipant());

		// The loser of the first match plays the challenge
		bracket.record("V1-1", 1, null, null);
		assertEquals(Status.READY, challenge.getStatus());
		assertEquals(Set.of(3L, 4L), Set.of(challenge.getA().getParticipant(), challenge.getB().getParticipant()));

		// Who loses the challenge goes to the repechage; who wins goes on
		bracket.record("V1-3", 3, null, null);
		bracket.record("V1-2", 2, null, null);
		Match repechage = bracket.getLosersRounds().get(0).get(0);
		assertEquals(Set.of(4L, 5L), Set.of(repechage.getA().getParticipant(), repechage.getB().getParticipant()));
		assertEquals(Set.of(1L, 2L), Set.of(bracket.getMatch("V2-1").getA().getParticipant(), bracket.getMatch("V2-1").getB().getParticipant()));
		assertEquals(3L, bracket.getMatch("V2-2").getA().getParticipant());
	}

	@Test
	void repechageChampionWinningTheGrandFinalIsTheChampion() {
		// Two participants: 1 beats 2, 2 comes back through the repechage
		Bracket bracket = new Bracket(ids(2), true);
		bracket.record("V1-1", 1, null, null);
		assertEquals(Status.READY, bracket.getMatch("F1").getStatus());
		bracket.record("F1", 2, null, null);
		assertTrue(bracket.isFinished());
		assertNull(bracket.getMatch("F2"));
		assertEquals(2L, bracket.getChampion());
		assertEquals(Map.of(2L, 1, 1L, 2), bracket.getPlacements());
	}

	@Test
	void placementsWhilePlaying() {
		Bracket bracket = new Bracket(ids(4), false);
		bracket.record("V1-1", 1, null, null);
		bracket.record("V1-2", 2, null, null);

		// Two eliminated in the first round share the 3rd place
		assertEquals(Map.of(4L, 3, 3L, 3), bracket.getPlacements());
	}

	@Test
	void rejectsInvalidResults() {
		Bracket bracket = new Bracket(ids(4), true);
		assertThrows(IllegalArgumentException.class, () -> bracket.record("X9", 1, null, null));
		assertThrows(IllegalStateException.class, () -> bracket.record("V2-1", 1, null, null));
		assertThrows(IllegalArgumentException.class, () -> bracket.record("V1-1", 2, null, null));
		assertThrows(IllegalArgumentException.class, () -> new Bracket(ids(1), true));
		assertThrows(IllegalArgumentException.class, () -> new Bracket(new ArrayList<Long>(List.of(1L, 1L)), true));
	}

	@Test
	void podiumHasTheChampionThenTheSecondAndThirdPlaces() {
		for (int n = 3; n <= 12; n++) {
			Bracket bracket = new Bracket(ids(n), true);
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

	@Test
	void aSlotWithoutSourceIsDrawnAndTheRoundsHaveNames() {
		Bracket five = new Bracket(ids(5), false);
		assertTrue(five.getWinnersRounds().get(0).stream().allMatch(m -> m.getA().describeSource().equals("Sorteado")));

		Bracket thirtyTwo = new Bracket(ids(32), false);
		assertEquals("1ª rodada", thirtyTwo.winnersRoundName(1));
	}
}
