package widgets;

import static widgets.BattleAudio.isOn;

import java.util.List;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The song of the tropical forest arena, a Latin American one: G major, the seven notes of do re mi fa sol la si in equal
 * temperament, and the chords that stand on it, I - V - vi - IV, over the rhythm that runs through the music of the continent, the
 * tresillo, three beats, three beats and two (3 + 3 + 2 eighths). A marimba, the instrument of Central America and of the
 * Pacific coast of Colombia and Ecuador, sings a melody that follows the chord of each bar, a bass plays the tresillo between the
 * root and the fifth, a guitar strums the chord on it, and the percussion keeps the time: a bass drum, congas, maracas, a guiro and
 * the claves, which play the pattern of the son clave, three hits in a bar and two in the next. As the fight heats up the melody
 * doubles its notes and the percussion fills the bar.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class BattleAudioForest {

	/** The major scale, as semitones over its tonic */
	private static final int[] MAJOR = { 0, 2, 4, 5, 7, 9, 11 };
	/** The tonic of the melody, G4: the melody stays in G, and the chords move under it */
	private static final double TONIC = 392;
	/** The melody of each bar, one degree of the scale for each eighth (7 is the octave): the notes of the chord of the bar, and the notes between them */
	private static final int[][] MELODY = { { 0, 2, 4, 2, 4, 5, 4, 2 }, { 4, 6, 8, 6, 4, 6, 4, 2 }, { 5, 7, 9, 7, 5, 7, 9, 7 }, { 3, 5, 7, 5, 4, 3, 2, 0 } };
	/** The tresillo, on the sixteenths of the bar: 3 + 3 + 2 eighths. The bass plays the root on it, and the marimba and the guitar fall on it */
	private static final int[] TRESILLO = { 0, 6, 12 };
	/** The fifth of the bass, in between the roots */
	private static final int[] FIFTHS = { 10, 14 };
	/** Where the guitar strums, and the congas answer */
	private static final int[] STRUM = { 6, 12, 14 };
	private static final int[] CONGAS = { 7, 10, 15 };
	/** The two sides of the son clave: three hits in a bar, and two in the next */
	private static final int[] CLAVE_THREE = { 0, 6, 12 };
	private static final int[] CLAVE_TWO = { 4, 8 };
	private static final double[] MAJOR_CHORD = { 1, 1.26, 1.498 };
	private static final double[] MINOR_CHORD = { 1, 1.189, 1.498 };

	/** One drum of the kit: its sound, the steps of the bar where it plays, the tension from which it plays and its gain */
	private record Stroke(double[] sound, int[] steps, double from, double gain) {
	}

	private final BattleAudio host;
	private final double[][] marimba = new double[12][];
	private final List<Stroke> kit;
	/** The guiro: a stick scraped along a notched gourd, three short rasps */
	private final double[] guiro;

	/** The score of the song, playing in the mixer of the given battle audio. */
	BattleAudioForest(BattleAudio host) {
		this.host = host;
		for (int degree = 0; degree < this.marimba.length; degree++) {
			double frequency = TONIC * Math.pow(2, (MAJOR[degree % 7] + 12 * (degree / 7)) / 12.0);
			this.marimba[degree] = Synth.mix(Synth.sine(frequency, 0.5, 7), Synth.sine(frequency * 4, 0.16, 26), 0.28);
		}
		double[] rasp = Synth.lowpass(Synth.noise(0.03, 9), 0.6);
		this.guiro = Synth.concat(rasp, Synth.silence(0.012), rasp, Synth.silence(0.012), Synth.noise(0.07, 7));
		this.kit = List.of(new Stroke(host.kick, new int[] { 0, 8 }, 0.12, 0.7), new Stroke(host.kick, new int[] { 14 }, 0.5, 0.4), new Stroke(host.snare, new int[] { 4 }, 0.45, 0.28),
				new Stroke(host.hat, new int[] { 0, 2, 4, 6, 8, 10, 12, 14 }, 0.3, 0.14), new Stroke(host.smallDrum, CONGAS, 0.35, 0.26), new Stroke(this.guiro, new int[] { 3, 11 }, 0.55, 0.12));
	}

	/** Plays what the sixteenth of the song has: the kit, the bass, the guitar and the marimba. */
	void step(int inBar, int bar, double root, boolean minor, double tension) {
		for (Stroke stroke : this.kit) {
			if (tension > stroke.from() && isOn(stroke.steps(), inBar)) {
				host.musicVoices.add(new Voice(stroke.sound(), stroke.gain(), 1));
			}
		}
		if (tension > 0.2 && isOn(bar % 2 == 0 ? CLAVE_THREE : CLAVE_TWO, inBar)) {
			host.musicVoices.add(new Voice(host.woodBlock, 0.2, 1));
		}
		this.bass(inBar, root, tension);
		this.strum(inBar, root, minor, tension);
		this.melody(inBar, bar, tension);
	}

	/** The bass: the root of the chord on the tresillo, and the fifth in between once the fight is warm. */
	private void bass(int inBar, double root, double tension) {
		boolean onBeat = isOn(TRESILLO, inBar);
		if (onBeat || tension > 0.25 && isOn(FIFTHS, inBar)) {
			double note = onBeat ? root : root * 1.498;
			host.musicVoices.add(new Voice(Synth.lowpass(Synth.saw(note, 0.3, 7), 0.22), 0.3 + 0.2 * tension, 1));
		}
	}

	/** The guitar: the chord strummed on the tresillo, one string after the other, from the middle of the fight on. */
	private void strum(int inBar, double root, boolean minor, double tension) {
		if (tension > 0.3 && isOn(STRUM, inBar)) {
			double[] chord = minor ? MINOR_CHORD : MAJOR_CHORD;
			for (int string = 0; string < chord.length; string++) {
				double frequency = root * 4 * chord[string];
				double[] pluck = Synth.mix(Synth.sine(frequency, 0.4, 8), Synth.saw(frequency, 0.25, 16), 0.25);
				host.sided(Synth.concat(Synth.silence(0.014 * string), pluck), 0.07 + 0.04 * tension, 1, inBar == 6);
			}
		}
	}

	/** The marimba: the melody of the bar, a note on each hit of the tresillo when calm and on each eighth when the fight is hot, from one side to the other. */
	private void melody(int inBar, int bar, double tension) {
		if (tension > 0.45 ? inBar % 2 == 0 : isOn(TRESILLO, inBar)) {
			host.sided(this.marimba[MELODY[bar][inBar / 2]], 0.22 + 0.1 * tension, 1, inBar % 4 == 0);
		}
	}
}
