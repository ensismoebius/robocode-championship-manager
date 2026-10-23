package widgets.scenes;

import java.util.function.IntBinaryOperator;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Helpers shared by the procedural textures: blending two colors, the value
 * noise lattice that repeats every period cells, and the picture made of a
 * function of its pixels.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class TextureMath {

	/** The math is static: this class is not instantiated. */
	private TextureMath() {
	}

	/**
	 * Blends two 0xRRGGBB colors, t from 0 (a) to 1 (b), clamped
	 */
	static int mix(int first, int second, double time) {
		time = Math.max(0, Math.min(1, time));
		int row = (int) Math.round(((first >> 16) & 0xff) * (1 - time) + ((second >> 16) & 0xff) * time);
		int graphics = (int) Math.round(((first >> 8) & 0xff) * (1 - time) + ((second >> 8) & 0xff) * time);
		int blValue = (int) Math.round((first & 0xff) * (1 - time) + (second & 0xff) * time);
		return row << 16 | graphics << 8 | blValue;
	}

	/**
	 * A pseudo random value from 0 to 1 at a lattice point, the same for the same
	 * point and seed, repeating every periodU by periodV cells
	 */
	static double lattice(int posX, int posY, int periodU, int periodV, int seed) {
		int height = Math.floorMod(posX, periodU) * 374761393 + Math.floorMod(posY, periodV) * 668265263 + seed * 1442695041;
		height = (height ^ (height >>> 13)) * 1274126177;
		return ((height ^ (height >>> 16)) & 0xffff) / 65535.0;
	}

	/** A picture of the given size, each pixel the 0xRRGGBB color the function gives for its column and row. */
	static Image image(int width, int height, IntBinaryOperator color) {
		int[] pixels = new int[width * height];
		for (int posY = 0; posY < height; posY++) {
			for (int posX = 0; posX < width; posX++) {
				pixels[posY * width + posX] = 0xff000000 | color.applyAsInt(posX, posY);
			}
		}
		WritableImage image = new WritableImage(width, height);
		image.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
		return image;
	}
}
