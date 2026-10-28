package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tournament.Importance;

class ArenaStyleTest {

	@Test
	void configuredArenaIsKeptInAnyMatch() {
		assertEquals(ArenaStyle.VOLCANO, ArenaStyle.resolve("VOLCANO", new Random(1), false));
		assertEquals(ArenaStyle.VOLCANO, ArenaStyle.resolve("VOLCANO", new Random(1), true));
		assertEquals(ArenaStyle.FINALS, ArenaStyle.resolve("EDEN_SOLAR", new Random(1), false));
	}

	@Test
	void randomArenaVariesButKeepsTheArenaOfTheFinalsForTheFinals() {
		Random random = new Random(5);
		Set<ArenaStyle> seen = EnumSet.noneOf(ArenaStyle.class);
		for (int i = 0; i < 60; i++) {
			seen.add(ArenaStyle.resolve(i % 2 == 0 ? "RANDOM" : "unknown", random, false));
		}
		Set<ArenaStyle> others = EnumSet.allOf(ArenaStyle.class);
		others.remove(ArenaStyle.FINALS);
		assertEquals(others, seen);
	}

	@Test
	void aRandomFinalAlwaysHasTheArenaOfTheFinals() {
		Random random = new Random(5);
		for (int i = 0; i < 20; i++) {
			assertEquals(ArenaStyle.FINALS, ArenaStyle.resolve(i % 2 == 0 ? "RANDOM" : "unknown", random, true));
		}
	}

	@Test
	void theArenasOfRealMatchesFollowTheStage() {
		var fighters = ArenaSimulation.fighters();
		assertEquals(ArenaStyle.FINALS, ArenaSimulation.arena(fighters, "RANDOM", Importance.FINAL).style);
		for (Importance importance : new Importance[] { Importance.REGULAR, Importance.SEMIFINAL, Importance.REGULAR }) {
			assertTrue(ArenaSimulation.arena(fighters, "RANDOM", importance).style != ArenaStyle.FINALS, importance.name());
		}
	}

	@Test
	void everyArenaHasItsSong() {
		for (ArenaStyle style : ArenaStyle.values()) {
			assertTrue(style.song != null, style.name());
		}
	}
}
