package widgets.scenes;

import static widgets.scenes.BattleArena3D.LAVA_TILE;
import static widgets.scenes.BattleArena3D.distance2;
import static widgets.scenes.BattleArena3D.material;
import widgets.scenes.BattleArena3D.Flow;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The lava flows of the arena: the surfaces of lava whose texture slides, the
 * strips and ribbons that follow the rivers and the moat, and the rivers of
 * the jungle, which flow the same way.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class LavaFlows {

	private final BattleArena3D host;

	LavaFlows(BattleArena3D host) {
		this.host = host;
	}

	/**
	 * A quad of flowing lava
	 *
	 * @param corners
	 *            four {x, y, z}, in order around the quad: the texture goes from
	 *            the first to the second (u) and from the first to the fourth (v)
	 * @param du
	 *            tiles per second the texture slides along u, negative goes from
	 *            the first corner to the second
	 */
	void flow(double[][] corners, double du, double dv) {
		this.flow(corners, du, dv, host.lava, LAVA_TILE);
	}

	/**
	 * @param tile
	 *            units of the world one tile of the texture covers
	 */
	void flow(double[][] corners, double du, double dv, PhongMaterial material, double tile) {
		double uLength = Math.sqrt(distance2(corners[0], corners[1])) / tile;
		double vLength = Math.sqrt(distance2(corners[0], corners[3])) / tile;
		TriangleMesh mesh = new TriangleMesh();
		for (double[] corner : corners) {
			mesh.getPoints().addAll((float) corner[0], (float) corner[1], (float) corner[2]);
		}
		float[] base = { 0, 0, (float) uLength, 0, (float) uLength, (float) vLength, 0, (float) vLength };
		mesh.getTexCoords().addAll(base);
		mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		// Only the lava glows, water is lit like the rest
		(host.style == ArenaStyle.VOLCANO ? host.glowing : host.arena).getChildren().add(view);
		host.flows.add(new Flow(mesh, base, du, dv));
	}

	/**
	 * A flat strip of lava from one point to another, flowing that way
	 */
	void flowStrip(double x1, double z1, double x2, double z2, double width, double y, double speed) {
		this.flowStrip(x1, z1, x2, z2, width, y, speed, host.lava, LAVA_TILE);
	}

	void flowStrip(double x1, double z1, double x2, double z2, double width, double y, double speed, PhongMaterial material, double tile) {
		double length = Math.hypot(x2 - x1, z2 - z1);
		double px = -(z2 - z1) / length * width / 2;
		double pz = (x2 - x1) / length * width / 2;
		this.flow(new double[][] { { x1 - px, y, z1 - pz }, { x2 - px, y, z2 - pz }, { x2 + px, y, z2 + pz }, { x1 + px, y, z1 + pz } }, -speed, 0, material, tile);
	}

	/**
	 * A flat ribbon along a winding line, with its own half width at each point: a
	 * single mesh, so the texture runs on without a seam at the joints
	 */
	void flowRibbon(double[] xs, double[] zs, double[] halfWidths, double y, double du, double dv, PhongMaterial material, double tile) {
		int count = xs.length;
		TriangleMesh mesh = new TriangleMesh();
		float[] texture = new float[4 * count];
		double along = 0;
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				along += Math.hypot(xs[i] - xs[i - 1], zs[i] - zs[i - 1]);
			}
			// The side of the ribbon: across the direction of the line
			double dx = xs[Math.min(i + 1, count - 1)] - xs[Math.max(i - 1, 0)];
			double dz = zs[Math.min(i + 1, count - 1)] - zs[Math.max(i - 1, 0)];
			double length = Math.hypot(dx, dz);
			double sx = -dz / length * halfWidths[i];
			double sz = dx / length * halfWidths[i];
			mesh.getPoints().addAll((float) (xs[i] + sx), (float) y, (float) (zs[i] + sz), (float) (xs[i] - sx), (float) y, (float) (zs[i] - sz));
			// Along the line the texture grows with the length, across it with the width
			float u = (float) (along / tile);
			float v = (float) (2 * halfWidths[i] / tile);
			texture[4 * i] = u;
			texture[4 * i + 1] = 0;
			texture[4 * i + 2] = u;
			texture[4 * i + 3] = v;
		}
		mesh.getTexCoords().addAll(texture);
		for (int i = 0; i < count - 1; i++) {
			int left = 2 * i;
			int right = left + 1;
			int nextLeft = left + 2;
			int nextRight = left + 3;
			mesh.getFaces().addAll(left, left, nextLeft, nextLeft, nextRight, nextRight);
			mesh.getFaces().addAll(left, left, nextRight, nextRight, right, right);
		}
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		host.arena.getChildren().add(view);
		host.flows.add(new Flow(mesh, texture, du, dv));
	}

	/**
	 * A river from one side of the field to the other, meandering around a line at
	 * distance centre from the middle, its banks wider and narrower in turn
	 */
	void river(double fromX, double toX, double sign, double centre, double phase, double width, double speed, PhongMaterial material, double tile) {
		int count = 150;
		double[] xs = new double[count];
		double[] zs = new double[count];
		double[] halfWidths = new double[count];
		for (int i = 0; i < count; i++) {
			double x = fromX + (toX - fromX) * i / (count - 1);
			double bend = 45 * Math.sin(x / 330 + phase) + 22 * Math.sin(x / 120 + 1.7 + 2 * phase) + 10 * Math.sin(x / 57 + 0.3 * phase);
			xs[i] = x;
			zs[i] = sign * (centre + bend);
			halfWidths[i] = width / 2 * (0.8 + 0.3 * (0.5 + 0.5 * Math.sin(x / 190 + 0.9 + phase)));
		}
		this.flowRibbon(xs, zs, halfWidths, 3, -speed, 0, material, tile);
	}

	void updateFlows() {
		for (Flow flow : host.flows) {
			float[] coordinates = new float[flow.base().length];
			// Kept small: the texture repeats every 1
			float u = (float) ((host.clock * flow.du()) % 1);
			float v = (float) ((host.clock * flow.dv()) % 1);
			for (int i = 0; i < coordinates.length / 2; i++) {
				coordinates[2 * i] = flow.base()[2 * i] + u;
				coordinates[2 * i + 1] = flow.base()[2 * i + 1] + v;
			}
			flow.mesh().getTexCoords().setAll(coordinates);
		}
	}
}
