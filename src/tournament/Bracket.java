package tournament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Elimination bracket for any number of participants (identified by their ids).
 *
 * <ul>
 * <li>Single elimination: one loss and the participant is out.</li>
 * <li>With repechage (double elimination): the losers of the main bracket go
 * to the repechage bracket and are only out after a second loss. The main
 * bracket champion and the repechage champion play the grand final, which
 * decides the champion: there is no rematch.</li>
 * </ul>
 *
 * When the number of participants is not a power of two the bracket is
 * completed with byes, given to the first seeds. Matches with a bye are decided
 * automatically.
 *
 * The bracket has no state besides the results, so it is rebuilt from the
 * seeds and the results list (see {@link #record}).
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Bracket {

	public enum Side {
		/** The main bracket */
		WINNERS,
		/** The repechage bracket */
		LOSERS,
		/** The grand final */
		FINAL
	}

	/** How much a match matters, so the program can warn about the big ones */
	public enum Importance {
		REGULAR, SEMIFINAL, FINAL
	}

	public enum Status {
		/** Waiting for the result of previous matches */
		WAITING,
		/** Both participants known, can be played */
		READY,
		/** Played */
		FINISHED,
		/** Decided without playing: one or both participants are missing */
		BYE;

		/** Tells whether the state is final: the match is finished or it is a bye. */
		public boolean isDone() {
			return this == FINISHED || this == BYE;
		}
	}

	/**
	 * One of the two places of a match
	 */
	public static final class Slot {

		/** Where the participant comes from, null for the first round */
		private final Match source;
		private final boolean sourceWinner;

		private boolean resolved;
		private Long participant;

		/** A slot filled by a seed of the championship. */
		private Slot(Long seed) {
			this.source = null;
			this.sourceWinner = false;
			this.participant = seed;
			this.resolved = true;
		}

		/** A slot that takes the winner or the loser of another match. */
		private Slot(Match source, boolean sourceWinner) {
			this.source = source;
			this.sourceWinner = sourceWinner;
		}

		/**
		 * @return the participant, or null if not known yet or if it is a bye
		 */
		public Long getParticipant() {
			return participant;
		}

		/**
		 * @return true when it is known who (or nobody) is in this slot
		 */
		public boolean isResolved() {
			return resolved;
		}

		/** Returns the source. */
		public Match getSource() {
			return source;
		}

		/** Tells whether the slot takes the winner of its source match, not the loser. */
		public boolean isSourceWinner() {
			return sourceWinner;
		}

		/**
		 * Describes where the participant will come from, e.g. "Vencedor de V1-2"
		 */
		public String describeSource() {
			if (this.source == null) {
				return "Folga";
			}
			return (this.sourceWinner ? "Vencedor de " : "Perdedor de ") + this.source.getCode();
		}

		/** Fills the slot with the winner or the loser of its source match, once that match is done. */
		private void resolve() {
			if (!this.resolved && this.source != null && this.source.status.isDone()) {
				this.participant = this.sourceWinner ? this.source.winner : this.source.loser;
				this.resolved = true;
			}
		}
	}

	public static final class Match {

		private final String code;
		private final Side side;
		private final int round;
		private final int position;
		private final Slot a;
		private final Slot b;
		private final int depth;

		private Status status = Status.WAITING;
		private Long winner;
		private Long loser;
		private Integer winnerScore;
		private Integer loserScore;

		/** A match between two slots, in the given round and position of its side. */
		private Match(String code, Side side, int round, int position, Slot slot, Slot slot2) {
			this.code = code;
			this.side = side;
			this.round = round;
			this.position = position;
			this.a = slot;
			this.b = slot2;
			this.depth = 1 + Math.max(depthOf(slot), depthOf(slot2));
		}

		/** How many matches come before a slot, following its sources. */
		private static int depthOf(Slot slot) {
			return slot.source == null ? 0 : slot.source.depth;
		}

		/**
		 * Unique code: V (main bracket) or R (repechage) followed by round-position,
		 * or F1/F2 for the finals
		 */
		public String getCode() {
			return code;
		}

		/** Returns the side. */
		public Side getSide() {
			return side;
		}

		/**
		 * 1 based round inside its side
		 */
		public int getRound() {
			return round;
		}

		/**
		 * 1 based position inside its round
		 */
		public int getPosition() {
			return position;
		}

		/** Returns the a. */
		public Slot getA() {
			return a;
		}

		/** Returns the b. */
		public Slot getB() {
			return b;
		}

		/**
		 * How many matches must be played before this one, used for the playing order
		 */
		public int getDepth() {
			return depth;
		}

		/** Returns the status. */
		public Status getStatus() {
			return status;
		}

		/** Returns the winner. */
		public Long getWinner() {
			return winner;
		}

		/** Returns the loser. */
		public Long getLoser() {
			return loser;
		}

		/** Returns the winner score. */
		public Integer getWinnerScore() {
			return winnerScore;
		}

		/** Returns the loser score. */
		public Integer getLoserScore() {
			return loserScore;
		}

		/** Tells whether a participant takes part in this match. */
		public boolean involves(Long participant) {
			return participant != null && (participant.equals(this.a.participant) || participant.equals(this.b.participant));
		}

		/** Fills the slots when their sources are done, and makes the match ready when both are known. */
		private void update() {
			if (this.status != Status.WAITING) {
				return;
			}
			this.a.resolve();
			this.b.resolve();
			if (!this.a.resolved || !this.b.resolved) {
				return;
			}

			if (this.a.participant != null && this.b.participant != null) {
				this.status = Status.READY;
			} else {
				// Bye: whoever is here goes on without playing
				this.status = Status.BYE;
				this.winner = this.a.participant != null ? this.a.participant : this.b.participant;
				this.loser = null;
			}
		}

		@Override
		/** The code of the match with its participants and status. */
		public String toString() {
			return this.code + "[" + this.a.participant + " x " + this.b.participant + " " + this.status + "]";
		}
	}

	private final List<Long> participants;
	private final boolean repechage;

	private final List<Match> matches = new ArrayList<Match>();
	private final Map<String, Match> byCode = new HashMap<String, Match>();
	private final List<List<Match>> winnersRounds = new ArrayList<List<Match>>();
	private final List<List<Match>> losersRounds = new ArrayList<List<Match>>();
	private final List<Match> finals = new ArrayList<Match>();

	/**
	 * The match that decides the champion
	 */
	private Match decider;

	/**
	 * @param seeds
	 *            the participants in seed order, at least two
	 * @param repechage
	 *            double elimination if true, single elimination otherwise
	 */
	public Bracket(List<Long> seeds, boolean repechage) {
		if (seeds.size() < 2) {
			throw new IllegalArgumentException("São necessários ao menos dois participantes");
		}
		if (seeds.stream().distinct().count() != seeds.size() || seeds.contains(null)) {
			throw new IllegalArgumentException("Participantes repetidos ou nulos");
		}

		this.participants = List.copyOf(seeds);
		this.repechage = repechage;

		this.build();
		this.update();
	}

	/** Adds a match to the bracket and indexes it by its code. */
	private Match add(String code, Side side, int round, int position, Slot slot, Slot slot2) {
		Match match = new Match(code, side, round, position, slot, slot2);
		this.matches.add(match);
		this.byCode.put(code, match);
		return match;
	}

	/** Creates the matches of the bracket: the rounds of the winners, the losers and the finals. */
	private void build() {
		int size = 2;
		while (size < this.participants.size()) {
			size *= 2;
		}
		int rounds = Integer.numberOfTrailingZeros(size);

		// Main bracket
		int[] order = Bracket.seedingOrder(size);
		for (int row = 1; row <= rounds; row++) {
			List<Match> round = new ArrayList<Match>();
			for (int index = 0; index < size >> row; index++) {
				Slot slot, slot2;
				if (row == 1) {
					slot = new Slot(this.seed(order[2 * index]));
					slot2 = new Slot(this.seed(order[2 * index + 1]));
				} else {
					List<Match> previous = this.winnersRounds.get(row - 2);
					slot = new Slot(previous.get(2 * index), true);
					slot2 = new Slot(previous.get(2 * index + 1), true);
				}
				round.add(this.add("V" + row + "-" + (index + 1), Side.WINNERS, row, index + 1, slot, slot2));
			}
			this.winnersRounds.add(round);
		}

		Match winnersFinal = this.winnersRounds.get(rounds - 1).get(0);
		this.decider = winnersFinal;

		if (!this.repechage) {
			return;
		}

		// Repechage bracket. Odd rounds: the repechage survivors play each other.
		// Even rounds: they play the losers of the next main bracket round
		for (int innerIndex = 1; innerIndex <= 2 * (rounds - 1); innerIndex++) {
			List<Match> round = new ArrayList<Match>();

			if (innerIndex == 1) {
				List<Match> firstRound = this.winnersRounds.get(0);
				for (int index2 = 0; index2 < size / 4; index2++) {
					round.add(this.add("R1-" + (index2 + 1), Side.LOSERS, 1, index2 + 1, new Slot(firstRound.get(2 * index2), false), new Slot(firstRound.get(2 * index2 + 1), false)));
				}
			} else if (innerIndex % 2 == 0) {
				List<Match> previous = this.losersRounds.get(innerIndex - 2);
				List<Match> dropping = this.winnersRounds.get(innerIndex / 2);
				// Alternating the order of who comes from the main bracket avoids
				// repeating early matches
				boolean reversed = (innerIndex / 2) % 2 == 1;
				for (int index3 = 0; index3 < previous.size(); index3++) {
					Match from = dropping.get(reversed ? dropping.size() - 1 - index3 : index3);
					round.add(this.add("R" + innerIndex + "-" + (index3 + 1), Side.LOSERS, innerIndex, index3 + 1, new Slot(previous.get(index3), true), new Slot(from, false)));
				}
			} else {
				List<Match> previous = this.losersRounds.get(innerIndex - 2);
				for (int index4 = 0; index4 < previous.size() / 2; index4++) {
					round.add(this.add("R" + innerIndex + "-" + (index4 + 1), Side.LOSERS, innerIndex, index4 + 1, new Slot(previous.get(2 * index4), true), new Slot(previous.get(2 * index4 + 1), true)));
				}
			}
			this.losersRounds.add(round);
		}

		// With two participants there is no repechage bracket: the loser of the
		// only match goes straight to the final
		Slot repechageChampion = this.losersRounds.isEmpty() ? new Slot(winnersFinal, false) : new Slot(this.losersRounds.get(this.losersRounds.size() - 1).get(0), true);

		Match grandFinal = this.add("F1", Side.FINAL, 1, 1, new Slot(winnersFinal, true), repechageChampion);
		this.finals.add(grandFinal);
		this.decider = grandFinal;
	}

	/** The participant of a seed number, or null when there are fewer participants. */
	private Long seed(int seedNumber) {
		return seedNumber <= this.participants.size() ? this.participants.get(seedNumber - 1) : null;
	}

	/**
	 * Standard bracket order of the seeds (1 based) so the best seeds only meet at
	 * the end and the byes go to the first seeds. For 8: 1 8 4 5 2 7 3 6
	 */
	static int[] seedingOrder(int size) {
		List<Integer> order = new ArrayList<Integer>(List.of(1, 2));
		while (order.size() < size) {
			int total = order.size() * 2 + 1;
			List<Integer> next = new ArrayList<Integer>();
			for (int seed : order) {
				next.add(seed);
				next.add(total - seed);
			}
			order = next;
		}
		return order.stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Matches are created after the ones they depend on, so one pass is enough
	 */
	private void update() {
		for (Match match : this.matches) {
			match.update();
		}
	}

	/**
	 * Records the result of a match
	 *
	 * @throws IllegalArgumentException
	 *             if the match does not exist or the winner is not playing it
	 * @throws IllegalStateException
	 *             if the match can not be played now
	 */
	public void record(String code, long winner, Integer winnerScore, Integer loserScore) {
		Match match = this.byCode.get(code);
		if (match == null) {
			throw new IllegalArgumentException("Partida inexistente: " + code);
		}
		if (match.status != Status.READY) {
			throw new IllegalStateException("A partida " + code + " não pode ser jogada agora (" + match.status + ")");
		}
		if (!match.involves(winner)) {
			throw new IllegalArgumentException("O vencedor não participa da partida " + code);
		}

		match.winner = winner;
		match.loser = winner == match.a.participant ? match.b.participant : match.a.participant;
		match.winnerScore = winnerScore;
		match.loserScore = loserScore;
		match.status = Status.FINISHED;

		this.update();
	}

	/** Returns the participants. */
	public List<Long> getParticipants() {
		return participants;
	}

	/** Tells whether the bracket has a losers bracket, the repechage. */
	public boolean hasRepechage() {
		return repechage;
	}

	/**
	 * All matches, the ones a match depends on always come before it
	 */
	public List<Match> getMatches() {
		return Collections.unmodifiableList(this.matches);
	}

	/** The match with the given code, or null when there is none. */
	public Match getMatch(String code) {
		return this.byCode.get(code);
	}

	/** Returns the winners rounds. */
	public List<List<Match>> getWinnersRounds() {
		return Collections.unmodifiableList(this.winnersRounds);
	}

	/** Returns the losers rounds. */
	public List<List<Match>> getLosersRounds() {
		return Collections.unmodifiableList(this.losersRounds);
	}

	/** Returns the finals. */
	public List<Match> getFinals() {
		return Collections.unmodifiableList(this.finals);
	}

	/**
	 * Name of the round of a match, like "Quartas de final" or "Repescagem 2"
	 */
	public String roundName(Match match) {
		return switch (match.getSide()) {
		case WINNERS -> this.winnersRoundName(match.getRound());
		case LOSERS -> this.losersRoundName(match.getRound());
		case FINAL -> "Grande final";
		};
	}

	/**
	 * Whether a match is a semifinal or a final (of the main bracket, of the
	 * repechage, the grand final or its reset), or a regular one
	 */
	public Importance importance(Match match) {
		return switch (match.getSide()) {
		case WINNERS -> switch (this.winnersRounds.size() - match.getRound()) {
		case 0 -> Importance.FINAL;
		case 1 -> Importance.SEMIFINAL;
		default -> Importance.REGULAR;
		};
		case LOSERS -> match.getRound() == this.losersRounds.size() ? Importance.FINAL : Importance.REGULAR;
		case FINAL -> Importance.FINAL;
		};
	}

	/**
	 * @param round
	 *            1 based round of the main bracket
	 */
	public String winnersRoundName(int round) {
		return switch (this.winnersRounds.size() - round) {
		case 0 -> this.repechage ? "Final da chave principal" : "Final";
		case 1 -> "Semifinal";
		case 2 -> "Quartas de final";
		case 3 -> "Oitavas de final";
		default -> round + "ª rodada";
		};
	}

	/**
	 * @param round
	 *            1 based round of the repechage
	 */
	public String losersRoundName(int round) {
		return round == this.losersRounds.size() ? "Final da repescagem" : "Repescagem " + round;
	}

	/**
	 * The next match to be played: the one that depends on fewer matches, main
	 * bracket first
	 */
	public Optional<Match> getNextMatch() {
		return this.matches.stream().filter(matrix -> matrix.status == Status.READY)
				.min(Comparator.comparingInt(Match::getDepth).thenComparing(Match::getSide).thenComparingInt(Match::getRound).thenComparingInt(Match::getPosition));
	}

	/** Tells whether the bracket has a champion. */
	public boolean isFinished() {
		return this.getChampion() != null;
	}

	/**
	 * @return the champion or null if the championship is not over
	 */
	public Long getChampion() {
		return this.decider.status.isDone() ? this.decider.winner : null;
	}

	/**
	 * Final positions known so far, best first. The champion is first, the others
	 * are ranked by how far they went before being eliminated, ties share the
	 * position. Participants still playing are not included.
	 */
	public Map<Long, Integer> getPlacements() {
		Long champion = this.getChampion();

		// How deep in the bracket each participant was eliminated
		Map<Long, Integer> eliminatedAt = new HashMap<Long, Integer>();
		for (Match match : this.matches) {
			if (match.status == Status.FINISHED && this.isEliminatedBy(match)) {
				eliminatedAt.put(match.loser, match.depth);
			}
		}

		List<Long> ranked = new ArrayList<Long>(eliminatedAt.keySet());
		ranked.sort(Comparator.comparing((Long point) -> eliminatedAt.get(point)).reversed());

		int stillPlaying = (int) this.participants.stream().filter(point2 -> !eliminatedAt.containsKey(point2) && !point2.equals(champion)).count();

		Map<Long, Integer> placements = new LinkedHashMap<Long, Integer>();
		if (champion != null) {
			placements.put(champion, 1);
		}
		for (Long participant : ranked) {
			int depth = eliminatedAt.get(participant);
			long better = eliminatedAt.values().stream().filter(delta -> delta > depth).count();
			placements.put(participant, 1 + stillPlaying + (champion != null ? 1 : 0) + (int) better);
		}
		return placements;
	}

	/**
	 * The best three, for the trophies: the champion, the second place and the
	 * third, in this order. Shorter if there are fewer robots, empty if the
	 * championship is not over. Of a tie (two robots eliminated at the same depth)
	 * the first found is taken.
	 */
	public List<Long> getPodium() {
		List<Long> podium = new ArrayList<Long>();
		if (this.getChampion() == null) {
			return podium;
		}
		Map<Long, Integer> placements = this.getPlacements();
		podium.add(this.getChampion());
		for (int place = 2; place <= 3; place++) {
			final int wanted = place;
			placements.entrySet().stream().filter(event -> event.getValue() == wanted).map(Map.Entry::getKey).findFirst().ifPresent(podium::add);
		}
		return podium;
	}

	/**
	 * @return true if losing this match took the loser out of the championship
	 */
	private boolean isEliminatedBy(Match match) {
		for (Match other : this.matches) {
			for (Slot slot : List.of(other.a, other.b)) {
				if (slot.source == match && !slot.sourceWinner) {
					return false;
				}
			}
		}
		return true;
	}
}
