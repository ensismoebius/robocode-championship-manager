package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The city around the Eden Solar stadium: a solarpunk African city of the year 2187, laid out in rings around the stadium
 * and cut by wide avenues paved with kente. A canal of water goes round the stadium; beyond it the first ring is a quarter
 * of round houses with solar domes, and every ring after it rises higher, with towers of green terraces, towers of mud,
 * baobab towers and towers of glass more than a thousand units tall, until four great baobabs stand over the rest. Solar trees line the avenues, glass bridges join the tall towers
 * and a far skyline, in the haze, closes the horizon. The rings follow the plan of the stadium, which swells like a kidney.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenCity implements FlyTour.Clearance {

	/** The width of the canal and of its two banks, and how far from the stadium they start. */
	private static final double MOAT = 150;
	/** How far from the wall of the stadium the first ring starts, how deep a ring is, and how wide a lot is. */
	private static final double GAP = 260;
	private static final double STEP = 440;
	private static final double LOT = 470;
	static final int RINGS = 5;
	/** The ring where the four great baobabs stand. */
	private static final int GRAND_RING = 3;
	/** The avenues: how many, and how wide. */
	private static final int AVENUES = 12;
	private static final double AVENUE = 56;
	private static final double[] HEIGHTS = { 90, 380, 720, 1050, 1350 };
	/** How tall the four great baobabs are, and how much room the camera keeps above and around everything. */
	private static final double GRAND_HEIGHT = 1500;
	private static final double ROOM = 70;
	/** How far around a great baobab, along its ring and the ones after it, the lots are only groves: a park, where the camera circles it. */
	private static final double PARK = 700;
	private static final double[] UP = { 0, -1, 0 };

	private final BattleArena3D host;
	/** The radius of the outer wall of the stadium, before its plan swells. */
	private final double edge;
	private final EdenForms forms = new EdenForms();
	/** The tall towers, each {x, z, height, ring, lot}, to join with bridges. */
	private final List<double[]> towers = new ArrayList<>();
	private final LowPoly haze = new LowPoly();
	/** The great baobabs, each {x, z, height}. */
	private final List<double[]> grand = new ArrayList<>();
	/** What the camera must not go into, each {x, z, radius, top, room}: the top is a height, negative up, and the room is kept around it. */
	private final List<double[]> solids = new ArrayList<>();

	/** The city around a stadium whose outer wall is at the given radius, drawn in the host scene. */
	EdenCity(BattleArena3D host, double edge) {
		this.host = host;
		this.edge = edge;
	}

	/** The great baobabs of the city, each {x, z, height}: none before it is built. */
	List<double[]> grandBaobabs() {
		return this.grand;
	}

	/** The angle of the avenue nearest to the angle given. */
	static double nearestAvenue(double angle) {
		double sector = Math.PI * 2 / AVENUES;
		return avenueAngle(0) + Math.round((angle - avenueAngle(0)) / sector) * sector;
	}

	/** Where the avenues begin, beside the canal, and where they end, at the last ring: radii before the plan swells. */
	double[] avenueReach() {
		return new double[] { this.edge + 20, this.edge + GAP + RINGS * STEP };
	}

	/** The forms built so far, for the tests. */
	EdenForms forms() {
		return this.forms;
	}

	/** Builds the canal, the avenues, the houses and towers, the trees, the bridges and the far skyline. */
	void build(Random wild) {
		this.canal();
		this.avenues();
		this.quarters(wild);
		this.bridges(wild);
		this.skyline(wild);
		host.arena.getChildren().addAll(this.forms.views());
		PhongMaterial fog = new PhongMaterial(Color.web("#566f7b"));
		fog.setSpecularColor(Color.BLACK);
		host.arena.getChildren().add(this.haze.view(fog));
	}

	/** The radius, before the plan swells, where a ring has its middle. */
	private double middleOf(int ring) {
		return this.edge + GAP + (ring + 0.5) * STEP;
	}

	/** How many lots a ring has: a multiple of the number of avenues, so every sector has the same. */
	private int lotsIn(int ring) {
		double around = Math.PI * 2 * this.middleOf(ring) * 1.28 / LOT;
		return AVENUES * Math.max(2, (int) Math.round(around / AVENUES));
	}

	/** The angle of the middle of an avenue. */
	private static double avenueAngle(int avenue) {
		return Math.PI * 2 * (avenue + 0.5) / AVENUES;
	}

	/** Whether a lot of the given size, at the angle and radius, would touch an avenue. */
	private static boolean onAnAvenue(double angle, double radius, double size) {
		double sector = Math.PI * 2 / AVENUES;
		double offset = ((angle - avenueAngle(0)) % sector + sector) % sector;
		double nearest = Math.min(offset, sector - offset);
		return nearest * radius * (1 + EdenStadium.swell(angle)) < AVENUE / 2 + size / 2 + 8;
	}

	/** A band of the ground between two radii, all the way around, with the picture along it. */
	private void band(double inner, double outer, double height, PhongMaterial material) {
		SurfaceMesh mesh = new SurfaceMesh();
		int steps = 192;
		for (int step = 0; step < steps; step++) {
			double first = Math.PI * 2 * step / steps;
			double second = Math.PI * 2 * (step + 1) / steps;
			double[] nearFirst = EdenStadium.point(first, inner, height);
			double[] nearSecond = EdenStadium.point(second, inner, height);
			double[] farSecond = EdenStadium.point(second, outer, height);
			double[] farFirst = EdenStadium.point(first, outer, height);
			float run = (float) (Math.hypot(nearSecond[0] - nearFirst[0], nearSecond[2] - nearFirst[2]) / 128);
			float start = run * step;
			float width = (float) (Math.hypot(farFirst[0] - nearFirst[0], farFirst[2] - nearFirst[2]) / 64);
			mesh.quad(nearFirst, nearSecond, farSecond, farFirst, new float[] { start, 0, start + run, 0, start + run, width, start, width }, UP);
		}
		host.arena.getChildren().add(mesh.view(material));
	}

	/** A material with a picture and the light of the picture. */
	private static PhongMaterial glowingPicture(javafx.scene.image.Image picture, javafx.scene.image.Image light) {
		PhongMaterial material = ArenaMaterials.textured(picture, Color.gray(0.1));
		material.setSelfIlluminationMap(light);
		return material;
	}

	/** The canal around the stadium, with the bank of kente on each side of it. */
	private void canal() {
		PhongMaterial water = ArenaMaterials.textured(EdenCityTexture.WATER, Color.gray(0.6));
		water.setSelfIlluminationMap(EdenCityTexture.WATER_LIGHT);
		PhongMaterial kente = glowingPicture(EdenCityTexture.KENTE, EdenCityTexture.KENTE_LIGHT);
		this.band(this.edge + 60, this.edge + 60 + MOAT, EdenForms.GROUND - 2, water);
		this.band(this.edge + 30, this.edge + 60, EdenForms.GROUND - 2.5, kente);
		this.band(this.edge + 60 + MOAT, this.edge + 90 + MOAT, EdenForms.GROUND - 2.5, kente);
	}

	/** The avenues, wide and paved with kente, from the stadium out through every ring, with the ring roads between the rings. */
	private void avenues() {
		PhongMaterial kente = glowingPicture(EdenCityTexture.KENTE, EdenCityTexture.KENTE_LIGHT);
		for (int ring = 1; ring < RINGS; ring++) {
			double road = this.edge + GAP + ring * STEP;
			this.band(road - AVENUE / 2, road + AVENUE / 2, EdenForms.GROUND - 3, kente);
		}
		SurfaceMesh paving = new SurfaceMesh();
		double start = this.edge + 20;
		double end = this.edge + GAP + RINGS * STEP;
		int pieces = 14;
		for (int avenue = 0; avenue < AVENUES; avenue++) {
			double angle = avenueAngle(avenue);
			double[] across = { -Math.sin(angle) * AVENUE / 2, 0, Math.cos(angle) * AVENUE / 2 };
			for (int piece = 0; piece < pieces; piece++) {
				double[] near = EdenStadium.point(angle, start + (end - start) * piece / pieces, EdenForms.GROUND - 3.5);
				double[] far = EdenStadium.point(angle, start + (end - start) * (piece + 1) / pieces, EdenForms.GROUND - 3.5);
				float run = (float) (Math.hypot(far[0] - near[0], far[2] - near[2]) / 128);
				paving.quad(new double[] { near[0] - across[0], near[1], near[2] - across[2] }, new double[] { far[0] - across[0], far[1], far[2] - across[2] }, new double[] { far[0] + across[0], far[1], far[2] + across[2] },
						new double[] { near[0] + across[0], near[1], near[2] + across[2] }, new float[] { 0, run * piece, 0.9f, run * piece, 0.9f, run * (piece + 1), 0, run * (piece + 1) }, UP);
			}
		}
		host.arena.getChildren().add(paving.view(kente));
	}

	/** The kind of building of a lot, by the ring and a throw of the dice. */
	private enum Kind {
		HOUSES, TERRACE, SPIRE, BAOBAB, SKYSCRAPER, GROVE
	}

	/** The kind of building of a lot of the ring, from a throw of the dice from 0 to 1, by the odds of each ring: groves are what is left. */
	private static Kind kindOf(int ring, double dice) {
		double[][] odds = { { 0.75, 0.1, 0, 0, 0, 0.92 }, { 0.2, 0.35, 0.15, 0, 0.2, 0.9 }, { 0, 0.3, 0.1, 0.15, 0.33, 0.9 }, { 0, 0.25, 0.08, 0.25, 0.4, 0.9 }, { 0, 0.2, 0.05, 0.3, 0.45, 0.92 } };
		double total = 0;
		for (int kind = 0; kind < 5; kind++) {
			total += odds[ring][kind];
			if (dice * odds[ring][5] < total) {
				return Kind.values()[kind];
			}
		}
		return Kind.GROVE;
	}

	/** The rings of lots, with what each one has: houses near the stadium, towers that rise higher ring after ring, groves in some lots, and the great baobabs. */
	private void quarters(Random wild) {
		List<Double> parks = new ArrayList<>();
		int grandLots = this.lotsIn(GRAND_RING);
		for (int lot = 0; lot < grandLots; lot++) {
			if (this.isGrandBaobab(lot)) {
				parks.add(Math.PI * 2 * (lot + 0.5) / grandLots + Math.PI / grandLots);
			}
		}
		for (int ring = 0; ring < RINGS; ring++) {
			int lots = this.lotsIn(ring);
			double radius = this.middleOf(ring);
			for (int lot = 0; lot < lots; lot++) {
				double angle = Math.PI * 2 * (lot + 0.5) / lots + (ring % 2 == 0 ? 0 : Math.PI / lots) + 0.1 * wild.nextDouble();
				double reach = radius * (1 + EdenStadium.swell(angle));
				double size = Math.min(LOT, Math.PI * 2 * reach / lots) * (0.5 + 0.2 * wild.nextDouble());
				if (ring == GRAND_RING && this.isGrandBaobab(lot)) {
					// The great baobab stands where it was meant to, between two avenues
					angle = Math.PI * 2 * (lot + 0.5) / lots + Math.PI / lots;
				} else if (onAnAvenue(angle, radius, size)) {
					continue;
				}
				double[] spot = EdenStadium.point(angle, radius, 0);
				boolean park = ring >= GRAND_RING - 1 && !(ring == GRAND_RING && this.isGrandBaobab(lot)) && inPark(parks, angle, reach);
				this.lot(ring, lot, spot[0], spot[2], angle, park ? -size : size, wild);
			}
		}
		this.trees(wild);
	}

	/** Whether a spot at the angle and at that distance from the middle is in the park around a great baobab. */
	private static boolean inPark(List<Double> parks, double angle, double reach) {
		for (double park : parks) {
			double apart = Math.abs(Math.atan2(Math.sin(angle - park), Math.cos(angle - park)));
			if (apart * reach < PARK) {
				return true;
			}
		}
		return false;
	}

	/** Builds what a lot has; a lot whose size is given as negative is in a park, and has only a grove. */
	private void lot(int ring, int lot, double posX, double posZ, double angle, double sized, Random wild) {
		double size = Math.abs(sized);
		if (ring == GRAND_RING && this.isGrandBaobab(lot)) {
			this.forms.baobabTower(posX, posZ, 300, GRAND_HEIGHT, wild);
			this.towers.add(new double[] { posX, posZ, GRAND_HEIGHT, ring, lot });
			this.grand.add(new double[] { posX, posZ, GRAND_HEIGHT });
			this.solid(posX, posZ, 150 * 2.9, GRAND_HEIGHT);
			return;
		}
		Kind kind = sized < 0 ? Kind.GROVE : kindOf(ring, wild.nextDouble());
		if (kind == Kind.HOUSES) {
			for (int house = 0; house < 2; house++) {
				double shift = (house - 0.5) * size * 0.9;
				this.forms.domeHouse(posX - Math.sin(angle) * shift, posZ + Math.cos(angle) * shift, size * 0.8, angle + Math.PI, wild);
			}
			this.solid(posX, posZ, size, 90);
		} else if (kind == Kind.GROVE) {
			this.forms.grove(posX, posZ, size * 0.5, wild);
			this.solid(posX, posZ, size * 0.5 + 30, 60);
		} else {
			double height = this.tower(kind, HEIGHTS[ring], posX, posZ, angle, size, wild);
			this.remember(posX, posZ, height, ring, lot);
		}
	}

	/** Builds a tower of the kind on a lot, as tall as the ring has them, and keeps the camera out of it: how tall it is. */
	private double tower(Kind kind, double base, double posX, double posZ, double angle, double size, Random wild) {
		switch (kind) {
		case TERRACE -> {
			double height = base * (0.6 + 0.8 * wild.nextDouble());
			this.forms.terraceTower(posX, posZ, angle, size, height, wild);
			this.solid(posX, posZ, size * 0.75, height * 1.05 + 30);
			return height;
		}
		case SPIRE -> {
			double height = Math.min(560, base * (0.5 + 0.6 * wild.nextDouble()));
			this.forms.spireTower(posX, posZ, angle, size * 0.9, height);
			this.solid(posX, posZ, size * 0.7, height + 45);
			return height;
		}
		case BAOBAB -> {
			double height = base * (0.8 + 0.5 * wild.nextDouble());
			this.forms.baobabTower(posX, posZ, size * 0.8, height, wild);
			// The branches and their gardens reach out almost three times the trunk
			this.solid(posX, posZ, size * 0.4 * 2.9, height);
			return height;
		}
		default -> {
			double height = base * (1 + 0.6 * wild.nextDouble());
			this.forms.skyscraper(posX, posZ, angle, size * 0.85, height, wild);
			this.solid(posX, posZ, size * 0.6, height * 1.1);
			return height;
		}
		}
	}

	/** Whether the lot of the ring is the one for a great baobab: the lot nearest to the middle between two avenues, one in each quarter of the city. */
	private boolean isGrandBaobab(int lot) {
		int lots = this.lotsIn(GRAND_RING);
		for (int quarter = 0; quarter < 4; quarter++) {
			double wanted = Math.PI / 3 + quarter * Math.PI / 2;
			long nearest = Math.round((wanted - Math.PI / lots) * lots / (Math.PI * 2) - 0.5);
			if (Math.floorMod(nearest, lots) == lot) {
				return true;
			}
		}
		return false;
	}

	/** Keeps a thing the camera must not go into: a round of the radius around the spot, from the ground to the height. */
	private void solid(double posX, double posZ, double radius, double height) {
		this.solid(posX, posZ, radius, EdenForms.GROUND - height, ROOM);
	}

	/** Keeps a thing the camera must not go into, with its top as a height and the room to keep around it. */
	private void solid(double posX, double posZ, double radius, double top, double room) {
		this.solids.add(new double[] { posX, posZ, radius, top, room });
	}

	/**
	 * The lowest the camera may be over the spot {x, z}, as a height (negative up): above everything of the city it would go into
	 * there, with some room; positive infinity where nothing of the city stands.
	 */
	double clearance(double posX, double posZ) {
		double lowest = Double.POSITIVE_INFINITY;
		for (double[] solid : this.solids) {
			double reach = solid[2] + solid[4];
			double deltaX = posX - solid[0];
			double deltaZ = posZ - solid[1];
			if (deltaX * deltaX + deltaZ * deltaZ < reach * reach) {
				lowest = Math.min(lowest, solid[3] - solid[4]);
			}
		}
		return lowest;
	}

	/** The lowest the camera may be over the spot, for a tour: the city only, whatever the way crosses. */
	@Override
	public double floor(double posX, double posZ, boolean crossing) {
		return this.clearance(posX, posZ);
	}

	/** Whether the spot is inside the wall of the stadium. */
	@Override
	public boolean inside(double posX, double posZ) {
		double angle = Math.atan2(posZ, posX);
		return Math.hypot(posX, posZ) < this.edge * (1 + EdenStadium.swell(angle));
	}

	/** Keeps a tower that is tall enough to have a bridge. */
	private void remember(double posX, double posZ, double height, int ring, int lot) {
		if (height > 150) {
			this.towers.add(new double[] { posX, posZ, height, ring, lot });
		}
	}

	/** The solar trees along the avenues, on both sides, alternating, and the ones in the plazas where an avenue meets a ring road. */
	private void trees(Random wild) {
		double start = this.edge + GAP * 0.6;
		double end = this.edge + GAP + RINGS * STEP;
		for (int avenue = 0; avenue < AVENUES; avenue++) {
			double angle = avenueAngle(avenue);
			double[] across = { -Math.sin(angle), 0, Math.cos(angle) };
			int side = 1;
			for (double radius = start; radius < end; radius += 330) {
				double[] spot = EdenStadium.point(angle, radius, 0);
				double treeX = spot[0] + across[0] * side * (AVENUE / 2 + 22);
				double treeZ = spot[2] + across[2] * side * (AVENUE / 2 + 22);
				double height = 90 + 60 * wild.nextDouble();
				this.forms.solarTree(treeX, treeZ, height);
				// The camera flies low along the middle of the avenue, past the plates
				this.solid(treeX, treeZ, 26, EdenForms.GROUND - height - 8, 15);
				side = -side;
			}
		}
	}

	/** Bridges of glass between the towers that stand next to each other in a ring and are tall enough, a third of the way up, never over an avenue. */
	private void bridges(Random wild) {
		for (int index = 0; index < this.towers.size(); index++) {
			double[] first = this.towers.get(index);
			for (int other = index + 1; other < Math.min(this.towers.size(), index + 6); other++) {
				double[] second = this.towers.get(other);
				boolean next = first[3] == second[3] && Math.abs(first[4] - second[4]) == 1;
				double apart = Math.hypot(first[0] - second[0], first[1] - second[1]);
				double middle = Math.atan2(first[1] + second[1], first[0] + second[0]);
				double across = Math.hypot(first[0] + second[0], first[1] + second[1]) / 2 / (1 + EdenStadium.swell(middle));
				if (next && apart < 1.1 * LOT && wild.nextDouble() < 0.5 && !onAnAvenue(middle, across, apart)) {
					double high = -Math.min(first[2], second[2]) * (0.35 + 0.2 * wild.nextDouble());
					this.forms.bridge(new double[] { first[0], high, first[1] }, new double[] { second[0], high, second[1] });
					for (int step = 0; step <= 4; step++) {
						this.solid(first[0] + (second[0] - first[0]) * step / 4, first[1] + (second[1] - first[1]) * step / 4, apart / 8, high - 6, 40);
					}
				}
			}
		}
	}

	/** The far skyline, in the haze beyond the city: slim towers around the whole horizon, which close it without drawing much. */
	private void skyline(Random wild) {
		double inner = this.edge + GAP + RINGS * STEP + 900;
		for (int index = 0; index < 120; index++) {
			double angle = Math.PI * 2 * (index + wild.nextDouble()) / 120;
			double radius = inner + wild.nextDouble() * 2200;
			double[] spot = EdenStadium.point(angle, radius, 0);
			double width = 120 + wild.nextDouble() * 150;
			double height = 700 + wild.nextDouble() * 1300;
			this.haze.frustum(EdenForms.rectangle(spot[0], spot[2], width / 2, width / 2, angle), EdenForms.GROUND, EdenForms.GROUND - height, 0.35 + 0.25 * wild.nextDouble());
		}
	}
}
