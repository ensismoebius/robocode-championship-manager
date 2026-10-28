package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ArenaStyleTest {

	@Test
	void configuredArenaIsKeptInAnyMatch() {
		assertEquals(ArenaStyle.VOLCANO, ArenaStyle.resolve("VOLCANO", new Random(1), false));
		assertEquals(ArenaStyle.VOLCANO, ArenaStyle.resolve("VOLCANO", new Random(1), true));
	}

	@Test
	void theArenaOfTheFinalIsOnlyForTheFinalEvenWhenConfigured() {
		assertEquals(ArenaStyle.FINALS, ArenaStyle.resolve("EDEN_SOLAR", new Random(1), true));
		Random random = new Random(7);
		for (int i = 0; i < 40; i++) {
			assertTrue(ArenaStyle.resolve("EDEN_SOLAR", random, false) != ArenaStyle.FINALS, "the arena of the final in another match");
		}
	}

	@Test
	void randomArenaVariesButKeepsTheArenaOfTheFinalForTheFinal() {
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
	void aRandomFinalAlwaysHasTheArenaOfTheFinal() {
		Random random = new Random(5);
		for (int i = 0; i < 20; i++) {
			assertEquals(ArenaStyle.FINALS, ArenaStyle.resolve(i % 2 == 0 ? "RANDOM" : "unknown", random, true));
		}
	}

	@Test
	void theArenasOfRealMatchesFollowTheFinal() {
		var fighters = ArenaSimulation.fighters();
		assertEquals(ArenaStyle.FINALS, ArenaSimulation.arena(fighters, "RANDOM", true).style);
		for (String configured : new String[] { "RANDOM", "EDEN_SOLAR", "RANDOM" }) {
			assertTrue(ArenaSimulation.arena(fighters, configured, false).style != ArenaStyle.FINALS, configured);
		}
	}

	@Test
	void everyArenaHasItsSong() {
		for (ArenaStyle style : ArenaStyle.values()) {
			assertTrue(style.song != null, style.name());
		}
	}
}
