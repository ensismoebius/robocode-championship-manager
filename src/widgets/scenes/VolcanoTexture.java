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
	/**
	 * The rock of the highlands around the lake of lava, across it (u) and up it (v, see {@link #rockV}): dark basalt in layers, its
	 * foot warmed by the lava, ash on the heights
	 */
	static final Image ROCK = TextureMath.image(512, 512, (posX, posY) -> rockPixel(posX, posY, false));
	/** The light of the rock itself: the glow of the lava on its foot and thin veins of fire low on the cliffs */
	static final Image ROCK_GLOW = TextureMath.image(512, 512, (posX, posY) -> rockPixel(posX, posY, true));
	/** The inside of a cave, from its mouth (u = 0) to its back (u = 1): rock that darkens into the deep */
	static final Image CAVE = TextureMath.image(128, 32, (posX, posY) -> cavePixel(posX, posY, false));
	/** The light inside a cave: the faint blue green of the fungi that grow in the dark, more the deeper */
	static final Image CAVE_GLOW = TextureMath.image(128, 32, (posX, posY) -> cavePixel(posX, posY, true));
	/** The highest the rock goes, for its texture */
	private static final double ROCK_TOP = 3600;

	/** The pictures are static: this class is not instantiated. */
	private VolcanoTexture() {
	}

	/** The smooth step from 0 at the first edge to 1 at the second. */
	private static double smooth(double low, double high, double value) {
		double time = Math.max(0, Math.min(1, (value - low) / (high - low)));
		return time * time * (3 - 2 * time);
	}

	/**
	 * Where a height (positive up, from the lava) is on the rock texture: 1 at the lava, 0 at the highest. The low part of the rock,
	 * the cliffs that are seen close, takes most of the texture.
	 */
	static double rockV(double height) {
		return 1 - Math.sqrt(Math.max(0, Math.min(1, height / ROCK_TOP)));
	}

	/** One pixel of the rock, or of its own light. */
	private static int rockPixel(int posX, int posY, boolean glow) {
		double across = (posX + 0.5) / 512 * LavaTexture.PERIOD;
		double up = (posY + 0.5) / 512;
		if (glow) {
			// The foot of the cliffs glows with the lava beside it; a few thin veins of fire run low on them
			int light = TextureMath.mix(0x000000, 0x3c1004, Math.pow(smooth(0.9, 1.0, up), 2));
			// Only here and there, where a crack reaches the lava
			double vein = 1 - Math.abs(LavaTexture.fbm(across * 1.5, up * 9, 4, 207) - 0.5) * 60;
			double where = smooth(0.62, 0.72, LavaTexture.fbm(across * 0.7, up * 3, 3, 208));
			return TextureMath.mix(light, 0x8a2a08, Math.max(0, vein) * where * smooth(0.7, 0.9, up) * 0.6);
		}
		double warp = LavaTexture.fbm(across, up * 6, 3, 201);
		double layers = smooth(0.48, 0.66, LavaTexture.fbm(across * 0.5 + warp * 0.8, up * 36, 4, 202));
		int color = TextureMath.mix(0x231c1a, 0x3a302a, LavaTexture.fbm(across * 2, up * 12, 4, 203));
		color = TextureMath.mix(color, 0x17110f, layers * 0.65);
		// Ash on the heights, the warm light of the lava on the foot
		color = TextureMath.mix(color, 0x5e5650, smooth(0.5, 0.12, up) * (0.3 + 0.5 * LavaTexture.fbm(across * 3, up * 18, 3, 204)));
		return TextureMath.mix(color, 0x5a2614, smooth(0.78, 0.98, up) * 0.5);
	}

	/** One pixel of the inside of a cave, or of its own light. */
	private static int cavePixel(int posX, int posY, boolean glow) {
		double deep = (posX + 0.5) / 128;
		double speck = TextureMath.lattice(posX, posY, 128, 32, 211);
		if (glow) {
			int light = TextureMath.mix(0x000000, 0x0b302a, smooth(0.3, 1, deep));
			return speck > 0.965 ? TextureMath.mix(light, 0x34c8a8, smooth(0.25, 0.9, deep)) : light;
		}
		int color = TextureMath.mix(0x30241f, 0x060404, smooth(0, 0.85, deep));
		return TextureMath.mix(color, 0x000000, speck * 0.35);
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
