import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Opponent;
import support.ChampionshipFixture;
import support.FxSupport;
import support.TestConfiguration;
import tournament.Bracket;
import utils.configuration.Configuration;
import utils.database.Database;
import widgets.scenes.BattleArena3D;

class MatchPhotosTest {

	private static List<Opponent> fighters;
	private static Bracket.Match match;

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
}
