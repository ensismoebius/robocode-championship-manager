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

	static final Image MOSS = make((uCoord, vCoord) -> {
		double count = LavaTexture.fbm(uCoord, vCoord, 5, 4);
		double patch = LavaTexture.fbm(uCoord + 5.5, vCoord + 1.3, 3, 9);
		double speck = LavaTexture.fbm(uCoord * 2, vCoord * 2, 3, 12);
		int color = TextureMath.mix(0x1b2813, 0x2f5a1f, count * 1.6 - 0.2);
		color = TextureMath.mix(color, 0x62a23a, Math.max(0, count - 0.55) * 2.2);
		// Patches of soil and leaf litter
		color = TextureMath.mix(color, 0x3b2a17, Math.max(0, patch - 0.6) * 3);
		// Small bright points: clover and dew
		return TextureMath.mix(color, 0x9fd36a, Math.max(0, speck - 0.72) * 3.5);
	});

	static final Image BARK = make((uCoord, vCoord) -> {
		double count = LavaTexture.fbm(uCoord, vCoord, 5, 21);
		double ridge = 1 - Math.abs(2 * LavaTexture.fbm(uCoord + 3, vCoord + 8, 4, 22) - 1);
		int color = TextureMath.mix(0x24160d, 0x5b3c22, count * 1.4 - 0.1);
		// Dark cracks between the plates of the bark
		return TextureMath.mix(color, 0x120a05, Math.pow(ridge, 6) * 0.9);
	});

	static final Image LEAVES = make((uCoord, vCoord) -> {
		double count = LavaTexture.fbm(uCoord, vCoord, 5, 31);
		double speck = LavaTexture.fbm(uCoord * 3, vCoord * 3, 3, 33);
		int color = TextureMath.mix(0x0f3a14, 0x3f8a2a, count * 1.5 - 0.15);
		// Lights through the leaves
		return TextureMath.mix(color, 0xa6e060, Math.max(0, speck - 0.66) * 3);
	});

	// The sky seen between the canopy: deep green above, a bright green haze at
	// the horizon (the middle of the picture), dark green below it. The noise
	// stains it with lighter and darker patches of leaves.
	static final Image SKY = make((uCoord, vCoord) -> {
		double time = vCoord / LavaTexture.PERIOD;
		double stain = LavaTexture.fbm(uCoord, vCoord * 0.7, 4, 51);
		int color;
		if (time < 0.5) {
			double upValue = time / 0.5;
			color = TextureMath.mix(0x0c4a2a, 0x58b878, Math.pow(upValue, 1.6));
		} else {
			color = TextureMath.mix(0x58b878, 0x0d3a24, Math.min(1, (time - 0.5) / 0.12));
		}
		return TextureMath.mix(color, stain > 0.5 ? 0x2f8a4c : 0x06301c, Math.abs(stain - 0.5) * 0.9);
	});

	// A shaft of light: soft across, fading at both ends, with streaks along it.
	// The alpha of the material scales it.
	static final Image BEAM = beam();

	private static Image beam() {
		int[] pixels = new int[SIZE * SIZE];
		for (int posY = 0; posY < SIZE; posY++) {
			double along = (double) posY / (SIZE - 1);
			double ends = Math.pow(Math.sin(Math.PI * Math.pow(along, 0.7)), 1.5);
			for (int posX = 0; posX < SIZE; posX++) {
				double across = Math.abs((double) posX / (SIZE - 1) * 2 - 1);
				double soft = Math.pow(Math.max(0, 1 - across * across), 3);
				// Dust in the light: slow streaks along the beam
				double streak = 0.8 + 0.2 * noise2((double) posX / SIZE * 8, (double) posY / SIZE * 3, 8, 3, 61);
				int alpha = (int) Math.round(255 * Math.min(1, soft * ends * streak));
				pixels[posY * SIZE + posX] = alpha << 24 | 0xf0f8b0;
			}
		}
		return image(pixels);
	}

	// Water: soft networks of light on a deep bottom. Few details, so it does not
	// shimmer while it slides. The relief makes the light of the sun glint on it.
	private static final double[][] WATER_FIELD = field((uCoord, vCoord) -> {
		double warpU = LavaTexture.fbm(uCoord + 1.3, vCoord + 4.1, 2, 41);
		double warpV = LavaTexture.fbm(uCoord + 8.2, vCoord + 6.6, 2, 42);
		double count = LavaTexture.fbm(uCoord + 1.6 * warpU, vCoord + 1.6 * warpV, 3, 43);
		return Math.pow(1 - Math.abs(2 * count - 1), 3);
	});
	static final Image WATER = color(WATER_FIELD, height -> TextureMath.mix(TextureMath.mix(0x0b3f4c, 0x1c8a8c, 0.2 + 0.7 * Math.sqrt(height)), 0xbff3e8, height * height * 0.55));
	static final Image WATER_RELIEF = relief(WATER_FIELD, 2.2);

	// A waterfall: streaks of foam falling, long and thin
	private static final double[][] FALL_FIELD = field((uCoord, vCoord) -> {
		double posX = uCoord / LavaTexture.PERIOD;
		double posY = vCoord / LavaTexture.PERIOD;
		double count = 0.5 * noise2(posX * 22, posY * 3, 22, 3, 51) + 0.3 * noise2(posX * 44, posY * 6, 44, 6, 52) + 0.2 * noise2(posX * 11, posY * 2, 11, 2, 53);
		return count;
	});
	static final Image WATERFALL = color(FALL_FIELD, count -> {
		double foam = Math.max(0, Math.min(1, (count - 0.4) / 0.4));
		return TextureMath.mix(TextureMath.mix(0x0e5a66, 0x2a98a0, count), 0xf2fffb, foam * foam);
	});
	static final Image WATERFALL_RELIEF = relief(FALL_FIELD, 2.8);

	private JungleTexture() {
	}

	/**
	 * The heights of a seamless field
	 */
	private static double[][] field(DoubleBinaryOperator height) {
		double[][] heights = new double[SIZE][SIZE];
		for (int posY = 0; posY < SIZE; posY++) {
			for (int posX = 0; posX < SIZE; posX++) {
				heights[posY][posX] = height.applyAsDouble((double) posX / SIZE * LavaTexture.PERIOD, (double) posY / SIZE * LavaTexture.PERIOD);
			}
		}
		return heights;
	}

	private static Image color(double[][] heights, java.util.function.DoubleToIntFunction color) {
		int[] pixels = new int[SIZE * SIZE];
		for (int posY = 0; posY < SIZE; posY++) {
			for (int posX = 0; posX < SIZE; posX++) {
				pixels[posY * SIZE + posX] = 0xff000000 | color.applyAsInt(heights[posY][posX]);
			}
		}
		return image(pixels);
	}

	/**
	 * A normal map (the relief) from the heights, wrapping at the borders
	 */
	private static Image relief(double[][] heights, double strength) {
		int[] pixels = new int[SIZE * SIZE];
		for (int posY = 0; posY < SIZE; posY++) {
			for (int posX = 0; posX < SIZE; posX++) {
				double deltaX = heights[posY][(posX + 1) % SIZE] - heights[posY][(posX + SIZE - 1) % SIZE];
				double deltaY = heights[(posY + 1) % SIZE][posX] - heights[(posY + SIZE - 1) % SIZE][posX];
				double nxValue = -deltaX * strength * 4;
				double nyValue = -deltaY * strength * 4;
				double length = Math.sqrt(nxValue * nxValue + nyValue * nyValue + 1);
				int row = (int) Math.round((nxValue / length * 0.5 + 0.5) * 255);
				int graphics = (int) Math.round((nyValue / length * 0.5 + 0.5) * 255);
				int second = (int) Math.round((1 / length * 0.5 + 0.5) * 255);
				pixels[posY * SIZE + posX] = 0xff000000 | row << 16 | graphics << 8 | second;
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
	private static double noise2(double uCoord, double vCoord, int periodU, int periodV, int seed) {
		int posX = (int) Math.floor(uCoord);
		int posY = (int) Math.floor(vCoord);
		double fxValue = uCoord - posX;
		double fyValue = vCoord - posY;
		fxValue = fxValue * fxValue * (3 - 2 * fxValue);
		fyValue = fyValue * fyValue * (3 - 2 * fyValue);
		double first = TextureMath.lattice(posX, posY, periodU, periodV, seed);
		double second = TextureMath.lattice(posX + 1, posY, periodU, periodV, seed);
		double column = TextureMath.lattice(posX, posY + 1, periodU, periodV, seed);
		double delta = TextureMath.lattice(posX + 1, posY + 1, periodU, periodV, seed);
		double top = first + (second - first) * fxValue;
		return top + (column + (delta - column) * fxValue - top) * fyValue;
	}

		private static Image make(DoubleBinaryOperator color) {
		int[] pixels = new int[SIZE * SIZE];
		for (int posY = 0; posY < SIZE; posY++) {
			for (int posX = 0; posX < SIZE; posX++) {
				pixels[posY * SIZE + posX] = 0xff000000 | (int) color.applyAsDouble((double) posX / SIZE * LavaTexture.PERIOD, (double) posY / SIZE * LavaTexture.PERIOD);
			}
		}
		WritableImage image = new WritableImage(SIZE, SIZE);
		image.getPixelWriter().setPixels(0, 0, SIZE, SIZE, PixelFormat.getIntArgbInstance(), pixels, 0, SIZE);
		return image;
	}

	}
