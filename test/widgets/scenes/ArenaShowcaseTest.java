package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import model.Author;
import model.Opponent;
import support.ChampionshipFixture;
import support.FxSupport;
import tournament.Importance;
import utils.robocode.BattleRecording;

class ArenaShowcaseTest {

	/** Robots with their team, name and battle cry, as the championship has them. */
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

	/** The robots of the sample battle, with the picture, music and authors that the card shows. */
	private static List<Opponent> sampleRobots() throws IOException {
		List<Opponent> robots = ArenaSimulation.fighters();
		for (Opponent robot : robots) {
			robot.setImage(ChampionshipFixture.logo());
			robot.setMusic(ArenaShowcaseTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes());
			robot.setSourceCode("public class " + robot.getName() + " extends Robot {}");
			Author author = new Author();
			author.setName("Autor de " + robot.getName());
			robot.setAuthors(List.of(author));
		}
		return robots;
	}

	/** The sample battle stretched to forty turns, where robot 1 is hit by a bullet on turn 34 and dies on turn 35. */
	private static List<String> longBattle() {
		List<String> lines = new ArrayList<String>(ArenaSimulation.OUTPUT.subList(0, 6));
		for (int turn = 0; turn < 40; turn++) {
			String walls = "0 " + (100 + turn) + " 100 0 0 0 100 0";
			String spin = "1 300 300 0 0 0 100 " + (turn < 35 ? 0 : 3);
			String bullets = switch (turn) {
			case 4 -> "4 0 130 100 2 1 -1;5 0 140 100 2 2 1";
			case 18 -> "7 0 120 100 2 1 -1";
			case 34 -> "6 0 290 300 3 2 1";
			default -> "";
			};
			lines.add("#T 0 " + turn + "|" + walls + ";" + spin + "|" + bullets);
		}
		return lines;
	}

	@Test
	void theShowcaseOfTheRobotsRunsBeforeASemifinal() throws IOException {
		BattleArena3D arena = ArenaSimulation.arena(sampleRobots(), "NEON");

		assertTrue(ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 4000, 0.05, "Semifinal", Importance.SEMIFINAL));
	}

	@Test
	void aRegularBattleIsAnnouncedWithoutAStageTitle() throws IOException {
		BattleArena3D arena = ArenaSimulation.arena(sampleRobots(), "NEON");

		assertTrue(ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 4000, 0.05, "Rodada", Importance.REGULAR));
	}

	@Test
	void aRobotWithoutItsOpponentIsShownByItsName() throws IOException {
		BattleArena3D arena = ArenaSimulation.arena(robots(), "NEON");

		assertTrue(ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 4000, 0.05, "Semifinal", Importance.SEMIFINAL));
	}

	@Test
	void aDeadRobotIsReplayedFromBeforeItsDeath() throws IOException {
		BattleArena3D arena = ArenaSimulation.arena(sampleRobots(), "NEON");
		BattleRecording recording = ArenaSimulation.recording(longBattle());

		FxSupport.runVoid(() -> {
			arena.play(recording, () -> {
			});
			// Not in a replay yet, so there is nothing to end
			arena.showcase.endReplay();

			arena.showcase.freezeForReplay(1, 35);
			arena.showcase.startReplay();
			arena.showcase.endReplay();
			// Back to the battle, the robot that died stays dead
			assertTrue(arena.robots.get(1).dead);

			// A replay that ends before the death is drawn: the robot is destroyed when it ends, and no bullet is to blame
			arena.showcase.freezeForReplay(1, 20);
			arena.showcase.startReplay();
			arena.showcase.endReplay();
			assertTrue(arena.robots.get(1).dead);

			// A robot that is not on the field has nothing to turn
			arena.shotSubject = 99;
			arena.showcase.turntable();
		});
	}
}
