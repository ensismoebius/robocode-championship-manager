package widgets;

import static widgets.BattleAudio.isOn;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The song of the desert arena: the maqam Hijaz on D, played by an oud that plucks its phrases over the chords, a ney that
 * sings above them with its breath, and a darbuka that keeps the maqsum, the rhythm of the old dances, with a deep
 * stroke in the middle of the skin and a bright one on its rim. Under them, a bass on the root of each chord; as the
 * battle heats up, come the hats, the claps and a sweep that rises before the phrase turns over.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class BattleAudioHijaz {

	/** The maqam Hijaz on D, as frequencies of its tonic: the step of three semitones between its second and third is what gives it its sound */
	private static final double[] HIJAZ = { 1, 1.0595, 1.2599, 1.3348, 1.4983, 1.5874, 1.7818, 2 };
	/** The tonic of the melody, D4: the melody stays on the tonic, whatever the chords do */
	private static final double TONIC = 293.66;
	/** The two phrases of the oud, as degrees of the scale: one degree for each eighth of the bar */
	private static final int[] FIRST = { 0, 1, 2, 3, 4, 3, 2, 1 };
	private static final int[] SECOND = { 4, 3, 2, 1, 0, 1, 2, 0 };
	/** The notes that the ney holds, one for each two bars */
	private static final int[] NEY = { 4, 3, 7, 4 };
	/** The strokes of the darbuka in a bar of sixteenths: the deep ones and the bright ones */
	private static final int[] DUM = { 0, 6, 8, 14 };
	private static final int[] TEK = { 4, 10, 12 };

	private final BattleAudio host;
	private final double[] dum = Percussion.dum();
	private final double[] tek = Percussion.tek();
	private final double[][] oudNotes = new double[HIJAZ.length][];
	private final double[][] neyNotes = new double[HIJAZ.length][];

	/** The score of the song, playing in the mixer of the given battle audio. */
	BattleAudioHijaz(BattleAudio host) {
		this.host = host;
		for (int degree = 0; degree < HIJAZ.length; degree++) {
			this.oudNotes[degree] = pluck(TONIC * HIJAZ[degree]);
			this.neyNotes[degree] = neyTone(TONIC * HIJAZ[degree]);
		}
	}

	/** Plays what the sixteenth of the bar has: the darbuka, the bass, the oud, the ney, the hats and the sweeps. */
	void step(int step, int inBar, int bar, double root, double tension) {
		this.darbuka(inBar, tension);
		this.bass(inBar, root, tension);
		this.oud(inBar, bar, tension);
		this.ney(inBar, bar, tension);
		this.hatsAndClaps(inBar, tension);
		this.sweep(step, tension);
	}

	/** A voice that plays from one side more than the other. */
	private void play(double[] sound, double gain, boolean left) {
		host.sided(sound, gain, 1, left);
	}

	/** The darbuka: the deep strokes of the maqsum, and the bright ones once the fight is warm. */
	private void darbuka(int inBar, double tension) {
		if (isOn(DUM, inBar)) {
			host.musicVoices.add(new Voice(this.dum, 0.42 + 0.25 * tension, 1));
		}
		if (tension > 0.15 && isOn(TEK, inBar)) {
			this.play(this.tek, 0.2 + 0.2 * tension, inBar < 8);
		}
	}

	/** The bass: the root of the chord on the first beat of each bar, and on the third when the fight is hot. */
	private void bass(int inBar, double root, double tension) {
		if (inBar == 0 || tension > 0.4 && inBar == 8) {
			host.musicVoices.add(new Voice(Synth.sine(root, 0.7, 3), 0.5, 1));
		}
	}

	/** The oud: its phrase, a quarter note at a time when calm, an eighth note at a time when tense, with grace notes at the top. */
	private void oud(int inBar, int bar, double tension) {
		int[] phrase = bar % 4 < 2 ? FIRST : SECOND;
		boolean quarters = tension <= 0.45;
		if (quarters ? inBar % 4 == 0 : inBar % 2 == 0) {
			int degree = phrase[inBar / 2];
			this.play(this.oudNotes[degree], 0.2 + 0.1 * tension, degree % 2 == 0);
		}
		if (tension > 0.75 && inBar % 2 == 1) {
			int degree = Math.min(HIJAZ.length - 1, phrase[inBar / 2] + 1);
			this.play(this.oudNotes[degree], 0.1, inBar % 4 == 1);
		}
	}

	/** The ney: a long note at the start of every second bar, above the oud, from the middle of the battle on. */
	private void ney(int inBar, int bar, double tension) {
		if (tension > 0.35 && inBar == 0 && bar % 2 == 0) {
			this.play(this.neyNotes[NEY[(bar / 2) % NEY.length]], 0.22, false);
		}
	}

	/** The hats on every second sixteenth once the fight is warm, bouncing between the ears, and the claps on the backbeat when it is hot. */
	private void hatsAndClaps(int inBar, double tension) {
		if (tension > 0.3 && inBar % 2 == 0) {
			this.play(host.hat, 0.12 + 0.1 * tension, inBar % 4 == 0);
		}
		if (tension > 0.6 && (inBar == 4 || inBar == 12)) {
			this.play(host.snare, 0.2, inBar == 4);
		}
	}

	/** A sweep that rises before the phrase turns over, every four bars, when the fight is at its hottest. */
	private void sweep(int step, double tension) {
		if (tension > 0.8 && step % 64 == 56) {
			host.musicVoices.add(new Voice(Synth.lowpass(Synth.sweep(300, 2000, 0.9, false), 0.2), 0.09, 1));
		}
	}

	/** An oud string plucked: a tone that dies away, with its octave over it. */
	private static double[] pluck(double frequency) {
		return Synth.mix(Synth.sine(frequency, 0.6, 7), Synth.sine(frequency * 2, 0.25, 14), 0.35);
	}

	/** A ney: a pure tone with a slow vibrato and its breath, swelling in and out. */
	private static double[] neyTone(double frequency) {
		double seconds = 1.6;
		double[] tone = new double[(int) (Synth.RATE * seconds)];
		double phase = 0;
		for (int index = 0; index < tone.length; index++) {
			double time = index / Synth.RATE;
			double vibrato = 1 + 0.008 * Math.sin(2 * Math.PI * 5.5 * time) * Math.min(1, time * 2);
			phase += frequency * vibrato / Synth.RATE;
			double swell = Math.min(1, time / 0.2) * Math.min(1, (seconds - time) / 0.5);
			tone[index] = (0.7 * Math.sin(2 * Math.PI * phase) + 0.2 * Math.sin(4 * Math.PI * phase)) * swell;
		}
		return Synth.mix(tone, Synth.lowpass(Synth.noise(seconds), 0.3), 0.05);
	}
}
