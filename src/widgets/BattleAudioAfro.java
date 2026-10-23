package widgets;

import static widgets.BattleAudio.PENTATONIC;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The Afrofuturist song, the music of the Eden Solar arena: Africa and science fiction in the same groove. From West
 * Africa come the pulse of a bell in a bar of sixteen (3+3+3+3+4), the congas, the shekere, an mbira that calls and
 * answers over the chords, and a talking drum. From the future come a sequencer that bounces between the ears, a
 * theremin that glides over the phrases, lasers and a riser at the end of every four bars, over a pad. As the fight
 * heats up they come in one by one.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class BattleAudioAfro {

	/** The bright scale of the sequencer: a second, a fourth, a fifth and a sixth over the root, the sound of the future */
	private static final double[] SKY = { 1, 1.125, 1.335, 1.498, 1.682, 2 };
	/** What the sequencer plays on each 16th of a bar, as steps of {@link #SKY} */
	private static final int[] ARPEGGIO = { 0, 2, 4, 2, 3, 1, 4, 2, 0, 2, 5, 3, 4, 2, 1, 3 };
	/** The strokes of the bell in a bar: 3+3+3+3+4 */
	private static final int[] BELL = { 0, 3, 6, 9, 12 };
	private static final int[] LOW_CONGA = { 0, 6, 10 };
	private static final int[] SLAPS = { 3, 7, 11, 14 };
	/** The mbira: the 16ths where it plays, and the steps of the minor pentatonic it plays in the call and in the answer */
	private static final int[] MBIRA_STEPS = { 0, 3, 6, 8, 11, 14 };
	private static final int[] CALL = { 0, 2, 4, 3, 2, 1 };
	private static final int[] ANSWER = { 2, 4, 5, 4, 3, 2 };

	private final BattleAudio host;
	private final double[] shaker = Percussion.shaker();
	private final double[] bell = Percussion.agogo();
	private final double[] conga = Percussion.conga();
	private final double[] slap = Percussion.slap();

	/** The score of the song, playing in the mixer of the given battle audio. */
	BattleAudioAfro(BattleAudio host) {
		this.host = host;
	}

	/** Plays what the 16th of the song has: the rhythm, the mbira, the sequencer and the sounds of the sky. */
	void step(int step, int inBar, int bar, double root, double tension) {
		this.rhythm(inBar, tension);
		this.subBass(inBar, root, tension);
		this.mbira(inBar, bar, root, tension);
		this.sequencer(inBar, root, tension);
		this.sky(step, inBar, bar, root, tension);
	}

	/** Whether the 16th of the bar is one of the steps. */
	private static boolean isOn(int[] steps, int inBar) {
		for (int step : steps) {
			if (step == inBar) {
				return true;
			}
		}
		return false;
	}

	/** A voice that plays from one side more than the other, so the sounds bounce between the ears. */
	private void play(double[] sound, double gain, double rate, boolean left) {
		host.musicVoices.add(new Voice(sound, gain, rate, left ? 1 : 0.55, left ? 0.55 : 1));
	}

	/** The percussion: the bell, the shekere on every 16th, the congas and their slaps, a djembe bass, a soft kick at the top. */
	private void rhythm(int inBar, double tension) {
		if (tension > 0.05 && isOn(BELL, inBar)) {
			// Three high strokes and two low ones, as the double bell is played
			this.play(this.bell, inBar % 6 == 0 ? 0.2 : 0.15, inBar % 6 == 0 ? 1 : 0.75, inBar < 8);
		}
		if (tension > 0.1) {
			this.play(this.shaker, inBar % 4 == 2 ? 0.2 : 0.1, 1, inBar % 2 == 0);
		}
		if (tension > 0.12 && isOn(LOW_CONGA, inBar)) {
			host.musicVoices.add(new Voice(this.conga, 0.4, inBar == 0 ? 0.9 : 1));
		}
		if (tension > 0.3 && isOn(SLAPS, inBar)) {
			this.play(this.slap, 0.22, 1, inBar < 8);
		}
		if (tension > 0.3 && (inBar == 0 || inBar == 10)) {
			host.musicVoices.add(new Voice(host.taiko, 0.4 + 0.3 * tension, 1.5));
		}
		if (tension > 0.62 && inBar % 4 == 0) {
			host.musicVoices.add(new Voice(host.kick, 0.5, 1));
		}
	}

	/** A deep sine on the root, twice a bar. */
	private void subBass(int inBar, double root, double tension) {
		if (tension > 0.18 && (inBar == 0 || inBar == 10)) {
			host.musicVoices.add(new Voice(Synth.sine(root, 0.5, 4), 0.5, 1));
		}
	}

	/** The mbira: the first two bars call and the next two answer, higher, with an octave over it when it heats up. */
	private void mbira(int inBar, int bar, double root, double tension) {
		if (tension <= 0.1) {
			return;
		}
		for (int index = 0; index < MBIRA_STEPS.length; index++) {
			if (MBIRA_STEPS[index] != inBar) {
				continue;
			}
			double frequency = root * 4 * PENTATONIC[(bar < 2 ? CALL : ANSWER)[index]];
			double[] note = Synth.mix(Synth.sine(frequency, 0.5, 6), Synth.sine(frequency * 3.9, 0.18, 26), 0.45);
			this.play(note, 0.2 + 0.1 * tension, 1, index % 2 == 0);
			if (tension > 0.55) {
				this.play(note, 0.08, 2, index % 2 == 1);
			}
		}
	}

	/** The sequencer: a plucked line of 16ths in the bright scale, bouncing between the ears. */
	private void sequencer(int inBar, double root, double tension) {
		if (tension <= 0.3) {
			return;
		}
		double frequency = root * 4 * SKY[ARPEGGIO[inBar]] * (tension > 0.8 && inBar % 4 == 3 ? 2 : 1);
		this.play(Synth.lowpass(Synth.saw(frequency, 0.16, 12), 0.22 + 0.3 * tension), 0.07 + 0.1 * tension, 1, inBar % 2 == 0);
	}

	/** The sounds of the sky: the theremin, the lasers, the riser before the phrase turns over, and the talking drum. */
	private void sky(int step, int inBar, int bar, double root, double tension) {
		if (tension > 0.5 && inBar == 0 && bar % 2 == 0) {
			host.musicVoices.add(new Voice(lead(root * 4 * SKY[bar == 0 ? 3 : 4], 1.9), 0.14, 1));
		}
		if (tension > 0.72 && (inBar == 6 || inBar == 14)) {
			host.musicVoices.add(new Voice(Synth.sweep(2400, 500, 0.16, true), 0.06, 1));
		}
		if (tension > 0.6 && step % 64 == 56) {
			host.musicVoices.add(new Voice(Synth.lowpass(Synth.sweep(300, 2000, 0.9, false), 0.2), 0.09, 1));
		}
		if (tension > 0.45 && bar == 3 && (inBar == 12 || inBar == 14)) {
			host.musicVoices.add(new Voice(talkingDrum(root * 3, inBar == 12 ? 0.5 : 0.8, 0.3), 0.35, 1));
		}
	}

	/** A talking drum: a tone that bends up and falls back, as the drummer squeezes the strings under the arm. */
	private static double[] talkingDrum(double base, double bend, double seconds) {
		double[] samples = new double[(int) (Synth.RATE * seconds)];
		double phase = 0;
		for (int index = 0; index < samples.length; index++) {
			double time = (double) index / samples.length;
			phase += base * (1 + bend * Math.sin(Math.PI * Math.min(1, time * 1.6))) / Synth.RATE;
			samples[index] = Math.sin(2 * Math.PI * phase) * Math.exp(-5 * time) * Math.min(1, index / (Synth.RATE * 0.003));
		}
		return samples;
	}

	/** A theremin: a pure tone that slides up to the note, with a slow vibrato, and swells in and out. */
	private static double[] lead(double frequency, double seconds) {
		double[] samples = new double[(int) (Synth.RATE * seconds)];
		double phase = 0;
		for (int index = 0; index < samples.length; index++) {
			double time = index / Synth.RATE;
			double glide = 1 - 0.06 * Math.exp(-time * 9);
			double vibrato = 1 + 0.012 * Math.sin(2 * Math.PI * 5.2 * time) * Math.min(1, time * 2);
			phase += frequency * glide * vibrato / Synth.RATE;
			double swell = Math.min(1, time / 0.12) * Math.min(1, (seconds - time) / 0.5);
			samples[index] = 0.8 * (Math.sin(2 * Math.PI * phase) + 0.25 * Math.sin(4 * Math.PI * phase)) * swell;
		}
		return samples;
	}
}
