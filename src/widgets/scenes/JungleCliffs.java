package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The highlands at both ends of the jungle arena, where the waterfalls come from: a great wall of wet rock, high and wide, with the
 * forest on top of it. A stream runs over the top and falls from the lip of the wall into the pool at its foot; on both sides of the
 * fall the wall comes forward and holds the pool; the rivers of the arena run out of gorges in it; vines hang down its face, and the
 * trees at its edge lean over it. It stands inside the painted ring of forest, which goes on behind it.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class JungleCliffs {

	/** How high the stream on top is, where it falls (positive up, from the ground) */
	static final double LIP = 459;
	/** How high the top is behind the lip, before it climbs toward the back */
	private static final double TOP = 470;
	/** How far behind the lip the top starts to climb, and how much it climbs for each unit back */
	private static final double FLAT = 650;
	private static final double CLIMB = 0.45;
	/** How far the wall reaches: beyond it the painted ring of forest hides everything */
	private static final double REACH = 2650;
	/** How much the wall at the sides of the fall comes toward the field, around the pool */
	private static final double ARMS = 110;
	/** How wide the gorges of the rivers are, beyond their banks */
	private static final double GORGE = 34;
	private static final double GROUND = 6;
	private static final double[] UP = { 0, -1, 0 };
	private static final String[] ROCK_TONES = { "#4b4a45", "#5a5850", "#3f4640", "#535c4c" };

	private final BattleArena3D host;
	private final double riverZ;
	private final double riverWidth;
	/** The wall at each end: where its face is (x) and where its fall is (z), and its side */
	private final List<double[]> walls = new ArrayList<>();

	/** Creates the highlands of the jungle, around rivers at the distance and of the width given. */
	JungleCliffs(BattleArena3D host, double riverZ, double riverWidth) {
		this.host = host;
		this.riverZ = riverZ;
		this.riverWidth = riverWidth;
	}

	/**
	 * Builds the wall at one end, its face at x, the fall at z: the rock, the forest on top, the stream that falls, and the vines.
	 *
	 * @param side
	 *            1 or -1: which end of the arena, the wall goes away from the field
	 */
	void build(double faceX, double fallZ, int side, JungleArena.JungleMaterials materials, Random wild) {
		double[] wall = { faceX, fallZ, side };
		this.walls.add(wall);
		this.land(wall, materials, wild);
		this.canopy(wall, materials, wild);
		this.vines(wall, wild);
		this.stream(wall, materials);
	}

	/** Whether the point is under a wall, or this near one: no tree or fern of the floor of the jungle grows there. */
	boolean covers(double posX, double posZ, double margin) {
		for (double[] wall : this.walls) {
			double outward = (posX - wall[0]) * wall[2];
			if (outward > -ARMS - margin - 60 && this.inside(wall, outward, posZ) > -margin) {
				return true;
			}
		}
		return false;
	}

	/** The smooth step from 0 at the first edge to 1 at the second. */
	private static double smooth(double low, double high, double value) {
		double time = Math.max(0, Math.min(1, (value - low) / (high - low)));
		return time * time * (3 - 2 * time);
	}

	/** Noise from 0 to 1 along z, the same for the same z and seed. */
	private static double noise(double posZ, double scale, int seed) {
		return LavaTexture.fbm(posZ / scale + 60, 0.5, 3, seed);
	}

	/** Where the face of the wall is, as how far out from the line of the fall, at so far from the fall along it */
	private static double face(double apart, double posZ) {
		double far = Math.abs(apart);
		return -ARMS * smooth(110, 260, far) + 70 * (noise(posZ, 260, 401) - 0.5) * smooth(260, 500, far);
	}

	/** How near the face of the wall the fall is felt: 1 at the fall, nothing far from it. */
	private static double nearFall(double apart) {
		return Math.exp(-(apart / 120) * (apart / 120));
	}

	/**
	 * How far inside the rock the point is, so far out from the face and at z (negative outside it): from the face, or from the
	 * gorges of the rivers, whichever is nearer.
	 */
	private double inside(double[] wall, double outward, double posZ) {
		double apart = posZ - wall[1];
		double fromFace = outward - face(apart, posZ);
		double posX = wall[0] + wall[2] * outward;
		// The two rivers: one on each side, each with its own bends (see JungleArena.jungleRivers)
		double first = Math.abs(posZ + (this.riverZ + LavaFlows.bend(posX, 0))) - LavaFlows.halfWidth(posX, 0, this.riverWidth) - GORGE;
		double second = Math.abs(posZ - (this.riverZ + LavaFlows.bend(posX, 2.3))) - LavaFlows.halfWidth(posX, 2.3, this.riverWidth) - GORGE;
		return Math.min(fromFace, Math.min(first, second));
	}

	/** How high the ground is (positive up) at so far out from the face and at z. */
	private double height(double[] wall, double outward, double posZ) {
		double apart = posZ - wall[1];
		double fall = nearFall(apart);
		double depth = this.inside(wall, outward, posZ);
		if (depth <= 0) {
			return 0;
		}
		double fromFace = outward - face(apart, posZ);
		// The face is sheer behind the fall and rougher away from it; the walls of the gorges are steep too
		double band = depth < fromFace - 0.01 ? 40 + 30 * noise(posZ + outward, 90, 402) : 14 + (1 - fall) * 70 * (0.6 + 0.8 * noise(posZ, 140, 403));
		double top = TOP + CLIMB * Math.max(0, outward - FLAT) + 60 * smooth(300, 900, Math.abs(apart));
		top += 90 * (noise(posZ + outward * 0.7, 220, 404) - 0.5) * (1 - fall);
		// The bed of the stream, a little lower than its banks
		top -= 18 * Math.exp(-(apart / 45) * (apart / 45)) * (outward < FLAT + 60 ? 1 : 0);
		return top * smooth(0, band, depth);
	}

	/** The point in the arena so far out from the face of the wall, at z, at its height. */
	private double[] point(double[] wall, double outward, double posZ, double rise) {
		return new double[] { wall[0] + wall[2] * outward, GROUND - rise, posZ };
	}

	/** The steps of a grid from a to b, every step. */
	private static List<Double> steps(double[][] spans) {
		List<Double> values = new ArrayList<>();
		for (double[] span : spans) {
			for (double value = span[0]; value < span[1] - 1e-9; value += span[2]) {
				values.add(value);
			}
		}
		values.add(spans[spans.length - 1][1]);
		return values;
	}

	/**
	 * The rock of the wall: a grid of heights, finer near the face. The steep faces are rock, of a few tones; the flat ones are moss.
	 */
	private void land(double[] wall, JungleArena.JungleMaterials materials, Random wild) {
		List<Double> outs = steps(new double[][] { { -ARMS - 150, -40, 20 }, { -40, 60, 10 }, { 60, 400, 20 }, { 400, REACH - Math.abs(wall[0]) + 100, 40 } });
		List<Double> zs = steps(new double[][] { { -2100, -700, 50 }, { -700, 700, 20 }, { 700, 2100, 50 } });
		double[][][] grid = new double[outs.size()][zs.size()][];
		double[][] rises = new double[outs.size()][zs.size()];
		for (int row = 0; row < outs.size(); row++) {
			for (int col = 0; col < zs.size(); col++) {
				double outward = outs.get(row);
				double posZ = zs.get(col);
				double rise = this.height(wall, outward, posZ);
				// The top is rough, but not over the stream
				if (rise > TOP * 0.95) {
					rise += (TextureMath.lattice(row, col, 4096, 4096, 405) - 0.5) * 18 * (1 - nearFall(posZ - wall[1]));
				}
				rises[row][col] = rise;
				grid[row][col] = this.point(wall, outward, posZ, rise);
			}
		}
		SurfaceMesh moss = new SurfaceMesh();
		SurfaceMesh[] rock = new SurfaceMesh[ROCK_TONES.length];
		for (int tone = 0; tone < rock.length; tone++) {
			rock[tone] = new SurfaceMesh();
		}
		for (int row = 0; row + 1 < outs.size(); row++) {
			for (int col = 0; col + 1 < zs.size(); col++) {
				double[] first = grid[row][col];
				double[] second = grid[row][col + 1];
				double[] third = grid[row + 1][col + 1];
				double[] fourth = grid[row + 1][col];
				boolean flat = rises[row][col] < 0.5 && rises[row][col + 1] < 0.5 && rises[row + 1][col + 1] < 0.5 && rises[row + 1][col] < 0.5;
				boolean beyond = Math.hypot(first[0], first[2]) > REACH && Math.hypot(third[0], third[2]) > REACH && Math.hypot(second[0], second[2]) > REACH
						&& Math.hypot(fourth[0], fourth[2]) > REACH;
				if (flat || beyond) {
					continue;
				}
				SurfaceMesh tone = rock[(int) (TextureMath.lattice(row, col, 4096, 4096, 406) * rock.length) % rock.length];
				this.facet(first, second, third, moss, tone);
				this.facet(first, third, fourth, moss, tone);
			}
		}
		PhongMaterial mossy = materials.moss();
		this.host.arena.getChildren().add(moss.view(mossy));
		for (int tone = 0; tone < rock.length; tone++) {
			PhongMaterial stone = new PhongMaterial(Color.web(ROCK_TONES[tone]));
			stone.setSpecularColor(Color.gray(0.08));
			stone.setSpecularPower(14);
			this.host.arena.getChildren().add(rock[tone].view(stone));
		}
	}

	/** A triangle of the land: moss when it is nearly flat, rock when it is steep. */
	private void facet(double[] first, double[] second, double[] third, SurfaceMesh moss, SurfaceMesh rock) {
		double[] along = { second[0] - first[0], second[1] - first[1], second[2] - first[2] };
		double[] across = { third[0] - first[0], third[1] - first[1], third[2] - first[2] };
		double normalX = along[1] * across[2] - along[2] * across[1];
		double normalY = along[2] * across[0] - along[0] * across[2];
		double normalZ = along[0] * across[1] - along[1] * across[0];
		double length = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
		if (length < 1e-9) {
			return;
		}
		if (Math.abs(normalY) / length > 0.8) {
			moss.triangle(first, second, third, new float[] { (float) (first[0] / 260), (float) (first[2] / 260), (float) (second[0] / 260), (float) (second[2] / 260),
					(float) (third[0] / 260), (float) (third[2] / 260) }, UP);
		} else {
			rock.triangle(first, second, third, new float[6], UP);
		}
	}

	/**
	 * The forest on top of the wall: crowns of trees packed together, more of them near the edge, where they are seen, and big trees
	 * at the edge, leaning over the face.
	 */
	private void canopy(double[] wall, JungleArena.JungleMaterials materials, Random wild) {
		// The crowns are clumps of leaves as those of the trees, a little sunk into the top so it does not show between them
		Forest crowns = new Forest();
		int placed = 0;
		for (int attempt = 0; attempt < 6000 && placed < 260; attempt++) {
			double outward = Math.pow(wild.nextDouble(), 1.6) * (REACH - Math.abs(wall[0]));
			double posZ = (wild.nextDouble() * 2 - 1) * 2100;
			double[] spot = this.point(wall, outward, posZ, 0);
			double rise = this.height(wall, outward, posZ);
			double apart = posZ - wall[1];
			// On top, a little in from the edge, inside the ring, not over the stream
			if (rise < TOP * 0.9 || this.inside(wall, outward, posZ) < 30 || Math.hypot(spot[0], spot[2]) > REACH - 40 || (Math.abs(apart) < 70 && outward < FLAT)) {
				continue;
			}
			placed++;
			double radius = 55 + wild.nextDouble() * 45;
			crowns.clump(spot[0], GROUND - rise - radius * 0.25 - wild.nextDouble() * 40, spot[2], radius, wild);
		}
		MeshView canopy = new MeshView(crowns.leaves);
		canopy.setMaterial(materials.leaves());
		canopy.setCullFace(CullFace.BACK);
		this.host.arena.getChildren().add(canopy);
		// Big trees at the edge, on both sides of the fall
		Forest edge = new Forest();
		for (int tree = 0; tree < 9; tree++) {
			double apart = (wild.nextBoolean() ? 1 : -1) * (130 + wild.nextDouble() * 650);
			double posZ = wall[1] + apart;
			double outward = this.edgeOf(wall, posZ) + 25 + wild.nextDouble() * 45;
			double rise = this.height(wall, outward, posZ);
			if (rise < TOP * 0.9) {
				continue;
			}
			double[] foot = this.point(wall, outward, posZ, rise);
			edge.addTree(foot[0], foot[1], foot[2], 0.55 + wild.nextDouble() * 0.35, wild);
		}
		MeshView trunks = new MeshView(edge.bark);
		trunks.setMaterial(materials.bark());
		trunks.setCullFace(CullFace.NONE);
		MeshView leaves = new MeshView(edge.leaves);
		leaves.setMaterial(materials.leaves());
		leaves.setCullFace(CullFace.BACK);
		this.host.arena.getChildren().addAll(trunks, leaves);
	}

	/** How far out from the face line the edge of the top is, at z. */
	private double edgeOf(double[] wall, double posZ) {
		double apart = posZ - wall[1];
		double from = face(apart, posZ);
		for (double outward = from; outward < from + 300; outward += 5) {
			if (this.height(wall, outward, posZ) > TOP * 0.9) {
				return outward;
			}
		}
		return from + 300;
	}

	/**
	 * Vines that hang down the face from the edge of the top, close to the rock, swaying a little: where the face is at each
	 * height is looked for, so they follow it.
	 */
	private void vines(double[] wall, Random wild) {
		LowPoly vines = new LowPoly();
		for (int vine = 0; vine < 46; vine++) {
			double apart = (wild.nextBoolean() ? 1 : -1) * (85 + wild.nextDouble() * 820);
			double posZ = wall[1] + apart;
			double top = this.height(wall, this.edgeOf(wall, posZ) + 5, posZ);
			if (top < TOP * 0.8) {
				continue;
			}
			double length = Math.min(top - 20, 90 + wild.nextDouble() * 230);
			double sway = (wild.nextDouble() - 0.5) * 20;
			double[] last = null;
			for (int knot = 0; knot <= 6; knot++) {
				double rise = top - 3 - length * knot / 6;
				double outward = this.faceAt(wall, posZ + sway * knot / 6, rise) - 3;
				double[] here = this.point(wall, outward, posZ + sway * Math.sin(knot * 0.9) / 2, rise);
				if (last != null) {
					vines.tube(last, here, 1.4 - knot * 0.1, 4);
				}
				last = here;
			}
		}
		PhongMaterial green = new PhongMaterial(Color.web("#2c5a1e"));
		green.setSpecularColor(Color.gray(0.05));
		this.host.arena.getChildren().add(vines.view(green));
	}

	/** How far out from the face line the face of the wall is, at z and at the height. */
	private double faceAt(double[] wall, double posZ, double rise) {
		double low = face(posZ - wall[1], posZ) - 1;
		double high = low + 400;
		for (int step = 0; step < 16; step++) {
			double middle = (low + high) / 2;
			if (this.height(wall, middle, posZ) < rise) {
				low = middle;
			} else {
				high = middle;
			}
		}
		return low;
	}

	/** The stream over the top that becomes the fall: it comes out of the forest and runs to the lip. */
	private void stream(double[] wall, JungleArena.JungleMaterials materials) {
		int count = 16;
		double[] xs = new double[count];
		double[] zs = new double[count];
		double[] halves = new double[count];
		for (int index = 0; index < count; index++) {
			double share = (double) index / (count - 1);
			double outward = FLAT - (FLAT + 2.5) * share;
			xs[index] = wall[0] + wall[2] * outward;
			zs[index] = wall[1] + 14 * Math.sin(share * 5) * (1 - share);
			// It widens as it comes to the lip, as wide as the fall
			halves[index] = 30 + 40 * share * share;
		}
		this.host.lavaFlows.flowRibbon(xs, zs, halves, GROUND - LIP, -0.35, 0, materials.water(), 120);
	}
}
