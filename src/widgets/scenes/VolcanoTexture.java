package widgets.scenes;

import javafx.scene.image.Image;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The sky of the volcano arena: a night thick with ash, where clouds of smoke roll low and are lit from below by the glow of the
 * lava, red and orange toward the horizon and dark toward the top. The embers in the air are not painted here, where they would be
 * blurred: they are specks of light in the arena (see {@link VolcanoArena}).
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class VolcanoTexture {

	/** The sky, around the whole arena, from the top (the first row) to below the horizon (the last) */
	static final Image SKY = TextureMath.image(768, 384, VolcanoTexture::skyPixel);

	/** The pictures are static: this class is not instantiated. */
	private VolcanoTexture() {
	}

	/** The smooth step from 0 at the first edge to 1 at the second. */
	private static double smooth(double low, double high, double value) {
		double time = Math.max(0, Math.min(1, (value - low) / (high - low)));
		return time * time * (3 - 2 * time);
	}

	/**
	 * One pixel of the sky. The smoke is read on a flat layer above the arena, as the clouds of the Eden are, so it keeps its shape
	 * over the head; the lower it is seen, toward the horizon, the more the lava lights it from below.
	 */
	private static int skyPixel(int posX, int posY) {
		double azimuth = (posX + 0.5) / 768 * Math.PI * 2;
		double polar = (posY + 0.5) / 384 * Math.PI;
		// Below the horizon, where the sea of lava ends at the edge of the sky, the glow of the horizon goes on: no dark band between them
		double upward = Math.max(0, Math.cos(polar));
		double[] direction = { Math.sin(polar) * Math.cos(azimuth), upward, Math.sin(polar) * Math.sin(azimuth) };
		double layer = Math.max(upward, 0.28);
		double smokeX = direction[0] / layer * 0.9 + 20;
		double smokeZ = direction[2] / layer * 0.9 + 20;
		double warpU = LavaTexture.fbm(smokeX + 5.3, smokeZ + 1.7, 3, 141);
		double warpV = LavaTexture.fbm(smokeX + 9.1, smokeZ + 4.4, 3, 142);
		double density = LavaTexture.fbm(smokeX + 2.2 * warpU, smokeZ + 2.2 * warpV, 5, 143);
		// Near the horizon the smoke melts into the glowing haze, so it is not drawn out into streaks
		double cover = smooth(0.34, 0.62, density) * smooth(0.18, 0.42, upward);
		double thick = smooth(0.5, 0.8, density);
		// How much the lava below lights the air: a lot at the horizon, nothing at the top
		double glow = Math.pow(smooth(0.95, 0.0, upward), 1.3);

		// The air: a dark maroon at the top, a red glow toward the horizon
		int color = TextureMath.mix(0x120708, 0x6a1a08, glow);
		color = TextureMath.mix(color, 0xc04a12, glow * glow * glow * 0.75);
		// The smoke: dark ash, its underside lit orange by the lava, more the lower it is
		int smoke = TextureMath.mix(0x3a2722, 0x1a1110, thick);
		smoke = TextureMath.mix(smoke, TextureMath.mix(0x8a2e0c, 0xe0661a, glow), (0.15 + 0.85 * glow) * (1 - thick * 0.55) * 0.85);
		return TextureMath.mix(color, smoke, cover);
	}
}
