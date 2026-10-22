package widgets.scenes;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import widgets.scenes.Critters.Joint;
import widgets.scenes.Critters.Kind;

/**
 * The meshes of the critters: one builder for each kind, from boxes, cylinders and spheres, with the joints that move them
 */
final class CritterModels {

	/** The body of a creature, its joints added to the list to be animated */
	static Group creature(Kind kind, List<Joint> joints) {
		Group body = new Group();
		switch (kind) {
		case MONKEY -> monkey(body, joints);
		case PENGUIN -> penguin(body, joints);
		case BEETLE -> beetle(body, joints);
		case SALAMANDER -> salamander(body, joints);
		case BIRD -> bird(body, joints);
		case BAT -> bat(body, joints);
		}
		return body;
	}

	/** The monkey: its shape, and the joints that move it */
	static void monkey(Group body, List<Joint> joints) {
		PhongMaterial fur = paint(Color.web("#7a4a22"), 12);
		PhongMaterial belly = paint(Color.web("#b8834f"), 12);
		PhongMaterial face = paint(Color.web("#d9b38c"), 12);
		PhongMaterial dark = paint(Color.web("#140c07"), 30);
		body.getChildren().addAll(part(new Sphere(9, 12), fur, 0, -14, 0, 1, 1.15, 1), part(new Sphere(5.5, 10), belly, 0, -12, 4, 0.9, 1.1, 0.6),
				part(new Sphere(6.5, 12), fur, 0, -29, 3, 1, 1, 1), part(new Sphere(2.2, 8), face, -6, -30, 2, 1, 1, 0.6),
				part(new Sphere(2.2, 8), face, 6, -30, 2, 1, 1, 0.6), part(new Sphere(3.8, 10), face, 0, -26.5, 8, 1, 0.9, 0.7),
				part(new Sphere(0.9, 6), dark, 0, -25, 11.3, 1.4, 0.8, 1));
		body.getChildren().addAll(eyes(2, -28.5, 9.6, 1.1));
		// The tail leaves the back and curves like an S, wagging a little when it walks
		body.getChildren().add(joint(joints, Rotate.Y_AXIS, 0, -12, -7, curledTail(fur), 0.3, 0.8));
		for (int side = -1; side <= 1; side += 2) {
			Group arm = new Group(box(3.2, 10, 3.2, fur, 0, 5, 0), part(new Sphere(2, 8), face, 0, 11, 0.5, 1, 1, 1));
			body.getChildren().add(joint(joints, Rotate.X_AXIS, side * 7.5, -23, 0, arm, 0.4, side > 0 ? Math.PI : 0));
			body.getChildren().add(joint(joints, Rotate.X_AXIS, side * 4.5, -11, 0, box(3.5, 11, 3.5, fur, 0, 5.5, 0), 0.5, side > 0 ? 0 : Math.PI));
		}
	
	}

	/** The penguin: its shape, and the joints that move it */
	static void penguin(Group body, List<Joint> joints) {
		PhongMaterial black = paint(Color.web("#151a22"), 40);
		PhongMaterial belly = paint(Color.web("#f4f7fa"), 20);
		PhongMaterial beak = paint(Color.web("#f0a020"), 30);
		body.getChildren().addAll(part(new Sphere(8, 12), black, 0, -13, 0, 1, 1.25, 0.9), part(new Sphere(6, 10), belly, 0, -12, 4.5, 0.9, 1.2, 0.6),
				beak(beak, 0, -17, 8.5));
		body.getChildren().addAll(eyes(2.8, -20, 7, 1.6));
		for (int side = -1; side <= 1; side += 2) {
			body.getChildren().add(joint(joints, Rotate.Z_AXIS, side * 7, -13, 0, box(2, 10, 5, black, 0, 5, 0), 0.25 * side, 0));
			body.getChildren().add(joint(joints, Rotate.X_AXIS, side * 3, 0, 1, box(4, 1, 6, beak, 0, 0, 2.5), 0.45, side > 0 ? 0 : Math.PI));
		}
	
	}

