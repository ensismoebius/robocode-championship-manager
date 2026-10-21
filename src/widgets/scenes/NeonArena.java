package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The night city of the neon arena: the floor with its grid, the towers, the
 * sun, the streets with their cars and the scanner that sweeps the field. It
 * builds into the arena of its host and animates it every frame.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class NeonArena {

	private final BattleArena3D host;
	private final List<PhongMaterial> pulseMaterials = new ArrayList<PhongMaterial>();
	private final List<Color> pulseColors = new ArrayList<Color>();
	private final List<Double> pulsePhases = new ArrayList<Double>();
	private double neonClock;
	private final List<Car> cars = new ArrayList<>();
	private Box scanner;

	NeonArena(BattleArena3D host) {
		this.host = host;
	}

	/** A material that is the color, lit by the glow light of the arena */
	private PhongMaterial neonGlow(Color color, boolean pulse, Random random) {
		PhongMaterial material = new PhongMaterial(color);
		if (pulse) {
			this.pulseMaterials.add(material);
			this.pulseColors.add(color);
			this.pulsePhases.add(random.nextDouble() * Math.PI * 2);
		}
		return material;
	}

	/**
	 * Night city: a circuit floor with a grid, pads in the corners and pylons; around it, towers with lit windows, a grid to the horizon
	 * and a setting sun under a violet sky
	 */
	void build(double w, double h) {
		Random random = new Random(8);
		this.pulseMaterials.clear();
		this.pulseColors.clear();
		this.pulsePhases.clear();

		host.arena.getChildren().add(ArenaMaterials.skyDome(NeonTexture.SKY));
		this.sun(w, h, random);
		this.floor(w, h, random);
		this.grid(w, h, random);
		this.edges(w, h, random);
		this.pads(w, h, random);
		this.scanner(w, h, random);
		this.pylons(w, h, random);
		this.far(w, h, random);
		this.city(w, h, random);
	}

	/** The sun, far away, over a violet horizon */
	private void sun(double w, double h, Random random) {
		// The sun, far away, facing the middle
		double sunX = -0.78 * 7200;
		double sunZ = -0.62 * 7200;
		double sunSize = 1250;
		double rightX = 0.62;
		double rightZ = -0.78;
		PhongMaterial sun = new PhongMaterial(Color.BLACK);
		sun.setDiffuseMap(NeonTexture.SUN);
		sun.setSelfIlluminationMap(NeonTexture.SUN);
		TriangleMesh sunMesh = new TriangleMesh();
		sunMesh.getPoints().addAll((float) (sunX - rightX * sunSize), (float) (-950 - sunSize), (float) (sunZ - rightZ * sunSize), (float) (sunX + rightX * sunSize),
				(float) (-950 - sunSize), (float) (sunZ + rightZ * sunSize), (float) (sunX + rightX * sunSize), (float) (-950 + sunSize), (float) (sunZ + rightZ * sunSize),
				(float) (sunX - rightX * sunSize), (float) (-950 + sunSize), (float) (sunZ - rightZ * sunSize));
		sunMesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
		sunMesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
		host.arena.getChildren().add(host.meshView(sunMesh, sun));
	}
	/** The floor: a dark slab with a panel texture, and the ground around it */
	private void floor(double w, double h, Random random) {
		Box ground = new Box(9000, 2, 9000);
		ground.setMaterial(ArenaMaterials.material(Color.web("#07080f")));
		ground.setTranslateY(6);
		host.arena.getChildren().add(ground);

		// The floor: a slab with a panel texture over it
		Box floor = new Box(w + 40, 4.4, h + 40);
		floor.setMaterial(ArenaMaterials.material(Color.web("#0d1020")));
		floor.setTranslateY(2.8);
		host.arena.getChildren().add(floor);
		PhongMaterial panels = ArenaMaterials.textured(NeonTexture.FLOOR, Color.gray(0.3));
		panels.setSpecularPower(34);
		host.arena.getChildren().add(host.tiledQuad(new double[][] { { -w / 2 - 20, 0, -h / 2 - 20 }, { w / 2 + 20, 0, -h / 2 - 20 }, { w / 2 + 20, 0, h / 2 + 20 },
				{ -w / 2 - 20, 0, h / 2 + 20 } }, panels, NeonTexture.FLOOR_TILE));
	}
	/** The grid of the floor: thin lines every 50, brighter every 200 */
	private void grid(double w, double h, Random random) {
		// The grid: thin lines every 50, brighter every 200
		TriangleMesh minor = new TriangleMesh();
		TriangleMesh major = new TriangleMesh();
		for (double x = 0; x <= w; x += 50) {
			NeonCity.box(x % 200 == 0 ? major : minor, x - w / 2, -0.3, 0, x % 200 == 0 ? 2.6 : 1.2, 0.5, h);
		}
		for (double y = 0; y <= h; y += 50) {
			NeonCity.box(y % 200 == 0 ? major : minor, 0, -0.3, y - h / 2, w, 0.5, y % 200 == 0 ? 2.6 : 1.2);
		}
		host.glowing.getChildren().add(host.meshView(minor, this.neonGlow(Color.web("#157fae"), false, random)));
		host.glowing.getChildren().add(host.meshView(major, this.neonGlow(Color.web("#2ec4ff"), true, random)));
	}
	/** The edge of the field and the curbs around it, lit */
	private void edges(double w, double h, Random random) {
		// The edge of the field and a strip of light just outside it
		TriangleMesh edge = new TriangleMesh();
		NeonCity.box(edge, 0, -0.4, -h / 2, w + 6, 0.6, 5);
		NeonCity.box(edge, 0, -0.4, h / 2, w + 6, 0.6, 5);
		NeonCity.box(edge, -w / 2, -0.4, 0, 5, 0.6, h + 6);
		NeonCity.box(edge, w / 2, -0.4, 0, 5, 0.6, h + 6);
		host.glowing.getChildren().add(host.meshView(edge, this.neonGlow(Color.web("#bff3ff"), true, random)));
		TriangleMesh curb = new TriangleMesh();
		NeonCity.box(curb, 0, -1.5, -h / 2 - 14, w + 40, 3, 4);
		NeonCity.box(curb, 0, -1.5, h / 2 + 14, w + 40, 3, 4);
		NeonCity.box(curb, -w / 2 - 14, -1.5, 0, 4, 3, h + 40);
		NeonCity.box(curb, w / 2 + 14, -1.5, 0, 4, 3, h + 40);
		host.glowing.getChildren().add(host.meshView(curb, this.neonGlow(Color.web("#ff3fa4"), true, random)));
	}
	/** Pads in the corners: a square and a corner mark inside */
	private void pads(double w, double h, Random random) {
		// Pads in the corners: a square and a corner mark inside
		TriangleMesh pads = new TriangleMesh();
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				double cx = sx * (w / 2 - 55);
				double cz = sz * (h / 2 - 55);
				NeonCity.box(pads, cx, -0.5, cz - 36, 72, 0.6, 2);
				NeonCity.box(pads, cx, -0.5, cz + 36, 72, 0.6, 2);
				NeonCity.box(pads, cx - 36, -0.5, cz, 2, 0.6, 72);
				NeonCity.box(pads, cx + 36, -0.5, cz, 2, 0.6, 72);
				NeonCity.box(pads, cx - sx * 14, -0.5, cz - sz * 14, 24, 0.6, 3);
				NeonCity.box(pads, cx - sx * 14, -0.5, cz - sz * 14, 3, 0.6, 24);
			}
		}
		host.glowing.getChildren().add(host.meshView(pads, this.neonGlow(Color.web("#2ec4ff"), true, random)));
	}
	/** Pylons in the corners, with a light on top and a vertical strip */
	private void pylons(double w, double h, Random random) {
		// Pylons in the corners, with a light on top and a vertical strip
		PhongMaterial metal = ArenaMaterials.material(Color.web("#171c32"));
		TriangleMesh pylonLights = new TriangleMesh();
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				double px = sx * (w / 2 + 16);
				double pz = sz * (h / 2 + 16);
				host.arena.getChildren().add(ArenaMaterials.box(18, 74, 18, px, -37, pz, metal));
				NeonCity.box(pylonLights, px, -77, pz, 24, 6, 24);
				NeonCity.box(pylonLights, px - sx * 9.5, -37, pz - sz * 9.5, 3, 60, 3);
			}
		}
		host.glowing.getChildren().add(host.meshView(pylonLights, this.neonGlow(Color.web("#ff3fa4"), true, random)));
	}
	/** A grid out to the horizon, dimmer than the field, so the battle is what stands out */
	private void far(double w, double h, Random random) {
		// A grid out to the horizon, dimmer than the field, so the battle is what stands out
		TriangleMesh far = new TriangleMesh();
		double reach = 3600;
		for (int j = 1; j <= 35; j++) {
			for (int sign = -1; sign <= 1; sign += 2) {
				NeonCity.box(far, sign * (w / 2 + 100 * j), 4.6, 0, 3, 0.8, reach * 2);
				NeonCity.box(far, 0, 4.6, sign * (h / 2 + 100 * j), reach * 2, 0.8, 3);
			}
		}
		double continuation = reach - h / 2;
		for (double x = -w / 2; x <= w / 2 + 1; x += 100) {
			NeonCity.box(far, x, 4.6, -(h / 2 + continuation / 2), 3, 0.8, continuation);
			NeonCity.box(far, x, 4.6, h / 2 + continuation / 2, 3, 0.8, continuation);
		}
		double continuationX = reach - w / 2;
		for (double z = -h / 2; z <= h / 2 + 1; z += 100) {
			NeonCity.box(far, -(w / 2 + continuationX / 2), 4.6, z, continuationX, 0.8, 3);
			NeonCity.box(far, w / 2 + continuationX / 2, 4.6, z, continuationX, 0.8, 3);
		}
		host.glowing.getChildren().add(host.meshView(far, this.neonGlow(Color.web("#07283a"), false, random)));
	}
	/** The skyline: towers with lit windows and neon crowns, and the streets between them */
	private void city(double w, double h, Random random) {
		// The skyline: towers with lit windows and neon crowns
		// The streets of the city, between the towers
		double[] roadsX = NeonCity.roads(w / 2);
		double[] roadsZ = NeonCity.roads(h / 2);
		this.buildRoads(roadsX, roadsZ, random);
		NeonCity city = new NeonCity();
		city.skyline(48, random, roadsX, roadsZ);
		PhongMaterial facade = new PhongMaterial(Color.gray(0.6));
		facade.setDiffuseMap(NeonTexture.FACADE);
		facade.setSelfIlluminationMap(NeonTexture.FACADE_GLOW);
		host.arena.getChildren().add(host.meshView(city.facades, facade));
		host.glowing.getChildren().add(host.meshView(city.cyan, this.neonGlow(Color.web("#2ec4ff"), false, random)));
		host.glowing.getChildren().add(host.meshView(city.magenta, this.neonGlow(Color.web("#ff3fa4"), false, random)));
	}

	/** A scanner: a line of light that sweeps the field */
	private void scanner(double w, double h, Random random) {
		// A scanner: a line of light that sweeps the field
		this.scanner = ArenaMaterials.box(5, 0.6, h, -w / 2, -0.6, 0, this.neonGlow(Color.web("#ff3fa4"), false, random));
		host.glowing.getChildren().add(this.scanner);
	}

	/** A car on a street of the neon city: a light that goes along it, and comes back at the end */
	private record Car(Box node, boolean alongZ, double lane, double speed, double[] position) {
	}

	/**
	 * The streets of the neon city: dark asphalt with lines of light on the edges
	 * and in the middle, and cars, small lights, driving along them
	 *
	 * @param roadsX
	 *            the x of the streets that run along z
	 * @param roadsZ
	 *            the z of the streets that run along x
	 */
	private void buildRoads(double[] roadsX, double[] roadsZ, Random random) {
		double reach = NeonCity.ROAD_REACH;
		double half = NeonCity.ROAD_WIDTH / 2;
		PhongMaterial asphalt = ArenaMaterials.material(Color.web("#12162c"));
		TriangleMesh edges = new TriangleMesh();
		TriangleMesh dashes = new TriangleMesh();
		// The streets that run along x are a little above those that run along z, so where they cross there is no flicker
		for (double z : roadsZ) {
			host.arena.getChildren().add(ArenaMaterials.box(2 * reach, 1, NeonCity.ROAD_WIDTH, 0, 4.5, z, asphalt));
			for (double[] part : gaps(roadsX, -reach, reach)) {
				double middle = (part[0] + part[1]) / 2;
				NeonCity.box(edges, middle, 3.6, z - half + 3, part[1] - part[0], 0.5, 2.4);
				NeonCity.box(edges, middle, 3.6, z + half - 3, part[1] - part[0], 0.5, 2.4);
			}
			for (double x = -reach + 30; x < reach - 30; x += 52) {
				if (!near(x, roadsX, half + 10)) {
					NeonCity.box(dashes, x, 3.6, z, 24, 0.5, 2.4);
				}
			}
		}
		for (double x : roadsX) {
			host.arena.getChildren().add(ArenaMaterials.box(NeonCity.ROAD_WIDTH, 1, 2 * reach, x, 4.0, 0, asphalt));
			for (double[] part : gaps(roadsZ, -reach, reach)) {
				double middle = (part[0] + part[1]) / 2;
				NeonCity.box(edges, x - half + 3, 3.1, middle, 3.4, 0.5, part[1] - part[0]);
				NeonCity.box(edges, x + half - 3, 3.1, middle, 2.4, 0.5, part[1] - part[0]);
			}
			for (double z = -reach + 30; z < reach - 30; z += 52) {
				if (!near(z, roadsZ, half + 10)) {
					NeonCity.box(dashes, x, 3.1, z, 2.4, 0.5, 24);
				}
			}
		}
		host.glowing.getChildren().add(host.meshView(edges, this.neonGlow(Color.web("#1fa8e0"), true, random)));
		host.glowing.getChildren().add(host.meshView(dashes, this.neonGlow(Color.web("#d63fa0"), false, random)));

		// Cars: white lights going one way, magenta ones coming back
		PhongMaterial going = this.neonGlow(Color.web("#e6fbff"), false, random);
		PhongMaterial coming = this.neonGlow(Color.web("#ff3fa4"), false, random);
		for (int axis = 0; axis < 2; axis++) {
			double[] roads = axis == 0 ? roadsX : roadsZ;
			for (double road : roads) {
				for (int i = 0; i < 8; i++) {
					boolean forward = i % 2 == 0;
					double lane = road + (forward ? -17 : 17);
					double start = (random.nextDouble() * 2 - 1) * reach;
					double speed = (forward ? 1 : -1) * (130 + random.nextDouble() * 140);
					double y = axis == 0 ? 2.2 : 1.7;
					Box car = axis == 0 ? ArenaMaterials.box(10, 3.6, 28, lane, y, start, forward ? going : coming) : ArenaMaterials.box(28, 3.6, 10, start, y, lane, forward ? going : coming);
					host.glowing.getChildren().add(car);
					this.cars.add(new Car(car, axis == 0, lane, speed, new double[] { start }));
				}
			}
		}
	}

	/** The stretches of a street, from one end to the other, left between the streets that cross it */
	private static List<double[]> gaps(double[] crossing, double from, double to) {
		List<double[]> parts = new ArrayList<>();
		double start = from;
		double clear = NeonCity.ROAD_WIDTH / 2 + 3;
		for (double cross : java.util.Arrays.stream(crossing).sorted().toArray()) {
			parts.add(new double[] { start, cross - clear });
			start = cross + clear;
		}
		parts.add(new double[] { start, to });
		return parts;
	}

	private static boolean near(double coordinate, double[] roads, double distance) {
		for (double road : roads) {
			if (Math.abs(coordinate - road) < distance) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The lights of the neon arena breathe, out of step, and the scanner sweeps
	 */
	void update(double dt) {
		if (this.pulseMaterials.isEmpty()) {
			return;
		}
		this.neonClock += dt;
		for (Car car : this.cars) {
			double reach = NeonCity.ROAD_REACH;
			double at = car.position()[0] + car.speed() * dt;
			// At the end of the street the car comes back at the other end
			if (at > reach) {
				at -= 2 * reach;
			} else if (at < -reach) {
				at += 2 * reach;
			}
			car.position()[0] = at;
			if (car.alongZ()) {
				car.node().setTranslateZ(at);
			} else {
				car.node().setTranslateX(at);
			}
		}
		for (int i = 0; i < this.pulseMaterials.size(); i++) {
			double breath = 0.5 + 0.5 * Math.sin(this.neonClock * 1.3 + this.pulsePhases.get(i));
			this.pulseMaterials.get(i).setDiffuseColor(this.pulseColors.get(i).deriveColor(0, 1, 0.45 + 0.55 * breath, 1));
		}
		if (this.scanner != null) {
			this.scanner.setTranslateX(((this.neonClock / 7) % 1 - 0.5) * (host.fieldWidth + 20));
		}
	}
}
