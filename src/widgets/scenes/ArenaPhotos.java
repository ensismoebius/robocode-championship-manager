package widgets.scenes;

import static widgets.scenes.ArenaMaterials.argb;
import widgets.scenes.BattleArena3D.BattlePhoto;
import widgets.scenes.BattleArena3D.PendingPhoto;
import widgets.scenes.BattleArena3D.Phase;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The photos of the key moments of the battle: each one is taken at the end of
 * the frame, encoded as a small JPEG and kept for the championship.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaPhotos {

	/** The photos of the key moments are kept small, as JPEG */
	private static final int PHOTO_WIDTH = 640;
	private static final int PHOTO_HEIGHT = 360;

	private final BattleArena3D host;

	/** Creates the photographer of the arena, which takes the pictures of the moments. */
	ArenaPhotos(BattleArena3D host) {
		this.host = host;
	}

	/**
	 * Takes the photos that are due, at the end of the frame: the robots and the
	 * camera are then where they are drawn
	 */
	void flushPhotos(double deltaTime) {
		if (host.pendingPhotos.isEmpty()) {
			return;
		}
		List<PendingPhoto> waiting = new ArrayList<PendingPhoto>();
		for (PendingPhoto pending : host.pendingPhotos) {
			double remaining = pending.remaining() - deltaTime;
			if (remaining > 0) {
				waiting.add(new PendingPhoto(remaining, pending.round(), pending.destruction(), pending.subject(), pending.power()));
			} else if (host.phase != Phase.FINISHED && host.phase != Phase.LOADING) {
				this.takePhoto(pending);
			}
		}
		host.pendingPhotos.clear();
		host.pendingPhotos.addAll(waiting);
	}

	/** Takes a photo of the pending moment, when its subject is known. */
	void takePhoto(PendingPhoto pending) {
		if (pending.subject() == null) {
			return;
		}
		try {
			host.photos.add(new BattlePhoto(pending.round(), pending.destruction(), pending.subject(), pending.power(), encodePhoto(host.subScene.snapshot(null, null))));
		} catch (IOException | RuntimeException runtimeException) {
			// A photo is a bonus: the battle goes on without it
			runtimeException.printStackTrace();
		}
	}

	/**
	 * The picture as a small JPEG: each of its pixels is the average of the ones it
	 * covers
	 */
	static byte[] encodePhoto(WritableImage view) throws IOException {
		int width = (int) view.getWidth();
		int height = (int) view.getHeight();
		int[] pixels = new int[width * height];
		view.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
		int[] small = new int[PHOTO_WIDTH * PHOTO_HEIGHT];
		for (int posY = 0; posY < PHOTO_HEIGHT; posY++) {
			int top = posY * height / PHOTO_HEIGHT;
			int bottom = Math.max(top + 1, (posY + 1) * height / PHOTO_HEIGHT);
			for (int posX = 0; posX < PHOTO_WIDTH; posX++) {
				int left = posX * width / PHOTO_WIDTH;
				int right = Math.max(left + 1, (posX + 1) * width / PHOTO_WIDTH);
				int red = 0;
				int green = 0;
				int blue = 0;
				int count = 0;
				for (int syValue = top; syValue < bottom; syValue++) {
					for (int sxValue = left; sxValue < right; sxValue++) {
						int argb = pixels[syValue * width + sxValue];
						red += argb >> 16 & 0xff;
						green += argb >> 8 & 0xff;
						blue += argb & 0xff;
						count++;
					}
				}
				small[posY * PHOTO_WIDTH + posX] = (red / count) << 16 | (green / count) << 8 | blue / count;
			}
		}
		BufferedImage photo = new BufferedImage(PHOTO_WIDTH, PHOTO_HEIGHT, BufferedImage.TYPE_INT_RGB);
		photo.setRGB(0, 0, PHOTO_WIDTH, PHOTO_HEIGHT, small, 0, PHOTO_WIDTH);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (!ImageIO.write(photo, "jpg", out)) {
			throw new IOException("No JPEG writer");
		}
		return out.toByteArray();
	}
}
