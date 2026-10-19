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
