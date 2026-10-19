package widgets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SynthTest {

	@Test
	void samplesFollowTheRequestedTime() {
		assertEquals(4410, Synth.tone(440, 0.1, 0.5).length);
		assertEquals(4410, Synth.noise(0.1).length);
		assertEquals(4410, Synth.silence(0.1).length);
	}

	@Test
	void concatenatesInTheGivenOrder() {
		double[] all = Synth.concat(new double[] { 1, 2 }, Synth.silence(0.001), new double[] { 3 });
		assertEquals(2 + 44 + 1, all.length);
		assertEquals(1, all[0]);
		assertEquals(2, all[1]);
		assertEquals(3, all[all.length - 1]);
	}

	@Test
	void mixesByAveragingOrByGain() {
		double[] average = Synth.mix(new double[] { 1, 1 }, new double[] { 1, -1 });
		assertArrayEquals(new double[] { 1, 0 }, average, 1e-9);

		double[] withGain = Synth.mix(new double[] { 1, 1 }, new double[] { 1, -1 }, 0.5);
		assertArrayEquals(new double[] { 1.5, 0.5 }, withGain, 1e-9);
	}

	@Test
	void envelopeFadesInAndOutWithoutClicks() {
		int length = (int) (Synth.RATE * 0.05);
		assertEquals(0, Synth.envelope(0, length, 0.004), 1e-9);
		assertEquals(1, Synth.envelope(length / 2, length, 0.004), 1e-9);
		assertTrue(Synth.envelope(length - 1, length, 0.004) < 0.2);
	}

	@Test
	void toneStaysInsideTheFullScale() {
		for (double sample : Synth.tone(440, 0.05, 0.5)) {
			assertTrue(Math.abs(sample) <= 1.0);
		}
	}
}
