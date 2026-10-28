package widgets;


/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The percussion of the battle music, synthesized: drums, snares, hats and the heartbeat.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class Percussion {

	/** The percussion sounds are static: this class is not instantiated. */
	private Percussion() {
	}

	/** The taiko drum: a low sweep with a little noise. */
	static double[] taiko() {
		return Synth.mix(Synth.sineSweep(105, 40, 0.75), Synth.lowpass(Synth.noise(0.12, 7), 0.15), 0.6);
	}

	/** A small drum: a short high sweep with noise. */
	static double[] smallDrum() {
		return Synth.mix(Synth.sineSweep(260, 150, 0.16), Synth.lowpass(Synth.noise(0.08, 9), 0.4), 0.4);
	}

	/** The darbuka's deep stroke, in the middle of the skin: a short sweep down with a dull thud */
	static double[] dum() {
		return Synth.mix(Synth.sineSweep(170, 70, 0.22), Synth.lowpass(Synth.noise(0.04, 10), 0.2), 0.2);
	}

	/** The darbuka's bright stroke, with the fingers on the rim: a short click */
	static double[] tek() {
		return Synth.mix(Synth.sine(980, 0.05, 40), Synth.lowpass(Synth.noise(0.03, 30), 0.6), 0.5);
	}

	/** The frame drum of the Inuit, struck on its rim with a stick: a deep boom of the skin with a dry knock */
	static double[] frameDrum() {
		return Synth.mix(Synth.sineSweep(150, 88, 0.42), Synth.lowpass(Synth.noise(0.05, 11), 0.35), 0.4);
	}

	/** A wood block: a short, hollow click */
	static double[] woodBlock() {
		return Synth.mix(Synth.sine(1250, 0.07, 45), Synth.sine(2100, 0.04, 70), 0.4);
	}

	/** The kick drum: a deep and short sweep. */
	static double[] kick() {
		return Synth.mix(Synth.sineSweep(150, 42, 0.32), Synth.lowpass(Synth.noise(0.02, 8), 0.5), 0.15);
	}

	/** The snare: noise with a short tone. */
	static double[] snare() {
		return Synth.mix(Synth.lowpass(Synth.noise(0.2, 3.5), 0.6), Synth.sine(190, 0.12, 20), 0.35);
	}

	/** The hi-hat: very short noise, with the low frequencies removed. */
	static double[] hat() {
		return highPass(Synth.noise(0.05, 6), 0.3);
	}

	/** The noise minus its low pass: only the high frequencies are left. */
	private static double[] highPass(double[] noise, double alpha) {
		double[] low = Synth.lowpass(noise.clone(), alpha);
		for (int index = 0; index < noise.length; index++) {
			noise[index] -= low[index];
		}
		return noise;
	}

	/** A shekere, a gourd with a net of beads: noise with no lows, a little longer than a hat. */
	static double[] shaker() {
		return highPass(Synth.noise(0.09, 14), 0.2);
	}

	/** An agogo, a double iron bell: two tones that do not fit each other and ring. */
	static double[] agogo() {
		return Synth.mix(Synth.sine(1180, 0.22, 20), Synth.sine(1770, 0.16, 28), 0.7);
	}

	/** A conga played with the open hand: a short sweep with the ring of the skin. */
	static double[] conga() {
		return Synth.mix(Synth.sineSweep(240, 150, 0.26), Synth.lowpass(Synth.noise(0.03, 12), 0.3), 0.3);
	}

	/** A conga slap, the hand on the rim: sharp and dry. */
	static double[] slap() {
		return Synth.mix(highPass(Synth.noise(0.05, 40), 0.35), Synth.sine(420, 0.05, 50), 0.5);
	}

	/** A heartbeat: two low sweeps with a pause between them. */
	static double[] heartbeat() {
		return Synth.concat(Synth.sineSweep(70, 40, 0.16), Synth.silence(0.1), Synth.sineSweep(60, 35, 0.2));
	}
}
