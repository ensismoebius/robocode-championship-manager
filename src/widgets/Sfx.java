package widgets;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * 8 bit style sound effects, synthesized (square waves and noise) so no audio
 * files are needed. Played one at a time in background.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Sfx {

	private static final float RATE = 44100;
	private static final double VOLUME = 0.18;

	private static final ExecutorService PLAYER = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "sfx");
		thread.setDaemon(true);
		return thread;
	});

	/** Cursor moved / item selected */
	public static void blip() {
		Sfx.play(Sfx.tone(988, 0.05, 0.5));
	}

	/** Action confirmed */
	public static void confirm() {
		Sfx.play(Sfx.concat(Sfx.tone(784, 0.06, 0.5), Sfx.tone(1175, 0.10, 0.5)));
	}

	/** Something went wrong */
	public static void error() {
		Sfx.play(Sfx.concat(Sfx.tone(196, 0.09, 0.5), Sfx.silence(0.03), Sfx.tone(147, 0.16, 0.5)));
	}

	/** The VS hitting the screen */
	public static void slam() {
		Sfx.play(Sfx.mix(Sfx.noise(0.35), Sfx.sweep(220, 55, 0.35)));
	}

	/** A match was won */
	public static void victory() {
		Sfx.play(Sfx.concat(Sfx.tone(523, 0.09, 0.5), Sfx.tone(659, 0.09, 0.5), Sfx.tone(784, 0.09, 0.5), Sfx.tone(1047, 0.28, 0.5)));
	}

	/** The championship has a champion */
	public static void fanfare() {
		Sfx.play(Sfx.concat(Sfx.tone(523, 0.12, 0.5), Sfx.tone(523, 0.12, 0.5), Sfx.tone(523, 0.12, 0.5), Sfx.tone(698, 0.36, 0.5), Sfx.tone(622, 0.12, 0.5),
				Sfx.tone(698, 0.12, 0.5), Sfx.tone(784, 0.12, 0.5), Sfx.tone(1047, 0.6, 0.5)));
	}

	// --------------------------------------------------------------- synthesis

	/**
	 * Square wave with a short fade out, so it does not click
	 *
	 * @param duty
	 *            fraction of the period the wave is up
	 */
	static double[] tone(double frequency, double seconds, double duty) {
		double[] samples = new double[(int) (RATE * seconds)];
		for (int i = 0; i < samples.length; i++) {
			double phase = (i * frequency / RATE) % 1.0;
			samples[i] = (phase < duty ? 1 : -1) * Sfx.envelope(i, samples.length);
		}
		return samples;
	}

	/** Square wave gliding from one frequency to another */
	static double[] sweep(double from, double to, double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		double phase = 0;
		for (int i = 0; i < samples.length; i++) {
			double frequency = from + (to - from) * i / samples.length;
			phase = (phase + frequency / RATE) % 1.0;
			samples[i] = (phase < 0.5 ? 1 : -1) * Sfx.envelope(i, samples.length);
		}
		return samples;
	}

	/** White noise that fades out, like an explosion */
	static double[] noise(double seconds) {
		double[] samples = new double[(int) (RATE * seconds)];
		java.util.Random random = new java.util.Random(42);
		for (int i = 0; i < samples.length; i++) {
			double decay = 1.0 - (double) i / samples.length;
			samples[i] = (random.nextDouble() * 2 - 1) * decay * decay;
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

	static double[] mix(double[] a, double[] b) {
		double[] mixed = new double[Math.max(a.length, b.length)];
		for (int i = 0; i < mixed.length; i++) {
			mixed[i] = ((i < a.length ? a[i] : 0) + (i < b.length ? b[i] : 0)) / 2;
		}
		return mixed;
	}

	private static double envelope(int i, int length) {
		int attack = (int) (RATE * 0.004);
		int release = Math.min(length / 3, (int) (RATE * 0.03));
		if (i < attack) {
			return (double) i / attack;
		}
		if (i > length - release) {
			return (double) (length - i) / release;
		}
		return 1;
	}

	/**
	 * 16 bit little endian mono PCM
	 */
	static byte[] toPcm(double[] samples) {
		byte[] pcm = new byte[samples.length * 2];
		for (int i = 0; i < samples.length; i++) {
			int value = (int) (Math.max(-1, Math.min(1, samples[i])) * VOLUME * Short.MAX_VALUE);
			pcm[2 * i] = (byte) value;
			pcm[2 * i + 1] = (byte) (value >> 8);
		}
		return pcm;
	}

	private static void write(byte[] pcm) {
		AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
		try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
			line.open(format);
			line.start();
			line.write(pcm, 0, pcm.length);
			line.drain();
		} catch (LineUnavailableException | IllegalArgumentException e) {
			// Sound effects are optional
		}
	}

	private static void play(double[] samples) {
		byte[] pcm = Sfx.toPcm(samples);
		PLAYER.execute(() -> Sfx.write(pcm));
	}
}
