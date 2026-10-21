package widgets.scenes;

import javafx.scene.image.Image;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Seamless textures of the glacier, from noise: blocks of ice with frosted veins,
 * frost crystals and dark cracks. They are only made when the arena is used.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class IceTexture {

	private static final int SIZE = 256;

	private IceTexture() {
	}

	/**
	 * A block of ice of the color base: deeper in the body, frosted in the veins,
	 * with dark cracks running through it
	 */
	static Image block(int base, int seed) {
		int[] pixels = new int[SIZE * SIZE];
		for (int posY = 0; posY < SIZE; posY++) {
			for (int posX = 0; posX < SIZE; posX++) {
				double uCoord = (double) posX / SIZE * LavaTexture.PERIOD;
				double vCoord = (double) posY / SIZE * LavaTexture.PERIOD;
				double body = LavaTexture.fbm(uCoord, vCoord, 4, seed);
				double vein = 1 - Math.abs(2 * LavaTexture.fbm(uCoord + 3, vCoord + 8, 3, seed + 1) - 1);
				double frost = Math.max(0, LavaTexture.fbm(uCoord * 3, vCoord * 3, 3, seed + 2) - 0.66) * 3;
				double crack = Math.pow(vein, 18);
				int color = TextureMath.mix(0x1d3a55, base, body * 1.4 - 0.2);
				color = TextureMath.mix(color, 0xeaf8ff, Math.min(1, frost));
				color = TextureMath.mix(color, 0x0f2438, crack * 0.9);
				pixels[posY * SIZE + posX] = 0xff000000 | color;
			}
		}
		return JungleTexture.image(pixels);
	}
}
