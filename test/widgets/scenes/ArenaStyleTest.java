package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ArenaStyleTest {

	@Test
	void configuredArena() {
		assertEquals(ArenaStyle.VOLCANO, ArenaStyle.resolve("VOLCANO", new Random(1)));
	}

	@Test
	void randomArenaVaries() {
		Random random = new Random(5);
		Set<ArenaStyle> seen = EnumSet.noneOf(ArenaStyle.class);
		for (int i = 0; i < 60; i++) {
			seen.add(ArenaStyle.resolve(i % 2 == 0 ? "RANDOM" : "unknown", random));
		}
		assertTrue(seen.size() == ArenaStyle.values().length, seen.toString());
	}

	@Test
	void everyArenaHasItsSong() {
		for (ArenaStyle style : ArenaStyle.values()) {
			assertTrue(style.song != null, style.name());
		}
	}
}