	/** The beetle: its shape, and the joints that move it */
	static void beetle(Group body, List<Joint> joints) {
		PhongMaterial shell = paint(Color.web("#2a1b3d"), 60);
		PhongMaterial seam = paint(Color.web("#3b2a52"), 40);
		PhongMaterial glow = paint(Color.web("#3ff2ff"), 60);
		PhongMaterial pink = paint(Color.web("#ff3fa4"), 60);
		body.getChildren().addAll(part(new Box(12, 5, 15), shell, 0, -4, 0, 1, 1, 1), part(new Box(6, 3.5, 4), shell, 0, -4, 9.5, 1, 1, 1),
				part(new Box(3, 1, 15), glow, 0, -7, 0, 1, 1, 1), part(new Box(0.5, 0.6, 14), seam, -2.6, -6.6, 0, 1, 1, 1),
				part(new Box(0.5, 0.6, 14), seam, 2.6, -6.6, 0, 1, 1, 1));
		for (int side = -1; side <= 1; side += 2) {
			for (double posZ = -4; posZ <= 3; posZ += 7) {
				body.getChildren().add(part(new Sphere(0.9, 6), glow, side * 3.2, -6.6, posZ, 1, 0.5, 1));
			}
			body.getChildren().add(cylinder(0.3, 9, shell, side * 2.2, -11, 12, -side * 20));
			body.getChildren().add(part(new Sphere(0.9, 6), glow, side * 4.2, -15.5, 12.5, 1, 1, 1));
		}
		body.getChildren().addAll(eyes(2.2, -4.5, 12, 1));
		for (int side = -1; side <= 1; side += 2) {
			for (int row = -1; row <= 1; row++) {
				body.getChildren().add(joint(joints, Rotate.Y_AXIS, side * 5.5, -3, row * 5, box(7, 1.2, 1.2, shell, side * 3.5, 0, 0), 0.4, row * 1.7 + (side > 0 ? 0 : Math.PI)));
			}
		}
		body.getChildren().add(part(new Sphere(1.2, 6), pink, 0, -5.5, 14, 1, 1, 1));
	
	}

	/** The salamander: its shape, and the joints that move it */
	static void salamander(Group body, List<Joint> joints) {
		PhongMaterial skin = paint(Color.web("#5a1d12"), 25);
		PhongMaterial belly = paint(Color.web("#ff7a1a"), 15);
		PhongMaterial spot = paint(Color.web("#ffb020"), 30);
		PhongMaterial gill = paint(Color.web("#e0301a"), 20);
		body.getChildren().addAll(part(new Box(7, 4, 17), skin, 0, -3, 0, 1, 1, 1), part(new Box(5, 1, 15), belly, 0, -1, 0, 1, 1, 1),
				part(new Sphere(4, 10), skin, 0, -4, 10, 1, 0.8, 1.1), tail(skin, 0, -3, -15));
		for (int side = -1; side <= 1; side += 2) {
			body.getChildren().add(part(new Box(0.4, 3, 2.5), gill, side * 4.3, -5, 8, 1, 1, 1));
			for (double posZ = -6; posZ <= 6; posZ += 4) {
				body.getChildren().add(part(new Sphere(0.9, 6), spot, side * 1.6, -5.4, posZ, 1, 0.5, 1));
			}
		}
		body.getChildren().addAll(eyes(2, -6, 13, 0.9));
		for (int side = -1; side <= 1; side += 2) {
			for (int row = -1; row <= 1; row += 2) {
				body.getChildren().add(joint(joints, Rotate.Y_AXIS, side * 4.5, -2, row * 5, box(6, 1, 2, skin, side * 3, 0, 0), 0.35, row + (side > 0 ? 0 : Math.PI)));
			}
		}
	
	}

