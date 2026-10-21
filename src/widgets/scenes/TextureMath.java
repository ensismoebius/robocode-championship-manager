package widgets.scenes;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Helpers shared by the procedural textures: blending two colors and the value
 * noise lattice that repeats every period cells.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class TextureMath {

	private TextureMath() {
	}

	/**
	 * Blends two 0xRRGGBB colors, t from 0 (a) to 1 (b), clamped
	 */
	static int mix(int a, int b, double t) {
		t = Math.max(0, Math.min(1, t));
		int r = (int) Math.round(((a >> 16) & 0xff) * (1 - t) + ((b >> 16) & 0xff) * t);
		int g = (int) Math.round(((a >> 8) & 0xff) * (1 - t) + ((b >> 8) & 0xff) * t);
		int bl = (int) Math.round((a & 0xff) * (1 - t) + (b & 0xff) * t);
		return r << 16 | g << 8 | bl;
	}

	/**
	 * A pseudo random value from 0 to 1 at a lattice point, the same for the same
	 * point and seed, repeating every periodU by periodV cells
	 */
	static double lattice(int x, int y, int periodU, int periodV, int seed) {
		int h = Math.floorMod(x, periodU) * 374761393 + Math.floorMod(y, periodV) * 668265263 + seed * 1442695041;
		h = (h ^ (h >>> 13)) * 1274126177;
		return ((h ^ (h >>> 16)) & 0xffff) / 65535.0;
	}
}
