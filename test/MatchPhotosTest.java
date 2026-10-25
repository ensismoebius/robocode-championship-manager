import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import tournament.Match;

import javafx.stage.Stage;
import model.Opponent;
import support.ChampionshipFixture;
import support.FxSupport;
import support.TestConfiguration;
import utils.configuration.Configuration;
import utils.database.Database;
import widgets.scenes.ArenaSimulation;
import widgets.scenes.BattleArena3D;

class MatchPhotosTest {

	private static List<Opponent> fighters;
	private static Match match;

	@BeforeAll
	static void emptyDatabase() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
		fighters = ChampionshipFixture.opponents(2);
		match = ChampionshipFixture.fresh(fighters, false).getMatches().get(0);
	}

	@AfterAll
	static void closeTheDatabase() {
		Database.close();
	}

	/** Plays the sample battle in a window, until it ends, and returns the arena with the photos it took. */
	private static BattleArena3D playedBattle(List<Opponent> robots) throws InterruptedException {
		BattleArena3D arena = ArenaSimulation.arena(robots);
		Stage window = FxSupport.show(arena);
		CountDownLatch finished = new CountDownLatch(1);
		FxSupport.runVoid(() -> arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), finished::countDown));
		assertTrue(finished.await(90, TimeUnit.SECONDS), "the battle should end");
		FxSupport.close(window);
		return arena;
	}

	@Test
	void aBattleWithoutPhotosKeepsNone() {
		BattleArena3D arena = FxSupport.run(() -> new BattleArena3D(fighters));

		MatchPhotos.keep(3, arena, match, fighters);

		assertTrue(MatchPhotos.photosOf(3, List.of(fighters.get(0).getId())).isEmpty());
	}

	@Test
	void forgettingAMatchWithoutPhotosDoesNothing() {
		MatchPhotos.forget(3, match.getCode());

		assertEquals(0, MatchPhotos.photosOf(3, List.of(fighters.get(0).getId())).size());
	}

	@Test
	void thePhotosOfABattleAreKeptWithItsMatchAndForgottenWhenItsResultIsUndone() throws InterruptedException {
		List<Opponent> robots = ArenaSimulation.fighters();
		robots.get(0).setId(1);
		robots.get(1).setId(2);
		BattleArena3D arena = playedBattle(robots);
		// The hit that hurts is photographed while the battle plays
		assertFalse(arena.photos().isEmpty());

		MatchPhotos.keep(4, arena, match, robots);
		assertFalse(MatchPhotos.photosOf(4, List.of(1L, 2L)).isEmpty());

		MatchPhotos.forget(4, match.getCode());
		assertTrue(MatchPhotos.photosOf(4, List.of(1L, 2L)).isEmpty());
	}

	@Test
	void aDatabaseThatCanNotBeWrittenOnlyLosesThePhotos() throws IOException {
		Path blocker = Path.of("target", "test-home", "photos-blocker");
		Files.createDirectories(blocker.getParent());
		Files.writeString(blocker, "um arquivo, não uma pasta");
		try {
			TestConfiguration.load(Configuration.RANDOM, "/nowhere", blocker.resolve("robochamp.db").toString());
			// The database opens again on its next use, with the new file
			Database.close();
			BattleArena3D arena = FxSupport.run(() -> new BattleArena3D(fighters));

			// Each one reports the failure and goes on: the battle still counts without its photos
			MatchPhotos.keep(6, arena, match, fighters);
			MatchPhotos.forget(6, match.getCode());
			assertTrue(MatchPhotos.photosOf(6, List.of(fighters.get(0).getId())).isEmpty());
		} finally {
			TestConfiguration.load(Configuration.RANDOM, "/nowhere");
			TestConfiguration.freshDatabase();
		}
	}
}
