package widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import javafx.scene.paint.Color;
import support.FxSupport;

/** The animated widgets, shown in a window so their frames run. */
class AnimatedWidgetsTest {

	@Test
	void celebrationStartsEveryKindOfEffect() {
		Celebration celebration = FxSupport.run(Celebration::new);
		FxSupport.runVoid(() -> {
			celebration.setPrefSize(800, 600);
			celebration.layoutChildren();
			celebration.setRays(Color.GOLD, 400, 300);
			celebration.setAutoFireworks(2, Color.RED, Color.BLUE);
			celebration.setRain(20);
			celebration.flare();
			celebration.burst(100, 100, Color.RED, 30);
			celebration.rocket(200, Color.GREEN, 10);
			celebration.cannons(5);
			celebration.popper(300, 300, 20, Color.YELLOW);
			celebration.popper(300, 300, 5);
		});
		FxSupport.showFor(celebration, 600);

		FxSupport.runVoid(() -> {
			for (int frame = 0; frame < 30; frame++) {
				celebration.step(0.05, 0.05 * frame);
			}
			celebration.clear();
			celebration.setAutoFireworks(0);
			celebration.setRain(0);
		});
		assertFalse(celebration.getChildren().isEmpty());
	}

	@Test
	void celebrationWithoutSizeDoesNotDraw() {
		Celebration celebration = FxSupport.run(Celebration::new);
		FxSupport.runVoid(() -> {
			celebration.setPrefSize(0, 0);
			celebration.layoutChildren();
			celebration.step(0.05, 1);
		});
		assertEquals(1, celebration.getChildren().size());
	}

	@Test
	void confettiFallsAcrossTheWindow() {
		Confetti confetti = FxSupport.run(Confetti::new);
		FxSupport.showFor(confetti, 400);
		FxSupport.runVoid(() -> {
			confetti.setPrefSize(800, 600);
			confetti.layoutChildren();
			for (int frame = 0; frame < 20; frame++) {
				confetti.step(0.1, 0.1 * frame);
			}
		});
		assertFalse(confetti.getChildren().isEmpty());
	}

	@Test
	void arcadeBackgroundDrawsItsGrid() {
		ArcadeBackground plain = FxSupport.run(ArcadeBackground::new);
		ArcadeBackground coloured = FxSupport.run(() -> new ArcadeBackground(Color.RED));
		FxSupport.showFor(plain, 300);
		FxSupport.showFor(coloured, 300);
		FxSupport.runVoid(() -> {
			plain.setPrefSize(800, 600);
			plain.layoutChildren();
		});
		assertFalse(plain.getChildren().isEmpty());
	}
}
