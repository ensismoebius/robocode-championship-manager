package widgets;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Shared PCM synthesis primitives for {@link Sfx} and {@link BattleAudio}:
 * waves, sweeps, noise and the helpers that join, mix and envelope sample
 * arrays. Everything is generated, no audio files are needed.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class Synth {

	static final float RATE = 44100;

	/** Fade in of the short UI sounds, {@link #tone} and {@link #sweep} */
	private static final double ATTACK = 0.004;

	/** Fade in of the decaying waves, {@link #square} and {@link #sweep} */
	private static final double DECAY_ATTACK = 0.003;

	private Synth() {
	}

	/**
	 * Square wave with a short fade in and out, so it does not click
	 *
	 * @param duty
	 *            fraction of the period the wave is up
	 */
	static double[] tone(double frequency, double seconds, double duty) {
		double[] samples = new double[(int) (RATE * seconds)];
		for (int index = 0; index < samples.length; index++) {
			double phase = (index * frequency / RATE) % 1.0;
			samples[index] = (phase < duty ? 1 : -1) * Synth.envelope(index, samples.length, ATTACK);
		}
		return samples;
	}

	/** Square wave gliding from one frequency to another, with a short fade in */
	static double[] sweep(double from, double toValue, double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		double phase = 0;
		for (int index = 0; index < samples.length; index++) {
			double frequency = from + (toValue - from) * index / samples.length;
			phase = (phase + frequency / RATE) % 1.0;
			samples[index] = (phase < 0.5 ? 1 : -1) * Synth.envelope(index, samples.length, ATTACK);
		}
		return samples;
	}

	/** Square wave gliding, optionally with a decay */
	static double[] sweep(double from, double toValue, double seconds, boolean decay) {
		double[] samples = new double[(int) (RATE * seconds)];
		double phase = 0;
		for (int index = 0; index < samples.length; index++) {
			double time = (double) index / samples.length;
			phase = (phase + (from + (toValue - from) * time) / RATE) % 1;
			samples[index] = (phase < 0.5 ? 1 : -1) * (decay ? (1 - time) * (1 - time) : Synth.envelope(index, samples.length, DECAY_ATTACK));
		}
		return samples;
	}

	/** Square wave, decaying */
	static double[] square(double frequency, double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		for (int index = 0; index < samples.length; index++) {
			double phase = (index * frequency / RATE) % 1;
			samples[index] = (phase < 0.5 ? 1 : -1) * Synth.envelope(index, samples.length, DECAY_ATTACK);
		}
		return samples;
	}

	/** Saw wave, decaying */
	static double[] saw(double frequency, double seconds, double decay) {
		double[] samples = new double[(int) (RATE * seconds)];
		for (int index = 0; index < samples.length; index++) {
			double phase = (index * frequency / RATE) % 1;
			samples[index] = (phase * 2 - 1) * Math.exp(-decay * index / RATE) * Math.min(1, index / (RATE * 0.004));
		}
		return samples;
	}

	/** Decaying sine */
	static double[] sine(double frequency, double seconds, double decay) {
		double[] samples = new double[(int) (RATE * seconds)];
		for (int index = 0; index < samples.length; index++) {
			double time = index / RATE;
			samples[index] = Math.sin(2 * Math.PI * frequency * time) * Math.exp(-decay * time);
		}
		return samples;
	}

	/** Sine gliding down exponentially, the body of kicks and explosions */
	static double[] sineSweep(double from, double toValue, double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		double phase = 0;
		for (int index = 0; index < samples.length; index++) {
			double time = (double) index / samples.length;
			double frequency = toValue + (from - toValue) * Math.exp(-6 * time);
			phase += frequency / RATE;
			samples[index] = Math.sin(2 * Math.PI * phase) * (1 - time) * (1 - time) * Math.min(1, index / (RATE * 0.002));
		}
		return samples;
	}

	/** White noise that fades out, like an explosion (fixed seed: always the same) */
	static double[] noise(double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		java.util.Random random = new java.util.Random(42);
		for (int index = 0; index < samples.length; index++) {
			double decay = 1.0 - (double) index / samples.length;
			samples[index] = (random.nextDouble() * 2 - 1) * decay * decay;
		}
		return samples;
	}

	/** White noise with an exponential decay (0: flat) */
	static double[] noise(double seconds, double decay) {
		double[] samples = new double[(int) (RATE * seconds)];
		java.util.Random random = new java.util.Random();
		for (int index = 0; index < samples.length; index++) {
			samples[index] = (random.nextDouble() * 2 - 1) * Math.exp(-decay * index / RATE) * Math.min(1, (samples.length - index) / (RATE * 0.02));
		}
		return samples;
	}

	/** Random pops, like debris and fire */
	static double[] crackle(double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		java.util.Random random = new java.util.Random();
		for (int index = 0; index < samples.length; index++) {
			double time = (double) index / samples.length;
			if (random.nextDouble() < 0.004 * (1 - time)) {
				int length = 40 + random.nextInt(200);
				double amplitude = 0.6 + random.nextDouble() * 0.4;
				for (int innerIndex = 0; innerIndex < length && index + innerIndex < samples.length; innerIndex++) {
					samples[index + innerIndex] += (random.nextDouble() * 2 - 1) * amplitude * (1 - (double) innerIndex / length);
				}
			}
		}
		return samples;
	}

	/** One pole low pass, alpha from 0 (closed) to 1 (open) */
	static double[] lowpass(double[] samples, double alpha) {
		double state = 0;
		for (int index = 0; index < samples.length; index++) {
			state += (samples[index] - state) * alpha;
			samples[index] = state;
		}
		// Low pass makes it quieter, gets the level back
		double peak = 1e-9;
		for (double sample : samples) {
			peak = Math.max(peak, Math.abs(sample));
		}
		for (int index2 = 0; index2 < samples.length; index2++) {
			samples[index2] /= peak;
		}
		return samples;
	}

	static double[] silence(double seconds) {
		return new double[(int) (RATE * seconds)];
	}

	static double[] concat(double[]... parts) {
		int length = 0;
		for (double[] part : parts) {
			length += part.length;
		}
		double[] all = new double[length];
		int position = 0;
		for (double[] part : parts) {
			System.arraycopy(part, 0, all, position, part.length);
			position += part.length;
		}
		return all;
	}

	/** Average of two signals */
	static double[] mix(double[] first, double[] second) {
		double[] mixed = new double[Math.max(first.length, second.length)];
		for (int index = 0; index < mixed.length; index++) {
			mixed[index] = ((index < first.length ? first[index] : 0) + (index < second.length ? second[index] : 0)) / 2;
		}
		return mixed;
	}

	/**
	 * a + b * gain
	 */
	static double[] mix(double[] first, double[] second, double gain) {
		double[] mixed = new double[Math.max(first.length, second.length)];
		for (int index = 0; index < mixed.length; index++) {
			mixed[index] = (index < first.length ? first[index] : 0) + (index < second.length ? second[index] * gain : 0);
		}
		return mixed;
	}

	/**
	 * Fade in and out, so a sound does not click
	 *
	 * @param attackSeconds
	 *            how long the sound takes to reach full volume
	 */
	static double envelope(int index, int length, double attackSeconds) {
		int attack = (int) (RATE * attackSeconds);
		int release = Math.min(length / 3, (int) (RATE * 0.03));
		if (index < attack) {
			return (double) index / attack;
		}
		if (index > length - release) {
			return (double) (length - index) / release;
		}
		return 1;
	}
}
