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
		for (int i = 0; i <= SEGMENTS; i++) {
			double angle = Math.PI * 2 * i / SEGMENTS;
			double x = RADIUS * Math.cos(angle);
			double z = RADIUS * Math.sin(angle);
			// Two vertices per segment: the top of the picture, then the ground, which is
			// the line where the undergrowth starts, so the moss of the field continues it
			mesh.getPoints().addAll((float) x, (float) TOP, (float) z, (float) x, (float) GROUND, (float) z);
			mesh.getTexCoords().addAll((float) i / SEGMENTS, 0, (float) i / SEGMENTS, (float) UNDERGROWTH);
		}
		for (int i = 0; i < SEGMENTS; i++) {
			int top = 2 * i;
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
		for (int x = 0; x < WIDTH; x++) {
			double u = (double) x / WIDTH * LavaTexture.PERIOD;
			farTop[x] = HEIGHT * (0.27 + 0.05 * LavaTexture.fbm(u, 1.7, 4, 71));
			midTop[x] = HEIGHT * (0.34 + 0.07 * LavaTexture.fbm(u, 2.9, 4, 72));
		}
		// The farthest first: a haze above the trees, misty trees, then the nearer trees
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				int color;
				if (y < farTop[x]) {
					color = mix(0xa3dcc0, 0x4f9f7c, y / farTop[x]);
				} else if (y < midTop[x]) {
					color = mix(0x4a8c78, 0x2c6a58, (y - farTop[x]) / (midTop[x] - farTop[x] + 1));
				} else {
					color = mix(0x275c40, 0x0f3322, (y - midTop[x]) / (HEIGHT - midTop[x] + 1));
				}
				pixels[y * WIDTH + x] = color;
			}
		}

		// The trees, from the farthest to the nearest, so that each one covers the ones behind it.
		// Depth sets the size and the height, and the haze of the air between the tree and the eye
		List<Tree> trees = new ArrayList<>();
		for (int i = 0; i < 900; i++) {
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
		for (int y = (int) ground; y < HEIGHT; y++) {
			double shade = 0.85 - 0.3 * (y - ground) / (HEIGHT - ground);
			for (int x = 0; x < WIDTH; x++) {
				pixels[y * WIDTH + x] = scale(moss.getArgb(x % mossSize, y % mossSize), shade);
			}
		}
		for (int i = 0; i < WIDTH / 2; i++) {
			int x = random.nextInt(WIDTH);
			int height = 10 + random.nextInt(26);
			for (int k = 0; k < height; k++) {
				int y = HEIGHT - 1 - k;
				int half = (int) Math.round(3 * (1 - (double) k / height));
				int color = mix(0x2f8f2a, 0x17521c, (double) k / height);
				for (int dx = -half; dx <= half; dx++) {
					pixels[y * WIDTH + Math.floorMod(x + dx, WIDTH)] = color;
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
			for (int k = 0; k < crowns.length; k++) {
				double rx = size * (0.6 + 0.5 * random.nextDouble());
				crowns[k] = new double[] { x + (random.nextDouble() - 0.5) * size * 1.2, top + (random.nextDouble() - 0.5) * rx * 0.5, rx };
			}
		}

		void paint(int[] pixels) {
			// The trunk: thin at the top, wider at the root, leaning and waving
			for (int y = (int) top; y < foot; y++) {
				double t = (y - top) / (foot - top);
				double centre = x + lean * t + 3 * Math.sin(t * 5 + waver);
				double half = topHalf + (footHalf - topHalf) * t * t + 2.5 * Math.max(0, (t - 0.9) / 0.1);
				for (int wx = (int) Math.floor(centre - half) - 1; wx <= Math.ceil(centre + half) + 1; wx++) {
					double coverage = clamp(half + 0.5 - Math.abs(wx + 0.5 - centre));
					if (coverage == 0) {
						continue;
					}
					int px = Math.floorMod(wx, WIDTH);
					double grain = LavaTexture.fbm(px / (double) WIDTH * LavaTexture.PERIOD * 24, y / (double) HEIGHT * LavaTexture.PERIOD * 4, 2, 81);
					int bark = mix(mix(0x140c07, 0x3c2716, grain), HAZE_TRUNK, haze);
					pixels[y * WIDTH + px] = mix(pixels[y * WIDTH + px], bark, coverage);
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
	private static void crown(int[] pixels, double cx, double cy, double rx, double ry, int lit, int shadow) {
		int top = Math.max(0, (int) Math.floor(cy - ry));
		int bottom = Math.min(HEIGHT - 1, (int) Math.ceil(cy + ry));
		int left = (int) Math.floor(cx - rx);
		int right = (int) Math.ceil(cx + rx);
		for (int y = top; y <= bottom; y++) {
			for (int x = left; x <= right; x++) {
				double nx = (x + 0.5 - cx) / rx;
				double ny = (y + 0.5 - cy) / ry;
				double distance = Math.sqrt(nx * nx + ny * ny);
				// About a pixel from the inside to the outside of the edge
				double coverage = clamp((1 - distance) * Math.min(rx, ry) + 0.5);
				if (coverage == 0) {
					continue;
				}
				int wx = Math.floorMod(x, WIDTH);
				double leaves = LavaTexture.fbm(wx / (double) WIDTH * LavaTexture.PERIOD * 24, y / (double) HEIGHT * LavaTexture.PERIOD * 6, 3, 90);
				double shade = Math.min(1, distance * 0.55 + Math.max(0, ny) * 0.35);
				pixels[y * WIDTH + wx] = mix(pixels[y * WIDTH + wx], mix(lit, shadow, shade + (leaves - 0.5) * 0.7), coverage);
			}
		}
	}

	private static double clamp(double value) {
		return Math.max(0, Math.min(1, value));
	}

	/** Opaque colors, mixed by t from a (0) to b (1) */
	private static int mix(int a, int b, double t) {
		double k = Math.max(0, Math.min(1, t));
		int red = (int) Math.round(channel(a, 16) + (channel(b, 16) - channel(a, 16)) * k);
		int green = (int) Math.round(channel(a, 8) + (channel(b, 8) - channel(a, 8)) * k);
		int blue = (int) Math.round(channel(a, 0) + (channel(b, 0) - channel(a, 0)) * k);
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
