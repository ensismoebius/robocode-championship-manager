package widgets;

import static widgets.BattleAudio.isOn;

import java.util.List;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The song of the glacial tundra arena, after the music of the Inuit, which has no scale of its own, only habits: songs
 * of a few notes in a narrow range, with minor seconds and major thirds, that undulate and come down, over a frame
 * drum. Here the five notes are a minor second under the tonic, then the tonic, a second, a major third and a fifth, in
 * the range of a minor sixth. A voice sings them without words, two throat singers face each other and answer each
 * other's breaths, and a bare drone of the tonic and its fifth lies under everything, with no third to make it major
 * or minor. As the fight heats up the voice sings every beat and the drum doubles.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class BattleAudioTundra {

	/** The five notes of the song, as semitones over its tonic */
	private static final int[] SCALE = { -1, 0, 2, 4, 7 };
	/** The tonic of the voice, A3 */
	private static final double TONIC = 220;
	/** The two phrases of the voice, as notes of the scale, one for each eighth: they undulate and come down */
	private static final int[][] PHRASES = { { 4, 3, 3, 2, 3, 2, 1, 1 }, { 3, 2, 1, 0, 1, 1, 2, 1 } };
	/** The sixteenths of half a bar where the first throat singer breathes: the second one answers two sixteenths later */
	private static final int[] BREATHS = { 0, 3, 4, 6 };

	/** One beat of the frame drum: the steps of the bar where it plays, the tension from which it plays and its gain */
	private record Beat(int[] steps, double from, double gain) {
	}

	private final BattleAudio host;
	private final double[] drum = Percussion.frameDrum();
	private final double[] breathIn = breath(true);
	private final double[] breathOut = breath(false);
	private final double[][] sung = new double[SCALE.length][];
	private final double[][] called = new double[SCALE.length][];
	private final List<Beat> beats = List.of(new Beat(new int[] { 0, 8 }, -1, 0.62), new Beat(new int[] { 4, 12 }, -1, 0.42), new Beat(new int[] { 2, 6, 10, 14 }, 0.5, 0.26));

	/** The score of the song, playing in the mixer of the given battle audio. */
	BattleAudioTundra(BattleAudio host) {
		this.host = host;
		for (int note = 0; note < SCALE.length; note++) {
			double frequency = TONIC * Math.pow(2, SCALE[note] / 12.0);
			this.sung[note] = sing(frequency, 1.4);
			this.called[note] = sing(frequency, 0.6);
		}
	}

	/** Plays what the sixteenth of the song has: the frame drum, the voice and the throat singers. */
	void step(int inBar, int bar, double tension) {
		for (Beat beat : this.beats) {
			if (tension > beat.from() && isOn(beat.steps(), inBar)) {
				host.musicVoices.add(new Voice(this.drum, beat.gain(), inBar == 0 ? 0.92 : 1));
			}
		}
		this.voice(inBar, bar, tension);
		this.throatSingers(inBar, tension);
	}

	/** The voice: a long note on each half bar, and a shorter one on each beat when the fight is hot. */
	private void voice(int inBar, int bar, double tension) {
		boolean hot = tension > 0.5;
		if (inBar % (hot ? 4 : 8) == 0) {
			int note = PHRASES[bar % 2][inBar / 2];
			host.sided((hot ? this.called : this.sung)[note], 0.2 + 0.08 * tension, 1, inBar % 8 == 0);
		}
	}

	/** The throat singers, one in each ear: the first breathes out in a short pattern, and the second answers it, breathing in, two sixteenths later. */
	private void throatSingers(int inBar, double tension) {
		if (tension <= 0.2) {
			return;
		}
		int half = inBar % 8;
		if (isOn(BREATHS, half)) {
			host.sided(this.breathOut, 0.16 + 0.1 * tension, 1, true);
		}
		if (isOn(BREATHS, Math.floorMod(half - 2, 8))) {
			host.sided(this.breathIn, 0.16 + 0.1 * tension, 1, false);
		}
	}

	/** A voice that sings a note without words: a few harmonics with a slow vibrato, swelling in and out. */
	private static double[] sing(double frequency, double seconds) {
		double[] tone = new double[(int) (Synth.RATE * seconds)];
		double phase = 0;
		for (int index = 0; index < tone.length; index++) {
			double time = index / Synth.RATE;
			phase += frequency * (1 + 0.012 * Math.sin(2 * Math.PI * 4.8 * time)) / Synth.RATE;
			double swell = Math.min(1, time / (seconds * 0.18)) * Math.min(1, (seconds - time) / (seconds * 0.4));
			tone[index] = 0.55 * (Math.sin(2 * Math.PI * phase) + 0.45 * Math.sin(4 * Math.PI * phase) + 0.2 * Math.sin(6 * Math.PI * phase)) * swell;
		}
		return tone;
	}

	/**
	 * A breath of throat singing: air that swells and falls like a "ha", with a low growl in it. Breathing in, the air swells
	 * slowly and falls fast; breathing out, it swells fast and falls slowly.
	 */
	private static double[] breath(boolean inhale) {
		double[] air = Synth.lowpass(Synth.noise(0.17, 0), 0.32);
		for (int index = 0; index < air.length; index++) {
			double time = (double) index / air.length;
			double swell = inhale ? Math.pow(time, 1.5) * Math.sqrt(1 - time) * 1.6 : Math.pow(1 - time, 1.6) * Math.min(1, time * 12);
			double growl = Math.sin(2 * Math.PI * 92 * index / Synth.RATE) * (0.55 + 0.45 * Math.sin(2 * Math.PI * 31 * index / Synth.RATE));
			air[index] = (air[index] * 0.8 + growl * 0.45) * swell;
		}
		return air;
	}
}
