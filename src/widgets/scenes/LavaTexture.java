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
	 * Lava that glows less: as the other, with all the colors darker
	 *
	 * @param brightness
	 *            from 0 (black) to 1 (the same)
	 */
	static Image create(double crust, double brightness) {
		int[] pixels = pixels(crust);
		for (int i = 0; i < pixels.length; i++) {
			int red = (int) (((pixels[i] >> 16) & 0xff) * brightness);
			int green = (int) (((pixels[i] >> 8) & 0xff) * brightness);
			int blue = (int) ((pixels[i] & 0xff) * brightness);
			pixels[i] = 0xff000000 | red << 16 | green << 8 | blue;
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
		for (int py = 0; py < SIZE; py++) {
			for (int px = 0; px < SIZE; px++) {
				double u = (double) px / SIZE * PERIOD;
				double v = (double) py / SIZE * PERIOD;
				// Domain warp: the noise is read somewhere moved by other noise
				double warpU = fbm(u + 3.1, v + 7.7, 3, 1);
				double warpV = fbm(u + 11.3, v + 2.9, 3, 2);
				double n = fbm(u + 2.2 * warpU, v + 2.2 * warpV, 5, 3);
				// Ridges of the noise are the veins of magma
				double ridge = 1 - Math.abs(2 * n - 1);
				double heat = Math.pow(ridge, 5) * 0.85 + Math.pow(n, 2) * 0.35;
				heat = Math.max(0, (heat - crust) / (1 - crust));
				pixels[py * SIZE + px] = 0xff000000 | color(Math.min(1, heat));
			}
		}
		return pixels;
	}

	private static int color(double heat) {
		for (int i = 1; i < STOPS.length; i++) {
			if (heat <= STOPS[i]) {
				double t = (heat - STOPS[i - 1]) / (STOPS[i] - STOPS[i - 1]);
				return mix(COLORS[i - 1], COLORS[i], t);
			}
		}
		return COLORS[COLORS.length - 1];
	}

	private static int mix(int a, int b, double t) {
		int r = (int) Math.round(((a >> 16) & 0xff) * (1 - t) + ((b >> 16) & 0xff) * t);
		int g = (int) Math.round(((a >> 8) & 0xff) * (1 - t) + ((b >> 8) & 0xff) * t);
		int bl = (int) Math.round((a & 0xff) * (1 - t) + (b & 0xff) * t);
		return r << 16 | g << 8 | bl;
	}

	/**
	 * Fractal noise from 0 to 1, repeating every PERIOD
	 */
	static double fbm(double u, double v, int octaves, int seed) {
		double sum = 0;
		double amplitude = 0.5;
		double total = 0;
		int scale = 1;
		for (int octave = 0; octave < octaves; octave++) {
			sum += amplitude * noise(u * scale, v * scale, PERIOD * scale, seed + octave * 31);
			total += amplitude;
			amplitude *= 0.5;
			scale *= 2;
		}
		return sum / total;
	}

	/**
	 * Smooth value noise on a lattice that wraps after period cells
	 */
	private static double noise(double u, double v, int period, int seed) {
		int x = (int) Math.floor(u);
		int y = (int) Math.floor(v);
		double fx = u - x;
		double fy = v - y;
		fx = fx * fx * (3 - 2 * fx);
		fy = fy * fy * (3 - 2 * fy);
		double a = lattice(x, y, period, seed);
		double b = lattice(x + 1, y, period, seed);
		double c = lattice(x, y + 1, period, seed);
		double d = lattice(x + 1, y + 1, period, seed);
		return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
	}

	private static double lattice(int x, int y, int period, int seed) {
		int h = Math.floorMod(x, period) * 374761393 + Math.floorMod(y, period) * 668265263 + seed * 1442695041;
		h = (h ^ (h >>> 13)) * 1274126177;
		return ((h ^ (h >>> 16)) & 0xffff) / 65535.0;
	}
}
