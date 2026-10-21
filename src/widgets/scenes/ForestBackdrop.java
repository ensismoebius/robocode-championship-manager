package widgets.scenes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The dense forest all around the jungle arena, pre-rendered: the picture is
 * painted once, from noise and a random tree line, and wrapped on a ring far
 * away. The far layers are misty, the near ones have crowns, trunks and
 * undergrowth. It repeats around, so there is no seam.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ForestBackdrop {

	/** Pixels of the picture, all around and from the top of the trees to the ground */
	private static final int WIDTH = 3072;
	private static final int HEIGHT = 384;
	/** The ring: its radius, its top and the ground, in scene units (y goes down) */
	private static final double RADIUS = 2600;
	private static final double TOP = -1300;
	private static final double GROUND = 6;
	private static final int SEGMENTS = 128;
	/** Where the undergrowth starts, as a fraction of the height of the picture */
	private static final double UNDERGROWTH = 0.84;

	/** Painted only when first used: it stays the same for every battle */
	private static final Image PICTURE = paint();

	private ForestBackdrop() {
	}

	/**
	 * The ring of forest, seen from inside
	 */
	static MeshView ring() {
		TriangleMesh mesh = new TriangleMesh();
		for (int index = 0; index <= SEGMENTS; index++) {
			double angle = Math.PI * 2 * index / SEGMENTS;
			double posX = RADIUS * Math.cos(angle);
			double posZ = RADIUS * Math.sin(angle);
			// Two vertices per segment: the top of the picture, then the ground, which is
			// the line where the undergrowth starts, so the moss of the field continues it
			mesh.getPoints().addAll((float) posX, (float) TOP, (float) posZ, (float) posX, (float) GROUND, (float) posZ);
			mesh.getTexCoords().addAll((float) index / SEGMENTS, 0, (float) index / SEGMENTS, (float) UNDERGROWTH);
		}
		for (int index2 = 0; index2 < SEGMENTS; index2++) {
			int top = 2 * index2;
			int bottom = top + 1;
			int nextTop = top + 2;
			int nextBottom = top + 3;
			mesh.getFaces().addAll(top, top, bottom, bottom, nextTop, nextTop);
			mesh.getFaces().addAll(nextTop, nextTop, bottom, bottom, nextBottom, nextBottom);
		}
		PhongMaterial material = new PhongMaterial(Color.BLACK);
		material.setSelfIlluminationMap(PICTURE);
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		return view;
	}

	private static Image paint() {
		Random random = new Random(77);
		int[] pixels = new int[WIDTH * HEIGHT];
		double ground = HEIGHT * UNDERGROWTH;

		// Where each layer of the tree line starts, column by column, from noise
		double[] farTop = new double[WIDTH];
		double[] midTop = new double[WIDTH];
		for (int posX = 0; posX < WIDTH; posX++) {
			double uCoord = (double) posX / WIDTH * LavaTexture.PERIOD;
			farTop[posX] = HEIGHT * (0.27 + 0.05 * LavaTexture.fbm(uCoord, 1.7, 4, 71));
			midTop[posX] = HEIGHT * (0.34 + 0.07 * LavaTexture.fbm(uCoord, 2.9, 4, 72));
		}
		// The farthest first: a haze above the trees, misty trees, then the nearer trees
		for (int posX2 = 0; posX2 < WIDTH; posX2++) {
			for (int posY = 0; posY < HEIGHT; posY++) {
				int color;
				if (posY < farTop[posX2]) {
					color = mix(0xa3dcc0, 0x4f9f7c, posY / farTop[posX2]);
				} else if (posY < midTop[posX2]) {
					color = mix(0x4a8c78, 0x2c6a58, (posY - farTop[posX2]) / (midTop[posX2] - farTop[posX2] + 1));
				} else {
					color = mix(0x275c40, 0x0f3322, (posY - midTop[posX2]) / (HEIGHT - midTop[posX2] + 1));
				}
				pixels[posY * WIDTH + posX2] = color;
			}
		}

		// The trees, from the farthest to the nearest, so that each one covers the ones behind it.
		// Depth sets the size and the height, and the haze of the air between the tree and the eye
		List<Tree> trees = new ArrayList<>();
		for (int index = 0; index < 900; index++) {
			trees.add(new Tree(random, ground));
		}
		trees.sort(Comparator.comparingDouble(tree -> tree.depth));
		for (Tree tree : trees) {
			tree.paint(pixels);
		}

		// Undergrowth: the ground, with the same moss as the field, darker toward the
		// bottom, and ferns standing on it
		PixelReader moss = JungleTexture.MOSS.getPixelReader();
		int mossSize = (int) JungleTexture.MOSS.getWidth();
		for (int posY2 = (int) ground; posY2 < HEIGHT; posY2++) {
			double shade = 0.85 - 0.3 * (posY2 - ground) / (HEIGHT - ground);
			for (int posX3 = 0; posX3 < WIDTH; posX3++) {
				pixels[posY2 * WIDTH + posX3] = scale(moss.getArgb(posX3 % mossSize, posY2 % mossSize), shade);
			}
		}
		for (int index2 = 0; index2 < WIDTH / 2; index2++) {
			int posX4 = random.nextInt(WIDTH);
			int height = 10 + random.nextInt(26);
			for (int deepIndex = 0; deepIndex < height; deepIndex++) {
				int posY3 = HEIGHT - 1 - deepIndex;
				int half = (int) Math.round(3 * (1 - (double) deepIndex / height));
				int color = mix(0x2f8f2a, 0x17521c, (double) deepIndex / height);
				for (int deltaX = -half; deltaX <= half; deltaX++) {
					pixels[posY3 * WIDTH + Math.floorMod(posX4 + deltaX, WIDTH)] = color;
				}
			}
		}

		WritableImage image = new WritableImage(WIDTH, HEIGHT);
		image.getPixelWriter().setPixels(0, 0, WIDTH, HEIGHT, PixelFormat.getIntArgbInstance(), pixels, 0, WIDTH);
		return image;
	}

	/**
	 * A tree: a trunk and a cluster of crowns. Its depth, from 0 far away to 1 close,
	 * sets its size, its height and the haze: far trees are small, high and pale, near
	 * ones big, low and dark
	 */
	private static final class Tree {
		private static final int[] PALETTE = { 0x5fbf4a, 0x4fa84a, 0x6fc05a, 0x3f9a5a };
		private static final int HAZE = 0xb4e2c8;
		private static final int HAZE_TRUNK = 0x5d8a78;

		private final double depth;
		private final double haze;
		private final double x;
		private final double top;
		private final double foot;
		private final double topHalf;
		private final double footHalf;
		private final double lean;
		private final double waver;
		private final int lit;
		private final int shadow;
		private final double[][] crowns;

		Tree(Random random, double ground) {
			depth = Math.pow(random.nextDouble(), 0.8);
			haze = Math.pow(1 - depth, 1.5) * 0.8;
			x = random.nextDouble() * WIDTH;
			double size = 12 + 70 * depth;
			top = HEIGHT * (0.20 + 0.20 * depth) + HEIGHT * 0.04 * random.nextDouble();
			foot = ground;
			topHalf = (0.8 + 2.2 * depth) * (0.8 + 0.4 * random.nextDouble());
			footHalf = topHalf * (1.6 + 1.2 * random.nextDouble());
			lean = (random.nextDouble() - 0.5) * 14 * (0.5 + depth);
			waver = random.nextDouble() * Math.PI * 2;
			lit = mix(PALETTE[random.nextInt(PALETTE.length)], HAZE, haze);
			shadow = mix(0x163f22, 0x4f8a72, haze);
			crowns = new double[3][];
			for (int deepIndex = 0; deepIndex < crowns.length; deepIndex++) {
				double rxValue = size * (0.6 + 0.5 * random.nextDouble());
				crowns[deepIndex] = new double[] { x + (random.nextDouble() - 0.5) * size * 1.2, top + (random.nextDouble() - 0.5) * rxValue * 0.5, rxValue };
			}
		}

		void paint(int[] pixels) {
			// The trunk: thin at the top, wider at the root, leaning and waving
			for (int posY = (int) top; posY < foot; posY++) {
				double time = (posY - top) / (foot - top);
				double centre = x + lean * time + 3 * Math.sin(time * 5 + waver);
				double half = topHalf + (footHalf - topHalf) * time * time + 2.5 * Math.max(0, (time - 0.9) / 0.1);
				for (int wxValue = (int) Math.floor(centre - half) - 1; wxValue <= Math.ceil(centre + half) + 1; wxValue++) {
					double coverage = clamp(half + 0.5 - Math.abs(wxValue + 0.5 - centre));
					if (coverage == 0) {
						continue;
					}
					int posX = Math.floorMod(wxValue, WIDTH);
					double grain = LavaTexture.fbm(posX / (double) WIDTH * LavaTexture.PERIOD * 24, posY / (double) HEIGHT * LavaTexture.PERIOD * 4, 2, 81);
					int bark = mix(mix(0x140c07, 0x3c2716, grain), HAZE_TRUNK, haze);
					pixels[posY * WIDTH + posX] = mix(pixels[posY * WIDTH + posX], bark, coverage);
				}
			}
			// The crowns
			for (double[] crown : crowns) {
				crown(pixels, crown[0], crown[1], crown[2], crown[2] * 0.66, lit, shadow);
			}
		}
	}

	/**
	 * A crown, an ellipse of leaves: lit at the top, darker at the edges and
	 * underneath, with leaf noise in it. It wraps around the picture.
	 */
	private static void crown(int[] pixels, double cxValue, double cyValue, double rxValue, double ryValue, int lit, int shadow) {
		int top = Math.max(0, (int) Math.floor(cyValue - ryValue));
		int bottom = Math.min(HEIGHT - 1, (int) Math.ceil(cyValue + ryValue));
		int left = (int) Math.floor(cxValue - rxValue);
		int right = (int) Math.ceil(cxValue + rxValue);
		for (int posY = top; posY <= bottom; posY++) {
			for (int posX = left; posX <= right; posX++) {
				double nxValue = (posX + 0.5 - cxValue) / rxValue;
				double nyValue = (posY + 0.5 - cyValue) / ryValue;
				double distance = Math.sqrt(nxValue * nxValue + nyValue * nyValue);
				// About a pixel from the inside to the outside of the edge
				double coverage = clamp((1 - distance) * Math.min(rxValue, ryValue) + 0.5);
				if (coverage == 0) {
					continue;
				}
				int wxValue = Math.floorMod(posX, WIDTH);
				double leaves = LavaTexture.fbm(wxValue / (double) WIDTH * LavaTexture.PERIOD * 24, posY / (double) HEIGHT * LavaTexture.PERIOD * 6, 3, 90);
				double shade = Math.min(1, distance * 0.55 + Math.max(0, nyValue) * 0.35);
				pixels[posY * WIDTH + wxValue] = mix(pixels[posY * WIDTH + wxValue], mix(lit, shadow, shade + (leaves - 0.5) * 0.7), coverage);
			}
		}
	}

	private static double clamp(double value) {
		return Math.max(0, Math.min(1, value));
	}

	/** Opaque colors, mixed by t from a (0) to b (1) */
	private static int mix(int first, int second, double time) {
		double deepIndex = Math.max(0, Math.min(1, time));
		int red = (int) Math.round(channel(first, 16) + (channel(second, 16) - channel(first, 16)) * deepIndex);
		int green = (int) Math.round(channel(first, 8) + (channel(second, 8) - channel(first, 8)) * deepIndex);
		int blue = (int) Math.round(channel(first, 0) + (channel(second, 0) - channel(first, 0)) * deepIndex);
		return 0xff000000 | red << 16 | green << 8 | blue;
	}

	/** An opaque color, darker or lighter by factor */
	private static int scale(int color, double factor) {
		int red = (int) Math.round(channel(color, 16) * factor);
		int green = (int) Math.round(channel(color, 8) * factor);
		int blue = (int) Math.round(channel(color, 0) * factor);
		return 0xff000000 | red << 16 | green << 8 | blue;
	}

	private static int channel(int color, int shift) {
		return color >> shift & 0xff;
	}
}
