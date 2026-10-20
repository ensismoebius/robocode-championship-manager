package widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import widgets.BattleAudio.Song;

class BattleAudioTest {

	private static double rms(double[] samples) {
		double sum = 0;
		for (double sample : samples) {
			sum += sample * sample;
		}
		return Math.sqrt(sum / samples.length);
	}

	@Test
	void everySongPlaysAndGrowsWithTheTension() {
		for (Song song : Song.values()) {
			double calm = rms(BattleAudio.render(song, 0.05, 4));
			double tense = rms(BattleAudio.render(song, 0.95, 4));
			assertTrue(calm > 0.005, song + " calm is silent: " + calm);
			assertTrue(tense > calm * 1.3, song + " does not grow: " + calm + " -> " + tense);
			for (double sample : BattleAudio.render(song, 1, 2)) {
				assertTrue(Math.abs(sample) <= 1, song + " clips");
			}
		}
	}

	@Test
	void musicLevelScalesTheMusic() {
		double full = rms(BattleAudio.render(Song.EPIC, 0.8, 3, 1));
		double half = rms(BattleAudio.render(Song.EPIC, 0.8, 3, 0.5));
		assertTrue(Math.abs(half / full - 0.5) < 0.05, full + " -> " + half);
		assertEquals(0, rms(BattleAudio.render(Song.EPIC, 0.8, 3, 0)), 1e-12);
	}

	@Test
	void configuredOrRandomSong() {
		Random random = new Random(3);
		assertEquals(Song.WAR_DRUMS, Song.resolve("WAR_DRUMS", random));
		for (String configured : new String[] { "RANDOM", null, "nonsense" }) {
			assertTrue(Song.resolve(configured, random) != null);
		}
	}

	@Test
	void quickBulletsSoundHigherAndSlowOnesLower() {
		// Bullets fly at 20 - 3 * power
		assertTrue(BattleAudio.shotPitch(0.1) > BattleAudio.shotPitch(1));
		assertTrue(BattleAudio.shotPitch(1) > BattleAudio.shotPitch(2));
		assertTrue(BattleAudio.shotPitch(2) > BattleAudio.shotPitch(3));
		// Neither silly high nor a growl
		assertTrue(BattleAudio.shotPitch(0.1) < 2 && BattleAudio.shotPitch(3) > 0.25);
		// Out of range powers are kept in range
		assertEquals(BattleAudio.shotPitch(3), BattleAudio.shotPitch(9), 1e-9);
	}
}
