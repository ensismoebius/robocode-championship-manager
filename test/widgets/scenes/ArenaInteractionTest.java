package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import support.FxSupport;
import tournament.Bracket.Importance;

/** The keys, the mouse and the wheel of the arena, while a battle plays on it. */
class ArenaInteractionTest {

	private static void key(BattleArena3D arena, KeyCode code, boolean control) {
		FxSupport.runVoid(() -> arena.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, control, false, false)));
	}

	private static void mouse(BattleArena3D arena, javafx.event.EventType<MouseEvent> type, double x, double y) {
		FxSupport.runVoid(() -> arena.fireEvent(new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, true, false, false, null)));
	}

	private static void wheel(BattleArena3D arena, double delta) {
		FxSupport.runVoid(() -> arena.fireEvent(new ScrollEvent(ScrollEvent.SCROLL, 1, 1, 1, 1, false, false, false, false, false, false, 0, delta, 0, delta,
				ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null)));
	}

	@Test
	void theKeysChangeTheBattleAndTheViewOfTheArena() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "NEON");
		Stage window = FxSupport.show(arena);
		FxSupport.runVoid(() -> {
			arena.setStage("Final", Importance.FINAL);
			arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
			});
		});
		ArenaSimulation.frames(arena, 60, 0.05);

		// The first drag turns the cinematic view into the free one, the wheel zooms
		mouse(arena, MouseEvent.MOUSE_PRESSED, 100, 100);
		mouse(arena, MouseEvent.MOUSE_DRAGGED, 150, 120);
		mouse(arena, MouseEvent.MOUSE_DRAGGED, 160, 60);
		wheel(arena, 10);
		wheel(arena, -10);
		ArenaSimulation.frames(arena, 40, 0.05);
		assertEquals(BattleArena3D.ViewMode.FREE, FxSupport.run(() -> arena.viewMode));

		key(arena, KeyCode.V, false);
		key(arena, KeyCode.V, false);
		key(arena, KeyCode.SPACE, false);
		assertTrue(FxSupport.run(() -> arena.paused));
		key(arena, KeyCode.SPACE, false);
		assertFalse(FxSupport.run(() -> arena.paused));

		// Faster with the right key, slower with the left one, never past the ends of the table
		int start = FxSupport.run(() -> arena.speed);
		key(arena, KeyCode.RIGHT, false);
		key(arena, KeyCode.PLUS, false);
		key(arena, KeyCode.EQUALS, false);
		key(arena, KeyCode.ADD, false);
		assertEquals(Math.min(BattleArena3D.SPEEDS.length - 1, start + 4), FxSupport.run(() -> arena.speed));
		key(arena, KeyCode.LEFT, false);
		key(arena, KeyCode.MINUS, false);
		key(arena, KeyCode.SUBTRACT, false);
		assertEquals(Math.max(0, Math.min(BattleArena3D.SPEEDS.length - 1, start + 4) - 3), FxSupport.run(() -> arena.speed));

		key(arena, KeyCode.F, false);
		key(arena, KeyCode.Q, true);
		key(arena, KeyCode.ESCAPE, false);
		key(arena, KeyCode.ENTER, false);
		ArenaSimulation.frames(arena, 20, 0.05);

		// Enter goes on from the phase the battle is in: the showcase, the replay, or the playing battle
		for (BattleArena3D.Phase phase : List.of(BattleArena3D.Phase.SHOWCASE, BattleArena3D.Phase.REPLAY, BattleArena3D.Phase.PLAYING)) {
			FxSupport.runVoid(() -> arena.phase = phase);
			key(arena, KeyCode.ENTER, false);
			ArenaSimulation.frames(arena, 5, 0.05);
		}
		// The replay ends by Enter as well, when the battle is frozen on it
		FxSupport.runVoid(() -> arena.phase = BattleArena3D.Phase.FREEZE);
		FxSupport.runVoid(() -> arena.controls.skip(false));

		FxSupport.close(window);
	}
}
