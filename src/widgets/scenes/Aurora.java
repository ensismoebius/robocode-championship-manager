package widgets.scenes;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The aurora over the ice arena: curtains of light low in the sky that wave and
 * drift. It paints on the picture of the sky dome, over the color of the sky,
 * only on the rows where the curtains are, a few times a second.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class Aurora {

	private static final int WIDTH = 384;
	private static final int HEIGHT = 192;
	private static final int CURTAINS = 3;
	/** The rows of the picture where the curtains are, as fractions of its height (the horizon is at 0.5) */
	private static final double TOP = 0.06;
	private static final double BOTTOM = 0.44;

	private final WritableImage image = new WritableImage(WIDTH, HEIGHT);
	private final int sky;
	private final int firstRow = (int) (TOP * HEIGHT);
	private final int rows = (int) ((BOTTOM - TOP) * HEIGHT);
	private final int[] pixels = new int[WIDTH * this.rows];
	private final double[][] edge = new double[CURTAINS][WIDTH];
	private final double[][] ray = new double[CURTAINS][WIDTH];
	private final double[][] patch = new double[CURTAINS][WIDTH];

	/**
	 * @param sky
	 *            the color of the sky, which the aurora shines over
	 */
	Aurora(Color sky) {
		this.sky = ((int) Math.round(sky.getRed() * 255) << 16) | ((int) Math.round(sky.getGreen() * 255) << 8) | (int) Math.round(sky.getBlue() * 255);
		int[] all = new int[WIDTH * HEIGHT];
		java.util.Arrays.fill(all, 0xff000000 | this.sky);
		this.image.getPixelWriter().setPixels(0, 0, WIDTH, HEIGHT, PixelFormat.getIntArgbInstance(), all, 0, WIDTH);
		this.update(0);
	}

	/** The picture of the sky, which changes as the aurora is updated */
	Image image() {
		return this.image;
	}

	/**
	 * Paints the aurora as it is at the time (in seconds). Every wave goes a whole
	 * number of times around the sky, so the picture has no seam where it closes.
	 */
	void update(double time) {
		this.updateCurtains(time);
		this.paintRows();
	}

	/** The edge, the rays and the shine of each column of the curtains, at the time */
	private void updateCurtains(double time) {
		for (int column = 0; column < CURTAINS; column++) {
			for (int posX = 0; posX < WIDTH; posX++) {
				double around = (double) posX / WIDTH;
				// Where the lower edge of the curtain is: it undulates and the waves travel
				this.edge[column][posX] = 0.30 + 0.04 * column + 0.028 * Math.sin(2 * Math.PI * (around * (2 + column) + 0.31 * column) + 0.35 * (1 + 0.3 * column) * time)
						+ 0.016 * Math.sin(2 * Math.PI * (around * (5 + 2 * column) + 0.7 * column) - 0.8 * (1 + 0.2 * column) * time);
				// The rays: vertical strips of light that flicker and slide along the curtain
				double strips = 0.5 + 0.28 * Math.sin(2 * Math.PI * (around * 13) + 1.1 * time + column) + 0.22 * Math.sin(2 * Math.PI * (around * 29 + 0.2 * column) - 1.7 * time)
						+ 0.12 * Math.sin(2 * Math.PI * (around * 47) + 2.3 * time + 2 * column);
				this.ray[column][posX] = 0.3 + 0.7 * Math.pow(Math.max(0, Math.min(1, strips)), 1.3);
				// Where the curtain shines at all: it fades in and out along its arc
				double glow = 0.5 + 0.5 * Math.sin(2 * Math.PI * (around * (1 + column)) + 0.22 * time + 1.7 * column);
				double breath = 0.85 + 0.15 * Math.sin(0.6 * time + column);
				this.patch[column][posX] = Math.max(0, Math.min(1, glow * 1.6 - 0.15)) * breath;
			}
		}
	}

	/** Paints the rows of the picture: each pixel gets the light of the curtains that cross it */
	private void paintRows() {
		int baseRed = (this.sky >> 16) & 0xff;
		int baseGreen = (this.sky >> 8) & 0xff;
		int baseBlue = this.sky & 0xff;
		for (int row = 0; row < this.rows; row++) {
			double time2 = (double) (this.firstRow + row) / HEIGHT;
			for (int posX2 = 0; posX2 < WIDTH; posX2++) {
				double red = baseRed;
				double green = baseGreen;
				double blue = baseBlue;
				for (int column2 = 0; column2 < CURTAINS; column2++) {
					// Below the edge it ends sharply, above it the light rises and fades
					double delta = time2 - this.edge[column2][posX2];
					if (delta > 0.05 || delta < -0.4) {
						continue;
					}
					double rise = Math.max(0, Math.min(1, -delta / 0.13));
					double strength = delta > 0 ? Math.exp(-Math.pow(delta / 0.012, 2)) : Math.exp(-Math.pow(-delta / 0.075, 2));
					strength *= this.ray[column2][posX2] * this.patch[column2][posX2];
					if (strength < 0.004) {
						continue;
					}
					// Green at the foot, then teal, and violet at the top of the curtain
					int light = TextureMath.mix(0x4dffa8, 0x1fb5d6, Math.min(1, rise * 1.4));
					light = TextureMath.mix(light, 0x8a4dff, rise * rise * 0.85);
					if (delta > 0) {
						light = TextureMath.mix(light, 0xff5a9a, 0.4);
					}
					red += ((light >> 16) & 0xff) * strength * 0.95;
					green += ((light >> 8) & 0xff) * strength * 0.95;
					blue += (light & 0xff) * strength * 0.95;
				}
				this.pixels[row * WIDTH + posX2] = 0xff000000 | ((int) Math.min(255, red) << 16) | ((int) Math.min(255, green) << 8) | (int) Math.min(255, blue);
			}
		}
		this.image.getPixelWriter().setPixels(0, this.firstRow, WIDTH, this.rows, PixelFormat.getIntArgbInstance(), this.pixels, 0, WIDTH);
	}
}
