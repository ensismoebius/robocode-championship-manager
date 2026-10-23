package widgets;

import java.util.Random;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
/**
 * The adaptive songs of the battle, and how each one is played
 */
public enum Song {
	/** A minor, i - VI - III - VII: pad, kick, snare, arpeggio */
	EPIC("Épica", new double[] { 110.0, 87.31, 130.81, 98.0 }, new boolean[] { true, false, false, false }, 96, 54),
	/** E minor, i - VI - VII - v: pumping pad, bouncing bass, lead and toms */
	SYNTHWAVE("Synthwave", new double[] { 82.41, 65.41, 73.42, 61.74 }, new boolean[] { true, false, false, true }, 108, 44),
	/** D minor groove: a wood block clave, syncopated taikos, small drums, a plucked bass, a kalimba that calls and answers, war horns */
	WAR_DRUMS("Tambores de guerra", new double[] { 73.42, 73.42, 58.27, 65.41 }, new boolean[] { true, true, false, false }, 104, 44),
	/** E minor, i - III - VII - iv: Africa and science fiction, an agogo bell and congas in a 3+3+3+3+4 pulse, a shekere, an mbira that calls and answers, a sequencer, a theremin and lasers */
	AFRO_FUTURE("Afrofuturista", new double[] { 82.41, 98.0, 73.42, 110.0 }, new boolean[] { true, false, false, true }, 112, 48);

	public final String label;
	final double[] roots;
	final boolean[] minor;
	final double calmBpm;
	final double tenseBpm;

	/** A song of the battle: its label, the roots and modes of its chords, and its tempos when calm and when tense. */
	Song(String label, double[] roots, boolean[] minor, double calmBpm, double moreBpm) {
		this.label = label;
		this.roots = roots;
		this.minor = minor;
		this.calmBpm = calmBpm;
		this.tenseBpm = moreBpm;
	}

	/** Random songs do not repeat until all were played */
	private static final Rotation<Song> ROTATION = new Rotation<Song>(values());

	/**
	 * The configured song, the next of the rotation if not a song (like
	 * "RANDOM")
	 */
	public static Song resolve(String configured, Random random) {
		try {
			return Song.valueOf(configured);
		} catch (IllegalArgumentException | NullPointerException nullPointerException) {
			return ROTATION.next(random);
		}
	}
}
