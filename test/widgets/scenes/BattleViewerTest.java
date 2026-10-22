package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import support.FxSupport;

class BattleViewerTest {

	@Test
	void aViewerWithoutFightersPlaysWithoutAnEntrance() {
		BattleViewer viewer = FxSupport.run(BattleViewer::new);
		int before = FxSupport.run(() -> viewer.getChildren().size());

		FxSupport.runVoid(viewer::play);
		FxSupport.runVoid(viewer::stop);

		// Nobody was placed on the arena, so no fighter is added to the view
		assertEquals(before, FxSupport.run(() -> viewer.getChildren().size()));
	}
}
