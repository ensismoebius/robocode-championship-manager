package widgets.scenes;

import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The force field: small hexagons side by side, like a honeycomb, each one a faint cell with a thin edge. Only the
 * hexagons near the place that was hit are ever made.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class HexPanel {

	/** Distance from the center of a hexagon to its corners */
	private static final double RADIUS = 6;
	private static final double ROW = Math.sqrt(3) * RADIUS;
	/** The edge is the ring between the cell and the outer corners */
	private static final double CELL = 0.86;

	/** The panels are not instantiated. */
	private HexPanel() {
	}

	/** Height of the force field, from the floor */
	static final double WALL_HEIGHT = 80;

	/**
	 * The hexagons of the honeycomb whose centers are between two distances of a point of the wall, in the plane x-y
	 * of the wall: x runs along the wall (0 at its middle), y goes down from the top of the field, the floor is at
	 * y = 0 and the field goes up (negative y). Only the hexagons that fit the wall are made, so the honeycomb is
	 * the same whatever the point, and the hexagons of two flashes at the same place match.
	 *
	 * @param span
	 *            length of the wall
	 * @param along
	 *            position of the point along the wall, 0 at its middle
	 * @param above
	 *            height of the point over the floor
	 */
	static Group band(double span, double along, double above, double inner, double outer, PhongMaterial edge, PhongMaterial cell) {
		TriangleMesh edges = new TriangleMesh();
		TriangleMesh cells = new TriangleMesh();
		edges.getTexCoords().addAll(0, 0);
		cells.getTexCoords().addAll(0, 0);
		int first = (int) Math.ceil((-span / 2 + RADIUS) / (1.5 * RADIUS));
		int last = (int) Math.floor((span / 2 - RADIUS) / (1.5 * RADIUS));
		for (int column = first; column <= last; column++) {
			double centerX = column * 1.5 * RADIUS;
			if (Math.abs(centerX - along) > outer) {
				continue;
			}
			double shift = Math.floorMod(column, 2) == 0 ? 0 : ROW / 2;
			// Whole hexagons only, from the floor up to the height
			for (double height = ROW / 2 + shift; height + ROW / 2 <= WALL_HEIGHT + 1e-6; height += ROW) {
				double distance = Math.hypot(centerX - along, height - above);
				if (distance >= inner && distance < outer) {
					hexagon(cells, centerX, -height, RADIUS * CELL, 0);
					hexagon(edges, centerX, -height, RADIUS, RADIUS * CELL + 0.25);
				}
			}
		}
		return new Group(ArenaMaterials.shape(cells, cell), ArenaMaterials.shape(edges, edge));
	}

	/** A hexagon with a flat top, whole when the inner radius is 0, a ring between the radii otherwise. */
	private static void hexagon(TriangleMesh mesh, double centerX, double centerY, double outer, double inner) {
		int base = mesh.getPoints().size() / 3;
		for (int corner = 0; corner < 6; corner++) {
			double angle = Math.PI / 3 * corner;
			mesh.getPoints().addAll((float) (centerX + outer * Math.cos(angle)), (float) (centerY + outer * Math.sin(angle)), 0);
		}
		if (inner <= 0) {
			mesh.getPoints().addAll((float) centerX, (float) centerY, 0);
			for (int corner = 0; corner < 6; corner++) {
				mesh.getFaces().addAll(base + 6, 0, base + corner, 0, base + (corner + 1) % 6, 0);
			}
			return;
		}
		for (int corner = 0; corner < 6; corner++) {
			double angle = Math.PI / 3 * corner;
			mesh.getPoints().addAll((float) (centerX + inner * Math.cos(angle)), (float) (centerY + inner * Math.sin(angle)), 0);
		}
		for (int corner = 0; corner < 6; corner++) {
			int next = (corner + 1) % 6;
			mesh.getFaces().addAll(base + corner, 0, base + 6 + corner, 0, base + next, 0);
			mesh.getFaces().addAll(base + next, 0, base + 6 + corner, 0, base + 6 + next, 0);
		}
	}
}
