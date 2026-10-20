package widgets.scenes;

import java.util.function.DoubleBinaryOperator;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Seamless textures of the rain forest, from noise: moss, bark, leaves and
 * water. They are only made when the arena is used.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class JungleTexture {

	private static final int SIZE = 256;

	static final Image MOSS = make((u, v) -> {
		double n = LavaTexture.fbm(u, v, 5, 4);
		double patch = LavaTexture.fbm(u + 5.5, v + 1.3, 3, 9);
		double speck = LavaTexture.fbm(u * 2, v * 2, 3, 12);
		int color = mix(0x1b2813, 0x2f5a1f, n * 1.6 - 0.2);
		color = mix(color, 0x62a23a, Math.max(0, n - 0.55) * 2.2);
		// Patches of soil and leaf litter
		color = mix(color, 0x3b2a17, Math.max(0, patch - 0.6) * 3);
		// Small bright points: clover and dew
		return mix(color, 0x9fd36a, Math.max(0, speck - 0.72) * 3.5);
	});

	static final Image BARK = make((u, v) -> {
		double n = LavaTexture.fbm(u, v, 5, 21);
		double ridge = 1 - Math.abs(2 * LavaTexture.fbm(u + 3, v + 8, 4, 22) - 1);
		int color = mix(0x24160d, 0x5b3c22, n * 1.4 - 0.1);
		// Dark cracks between the plates of the bark
		return mix(color, 0x120a05, Math.pow(ridge, 6) * 0.9);
	});

	static final Image LEAVES = make((u, v) -> {
		double n = LavaTexture.fbm(u, v, 5, 31);
		double speck = LavaTexture.fbm(u * 3, v * 3, 3, 33);
		int color = mix(0x0f3a14, 0x3f8a2a, n * 1.5 - 0.15);
		// Lights through the leaves
		return mix(color, 0xa6e060, Math.max(0, speck - 0.66) * 3);
	});

	static final Image WATER = make((u, v) -> {
		double warpU = LavaTexture.fbm(u + 1.3, v + 4.1, 3, 41);
		double warpV = LavaTexture.fbm(u + 8.2, v + 6.6, 3, 42);
		double n = LavaTexture.fbm(u + 1.8 * warpU, v + 1.8 * warpV, 4, 43);
		int color = mix(0x0b3d4a, 0x1f8c8c, n * 1.5 - 0.2);
		// The light that the water bends on the bottom: thin bright lines
		double ridge = 1 - Math.abs(2 * n - 1);
		return mix(color, 0xcffff2, Math.pow(ridge, 9) * 0.95);
	});

	private JungleTexture() {
	}

	private static Image make(DoubleBinaryOperator color) {
		int[] pixels = new int[SIZE * SIZE];
		for (int py = 0; py < SIZE; py++) {
			for (int px = 0; px < SIZE; px++) {
				pixels[py * SIZE + px] = 0xff000000 | (int) color.applyAsDouble((double) px / SIZE * LavaTexture.PERIOD, (double) py / SIZE * LavaTexture.PERIOD);
			}
		}
		WritableImage image = new WritableImage(SIZE, SIZE);
		image.getPixelWriter().setPixels(0, 0, SIZE, SIZE, PixelFormat.getIntArgbInstance(), pixels, 0, SIZE);
		return image;
	}

	private static int mix(int a, int b, double t) {
		t = Math.max(0, Math.min(1, t));
		int r = (int) Math.round(((a >> 16) & 0xff) * (1 - t) + ((b >> 16) & 0xff) * t);
		int g = (int) Math.round(((a >> 8) & 0xff) * (1 - t) + ((b >> 8) & 0xff) * t);
		int bl = (int) Math.round((a & 0xff) * (1 - t) + (b & 0xff) * t);
		return r << 16 | g << 8 | bl;
	}
}
