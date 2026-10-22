package widgets.scenes;

import java.util.Random;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.TriangleMesh;

/**
 * The pictures of the fire: the sprite the particles are drawn on, and the textures of the flames, the smoke and the soft glow
 */
final class FireTextures {

	/** A quad of size 1 facing -Z, shared by all the sprites */
	static final TriangleMesh SPRITE = sprite();

	static final Image FLAME_TEXTURE = flameTexture();

	static final Image SMOKE_TEXTURE = smokeTexture();

	static final Image SOFT_TEXTURE = softTexture();

	/**
	 * A soft white dot, for embers and snow
	 */
	static Image softTexture() {
		int size = 32;
		WritableImage image = new WritableImage(size, size);
		for (int posY = 0; posY < size; posY++) {
			for (int posX = 0; posX < size; posX++) {
				double radius = Math.hypot((posX + 0.5) / size * 2 - 1, (posY + 0.5) / size * 2 - 1);
				double alpha = Math.max(0, 1 - radius);
				image.getPixelWriter().setColor(posX, posY, Color.color(1, 1, 1, Math.min(1, alpha * alpha * 1.6)));
			}
		}
		return image;
	}

	/** A square sprite, two triangles, with its texture coordinates. */
	static TriangleMesh sprite() {
		TriangleMesh mesh = new TriangleMesh();
		mesh.getPoints().addAll(-0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f, 0);
		mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
		mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
		return mesh;
	}

	/**
	 * A tongue of fire: white hot at the base, round at the bottom and pointed at
	 * the top, soft borders
	 */
	static Image flameTexture() {
		int size = 64;
		WritableImage image = new WritableImage(size, size);
		for (int posY = 0; posY < size; posY++) {
			for (int posX = 0; posX < size; posX++) {
				double uCoord = (posX + 0.5) / size * 2 - 1;
				// 1 at the top, -1 at the bottom
				double vCoord = 1 - (posY + 0.5) / size * 2;
				// Narrower going up
				double width = 0.62 * Math.pow(Math.max(0, (1 - vCoord) / 2), 0.55) + 0.04;
				double center = -0.25;
				double deltaX = uCoord / width;
				double deltaY = vCoord < center ? (vCoord - center) / 0.75 : (vCoord - center) / 1.2;
				double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
				double alpha = Math.max(0, 1 - distance);
				alpha = alpha * alpha * (3 - 2 * alpha);
				double heat = Math.max(0, 1 - distance * 1.4);
				image.getPixelWriter().setColor(posX, posY, Color.color(1, 0.75 + 0.25 * heat, 0.45 + 0.55 * heat, Math.min(1, alpha)));
			}
		}
		return image;
	}

	/**
	 * A puff: soft round blob with some noise, so it does not look like a ball
	 */
	static Image smokeTexture() {
		int size = 64;
		WritableImage image = new WritableImage(size, size);
		Random random = new Random(7);
		double[] phase = new double[6];
		for (int index = 0; index < phase.length; index++) {
			phase[index] = random.nextDouble() * Math.PI * 2;
		}
		for (int posY = 0; posY < size; posY++) {
			for (int posX = 0; posX < size; posX++) {
				double uCoord = (posX + 0.5) / size * 2 - 1;
				double vCoord = (posY + 0.5) / size * 2 - 1;
				double angle = Math.atan2(vCoord, uCoord);
				double radius = Math.hypot(uCoord, vCoord);
				// Bumpy border
				double border = 0.78 + 0.08 * Math.sin(3 * angle + phase[0]) + 0.06 * Math.sin(5 * angle + phase[1]) + 0.04 * Math.sin(9 * angle + phase[2]);
				double alpha = Math.max(0, 1 - radius / border);
				alpha = Math.pow(alpha, 0.8);
				// Darker and lighter spots inside
				double grain = 0.85 + 0.15 * Math.sin(uCoord * 7 + phase[3]) * Math.sin(vCoord * 6 + phase[4]) + 0.08 * Math.sin((uCoord + vCoord) * 11 + phase[5]);
				double gray = Math.max(0, Math.min(1, grain));
				image.getPixelWriter().setColor(posX, posY, Color.color(gray, gray, gray, Math.min(1, alpha * (0.75 + 0.25 * grain))));
			}
		}
		return image;
	}
}
