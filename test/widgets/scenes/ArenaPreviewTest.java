package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.scene.paint.Color;
import model.Opponent;
import support.FxSupport;

/** The robots presented while the battle loads take their colors as soon as they are known, and a robot keeps them from a battle to the next. */
class ArenaPreviewTest {

	private static final int RED = 0xffff0000;
	private static final int GREEN = 0xff00ff00;
	private static final int BLUE = 0xff0000ff;

	@BeforeEach
	@AfterEach
	void forgetTheColors() {
		ArenaPreview.forget();
	}

	@Test
	void theColorsTheBattleReportsPaintTheModelAtOnce() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");
		Opponent first = ArenaSimulation.fighters().get(0);

		FxSupport.runVoid(() -> {
			arena.showcase.startPreview();
			Color before = arena.robots.get(0).bodyColor;
			arena.setRobotColors(first.getFullName() + " 1.0", RED, GREEN, BLUE);

			assertNotEquals(before, arena.robots.get(0).bodyColor);
			assertEquals(Color.RED, arena.robots.get(0).bodyColor);
		});
	}

	@Test
	void aRobotSeenInABattleIsPresentedInItsColorsInTheNext() {
		Opponent first = ArenaSimulation.fighters().get(0);
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");
		FxSupport.runVoid(() -> arena.setRobotColors(first.getFullName() + " 1.0", RED, GREEN, BLUE));

		BattleArena3D next = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");

		FxSupport.runVoid(() -> assertEquals(RED, next.showcase.previewColors(0)[0]));
	}
}
