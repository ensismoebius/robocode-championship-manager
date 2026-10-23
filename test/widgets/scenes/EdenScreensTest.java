package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import support.FxSupport;

/** The great screens of the Eden Solar: they sit between the ribs of the roof and show the battle filmed from above. */
class EdenScreensTest {

	@Test
	void theScreensAreWiderWhereThePlanIsNarrowAndStayBetweenTheRibs() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");
		EdenScreens screens = arena.eden.screens();

		assertNotNull(screens);
		for (int center = 0; center < EdenStadium.SEGMENTS; center += EdenScreens.EVERY) {
			int width = screens.width(center);
			assertTrue(width >= 4 && width <= 10, "screen at " + center + " takes " + width + " pieces");
			assertTrue(screens.covers(EdenStadium.angle(center)), "the middle of the screen is covered");
			// The ribs of the roof grow halfway between two screens
			assertFalse(screens.covers(EdenStadium.angle(center + EdenScreens.EVERY / 2)), "a rib would be in front of the screen");
		}
	}

	@Test
	void otherArenasHaveNoScreens() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");

		assertNull(arena.eden.screens());
	}

	@Test
	void theScreensShowThePictureOfTheBattleOnceTheSceneIsShown() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");
		EdenScreens screens = arena.eden.screens();
		Stage window = FxSupport.show(arena);
		try {
			FxSupport.sleep(500);
			ArenaSimulation.frames(arena, 30, 0.05);
			FxSupport.runVoid(() -> assertTrue(screens.pictures() > 0, "no picture was taken"));

			FxSupport.runVoid(() -> {
				WritableImage picture = screens.picture();
				double sum = 0;
				for (int posX = 0; posX < picture.getWidth(); posX += 4) {
					for (int posY = 0; posY < picture.getHeight(); posY += 4) {
						sum += picture.getPixelReader().getColor(posX, posY).getBrightness();
					}
				}
				double mean = sum / (Math.ceil(picture.getWidth() / 4) * Math.ceil(picture.getHeight() / 4));
				assertTrue(mean > 0.1, "the picture of the screens is dark: " + mean);
			});
		} finally {
			FxSupport.close(window);
		}
	}
}
