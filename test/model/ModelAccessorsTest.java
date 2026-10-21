package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** The plain fields of the entities: what is set is what is read back. */
class ModelAccessorsTest {

	@Test
	void anAuthorKeepsItsIdentityAndObservations() {
		Author author = new Author();
		author.setId(7);
		author.setObservations("Mantém o robô");

		assertEquals(7, author.getId());
		assertEquals("Mantém o robô", author.getObservations());
	}

	@Test
	void aResultKeepsItsChampionshipMatchOpponentsAndScores() {
		Championship championship = new Championship();
		Opponent winner = new Opponent();
		Opponent loser = new Opponent();
		LocalDateTime when = LocalDateTime.of(2026, 10, 21, 9, 30);
		MatchResult result = new MatchResult();

		result.setChampionship(championship);
		result.setMatchCode("V1-1");
		result.setWinner(winner);
		result.setLoser(loser);
		result.setWinnerScore(120);
		result.setLoserScore(80);
		result.setPlayedAt(when);

		assertEquals(0, result.getId());
		assertSame(championship, result.getChampionship());
		assertEquals("V1-1", result.getMatchCode());
		assertSame(winner, result.getWinner());
		assertSame(loser, result.getLoser());
		assertEquals(120, result.getWinnerScore());
		assertEquals(80, result.getLoserScore());
		assertEquals(when, result.getPlayedAt());
	}

	@Test
	void aMomentIsNumberedOnceSaved() {
		assertEquals(0, new BattleMoment().getId());
		assertNull(new BattleMoment().getImage());
	}
}
