package utils.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The files of a team that are larger than the limit: a picture is made smaller and a song is cut to its first seconds.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
class MediaFitTest {

	private static final int MEGABYTE = 1024 * 1024;

	/** A picture of random colors: it does not compress, so it is large. */
	private static byte[] noise(int width, int height, int type, String format) throws IOException {
		Random random = new Random(7);
		BufferedImage picture = new BufferedImage(width, height, type);
		for (int posY = 0; posY < height; posY++) {
			for (int posX = 0; posX < width; posX++) {
				picture.setRGB(posX, posY, random.nextInt());
			}
		}
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(picture, format, bytes);
		return bytes.toByteArray();
	}

	private static BufferedImage read(byte[] image) throws IOException {
		return ImageIO.read(new ByteArrayInputStream(image));
	}

	private static byte[] tone() throws IOException {
		return MediaFitTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes();
	}

	/** The tone played again and again, until it has the bytes given. */
	private static byte[] longSong(int copies) throws IOException {
		ByteArrayOutputStream song = new ByteArrayOutputStream();
		byte[] tone = tone();
		for (int copy = 0; copy < copies; copy++) {
			song.write(tone);
		}
		return song.toByteArray();
	}

	@Test
	void aPictureThatFitsIsNotTouched() throws IOException {
		byte[] small = noise(40, 30, BufferedImage.TYPE_INT_RGB, "png");

		assertSame(small, MediaFit.logo(small, MEGABYTE, 640, 480));
	}

	@Test
	void aPictureLargerInPixelsIsScaledKeepingItsProportions() throws IOException {
		byte[] wide = noise(1280, 480, BufferedImage.TYPE_INT_RGB, "png");

		BufferedImage fitted = read(MediaFit.logo(wide, 10 * MEGABYTE, 640, 480));

		assertEquals(640, fitted.getWidth());
		assertEquals(240, fitted.getHeight());
	}

	@Test
	void aPictureLargerInBytesIsMadeSmallerUntilItFits() throws IOException {
		byte[] heavy = noise(900, 700, BufferedImage.TYPE_INT_RGB, "png");
		assertTrue(heavy.length > MEGABYTE);

		byte[] fitted = MediaFit.logo(heavy, MEGABYTE, 640, 480);

		assertTrue(fitted.length <= MEGABYTE, "has " + fitted.length);
		BufferedImage picture = read(fitted);
		assertNotNull(picture);
		assertTrue(picture.getWidth() <= 640 && picture.getHeight() <= 480);
		assertEquals(900 / 700.0, picture.getWidth() / (double) picture.getHeight(), 0.05);
	}

	@Test
	void aPictureWithTransparencyStaysAPng() throws IOException {
		byte[] ghost = noise(900, 700, BufferedImage.TYPE_INT_ARGB, "png");

		byte[] fitted = MediaFit.logo(ghost, MEGABYTE, 640, 480);

		assertTrue(fitted.length <= MEGABYTE);
		assertTrue(read(fitted).getColorModel().hasAlpha());
		assertEquals(0x89, fitted[0] & 0xff);
	}

	@Test
	void whatIsNotAPictureIsGivenBackAsItCame() {
		byte[] garbage = new byte[MEGABYTE + 1];

		assertSame(garbage, MediaFit.logo(garbage, MEGABYTE, 640, 480));
	}

	@Test
	void aSongThatFitsIsNotTouched() throws IOException {
		byte[] tone = tone();

		MediaFit.Music music = MediaFit.music(tone, MEGABYTE);

		assertSame(tone, music.data());
		assertFalse(music.cut());
		assertTrue(music.seconds() > 0);
	}

	@Test
	void aSongLargerThanTheLimitKeepsItsFirstSecondsInWholeFrames() throws IOException {
		byte[] song = longSong(30);
		int limit = song.length / 3;

		MediaFit.Music music = MediaFit.music(song, limit);

		assertTrue(music.cut());
		assertTrue(music.data().length <= limit, "has " + music.data().length);
		assertTrue(music.data().length > limit - 2000, "keeps as much as fits: " + music.data().length);
		assertTrue(music.seconds() > 0);
		// What is kept is the start of the song, and it still has the frames of a song
		MediaFit.Music again = MediaFit.music(music.data(), MEGABYTE);
		assertFalse(again.cut());
		assertEquals(music.seconds(), again.seconds(), 1e-9);
		assertEquals(music.data().length, again.data().length);
	}

	@Test
	void theSecondsKeptGrowWithTheLimit() throws IOException {
		byte[] song = longSong(30);

		MediaFit.Music shorter = MediaFit.music(song, song.length / 6);
		MediaFit.Music longer = MediaFit.music(song, song.length / 3);

		assertTrue(longer.seconds() > shorter.seconds());
		assertEquals(2.0, longer.seconds() / shorter.seconds(), 0.2);
	}

	@Test
	void theTagAtTheStartIsLeftOutOfTheCutSong() throws IOException {
		byte[] song = longSong(10);
		byte[] tagged = new byte[song.length + 10 + 100];
		tagged[0] = 'I';
		tagged[1] = 'D';
		tagged[2] = '3';
		tagged[3] = 3;
		// The size of the tag is 100 bytes, in four syncsafe bytes
		tagged[9] = 100;
		System.arraycopy(song, 0, tagged, 110, song.length);

		MediaFit.Music music = MediaFit.music(tagged, song.length / 2);

		assertTrue(music.cut());
		assertTrue(music.data().length <= song.length / 2);
		assertEquals(0xff, music.data()[0] & 0xff);
	}

	@Test
	void whatIsNotASongIsGivenBackAsItCame() {
		byte[] garbage = "isso não é áudio".repeat(100).getBytes(java.nio.charset.StandardCharsets.UTF_8);

		MediaFit.Music music = MediaFit.music(garbage, 100);

		assertSame(garbage, music.data());
		assertFalse(music.cut());
	}
}
