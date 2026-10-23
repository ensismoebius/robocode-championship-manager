package widgets.scenes;

import javafx.scene.image.Image;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The pictures of the Eden Solar arena, from noise: the stormy sky over the opening of the roof, the garden with
 * its hedges, paths and pools and the African patterns of the floor of the field, the crowd of the stands, the lattice of lights under the roof, and the glows of
 * the tree of life. They are only made when the arena is used.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenTexture {

	/** The crowd picture: as wide as this many units of the stands, a seat every 8 */
	static final int CROWD_WIDTH = 1024;
	/** The part of the crowd picture on the riser of a row (the rest is on its tread) */
	static final float RISER_SHARE = 1 / 3f;
	private static final int CROWD_HEIGHT = 96;
	private static final int SEAT = 8;
	private static final int GARDEN_SIZE = 2048;

	private static final int[] SHIRTS = { 0x9c3a36, 0xb8742e, 0xb89a38, 0x3a7d4f, 0x2f5f8e, 0x66468a, 0xb9b8ae, 0xa5502c, 0x2a7470, 0xa34670, 0x34404e, 0xbf8f3a };
	private static final int[] SKINS = { 0xf0c8a0, 0xd8a06a, 0xc08048, 0x8d5524, 0x5e3a22, 0xe2ae74 };
	private static final int[] FLOWERS = { 0xff7aa8, 0xd9479b, 0xf2efe6, 0xf2c14e, 0xe8505b };

	/** The sky: heavy clouds, blue and grey, torn where the sun breaks through */
	static final Image SKY = TextureMath.image(768, 384, EdenTexture::skyPixel);

	/** The bark of the baobab: smooth, copper, with rings running around the trunk; and its rings glowing faintly */
	static final Image BARK = TextureMath.image(256, 256, EdenTexture::barkPixel);
	static final Image BARK_GLOW = TextureMath.image(256, 256, (posX, posY) -> TextureMath.mix(0x000000, 0x5a300c, groove(posX, posY)));
	/** Rammed earth overgrown with moss: the walls and floors of the stadium, as if it had grown out of the ground */
	static final Image EARTH = TextureMath.image(256, 256, EdenTexture::earthPixel);
	/** The crowd: seat backs with the people in front of them on the riser, the seats seen from above on the tread */
	static final Image CROWD = TextureMath.image(CROWD_WIDTH, CROWD_HEIGHT, EdenTexture::crowdPixel);
	/** The underside of the roof: dark structure, ribs and beams, rows of small windows */
	static final Image ROOF = TextureMath.image(256, 256, (posX, posY) -> roofPixel(posX, posY, false));
	/** Only the windows of the roof, lit amber, to make it shine */
	static final Image ROOF_LIGHTS = TextureMath.image(256, 256, (posX, posY) -> roofPixel(posX, posY, true));
	/** The canopy of the tree of life at night: a faint green with points of light */
	static final Image SPARKLES = TextureMath.image(256, 128, (posX, posY) -> TextureMath.lattice(posX, posY, 256, 128, 101) > 0.982 ? TextureMath.mix(0xffd27a, 0xfff8e0, TextureMath.lattice(posX, posY, 256, 128, 102)) : 0x0b2309);
	/** The warm light the tree of life casts on the floor around it */
	static final Image GLOW = TextureMath.image(128, 128, (posX, posY) -> {
		double distance = Math.hypot(posX - 63.5, posY - 63.5) / 64;
		return distance >= 1 ? 0 : TextureMath.mix(0, 0xffa640, Math.pow(1 - distance, 2.2));
	});

	/** The light at the end of a corridor: dark above, warm and bright at the floor */
	static final Image TUNNEL = TextureMath.image(16, 64, (posX, posY) -> TextureMath.mix(0x120904, 0xff9a3c, Math.pow(posY / 63.0, 1.6)));

	private static String gardenKey;
	private static Image garden;
	private static Image lights;

	/** The pictures are static: this class is not instantiated. */
	private EdenTexture() {
	}

	/** The smooth step from 0 at the first edge to 1 at the second. */
	private static double smooth(double low, double high, double value) {
		double time = Math.max(0, Math.min(1, (value - low) / (high - low)));
		return time * time * (3 - 2 * time);
	}

	/**
	 * One pixel of the sky. The clouds are read on a flat layer high above, as the sky is seen through the opening
	 * of the roof, so they keep their shape over the head instead of pinching to a point.
	 */
	private static int skyPixel(int posX, int posY) {
		double azimuth = (posX + 0.5) / 768 * Math.PI * 2;
		double polar = (posY + 0.5) / 384 * Math.PI;
		double upward = Math.cos(polar);
		if (upward < 0.02) {
			return 0x1a2026;
		}
		double[] direction = { Math.sin(polar) * Math.cos(azimuth), upward, Math.sin(polar) * Math.sin(azimuth) };
		double layer = Math.max(upward, 0.12);
		double cloudX = direction[0] / layer * 1.1 + 12;
		double cloudZ = direction[2] / layer * 1.1 + 12;
		double warpU = LavaTexture.fbm(cloudX + 3.1, cloudZ + 7.7, 3, 81);
		double warpV = LavaTexture.fbm(cloudX + 11.3, cloudZ + 2.9, 3, 82);
		double density = LavaTexture.fbm(cloudX + 1.8 * warpU, cloudZ + 1.8 * warpV, 5, 83);
		double cover = smooth(0.40, 0.60, density);
		double thick = smooth(0.52, 0.78, density);

		// The sun, low in the south west
		double sunPolar = Math.toRadians(38);
		double sunAzimuth = Math.toRadians(215);
		double cosine = direction[0] * Math.sin(sunPolar) * Math.cos(sunAzimuth) + direction[1] * Math.cos(sunPolar) + direction[2] * Math.sin(sunPolar) * Math.sin(sunAzimuth);
		double angle = Math.acos(Math.max(-1, Math.min(1, cosine)));
		double halo = Math.exp(-Math.pow(angle / 0.26, 2));

		int color = TextureMath.mix(0x4c6b7b, 0x8fb0bc, halo);
		color = TextureMath.mix(color, TextureMath.mix(0x7d929c, 0x27323b, thick), cover);
		// Silver linings on the edges of the clouds near the sun
		color = TextureMath.mix(color, 0xdfe9ec, cover * (1 - thick) * halo * 0.8);
		color = TextureMath.mix(color, 0xd3e3e8, halo * (1 - thick) * 0.35);
		color = TextureMath.mix(color, 0xfff8e8, Math.exp(-Math.pow(angle / 0.045, 2)) * (1 - cover * 0.7));
		// A dark haze toward the horizon, behind the stadium
		return TextureMath.mix(color, 0x2b363f, smooth(0.3, 0.05, upward));
	}

	/** How deep a pixel of the bark is in one of the rings around the trunk, from 0 to 1. The picture repeats. */
	private static double groove(int posX, int posY) {
		double warp = LavaTexture.fbm(posX / 256.0 * 6, posY / 256.0 * 6, 3, 121);
		double row = posY / 256.0 * 16 + warp * 1.5;
		return smooth(0.16, 0.02, Math.abs(row - Math.rint(row)));
	}

	/** One pixel of the bark: long streaks, lighter and darker, and the dark rings */
	private static int barkPixel(int posX, int posY) {
		double streak = LavaTexture.fbm(posX / 256.0 * 18, posY / 256.0 * 6, 3, 122);
		int color = TextureMath.mix(0x84543a, 0xb07d58, streak * 1.3 - 0.15);
		return TextureMath.mix(color, 0x3d2416, groove(posX, posY) * 0.85);
	}

	/** One pixel of the earth: grain in two browns, and moss where the patches are thick. The picture repeats. */
	private static int earthPixel(int posX, int posY) {
		double patch = LavaTexture.fbm(posX / 256.0 * 6, posY / 256.0 * 6, 4, 131);
		double grain = LavaTexture.fbm(posX / 256.0 * 18, posY / 256.0 * 18, 2, 132);
		int earth = TextureMath.mix(0x5a4430, 0x7c6347, grain);
		return TextureMath.mix(earth, TextureMath.mix(0x2f4a22, 0x4f7a34, grain), smooth(0.52, 0.66, patch));
	}

	/** One pixel of the crowd: a seat every few units, a person in most of them */
	private static int crowdPixel(int posX, int posY) {
		int seat = posX / SEAT;
		int local = posX % SEAT;
		int seats = CROWD_WIDTH / SEAT;
		boolean taken = TextureMath.lattice(seat, 0, seats, 1, 91) < 0.86;
		int shirt = SHIRTS[(int) (TextureMath.lattice(seat, 0, seats, 1, 92) * SHIRTS.length) % SHIRTS.length];
		int skin = SKINS[(int) (TextureMath.lattice(seat, 0, seats, 1, 93) * SKINS.length) % SKINS.length];
		int riser = (int) (CROWD_HEIGHT * RISER_SHARE);
		if (posY < riser) {
			// Some are standing, some are cheering with their arms up
			int lift = (int) (TextureMath.lattice(seat, 0, seats, 1, 94) * 4);
			boolean cheering = TextureMath.lattice(seat, 0, seats, 1, 95) < 0.12;
			int row = posY + lift;
			if (taken && cheering && (local == 0 || local == SEAT - 1) && row < 15) {
				return skin;
			}
			if (taken && local >= 2 && local <= 5 && row >= 5 && row < 13) {
				return row < 7 ? 0x1b120c : skin;
			}
			if (taken && local >= 1 && local <= 6 && row >= 13) {
				return TextureMath.mix(shirt, 0, (row - 13) / 40.0);
			}
			return 0x15181c;
		}
		int tread = posY - riser;
		if (tread < 3) {
			return 0x2c3238;
		}
		if (taken && local >= 1 && local <= 6 && tread >= 6 && tread < 26) {
			return TextureMath.mix(shirt, 0x15181c, 0.35);
		}
		return 0x1d2126;
	}

	/** One pixel under the roof: the structure, or (for the lights only) black but for the windows */
	private static int roofPixel(int posX, int posY, boolean lightsOnly) {
		boolean rib = posX % 64 < 3;
		boolean beam = posY % 32 < 2;
		int cellX = posX % 16;
		int cellY = posY % 16;
		boolean window = !rib && !beam && cellX >= 4 && cellX < 12 && cellY >= 6 && cellY < 10;
		double luck = TextureMath.lattice(posX / 16, posY / 16, 16, 16, 97);
		if (window && luck < 0.9) {
			return TextureMath.mix(0x8a4a10, 0xffb24a, 0.55 + luck * 0.5);
		}
		if (lightsOnly) {
			return 0;
		}
		return rib ? 0x4a423a : beam ? 0x3d362f : 0x221e1b;
	}

	/**
	 * The picture of the garden, a disc of the given radius around the field: the field has the African patterns
	 * of {@link EdenFloor}, the rest is a garden of hedges, flower beds, gravel paths and pools. Made for the size
	 * of the field, and kept while the size is the same.
	 */
	static synchronized Image garden(double radius, double width, double height) {
		makeGarden(radius, width, height);
		return garden;
	}

	/** The lights of the floor of the field (its traces and nodes), in the frame of the picture of the garden. */
	static synchronized Image gardenLights(double radius, double width, double height) {
		makeGarden(radius, width, height);
		return lights;
	}

	/** Makes the picture of the garden and the one of its lights, unless they are already made for this size. */
	private static void makeGarden(double radius, double width, double height) {
		String key = radius + " " + width + " " + height;
		if (!key.equals(gardenKey)) {
			garden = TextureMath.image(GARDEN_SIZE, GARDEN_SIZE, (posX, posY) -> gardenPixel(onFloor(posX, radius), onFloor(posY, radius), radius, width, height));
			lights = TextureMath.image(GARDEN_SIZE, GARDEN_SIZE, (posX, posY) -> lightPixel(onFloor(posX, radius), onFloor(posY, radius), width, height));
			gardenKey = key;
		}
	}

	/** The place on the floor, from the middle of the field, of a pixel of the pictures of the garden. */
	private static double onFloor(int pixel, double radius) {
		return ((pixel + 0.5) / GARDEN_SIZE * 2 - 1) * radius;
	}

	/** The light of the garden at a point: only the field has any. */
	private static int lightPixel(double posX, double posZ, double width, double height) {
		boolean inside = Math.abs(posX) < width / 2 && Math.abs(posZ) < height / 2;
		return inside ? EdenFloor.glow(posX, posZ, width, height) : 0;
	}

	/** The garden at a point of the floor, x and z from the middle of the field. */
	private static int gardenPixel(double posX, double posZ, double radius, double width, double height) {
		// How far outside the field the point is, 0 or less inside it
		double outside = Math.max(Math.abs(posX) - width / 2, Math.abs(posZ) - height / 2);
		if (outside <= 0) {
			return EdenFloor.color(posX, posZ, width, height);
		}
		double grass = LavaTexture.fbm((posX / radius + 1) * 6, (posZ / radius + 1) * 6, 3, 111);
		double distance = Math.hypot(posX, posZ);
		double angle = Math.atan2(posZ, posX);
		int color = TextureMath.mix(0x1f4a1f, 0x30652b, grass * 1.4 - 0.2);
		// The path to each corridor
		if (Math.abs(posZ) < 30 && Math.abs(posX) > width / 2 + 24) {
			return TextureMath.mix(0xb8af96, 0x948b74, LavaTexture.fbm(posX / 9, posZ / 9, 2, 112));
		}
		// Hedges going out like the spokes of a wheel, and in rings, as a mandala
		double spoke = Math.abs(Math.IEEEremainder(angle, Math.PI / 8)) * distance;
		double ring = Math.min(Math.abs(distance - (radius - 190)), Math.abs(distance - (radius - 60)));
		double hedge = Math.min(spoke, ring);
		if (hedge < 5 && outside > 30 && distance < radius - 26) {
			// Rounded: lit along the top, in shadow at the sides
			return TextureMath.mix(0x2f5e28, 0x0e240f, hedge / 5 + grass * 0.2);
		}
		return ringPixel(color, posX, posZ, distance, angle, radius, grass, outside);
	}

	/** The rings of the garden: the hedge at the rim, the path with its pools, and the flower beds */
	private static int ringPixel(int lawn, double posX, double posZ, double distance, double angle, double radius, double grass, double outside) {
		if (distance > radius - 26) {
			return TextureMath.mix(0x163817, 0x22492a, grass);
		}
		double path = radius - 110;
		// Pools on the path, every 30 degrees
		double pool = Math.round((angle - Math.PI / 12) / (Math.PI / 6)) * (Math.PI / 6) + Math.PI / 12;
		double fromPool = Math.hypot(posX - path * Math.cos(pool), posZ - path * Math.sin(pool));
		if (fromPool < 22) {
			return TextureMath.mix(0xbfe3e8, 0x7fb3c0, fromPool / 22);
		}
		if (fromPool < 28) {
			return 0xe9e6dc;
		}
		if (Math.abs(distance - path) < 8) {
			return TextureMath.mix(0xb8af96, 0x948b74, LavaTexture.fbm(posX / 9, posZ / 9, 2, 112));
		}
		if (distance > radius - 96 && distance < radius - 32 && outside > 40) {
			double bloom = LavaTexture.fbm((posX / radius + 1) * 15, (posZ / radius + 1) * 15, 2, 113);
			if (bloom > 0.6) {
				return FLOWERS[(int) (TextureMath.lattice((int) Math.floor(posX / 4), (int) Math.floor(posZ / 4), 4096, 4096, 114) * FLOWERS.length) % FLOWERS.length];
			}
			return TextureMath.mix(0x2f6a2a, 0x447f36, grass);
		}
		return lawn;
	}
}
