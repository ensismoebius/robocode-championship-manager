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
		for (int i = 0; i < samples.length; i++) {
			double phase = (i * frequency / RATE) % 1.0;
			samples[i] = (phase < duty ? 1 : -1) * Synth.envelope(i, samples.length, ATTACK);
		}
		return samples;
	}

	/** Square wave gliding from one frequency to another, with a short fade in */
	static double[] sweep(double from, double to, double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		double phase = 0;
		for (int i = 0; i < samples.length; i++) {
			double frequency = from + (to - from) * i / samples.length;
			phase = (phase + frequency / RATE) % 1.0;
			samples[i] = (phase < 0.5 ? 1 : -1) * Synth.envelope(i, samples.length, ATTACK);
		}
		return samples;
	}

	/** Square wave gliding, optionally with a decay */
	static double[] sweep(double from, double to, double seconds, boolean decay) {
		double[] samples = new double[(int) (RATE * seconds)];
		double phase = 0;
		for (int i = 0; i < samples.length; i++) {
			double t = (double) i / samples.length;
			phase = (phase + (from + (to - from) * t) / RATE) % 1;
			samples[i] = (phase < 0.5 ? 1 : -1) * (decay ? (1 - t) * (1 - t) : Synth.envelope(i, samples.length, DECAY_ATTACK));
		}
		return samples;
	}

	/** Square wave, decaying */
	static double[] square(double frequency, double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		for (int i = 0; i < samples.length; i++) {
			double phase = (i * frequency / RATE) % 1;
			samples[i] = (phase < 0.5 ? 1 : -1) * Synth.envelope(i, samples.length, DECAY_ATTACK);
		}
		return samples;
	}

	/** Saw wave, decaying */
	static double[] saw(double frequency, double seconds, double decay) {
		double[] samples = new double[(int) (RATE * seconds)];
		for (int i = 0; i < samples.length; i++) {
			double phase = (i * frequency / RATE) % 1;
			samples[i] = (phase * 2 - 1) * Math.exp(-decay * i / RATE) * Math.min(1, i / (RATE * 0.004));
		}
		return samples;
	}

	/** Decaying sine */
	static double[] sine(double frequency, double seconds, double decay) {
		double[] samples = new double[(int) (RATE * seconds)];
		for (int i = 0; i < samples.length; i++) {
			double t = i / RATE;
			samples[i] = Math.sin(2 * Math.PI * frequency * t) * Math.exp(-decay * t);
		}
		return samples;
	}

	/** Sine gliding down exponentially, the body of kicks and explosions */
	static double[] sineSweep(double from, double to, double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		double phase = 0;
		for (int i = 0; i < samples.length; i++) {
			double t = (double) i / samples.length;
			double frequency = to + (from - to) * Math.exp(-6 * t);
			phase += frequency / RATE;
			samples[i] = Math.sin(2 * Math.PI * phase) * (1 - t) * (1 - t) * Math.min(1, i / (RATE * 0.002));
		}
		return samples;
	}

	/** White noise that fades out, like an explosion (fixed seed: always the same) */
	static double[] noise(double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		java.util.Random random = new java.util.Random(42);
		for (int i = 0; i < samples.length; i++) {
			double decay = 1.0 - (double) i / samples.length;
			samples[i] = (random.nextDouble() * 2 - 1) * decay * decay;
		}
		return samples;
	}

	/** White noise with an exponential decay (0: flat) */
	static double[] noise(double seconds, double decay) {
		double[] samples = new double[(int) (RATE * seconds)];
		java.util.Random random = new java.util.Random();
		for (int i = 0; i < samples.length; i++) {
			samples[i] = (random.nextDouble() * 2 - 1) * Math.exp(-decay * i / RATE) * Math.min(1, (samples.length - i) / (RATE * 0.02));
		}
		return samples;
	}

	/** Random pops, like debris and fire */
	static double[] crackle(double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		java.util.Random random = new java.util.Random();
		for (int i = 0; i < samples.length; i++) {
			double t = (double) i / samples.length;
			if (random.nextDouble() < 0.004 * (1 - t)) {
				int length = 40 + random.nextInt(200);
				double amplitude = 0.6 + random.nextDouble() * 0.4;
				for (int j = 0; j < length && i + j < samples.length; j++) {
					samples[i + j] += (random.nextDouble() * 2 - 1) * amplitude * (1 - (double) j / length);
				}
			}
		}
		return samples;
	}

	/** One pole low pass, alpha from 0 (closed) to 1 (open) */
	static double[] lowpass(double[] samples, double alpha) {
		double state = 0;
		for (int i = 0; i < samples.length; i++) {
			state += (samples[i] - state) * alpha;
			samples[i] = state;
		}
		// Low pass makes it quieter, gets the level back
		double peak = 1e-9;
		for (double sample : samples) {
			peak = Math.max(peak, Math.abs(sample));
		}
		for (int i = 0; i < samples.length; i++) {
			samples[i] /= peak;
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
	static double[] mix(double[] a, double[] b) {
		double[] mixed = new double[Math.max(a.length, b.length)];
		for (int i = 0; i < mixed.length; i++) {
			mixed[i] = ((i < a.length ? a[i] : 0) + (i < b.length ? b[i] : 0)) / 2;
		}
		return mixed;
	}

	/**
	 * a + b * gain
	 */
	static double[] mix(double[] a, double[] b, double gain) {
		double[] mixed = new double[Math.max(a.length, b.length)];
		for (int i = 0; i < mixed.length; i++) {
			mixed[i] = (i < a.length ? a[i] : 0) + (i < b.length ? b[i] * gain : 0);
		}
		return mixed;
	}

	/**
	 * Fade in and out, so a sound does not click
	 *
	 * @param attackSeconds
	 *            how long the sound takes to reach full volume
	 */
	static double envelope(int i, int length, double attackSeconds) {
		int attack = (int) (RATE * attackSeconds);
		int release = Math.min(length / 3, (int) (RATE * 0.03));
		if (i < attack) {
			return (double) i / attack;
		}
		if (i > length - release) {
			return (double) (length - i) / release;
		}
		return 1;
	}
}
