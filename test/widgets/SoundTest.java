package widgets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

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
}
