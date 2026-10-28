package widgets.scenes;

import java.util.function.DoubleBinaryOperator;

import javafx.scene.image.Image;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The pictures of the desert arena: the mosaic of the courtyard, eight-pointed stars in turquoise and cobalt around a
 * gold medallion, as in the tile work of old palaces; the sand of the dunes, with the ripples of the wind; the
 * sandstone of the houses and the windows that glow at dusk; and the sky at sunset, with the sun on the horizon.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertTexture {

	private static final int GROUT = 0x3a2216;
	private static final int TURQUOISE = 0x1aa5a0;
	private static final int COBALT = 0x1d3f8f;
	private static final int OCHRE = 0xd9a441;
	private static final int CREAM = 0xf3e6c4;
	private static final int TERRACOTTA = 0xb5542b;
	/** The windows are cells of this many pixels, each with a pointed arch: half its width, the height where it bends, and the height of its sill over the bottom of the cell */
	private static final int CELL = 32;
	private static final double ARCH_HALF = 8;
	private static final double ARCH_SPRINGLINE = 12;
	private static final double SILL = 28;
	private static final int PLASTER = 0xe6d3a6;
	private static final int DARK = 0x1b120c;
	private static final int LATTICE = 0x5a2f10;
	private static final int LIT = 0xffb35a;
	/** Where the sun is in the sky: its azimuth, and how high over the horizon */
	private static final double SUN_AZIMUTH = 2.3;
	private static final double SUN_HEIGHT = 0.09;

	/** The mosaic of one tile: its eight-pointed star touches the middle of each side of the tile */
	static final Image MOSAIC = TextureMath.image(256, 256, DesertTexture::mosaicPixel);
	/** The sand of the dunes, with the ripples of the wind */
	static final Image SAND = make((uCoord, vCoord) -> {
		double grain = LavaTexture.fbm(uCoord * 2, vCoord * 2, 4, 61);
		double ripple = Math.sin(vCoord * Math.PI * 4 + 2 * LavaTexture.fbm(uCoord, vCoord, 3, 62));
		int color = TextureMath.mix(0xb88a55, 0xe8c994, grain);
		return TextureMath.mix(color, 0x8a5e35, Math.max(0, ripple - 0.6) * 0.5);
	});
	/** The sandstone of the houses and of the courtyard, with its layers */
	static final Image SANDSTONE = make((uCoord, vCoord) -> {
		double pits = LavaTexture.fbm(uCoord * 3, vCoord * 3, 4, 71);
		double layers = Math.sin(vCoord * Math.PI * 3);
		int color = TextureMath.mix(0xdcb884, 0xb08352, pits);
		return TextureMath.mix(color, 0x9a6d3f, Math.max(0, layers) * 0.12);
	});
	/** The wall between the ledge and the frieze, with the pointed arches of the windows dark in it */
	static final Image ARCHES = TextureMath.image(128, 128, (posX, posY) -> archPixel(posX, posY, false));
	/** The same windows: only the lit ones glow, through their lattices */
	static final Image WINDOWS = TextureMath.image(128, 128, (posX, posY) -> archPixel(posX, posY, true));
	/** The sky at sunset, seen from inside: the sun low over the dunes, a few stars above */
	static final Image SKY = TextureMath.image(768, 384, DesertTexture::skyPixel);

	/** The pictures are static: this class is not instantiated. */
	private DesertTexture() {
	}

	/** A seamless picture, from a function of the coordinates that repeats every period of the noise. */
	private static Image make(DoubleBinaryOperator color) {
		return TextureMath.image(256, 256, (posX, posY) -> (int) color.applyAsDouble((double) posX / 256 * LavaTexture.PERIOD, (double) posY / 256 * LavaTexture.PERIOD));
	}

	/** One pixel of the mosaic: the grout at the sides, a star of turquoise and cobalt with a gold medallion, cream and terracotta around */
	private static int mosaicPixel(int posX, int posY) {
		double x = (posX + 0.5) / 256 - 0.5;
		double y = (posY + 0.5) / 256 - 0.5;
		if (0.5 - Math.max(Math.abs(x), Math.abs(y)) < 0.012) {
			return GROUT;
		}
		double radius = Math.hypot(x, y);
		// Eight points: the star reaches the middle of each side of the tile, and goes in between them
		// Sharp points and deep notches between them, the eight-pointed star of the old tile work
		double star = 0.16 + 0.34 * Math.pow(1 - Math.abs(Math.sin(4 * Math.atan2(y, x))), 1.4);
		if (Math.abs(radius - star) < 0.01) {
			return CREAM;
		}
		if (radius >= star) {
			return Math.abs(x) + Math.abs(y) > 0.78 ? TERRACOTTA : CREAM;
		}
		if (radius < 0.1) {
			return radius < 0.035 ? TERRACOTTA : OCHRE;
		}
		return radius < 0.7 * star ? COBALT : TURQUOISE;
	}

	/** One pixel of the windows: the stone of the wall, the trim of the arch, and inside the arch the dark or the lit window with its lattice */
	private static int archPixel(int posX, int posY, boolean glow) {
		double across = Math.abs(posX % CELL + 0.5 - CELL / 2.0);
		double rise = SILL - (posY % CELL + 0.5);
		if (inArch(across, rise, 0)) {
			boolean lit = Math.floorMod((posX / CELL) * 73856093 ^ (posY / CELL) * 19349663, 1000) < 380;
			boolean lattice = (posX + posY) % 6 == 0 || Math.floorMod(posX - posY, 6) == 0;
			if (glow) {
				return lit && !lattice ? LIT : 0x000000;
			}
			return lit ? (lattice ? LATTICE : 0x8a4d1c) : DARK;
		}
		if (glow) {
			return 0x000000;
		}
		if (inArch(across, rise, 2)) {
			return PLASTER;
		}
		return TextureMath.mix(0xd2ad7c, 0xb98c5a, LavaTexture.fbm(posX / 128.0 * LavaTexture.PERIOD * 2, posY / 128.0 * LavaTexture.PERIOD * 2, 3, 81));
	}

	/** Whether a point of a window cell is inside its pointed arch, grown by a margin */
	private static boolean inArch(double across, double rise, double margin) {
		return rise > -margin && across < IslamicForms.archHalfWidth(rise - ARCH_SPRINGLINE, ARCH_HALF + margin);
	}

	/** One pixel of the sky: the sunset from the horizon up to the dark, the sun, the sand below the horizon and a few stars */
	private static int skyPixel(int posX, int posY) {
		double azimuth = (posX + 0.5) / 768 * Math.PI * 2;
		double polar = (posY + 0.5) / 384 * Math.PI;
		double upward = Math.cos(polar);
		if (upward < 0) {
			return TextureMath.mix(0xc08a52, 0x5e3f24, Math.min(1, -upward * 2.5));
		}
		double sunDeltaX = Math.atan2(Math.sin(azimuth - SUN_AZIMUTH), Math.cos(azimuth - SUN_AZIMUTH)) * Math.sin(polar);
		double sunDeltaY = polar - (Math.PI / 2 - SUN_HEIGHT);
		double sunDistance = Math.hypot(sunDeltaX, sunDeltaY);
		if (sunDistance < 0.06) {
			return 0xfff2c4;
		}
		int color = gradient(upward);
		double glow = Math.exp(-sunDistance * sunDistance * 40) * 0.9 + Math.exp(-sunDistance * 6) * 0.35;
		color = TextureMath.mix(color, 0xffd27a, Math.min(1, glow));
		if (upward > 0.5 && Math.floorMod(posX * 7919 + posY * 104729, 9973) < 9) {
			return TextureMath.mix(color, 0xf4efe0, 0.8);
		}
		return color;
	}

	/** The colors of the sky from the horizon, where it is orange, up to the zenith, where it is dark indigo. */
	private static int gradient(double upward) {
		if (upward < 0.12) {
			return TextureMath.mix(0xffa050, 0xe8607a, upward / 0.12);
		}
		if (upward < 0.45) {
			return TextureMath.mix(0xe8607a, 0x5a2d6e, (upward - 0.12) / 0.33);
		}
		return TextureMath.mix(0x5a2d6e, 0x14122e, (upward - 0.45) / 0.55);
	}
}
