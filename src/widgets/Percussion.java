package widgets;


/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The percussion of the battle music, synthesized: drums, snares, hats and the heartbeat.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class Percussion {

	private Percussion() {
	}

	static double[] taiko() {
		return Synth.mix(Synth.sineSweep(105, 40, 0.75), Synth.lowpass(Synth.noise(0.12, 7), 0.15), 0.6);
	}

	static double[] smallDrum() {
		return Synth.mix(Synth.sineSweep(260, 150, 0.16), Synth.lowpass(Synth.noise(0.08, 9), 0.4), 0.4);
	}

	/** A wood block: a short, hollow click */
	static double[] woodBlock() {
		return Synth.mix(Synth.sine(1250, 0.07, 45), Synth.sine(2100, 0.04, 70), 0.4);
	}

	static double[] kick() {
		return Synth.mix(Synth.sineSweep(150, 42, 0.32), Synth.lowpass(Synth.noise(0.02, 8), 0.5), 0.15);
	}

	static double[] snare() {
		return Synth.mix(Synth.lowpass(Synth.noise(0.2, 3.5), 0.6), Synth.sine(190, 0.12, 20), 0.35);
	}

	static double[] hat() {
		double[] noise = Synth.noise(0.05, 6);
		// High pass: noise minus its low pass
		double[] low = Synth.lowpass(noise.clone(), 0.3);
		for (int index = 0; index < noise.length; index++) {
			noise[index] -= low[index];
		}
		return noise;
	}

	static double[] heartbeat() {
		return Synth.concat(Synth.sineSweep(70, 40, 0.16), Synth.silence(0.1), Synth.sineSweep(60, 35, 0.2));
	}
}
