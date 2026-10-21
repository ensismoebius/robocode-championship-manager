package tournament;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Championship;
import model.MatchResult;
import model.Opponent;
import utils.database.Database;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Creates championships and records their results
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class ChampionshipService {

	/**
	 * @return the most recent championship or null if there is none
	 */
	public static Championship current() {
		return Database.read(em -> em.createQuery("select c from Championship c order by c.id desc", Championship.class).setMaxResults(1).getResultStream().findFirst().orElse(null));
	}

	public static long countOpponents() {
		Long count = Database.read(em -> em.createQuery("select count(o) from Opponent o", Long.class).getSingleResult());
		return count;
	}

	/**
	 * Creates a new championship with every registered robot, in random order
	 *
	 * @throws IllegalStateException
	 *             if there are less than two robots
	 */
	public static Championship create(boolean repechage, Random random) {
		return Database.inTransaction(em -> {
			List<Opponent> opponents = new ArrayList<Opponent>(em.createQuery("select o from Opponent o order by o.id", Opponent.class).getResultList());
			if (opponents.size() < 2) {
				throw new IllegalStateException("São necessários ao menos dois robôs cadastrados");
			}

			// The draw
			Collections.shuffle(opponents, random);

			Championship championship = new Championship();
			championship.setCreatedAt(LocalDateTime.now());
			championship.setName("Campeonato de " + championship.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
			championship.setRepechage(repechage);
			championship.getSeeds().addAll(opponents);
			em.persist(championship);
			return championship;
		});
	}

	/**
	 * Rebuilds the bracket of the championship from its results
	 */
	public static Bracket bracketOf(Championship championship) {
		Bracket bracket = new Bracket(championship.getSeeds().stream().map(Opponent::getId).toList(), championship.isRepechage());
		for (MatchResult result : championship.getResults()) {
			// Championships drawn before the rematch was removed may have its result
			if (bracket.getMatch(result.getMatchCode()) == null) {
				continue;
			}
			bracket.record(result.getMatchCode(), result.getWinner().getId(), result.getWinnerScore(), result.getLoserScore());
		}
		return bracket;
	}

	/**
	 * The robots of the championship by id
	 */
	public static Map<Long, Opponent> opponentsOf(Championship championship) {
		Map<Long, Opponent> opponents = new LinkedHashMap<Long, Opponent>();
		championship.getSeeds().forEach(o -> opponents.put(o.getId(), o));
		return opponents;
	}

	/**
	 * Saves the result of a match after checking it against the bracket
	 *
	 * @param winnerScore
	 *            and loserScore may be null when the result was not given by
	 *            robocode
	 * @return the championship with the new result
	 */
	public static Championship recordResult(Championship championship, String matchCode, long winnerId, Integer winnerScore, Integer loserScore) {
		Bracket bracket = ChampionshipService.bracketOf(championship);
		bracket.record(matchCode, winnerId, winnerScore, loserScore);
		long loserId = bracket.getMatch(matchCode).getLoser();

		return Database.inTransaction(em -> {
			Championship managed = em.find(Championship.class, championship.getId());

			MatchResult result = new MatchResult();
			result.setChampionship(managed);
			result.setMatchCode(matchCode);
			result.setWinner(em.find(Opponent.class, winnerId));
			result.setLoser(em.find(Opponent.class, loserId));
			result.setWinnerScore(winnerScore);
			result.setLoserScore(loserScore);
			result.setPlayedAt(LocalDateTime.now());
			managed.getResults().add(result);

			return managed;
		});
	}

	/**
	 * Removes the last result
	 *
	 * @return the championship without it
	 */
	public static Championship undoLastResult(Championship championship) {
		return Database.inTransaction(em -> {
			Championship managed = em.find(Championship.class, championship.getId());
			if (!managed.getResults().isEmpty()) {
				managed.getResults().remove(managed.getResults().size() - 1);
			}
			return managed;
		});
	}
}
