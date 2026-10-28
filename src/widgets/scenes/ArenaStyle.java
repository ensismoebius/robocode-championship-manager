package widgets.scenes;

import java.util.Arrays;
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
	ICE("Tundra Glacial", Color.web("#0b1728"), Color.color(0.46, 0.5, 0.58), Color.color(0.85, 0.92, 1.0), Color.color(0.80, 0.95, 1.0), Song.TUNDRA),
	/** Mossy ground, rivers, huge trees, ferns, waterfalls, fireflies and mist */
	JUNGLE("Floresta Tropical", Color.web("#08261f"), Color.color(0.40, 0.50, 0.38), Color.color(1.0, 0.93, 0.70), Color.color(0.45, 1.0, 0.55), Song.FOREST),
	/** The Eden Solar (Africa, 2187): a stadium open to a stormy sky, a round garden, the crowd with its flags, warm lights and two trees of life */
	EDEN_SOLAR("Éden Solar", Color.web("#1e2a33"), Color.color(0.34, 0.36, 0.40), Color.color(0.74, 0.74, 0.80), Color.color(1.0, 0.72, 0.32), Song.AFRO_FUTURE),
	/** The Middle East at sunset: a courtyard of mosaic tiles with eight-pointed stars, a plain of sand with palms, a town of mosques, minarets, domes and gates on the dunes, and lanterns at the corners */
	MIDDLE_EAST("Areias do Oriente Médio", Color.web("#2b1d33"), Color.color(0.44, 0.36, 0.34), Color.color(1.0, 0.84, 0.62), Color.color(0.35, 0.90, 0.84), Song.HIJAZ);

	public final String label;
	/** The sky: a solid color, a gradient leaves trails in the 3D */
	final Color sky;
	final Color ambient;
	final Color key;
	/** The force field when hit */
	final Color fence;
	/** The song that goes with the arena */
	public final Song song;

	/** An arena style: its name, its colours and its music. */
	ArenaStyle(String label, Color sky, Color ambient, Color key, Color fence, Song song) {
		this.label = label;
		this.sky = sky;
		this.ambient = ambient;
		this.key = key;
		this.fence = fence;
		this.song = song;
	}

	/** The arena of the finals: the random arenas of the other matches never pick it, and a random final always does */
	public static final ArenaStyle FINALS = EDEN_SOLAR;

	/** Random arenas do not repeat until all were used, except the arena of the finals, which is kept for the finals */
	private static final Rotation<ArenaStyle> ROTATION = new Rotation<ArenaStyle>(Arrays.stream(values()).filter(style -> style != FINALS).toArray(ArenaStyle[]::new));

	/**
	 * The configured arena. When it is not an arena (like "RANDOM"), the arena of the finals for a final, and for any other
	 * match the next of the rotation, which has all the arenas but the one of the finals.
	 */
	public static ArenaStyle resolve(String configured, Random random, boolean finalMatch) {
		try {
			return ArenaStyle.valueOf(configured);
		} catch (IllegalArgumentException | NullPointerException nullPointerException) {
			return finalMatch ? FINALS : ROTATION.next(random);
		}
	}
}
