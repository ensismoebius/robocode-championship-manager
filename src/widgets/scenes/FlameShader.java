package widgets.scenes;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The picture of a flame the way a fire shader draws it, made on the processor
 * since the 3D scene has no shaders of its own: a noise of many scales that scrolls
 * up with the time bends the shape of the flame and eats its edges, a mask in the
 * shape of a drop keeps it round at the base and thin at the top, and the density
 * that is left goes through a ramp of colors, from the red of the borders to the
 * pale yellow of the core. A different time gives the next frame of the flame.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class FlameShader {

	/** The size of a frame */
	static final int WIDTH = 64;
	static final int HEIGHT = 96;

	/** The stops of the color of a flame, from its edge to its core: dark red, red, orange, yellow, pale yellow */
	private static final Color[] HEAT = { Color.web("#8f1604"), Color.web("#e03408"), Color.web("#ff8a1c"), Color.web("#ffc92e"), Color.web("#ffe98a") };

	/** The same for the flames that burn blue all the way: deep blue, blue, light blue, pale blue, white */
	private static final Color[] BLUE_HEAT = { Color.web("#0a1a8a"), Color.web("#1f3fe0"), Color.web("#3f8cff"), Color.web("#6fbcff"), Color.web("#b8e4ff") };

	/** Every flame is blue where it is born, as in a real fire */
	private static final Color BASE_BLUE = Color.web("#5aa9ff");

	/** Only draws flames, it has no instances. */
	private FlameShader() {
	}

	/** Eases from 0 at the first edge to 1 at the second. */
	private static double smooth(double edge0, double edge1, double value) {
		double fraction = Math.max(0, Math.min(1, (value - edge0) / (edge1 - edge0)));
		return fraction * fraction * (3 - 2 * fraction);
	}

	/** A pseudo random value from 0 to 1 for a point of the grid of the noise. */
	private static double hash(int cellX, int cellY, long seed) {
		long bits = cellX * 374761393L + cellY * 668265263L + seed * 2246822519L;
		bits = (bits ^ (bits >>> 13)) * 1274126177L;
		bits ^= bits >>> 16;
		return (bits & 0xffffff) / (double) 0x1000000;
	}

	/** A smooth noise from 0 to 1: the values of the grid, eased between them. */
	private static double noise(double posX, double posY, long seed) {
		int cellX = (int) Math.floor(posX);
		int cellY = (int) Math.floor(posY);
		double fractionX = smooth(0, 1, posX - cellX);
		double fractionY = smooth(0, 1, posY - cellY);
		double top = hash(cellX, cellY, seed) * (1 - fractionX) + hash(cellX + 1, cellY, seed) * fractionX;
		double bottom = hash(cellX, cellY + 1, seed) * (1 - fractionX) + hash(cellX + 1, cellY + 1, seed) * fractionX;
		return top * (1 - fractionY) + bottom * fractionY;
	}

	/** The noise of many scales, each one half the size and half the strength of the one before: from 0 to 1. */
	private static double fractal(double posX, double posY, long seed) {
		double sum = 0;
		double strength = 0.5;
		double scale = 1;
		for (int octave = 0; octave < 4; octave++) {
			sum += strength * noise(posX * scale, posY * scale, seed + octave);
			strength /= 2;
			scale *= 2;
		}
		return sum / 0.9375;
	}

	/** The color of a flame at a heat from 0 (its edge) to 1 (its core), in the orange or in the blue palette. */
	static Color heatColor(double heat, boolean blue) {
		Color[] stops = blue ? BLUE_HEAT : HEAT;
		double scaled = Math.max(0, Math.min(1, heat)) * (stops.length - 1);
		int index = Math.min(stops.length - 2, (int) scaled);
		return stops[index].interpolate(stops[index + 1], scaled - index);
	}

	/**
	 * The density of the flame at a point and how hot it is, from 0 to 1.
	 *
	 * @param across
	 *            from -1 (left) to 1 (right)
	 * @param rise
	 *            0 at the bottom, 1 at the top
	 * @param time
	 *            makes the noise scroll up: the next frame is a little later
	 * @return the density, and the heat
	 */
	static double[] densityAt(double across, double rise, double time, long seed) {
		// The air pushes the flame to the sides, more the higher it goes
		double sway = (fractal(across * 1.3 + 3.7, rise * 1.8 - time * 2.2, seed) - 0.5) * 0.9 * rise;
		double bent = across + sway;
		// A drop: round at the base, which closes in a point at the very bottom, thin at the top
		double width = 0.8 * Math.pow(1 - rise, 0.9) * (0.1 + 0.9 * smooth(0, 0.3, rise));
		double body = 1 - Math.abs(bent) / Math.max(width, 1e-3);
		// The turbulence that tears the flame, stronger toward the top
		double torn = fractal(across * 2.6, rise * 3.2 - time * 3.0, seed + 11) - 0.5;
		double density = body * 1.15 - rise * 0.3 + torn * 0.9 * (0.35 + 0.65 * rise);
		double heat = Math.max(0, Math.min(0.9, 0.2 + (density - 0.3) * 0.75 + 0.08 * (1 - rise)));
		return new double[] { density, heat };
	}

	/**
	 * One frame of a flame.
	 *
	 * @param seed
	 *            makes each flame different
	 * @param time
	 *            the moment of the frame, in the units of the scroll of the noise
	 * @param blue
	 *            the flame burns blue all the way, not only at its base
	 */
	static Image frame(long seed, double time, boolean blue) {
		WritableImage image = new WritableImage(WIDTH, HEIGHT);
		for (int posY = 0; posY < HEIGHT; posY++) {
			for (int posX = 0; posX < WIDTH; posX++) {
				double rise = 1 - (posY + 0.5) / HEIGHT;
				double[] flame = densityAt((posX + 0.5) / WIDTH * 2 - 1, rise, time, seed);
				// Crisp edges, a little soft; the base fades into the air
				double alpha = smooth(0.1, 0.4, flame[0]) * smooth(0, 0.02, rise);
				Color color = heatColor(flame[1], blue);
				if (!blue) {
					// Blue where it is born
					color = color.interpolate(BASE_BLUE, 0.85 * (1 - smooth(0, 0.14, rise)));
				}
				image.getPixelWriter().setColor(posX, posY, Color.color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(1, alpha * (0.7 + 0.3 * flame[1]))));
			}
		}
		return image;
	}
}
