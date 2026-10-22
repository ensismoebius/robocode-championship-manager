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

	/** The textures are static: this class is not instantiated. */
	private NeonTexture() {
	}

	/** Makes an image of the given size from the pixels. */
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
		for (int index = 0; index < pixels.length; index++) {
			int grain = random.nextInt(7);
			pixels[index] = 0xff000000 | TextureMath.mix(0x0b0e1c, 0x131830, grain / 6.0);
		}
		seams(pixels, size);
		traces(pixels, size, random);
		return image(size, size, pixels);
	}

	/** The seams: a dark groove along each edge of the panel, with a thin light edge inside */
	private static void seams(int[] pixels, int size) {
		for (int index2 = 0; index2 < size; index2++) {
			for (int deepIndex = 0; deepIndex < 3; deepIndex++) {
				pixels[index2 * size + deepIndex] = 0xff04050b;
				pixels[deepIndex * size + index2] = 0xff04050b;
				pixels[index2 * size + size - 1 - deepIndex] = 0xff04050b;
				pixels[(size - 1 - deepIndex) * size + index2] = 0xff04050b;
			}
			pixels[index2 * size + 3] = 0xff17304a;
			pixels[3 * size + index2] = 0xff17304a;
		}
	}

	/** The traces of circuit: each turns a few times, and ends in a glowing node */
	private static void traces(int[] pixels, int size, Random random) {
		for (int trace = 0; trace < 9; trace++) {
			int posX = 24 + random.nextInt(size - 48);
			int posY = 24 + random.nextInt(size - 48);
			boolean horizontal = random.nextBoolean();
			for (int segment = 0; segment < 3; segment++) {
				int length = 20 + random.nextInt(60);
				int step = random.nextBoolean() ? 1 : -1;
				for (int index3 = 0; index3 < length; index3++) {
					posX = Math.max(8, Math.min(size - 9, posX + (horizontal ? step : 0)));
					posY = Math.max(8, Math.min(size - 9, posY + (horizontal ? 0 : step)));
					pixels[posY * size + posX] = 0xff1b4f70;
				}
				horizontal = !horizontal;
			}
			for (int deltaY = -2; deltaY <= 2; deltaY++) {
				for (int deltaX = -2; deltaX <= 2; deltaX++) {
					if (Math.abs(deltaX) + Math.abs(deltaY) <= 3) {
						pixels[(posY + deltaY) * size + posX + deltaX] = (deltaX == 0 && deltaY == 0) ? 0xff7fe4ff : 0xff2587b5;
					}
				}
			}
		}
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
		for (int posY = 0; posY < size; posY++) {
			for (int posX = 0; posX < size; posX++) {
				int wall = 0xff000000 | TextureMath.mix(0x0a0d1a, 0x141a30, (double) posY / size);
				pixels[posY * size + posX] = glowOnly ? 0xff000000 : wall;
			}
		}
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				boolean lit = random.nextDouble() < 0.38;
				int color = WINDOW_COLORS[random.nextInt(WINDOW_COLORS.length)];
				double strength = 0.55 + random.nextDouble() * 0.45;
				for (int posY2 = row * cellHeight + 5; posY2 < (row + 1) * cellHeight - 6; posY2++) {
					for (int posX2 = column * cellWidth + 6; posX2 < (column + 1) * cellWidth - 6; posX2++) {
						if (lit) {
							pixels[posY2 * size + posX2] = 0xff000000 | TextureMath.mix(0x000000, color, strength);
						} else if (!glowOnly) {
							pixels[posY2 * size + posX2] = 0xff05070f;
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
		for (int posY = 0; posY < height; posY++) {
			double time = (double) posY / height;
			int color;
			if (time < 0.5) {
				double upValue = time / 0.5;
				color = TextureMath.mix(0x04050e, 0x1a0b3d, Math.pow(upValue, 1.4));
				color = TextureMath.mix(color, 0xff2a9d, Math.pow(upValue, 9) * 0.85);
			} else {
				color = TextureMath.mix(0x4a0f46, 0x06070d, Math.min(1, (time - 0.5) / 0.05));
			}
			for (int posX = 0; posX < width; posX++) {
				pixels[posY * width + posX] = 0xff000000 | color;
			}
		}
		for (int index = 0; index < 700; index++) {
			int posX2 = random.nextInt(width);
			int posY2 = (int) (Math.pow(random.nextDouble(), 0.8) * height * 0.42);
			// Stretched at the top of the sphere: wider stars there
			double brightness = 0.4 + random.nextDouble() * 0.6;
			pixels[posY2 * width + posX2] = 0xff000000 | TextureMath.mix(0x1a0b3d, 0xe8f4ff, brightness);
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
		for (int posY = 0; posY < size; posY++) {
			for (int posX = 0; posX < size; posX++) {
				double posX2 = (posX + 0.5) / size * 2 - 1;
				double posY2 = (posY + 0.5) / size * 2 - 1;
				double radius = Math.hypot(posX2, posY2);
				double alpha = 0;
				int color = 0xff2a9d;
				if (radius < 0.62) {
					double vCoord = (posY2 + 0.62) / 1.24;
					color = TextureMath.mix(0xffe14d, 0xff2a9d, vCoord * 1.1);
					alpha = Math.min(1, (0.62 - radius) * 90);
					// Bands in the lower half
					if (vCoord > 0.45) {
						double band = (vCoord - 0.45) * 16;
						double cut = (vCoord - 0.45) * 1.9;
						if (band - Math.floor(band) < cut) {
							alpha = 0;
						}
					}
				} else if (radius < 1) {
					double halo = 1 - (radius - 0.62) / 0.38;
					alpha = halo * halo * 0.35;
				}
				pixels[posY * size + posX] = ((int) Math.round(alpha * 255) << 24) | color;
			}
		}
		return image(size, size, pixels);
	}

	}
