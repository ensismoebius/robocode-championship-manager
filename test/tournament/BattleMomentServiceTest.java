package tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.BattleMoment;
import support.ChampionshipFixture;
import support.TestConfiguration;
import utils.configuration.Configuration;
import utils.database.Database;

class BattleMomentServiceTest {

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
	void theMomentsOfAMatchAreSavedFoundAndDeleted() {
		BattleMoment moment = ChampionshipFixture.moment("V1-1", 1, 2, 1, true);
		BattleMomentService.save(1, "V1-1", List.of(moment));

		List<BattleMoment> found = BattleMomentService.photosOf(1, List.of(1L, 2L));
		assertEquals(1, found.size());
		assertTrue(found.get(0).isDestruction());

		BattleMomentService.delete(1, "V1-1");
		assertTrue(BattleMomentService.photosOf(1, List.of(1L, 2L)).isEmpty());
	}

	@Test
	void savingNoMomentsKeepsTheChampionshipWithoutPhotos() {
		BattleMomentService.save(2, "F1", List.of());

		assertTrue(BattleMomentService.photosOf(2, List.of(1L)).isEmpty());
	}

	@Test
	void noRobotsHaveNoPhotos() {
		assertTrue(BattleMomentService.photosOf(1, List.of()).isEmpty());
	}
}
