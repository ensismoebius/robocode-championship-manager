package widgets.scenes;

import javafx.scene.paint.Color;

/**
 * The colors of the fire, made once: every one is a step of the life of a particle and a level of its opacity
 */
final class FireColors {

	// The colors of the particles are made once: every one is a step of its life
	// and a level of opacity, and the particles take them from these tables
	// instead of making new colors in every frame
	static final int LIFE_STEPS = 48;

	static final int OPACITY_LEVELS = 16;

	static final int SMOKE_SHADES = 8;

	/** The darkest gray of the smoke, the shades go from 0 to this */
	static final double SMOKE_SHADE_MAX = 0.25;

	static final Color[] FLAME_COLORS = new Color[2 * LIFE_STEPS * OPACITY_LEVELS];

	static final Color[] SMOKE_COLORS = new Color[SMOKE_SHADES * LIFE_STEPS * OPACITY_LEVELS];

	static final Color[] EMBER_COLORS = new Color[LIFE_STEPS];

	static final Color[] SPARK_COLORS = new Color[LIFE_STEPS];

	static final Color[] FIREFLY_COLORS = new Color[OPACITY_LEVELS];

	static final Color[] SNOW_COLORS = new Color[OPACITY_LEVELS];
	static final Color[] SAND_COLORS = new Color[OPACITY_LEVELS];

	/** The index of the life step of a particle, from its life between zero and one. */
	static int step(double time) {
		return Math.max(0, Math.min(LIFE_STEPS - 1, (int) (time * LIFE_STEPS)));
	}

	/** The index of the opacity level closest to the given opacity. */
	static int level(double opacity) {
		return Math.max(0, Math.min(OPACITY_LEVELS - 1, (int) Math.round(opacity * (OPACITY_LEVELS - 1))));
	}

	/** The middle of a life step, as a fraction of the life. */
	static double middle(int step) {
		return (step + 0.5) / LIFE_STEPS;
	}

	/** The opacity of an opacity level. */
	static double opacity(int level) {
		return (double) level / (OPACITY_LEVELS - 1);
	}

	/** The colour with the given opacity. */
	static Color withOpacity(Color color, double opacity) {
		return Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
	}

	/**
	 * The colour of a flame, cached by its life step and opacity level. The colors
	 * of the fire are in its picture: this only fades it, and cools the orange ones
	 * toward red as they age. The blue ones keep their blue.
	 */
	static Color flameColor(boolean blue, int step, int level) {
		int index = ((blue ? 1 : 0) * LIFE_STEPS + step) * OPACITY_LEVELS + level;
		Color color = FLAME_COLORS[index];
		if (color == null) {
			double time = middle(step);
			Color base = blue ? Color.WHITE : Color.WHITE.interpolate(Color.web("#ff9a74"), Math.max(0, (time - 0.3) / 0.7));
			// See through: many flames pile up on a burning robot, and it must not become a solid mass
			color = FLAME_COLORS[index] = withOpacity(base, opacity(level) * FLAME_OPACITY);
		}
		return color;
	}

	/** The most opaque a flame gets */
	static final double FLAME_OPACITY = 0.55;

	/** The colour of a smoke particle, cached by its shade, life step and opacity level. */
	static Color smokeColor(int shade, int step, int level) {
		int index = (shade * LIFE_STEPS + step) * OPACITY_LEVELS + level;
		Color color = SMOKE_COLORS[index];
		if (color == null) {
			double time = middle(step);
			double gray = (shade + 0.5) / SMOKE_SHADES * SMOKE_SHADE_MAX;
			Color base = time < 0.12 ? Color.web("#ff7a2a").interpolate(Color.gray(gray), time / 0.12) : Color.gray(gray).interpolate(Color.gray(Math.min(0.4, gray + 0.12)), (time - 0.12) / 0.88);
			color = SMOKE_COLORS[index] = withOpacity(base, opacity(level));
		}
		return color;
	}

	/** The colour of an ember, cached by its life step. */
	static Color emberColor(int step) {
		Color color = EMBER_COLORS[step];
		if (color == null) {
			double time = middle(step);
			double alpha = (time < 0.1 ? time * 10 : 1 - (time - 0.1) / 0.9) * 0.95;
			color = EMBER_COLORS[step] = Color.color(1, 0.62 - 0.4 * time, 0.15 - 0.1 * time, Math.max(0, alpha));
		}
		return color;
	}

	/** The colour of a spark, cached by its life step. */
	static Color sparkColor(int step) {
		Color color = SPARK_COLORS[step];
		if (color == null) {
			double time = middle(step);
			color = SPARK_COLORS[step] = Color.color(1, 0.85 - 0.4 * time, 0.4 - 0.3 * time, 1 - time * time);
		}
		return color;
	}

	/** The colour of a firefly for the given opacity level. */
	static Color fireflyColor(int level) {
		return cachedColor(FIREFLY_COLORS, level, 0.78, 1, 0.35);
	}

	/** The colour of a snowflake for the given opacity level. */
	static Color snowColor(int level) {
		return cachedColor(SNOW_COLORS, level, 0.94, 0.97, 1);
	}

	/** The colour of a grain of sand blown by the wind for the given opacity level. */
	static Color sandColor(int level) {
		return cachedColor(SAND_COLORS, level, 0.92, 0.78, 0.52);
	}

	/** The color of a level of opacity, made the first time it is needed */
	static Color cachedColor(Color[] cache, int level, double red, double green, double blue) {
		Color color = cache[level];
		if (color == null) {
			color = cache[level] = Color.color(red, green, blue, opacity(level));
		}
		return color;
	}
}
