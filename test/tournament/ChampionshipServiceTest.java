package tournament;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Championship;
import model.MatchResult;
import model.Opponent;
import support.TestConfiguration;
import utils.configuration.Configuration;
import utils.database.Database;

class ChampionshipServiceTest {

	@BeforeAll
	static void emptyDatabase() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
	}

	@AfterAll
	static void closeTheDatabase() {
		Database.close();
	}

	@Test
	void aChampionshipNeedsTwoRobotsAndIgnoresResultsOfMatchesThatNoLongerExist() {
		assertThrows(IllegalStateException.class, () -> ChampionshipService.create(false, new Random(1)));

		Database.inTransaction(entityManager -> {
			for (int index = 1; index <= 4; index++) {
				Opponent opponent = new Opponent();
				opponent.setTeam("time" + index);
				opponent.setName("Robo" + index);
				opponent.setBattleCry("Grito");
				entityManager.persist(opponent);
			}
			return null;
		});
		Championship championship = ChampionshipService.create(false, new Random(1));
		assertNotNull(championship.getId());

		// A result whose match is not in the bracket, left by an older draw
		Database.inTransaction(entityManager -> {
			Championship managed = entityManager.find(Championship.class, championship.getId());
			MatchResult orphan = new MatchResult();
			orphan.setChampionship(managed);
			orphan.setMatchCode("ZZ-9");
			orphan.setWinner(entityManager.createQuery("select o from Opponent o order by o.id", Opponent.class).setMaxResults(1).getSingleResult());
			orphan.setLoser(entityManager.createQuery("select o from Opponent o order by o.id desc", Opponent.class).setMaxResults(1).getSingleResult());
			orphan.setWinnerScore(1);
			orphan.setLoserScore(0);
			orphan.setPlayedAt(LocalDateTime.now());
			entityManager.persist(orphan);
			return null;
		});

		Bracket bracket = ChampionshipService.bracketOf(ChampionshipService.current());
		assertNull(bracket.getMatch("ZZ-9"));
	}
}
