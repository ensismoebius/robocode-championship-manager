package widgets.scenes;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.util.Duration;
import static widgets.scenes.BattleArena3D.solidMap;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The materials, meshes and shapes shared by the battle arena and its parts: the
 * sky, the tiled and textured surfaces, the boxes and cylinders, the glowing
 * energy and the transforms that grow and shrink a piece.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaMaterials {

	private ArenaMaterials() {
	}

	/**
	 * A dome around everything, seen from inside, that shines by itself
	 */
	static Sphere skyDome(Image picture) {
		Sphere dome = new Sphere(14000, 48);
		dome.setCullFace(CullFace.FRONT);
		PhongMaterial sky = new PhongMaterial(Color.BLACK);
		sky.setSelfIlluminationMap(picture);
		dome.setMaterial(sky);
		return dome;
	}

	static double distance2(double[] a, double[] b) {
		return (a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]);
	}

	/**
	 * A textured material
	 */
	static PhongMaterial textured(Image texture, Color specular) {
		PhongMaterial material = new PhongMaterial(Color.WHITE);
		material.setDiffuseMap(texture);
		material.setSpecularColor(specular);
		material.setSpecularPower(40);
		return material;
	}

	static PhongMaterial material(Color color) {
		PhongMaterial material = new PhongMaterial(color);
		material.setSpecularColor(color.brighter());
		material.setSpecularPower(24);
		return material;
	}

	static Cylinder cylinder(double radius, double height, int divisions, double x, double y, double z, Point3D axis, double angle, PhongMaterial material) {
		Cylinder cylinder = new Cylinder(radius, height, divisions);
		cylinder.setTranslateX(x);
		cylinder.setTranslateY(y);
		cylinder.setTranslateZ(z);
		cylinder.setRotationAxis(axis);
		cylinder.setRotate(angle);
		cylinder.setMaterial(material);
		return cylinder;
	}

	static Box box(double w, double h, double d, double x, double y, double z, PhongMaterial material) {
		Box box = new Box(w, h, d);
		box.setTranslateX(x);
		box.setTranslateY(y);
		box.setTranslateZ(z);
		box.setMaterial(material);
		return box;
	}

	// ---------------------------------------------------------------- effects

	/** A crystal of light, built once: the flash of a shot, a spark */
	static final TriangleMesh CRYSTAL = spindleMesh(1, 1, 1, 4);

	private static TriangleMesh spindleMesh(double radius, double front, double back, int sides) {
		LowPoly shape = new LowPoly();
		shape.spindle(0, 0, 0, radius, front, back, sides);
		return shape.mesh;
	}

	/**
	 * A see through bubble: the color with a low opacity and no light of its own, so
	 * the arena shows through it. A light map would make it opaque.
	 */
	static PhongMaterial bubble(Color color) {
		Color light = color.interpolate(Color.WHITE, 0.5);
		PhongMaterial material = new PhongMaterial(Color.color(light.getRed(), light.getGreen(), light.getBlue(), 0.3));
		material.setSpecularColor(Color.color(1, 1, 1, 0.6));
		material.setSpecularPower(64);
		return material;
	}

	/**
	 * A see through material that shines in the color by itself
	 */
	static PhongMaterial energy(Color color, double opacity) {
		PhongMaterial material = new PhongMaterial(Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity));
		material.setSelfIlluminationMap(solidMap(color));
		return material;
	}

	static MeshView shape(TriangleMesh mesh, PhongMaterial material) {
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		return view;
	}

	static ScaleTransition grow(Node node, double from, double to, double millis) {
		ScaleTransition grow = new ScaleTransition(Duration.millis(millis), node);
		grow.setFromX(from);
		grow.setFromY(from);
		grow.setFromZ(from);
		grow.setToX(to);
		grow.setToY(to);
		grow.setToZ(to);
		grow.setInterpolator(Interpolator.EASE_OUT);
		return grow;
	}

	static ScaleTransition shrink(Node node, double delay, double millis) {
		ScaleTransition vanish = new ScaleTransition(Duration.millis(millis), node);
		vanish.setDelay(Duration.millis(delay));
		vanish.setToX(0);
		vanish.setToY(0);
		vanish.setToZ(0);
		return vanish;
	}

	/**
	 * A robocode ARGB color, exactly as the robot defined it (black included), or
	 * the fallback if there is none
	 */
	static Color argb(int argb, Color fallback) {
		if (argb == 0) {
			return fallback;
		}
		return Color.rgb((argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff);
	}

	/**
	 * The material of a part painted in a color chosen by the robot. It is the
	 * color itself, but a dark paint (a black tank) still shows its shape: the
	 * light glints on it
	 */
	static PhongMaterial paint(Color color) {
		PhongMaterial material = new PhongMaterial(color);
		double floor = 0.38;
		material.setSpecularColor(Color.color(Math.max(floor, Math.min(1, color.getRed() * 1.3)), Math.max(floor, Math.min(1, color.getGreen() * 1.3)), Math.max(floor, Math.min(1, color.getBlue() * 1.3))));
		material.setSpecularPower(36);
		return material;
	}
}
