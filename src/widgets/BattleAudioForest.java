package widgets;

import static widgets.BattleAudio.isOn;

import java.util.List;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The song of the tropical forest arena, in the Western scale: G major, the seven notes of do re mi fa sol la si in
 * equal temperament, and the chords that stand on it, I - V - vi - IV. A marimba sings a melody that follows the chord of
 * each bar, a bass walks between the root and the fifth, a guitar strums the chord on the backbeat and the kit of a band
 * keeps the time. As the fight heats up the melody doubles its notes and the kit fills the bar.
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
	/** The steps of the bass: the root on the beats, and the fifth in between them */
	private static final int[] ROOTS = { 0, 8 };
	private static final int[] FIFTHS = { 6, 14 };
	/** The steps of the backbeat, where the guitar strums and the snare answers */
	private static final int[] BACKBEAT = { 4, 12 };
	private static final double[] MAJOR_CHORD = { 1, 1.26, 1.498 };
	private static final double[] MINOR_CHORD = { 1, 1.189, 1.498 };

	/** One drum of the kit: its sound, the steps of the bar where it plays, the tension from which it plays and its gain */
	private record Stroke(double[] sound, int[] steps, double from, double gain) {
	}

	private final BattleAudio host;
	private final double[][] marimba = new double[12][];
	private final List<Stroke> kit;

	/** The score of the song, playing in the mixer of the given battle audio. */
	BattleAudioForest(BattleAudio host) {
		this.host = host;
		for (int degree = 0; degree < this.marimba.length; degree++) {
			double frequency = TONIC * Math.pow(2, (MAJOR[degree % 7] + 12 * (degree / 7)) / 12.0);
			this.marimba[degree] = Synth.mix(Synth.sine(frequency, 0.5, 7), Synth.sine(frequency * 4, 0.16, 26), 0.28);
		}
		this.kit = List.of(new Stroke(host.kick, ROOTS, 0.12, 0.7), new Stroke(host.kick, new int[] { 10 }, 0.5, 0.45), new Stroke(host.snare, BACKBEAT, 0.4, 0.4),
				new Stroke(host.hat, new int[] { 0, 2, 4, 6, 8, 10, 12, 14 }, 0.3, 0.16), new Stroke(host.smallDrum, new int[] { 7, 15 }, 0.55, 0.26));
	}

	/** Plays what the sixteenth of the song has: the kit, the bass, the guitar and the marimba. */
	void step(int inBar, int bar, double root, boolean minor, double tension) {
		for (Stroke stroke : this.kit) {
			if (tension > stroke.from() && isOn(stroke.steps(), inBar)) {
				host.musicVoices.add(new Voice(stroke.sound(), stroke.gain(), 1));
			}
		}
		this.bass(inBar, root, tension);
		this.strum(inBar, root, minor, tension);
		this.melody(inBar, bar, tension);
	}

	/** The bass: the root of the chord on the beats, and the fifth in between once the fight is warm. */
	private void bass(int inBar, double root, double tension) {
		boolean onBeat = isOn(ROOTS, inBar);
		if (onBeat || tension > 0.25 && isOn(FIFTHS, inBar)) {
			double note = onBeat ? root : root * 1.498;
			host.musicVoices.add(new Voice(Synth.lowpass(Synth.saw(note, 0.3, 7), 0.22), 0.3 + 0.2 * tension, 1));
		}
	}

	/** The guitar: the chord strummed on the backbeat, one string after the other, from the middle of the fight on. */
	private void strum(int inBar, double root, boolean minor, double tension) {
		if (tension > 0.3 && isOn(BACKBEAT, inBar)) {
			double[] chord = minor ? MINOR_CHORD : MAJOR_CHORD;
			for (int string = 0; string < chord.length; string++) {
				double frequency = root * 4 * chord[string];
				double[] pluck = Synth.mix(Synth.sine(frequency, 0.4, 8), Synth.saw(frequency, 0.25, 16), 0.25);
				host.sided(Synth.concat(Synth.silence(0.014 * string), pluck), 0.07 + 0.04 * tension, 1, inBar == 4);
			}
		}
	}

	/** The marimba: the melody of the bar, a note on each beat when calm and on each eighth when the fight is hot, from one side to the other. */
	private void melody(int inBar, int bar, double tension) {
		if (inBar % (tension > 0.45 ? 2 : 4) == 0) {
			host.sided(this.marimba[MELODY[bar][inBar / 2]], 0.22 + 0.1 * tension, 1, inBar % 4 == 0);
		}
	}
}
