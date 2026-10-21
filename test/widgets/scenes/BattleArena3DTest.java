package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.scene.paint.Color;
import support.FxSupport;
import utils.robocode.BattleRecording;

class BattleArena3DTest {

	@Test
	void aWholeBattleRunsToItsEnd() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		BattleRecording recording = ArenaSimulation.recording(ArenaSimulation.OUTPUT);

		boolean finished = ArenaSimulation.battle(arena, recording, 4000, 0.05);

		assertTrue(finished, "the battle should call back when it ends");
	}

	@Test
	void everyArenaStyleRunsItsOwnBattle() {
		for (ArenaStyle style : ArenaStyle.values()) {
			BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), style.name());
			BattleRecording recording = ArenaSimulation.recording(ArenaSimulation.OUTPUT);

			assertTrue(ArenaSimulation.battle(arena, recording, 4000, 0.05), style.name());
			assertEquals(style, arena.style);
		}
	}

	@Test
	void anUnknownArenaFallsBackToARandomStyle() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "NOWHERE");

		assertTrue(ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 4000, 0.05));
		assertNotNull(arena.style);
	}

	@Test
	void theCameraViewsCycleAndTheAudioFadesOut() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 200, 0.05);

		FxSupport.runVoid(() -> {
			for (int index = 0; index < 5; index++) {
				arena.cycleView();
			}
			arena.fadeOutAudio(0.5);
			arena.stopShowcaseMusic();
		});
		ArenaSimulation.frames(arena, 40, 0.05);
	}

	@Test
	void interpolationMovesFromOneValueToTheOther() {
		assertEquals(0.0, BattleArena3D.lerp(0, 10, 0), 1e-9);
		assertEquals(10.0, BattleArena3D.lerp(0, 10, 1), 1e-9);
		assertEquals(5.0, BattleArena3D.lerp(0, 10, 0.5), 1e-9);
		// The short way round: from almost a full turn to a little past zero
		assertEquals(Math.PI * 2 + 0.1, BattleArena3D.lerpAngle(Math.PI * 2 - 0.1, 0.1, 1), 1e-9);
	}

	@Test
	void colorsBecomeCssText() {
		assertEquals("#ff0000", BattleArena3D.toCss(Color.RED));
		assertEquals("#000000", BattleArena3D.toCss(Color.BLACK));
	}

	@Test
	void theWorldFollowsTheFieldOfTheRecording() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 1, 0.05);

		FxSupport.runVoid(() -> {
			// The field of the recording is 800 by 600: its centre is the origin of the world
			assertEquals(0.0, arena.worldX(400), 1e-9);
			assertEquals(-300.0, arena.worldZ(0), 1e-9);
			assertEquals(-200.0, arena.worldZ(100), 1e-9);
		});
	}
}
