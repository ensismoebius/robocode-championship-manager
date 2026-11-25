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
 * Everyone plays in every round: there are no byes, and nobody goes on without
 * beating someone. When the number of participants in a round is odd, the one
 * left without a pair plays a challenge against the loser of the first match of
 * the round: the winner goes on and the loser goes to the repechage.
 *
 * The bracket has no state besides the results, so it is rebuilt from the
 * seeds and the results list (see {@link #record}).
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Bracket {

	private final List<Long> participants;
	private final boolean repechage;

	private final List<Match> matches = new ArrayList<Match>();
	private final Map<String, Match> byCode = new HashMap<String, Match>();
	private final List<List<Match>> winnersRounds = new ArrayList<List<Match>>();
	private final List<List<Match>> losersRounds = new ArrayList<List<Match>>();
	private final List<Match> finals = new ArrayList<Match>();
	/** The losers of each round of the main bracket, who go to the repechage */
	private final List<List<Slot>> dropped = new ArrayList<List<Slot>>();

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

	/** One round of matches: who goes on, who is out of the side (or goes to the repechage) and the matches themselves. */
	private record Round(List<Match> matches, List<Slot> advancing, List<Slot> losing) {
	}

	/** Creates the matches of the bracket: the rounds of the winners, the losers and the finals. */
	private void build() {
		this.buildWinners();

		Match winnersFinal = this.winnersRounds.get(this.winnersRounds.size() - 1).get(0);
		this.decider = winnersFinal;

		if (!this.repechage) {
			return;
		}

		Slot repechageChampion = this.buildRepechage();
		Match grandFinal = this.add("F1", Side.FINAL, 1, 1, new Slot(winnersFinal, true), repechageChampion);
		this.finals.add(grandFinal);
		this.decider = grandFinal;
	}

	/**
	 * Pairs the slots two by two, in order. Everyone plays in every round, and who goes on has beaten someone: when the number of
	 * slots is odd, the one left without a pair plays a challenge against the loser of the first match of the round. The winner
	 * goes on; the loser of the challenge takes, among the losing ones, the place of the loser of that first match, who
	 * has already played the challenge.
	 */
	private Round pairUp(Side side, int round, List<Slot> slots) {
		String prefix = side == Side.WINNERS ? "V" : "R";
		List<Match> matches = new ArrayList<Match>();
		List<Slot> advancing = new ArrayList<Slot>();
		List<Slot> losing = new ArrayList<Slot>();
		int pairs = slots.size() / 2;
		for (int index = 0; index < pairs; index++) {
			Match match = this.add(prefix + round + "-" + (index + 1), side, round, index + 1, slots.get(2 * index), slots.get(2 * index + 1));
			matches.add(match);
			advancing.add(new Slot(match, true));
			losing.add(new Slot(match, false));
		}
		if (slots.size() % 2 == 1) {
			Match challenge = this.add(prefix + round + "-" + (pairs + 1), side, round, pairs + 1, slots.get(slots.size() - 1), new Slot(matches.get(0), false));
			challenge.challenge = true;
			matches.add(challenge);
			advancing.add(new Slot(challenge, true));
			losing.set(0, new Slot(challenge, false));
		}
		return new Round(matches, advancing, losing);
	}

	/**
	 * The rounds of the main bracket: the first one takes the seeds, spread so that the best only meet at the end, the others take
	 * the winners before them, until there is one. The losers of each round are kept, to go to the repechage.
	 */
	private void buildWinners() {
		List<Slot> slots = new ArrayList<Slot>();
		int size = 2;
		while (size < this.participants.size()) {
			size *= 2;
		}
		for (int seed : Bracket.seedingOrder(size)) {
			if (seed <= this.participants.size()) {
				slots.add(new Slot(this.participants.get(seed - 1)));
			}
		}
		for (int row = 1; slots.size() > 1; row++) {
			Round round = this.pairUp(Side.WINNERS, row, slots);
			this.winnersRounds.add(round.matches());
			this.dropped.add(round.losing());
			slots = round.advancing();
		}
	}

	/**
	 * The rounds of the repechage: the losers of the first round of the main bracket play each other; then the survivors play the
	 * losers of the next round of the main bracket, who drop in; and so on until the loser of the final of the main bracket drops
	 * in. A loss here is the end.
	 *
	 * @return the slot of the champion of the repechage
	 */
	private Slot buildRepechage() {
		List<Slot> pool = this.dropped.get(0);
		int round = 0;
		for (int main = 2; main <= this.dropped.size(); main++) {
			// The survivors play each other
			if (pool.size() > 1) {
				Round survivors = this.pairUp(Side.LOSERS, ++round, pool);
				this.losersRounds.add(survivors.matches());
				pool = survivors.advancing();
			}
			// And then they play the losers that drop in, alternating the order of who comes from the main bracket, which avoids
			// repeating early matches
			List<Slot> dropping = new ArrayList<Slot>(this.dropped.get(main - 1));
			if (main % 2 == 0) {
				Collections.reverse(dropping);
			}
			List<Slot> mixed = new ArrayList<Slot>();
			for (int index = 0; index < Math.max(pool.size(), dropping.size()); index++) {
				if (index < pool.size()) {
					mixed.add(pool.get(index));
				}
				if (index < dropping.size()) {
					mixed.add(dropping.get(index));
				}
			}
			Round next = this.pairUp(Side.LOSERS, ++round, mixed);
			this.losersRounds.add(next.matches());
			pool = next.advancing();
		}
		// With two participants there is no repechage bracket: the loser of the only match goes straight to the final
		return pool.get(0);
	}

	/**
	 * Standard bracket order of the seeds (1 based) so the best seeds only meet at
	 * the end. For 8: 1 8 4 5 2 7 3 6
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
		case 0 -> this.repechage ? "Fim da chave principal" : "Final";
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
		return round == this.losersRounds.size() ? "Fim da repescagem" : "Repescagem " + round;
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

	/** Tells whether the match decides the championship: the grand final, or the final of a bracket without repechage. */
	public boolean isDecider(Match match) {
		return match == this.decider;
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
