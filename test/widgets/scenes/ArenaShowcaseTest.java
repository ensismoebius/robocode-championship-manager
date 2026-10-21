package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import model.Author;
import model.Opponent;
import support.ChampionshipFixture;
import tournament.Bracket.Importance;

class ArenaShowcaseTest {

	/** Robots with their picture, team, music and authors, as the championship has them. */
	private static List<Opponent> robots() throws IOException {
		List<Opponent> robots = ChampionshipFixture.opponents(2);
		for (Opponent robot : robots) {
			robot.setMusic(ArenaShowcaseTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes());
			robot.setSourceCode("public class " + robot.getName() + " extends Robot {}");
			Author author = new Author();
			author.setName("Autor de " + robot.getName());
			robot.setAuthors(List.of(author));
		}
		return robots;
	}

	@Test
	void theShowcaseOfTheRobotsRunsBeforeASemifinal() throws IOException {
		BattleArena3D arena = ArenaSimulation.arena(robots(), "NEON");

		assertTrue(ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 4000, 0.05, "Semifinal", Importance.SEMIFINAL));
	}
}
