package widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Random;

import org.junit.jupiter.api.Test;

import widgets.Song;

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

	@Test
	void machinesSoundOnlyWhenTheyMove() {
		assertEquals(0, rms(BattleAudio.machines(0, 0, 2)), 1e-12);
		assertTrue(rms(BattleAudio.machines(40, 0, 3)) > 0.01, "tracks are silent");
		assertTrue(rms(BattleAudio.machines(0, 90, 3)) > 0.005, "turret is silent");
	}

	@Test
	void theSongIsKeptAndTheSlowMotionLeavesAHeartbeat() {
		BattleAudio audio = new BattleAudio();
		audio.setSong(Song.SYNTHWAVE);
		assertEquals(Song.SYNTHWAVE, audio.getSong());

		audio.setSlowMotion(true);
		double[] frame = new double[2];
		double energy = 0;
		for (int i = 0; i < 3 * Synth.RATE; i++) {
			audio.nextFrame(frame);
			energy += frame[0] * frame[0] + frame[1] * frame[1];
		}

		// Only the heartbeat is left of the soundtrack, but it is still there
		assertTrue(energy > 0);
	}

	@Test
	void theMixerIsStartedOnlyOnce() throws ReflectiveOperationException {
		BattleAudio audio = new BattleAudio();
		// Already running: a second start does not open another mixer thread
		Field running = BattleAudio.class.getDeclaredField("running");
		running.setAccessible(true);
		running.setBoolean(audio, true);

		audio.start();

		Field thread = BattleAudio.class.getDeclaredField("thread");
		thread.setAccessible(true);
		assertNull(thread.get(audio));
	}

	@Test
	void fasterTracksHumLouder() {
		// The drive hums louder the faster the tracks run
		assertTrue(rms(BattleAudio.machines(120, 0, 3)) > rms(BattleAudio.machines(20, 0, 3)));
	}

	@Test
	void soundsComeFromTheSideTheyHappenOn() {
		BattleAudio audio = new BattleAudio();
		// Looking to +z: the right hand is +x
		audio.setListener(0, 0, 0, 1);
		double[] right = audio.spatial(300, 0);
		double[] left = audio.spatial(-300, 0);
		double[] ahead = audio.spatial(0, 300);
		assertTrue(right[1] > 2 * right[0], "a sound on the right is not in the right ear");
		assertTrue(left[0] > 2 * left[1], "a sound on the left is not in the left ear");
		assertEquals(ahead[0], ahead[1], 1e-9);
		assertTrue(audio.spatial(900, 0)[1] < right[1], "a farther sound is not quieter");
	}

	@Test
	void theRightEarHearsTheRobotOnTheRight() {
		BattleAudio audio = new BattleAudio();
		audio.setListener(0, 0, 0, 1);
		audio.setMachine(1, 60, 0, 300, 0);
		// The music is in the middle of both ears: silent, the sides are what is tested
		audio.setMusicLevel(0);
		double[] frame = new double[2];
		double left = 0;
		double right = 0;
		for (int i = 0; i < 2 * Synth.RATE; i++) {
			audio.nextFrame(frame);
			left += frame[0] * frame[0];
			right += frame[1] * frame[1];
		}
		assertTrue(right > 4 * left, "left " + left + " right " + right);
	}

	@Test
	void tracksAreLowAndInternal() {
		// The difference between two samples grows with the treble: a dull sound has little of it
		double[] samples = BattleAudio.machines(60, 0, 3);
		double energy = 0;
		double change = 0;
		for (int i = 0; i < samples.length; i++) {
			energy += samples[i] * samples[i];
			if (i > 0) {
				change += (samples[i] - samples[i - 1]) * (samples[i] - samples[i - 1]);
			}
		}
		assertTrue(Math.sqrt(change / energy) < 0.1, "the tracks are too bright: " + Math.sqrt(change / energy));
	}
}
