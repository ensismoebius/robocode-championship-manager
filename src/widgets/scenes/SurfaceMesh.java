package widgets.scenes;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * A surface of triangles with a picture on it, seen from one side only: each triangle is turned to the side it
 * is asked to face, so from the other side nothing is drawn (a roof seen from above, a wall seen from behind).
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class SurfaceMesh {

	final TriangleMesh mesh = new TriangleMesh();

	/** A quad of corners in order around it, with the texture coordinates (u, v) of each corner, facing the direction. */
	void quad(double[] first, double[] second, double[] third, double[] fourth, float[] coords, double[] facing) {
		this.triangle(first, second, third, new float[] { coords[0], coords[1], coords[2], coords[3], coords[4], coords[5] }, facing);
		this.triangle(first, third, fourth, new float[] { coords[0], coords[1], coords[4], coords[5], coords[6], coords[7] }, facing);
	}

	/**
	 * A triangle with the texture coordinates (u, v) of its corners, turned to face the direction (JavaFX takes the
	 * counter clockwise side as the front)
	 */
	void triangle(double[] first, double[] second, double[] third, float[] coords, double[] facing) {
		double[] along = { second[0] - first[0], second[1] - first[1], second[2] - first[2] };
		double[] across = { third[0] - first[0], third[1] - first[1], third[2] - first[2] };
		double normalX = along[1] * across[2] - along[2] * across[1];
		double normalY = along[2] * across[0] - along[0] * across[2];
		double normalZ = along[0] * across[1] - along[1] * across[0];
		boolean flip = normalX * facing[0] + normalY * facing[1] + normalZ * facing[2] < 0;
		int[] order = flip ? new int[] { 0, 2, 1 } : new int[] { 0, 1, 2 };
		double[][] corners = { first, second, third };
		int point = this.mesh.getPoints().size() / 3;
		int coord = this.mesh.getTexCoords().size() / 2;
		for (int index : order) {
			this.mesh.getPoints().addAll((float) corners[index][0], (float) corners[index][1], (float) corners[index][2]);
			this.mesh.getTexCoords().addAll(coords[index * 2], coords[index * 2 + 1]);
		}
		this.mesh.getFaces().addAll(point, coord, point + 1, coord + 1, point + 2, coord + 2);
	}

	/** The surface as a node with the material, drawn on its front side only. */
	MeshView view(PhongMaterial material) {
		MeshView view = new MeshView(this.mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.BACK);
		return view;
	}
}