	/** The bird: its shape, and the joints that move it */
	static void bird(Group body, List<Joint> joints) {
		PhongMaterial red = paint(Color.web("#e0452b"), 20);
		PhongMaterial yellow = paint(Color.web("#f2c230"), 20);
		PhongMaterial blue = paint(Color.web("#2a7fd1"), 20);
		PhongMaterial tip = paint(Color.web("#7fc4ff"), 20);
		PhongMaterial beak = paint(Color.web("#f0a020"), 20);
		body.getChildren().addAll(part(new Sphere(5, 10), red, 0, 0, 0, 0.9, 0.9, 1.4), part(new Sphere(3.2, 8), yellow, 0, -2, 5.5, 1, 1, 1),
				part(new Box(0.8, 3, 2), yellow, 0, -6.5, 4.5, 1, 1, 1), part(new Box(0.8, 2.5, 2), yellow, 0, -6, 6.5, 1, 1, 1), beak(beak, 0, -1.5, 9));
		body.getChildren().addAll(eyes(1.8, -2.6, 7, 1.1));
		for (int side = -1; side <= 1; side += 2) {
			body.getChildren().add(part(rotated(new Box(2.5, 0.3, 6), side * 12, Rotate.Y_AXIS), red, side * 1.5, 0, -9.5, 1, 1, 1));
			body.getChildren().add(cylinder(0.5, 5, beak, side * 1.5, 6.5, 0.5, 0));
			body.getChildren().add(part(new Box(3, 0.4, 2), beak, side * 1.5, 9, 1.5, 1, 1, 1));
			Group wing = new Group(box(18, 0.6, 6, blue, side * 9, 0, 0), part(new Box(4, 0.6, 6), tip, side * 17, 0, 0, 1, 1, 1));
			body.getChildren().add(joint(joints, Rotate.Z_AXIS, side * 3, -1, 0, wing, 0.8 * side, 0));
		}
	
	}

	/** The bat: its shape, and the joints that move it */
	static void bat(Group body, List<Joint> joints) {
		PhongMaterial fur = paint(Color.web("#1d1424"), 10);
		PhongMaterial membrane = paint(Color.web("#2b1d33"), 10);
		PhongMaterial rib = paint(Color.web("#4a3a5c"), 10);
		PhongMaterial red = paint(Color.web("#ff3030"), 40);
		body.getChildren().addAll(part(new Sphere(4, 10), fur, 0, 0, 0, 1, 0.9, 1.3), part(new Sphere(2.6, 8), fur, 0, -1, 4.5, 1, 1, 1));
		for (int side = -1; side <= 1; side += 2) {
			body.getChildren().add(cylinder(0.7, 3.5, fur, side * 1.6, -4.5, 4.5, 0));
			body.getChildren().add(part(new Sphere(0.7, 6), red, side * 1.1, -1.2, 6.6, 1, 1, 1));
			Group wing = new Group(box(16, 0.4, 8, membrane, side * 8, 0, 0));
			for (double posX = 4; posX <= 13; posX += 4.5) {
				wing.getChildren().add(box(0.4, 0.6, 8.5, rib, side * posX, -0.2, 0));
			}
			body.getChildren().add(joint(joints, Rotate.Z_AXIS, side * 2, 0, 0, wing, 0.9 * side, 0));
		}
	
	}

	/** Two eyes at x = ±x: a white with a black pupil in front of it */
	static List<Node> eyes(double posX, double posY, double posZ, double size) {
		PhongMaterial white = paint(Color.web("#f4f4ee"), 60);
		PhongMaterial pupil = paint(Color.web("#0a0a0a"), 60);
		List<Node> parts = new ArrayList<>();
		for (int side = -1; side <= 1; side += 2) {
			parts.add(part(new Sphere(size, 8), white, side * posX, posY, posZ, 1, 1, 1));
			parts.add(part(new Sphere(size * 0.55, 6), pupil, side * posX, posY, posZ + size * 0.6, 1, 1, 1));
		}
		return parts;
	}

	/** A cylinder standing on its middle, tilted sideways by degrees around Z */
	static Node cylinder(double radius, double height, PhongMaterial material, double posX, double posY, double posZ, double tilt) {
		Cylinder cylinder = new Cylinder(radius, height, 6);
		cylinder.setMaterial(material);
		if (tilt != 0) {
			cylinder.getTransforms().add(new Rotate(tilt, Rotate.Z_AXIS));
		}
		cylinder.setTranslateX(posX);
		cylinder.setTranslateY(posY);
		cylinder.setTranslateZ(posZ);
		return cylinder;
	}

	/** A box turned around an axis, before it is placed */
	static Box rotated(Box box, double angle, Point3D axis) {
		box.getTransforms().add(new Rotate(angle, axis));
		return box;
	}

