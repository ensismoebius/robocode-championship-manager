package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import widgets.Song;

/** The desert of the Middle East arena: the plain around the courtyard is flat, the dunes rise beyond it, and the arena has its song. */
class DesertArenaTest {

	private static final double WIDTH = 800;
	private static final double DEPTH = 600;

	@Test
	void thePlainIsFlatAndTheDunesRiseBeyondIt() {
		// The courtyard and the plain around it, up to the flat distance, have no dunes at all
		assertEquals(0, DesertDunes.height(0, 0, WIDTH, DEPTH), 1e-9);
		assertEquals(0, DesertDunes.height(300, 200, WIDTH, DEPTH), 1e-9);
		assertEquals(0, DesertDunes.height(600, 0, WIDTH, DEPTH), 1e-9);
		assertTrue(DesertDunes.height(1500, 0, WIDTH, DEPTH) > 0, "no dune beyond the plain");
	}

	@Test
	void theArenaHasItsOwnDesertAndSong() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "MIDDLE_EAST");

		assertNotNull(arena.desert);
		assertEquals(Song.HIJAZ, ArenaStyle.MIDDLE_EAST.song);
	}

	@Test
	void thePointedArchNarrowsFromItsSpringlineToItsTip() {
		double half = 10;
		assertEquals(half, IslamicForms.archHalfWidth(-5, half), 1e-9);
		assertEquals(half, IslamicForms.archHalfWidth(0, half), 1e-9);
		double tip = Math.sqrt(3) * half;
		assertEquals(0, IslamicForms.archHalfWidth(tip, half), 1e-9);
		assertEquals(0, IslamicForms.archHalfWidth(tip + 5, half), 1e-9);
		double before = half;
		for (double height = 1; height < tip; height += 1) {
			double now = IslamicForms.archHalfWidth(height, half);
			assertTrue(now < before, "the arch does not narrow at " + height);
			before = now;
		}
	}

	@Test
	void theMonumentsStandOnLevelGround() {
		for (double[] site : DesertDunes.SITES) {
			double level = DesertDunes.height(site[0], site[1], WIDTH, DEPTH);
			for (double angle = 0; angle < Math.PI * 2; angle += 0.5) {
				double posX = site[0] + Math.cos(angle) * site[2] * 0.95;
				double posZ = site[1] + Math.sin(angle) * site[2] * 0.95;
				assertEquals(level, DesertDunes.height(posX, posZ, WIDTH, DEPTH), 1e-9, "the ground is not level at a site");
			}
		}
	}
}
