package widgets.scenes;

import java.util.Random;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
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
			// Two vertices per segment: the top of the picture, then the ground
			mesh.getPoints().addAll((float) x, (float) TOP, (float) z, (float) x, (float) GROUND, (float) z);
			mesh.getTexCoords().addAll((float) i / SEGMENTS, 0, (float) i / SEGMENTS, 1);
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

		// Misty crowns far away, many of them, so the line is thick
		for (int i = 0; i < 620; i++) {
			double radius = 18 + random.nextDouble() * 18;
			crown(pixels, random.nextDouble() * WIDTH, HEIGHT * (0.22 + 0.10 * random.nextDouble()), radius, radius * 0.7, 0x7fbfa4, 0x3a7560);
		}
		// Trunks of the nearer trees, between the crowns
		for (int i = 0; i < 70; i++) {
			double x = random.nextDouble() * WIDTH;
			double half = 2 + random.nextDouble() * 3;
			int top = (int) (HEIGHT * (0.44 + 0.08 * random.nextDouble()));
			for (int y = top; y < ground; y++) {
				for (int dx = (int) Math.floor(-half); dx <= half; dx++) {
					int wx = Math.floorMod((int) x + dx, WIDTH);
					double grain = LavaTexture.fbm(wx / (double) WIDTH * LavaTexture.PERIOD * 24, y / (double) HEIGHT * LavaTexture.PERIOD * 4, 2, 81);
					pixels[y * WIDTH + wx] = mix(0x140c07, 0x3c2716, grain);
				}
			}
		}
		// The near crowns: bigger, brighter at the top, shaded at the edges
		for (int i = 0; i < 420; i++) {
			double radius = 40 + random.nextDouble() * 40;
			int palette = random.nextInt(3);
			int lit = palette == 0 ? 0x5fbf4a : palette == 1 ? 0x4fa84a : 0x6fc05a;
			crown(pixels, random.nextDouble() * WIDTH, HEIGHT * (0.30 + 0.14 * random.nextDouble()), radius, radius * 0.62, lit, 0x163f22);
		}

		// Undergrowth: the ground, and ferns standing on it
		for (int y = (int) ground; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				double grain = LavaTexture.fbm(x / (double) WIDTH * LavaTexture.PERIOD * 24, y / (double) HEIGHT * LavaTexture.PERIOD * 6, 2, 82);
				pixels[y * WIDTH + x] = mix(0x123a1e, 0x061509, (y - ground) / (HEIGHT - ground) + (grain - 0.5) * 0.6);
			}
		}
		for (int i = 0; i < WIDTH / 6; i++) {
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
				double nx = (x - cx) / rx;
				double ny = (y - cy) / ry;
				double distance = nx * nx + ny * ny;
				if (distance > 1) {
					continue;
				}
				int wx = Math.floorMod(x, WIDTH);
				double leaves = LavaTexture.fbm(wx / (double) WIDTH * LavaTexture.PERIOD * 24, y / (double) HEIGHT * LavaTexture.PERIOD * 6, 3, 90);
				double shade = Math.min(1, Math.sqrt(distance) * 0.55 + Math.max(0, ny) * 0.35);
				pixels[y * WIDTH + wx] = mix(lit, shadow, shade + (leaves - 0.5) * 0.7);
			}
		}
	}

	/** Opaque colors, mixed by t from a (0) to b (1) */
	private static int mix(int a, int b, double t) {
		double k = Math.max(0, Math.min(1, t));
		int red = (int) Math.round(channel(a, 16) + (channel(b, 16) - channel(a, 16)) * k);
		int green = (int) Math.round(channel(a, 8) + (channel(b, 8) - channel(a, 8)) * k);
		int blue = (int) Math.round(channel(a, 0) + (channel(b, 0) - channel(a, 0)) * k);
		return 0xff000000 | red << 16 | green << 8 | blue;
	}

	private static int channel(int color, int shift) {
		return color >> shift & 0xff;
	}
}
