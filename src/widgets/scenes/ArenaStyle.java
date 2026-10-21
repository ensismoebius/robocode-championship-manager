package widgets.scenes;

import java.util.Random;

import javafx.scene.paint.Color;
import widgets.Song;
import widgets.Rotation;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The looks of the 3D arena: the sky, the lights, the force field and what
 * floats in the air
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public enum ArenaStyle {

	/** Dark floor with a cyan grid */
	NEON("Grade Neon", Color.web("#0a0b18"), Color.gray(0.5), Color.gray(0.8), Color.color(0.20, 0.88, 1.0), Song.SYNTHWAVE),
	/** Basalt cracked by lava, a lava moat, volcanoes and embers */
	VOLCANO("Forja Vulcânica", Color.web("#140605"), Color.color(0.5, 0.36, 0.32), Color.color(1.0, 0.72, 0.52), Color.color(1.0, 0.45, 0.10), Song.WAR_DRUMS),
	/** Plates of ice, snow falling and ice crystals */
	ICE("Tundra Glacial", Color.web("#0b1728"), Color.color(0.46, 0.5, 0.58), Color.color(0.85, 0.92, 1.0), Color.color(0.80, 0.95, 1.0), Song.EPIC),
	/** Mossy ground, rivers, huge trees, ferns, waterfalls, fireflies and mist */
	JUNGLE("Floresta Tropical", Color.web("#08261f"), Color.color(0.40, 0.50, 0.38), Color.color(1.0, 0.93, 0.70), Color.color(0.45, 1.0, 0.55), Song.WAR_DRUMS);

	public final String label;
	/** The sky: a solid color, a gradient leaves trails in the 3D */
	final Color sky;
	final Color ambient;
	final Color key;
	/** The force field when hit */
	final Color fence;
	/** The song that goes with the arena */
	public final Song song;

	ArenaStyle(String label, Color sky, Color ambient, Color key, Color fence, Song song) {
		this.label = label;
		this.sky = sky;
		this.ambient = ambient;
		this.key = key;
		this.fence = fence;
		this.song = song;
	}

	/** Random arenas do not repeat until all were used */
	private static final Rotation<ArenaStyle> ROTATION = new Rotation<ArenaStyle>(values());

	/**
	 * The configured arena, the next of the rotation if not an arena (like
	 * "RANDOM")
	 */
	public static ArenaStyle resolve(String configured, Random random) {
		try {
			return ArenaStyle.valueOf(configured);
		} catch (IllegalArgumentException | NullPointerException e) {
			return ROTATION.next(random);
		}
	}
}
