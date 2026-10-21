package widgets.scenes;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * A seamless lava texture: dark crust broken by veins of glowing magma. Fractal
 * noise, warped by more noise so it looks like it flowed, mapped to the colors
 * of hot rock. It tiles, so it can slide over a surface forever.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class LavaTexture {

	static final int SIZE = 256;
	/** Cells of the noise across the texture, it repeats after them */
	static final int PERIOD = 6;

	/** From cold crust to white hot */
	private static final double[] STOPS = { 0, 0.22, 0.42, 0.6, 0.78, 1 };
	private static final int[] COLORS = { 0x160302, 0x3d0a03, 0x9a1d08, 0xee5410, 0xffa22a, 0xfff0a8 };

	private LavaTexture() {
	}

	/**
	 * @param crust
	 *            from 0 (all molten) to almost 1 (a cooled crust, only the veins
	 *            glow)
	 */
	static Image create(double crust) {
		WritableImage image = new WritableImage(SIZE, SIZE);
		image.getPixelWriter().setPixels(0, 0, SIZE, SIZE, PixelFormat.getIntArgbInstance(), pixels(crust), 0, SIZE);
		return image;
	}

	/**
	 * A floor of dark gray rock cracked by dimmer veins of magma: the lava with
	 * the glow lowered and the blacks lifted to the gray
	 *
	 * @param brightness
	 *            of the veins, from 0 (none) to 1 (as the lava)
	 * @param gray
	 *            the color of the rock, 0xRRGGBB
	 */
	static Image floor(double crust, double brightness, int gray) {
		int[] pixels = pixels(crust);
		for (int index = 0; index < pixels.length; index++) {
			int color = 0;
			for (int shift = 16; shift >= 0; shift -= 8) {
				int base = (gray >> shift) & 0xff;
				int glow = (int) (((pixels[index] >> shift) & 0xff) * brightness);
				// What the base leaves free, the glow fills
				color |= Math.min(255, base + glow * (255 - base) / 255) << shift;
			}
			pixels[index] = 0xff000000 | color;
		}
		WritableImage image = new WritableImage(SIZE, SIZE);
		image.getPixelWriter().setPixels(0, 0, SIZE, SIZE, PixelFormat.getIntArgbInstance(), pixels, 0, SIZE);
		return image;
	}

	/**
	 * ARGB pixels, row by row
	 */
	static int[] pixels(double crust) {
		int[] pixels = new int[SIZE * SIZE];
		for (int posY = 0; posY < SIZE; posY++) {
			for (int posX = 0; posX < SIZE; posX++) {
				double uCoord = (double) posX / SIZE * PERIOD;
				double vCoord = (double) posY / SIZE * PERIOD;
				// Domain warp: the noise is read somewhere moved by other noise
				double warpU = fbm(uCoord + 3.1, vCoord + 7.7, 3, 1);
				double warpV = fbm(uCoord + 11.3, vCoord + 2.9, 3, 2);
				double count = fbm(uCoord + 2.2 * warpU, vCoord + 2.2 * warpV, 5, 3);
				// Ridges of the noise are the veins of magma
				double ridge = 1 - Math.abs(2 * count - 1);
				double heat = Math.pow(ridge, 5) * 0.85 + Math.pow(count, 2) * 0.35;
				heat = Math.max(0, (heat - crust) / (1 - crust));
				pixels[posY * SIZE + posX] = 0xff000000 | color(Math.min(1, heat));
			}
		}
		return pixels;
	}

	private static int color(double heat) {
		for (int index = 1; index < STOPS.length; index++) {
			if (heat <= STOPS[index]) {
				double time = (heat - STOPS[index - 1]) / (STOPS[index] - STOPS[index - 1]);
				return TextureMath.mix(COLORS[index - 1], COLORS[index], time);
			}
		}
		return COLORS[COLORS.length - 1];
	}

		/**
	 * Fractal noise from 0 to 1, repeating every PERIOD
	 */
	static double fbm(double uCoord, double vCoord, int octaves, int seed) {
		double sum = 0;
		double amplitude = 0.5;
		double total = 0;
		int scale = 1;
		for (int octave = 0; octave < octaves; octave++) {
			sum += amplitude * noise(uCoord * scale, vCoord * scale, PERIOD * scale, seed + octave * 31);
			total += amplitude;
			amplitude *= 0.5;
			scale *= 2;
		}
		return sum / total;
	}

	/**
	 * Smooth value noise on a lattice that wraps after period cells
	 */
	private static double noise(double uCoord, double vCoord, int period, int seed) {
		int posX = (int) Math.floor(uCoord);
		int posY = (int) Math.floor(vCoord);
		double fxValue = uCoord - posX;
		double fyValue = vCoord - posY;
		fxValue = fxValue * fxValue * (3 - 2 * fxValue);
		fyValue = fyValue * fyValue * (3 - 2 * fyValue);
		double first = TextureMath.lattice(posX, posY, period, period, seed);
		double second = TextureMath.lattice(posX + 1, posY, period, period, seed);
		double column = TextureMath.lattice(posX, posY + 1, period, period, seed);
		double delta = TextureMath.lattice(posX + 1, posY + 1, period, period, seed);
		return (first + (second - first) * fxValue) + ((column + (delta - column) * fxValue) - (first + (second - first) * fxValue)) * fyValue;
	}

	}
