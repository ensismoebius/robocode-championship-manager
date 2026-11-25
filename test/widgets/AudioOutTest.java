package widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import org.junit.jupiter.api.Test;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The tests run with the sound off: the line plays nothing, but takes the sound at the pace it would be heard.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
class AudioOutTest {

	@Test
	void theTestsRunWithTheSoundOff() {
		assertTrue(AudioOut.silent(), "the build sets " + AudioOut.PROPERTY + "=off for the tests");
	}

	@Test
	void theSilentLineTakesTheSoundAtThePaceItWouldBeHeard() throws Exception {
		AudioFormat format = new AudioFormat(Synth.RATE, 16, 2, true, false);
		try (SourceDataLine line = AudioOut.line(format)) {
			line.open(format);
			line.start();
			// A tenth of a second of sound
			byte[] tenth = new byte[(int) (Synth.RATE / 10) * 4];
			long start = System.nanoTime();
			assertEquals(tenth.length, line.write(tenth, 0, tenth.length));
			double seconds = (System.nanoTime() - start) / 1e9;
			assertTrue(seconds >= 0.09, "it took " + seconds + " s");
			assertEquals(Synth.RATE / 10, line.getLongFramePosition(), 1);
		}
	}
}
