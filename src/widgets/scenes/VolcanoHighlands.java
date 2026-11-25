package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The land around the lake of lava of the volcano arena, where life holds on. The lake ends at a shore of dark rock, a ledge
 * where the salamanders bask in the warmth, and from it rise cliffs, and beyond them highlands that climb into mountains against
 * the glowing sky. In the foot of the cliffs open caves, cool and dark, where fungi glow blue green and the bats live: at dusk they
 * circle in front of the mouths. Beside some caves there are hot springs, water warmed by the volcano. In the lake stand islands
 * of basalt columns.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class VolcanoHighlands {

	/** A cave in the foot of the cliffs: its angle around the arena, how far from the middle its back is, how wide and high its mouth is */
	record Cave(double angle, double radius, double width, double height) {
		/** The point at so far across the mouth (along the cliff), so high (positive up) and so far from the middle. */
		double[] at(double across, double up, double distance) {
			double cos = Math.cos(this.angle);
			double sin = Math.sin(this.angle);
			return new double[] { cos * distance - sin * across, -up, sin * distance + cos * across };
		}
	}

	/** A volcano of the lake: where it stands and how wide its foot is */
	record Peak(double posX, double posZ, double radius) {
	}

	/** The nearest the shore comes to the middle, and how much farther it can go */
	static final double SHORE = 2450;
	private static final double SHORE_SPREAD = 380;
	/** Half the side of the lake of lava: everywhere beyond the shore, under the rock */
	static final double LAKE = 3000;
	/** How many steps around the arena the land has, and how many times the rock texture goes around */
	private static final int SEGMENTS = 200;
	private static final double ROCK_TILES = 48;
	/** How wide the ledge at the foot of the cliffs is, how much wider it can be, and how much wider in front of a cave */
	private static final double LEDGE = 30;
	private static final double LEDGE_SPREAD = 70;
	private static final double CAVE_LEDGE = 150;
	/** How high the ledge is (positive up): above the lava, which is at -7 */
	static final double LEDGE_HEIGHT = 5;
	/** How deep the mouth of a cave goes, from its arch to the dark of its back */
	static final double CAVE_DEPTH = 75;
	/** How many points the arch of a cave has */
	private static final int ARCH = 10;
	private static final int BATS = 12;
	/** Up (to -Y): the side the land and the floors face */
	private static final double[] UP = { 0, -1, 0 };

	private final BattleArena3D host;
	private final List<Cave> caves = new ArrayList<>();
	private final List<Peak> peaks = new ArrayList<>();
	private final List<Swarm> swarms = new ArrayList<>();
	private double clock;

	/** The bats of a cave: two poses of the wings, shown by turns, and the turn of the flock around the mouth */
	private record Swarm(MeshView wingsUp, MeshView wingsDown, Rotate turn, double speed, double beat) {
	}

	/** Creates the highlands of the arena, drawn in the host scene. */
	VolcanoHighlands(BattleArena3D host) {
		this.host = host;
	}

	/** The caves in the foot of the cliffs. */
	List<Cave> caves() {
		return this.caves;
	}

	/** The volcanoes of the lake, as the highlands were built around them. */
	List<Peak> peaks() {
		return this.peaks;
	}

	/** Builds the shore, the cliffs and the highlands, the caves between the volcanoes, and the islands of basalt in the lake. */
	void build(double width, double height, List<Peak> volcanoes, Random wild) {
		this.caves.clear();
		this.peaks.clear();
		this.peaks.addAll(volcanoes);
		this.swarms.clear();
		this.placeCaves(wild);
		PhongMaterial rock = new PhongMaterial(Color.WHITE);
		rock.setDiffuseMap(VolcanoTexture.ROCK);
		rock.setSelfIlluminationMap(VolcanoTexture.ROCK_GLOW);
		rock.setSpecularColor(Color.web("#1a1210"));
		this.land(rock, wild);
		for (Cave cave : this.caves) {
			this.cave(cave, rock, wild);
		}
		this.islands(width, height, wild);
	}

	/** The bats circle and beat their wings. */
	void update(double deltaTime) {
		this.clock += deltaTime;
		for (Swarm swarm : this.swarms) {
			swarm.turn().setAngle(swarm.turn().getAngle() + swarm.speed() * deltaTime);
			boolean up = ((this.clock * 7 + swarm.beat()) % 1 + 1) % 1 < 0.5;
			swarm.wingsUp().setVisible(up);
			swarm.wingsDown().setVisible(!up);
		}
	}

	/** The caves: one in most of the gaps between the volcanoes, so no volcano hides it from the field. */
	private void placeCaves(Random wild) {
		List<Double> angles = new ArrayList<>();
		for (Peak peak : this.peaks) {
			angles.add(Math.atan2(peak.posZ(), peak.posX()));
		}
		angles.sort(null);
		List<double[]> chosen = new ArrayList<>();
		for (int index = 0; index < angles.size(); index++) {
			double from = angles.get(index);
			double to = index + 1 < angles.size() ? angles.get(index + 1) : angles.get(0) + Math.PI * 2;
			if (wild.nextDouble() < 0.75) {
				chosen.add(new double[] { (from + to) / 2, 150 + wild.nextDouble() * 60, 110 + wild.nextDouble() * 50 });
			}
		}
		if (angles.isEmpty()) {
			chosen.add(new double[] { wild.nextDouble() * Math.PI * 2, 180, 130 });
		}
		// The ledge in front of a cave is wider, so it is first placed, and then its back, at the foot of the cliff
		for (double[] spec : chosen) {
			this.caves.add(new Cave(spec[0], 0, spec[1], spec[2]));
		}
		for (int index = 0; index < this.caves.size(); index++) {
			Cave cave = this.caves.get(index);
			this.caves.set(index, new Cave(cave.angle(), shore(cave.angle()) + this.ledge(cave.angle()) - 3, cave.width(), cave.height()));
		}
	}

	/** How far from the middle the shore is, at the angle. */
	static double shore(double angle) {
		return SHORE + SHORE_SPREAD * LavaTexture.fbm(around(angle, 1), 0.5, 3, 301);
	}

	/** Where the angle is around the arena, as a coordinate of the noise that wraps around once for each turn. */
	private static double around(double angle, int turns) {
		return (angle / (Math.PI * 2) % 1 + 1) % 1 * LavaTexture.PERIOD * turns;
	}

	/**
	 * How much a cave is felt at the angle: 1 all across its mouth and a margin beside it, then less and less, nothing far to its
	 * sides. Flat across the mouth, so the land, which has its points only every few steps, is the same all along it.
	 *
	 * @param margin
	 *            how far beyond the side of the mouth it is still all felt
	 * @param fade
	 *            how far from there it fades
	 */
	private double nearCave(double angle, double margin, double fade) {
		double most = 0;
		for (Cave cave : this.caves) {
			double apart = Math.atan2(Math.sin(angle - cave.angle()), Math.cos(angle - cave.angle()));
			double beyond = Math.max(0, Math.abs(apart) * shore(cave.angle()) - cave.width() / 2 - margin) / fade;
			most = Math.max(most, Math.exp(-beyond * beyond));
		}
		return most;
	}

	/** How wide the ledge at the foot of the cliffs is, at the angle: wider in front of the caves. */
	private double ledge(double angle) {
		return LEDGE + LEDGE_SPREAD * LavaTexture.fbm(around(angle, 2), 1.5, 3, 302) + CAVE_LEDGE * this.nearCave(angle, 90, 110);
	}

	/** How high the cliffs are, at the angle: high enough over a cave for its mouth. */
	private double cliff(double angle) {
		double cliff = 260 + 420 * LavaTexture.fbm(around(angle, 2), 2.5, 3, 303);
		double tallest = 0;
		for (Cave cave : this.caves) {
			tallest = Math.max(tallest, cave.height());
		}
		return cliff + this.nearCave(angle, 140, 250) * Math.max(0, tallest + 240 - cliff);
	}

	/**
	 * The land, as rings around the lake: the shore that goes under the lava, the ledge, the cliff, and the highlands that climb into
	 * mountains far away. It is faceted, as the rest of the arena, and only seen from above.
	 */
	private void land(PhongMaterial rock, Random wild) {
		double[] steps = { -60, 0, 0, 16, 40, 75, 115, 230, 450, 850, 1450, 2300, 3400, 4800, 6400, 8400 };
		int rings = steps.length;
		double[][][] points = new double[rings][SEGMENTS][];
		double[][] heights = new double[rings][SEGMENTS];
		double step = Math.PI * 2 / SEGMENTS;
		for (int segment = 0; segment < SEGMENTS; segment++) {
			double angle = segment * step;
			double shore = shore(angle);
			double ledge = this.ledge(angle);
			double cliff = this.cliff(angle);
			for (int ring = 0; ring < rings; ring++) {
				double smooth = LavaTexture.fbm(around(angle, 2), ring * 0.83 + 0.3, 3, 310);
				double rough = TextureMath.lattice(segment, ring, SEGMENTS, 64, 311);
				double bump = 0.7 * smooth + 0.3 * rough;
				double rise = switch (ring) {
				case 0 -> -16;
				case 1 -> LEDGE_HEIGHT - 2;
				case 2 -> LEDGE_HEIGHT;
				case 3 -> LEDGE_HEIGHT + cliff * (0.24 + 0.14 * bump);
				case 4 -> LEDGE_HEIGHT + cliff * (0.6 + 0.16 * bump);
				case 5 -> cliff * (0.88 + 0.1 * bump);
				case 6 -> cliff;
				case 7 -> cliff + 60 * bump;
				case 8 -> cliff + 40 + 160 * bump;
				case 9 -> cliff + 150 + 380 * bump;
				case 10 -> 600 + 900 * bump;
				case 11 -> 800 + 1100 * bump;
				case 12 -> 1000 + 1500 * bump;
				case 13 -> 1200 + 1800 * bump;
				case 14 -> 1400 + 2000 * bump;
				default -> 1000 + 1800 * bump;
				};
				if (ring >= 10) {
					rise = Math.max(rise, cliff + 80);
				}
				// The rings are moved a little, each point its own way, but never past the ring before: the land has no overhang
				double shift = TextureMath.lattice(segment, ring, SEGMENTS, 64, 312) - 0.5;
				double turn = TextureMath.lattice(segment, ring, SEGMENTS, 64, 313) - 0.5;
				double distance = shore + (ring >= 2 ? ledge : 0) + steps[ring];
				if (ring >= 7) {
					distance += shift * 0.4 * (steps[ring] - steps[ring - 1]);
				} else if (ring >= 3) {
					distance += shift * 8;
				}
				double at = angle + turn * step * (ring >= 7 ? 0.6 : ring >= 3 ? 0.25 : 0);
				points[ring][segment] = new double[] { Math.cos(at) * distance, -rise, Math.sin(at) * distance };
				heights[ring][segment] = rise;
			}
		}
		SurfaceMesh land = new SurfaceMesh();
		for (int ring = 0; ring + 1 < rings; ring++) {
			for (int segment = 0; segment < SEGMENTS; segment++) {
				int next = (segment + 1) % SEGMENTS;
				float from = (float) ((double) segment / SEGMENTS * ROCK_TILES);
				float to = (float) ((double) (segment + 1) / SEGMENTS * ROCK_TILES);
				float[] coords = { from, (float) VolcanoTexture.rockV(heights[ring][segment]), to, (float) VolcanoTexture.rockV(heights[ring][next]), to,
						(float) VolcanoTexture.rockV(heights[ring + 1][next]), from, (float) VolcanoTexture.rockV(heights[ring + 1][segment]) };
				land.quad(points[ring][segment], points[ring][next], points[ring + 1][next], points[ring + 1][segment], coords, UP);
			}
		}
		this.host.arena.getChildren().add(land.view(rock));
		this.springs(wild);
	}

	/**
	 * A cave in the foot of the cliff: an arch of rock that comes out of it, dark inside, the dark of its back, boulders around the
	 * mouth, the fungi that glow inside, and the bats that circle in front of it.
	 */
	private void cave(Cave cave, PhongMaterial rock, Random wild) {
		double back = cave.radius();
		double front = back - CAVE_DEPTH;
		double half = cave.width() / 2;
		double[][] innerFront = new double[ARCH + 1][];
		double[][] innerBack = new double[ARCH + 1][];
		double[][] outerFront = new double[ARCH + 1][];
		double[][] outerBack = new double[ARCH + 1][];
		double[][] arch = new double[ARCH + 1][2];
		double[][] hood = new double[ARCH + 1][2];
		for (int point = 0; point <= ARCH; point++) {
			double turn = Math.PI * point / ARCH;
			double jitter = 1 + (wild.nextDouble() - 0.5) * 0.16;
			double rough = 1 + (wild.nextDouble() - 0.5) * 0.2;
			boolean foot = point == 0 || point == ARCH;
			arch[point][0] = half * Math.cos(turn) * jitter;
			arch[point][1] = foot ? 0 : cave.height() * Math.sin(turn) * jitter;
			hood[point][0] = (half + 55) * Math.cos(turn) * rough;
			hood[point][1] = foot ? 0 : (cave.height() + 60) * Math.sin(turn) * rough;
			innerFront[point] = cave.at(arch[point][0], LEDGE_HEIGHT + arch[point][1], front);
			innerBack[point] = cave.at(arch[point][0] * 0.72, LEDGE_HEIGHT + arch[point][1] * 0.72, back);
			outerFront[point] = cave.at(hood[point][0], LEDGE_HEIGHT + hood[point][1], front);
			outerBack[point] = cave.at(hood[point][0] * 1.2, LEDGE_HEIGHT + hood[point][1] * 1.25, back + 20);
		}
		double[] toField = { -Math.cos(cave.angle()), 0, -Math.sin(cave.angle()) };
		SurfaceMesh inside = new SurfaceMesh();
		SurfaceMesh outside = new SurfaceMesh();
		SurfaceMesh dark = new SurfaceMesh();
		double[] axisFront = cave.at(0, LEDGE_HEIGHT + cave.height() * 0.4, front);
		double[] axisBack = cave.at(0, LEDGE_HEIGHT + cave.height() * 0.3, back);
		double[] footFront = cave.at(0, LEDGE_HEIGHT, front);
		double[] footBack = cave.at(0, LEDGE_HEIGHT, back);
		for (int point = 0; point < ARCH; point++) {
			float around = (float) point / ARCH;
			float next = (float) (point + 1) / ARCH;
			// Inside, facing the middle of the arch: it darkens toward the back
			inside.quad(innerFront[point], innerFront[point + 1], innerBack[point + 1], innerBack[point], new float[] { 0, around, 0, next, 1, next, 1, around },
					toward(middle(innerFront[point], innerBack[point + 1]), middle(axisFront, axisBack)));
			// Outside, facing away from the arch, and the front of the arch, facing the field
			float[] hoodCoords = { uOf(hood[point][0]), vOf(hood[point][1]), uOf(hood[point + 1][0]), vOf(hood[point + 1][1]), uOf(hood[point + 1][0] * 1.2),
					vOf(hood[point + 1][1] * 1.25), uOf(hood[point][0] * 1.2), vOf(hood[point][1] * 1.25) };
			outside.quad(outerFront[point], outerFront[point + 1], outerBack[point + 1], outerBack[point], hoodCoords,
					toward(middle(footFront, footBack), middle(outerFront[point], outerBack[point + 1])));
			float[] faceCoords = { uOf(arch[point][0]), vOf(arch[point][1]), uOf(arch[point + 1][0]), vOf(arch[point + 1][1]), uOf(hood[point + 1][0]), vOf(hood[point + 1][1]),
					uOf(hood[point][0]), vOf(hood[point][1]) };
			outside.quad(innerFront[point], innerFront[point + 1], outerFront[point + 1], outerFront[point], faceCoords, toField);
			// The dark of the back
			dark.triangle(axisBack, innerBack[point], innerBack[point + 1], new float[] { 0, 0, 0, 0, 0, 0 }, toField);
		}
		dark.triangle(axisBack, innerBack[ARCH], innerBack[0], new float[] { 0, 0, 0, 0, 0, 0 }, toField);
		// The floor, a little above the ledge
		double floor = LEDGE_HEIGHT + 0.6;
		inside.quad(cave.at(arch[0][0], floor, front), cave.at(arch[ARCH][0], floor, front), cave.at(arch[ARCH][0] * 0.72, floor, back), cave.at(arch[0][0] * 0.72, floor, back),
				new float[] { 0, 0, 0, 1, 1, 1, 1, 0 }, UP);
		PhongMaterial deep = new PhongMaterial(Color.WHITE);
		deep.setDiffuseMap(VolcanoTexture.CAVE);
		deep.setSelfIlluminationMap(VolcanoTexture.CAVE_GLOW);
		deep.setSpecularColor(Color.BLACK);
		PhongMaterial blackness = new PhongMaterial(Color.BLACK);
		blackness.setSpecularColor(Color.BLACK);
		blackness.setSelfIlluminationMap(ArenaHelpers.solidMap(Color.web("#03100d")));
		this.host.arena.getChildren().addAll(inside.view(deep), outside.view(rock), dark.view(blackness));
		this.boulders(cave, front, back, wild);
		this.fungi(cave, front, wild);
		this.bats(cave, front, wild);
	}

	/** Where the texture of the rock is across a cave, for a point so far across its mouth. */
	private static float uOf(double across) {
		return (float) (across / 350);
	}

	/** Where the texture of the rock is up a cave, for a point so high above its ledge. */
	private static float vOf(double up) {
		return (float) VolcanoTexture.rockV(LEDGE_HEIGHT + up);
	}

	/** The point halfway between two others. */
	private static double[] middle(double[] first, double[] second) {
		return new double[] { (first[0] + second[0]) / 2, (first[1] + second[1]) / 2, (first[2] + second[2]) / 2 };
	}

	/** The direction from one point to another. */
	private static double[] toward(double[] from, double[] to) {
		return new double[] { to[0] - from[0], to[1] - from[1], to[2] - from[2] };
	}

	/** Boulders fallen around the mouth of a cave, at its sides and over its arch, so its edges are rough as the cliff. */
	private void boulders(Cave cave, double front, double back, Random wild) {
		LowPoly stones = new LowPoly();
		for (int side = -1; side <= 1; side += 2) {
			for (int stone = 0; stone < 3; stone++) {
				double[] spot = cave.at(side * (cave.width() / 2 + 40 + wild.nextDouble() * 50), 0, front + wild.nextDouble() * (back - front + 10));
				double size = 22 + wild.nextDouble() * 28;
				stones.rock(spot[0], spot[2], -LEDGE_HEIGHT, size * (1.2 + wild.nextDouble()), size, size * 0.9, 6, 0.55, 0.5, wild);
			}
		}
		for (int stone = 0; stone < 3; stone++) {
			double[] spot = cave.at((wild.nextDouble() - 0.5) * cave.width() * 0.8, 0, front + 15 + wild.nextDouble() * 40);
			double size = 26 + wild.nextDouble() * 22;
			stones.rock(spot[0], spot[2], -(LEDGE_HEIGHT + cave.height() + 35), size * 1.1, size * 1.3, size, 6, 0.6, 0.5, wild);
		}
		PhongMaterial stone = new PhongMaterial(Color.web("#2e2420"));
		stone.setSpecularColor(Color.web("#1a1412"));
		this.host.arena.getChildren().add(stones.view(stone));
	}

	/** The fungi that glow blue green in the dark of a cave, and a few at its mouth: the light, and the food, of the life inside. */
	private void fungi(Cave cave, double front, Random wild) {
		LowPoly cyan = new LowPoly();
		LowPoly green = new LowPoly();
		for (int fungus = 0; fungus < 14; fungus++) {
			boolean outside = fungus >= 10;
			double deep = outside ? -(10 + wild.nextDouble() * 30) : (0.3 + wild.nextDouble() * 0.65) * CAVE_DEPTH;
			double narrow = outside ? 1 : 1 - 0.28 * deep / CAVE_DEPTH;
			double across = (wild.nextBoolean() ? 1 : -1) * cave.width() / 2 * narrow * (outside ? 1.1 + wild.nextDouble() * 0.3 : 0.55 + wild.nextDouble() * 0.4);
			double[] spot = cave.at(across, 0, front + deep);
			double size = 2.5 + wild.nextDouble() * 4;
			LowPoly kind = wild.nextDouble() < 0.65 ? cyan : green;
			double ground = -(LEDGE_HEIGHT + 0.6);
			kind.cylinderY(spot[0], spot[2], size * 0.18, ground, ground - size, 5);
			kind.cylinderY(spot[0], spot[2], size * 0.75, ground - size, ground - size * 1.3, 7);
		}
		this.host.arena.getChildren().addAll(cyan.view(glow("#58f5d2")), green.view(glow("#a6f56a")));
	}

	/** A material that shines by itself in the color. */
	private static PhongMaterial glow(String color) {
		PhongMaterial light = new PhongMaterial(Color.BLACK);
		light.setSpecularColor(Color.BLACK);
		light.setSelfIlluminationMap(ArenaHelpers.solidMap(Color.web(color)));
		return light;
	}

	/** The bats of a cave, circling in front of its mouth, a little above it. */
	private void bats(Cave cave, double front, Random wild) {
		LowPoly up = new LowPoly();
		LowPoly down = new LowPoly();
		for (int bat = 0; bat < BATS; bat++) {
			double turn = wild.nextDouble() * Math.PI * 2;
			double reach = 35 + wild.nextDouble() * 85;
			double height = (wild.nextDouble() - 0.5) * 70;
			double[] body = { Math.cos(turn) * reach, height, Math.sin(turn) * reach };
			up.box(body[0], body[1], body[2], 2.6, 2.6, 2.6);
			down.box(body[0], body[1], body[2], 2.6, 2.6, 2.6);
			// The wings go out across the way the bat flies, which is around the flock
			for (int side = -1; side <= 1; side += 2) {
				double tipX = body[0] + Math.cos(turn) * side * 7.5;
				double tipZ = body[2] + Math.sin(turn) * side * 7.5;
				up.tube(body, new double[] { tipX, body[1] - 3.5, tipZ }, 1.1, 3);
				down.tube(body, new double[] { tipX, body[1] + 2.5, tipZ }, 1.1, 3);
			}
		}
		PhongMaterial skin = new PhongMaterial(Color.web("#1e1416"));
		skin.setSpecularColor(Color.BLACK);
		double[] center = cave.at(0, LEDGE_HEIGHT + cave.height() + 40, front - 90);
		Rotate rotate = new Rotate(wild.nextDouble() * 360, Rotate.Y_AXIS);
		MeshView wingsUp = up.view(skin);
		MeshView wingsDown = down.view(skin);
		for (MeshView view : List.of(wingsUp, wingsDown)) {
			view.getTransforms().addAll(new Translate(center[0], center[1], center[2]), rotate);
		}
		this.host.arena.getChildren().addAll(wingsUp, wingsDown);
		this.swarms.add(new Swarm(wingsUp, wingsDown, rotate, (wild.nextBoolean() ? 1 : -1) * (25 + wild.nextDouble() * 20), wild.nextDouble()));
	}

	/**
	 * Hot springs on the ledge beside some caves: water the volcano warms, with the yellow and orange crust of the minerals around it.
	 */
	private void springs(Random wild) {
		LowPoly crust = new LowPoly();
		LowPoly water = new LowPoly();
		for (Cave cave : this.caves) {
			if (wild.nextDouble() < 0.4) {
				continue;
			}
			double side = wild.nextBoolean() ? 1 : -1;
			double angle = cave.angle() + side * (cave.width() / 2 + 90 + wild.nextDouble() * 40) / cave.radius();
			double ledge = this.ledge(angle);
			double radius = Math.min(38, ledge * 0.38);
			double distance = shore(angle) + ledge * 0.5;
			double posX = Math.cos(angle) * distance;
			double posZ = Math.sin(angle) * distance;
			crust.cylinderY(posX, posZ, radius * 1.3, -LEDGE_HEIGHT + 0.5, -LEDGE_HEIGHT - 0.6, 12);
			water.cylinderY(posX, posZ, radius, -LEDGE_HEIGHT, -LEDGE_HEIGHT - 0.9, 12);
		}
		PhongMaterial minerals = new PhongMaterial(Color.web("#b0702a"));
		minerals.setSpecularColor(Color.web("#3a2a10"));
		PhongMaterial pool = new PhongMaterial(Color.web("#1a6a6a"));
		pool.setSpecularColor(Color.web("#9ae0e0"));
		pool.setSpecularPower(60);
		pool.setSelfIlluminationMap(ArenaHelpers.solidMap(Color.web("#0b3c3e")));
		this.host.arena.getChildren().addAll(crust.view(minerals), water.view(pool));
	}

	/**
	 * Islands of basalt columns in the lake, between the plateau and the volcanoes: hexagons packed side by side, the highest in the
	 * middle, as the lava cooled and cracked.
	 */
	private void islands(double width, double height, Random wild) {
		LowPoly columns = new LowPoly();
		int placed = 0;
		for (int attempt = 0; attempt < 200 && placed < 7; attempt++) {
			double angle = wild.nextDouble() * Math.PI * 2;
			double distance = 950 + wild.nextDouble() * 450;
			double posX = Math.cos(angle) * distance;
			double posZ = Math.sin(angle) * distance;
			double size = 50 + wild.nextDouble() * 50;
			if (!this.clearOfAll(posX, posZ, size, width, height)) {
				continue;
			}
			placed++;
			double column = 11 + wild.nextDouble() * 5;
			double top = 70 + wild.nextDouble() * 170;
			double pitchX = column * 1.75;
			double pitchZ = pitchX * 0.866;
			for (int row = (int) -Math.ceil(size / pitchZ); row <= Math.ceil(size / pitchZ); row++) {
				for (int col = (int) -Math.ceil(size / pitchX); col <= Math.ceil(size / pitchX); col++) {
					double atX = col * pitchX + (Math.floorMod(row, 2) == 1 ? pitchX / 2 : 0);
					double atZ = row * pitchZ;
					double from = Math.hypot(atX, atZ) / size;
					if (from > 1) {
						continue;
					}
					double rise = top * (1 - 0.6 * from * from) * (0.7 + 0.4 * wild.nextDouble());
					columns.cylinderY(posX + atX, posZ + atZ, column, 10, -rise, 6);
				}
			}
		}
		PhongMaterial basalt = new PhongMaterial(Color.web("#3a3436"));
		basalt.setSpecularColor(Color.web("#4a4446"));
		basalt.setSpecularPower(12);
		this.host.arena.getChildren().add(columns.view(basalt));
	}

	/** Whether an island of the size can be at the spot: off the plateau and its rivers, away from the volcanoes and their falls. */
	private boolean clearOfAll(double posX, double posZ, double size, double width, double height) {
		if (Math.abs(posX) < width / 2 + 380 + size + 60 && Math.abs(posZ) < height / 2 + 380 + size + 60) {
			return false;
		}
		for (Peak peak : this.peaks) {
			if (Math.hypot(posX - peak.posX(), posZ - peak.posZ()) < peak.radius() * 1.6 + size + 60) {
				return false;
			}
		}
		return true;
	}

	/** The point of the axis of the cave that the camera looks at, for the tour. */
	static double[] mouth(Cave cave) {
		return cave.at(0, LEDGE_HEIGHT + cave.height() * 0.5, cave.radius() - CAVE_DEPTH * 0.5);
	}
}
