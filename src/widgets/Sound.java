package widgets;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Plays MP3 (or WAV) audio with javax.sound. MP3 is decoded by the pure Java
 * mp3spi/JLayer libraries, so it does not depend on the codecs installed in the
 * system like the JavaFX media player does.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Sound {

	private final byte[] audio;
	private final String name;

	private volatile double volume = 1.0;
	private volatile boolean loop = false;
	private volatile boolean stopped = false;
	private Thread thread;

	private Sound(byte[] audio, String name) {
		this.audio = audio;
		this.name = name;
	}

	/**
	 * @return the sound or null if the resource does not exist
	 */
	public static Sound fromResource(Class<?> base, String resource) {
		try (InputStream inputStream = base.getResourceAsStream(resource)) {
			if (inputStream == null) {
				System.err.println("Áudio não encontrado: " + resource);
				return null;
			}
			return new Sound(inputStream.readAllBytes(), resource);
		} catch (IOException ioException) {
			System.err.println("Não foi possível ler " + resource + ": " + ioException.getMessage());
			return null;
		}
	}

	/**
	 * @return the sound or null if there is no audio
	 */
	public static Sound fromBytes(byte[] audio, String name) {
		if (audio == null || audio.length == 0) {
			return null;
		}
		return new Sound(audio, name);
	}

	/**
	 * @return true if the audio can be decoded and played
	 */
	public static boolean isPlayable(byte[] audio) {
		if (audio == null || audio.length == 0) {
			return false;
		}
		try (AudioInputStream decoded = Sound.decode(audio)) {
			// Decodes the first frames, a broken file fails here
			return decoded.read(new byte[4096]) > 0;
		} catch (IOException | UnsupportedAudioFileException | IllegalArgumentException illegalArgumentException) {
			return false;
		}
	}

	/**
	 * Decodes the audio to 16 bits signed little endian PCM
	 */
	private static AudioInputStream decode(byte[] audio) throws UnsupportedAudioFileException, IOException {
		// mp3spi needs a stream that supports mark/reset
		AudioInputStream source = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(audio)));
		AudioFormat sourceFormat = source.getFormat();
		AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2,
				sourceFormat.getSampleRate(), false);
		return AudioSystem.getAudioInputStream(pcm, source);
	}

	/**
	 * @param volume
	 *            from 0 (silent) to 1 (original volume)
	 */
	public void setVolume(double volume) {
		this.volume = Math.max(0, Math.min(1, volume));
	}

	/**
	 * Plays again when it reaches the end, until {@link #stop()} is called
	 */
	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	/**
	 * Starts playing in background
	 */
	public synchronized void play() {
		if (this.thread != null) {
			return;
		}
		this.stopped = false;
		this.thread = new Thread(this::playback, "sound-" + this.name);
		this.thread.setDaemon(true);
		this.thread.start();
	}

	/**
	 * Stops playing, can not be played again
	 */
	public void stop() {
		this.stopped = true;
		Thread thread2 = this.thread;
		if (thread2 != null) {
			thread2.interrupt();
		}
	}

	private void playback() {
		SourceDataLine line = null;
		try {
			do {
				try (AudioInputStream decoded = Sound.decode(this.audio)) {
					AudioFormat format = decoded.getFormat();
					if (line == null) {
						line = AudioSystem.getSourceDataLine(format);
						line.open(format);
						line.start();
					}

					byte[] buffer = new byte[8192];
					int read;
					while (!this.stopped && (read = decoded.read(buffer, 0, buffer.length)) > 0) {
						this.applyVolume(buffer, read);
						line.write(buffer, 0, read);
					}
				}
			} while (this.loop && !this.stopped);

			if (!this.stopped) {
				line.drain();
			}
		} catch (IOException | UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException illegalArgumentException) {
			if (!this.stopped) {
				System.err.println("Não foi possível tocar " + this.name + ": " + illegalArgumentException);
			}
		} finally {
			if (line != null) {
				line.stop();
				line.flush();
				line.close();
			}
		}
	}

	/**
	 * Scales the 16 bits little endian samples. Done in software because not all
	 * audio systems offer a gain control
	 */
	private void applyVolume(byte[] buffer, int length) {
		double vCoord = this.volume;
		if (vCoord >= 1.0) {
			return;
		}
		for (int index = 0; index + 1 < length; index += 2) {
			int sample = (short) ((buffer[index] & 0xff) | (buffer[index + 1] << 8));
			sample = (int) (sample * vCoord);
			buffer[index] = (byte) sample;
			buffer[index + 1] = (byte) (sample >> 8);
		}
	}
}
