package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import model.Opponent;
import support.ChampionshipFixture;
import support.FxSupport;
import widgets.scenes.ArenaHud.HudPlayer;

class ArenaHudTest {

	private static BattleArena3D playedArena() {
		List<Opponent> fighters = ArenaSimulation.fighters();
		for (Opponent fighter : fighters) {
			fighter.setImage(ChampionshipFixture.logo());
		}
		BattleArena3D arena = ArenaSimulation.arena(fighters);
		FxSupport.runVoid(() -> arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
		}));
		return arena;
	}

	/** How much longer than the energy the trail is. */
	private static double lead(HudPlayer player) {
		return FxSupport.run(() -> player.lag().getMaxWidth() - player.energy().getMaxWidth());
	}

	@Test
	void theDamageTrailCatchesUpEvenWhileTheRobotKeepsLosingEnergy() {
		BattleArena3D arena = playedArena();
		HudPlayer player = FxSupport.run(() -> arena.hudPlayers.get(0));
		int index = player.index();

		// A burning robot: a little less energy at every frame, for two seconds
		for (int step = 0; step <= 40; step++) {
			double energy = 100 - step;
			FxSupport.runVoid(() -> arena.hud.updateHud(Map.of(index, energy)));
			FxSupport.sleep(50);
		}
		double energyWidth = FxSupport.run(() -> player.energy().getMaxWidth());
		double trailWidth = FxSupport.run(() -> player.lag().getMaxWidth());
		assertTrue(trailWidth - energyWidth > 1, "the trail is lit while it burns");

		// The damage stops: the trail drains down to the energy
		for (int step = 0; step < 30 && lead(player) > 0.5; step++) {
			FxSupport.runVoid(() -> arena.hud.updateHud(Map.of(index, 60.0)));
			FxSupport.sleep(100);
		}
		assertEquals(FxSupport.run(() -> player.energy().getMaxWidth()), FxSupport.run(() -> player.lag().getMaxWidth()), 0.5);
		assertFalse(lead(player) > 0.5);
	}
}
