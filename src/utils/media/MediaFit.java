package utils.media;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Makes the files of a team fit the limits of the registration instead of refusing them: an image larger than the limit is
 * scaled down, keeping its proportions, until it fits in the size in pixels and in bytes, and a song larger than the limit is
 * cut to its first seconds, at the end of a whole frame, until it fits in the bytes. What already fits, or is not a file of
 * the kind (it is for the registration to refuse it), is given back as it came.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class MediaFit {

	/** The qualities a picture is tried with, from the best, before it is made smaller. */
	private static final float[] QUALITIES = { 0.92f, 0.8f, 0.65f, 0.5f };
	/** How much smaller a picture is made each time it still does not fit, and the smallest side it is made to. */
	private static final double SHRINK = 0.85;
	private static final int SMALLEST = 16;

	/** MPEG audio, layer III: the bitrates (kbit/s) by the index of the header, for MPEG 1 and for MPEG 2 and 2.5. */
	private static final int[] BITRATES_V1 = { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, -1 };
	private static final int[] BITRATES_V2 = { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1 };
	private static final int[][] SAMPLE_RATES = { { 11025, 12000, 8000 }, { 0, 0, 0 }, { 22050, 24000, 16000 }, { 44100, 48000, 32000 } };

	/** A song as it was fit: its bytes, how many seconds it lasts and whether it was cut. */
	public record Music(byte[] data, double seconds, boolean cut) {
	}

	/** Only static methods: this class is not instantiated. */
	private MediaFit() {
	}

	/**
	 * Makes an image fit in the width, the height and the bytes given. A picture that fits, or that cannot be read, is given back as it is.
	 * A picture with transparency stays a PNG, any other becomes a JPEG, which is smaller.
	 */
	public static byte[] logo(byte[] image, int maxBytes, int maxWidth, int maxHeight) {
		BufferedImage picture;
		try {
			picture = ImageIO.read(new ByteArrayInputStream(image));
		} catch (IOException ioException) {
			return image;
		}
		if (picture == null || image.length == 0) {
			return image;
		}
		if (picture.getWidth() <= maxWidth && picture.getHeight() <= maxHeight && image.length <= maxBytes) {
			return image;
		}
		boolean transparent = picture.getColorModel().hasAlpha();
		double scale = Math.min(1, Math.min((double) maxWidth / picture.getWidth(), (double) maxHeight / picture.getHeight()));
		byte[] best = image;
		while (true) {
			int width = Math.max(1, (int) Math.round(picture.getWidth() * scale));
			int height = Math.max(1, (int) Math.round(picture.getHeight() * scale));
			BufferedImage scaled = scaled(picture, width, height, transparent);
			for (float quality : transparent ? new float[] { 1f } : QUALITIES) {
				best = encode(scaled, transparent, quality);
				if (best.length <= maxBytes) {
					return best;
				}
			}
			if (Math.min(width, height) <= SMALLEST) {
				return best;
			}
			scale *= SHRINK;
		}
	}

	/** The picture scaled to the size, smoothly, on white when it has no transparency. */
	private static BufferedImage scaled(BufferedImage picture, int width, int height, boolean transparent) {
		BufferedImage out = new BufferedImage(width, height, transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = out.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics.drawImage(picture.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null);
		graphics.dispose();
		return out;
	}

	/** The picture as a PNG, or as a JPEG of the quality. */
	private static byte[] encode(BufferedImage picture, boolean png, float quality) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try {
			if (png) {
				ImageIO.write(picture, "png", bytes);
			} else {
				Iterator<ImageWriter> writers = ImageIO.getImageWriters(ImageTypeSpecifier.createFromRenderedImage(picture), "jpeg");
				ImageWriter writer = writers.next();
				ImageWriteParam parameters = writer.getDefaultWriteParam();
				parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				parameters.setCompressionQuality(quality);
				try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
					writer.setOutput(output);
					writer.write(null, new IIOImage(picture, null, null), parameters);
				} finally {
					writer.dispose();
				}
			}
		} catch (IOException ioException) {
			throw new UncheckedIOException(ioException);
		}
		return bytes.toByteArray();
	}

	/**
	 * Makes an mp3 fit in the bytes given, by keeping its first frames, whole ones. The tags at the start (which may carry a cover
	 * picture) and the header of the length of a variable bitrate file are left out, since they would no longer be true. A song that
	 * fits, or that has no frames of layer III to cut, is given back as it is.
	 */
	public static Music music(byte[] song, int maxBytes) {
		Scan scan = scan(song, maxBytes);
		if (scan.firstStart() < 0) {
			return new Music(song, 0, false);
		}
		if (!scan.cut() && song.length <= maxBytes) {
			return new Music(song, scan.seconds(), false);
		}
		return new Music(Arrays.copyOfRange(song, scan.firstStart(), scan.end()), scan.seconds(), scan.cut());
	}

	/** What was found keeping the frames that fit: where the first one begins (-1 if there is none), where the last one kept ends, their seconds and whether more were left out. */
	private record Scan(int firstStart, int end, double seconds, boolean cut) {
	}

	/** Walks the frames from the start of the audio, keeping them while they fit in the bytes given. */
	private static Scan scan(byte[] song, int maxBytes) {
		int position = skipTag(song);
		int end = position;
		double seconds = 0;
		boolean first = true;
		int firstStart = -1;
		while (position + 4 <= song.length) {
			int[] frame = frameAt(song, position);
			if (frame == null) {
				position++;
				continue;
			}
			int length = frame[0];
			if (position + length > song.length) {
				break;
			}
			boolean header = first && isLengthHeader(song, position, length);
			first = false;
			if (header) {
				position += length;
				continue;
			}
			if (firstStart < 0) {
				firstStart = position;
			}
			if (position + length - firstStart > maxBytes) {
				return new Scan(firstStart, end, seconds, true);
			}
			seconds += frame[1] / (double) frame[2];
			position += length;
			end = position;
		}
		return new Scan(firstStart, end, seconds, false);
	}

	/** Where the audio begins: after the ID3v2 tag, if there is one. */
	private static int skipTag(byte[] song) {
		if (song.length >= 10 && song[0] == 'I' && song[1] == 'D' && song[2] == '3') {
			int size = (song[6] & 0x7f) << 21 | (song[7] & 0x7f) << 14 | (song[8] & 0x7f) << 7 | (song[9] & 0x7f);
			boolean footer = (song[5] & 0x10) != 0;
			return Math.min(song.length, 10 + size + (footer ? 10 : 0));
		}
		return 0;
	}

	/**
	 * The frame of layer III that begins at the position: its length in bytes, its samples and its sample rate, or null when there is no
	 * valid header there.
	 */
	private static int[] frameAt(byte[] song, int position) {
		int second = song[position + 1] & 0xff;
		int third = song[position + 2] & 0xff;
		if ((song[position] & 0xff) != 0xff || (second & 0xe0) != 0xe0) {
			return null;
		}
		int version = (second >> 3) & 3;
		int layer = (second >> 1) & 3;
		int bitrateIndex = third >> 4;
		int rateIndex = (third >> 2) & 3;
		if (version == 1 || layer != 1 || bitrateIndex == 0 || bitrateIndex == 15 || rateIndex == 3) {
			return null;
		}
		int bitrate = (version == 3 ? BITRATES_V1 : BITRATES_V2)[bitrateIndex] * 1000;
		int rate = SAMPLE_RATES[version][rateIndex];
		int samples = version == 3 ? 1152 : 576;
		int length = samples / 8 * bitrate / rate + ((third >> 1) & 1);
		return new int[] { length, samples, rate };
	}

	/** Whether the frame is the one that only tells the length of a variable bitrate song (Xing, Info or VBRI), with no sound of its own. */
	private static boolean isLengthHeader(byte[] song, int position, int length) {
		String head = new String(song, position, Math.min(length, 64), java.nio.charset.StandardCharsets.ISO_8859_1);
		return head.contains("Xing") || head.contains("Info") || head.contains("VBRI");
	}
}
