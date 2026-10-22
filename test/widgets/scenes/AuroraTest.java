package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.paint.Color;
import support.FxSupport;

class AuroraTest {

	private static final int WIDTH = 384;
	private static final int HEIGHT = 192;

	private static int[] pixels(Image image) {
		int[] pixels = new int[WIDTH * HEIGHT];
		image.getPixelReader().getPixels(0, 0, WIDTH, HEIGHT, PixelFormat.getIntArgbInstance(), pixels, 0, WIDTH);
		return pixels;
	}

	@Test
	void theCurtainsArePaintedOverTheSkyInTheBand() {
		Aurora aurora = FxSupport.run(() -> new Aurora(Color.NAVY));
		int[] picture = FxSupport.run(() -> pixels(aurora.image()));
		// The opaque sky colour of NAVY
		int sky = 0xff000080;

		// Above and below the band of the curtains there is only the sky
		assertEquals(sky, picture[0]);
		assertEquals(sky, picture[(HEIGHT - 1) * WIDTH]);
		// Inside the band the curtains light some pixels
		assertTrue(Arrays.stream(picture).anyMatch(pixel -> pixel != sky));
	}

	@Test
	void theCurtainsMoveWithTheTime() {
		Aurora aurora = FxSupport.run(() -> new Aurora(Color.NAVY));
		int[] before = FxSupport.run(() -> pixels(aurora.image()));

		FxSupport.runVoid(() -> aurora.update(3.0));
		int[] after = FxSupport.run(() -> pixels(aurora.image()));

		assertFalse(Arrays.equals(before, after));
	}
}