	/** A material of the given colour and shine. */
	static PhongMaterial paint(Color color, double shine) {
		PhongMaterial material = new PhongMaterial(color);
		material.setSpecularColor(Color.gray(0.3));
		material.setSpecularPower(shine);
		return material;
	}

	/** A box of the given size, placed at the given position. */
	static Box box(double sizeX, double sizeY, double sizeZ, PhongMaterial material, double posX, double posY, double posZ) {
		Box box = new Box(sizeX, sizeY, sizeZ);
		return (Box) part(box, material, posX, posY, posZ, 1, 1, 1);
	}

	/** A joint: a hip at (x, y, z) that turns around the axis, with the child hanging from it */
	static Node joint(List<Joint> joints, Point3D axis, double posX, double posY, double posZ, Node child, double amplitude, double phase) {
		Rotate rotate = new Rotate(0, axis);
		Group hip = new Group(child);
		hip.setTranslateX(posX);
		hip.setTranslateY(posY);
		hip.setTranslateZ(posZ);
		hip.getTransforms().add(rotate);
		joints.add(new Joint(rotate, amplitude, phase));
		return hip;
	}

	/**
	 * A tail that stands up curved like a loose S seen from the side: it rises from
	 * the rump, swings back, forward and back again. Made of cylinders, one between each two points of
	 * the curve, that get thinner towards the tip, around its root at the origin
	 * (the back of the animal is at -z, up is -y).
	 */
	static Node curledTail(PhongMaterial material) {
		Group tail = new Group();
		int links = 16;
		Point3D from = null;
		for (int index = 0; index <= links; index++) {
			double time2 = (double) index / links;
			// Up along y, standing; it swings back, forward and back again along z, a
			// loose S: the swings are uneven and the tail wanders a little to the side
			Point3D point3D = new Point3D(2 * Math.sin(4.1 * time2 + 0.6), -20 * time2, -3 * time2 - 3.6 * Math.sin(2 * Math.PI * Math.pow(time2, 1.15)));
			if (from != null) {
				tail.getChildren().add(link(material, from, point3D, 1.9 - 0.7 * time2));
			}
			from = point3D;
		}
		return tail;
	}

	/**
	 * A cylinder from a point to another, a little longer so the links of a chain
	 * overlap at the bends
	 */
	static Node link(PhongMaterial material, Point3D from, Point3D point3D, double radius) {
		Point3D direction = point3D.subtract(from);
		Cylinder cylinder = new Cylinder(radius, direction.magnitude() * 1.15, 8);
		cylinder.setMaterial(material);
		Point3D middle = from.midpoint(point3D);
		// A cylinder stands along y: turned around the axis perpendicular to y and the direction
		cylinder.getTransforms().addAll(new Translate(middle.getX(), middle.getY(), middle.getZ()), new Rotate(Rotate.Y_AXIS.angle(direction), Rotate.Y_AXIS.crossProduct(direction)));
		return cylinder;
	}

	/** A tail, pointing along z, from the given position. */
	static Node tail(PhongMaterial material, double posX, double posY, double posZ) {
		return pointing(1.6, 16, material, posX, posY, posZ);
	}

	/** A beak, pointing along z, from the given position. */
	static Node beak(PhongMaterial material, double posX, double posY, double posZ) {
		return pointing(1.2, 4, material, posX, posY, posZ);
	}

	/** A cylinder lying along z, its center at x, y, z */
	static Node pointing(double radius, double length, PhongMaterial material, double posX, double posY, double posZ) {
		Cylinder cylinder = new Cylinder(radius, length, 6);
		cylinder.setMaterial(material);
		cylinder.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
		cylinder.setTranslateX(posX);
		cylinder.setTranslateY(posY);
		cylinder.setTranslateZ(posZ);
		return cylinder;
	}

	/** Places a shape at the given position and scale, with the given material, and returns it. */
	static <T extends Shape3D> T part(T shape, PhongMaterial material, double posX, double posY, double posZ, double scaleX, double scaleY, double scaleZ) {
		shape.setMaterial(material);
		shape.setTranslateX(posX);
		shape.setTranslateY(posY);
		shape.setTranslateZ(posZ);
		shape.setScaleX(scaleX);
		shape.setScaleY(scaleY);
		shape.setScaleZ(scaleZ);
		return shape;
	}
}
