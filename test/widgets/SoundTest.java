package widgets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class SoundTest {

	@Test
	void decodesMp3WithoutNativeCodecs() throws IOException {
		byte[] tone = SoundTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes();
		assertTrue(Sound.isPlayable(tone));
	}

	@Test
	void decodesTheBundledMusic() {
		// The start and versus screens music
		assertNotNull(Sound.fromResource(widgets.scenes.StartScreen.class, "versusMusic.mp3"));
		try (var is = widgets.scenes.StartScreen.class.getResourceAsStream("versusMusic.mp3")) {
			assertTrue(Sound.isPlayable(is.readAllBytes()));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	@Test
	void rejectsWhatIsNotAudio() {
		assertFalse(Sound.isPlayable("definitely not an mp3 file, just text".getBytes()));
		assertFalse(Sound.isPlayable(new byte[0]));
		assertFalse(Sound.isPlayable(null));
		assertNull(Sound.fromBytes(null, "x"));
	}

	@Test
	void aMissingResourceHasNoSound() {
		assertNull(Sound.fromResource(Sound.class, "nao-existe.mp3"));
	}

	@Test
	void aSoundThatCanNotBePlayedStopsQuietly() throws InterruptedException {
		Sound noise = Sound.fromBytes("isso não é áudio".getBytes(StandardCharsets.UTF_8), "ruido");

		noise.play();
		Thread.sleep(300);
		noise.stop();

		assertFalse(Sound.isPlayable("isso não é áudio".getBytes(StandardCharsets.UTF_8)));
	}
}
