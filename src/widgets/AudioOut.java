package widgets;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Where the sound goes out: the audio line of the computer, or, when the system property {@value #PROPERTY} is "off", a silent
 * line that takes the sound at the pace it would be played and plays nothing. The tests run silent, so they do not open streams in
 * the sound server of the computer they run on, nor change its settings.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class AudioOut {

	/** The system property that, set to "off", silences every sound */
	public static final String PROPERTY = "robocode.audio";

	/** Only static methods: this class is not instantiated. */
	private AudioOut() {
	}

	/** Whether the sound is off: nothing reaches the audio device. */
	public static boolean silent() {
		return "off".equalsIgnoreCase(System.getProperty(PROPERTY));
	}

	/**
	 * A line for the sound in the format, not open yet: the real one, or a silent one when the sound is off.
	 *
	 * @throws LineUnavailableException
	 *             when the computer has no line for the format
	 */
	public static SourceDataLine line(AudioFormat format) throws LineUnavailableException {
		return silent() ? new Silent(format) : AudioSystem.getSourceDataLine(format);
	}

	/** A line that plays nothing: what is written to it takes as long as it would take to be heard, so the timing is the same. */
	private static final class Silent implements SourceDataLine {

		private AudioFormat format;
		private boolean open;
		private boolean running;
		private long frames;

		Silent(AudioFormat format) {
			this.format = format;
		}

		@Override
		public void open(AudioFormat format, int bufferSize) {
			this.open(format);
		}

		@Override
		public void open(AudioFormat format) {
			this.format = format;
			this.open = true;
		}

		@Override
		public void open() {
			this.open = true;
		}

		@Override
		public int write(byte[] data, int offset, int length) {
			int frameSize = Math.max(1, this.format.getFrameSize());
			long count = length / frameSize;
			this.frames += count;
			try {
				Thread.sleep((long) (count * 1000 / Math.max(1f, this.format.getFrameRate())));
			} catch (InterruptedException interrupted) {
				// Stopped, as a real line is: what is left of the sound is not waited for
				Thread.currentThread().interrupt();
			}
			return length;
		}

		@Override
		public void drain() {
		}

		@Override
		public void flush() {
		}

		@Override
		public void start() {
			this.running = true;
		}

		@Override
		public void stop() {
			this.running = false;
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		@Override
		public boolean isActive() {
			return this.running;
		}

		@Override
		public AudioFormat getFormat() {
			return this.format;
		}

		@Override
		public int getBufferSize() {
			return 0;
		}

		@Override
		public int available() {
			return 0;
		}

		@Override
		public int getFramePosition() {
			return (int) this.frames;
		}

		@Override
		public long getLongFramePosition() {
			return this.frames;
		}

		@Override
		public long getMicrosecondPosition() {
			return (long) (this.frames * 1_000_000L / Math.max(1f, this.format.getFrameRate()));
		}

		@Override
		public float getLevel() {
			return AudioSystem.NOT_SPECIFIED;
		}

		@Override
		public Line.Info getLineInfo() {
			return new DataLine.Info(SourceDataLine.class, this.format);
		}

		@Override
		public void close() {
			this.open = false;
			this.running = false;
		}

		@Override
		public boolean isOpen() {
			return this.open;
		}

		@Override
		public Control[] getControls() {
			return new Control[0];
		}

		@Override
		public boolean isControlSupported(Control.Type control) {
			return false;
		}

		@Override
		public Control getControl(Control.Type control) {
			throw new IllegalArgumentException("a silent line has no controls");
		}

		@Override
		public void addLineListener(LineListener listener) {
		}

		@Override
		public void removeLineListener(LineListener listener) {
		}
	}
}
