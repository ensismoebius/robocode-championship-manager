package widgets.scenes;

import java.util.Random;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Pictures for the neon arena: the floor with its circuits, the facades of the
 * towers with their lit windows, the night sky and the big setting sun. All of
 * them are drawn by code.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class NeonTexture {

	/** Units of the world one tile of the floor covers */
	static final double FLOOR_TILE = 100;
	/** Units of the world one tile of the facade covers, across and up */
	static final double FACADE_WIDTH = 160;
	static final double FACADE_HEIGHT = 240;

	private static final int[] WINDOW_COLORS = { 0x2ec4ff, 0x2ec4ff, 0xff3fa4, 0xffd24d, 0xe8f4ff, 0x7dff9a };

	static final Image FLOOR = floor();
	/** The walls of a tower: unlit windows */
	static final Image FACADE = facade(false);
	/** Only the lit windows, the rest black: the tower shines with it */
	static final Image FACADE_GLOW = facade(true);
	static final Image SKY = sky();
	static final Image SUN = sun();

	private NeonTexture() {
	}

	private static Image image(int width, int height, int[] pixels) {
		WritableImage image = new WritableImage(width, height);
		image.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
		return image;
	}

	/**
	 * A panel of dark metal: seams around it, fine traces of circuit and a few
	 * glowing nodes
	 */
	private static Image floor() {
		int size = 256;
		int[] pixels = new int[size * size];
		Random random = new Random(11);
		for (int i = 0; i < pixels.length; i++) {
			int grain = random.nextInt(7);
			pixels[i] = 0xff000000 | TextureMath.mix(0x0b0e1c, 0x131830, grain / 6.0);
		}
		// Seams: dark groove with a thin light edge inside
		for (int i = 0; i < size; i++) {
			for (int k = 0; k < 3; k++) {
				pixels[i * size + k] = 0xff04050b;
				pixels[k * size + i] = 0xff04050b;
				pixels[i * size + size - 1 - k] = 0xff04050b;
				pixels[(size - 1 - k) * size + i] = 0xff04050b;
			}
			pixels[i * size + 3] = 0xff17304a;
			pixels[3 * size + i] = 0xff17304a;
		}
		// Traces: each turns a few times, ends in a node
		for (int trace = 0; trace < 9; trace++) {
			int x = 24 + random.nextInt(size - 48);
			int y = 24 + random.nextInt(size - 48);
			boolean horizontal = random.nextBoolean();
			for (int segment = 0; segment < 3; segment++) {
				int length = 20 + random.nextInt(60);
				int step = random.nextBoolean() ? 1 : -1;
				for (int i = 0; i < length; i++) {
					x = Math.max(8, Math.min(size - 9, x + (horizontal ? step : 0)));
					y = Math.max(8, Math.min(size - 9, y + (horizontal ? 0 : step)));
					pixels[y * size + x] = 0xff1b4f70;
				}
				horizontal = !horizontal;
			}
			for (int dy = -2; dy <= 2; dy++) {
				for (int dx = -2; dx <= 2; dx++) {
					if (Math.abs(dx) + Math.abs(dy) <= 3) {
						pixels[(y + dy) * size + x + dx] = (dx == 0 && dy == 0) ? 0xff7fe4ff : 0xff2587b5;
					}
				}
			}
		}
		return image(size, size, pixels);
	}

	/**
	 * A tower side that repeats: rows of windows, some of them lit
	 */
	private static Image facade(boolean glowOnly) {
		int size = 256;
		int columns = 8;
		int rows = 12;
		int[] pixels = new int[size * size];
		// The same windows for both pictures
		Random random = new Random(23);
		int cellWidth = size / columns;
		int cellHeight = size / rows;
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				int wall = 0xff000000 | TextureMath.mix(0x0a0d1a, 0x141a30, (double) py / size);
				pixels[py * size + px] = glowOnly ? 0xff000000 : wall;
			}
		}
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				boolean lit = random.nextDouble() < 0.38;
				int color = WINDOW_COLORS[random.nextInt(WINDOW_COLORS.length)];
				double strength = 0.55 + random.nextDouble() * 0.45;
				for (int py = row * cellHeight + 5; py < (row + 1) * cellHeight - 6; py++) {
					for (int px = column * cellWidth + 6; px < (column + 1) * cellWidth - 6; px++) {
						if (lit) {
							pixels[py * size + px] = 0xff000000 | TextureMath.mix(0x000000, color, strength);
						} else if (!glowOnly) {
							pixels[py * size + px] = 0xff05070f;
						}
					}
				}
			}
		}
		return image(size, size, pixels);
	}

	/**
	 * Night sky: dark violet up high, a magenta glow at the horizon (the middle of
	 * the picture), dark below it, and stars
	 */
	private static Image sky() {
		int width = 1024;
		int height = 512;
		int[] pixels = new int[width * height];
		Random random = new Random(5);
		for (int py = 0; py < height; py++) {
			double t = (double) py / height;
			int color;
			if (t < 0.5) {
				double up = t / 0.5;
				color = TextureMath.mix(0x04050e, 0x1a0b3d, Math.pow(up, 1.4));
				color = TextureMath.mix(color, 0xff2a9d, Math.pow(up, 9) * 0.85);
			} else {
				color = TextureMath.mix(0x4a0f46, 0x06070d, Math.min(1, (t - 0.5) / 0.05));
			}
			for (int px = 0; px < width; px++) {
				pixels[py * width + px] = 0xff000000 | color;
			}
		}
		for (int i = 0; i < 700; i++) {
			int px = random.nextInt(width);
			int py = (int) (Math.pow(random.nextDouble(), 0.8) * height * 0.42);
			// Stretched at the top of the sphere: wider stars there
			double brightness = 0.4 + random.nextDouble() * 0.6;
			pixels[py * width + px] = 0xff000000 | TextureMath.mix(0x1a0b3d, 0xe8f4ff, brightness);
		}
		return image(width, height, pixels);
	}

	/**
	 * A synthwave sun: a disc from yellow to magenta, cut by dark bands that get
	 * thicker towards the bottom, with a soft halo
	 */
	private static Image sun() {
		int size = 512;
		int[] pixels = new int[size * size];
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double x = (px + 0.5) / size * 2 - 1;
				double y = (py + 0.5) / size * 2 - 1;
				double radius = Math.hypot(x, y);
				double alpha = 0;
				int color = 0xff2a9d;
				if (radius < 0.62) {
					double v = (y + 0.62) / 1.24;
					color = TextureMath.mix(0xffe14d, 0xff2a9d, v * 1.1);
					alpha = Math.min(1, (0.62 - radius) * 90);
					// Bands in the lower half
					if (v > 0.45) {
						double band = (v - 0.45) * 16;
						double cut = (v - 0.45) * 1.9;
						if (band - Math.floor(band) < cut) {
							alpha = 0;
						}
					}
				} else if (radius < 1) {
					double halo = 1 - (radius - 0.62) / 0.38;
					alpha = halo * halo * 0.35;
				}
				pixels[py * size + px] = ((int) Math.round(alpha * 255) << 24) | color;
			}
		}
		return image(size, size, pixels);
	}

	}
