package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import org.junit.jupiter.api.Test;

import support.FxSupport;

/** The pictures of the flames: warm ones with a blue base, blue ones, and different from one another. */
class FireTexturesTest {

	/** The color of the pixel at a fraction of the width and of the height of the picture, from the top */
	private static int pixel(Image image, double across, double down) {
		PixelReader reader = image.getPixelReader();
		return reader.getArgb((int) (across * (image.getWidth() - 1)), (int) (down * (image.getHeight() - 1)));
	}

	private static int red(int argb) {
		return (argb >> 16) & 255;
	}

	private static int blue(int argb) {
		return argb & 255;
	}

	private static int alpha(int argb) {
		return argb >>> 24;
	}

	@Test
	void thereAreWarmFlamesAndBlueOnes() {
		Image[][] textures = FxSupport.run(() -> FireTextures.FLAME_TEXTURES);

		assertEquals(FireTextures.FLAME_VARIANTS + FireTextures.BLUE_VARIANTS, textures.length);
		FxSupport.runVoid(() -> {
			for (int index = 0; index < FireTextures.FLAME_VARIANTS; index++) {
				// The core, in the middle of the lower body: yellow, more red than blue, and opaque
				int core = pixel(textures[index][0], 0.5, 0.75);
				assertTrue(alpha(core) > 150, "opaque core");
				assertTrue(red(core) > blue(core) + 60, "warm: " + Integer.toHexString(core));
			}
			for (int index = FireTextures.FLAME_VARIANTS; index < textures.length; index++) {
				int core = pixel(textures[index][0], 0.5, 0.75);
				assertTrue(blue(core) > red(core) + 20, "blue: " + Integer.toHexString(core));
			}
		});
	}

	@Test
	void everyFlameIsBlueWhereItIsBornAndTransparentOutsideItsBody() {
		FxSupport.runVoid(() -> {
			for (int index = 0; index < FireTextures.FLAME_VARIANTS; index++) {
				Image flame = FireTextures.FLAME_TEXTURES[index][0];
				// Just over the bottom, in the middle: blue
				int base = pixel(flame, 0.5, 0.97);
				assertTrue(blue(base) > red(base) - 40, "blue at the base: " + Integer.toHexString(base));
				// The corners of the picture are air
				assertEquals(0, alpha(pixel(flame, 0.02, 0.05)));
				assertEquals(0, alpha(pixel(flame, 0.98, 0.05)));
			}
		});
	}

	@Test
	void theVariantsAreNotTheSameFlame() {
		FxSupport.runVoid(() -> {
			int different = 0;
			Image first = FireTextures.FLAME_TEXTURES[0][0];
			for (int index = 1; index < FireTextures.FLAME_VARIANTS; index++) {
				for (int step = 0; step < 40; step++) {
					double across = (step % 8 + 0.5) / 8;
					double down = (step / 8 + 0.5) / 5;
					if (pixel(first, across, down) != pixel(FireTextures.FLAME_TEXTURES[index][0], across, down)) {
						different++;
					}
				}
			}
			assertTrue(different > 20, "the flames differ: " + different);
		});
	}

	@Test
	void theFramesOfAFlameAreItsAnimation() {
		FxSupport.runVoid(() -> {
			Image[] frames = FireTextures.FLAME_TEXTURES[0];
			assertEquals(FireTextures.FLAME_FRAMES, frames.length);
			// The noise scrolls from one frame to the next: the flame is torn in other places
			int changed = 0;
			for (int step = 0; step < 60; step++) {
				double across = 0.2 + 0.6 * (step % 10) / 10;
				double down = 0.1 + 0.5 * (step / 10) / 6;
				if (pixel(frames[0], across, down) != pixel(frames[FireTextures.FLAME_FRAMES - 1], across, down)) {
					changed++;
				}
			}
			assertTrue(changed > 5, "the flame moves: " + changed);
		});
	}
}
