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
		int color = TextureMath.mix(0x1b2813, 0x2f5a1f, n * 1.6 - 0.2);
		color = TextureMath.mix(color, 0x62a23a, Math.max(0, n - 0.55) * 2.2);
		// Patches of soil and leaf litter
		color = TextureMath.mix(color, 0x3b2a17, Math.max(0, patch - 0.6) * 3);
		// Small bright points: clover and dew
		return TextureMath.mix(color, 0x9fd36a, Math.max(0, speck - 0.72) * 3.5);
	});

	static final Image BARK = make((u, v) -> {
		double n = LavaTexture.fbm(u, v, 5, 21);
		double ridge = 1 - Math.abs(2 * LavaTexture.fbm(u + 3, v + 8, 4, 22) - 1);
		int color = TextureMath.mix(0x24160d, 0x5b3c22, n * 1.4 - 0.1);
		// Dark cracks between the plates of the bark
		return TextureMath.mix(color, 0x120a05, Math.pow(ridge, 6) * 0.9);
	});

	static final Image LEAVES = make((u, v) -> {
		double n = LavaTexture.fbm(u, v, 5, 31);
		double speck = LavaTexture.fbm(u * 3, v * 3, 3, 33);
		int color = TextureMath.mix(0x0f3a14, 0x3f8a2a, n * 1.5 - 0.15);
		// Lights through the leaves
		return TextureMath.mix(color, 0xa6e060, Math.max(0, speck - 0.66) * 3);
	});

	// The sky seen between the canopy: deep green above, a bright green haze at
	// the horizon (the middle of the picture), dark green below it. The noise
	// stains it with lighter and darker patches of leaves.
	static final Image SKY = make((u, v) -> {
		double t = v / LavaTexture.PERIOD;
		double stain = LavaTexture.fbm(u, v * 0.7, 4, 51);
		int color;
		if (t < 0.5) {
			double up = t / 0.5;
			color = TextureMath.mix(0x0c4a2a, 0x58b878, Math.pow(up, 1.6));
		} else {
			color = TextureMath.mix(0x58b878, 0x0d3a24, Math.min(1, (t - 0.5) / 0.12));
		}
		return TextureMath.mix(color, stain > 0.5 ? 0x2f8a4c : 0x06301c, Math.abs(stain - 0.5) * 0.9);
	});

	// A shaft of light: soft across, fading at both ends, with streaks along it.
	// The alpha of the material scales it.
	static final Image BEAM = beam();

	private static Image beam() {
		int[] pixels = new int[SIZE * SIZE];
		for (int py = 0; py < SIZE; py++) {
			double along = (double) py / (SIZE - 1);
			double ends = Math.pow(Math.sin(Math.PI * Math.pow(along, 0.7)), 1.5);
			for (int px = 0; px < SIZE; px++) {
				double across = Math.abs((double) px / (SIZE - 1) * 2 - 1);
				double soft = Math.pow(Math.max(0, 1 - across * across), 3);
				// Dust in the light: slow streaks along the beam
				double streak = 0.8 + 0.2 * noise2((double) px / SIZE * 8, (double) py / SIZE * 3, 8, 3, 61);
				int alpha = (int) Math.round(255 * Math.min(1, soft * ends * streak));
				pixels[py * SIZE + px] = alpha << 24 | 0xf0f8b0;
			}
		}
		return image(pixels);
	}

	// Water: soft networks of light on a deep bottom. Few details, so it does not
	// shimmer while it slides. The relief makes the light of the sun glint on it.
	private static final double[][] WATER_FIELD = field((u, v) -> {
		double warpU = LavaTexture.fbm(u + 1.3, v + 4.1, 2, 41);
		double warpV = LavaTexture.fbm(u + 8.2, v + 6.6, 2, 42);
		double n = LavaTexture.fbm(u + 1.6 * warpU, v + 1.6 * warpV, 3, 43);
		return Math.pow(1 - Math.abs(2 * n - 1), 3);
	});
	static final Image WATER = color(WATER_FIELD, h -> TextureMath.mix(TextureMath.mix(0x0b3f4c, 0x1c8a8c, 0.2 + 0.7 * Math.sqrt(h)), 0xbff3e8, h * h * 0.55));
	static final Image WATER_RELIEF = relief(WATER_FIELD, 2.2);

	// A waterfall: streaks of foam falling, long and thin
	private static final double[][] FALL_FIELD = field((u, v) -> {
		double x = u / LavaTexture.PERIOD;
		double y = v / LavaTexture.PERIOD;
		double n = 0.5 * noise2(x * 22, y * 3, 22, 3, 51) + 0.3 * noise2(x * 44, y * 6, 44, 6, 52) + 0.2 * noise2(x * 11, y * 2, 11, 2, 53);
		return n;
	});
	static final Image WATERFALL = color(FALL_FIELD, n -> {
		double foam = Math.max(0, Math.min(1, (n - 0.4) / 0.4));
		return TextureMath.mix(TextureMath.mix(0x0e5a66, 0x2a98a0, n), 0xf2fffb, foam * foam);
	});
	static final Image WATERFALL_RELIEF = relief(FALL_FIELD, 2.8);

	private JungleTexture() {
	}

	/**
	 * The heights of a seamless field
	 */
	private static double[][] field(DoubleBinaryOperator height) {
		double[][] heights = new double[SIZE][SIZE];
		for (int py = 0; py < SIZE; py++) {
			for (int px = 0; px < SIZE; px++) {
				heights[py][px] = height.applyAsDouble((double) px / SIZE * LavaTexture.PERIOD, (double) py / SIZE * LavaTexture.PERIOD);
			}
		}
		return heights;
	}

	private static Image color(double[][] heights, java.util.function.DoubleToIntFunction color) {
		int[] pixels = new int[SIZE * SIZE];
		for (int py = 0; py < SIZE; py++) {
			for (int px = 0; px < SIZE; px++) {
				pixels[py * SIZE + px] = 0xff000000 | color.applyAsInt(heights[py][px]);
			}
		}
		return image(pixels);
	}

	/**
	 * A normal map (the relief) from the heights, wrapping at the borders
	 */
	private static Image relief(double[][] heights, double strength) {
		int[] pixels = new int[SIZE * SIZE];
		for (int py = 0; py < SIZE; py++) {
			for (int px = 0; px < SIZE; px++) {
				double dx = heights[py][(px + 1) % SIZE] - heights[py][(px + SIZE - 1) % SIZE];
				double dy = heights[(py + 1) % SIZE][px] - heights[(py + SIZE - 1) % SIZE][px];
				double nx = -dx * strength * 4;
				double ny = -dy * strength * 4;
				double length = Math.sqrt(nx * nx + ny * ny + 1);
				int r = (int) Math.round((nx / length * 0.5 + 0.5) * 255);
				int g = (int) Math.round((ny / length * 0.5 + 0.5) * 255);
				int b = (int) Math.round((1 / length * 0.5 + 0.5) * 255);
				pixels[py * SIZE + px] = 0xff000000 | r << 16 | g << 8 | b;
			}
		}
		return image(pixels);
	}

	static Image image(int[] pixels) {
		WritableImage image = new WritableImage(SIZE, SIZE);
		image.getPixelWriter().setPixels(0, 0, SIZE, SIZE, PixelFormat.getIntArgbInstance(), pixels, 0, SIZE);
		return image;
	}

	/**
	 * Smooth noise from 0 to 1 that repeats after periodU and periodV cells, with
	 * different periods: long streaks
	 */
	private static double noise2(double u, double v, int periodU, int periodV, int seed) {
		int x = (int) Math.floor(u);
		int y = (int) Math.floor(v);
		double fx = u - x;
		double fy = v - y;
		fx = fx * fx * (3 - 2 * fx);
		fy = fy * fy * (3 - 2 * fy);
		double a = TextureMath.lattice(x, y, periodU, periodV, seed);
		double b = TextureMath.lattice(x + 1, y, periodU, periodV, seed);
		double c = TextureMath.lattice(x, y + 1, periodU, periodV, seed);
		double d = TextureMath.lattice(x + 1, y + 1, periodU, periodV, seed);
		double top = a + (b - a) * fx;
		return top + (c + (d - c) * fx - top) * fy;
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

	}
