package widgets.scenes;

import static widgets.scenes.BattleArena3D.LAVA_TILE;
import static widgets.scenes.ArenaMaterials.distance2;
import static widgets.scenes.ArenaMaterials.material;
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

	/** Creates the lava flows of the arena, drawn in the host scene. */
	LavaFlows(BattleArena3D host) {
		this.host = host;
	}

	/**
	 * A quad of flowing lava
	 *
	 * @param corners
	 *            four {x, y, z}, in order around the quad: the texture goes from
	 *            the first to the second (u) and from the first to the fourth (v)
	 * @param duValue
	 *            tiles per second the texture slides along u, negative goes from
	 *            the first corner to the second
	 */
	void flow(double[][] corners, double duValue, double dvValue) {
		this.flow(corners, duValue, dvValue, host.lava, LAVA_TILE);
	}

	/**
	 * @param tile
	 *            units of the world one tile of the texture covers
	 */
	void flow(double[][] corners, double duValue, double dvValue, PhongMaterial material, double tile) {
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
		host.flows.add(new Flow(mesh, base, duValue, dvValue));
	}

	/**
	 * A flat strip of lava from one point to another, flowing that way
	 */
	void flowStrip(double x1Value, double z1Value, double x2Value, double z2Value, double width, double posY, double speed) {
		this.flowStrip(x1Value, z1Value, x2Value, z2Value, width, posY, speed, host.lava, LAVA_TILE);
	}

	/** A strip of flowing lava between two points, of the given width and height. */
	void flowStrip(double startX, double startZ, double endX, double endZ, double width, double posY, double speed, PhongMaterial material, double tile) {
		double length = Math.hypot(endX - startX, endZ - startZ);
		double posX = -(endZ - startZ) / length * width / 2;
		double posZ = (endX - startX) / length * width / 2;
		this.flow(new double[][] { { startX - posX, posY, startZ - posZ }, { endX - posX, posY, endZ - posZ }, { endX + posX, posY, endZ + posZ }, { startX + posX, posY, startZ + posZ } }, -speed, 0, material, tile);
	}

	/**
	 * A flat ribbon along a winding line, with its own half width at each point: a
	 * single mesh, so the texture runs on without a seam at the joints
	 */
	void flowRibbon(double[] xsValue, double[] zsValue, double[] halfWidths, double posY, double duValue, double dvValue, PhongMaterial material, double tile) {
		int count = xsValue.length;
		TriangleMesh mesh = new TriangleMesh();
		float[] texture = new float[4 * count];
		double along = 0;
		for (int index = 0; index < count; index++) {
			if (index > 0) {
				along += Math.hypot(xsValue[index] - xsValue[index - 1], zsValue[index] - zsValue[index - 1]);
			}
			// The side of the ribbon: across the direction of the line
			double deltaX = xsValue[Math.min(index + 1, count - 1)] - xsValue[Math.max(index - 1, 0)];
			double deltaZ = zsValue[Math.min(index + 1, count - 1)] - zsValue[Math.max(index - 1, 0)];
			double length = Math.hypot(deltaX, deltaZ);
			double sxValue = -deltaZ / length * halfWidths[index];
			double szValue = deltaX / length * halfWidths[index];
			mesh.getPoints().addAll((float) (xsValue[index] + sxValue), (float) posY, (float) (zsValue[index] + szValue), (float) (xsValue[index] - sxValue), (float) posY, (float) (zsValue[index] - szValue));
			// Along the line the texture grows with the length, across it with the width
			float uCoord = (float) (along / tile);
			float vCoord = (float) (2 * halfWidths[index] / tile);
			texture[4 * index] = uCoord;
			texture[4 * index + 1] = 0;
			texture[4 * index + 2] = uCoord;
			texture[4 * index + 3] = vCoord;
		}
		mesh.getTexCoords().addAll(texture);
		for (int index2 = 0; index2 < count - 1; index2++) {
			int left = 2 * index2;
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
		host.flows.add(new Flow(mesh, texture, duValue, dvValue));
	}

	/**
	 * A river from one side of the field to the other, meandering around a line at
	 * distance centre from the middle, its banks wider and narrower in turn
	 */
	void river(double fromX, double toX, double sign, double centre, double phase, double width, double speed, PhongMaterial material, double tile) {
		int count = 150;
		double[] xsValue = new double[count];
		double[] zsValue = new double[count];
		double[] halfWidths = new double[count];
		for (int index = 0; index < count; index++) {
			double posX = fromX + (toX - fromX) * index / (count - 1);
			double bend = 45 * Math.sin(posX / 330 + phase) + 22 * Math.sin(posX / 120 + 1.7 + 2 * phase) + 10 * Math.sin(posX / 57 + 0.3 * phase);
			xsValue[index] = posX;
			zsValue[index] = sign * (centre + bend);
			halfWidths[index] = width / 2 * (0.8 + 0.3 * (0.5 + 0.5 * Math.sin(posX / 190 + 0.9 + phase)));
		}
		this.flowRibbon(xsValue, zsValue, halfWidths, 3, -speed, 0, material, tile);
	}

	/** Moves the texture of every flow by the time that passed, so that the lava seems to flow. */
	void updateFlows() {
		for (Flow flow : host.flows) {
			float[] coordinates = new float[flow.base().length];
			// Kept small: the texture repeats every 1
			float uCoord = (float) ((host.clock * flow.du()) % 1);
			float vCoord = (float) ((host.clock * flow.dv()) % 1);
			for (int index = 0; index < coordinates.length / 2; index++) {
				coordinates[2 * index] = flow.base()[2 * index] + uCoord;
				coordinates[2 * index + 1] = flow.base()[2 * index + 1] + vCoord;
			}
			flow.mesh().getTexCoords().setAll(coordinates);
		}
	}
}
