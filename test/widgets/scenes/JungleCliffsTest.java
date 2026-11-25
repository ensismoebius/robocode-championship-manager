package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The highlands of the jungle arena: their wall stands beyond the field at both ends, and the rivers run out of gorges in it.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
class JungleCliffsTest {

	@Test
	void theWallStandsAtBothEndsBeyondTheFieldWithGorgesForTheRivers() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");
		JungleCliffs cliffs = arena.jungle.cliffs();
		double width = arena.fieldWidth;
		double riverZ = arena.fieldHeight / 2 + 160;

		assertFalse(cliffs.covers(0, 0, 0), "the field is open");
		for (int side = -1; side <= 1; side += 2) {
			// Far behind the face, out of the gorges, it is rock
			assertTrue(cliffs.covers(side * (width / 2 + 1700), 0, 0));
			assertTrue(cliffs.covers(side * (width / 2 + 1700), 1200, 0));
			// In front of the face, where the pool is, it is not
			assertFalse(cliffs.covers(side * (width / 2 + 800), 0, 0));
			// The rivers go on through the wall, in their gorges
			for (double posX = width / 2 + 1000; posX < width / 2 + 2000; posX += 50) {
				assertFalse(cliffs.covers(side * posX, riverZ + LavaFlows.bend(side * posX, 2.3), 0), "a river runs into the rock at " + side * posX);
				assertFalse(cliffs.covers(side * posX, -(riverZ + LavaFlows.bend(side * posX, 0)), 0), "a river runs into the rock at " + side * posX);
			}
		}
	}
}
