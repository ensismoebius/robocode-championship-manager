package widgets.scenes;

import java.util.function.DoubleBinaryOperator;

import javafx.scene.image.Image;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The pictures of the desert arena: the mosaic of the courtyard, the "star and cross" of the zellij tile work, with its
 * eight-pointed stars (the khatam) nested in cobalt, turquoise and honey, and the crosses they leave between them in
 * emerald, set off by lines of plaster, whose lines glow as if made of light; the sand of the dunes, with the ripples of
 * the wind; the pearl white ceramic panels of the futuristic town, each with a star of the same geometry, and the pointed
 * windows that glow at dusk, outlined in light; the streets of the city, with their pavements and the dashes of light down the
 * middle, the paving of its blocks, and its gardens, the "four gardens" (charbagh) of the Islamic tradition, with their
 * channels of glowing water crossing at a pool; and the sky at sunset, with the sun on the horizon.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertTexture {

	/** The colours of the zellij: dark blue, emerald green, honey yellow, turquoise and the white of the plaster */
	private static final int TURQUOISE = 0x22b5ae;
	private static final int COBALT = 0x2450b0;
	private static final int HONEY = 0xe3ac47;
	private static final int CREAM = 0xf3e6c4;
	private static final int EMERALD = 0x23935c;
	/** The windows are cells of this many pixels, each with a pointed arch: half its width, the height where it bends, and the height of its sill over the bottom of the cell */
	private static final int CELL = 32;
	private static final double ARCH_HALF = 8;
	private static final double ARCH_SPRINGLINE = 12;
	private static final double SILL = 28;
	/** The ceramic of the town: pearl white panels with seams, the glass of the windows and the lights that outline them */
	private static final int PEARL = 0xe9eff1;
	private static final int PEARL_SHADE = 0xbccbd2;
	private static final int SEAM = 0x86aab6;
	private static final int TRIM = 0x7fe0ec;
	private static final int GLASS = 0x1c8c9c;
	private static final int DARK = 0x0b1a2a;
	private static final int LATTICE = 0x0f3a44;
	private static final int LIT = 0xffc860;
	private static final int CYAN = 0x5ff0ff;
	/** The glass of a lit floor of a skyscraper, and how much of their light shows: the skyscrapers are huge, so their lights are dimmed */
	private static final int TOWER_GLASS = 0x145a66;
	private static final double DIM = 0.65;
	private static final int OUTLINE = 0x2a9fb0;
	/** The lines of the mosaic shine in this colour, dimmed on the floor of the courtyard so the battle stays clear */
	private static final int SHINE = 0x58d8e8;
	/** The panels of ceramic are this many pixels wide, and the star on each one is as big as the picture */
	private static final int PANEL = 128;
	/** Where the sun is in the sky: its azimuth, and how high over the horizon */
	private static final double SUN_AZIMUTH = 2.3;
	private static final double SUN_HEIGHT = 0.09;

	/** The mosaic of one tile, the star and cross: the star touches the middle of each side of the tile, and the cross is what four tiles leave at their corner */
	static final Image MOSAIC = TextureMath.image(256, 256, DesertTexture::mosaicPixel);
	/** The lines of plaster of the mosaic, alone, to shine through the same tiles */
	static final Image MOSAIC_LIGHT = TextureMath.image(256, 256, (posX, posY) -> mosaicPixel(posX, posY) == CREAM ? SHINE : 0x000000);
	/** The same lines, dimmer: the light of the floor of the courtyard */
	static final Image FLOOR_LIGHT = TextureMath.image(256, 256, (posX, posY) -> mosaicPixel(posX, posY) == CREAM ? TextureMath.mix(0x000000, SHINE, 0.45) : 0x000000);
	/** The sand of the dunes, with the ripples of the wind */
	static final Image SAND = make((uCoord, vCoord) -> {
		double grain = LavaTexture.fbm(uCoord * 2, vCoord * 2, 4, 61);
		double ripple = Math.sin(vCoord * Math.PI * 4 + 2 * LavaTexture.fbm(uCoord, vCoord, 3, 62));
		int color = TextureMath.mix(0xb88a55, 0xe8c994, grain);
		return TextureMath.mix(color, 0x8a5e35, Math.max(0, ripple - 0.6) * 0.5);
	});
	/** The ceramic of the houses and of the courtyard: pearl white panels with their seams, and on each one the outline of a star of light blue */
	static final Image PANELS = TextureMath.image(256, 256, DesertTexture::panelPixel);
	/** The wall between the ledge and the frieze, with the pointed arches of the windows in it, trimmed in light */
	static final Image ARCHES = TextureMath.image(128, 128, (posX, posY) -> archPixel(posX, posY, false));
	/** The same windows: only the lit ones glow, through their lattices, and the trim of every arch */
	static final Image WINDOWS = TextureMath.image(128, 128, (posX, posY) -> archPixel(posX, posY, true));
	/** The glass skin of the skyscrapers: a cell of glass for each floor in its frame of ceramic, some of them lit */
	static final Image CURTAIN = TextureMath.image(128, 128, (posX, posY) -> curtainPixel(posX, posY, false));
	/** The same skin: only the lit floors glow, in cyan and gold */
	static final Image CURTAIN_LIGHT = TextureMath.image(128, 128, (posX, posY) -> curtainPixel(posX, posY, true));
	/** The skin of the lattice towers: diamonds of glass in a lattice of ceramic, as the mashrabiya of the old houses grown to a skyscraper */
	static final Image MASHRABIYA = TextureMath.image(128, 128, (posX, posY) -> diamondPixel(posX, posY, false));
	/** The same lattice: only the lit diamonds glow */
	static final Image MASHRABIYA_LIGHT = TextureMath.image(128, 128, (posX, posY) -> diamondPixel(posX, posY, true));
	/** A street, across its width: the pavements on both sides, the kerbs, the asphalt and the dashes down the middle, repeating along its length */
	static final Image ROAD = TextureMath.image(128, 128, (posX, posY) -> roadPixel(posX, posY, false));
	/** The same street: the dashes and the edge of the kerbs shine */
	static final Image ROAD_LIGHT = TextureMath.image(128, 128, (posX, posY) -> roadPixel(posX, posY, true));
	/** The crossing of two streets: asphalt */
	static final Image ASPHALT = TextureMath.image(64, 64, (posX, posY) -> asphaltPixel(posX, posY));
	/** The paving of a block of houses: stones of a pale sandstone in rows, with their joints */
	static final Image PAVING = TextureMath.image(256, 256, DesertTexture::pavingPixel);
	/** A pedestrian street: paving on both sides and a band of tile down the middle */
	static final Image PROMENADE = TextureMath.image(128, 128, DesertTexture::promenadePixel);
	/** A garden of a whole block, in the "four gardens" way: a path around it, two channels of water crossing at a pool, and in each of the four beds a star of a darker green */
	static final Image GARDEN = TextureMath.image(256, 256, (posX, posY) -> gardenPixel(posX, posY, false));
	/** The same garden: only the water shines */
	static final Image GARDEN_LIGHT = TextureMath.image(256, 256, (posX, posY) -> gardenPixel(posX, posY, true));
	/** The sky at sunset, seen from inside: the sun low over the dunes, a few stars above */
	static final Image SKY = TextureMath.image(768, 384, DesertTexture::skyPixel);

	/** The pictures are static: this class is not instantiated. */
	private DesertTexture() {
	}

	/** A seamless picture, from a function of the coordinates that repeats every period of the noise. */
	private static Image make(DoubleBinaryOperator color) {
		return TextureMath.image(256, 256, (posX, posY) -> (int) color.applyAsDouble((double) posX / 256 * LavaTexture.PERIOD, (double) posY / 256 * LavaTexture.PERIOD));
	}

	/**
	 * The signed distance, in tiles, from the outline of the khatam, the eight-pointed star, scaled by a share: positive inside
	 * it. The khatam is two squares laid over each other, one of them turned by 45 degrees, so its outline is made of straight
	 * lines. With a share of 1 its tips touch the middle of the sides of the tile.
	 */
	private static double khatam(double across, double down, double scale) {
		double reach = 0.5 * scale;
		return Math.max((reach - (Math.abs(across) + Math.abs(down))) / Math.sqrt(2), reach / Math.sqrt(2) - Math.max(Math.abs(across), Math.abs(down)));
	}

	/** One pixel of the mosaic: a star inside its outline of plaster, a cross outside it */
	static int mosaicPixel(int posX, int posY) {
		double across = (posX + 0.5) / 256 - 0.5;
		double down = (posY + 0.5) / 256 - 0.5;
		double edge = khatam(across, down, 1);
		if (Math.abs(edge) < 0.006) {
			return CREAM;
		}
		return edge > 0 ? starPixel(across, down) : crossPixel(across, down, edge);
	}

	/** Inside the star: smaller stars nested in it, of cobalt, turquoise and honey, each set off by a line of plaster */
	private static int starPixel(double across, double down) {
		double middle = khatam(across, down, 0.62);
		if (Math.abs(middle) < 0.005) {
			return CREAM;
		}
		if (middle <= 0) {
			return COBALT;
		}
		double heart = khatam(across, down, 0.3);
		if (Math.abs(heart) < 0.005) {
			return CREAM;
		}
		if (heart <= 0) {
			return TURQUOISE;
		}
		return khatam(across, down, 0.12) < 0 ? HONEY : COBALT;
	}

	/** Outside the star: the cross that four stars leave at their corner, in emerald with a wide border of plaster, and a honey diamond in the middle */
	private static int crossPixel(double across, double down, double edge) {
		if (edge > -0.05) {
			return CREAM;
		}
		double corner = 1 - Math.abs(across) - Math.abs(down);
		if (Math.abs(corner - 0.085) < 0.005) {
			return CREAM;
		}
		return corner < 0.085 ? HONEY : EMERALD;
	}

	/** One pixel of a panel of ceramic: its seams, the outline of its star, and the pearl with a little mottling */
	private static int panelPixel(int posX, int posY) {
		if (posX % PANEL < 2 || posY % PANEL < 2) {
			return SEAM;
		}
		double across = (posX + 0.5) / 256 - 0.5;
		double down = (posY + 0.5) / 256 - 0.5;
		if (Math.abs(khatam(across, down, 0.8)) < 0.007) {
			return TRIM;
		}
		return TextureMath.mix(PEARL, PEARL_SHADE, 0.45 * LavaTexture.fbm(posX / 256.0 * LavaTexture.PERIOD * 2, posY / 256.0 * LavaTexture.PERIOD * 2, 3, 71));
	}

	/**
	 * One pixel of the windows: the ceramic of the wall, the trim of the arch, and inside the arch the dark or the lit window
	 * with its lattice. When it glows, the lit windows shine in cyan or in gold and the trim of every arch is a thin line of light.
	 */
	private static int archPixel(int posX, int posY, boolean glow) {
		double across = Math.abs(posX % CELL + 0.5 - CELL / 2.0);
		double rise = SILL - (posY % CELL + 0.5);
		int cell = Math.floorMod((posX / CELL) * 73856093 ^ (posY / CELL) * 19349663, 1000);
		if (inArch(across, rise, 0)) {
			boolean lit = cell < 380;
			boolean lattice = (posX + posY) % 6 == 0 || Math.floorMod(posX - posY, 6) == 0;
			if (glow) {
				return lit && !lattice ? (cell % 3 == 0 ? LIT : CYAN) : 0x000000;
			}
			return lit ? (lattice ? LATTICE : GLASS) : DARK;
		}
		boolean trim = inArch(across, rise, 2);
		if (glow) {
			return trim ? OUTLINE : 0x000000;
		}
		if (trim) {
			return TRIM;
		}
		return TextureMath.mix(PEARL, PEARL_SHADE, LavaTexture.fbm(posX / 128.0 * LavaTexture.PERIOD * 2, posY / 128.0 * LavaTexture.PERIOD * 2, 3, 81));
	}

	/** Whether a cell of a picture is lit, from its place: about a quarter of them are, scattered */
	private static boolean lit(int column, int row) {
		return Math.floorMod(column * 73856093 ^ row * 19349663, 1000) < 240;
	}

	/** The colour a lit cell shines in, from its place: cyan, with a gold one now and then */
	private static int shine(int column, int row) {
		return Math.floorMod(column * 31 + row * 17, 3) == 0 ? LIT : CYAN;
	}

	/** One pixel of the glass skin: cells of 8 pixels, each with its frame of ceramic along the top and the left, and the glass inside */
	private static int curtainPixel(int posX, int posY, boolean glow) {
		boolean frame = posX % 8 < 1 || posY % 8 < 1;
		boolean glowing = !frame && lit(posX / 8, posY / 8);
		if (glow) {
			return glowing ? TextureMath.mix(0x000000, shine(posX / 8, posY / 8), DIM) : 0x000000;
		}
		if (frame) {
			return PEARL_SHADE;
		}
		return glowing ? TOWER_GLASS : TextureMath.mix(DARK, 0x1d6578, LavaTexture.fbm(posX / 128.0 * LavaTexture.PERIOD, posY / 128.0 * LavaTexture.PERIOD, 3, 91));
	}

	/** One pixel of the lattice: lines of ceramic along both diagonals, and a diamond of glass between them, some lit */
	private static int diamondPixel(int posX, int posY, boolean glow) {
		int sum = Math.floorMod(posX + posY, 32);
		int difference = Math.floorMod(posX - posY, 32);
		boolean line = sum < 2 || difference < 2;
		boolean glowing = !line && lit(Math.floorDiv(posX + posY, 32), Math.floorDiv(posX - posY, 32));
		if (glow) {
			return glowing && sum > 3 && difference > 3 ? TextureMath.mix(0x000000, shine(Math.floorDiv(posX + posY, 32), Math.floorDiv(posX - posY, 32)), DIM) : 0x000000;
		}
		if (line) {
			return PEARL;
		}
		return glowing ? TOWER_GLASS : DARK;
	}

	/** Whether a point of a window cell is inside its pointed arch, grown by a margin */
	private static boolean inArch(double across, double rise, double margin) {
		return rise > -margin && across < IslamicForms.archHalfWidth(rise - ARCH_SPRINGLINE, ARCH_HALF + margin);
	}

	/** One pixel of a street: 128 pixels across, from pavement to pavement */
	private static int roadPixel(int posX, int posY, boolean glow) {
		boolean kerb = posX >= 12 && posX < 15 || posX >= 113 && posX < 116;
		boolean dash = posX >= 62 && posX < 66 && posY % 64 < 36;
		if (glow) {
			return kerb ? TextureMath.mix(0x000000, CYAN, 0.5) : dash ? TextureMath.mix(0x000000, LIT, 0.55) : 0x000000;
		}
		if (posX < 12 || posX >= 116) {
			return posY % 32 < 1 || posX % 32 < 1 ? SEAM : PEARL_SHADE;
		}
		if (kerb) {
			return PEARL;
		}
		return dash ? PEARL : TextureMath.mix(0x26303b, 0x39454f, LavaTexture.fbm(posX / 128.0 * LavaTexture.PERIOD, posY / 128.0 * LavaTexture.PERIOD, 3, 101));
	}

	/** One pixel of the asphalt of a crossing */
	private static int asphaltPixel(int posX, int posY) {
		return TextureMath.mix(0x26303b, 0x39454f, LavaTexture.fbm(posX / 64.0 * LavaTexture.PERIOD, posY / 64.0 * LavaTexture.PERIOD, 3, 101));
	}

	/** One pixel of the paving: rows of stones 32 pixels long and 16 high, every other row shifted by half a stone, with a joint around each one */
	private static int pavingPixel(int posX, int posY) {
		int row = posY / 16;
		int along = (posX + (row % 2) * 16) % 32;
		if (posY % 16 < 1 || along < 1) {
			return 0xa99a7d;
		}
		int stone = Math.floorMod(((posX + (row % 2) * 16) / 32) * 73856093 ^ row * 19349663, 3);
		return TextureMath.mix(stone == 0 ? 0xdccfb4 : 0xd2c4a6, 0xc4b594, LavaTexture.fbm(posX / 256.0 * LavaTexture.PERIOD * 2, posY / 256.0 * LavaTexture.PERIOD * 2, 2, 111));
	}

	/**
	 * One pixel of a pedestrian street, 128 pixels across: stone paving on both sides, and down the middle a band of tile, between two
	 * lines of cobalt, with a chain of diamonds in turquoise and honey, 32 pixels long each.
	 */
	private static int promenadePixel(int posX, int posY) {
		if (posX < 38 || posX >= 90) {
			return pavingPixel(posX, posY);
		}
		int local = posX - 38;
		if (local < 3 || local >= 49) {
			return COBALT;
		}
		double across = (local - 26) / 23.0;
		double along = (posY % 32 - 16) / 16.0;
		double reach = Math.abs(across) + Math.abs(along);
		return reach < 0.35 ? HONEY : reach < 0.8 ? TURQUOISE : CREAM;
	}

	/**
	 * One pixel of a garden: the paved path around it, the cross of water with its pool in the middle, and in each of the four
	 * beds the outline of a khatam star with its flower bed inside, on a lawn.
	 */
	private static int gardenPixel(int posX, int posY, boolean glow) {
		double across = (posX + 0.5) / 256 - 0.5;
		double down = (posY + 0.5) / 256 - 0.5;
		boolean water = Math.abs(across) + Math.abs(down) < 0.13 && Math.max(Math.abs(across), Math.abs(down)) < 0.115 || Math.min(Math.abs(across), Math.abs(down)) < 0.018 && Math.max(Math.abs(across), Math.abs(down)) < 0.43;
		if (glow) {
			return water ? TextureMath.mix(0x000000, CYAN, 0.45) : 0x000000;
		}
		if (water) {
			return 0x2fa9c4;
		}
		boolean rim = Math.abs(across) + Math.abs(down) < 0.155 && Math.max(Math.abs(across), Math.abs(down)) < 0.14 || Math.min(Math.abs(across), Math.abs(down)) < 0.034 && Math.max(Math.abs(across), Math.abs(down)) < 0.43;
		if (rim || Math.max(Math.abs(across), Math.abs(down)) > 0.43) {
			return CREAM;
		}
		double bedX = (Math.abs(across) - 0.23) * 2.2;
		double bedY = (Math.abs(down) - 0.23) * 2.2;
		double star = khatam(bedX, bedY, 0.9);
		if (Math.abs(star) < 0.012) {
			return CREAM;
		}
		if (star > 0) {
			return khatam(bedX, bedY, 0.25) > 0 ? HONEY : 0x1f6b33;
		}
		return TextureMath.mix(0x4aa043, 0x35832f, LavaTexture.fbm(posX / 256.0 * LavaTexture.PERIOD * 2, posY / 256.0 * LavaTexture.PERIOD * 2, 3, 121));
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
