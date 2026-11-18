package widgets.scenes;

import javafx.scene.image.Image;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The pictures of the city around the Eden Solar stadium: the bold painted geometry of the Ndebele houses, the mud walls
 * of the Sahel with their lit slits, the woven bands of kente, solar panels and green walls. Each picture that glows has
 * a twin that is the light it gives.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenCityTexture {

	private static final int BLACK = 0x14110f;
	private static final int CREAM = 0xeadbbd;
	private static final int RED = 0xd23b3b;
	private static final int YELLOW = 0xf2c14e;
	private static final int TEAL = 0x1c9a8d;
	private static final int[] KENTE_COLORS = { 0xe8b630, 0xc23b22, 0x2f8f4a, 0x15120e };

	/** The painted diamonds of the Ndebele walls, and the light of their colors. */
	static final Image NDEBELE = TextureMath.image(128, 128, (posX, posY) -> ndebele(posX, posY, false));
	static final Image NDEBELE_LIGHT = TextureMath.image(128, 128, (posX, posY) -> ndebele(posX, posY, true));
	/** A wall of mud with slits of warm light. */
	static final Image ADOBE = TextureMath.image(128, 128, (posX, posY) -> adobe(posX, posY, false));
	static final Image ADOBE_LIGHT = TextureMath.image(128, 128, (posX, posY) -> adobe(posX, posY, true));
	/** The woven bands of kente, for the avenues and the plazas. */
	static final Image KENTE = TextureMath.image(128, 64, (posX, posY) -> kente(posX, posY, false));
	static final Image KENTE_LIGHT = TextureMath.image(128, 64, (posX, posY) -> kente(posX, posY, true));
	/** Solar panels: blue cells with silver lines, and the cyan the lines glow with. */
	static final Image SOLAR = TextureMath.image(64, 64, (posX, posY) -> solar(posX, posY, false));
	static final Image SOLAR_LIGHT = TextureMath.image(64, 64, (posX, posY) -> solar(posX, posY, true));
	/** The glass skin of the tall towers: panes of teal glass between mullions, a floor every few rows, some rooms lit; and the light of them. */
	static final Image CURTAIN = TextureMath.image(64, 120, (posX, posY) -> curtain(posX, posY, false));
	static final Image CURTAIN_LIGHT = TextureMath.image(64, 120, (posX, posY) -> curtain(posX, posY, true));
	/** A green wall of leaves with flowers. */
	static final Image LEAVES = TextureMath.image(128, 128, EdenCityTexture::leaves);
	/** The water of the canal: deep teal that shines a little, and the light it gives. */
	static final Image WATER = TextureMath.image(64, 64, EdenCityTexture::water);
	static final Image WATER_LIGHT = TextureMath.image(64, 64, (posX, posY) -> TextureMath.mix(0x000000, water(posX, posY), 0.4));
	/** The ground of the plain: grass, fields of crops and plots of red earth, and the light that keeps it from being dark. */
	static final Image GROUND = TextureMath.image(256, 256, EdenCityTexture::ground);
	static final Image GROUND_LIGHT = TextureMath.image(256, 256, (posX, posY) -> TextureMath.mix(0x000000, ground(posX, posY), 0.22));

	/** The textures are static: this class is not instantiated. */
	private EdenCityTexture() {
	}

	/** A pixel of the Ndebele diamonds: rings of red, yellow and teal outlined in black, on cream; the light is the colors at half. */
	private static int ndebele(int posX, int posY, boolean light) {
		int across = posX % 32;
		int down = posY % 32;
		int distance = Math.abs(across - 16) + Math.abs(down - 16);
		int color;
		if (down < 2 || down >= 30 || (distance >= 4 && distance < 6) || (distance >= 11 && distance < 13) || (distance >= 19 && distance < 21)) {
			color = BLACK;
		} else if (distance < 4) {
			color = RED;
		} else if (distance < 11) {
			color = YELLOW;
		} else if (distance < 19) {
			color = TEAL;
		} else {
			color = CREAM;
		}
		if (!light) {
			return color;
		}
		return color == BLACK || color == CREAM ? 0x000000 : TextureMath.mix(0x000000, color, 0.55);
	}

	/** A pixel of the mud wall: ochre with courses and grain, and slits of lit window in columns. */
	private static int adobe(int posX, int posY, boolean light) {
		boolean slit = posX % 32 >= 13 && posX % 32 < 19 && posY % 64 >= 14 && posY % 64 < 46;
		if (light) {
			return slit ? 0xffc46b : 0x000000;
		}
		if (slit) {
			return 0x3a2414;
		}
		double grain = TextureMath.lattice(posX / 2, posY / 2, 64, 64, 31);
		int base = TextureMath.mix(0xb9683d, 0xd9a441, grain * 0.7);
		return posY % 16 == 0 ? TextureMath.mix(base, 0x5a3820, 0.5) : base;
	}

	/** A pixel of kente: bands of gold, red, green and black in blocks, some of them checkered. */
	private static int kente(int posX, int posY, boolean light) {
		int band = posY / 8;
		int block = posX / 16;
		int index = (band + block) % 4;
		if ((band + block) % 2 == 0 && (posX / 4 + posY / 4) % 2 == 0) {
			index = (index + 1) % 4;
		}
		int color = KENTE_COLORS[index];
		return light ? TextureMath.mix(0x000000, color, 0.3) : color;
	}

	/** A pixel of a solar panel: dark blue cells divided by thin silver lines. */
	private static int solar(int posX, int posY, boolean light) {
		boolean line = posX % 16 == 0 || posY % 16 == 0;
		if (light) {
			return line ? TextureMath.mix(0x000000, 0x2fdcd0, 0.45) : 0x000000;
		}
		return line ? 0x9fb6c8 : TextureMath.mix(0x0a2240, 0x1a4a78, posY / 64.0);
	}

	/** A pixel of the glass skin: mullions every 16 pixels, a floor every 12, and the room behind each pane lit or not. */
	private static int curtain(int posX, int posY, boolean light) {
		boolean mullion = posX % 16 < 2 || posY % 12 < 2;
		double room = TextureMath.lattice(posX / 16, posY / 12, 4, 10, 17);
		boolean lit = room > 0.55;
		if (light) {
			if (mullion) {
				return posY % 12 < 2 ? TextureMath.mix(0x000000, 0x3be0d0, 0.35) : 0x000000;
			}
			return lit ? TextureMath.mix(0x000000, room > 0.8 ? 0xffe2b0 : 0x9fe8ff, 0.75) : TextureMath.mix(0x000000, 0x123a44, 0.5);
		}
		if (mullion) {
			return 0x1b2a30;
		}
		return lit ? TextureMath.mix(0x7fd0d8, 0xf2e2b8, room) : TextureMath.mix(0x16485a, 0x2f7f8f, posY % 12 / 12.0);
	}

	/** A pixel of the green wall: leaves in greens, and now and then a flower. */
	private static int leaves(int posX, int posY) {
		double leaf = TextureMath.lattice(posX / 3, posY / 3, 43, 43, 7);
		if (TextureMath.lattice(posX / 6, posY / 6, 21, 21, 9) > 0.93) {
			return leaf > 0.5 ? 0xf2c14e : 0xe8505b;
		}
		return TextureMath.mix(0x1d4520, 0x4aa83c, leaf);
	}

	/** A pixel of the water: teal, lighter in slow ripples that repeat without a seam. */
	private static int water(int posX, int posY) {
		double ripple = 0.5 + 0.5 * Math.sin(Math.PI * 2 * posX / 64 + 1.5 * Math.sin(Math.PI * 2 * posY / 64));
		return TextureMath.mix(0x0b3a46, 0x1f8f95, ripple * 0.6);
	}

	/** A pixel of the ground: plots of 32 pixels that are grass, rows of crops or red earth, each in its own shade. */
	private static int ground(int posX, int posY) {
		double plot = TextureMath.lattice(posX / 32, posY / 32, 8, 8, 5);
		double shade = TextureMath.lattice(posX / 4, posY / 4, 64, 64, 6);
		if (plot < 0.55) {
			return TextureMath.mix(0x5a8a40, 0x82ab52, shade);
		}
		boolean row = posX % 6 < 2;
		if (plot < 0.78) {
			return TextureMath.mix(row ? 0x8f5a34 : 0xb07a46, 0xb98850, shade * 0.5);
		}
		return TextureMath.mix(row ? 0x2f6a34 : 0x4f9a46, 0x6fb060, shade * 0.5);
	}
}
