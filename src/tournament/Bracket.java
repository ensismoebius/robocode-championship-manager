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

		private Slot(Long seed) {
			this.source = null;
			this.sourceWinner = false;
			this.participant = seed;
			this.resolved = true;
		}

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

		public Match getSource() {
			return source;
		}

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

		private Match(String code, Side side, int round, int position, Slot a, Slot b) {
			this.code = code;
			this.side = side;
			this.round = round;
			this.position = position;
			this.a = a;
			this.b = b;
			this.depth = 1 + Math.max(depthOf(a), depthOf(b));
		}

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

		public Slot getA() {
			return a;
		}

		public Slot getB() {
			return b;
		}

		/**
		 * How many matches must be played before this one, used for the playing order
		 */
		public int getDepth() {
			return depth;
		}

		public Status getStatus() {
			return status;
		}

		public Long getWinner() {
			return winner;
		}

		public Long getLoser() {
			return loser;
		}

		public Integer getWinnerScore() {
			return winnerScore;
		}

		public Integer getLoserScore() {
			return loserScore;
		}

		public boolean involves(Long participant) {
			return participant != null && (participant.equals(this.a.participant) || participant.equals(this.b.participant));
		}

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

	private Match add(String code, Side side, int round, int position, Slot a, Slot b) {
		Match match = new Match(code, side, round, position, a, b);
		this.matches.add(match);
		this.byCode.put(code, match);
		return match;
	}

	private void build() {
		int size = 2;
		while (size < this.participants.size()) {
			size *= 2;
		}
		int rounds = Integer.numberOfTrailingZeros(size);

		// Main bracket
		int[] order = Bracket.seedingOrder(size);
		for (int r = 1; r <= rounds; r++) {
			List<Match> round = new ArrayList<Match>();
			for (int i = 0; i < size >> r; i++) {
				Slot a, b;
				if (r == 1) {
					a = new Slot(this.seed(order[2 * i]));
					b = new Slot(this.seed(order[2 * i + 1]));
				} else {
					List<Match> previous = this.winnersRounds.get(r - 2);
					a = new Slot(previous.get(2 * i), true);
					b = new Slot(previous.get(2 * i + 1), true);
				}
				round.add(this.add("V" + r + "-" + (i + 1), Side.WINNERS, r, i + 1, a, b));
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
		for (int j = 1; j <= 2 * (rounds - 1); j++) {
			List<Match> round = new ArrayList<Match>();

			if (j == 1) {
				List<Match> firstRound = this.winnersRounds.get(0);
				for (int i = 0; i < size / 4; i++) {
					round.add(this.add("R1-" + (i + 1), Side.LOSERS, 1, i + 1, new Slot(firstRound.get(2 * i), false), new Slot(firstRound.get(2 * i + 1), false)));
				}
			} else if (j % 2 == 0) {
				List<Match> previous = this.losersRounds.get(j - 2);
				List<Match> dropping = this.winnersRounds.get(j / 2);
				// Alternating the order of who comes from the main bracket avoids
				// repeating early matches
				boolean reversed = (j / 2) % 2 == 1;
				for (int i = 0; i < previous.size(); i++) {
					Match from = dropping.get(reversed ? dropping.size() - 1 - i : i);
					round.add(this.add("R" + j + "-" + (i + 1), Side.LOSERS, j, i + 1, new Slot(previous.get(i), true), new Slot(from, false)));
				}
			} else {
				List<Match> previous = this.losersRounds.get(j - 2);
				for (int i = 0; i < previous.size() / 2; i++) {
					round.add(this.add("R" + j + "-" + (i + 1), Side.LOSERS, j, i + 1, new Slot(previous.get(2 * i), true), new Slot(previous.get(2 * i + 1), true)));
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

	public List<Long> getParticipants() {
		return participants;
	}

	public boolean hasRepechage() {
		return repechage;
	}

	/**
	 * All matches, the ones a match depends on always come before it
	 */
	public List<Match> getMatches() {
		return Collections.unmodifiableList(this.matches);
	}

	public Match getMatch(String code) {
		return this.byCode.get(code);
	}

	public List<List<Match>> getWinnersRounds() {
		return Collections.unmodifiableList(this.winnersRounds);
	}

	public List<List<Match>> getLosersRounds() {
		return Collections.unmodifiableList(this.losersRounds);
	}

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
		return this.matches.stream().filter(m -> m.status == Status.READY)
				.min(Comparator.comparingInt(Match::getDepth).thenComparing(Match::getSide).thenComparingInt(Match::getRound).thenComparingInt(Match::getPosition));
	}

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
		ranked.sort(Comparator.comparing((Long p) -> eliminatedAt.get(p)).reversed());

		int stillPlaying = (int) this.participants.stream().filter(p -> !eliminatedAt.containsKey(p) && !p.equals(champion)).count();

		Map<Long, Integer> placements = new LinkedHashMap<Long, Integer>();
		if (champion != null) {
			placements.put(champion, 1);
		}
		for (Long participant : ranked) {
			int depth = eliminatedAt.get(participant);
			long better = eliminatedAt.values().stream().filter(d -> d > depth).count();
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
			placements.entrySet().stream().filter(e -> e.getValue() == wanted).map(Map.Entry::getKey).findFirst().ifPresent(podium::add);
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
