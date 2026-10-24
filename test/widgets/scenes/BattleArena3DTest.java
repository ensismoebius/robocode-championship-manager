package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import support.FxSupport;
import tournament.Importance;
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
	void theMachinesAreSilentUntilTheFightStarts() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		ArenaTicker ticker = new ArenaTicker(arena);

		FxSupport.runVoid(() -> {
			for (Phase phase : Phase.values()) {
				arena.phase = phase;
				boolean fighting = phase == Phase.PLAYING || phase == Phase.FREEZE || phase == Phase.REPLAY;
				assertEquals(fighting ? 50 : 0, ticker.heard(50), 1e-9, phase.name());
			}
		});
	}

	@Test
	void theLoadingShowsHowFarTheBattleIsSimulated() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());

		FxSupport.runVoid(() -> {
			arena.setLoadingProgress(0.376);
			assertEquals(0.376, arena.loadingBar.getProgress(), 1e-9);
			assertTrue(arena.loading.getText().endsWith("38%"), arena.loading.getText());
			// Never out of the bar
			arena.setLoadingProgress(7);
			assertEquals(1, arena.loadingBar.getProgress(), 1e-9);

			// The battle is shown: the loading and its bar are gone
			arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
			});
			assertFalse(arena.loading.isVisible());
			assertFalse(arena.loadingBar.isVisible());
		});
	}

	@Test
	void onlySemifinalsAndFinalsAreWarned() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());

		FxSupport.runVoid(() -> {
			arena.setStage("Quartas de final", Importance.REGULAR);
			assertFalse(arena.stageLabel.isVisible());
			arena.setStage("Semifinal", Importance.SEMIFINAL);
			assertTrue(arena.stageLabel.isVisible());
			arena.setStage("Fim da repescagem", Importance.FINAL);
			assertTrue(arena.stageLabel.isVisible());
			assertEquals("★ FIM DA REPESCAGEM ★", arena.stageLabel.getText());
		});
	}

	@Test
	void theForceFieldIsDrawnBeforeTheSmokeInFrontOfIt() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());

		FxSupport.runVoid(() -> {
			// The see-through particles come last, so they blend over the force field instead of hiding it
			List<Node> layers = arena.world.getChildren();
			assertTrue(layers.indexOf(arena.fence) < layers.indexOf(arena.fireLayer));
			assertTrue(layers.indexOf(arena.fence) < layers.indexOf(arena.sunbeams));
		});
	}

	@Test
	void theRobotsTakeTheColorsTheyChangeToDuringTheBattle() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		// Walls starts black and turns red before its last turn of the first round
		List<String> output = new ArrayList<String>(ArenaSimulation.OUTPUT);
		int lastTurn = output.indexOf(output.stream().filter(line -> line.startsWith("#T 0 199")).findFirst().orElseThrow());
		output.add(lastTurn, "#C 0 -65536 -16711936 -14336");
		BattleRecording recording = ArenaSimulation.recording(output);

		assertTrue(ArenaSimulation.battle(arena, recording, 4000, 0.05));

		RobotView walls = FxSupport.run(() -> arena.robots.get(0));
		assertEquals(Color.rgb(255, 0, 0), walls.bodyColor);
		assertEquals(Color.rgb(0, 255, 0), walls.gunColor);
		assertEquals(Color.rgb(255, 0, 0), FxSupport.run(() -> walls.bodyMaterial.getDiffuseColor()));
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
		assertEquals(0.0, ArenaHelpers.lerp(0, 10, 0), 1e-9);
		assertEquals(10.0, ArenaHelpers.lerp(0, 10, 1), 1e-9);
		assertEquals(5.0, ArenaHelpers.lerp(0, 10, 0.5), 1e-9);
		// The short way round: from almost a full turn to a little past zero
		assertEquals(Math.PI * 2 + 0.1, ArenaHelpers.lerpAngle(Math.PI * 2 - 0.1, 0.1, 1), 1e-9);
	}

	@Test
	void colorsBecomeCssText() {
		assertEquals("#ff0000", ArenaHelpers.toCss(Color.RED));
		assertEquals("#000000", ArenaHelpers.toCss(Color.BLACK));
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
