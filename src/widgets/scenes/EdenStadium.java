package widgets.scenes;

import static widgets.scenes.ArenaHelpers.solidMap;
import static widgets.scenes.ArenaMaterials.material;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The stadium of the Eden Solar arena: a lower and an upper ring of stands full of people, the concourse between
 * them with its lit doors, aisles with small lights on the steps, a band of pale blue panels at the top, and the
 * roof, a ring of structure with rows of warm windows around a great opening to the sky. The lower ring opens at
 * both ends of the field, where the corridors are.
 *
 * <p>
 * The roof and the walls are seen from inside only: from a camera above the roof they are not drawn, so the field
 * is never hidden.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenStadium {

	/** Pieces around the stadium; the corridors take {@link #GAP} of them on each side, around the x axis */
	static final int SEGMENTS = 256;
	static final int GAP = 8;
	private static final int AISLE_EVERY = 16;
	private static final int ROWS = 10;
	private static final double ROW_DEPTH = 40;
	private static final double FRONT_HEIGHT = 30;
	private static final double LOWER_RISE = 21;
	private static final double UPPER_RISE = 27;
	/** The concourse between the two rings, and the wall of doors at its back */
	static final double CONCOURSE = 48;
	private static final double DOOR_WALL = 61;
	/** The top of the stadium: the band of panels and the roof, from its outer edge to the opening */
	private static final double BAND = 77;
	private static final double ROOF_RISE = 135;
	private static final double ROOF_DEPTH = 600;
	private static final double[] UP = { 0, -1, 0 };
	private static final double[] DOWN = { 0, 1, 0 };

	private final BattleArena3D host;
	private final double front;

	/** The stadium around the garden, its front wall at the given radius. */
	EdenStadium(BattleArena3D host, double front) {
		this.host = host;
		this.front = front;
	}

	/** Where the lower ring ends and the concourse begins. */
	double concourse() {
		return this.front + ROWS * ROW_DEPTH;
	}

	/** The height of the concourse floor, negative as up is. */
	static double concourseHeight() {
		return -(FRONT_HEIGHT + (ROWS - 1) * LOWER_RISE);
	}

	/** Whether the piece of the ring is in one of the two corridors, at the ends of the field. */
	static boolean inCorridor(int segment) {
		return Math.floorMod(segment + GAP, SEGMENTS / 2) < 2 * GAP;
	}

	/** The angle of the start of a piece of the ring. */
	static double angle(int segment) {
		return Math.PI * 2 * segment / SEGMENTS;
	}

	/** A point of the ring at the angle, radius and height. */
	private static double[] point(double angle, double radius, double height) {
		return new double[] { radius * Math.cos(angle), height, radius * Math.sin(angle) };
	}

	/** The direction toward the middle of the stadium, at the angle. */
	private static double[] inward(double angle) {
		return new double[] { -Math.cos(angle), 0, -Math.sin(angle) };
	}

	/** Builds the whole stadium. */
	void build() {
		this.buildRings();
		this.buildDoors();
		this.buildCorridorWalls();
		this.buildTop();
		this.buildRoof();
	}

	/** The two rings of stands: the crowd on the risers and treads, the aisles with their step lights */
	private void buildRings() {
		SurfaceMesh crowd = new SurfaceMesh();
		SurfaceMesh concrete = new SurfaceMesh();
		LowPoly stepLights = new LowPoly();
		double upper = this.concourse() + CONCOURSE;
		for (int segment = 0; segment < SEGMENTS; segment++) {
			boolean aisle = segment % AISLE_EVERY == AISLE_EVERY / 2;
			SurfaceMesh seats = aisle ? concrete : crowd;
			for (int row = 0; row < ROWS; row++) {
				double lowerTop = -(FRONT_HEIGHT + row * LOWER_RISE);
				if (!inCorridor(segment)) {
					// The first riser is the front wall, without people
					this.riser(row == 0 ? concrete : seats, segment, this.front + row * ROW_DEPTH, row == 0 ? 6 : lowerTop + LOWER_RISE, lowerTop, row);
					this.tread(seats, segment, this.front + row * ROW_DEPTH, lowerTop, row);
					if (aisle && row > 0) {
						this.stepLight(stepLights, segment, this.front + row * ROW_DEPTH, lowerTop);
					}
				}
				// The first riser of the upper ring is the wall of the doors, see buildDoors
				double upperTop = -(FRONT_HEIGHT + (ROWS - 1) * LOWER_RISE + DOOR_WALL + row * UPPER_RISE);
				if (row > 0) {
					this.riser(seats, segment, upper + row * ROW_DEPTH, upperTop + UPPER_RISE, upperTop, row + ROWS);
					this.stepLight(aisle ? stepLights : null, segment, upper + row * ROW_DEPTH, upperTop);
				}
				this.tread(seats, segment, upper + row * ROW_DEPTH, upperTop, row + ROWS);
			}
			this.concourseFloor(concrete, segment);
		}
		PhongMaterial people = ArenaMaterials.textured(EdenTexture.CROWD, Color.gray(0.05));
		people.setDiffuseColor(Color.gray(0.62));
		host.arena.getChildren().addAll(crowd.view(people), concrete.view(material(Color.web("#3a3d40"))), stepLights.view(glowing("#ffb24a")));
	}

	/** Where the crowd picture is along the ring, a little shifted on each row so the people do not line up. */
	private static float[] across(int segment, double radius, int row) {
		float shift = row * 0.37f;
		return new float[] { (float) (angle(segment) * radius / EdenTexture.CROWD_WIDTH) + shift, (float) (angle(segment + 1) * radius / EdenTexture.CROWD_WIDTH) + shift };
	}

	/** The riser of a step in one piece, facing the field: the people sitting in front of it */
	private void riser(SurfaceMesh mesh, int segment, double radius, double bottom, double top, int row) {
		double first = angle(segment);
		double second = angle(segment + 1);
		float[] uCoords = across(segment, radius, row);
		float share = EdenTexture.RISER_SHARE;
		mesh.quad(point(first, radius, bottom), point(second, radius, bottom), point(second, radius, top), point(first, radius, top), new float[] { uCoords[0], share, uCoords[1], share, uCoords[1], 0, uCoords[0], 0 },
				inward((first + second) / 2));
	}

	/** The tread of a step in one piece, facing up: the seats seen from above */
	private void tread(SurfaceMesh mesh, int segment, double radius, double top, int row) {
		double first = angle(segment);
		double second = angle(segment + 1);
		float[] uCoords = across(segment, radius, row);
		float share = EdenTexture.RISER_SHARE;
		mesh.quad(point(first, radius, top), point(second, radius, top), point(second, radius + ROW_DEPTH, top), point(first, radius + ROW_DEPTH, top), new float[] { uCoords[0], share, uCoords[1], share, uCoords[1], 1, uCoords[0], 1 }, UP);
	}

	/** A small warm light on the edge of a step of an aisle */
	private void stepLight(LowPoly lights, int segment, double radius, double top) {
		if (lights == null) {
			return;
		}
		double middle = (angle(segment) + angle(segment + 1)) / 2;
		lights.box(radius * Math.cos(middle), top - 1.2, radius * Math.sin(middle), 3, 2.4, 3);
	}

	/** The floor of the concourse between the rings, in one piece */
	private void concourseFloor(SurfaceMesh concrete, int segment) {
		double first = angle(segment);
		double second = angle(segment + 1);
		double height = concourseHeight();
		double inner = this.concourse();
		double outer = inner + CONCOURSE;
		concrete.quad(point(first, inner, height), point(second, inner, height), point(second, outer, height), point(first, outer, height), new float[] { 0, 0, 1, 0, 1, 1, 0, 1 }, UP);
	}

	/**
	 * The wall at the back of the concourse with its lit doors, the warm strips of light along the edges of the
	 * rings, and the tunnels at the end of each corridor
	 */
	private void buildDoors() {
		SurfaceMesh wall = new SurfaceMesh();
		SurfaceMesh doors = new SurfaceMesh();
		LowPoly strips = new LowPoly();
		double back = this.concourse() + CONCOURSE;
		double floor = concourseHeight();
		for (int segment = 0; segment < SEGMENTS; segment++) {
			double first = angle(segment);
			double second = angle(segment + 1);
			double middle = (first + second) / 2;
			wall.quad(point(first, back, floor), point(second, back, floor), point(second, back, floor - DOOR_WALL), point(first, back, floor - DOOR_WALL), new float[] { 0, 0, 1, 0, 1, 1, 0, 1 }, inward(middle));
			if (segment % 8 == 4) {
				// A door two pieces wide
				double narrow = (second - first) * 0.35;
				double third = angle(segment + 2);
				doors.quad(point(first + narrow, back - 0.6, floor), point(third - narrow, back - 0.6, floor), point(third - narrow, back - 0.6, floor - 42), point(first + narrow, back - 0.6, floor - 42), new float[] { 0, 0, 1, 0, 1, 1, 0, 1 }, inward(middle));
			}
			if (!inCorridor(segment)) {
				this.strip(strips, first, second, this.front - 1, -FRONT_HEIGHT);
			}
			this.strip(strips, first, second, this.concourse() - 1, floor);
			this.strip(strips, first, second, back - 1, floor - DOOR_WALL);
		}
		// The tunnel at the back of each corridor, its light warmer toward the floor
		SurfaceMesh tunnels = new SurfaceMesh();
		for (int side = -1; side <= 1; side += 2) {
			double angle = side < 0 ? Math.PI : 0;
			double radius = this.concourse() - 0.6;
			double half = 80 / radius;
			tunnels.quad(point(angle - half, radius, 6), point(angle + half, radius, 6), point(angle + half, radius, -120), point(angle - half, radius, -120), new float[] { 0, 1, 1, 1, 1, 0, 0, 0 }, inward(angle));
		}
		PhongMaterial tunnel = new PhongMaterial(Color.BLACK);
		tunnel.setSelfIlluminationMap(EdenTexture.TUNNEL);
		host.arena.getChildren().addAll(wall.view(material(Color.web("#2a2c2f"))), doors.view(glowing("#d9782e")), tunnels.view(tunnel), strips.view(glowing("#ffb24a")));
	}

	/** A short strip of warm light along the edge of a ring, between two angles */
	private void strip(LowPoly strips, double first, double second, double radius, double height) {
		strips.frustum(new double[][] { { radius * Math.cos(first), radius * Math.sin(first) }, { (radius + 3) * Math.cos(first), (radius + 3) * Math.sin(first) },
				{ (radius + 3) * Math.cos(second), (radius + 3) * Math.sin(second) }, { radius * Math.cos(second), radius * Math.sin(second) } }, height, height - 3, 1);
	}

	/**
	 * The walls on the sides of each corridor: the steps of the lower ring seen from the side, from the floor to
	 * each row, and the wall at the back of the corridor up to the concourse
	 */
	private void buildCorridorWalls() {
		SurfaceMesh walls = new SurfaceMesh();
		for (int edge : new int[] { GAP, -GAP, SEGMENTS / 2 - GAP, SEGMENTS / 2 + GAP }) {
			double angle = angle(edge);
			// The corridor is on the side of the smaller angle for the first edge of each pair
			boolean towardSmaller = edge == GAP || edge == SEGMENTS / 2 + GAP;
			double[] facing = towardSmaller ? new double[] { Math.sin(angle), 0, -Math.cos(angle) } : new double[] { -Math.sin(angle), 0, Math.cos(angle) };
			for (int row = 0; row < ROWS; row++) {
				double inner = this.front + row * ROW_DEPTH;
				double top = -(FRONT_HEIGHT + row * LOWER_RISE);
				walls.quad(point(angle, inner, 6), point(angle, inner + ROW_DEPTH, 6), point(angle, inner + ROW_DEPTH, top), point(angle, inner, top), new float[] { 0, 0, 1, 0, 1, 1, 0, 1 }, facing);
			}
		}
		for (int segment = 0; segment < SEGMENTS; segment++) {
			if (inCorridor(segment)) {
				double first = angle(segment);
				double second = angle(segment + 1);
				double radius = this.concourse();
				walls.quad(point(first, radius, 6), point(second, radius, 6), point(second, radius, concourseHeight()), point(first, radius, concourseHeight()), new float[] { 0, 0, 1, 0, 1, 1, 0, 1 }, inward((first + second) / 2));
			}
		}
		host.arena.getChildren().add(walls.view(material(Color.web("#2f3235"))));
	}

	/** The radius of the outer edge of the roof, behind the last row of the upper ring. */
	private double outerEdge() {
		return this.concourse() + CONCOURSE + ROWS * ROW_DEPTH;
	}

	/** The height of the top of the upper ring. */
	private static double topOfStands() {
		return -(FRONT_HEIGHT + (ROWS - 1) * LOWER_RISE + DOOR_WALL + (ROWS - 1) * UPPER_RISE);
	}

	/** The band at the top of the stands: a dark wall with pale blue panels that glow, as in the picture */
	private void buildTop() {
		SurfaceMesh band = new SurfaceMesh();
		SurfaceMesh panels = new SurfaceMesh();
		double radius = this.outerEdge();
		double bottom = topOfStands();
		for (int segment = 0; segment < SEGMENTS; segment++) {
			double first = angle(segment);
			double second = angle(segment + 1);
			double[] facing = inward((first + second) / 2);
			band.quad(point(first, radius, bottom), point(second, radius, bottom), point(second, radius, bottom - BAND), point(first, radius, bottom - BAND), new float[] { 0, 0, 1, 0, 1, 1, 0, 1 }, facing);
			if (segment % 2 == 0) {
				double narrow = (second - first) * 0.12;
				double third = angle(segment + 2);
				panels.quad(point(first + narrow, radius - 0.6, bottom - 10), point(third - narrow, radius - 0.6, bottom - 10), point(third - narrow, radius - 0.6, bottom - BAND + 8), point(first + narrow, radius - 0.6, bottom - BAND + 8),
						new float[] { 0, 0, 1, 0, 1, 1, 0, 1 }, facing);
			}
		}
		host.arena.getChildren().addAll(band.view(material(Color.web("#1c2228"))), panels.view(glowing("#a9d6f2")));
	}

	/**
	 * The roof: from the top of the stands it rises toward the opening, seen from below, with its lattice of warm
	 * windows; a ring of steel with small lights along the edge of the opening
	 */
	private void buildRoof() {
		SurfaceMesh roof = new SurfaceMesh();
		double outer = this.outerEdge();
		double base = topOfStands() - BAND;
		int bands = 8;
		for (int segment = 0; segment < SEGMENTS; segment++) {
			double first = angle(segment);
			double second = angle(segment + 1);
			float start = (float) (segment * 48.0 / SEGMENTS);
			float end = (float) ((segment + 1) * 48.0 / SEGMENTS);
			for (int ring = 0; ring < bands; ring++) {
				double near = (double) ring / bands;
				double far = (double) (ring + 1) / bands;
				double[] outerFirst = point(first, outer - near * ROOF_DEPTH, base - rise(near));
				double[] outerSecond = point(second, outer - near * ROOF_DEPTH, base - rise(near));
				double[] innerSecond = point(second, outer - far * ROOF_DEPTH, base - rise(far));
				double[] innerFirst = point(first, outer - far * ROOF_DEPTH, base - rise(far));
				roof.quad(outerFirst, outerSecond, innerSecond, innerFirst, new float[] { start, (float) near, end, (float) near, end, (float) far, start, (float) far }, DOWN);
			}
		}
		PhongMaterial lattice = ArenaMaterials.textured(EdenTexture.ROOF, Color.gray(0.1));
		lattice.setSelfIlluminationMap(EdenTexture.ROOF_LIGHTS);
		host.arena.getChildren().add(roof.view(lattice));

		double opening = outer - ROOF_DEPTH;
		double rim = base - ROOF_RISE;
		LowPoly truss = new LowPoly();
		truss.ring(opening - 6, opening + 10, rim + 6, rim - 8, 128);
		LowPoly lights = new LowPoly();
		for (int index = 0; index < 96; index++) {
			double angle = Math.PI * 2 * index / 96;
			lights.box((opening - 7) * Math.cos(angle), rim + 7, (opening - 7) * Math.sin(angle), 6, 2, 6);
		}
		host.arena.getChildren().addAll(truss.view(material(Color.web("#6b7076"))), lights.view(glowing("#d8eef7")));
	}

	/** How much the roof has risen at that share of the way from its outer edge to the opening. */
	private static double rise(double share) {
		return ROOF_RISE * share * share * (3 - 2 * share);
	}

	/** A material that shines by itself, in the color. */
	static PhongMaterial glowing(String color) {
		PhongMaterial material = new PhongMaterial(Color.BLACK);
		material.setSelfIlluminationMap(solidMap(Color.web(color)));
		return material;
	}
}
