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

	private static final double VOLUME = 0.18;

	private static final ExecutorService PLAYER = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "sfx");
		thread.setDaemon(true);
		return thread;
	});

	/** Cursor moved / item selected */
	public static void blip() {
		Sfx.play(Synth.tone(988, 0.05, 0.5));
	}

	/** Action confirmed */
	public static void confirm() {
		Sfx.play(Synth.concat(Synth.tone(784, 0.06, 0.5), Synth.tone(1175, 0.10, 0.5)));
	}

	/** Something went wrong */
	public static void error() {
		Sfx.play(Synth.concat(Synth.tone(196, 0.09, 0.5), Synth.silence(0.03), Synth.tone(147, 0.16, 0.5)));
	}

	/** The VS hitting the screen */
	public static void slam() {
		Sfx.play(Synth.mix(Synth.noise(0.35), Synth.sweep(220, 55, 0.35)));
	}

	/** A match was won */
	public static void victory() {
		Sfx.play(Synth.concat(Synth.tone(523, 0.09, 0.5), Synth.tone(659, 0.09, 0.5), Synth.tone(784, 0.09, 0.5), Synth.tone(1047, 0.28, 0.5)));
	}

	/** The championship has a champion */
	public static void fanfare() {
		Sfx.play(Synth.concat(Synth.tone(523, 0.12, 0.5), Synth.tone(523, 0.12, 0.5), Synth.tone(523, 0.12, 0.5), Synth.tone(698, 0.36, 0.5), Synth.tone(622, 0.12, 0.5),
				Synth.tone(698, 0.12, 0.5), Synth.tone(784, 0.12, 0.5), Synth.tone(1047, 0.6, 0.5)));
	}

	/** Brass-like chord: saws, a bit of each note, decaying */
	private static double[] brass(double seconds, double decay, double... frequencies) {
		double[] chord = new double[0];
		for (double frequency : frequencies) {
			chord = Synth.mix(chord, Synth.saw(frequency, seconds, decay), 0.5);
		}
		return chord;
	}

	/**
	 * The trophies ceremony: a fanfare that is bigger for each place, the first
	 * one with a boom and a crash on the last chord
	 *
	 * @param place
	 *            1, 2 or 3
	 */
	public static void ceremony(int place) {
		double[] c = { 261.63, 329.63, 392.00, 523.25 };
		double[] f = { 349.23, 440.00, 523.25, 698.46 };
		double[] g = { 392.00, 493.88, 587.33, 783.99 };
		double[] top = { 523.25, 659.25, 783.99, 1046.50 };
		double[] sound;
		if (place == 3) {
			sound = Synth.concat(brass(0.12, 3, c), brass(0.12, 3, c), brass(0.5, 2.5, f));
		} else if (place == 2) {
			sound = Synth.concat(brass(0.12, 3, c), brass(0.12, 3, c), brass(0.12, 3, c), brass(0.3, 2.5, f), brass(0.14, 3, g), brass(0.7, 2, top));
		} else {
			double[] tail = Synth.mix(brass(1.8, 1.4, top), brass(1.8, 1.4, 130.81, 261.63), 0.8);
			tail = Synth.mix(tail, Synth.sineSweep(110, 30, 1.2), 0.9);
			tail = Synth.mix(tail, Synth.lowpass(Synth.noise(1.6, 1.8), 0.5), 0.5);
			sound = Synth.concat(brass(0.13, 3, c), brass(0.13, 3, c), brass(0.13, 3, c), brass(0.34, 2.5, f), brass(0.13, 3, g), brass(0.13, 3, g), brass(0.13, 3, g), brass(0.4, 2.5, top),
					tail);
		}
		Sfx.play(sound);
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
		AudioFormat format = new AudioFormat(Synth.RATE, 16, 1, true, false);
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
