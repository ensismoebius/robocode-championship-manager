package widgets.scenes;

import static widgets.scenes.ArenaMaterials.material;
import static widgets.scenes.ArenaMaterials.textured;

import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The garden of the Eden Solar arena: a round lawn around the field, drawn with hedges, flower beds, paths and
 * pools, a walkway with lamps around it, and a belt of lush plants and round trees up to the stands. Near the
 * field everything is low, so the camera always sees the fight over it; the trees grow only by the stands.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenGarden {

	private static final double WALKWAY = 40;
	private static final int SEGMENTS = 128;
	private static final double[] UP = { 0, -1, 0 };

	private final BattleArena3D host;
	/** The radius of the garden, and the radius of the front wall of the stands */
	private final double radius;
	private final double front;

	/** The garden of the given radius, with the stands beginning at the front radius. */
	EdenGarden(BattleArena3D host, double radius, double front) {
		this.host = host;
		this.radius = radius;
		this.front = front;
	}

	/** Builds the floor, the lawn, the walkway, the hedge, the paths and the plants. */
	void build(double width, double height, double corridor, Random wild) {
		PhongMaterial soil = textured(JungleTexture.MOSS, Color.gray(0.05));
		soil.setDiffuseColor(Color.web("#8aa57a"));
		double far = 6000;
		host.arena.getChildren().add(host.tiledQuad(new double[][] { { -far, 6, -far }, { far, 6, -far }, { far, 6, far }, { -far, 6, far } }, soil, 300));
		this.lawn(width, height);
		this.walkway();
		this.hedge();
		this.paths(corridor);
		this.plants(wild);
		this.trees(wild);
	}

	/** Whether a point is on the way from the garden to a corridor, at the ends of the field. */
	private static boolean onTheWay(double posX, double posZ, double margin) {
		return Math.abs(posZ) < margin && Math.abs(posZ) < Math.abs(posX);
	}

	/** The round lawn, one picture over all of it */
	private void lawn(double width, double height) {
		SurfaceMesh disc = new SurfaceMesh();
		for (int segment = 0; segment < SEGMENTS; segment++) {
			double first = Math.PI * 2 * segment / SEGMENTS;
			double second = Math.PI * 2 * (segment + 1) / SEGMENTS;
			double[] center = { 0, 0.2, 0 };
			double[] left = { this.radius * Math.cos(first), 0.2, this.radius * Math.sin(first) };
			double[] right = { this.radius * Math.cos(second), 0.2, this.radius * Math.sin(second) };
			disc.triangle(center, left, right, new float[] { 0.5f, 0.5f, this.texCoord(left[0]), this.texCoord(left[2]), this.texCoord(right[0]), this.texCoord(right[2]) }, UP);
		}
		PhongMaterial garden = textured(EdenTexture.garden(this.radius, width, height), Color.gray(0.05));
		host.arena.getChildren().add(disc.view(garden));
	}

	/** The texture coordinate of a position on the lawn. */
	private float texCoord(double position) {
		return (float) (0.5 + position / (2 * this.radius));
	}

	/** A walkway of pale stone around the lawn, with lamps along it */
	private void walkway() {
		LowPoly stone = new LowPoly();
		stone.ring(this.radius, this.radius + WALKWAY, 6, -1, SEGMENTS);
		LowPoly posts = new LowPoly();
		LowPoly lamps = new LowPoly();
		for (int index = 0; index < 60; index++) {
			double angle = Math.PI * 2 * (index + 0.5) / 60;
			double posX = (this.radius + WALKWAY / 2) * Math.cos(angle);
			double posZ = (this.radius + WALKWAY / 2) * Math.sin(angle);
			if (onTheWay(posX, posZ, 40)) {
				continue;
			}
			posts.cylinderY(posX, posZ, 1.4, -1, -15, 6);
			lamps.box(posX, -17, posZ, 4.5, 4, 4.5);
		}
		host.arena.getChildren().addAll(stone.view(material(Color.web("#cfcabd"))), posts.view(material(Color.web("#2b2b2b"))), lamps.view(EdenStadium.glowing("#ffc46b")));
	}

	/** A low hedge on the rim of the lawn, open where the paths go to the corridors */
	private void hedge() {
		LowPoly hedge = new LowPoly();
		double inner = this.radius - 24;
		double outer = this.radius - 6;
		for (int segment = 0; segment < SEGMENTS; segment++) {
			double first = Math.PI * 2 * segment / SEGMENTS;
			double second = Math.PI * 2 * (segment + 1) / SEGMENTS;
			double middle = (first + second) / 2;
			if (onTheWay(outer * Math.cos(middle), outer * Math.sin(middle), 40)) {
				continue;
			}
			hedge.frustum(new double[][] { { inner * Math.cos(first), inner * Math.sin(first) }, { outer * Math.cos(first), outer * Math.sin(first) }, { outer * Math.cos(second), outer * Math.sin(second) },
					{ inner * Math.cos(second), inner * Math.sin(second) } }, 0.2, -7, 0.9);
		}
		host.arena.getChildren().add(hedge.view(material(Color.web("#1d4520"))));
	}

	/** The paths of pale stone from the walkway to the corridors, at both ends of the field */
	private void paths(double corridor) {
		LowPoly path = new LowPoly();
		double start = this.radius + WALKWAY;
		double length = corridor - start;
		for (int side = -1; side <= 1; side += 2) {
			path.box(side * (start + length / 2), 5.6, 0, length, 1, 68);
		}
		host.arena.getChildren().add(path.view(material(Color.web("#bdb7a6"))));
	}

	/** A point of the belt of plants between the walkway and the stands, away from the paths. */
	private double[] spot(Random wild, double inner, double outer) {
		double posX;
		double posZ;
		do {
			double angle = wild.nextDouble() * Math.PI * 2;
			double distance = inner + wild.nextDouble() * (outer - inner);
			posX = Math.cos(angle) * distance;
			posZ = Math.sin(angle) * distance;
		} while (onTheWay(posX, posZ, 70));
		return new double[] { posX, posZ };
	}

	/** Ferns, bushes and flowers, everything low, in the belt between the walkway and the stands */
	private void plants(Random wild) {
		double inner = this.radius + WALKWAY + 8;
		double outer = this.front - 12;
		String[] tones = { "#2f7a2a", "#3a8f35", "#25662a" };
		TriangleMesh[] ferns = new TriangleMesh[tones.length];
		LowPoly[] bushes = new LowPoly[tones.length];
		for (int index = 0; index < tones.length; index++) {
			ferns[index] = new TriangleMesh();
			ferns[index].getTexCoords().addAll(0, 0);
			bushes[index] = new LowPoly();
		}
		for (int index = 0; index < 220; index++) {
			double[] spot = this.spot(wild, inner, outer);
			host.jungle.fern(ferns[wild.nextInt(ferns.length)], spot[0], spot[1], wild);
		}
		this.bushes(bushes, wild, inner, outer);
		for (int index = 0; index < tones.length; index++) {
			PhongMaterial green = material(Color.web(tones[index]));
			green.setSpecularColor(Color.gray(0.1));
			MeshView fern = new MeshView(ferns[index]);
			fern.setMaterial(green);
			fern.setCullFace(CullFace.NONE);
			host.arena.getChildren().addAll(fern, bushes[index].view(green));
		}
	}

	/** Bushes of every tone between the radii, some of them in flower */
	private void bushes(LowPoly[] bushes, Random wild, double inner, double outer) {
		String[] blossoms = { "#ff6fa3", "#d9479b", "#f2efe6", "#f2c14e", "#e8505b" };
		LowPoly[] flowers = new LowPoly[blossoms.length];
		for (int index = 0; index < blossoms.length; index++) {
			flowers[index] = new LowPoly();
		}
		for (int index = 0; index < 170; index++) {
			double[] spot = this.spot(wild, inner, outer);
			double size = 10 + wild.nextDouble() * 12;
			double tall = 12 + wild.nextDouble() * 16;
			bushes[wild.nextInt(bushes.length)].rock(spot[0], spot[1], 6, tall, size, size, 7, 0.55, 0.5, wild);
			// Some bushes are in flower
			for (int bloom = 0; bloom < (index % 3 == 0 ? 5 : 0); bloom++) {
				double angle = wild.nextDouble() * Math.PI * 2;
				double reach = wild.nextDouble() * size * 0.6;
				flowers[wild.nextInt(flowers.length)].rock(spot[0] + Math.cos(angle) * reach, spot[1] + Math.sin(angle) * reach, 6 - tall * 0.75, 4, 3, 3, 5, 0.5, 0.4, wild);
			}
		}
		for (int index = 0; index < blossoms.length; index++) {
			host.arena.getChildren().add(flowers[index].view(material(Color.web(blossoms[index]))));
		}
	}

	/** A round clump of leaves around its middle point: narrow below, wide in the middle, narrow on top */
	private static void blob(LowPoly crown, double posX, double posZ, double middle, double size, Random wild) {
		crown.rock(posX, posZ, middle + size * 0.75, size * 0.75, size * 0.55, size * 0.55, 9, 1.8, 0.25, wild);
		crown.rock(posX, posZ, middle, size * 0.85, size, size, 9, 0.45, 0.25, wild);
	}

	/** Round trees by the front wall of the stands, not in front of the corridors */
	private void trees(Random wild) {
		String[] tones = { "#1f4a24", "#2a5e2c", "#173d1d" };
		LowPoly[] crowns = new LowPoly[tones.length];
		for (int index = 0; index < tones.length; index++) {
			crowns[index] = new LowPoly();
		}
		LowPoly trunks = new LowPoly();
		for (int index = 0; index < 46; index++) {
			double[] spot = this.spot(wild, this.front - 95, this.front - 25);
			if (Math.abs(spot[1]) < 230) {
				continue;
			}
			double trunk = 30 + wild.nextDouble() * 15;
			trunks.cylinderY(spot[0], spot[1], 5 + wild.nextDouble() * 3, 6, -trunk, 7);
			LowPoly crown = crowns[wild.nextInt(crowns.length)];
			double size = 30 + wild.nextDouble() * 12;
			blob(crown, spot[0], spot[1], -trunk - size * 0.7, size, wild);
			for (int side = -1; side <= 1; side += 2) {
				double angle = wild.nextDouble() * Math.PI * 2;
				blob(crown, spot[0] + side * Math.cos(angle) * size * 0.6, spot[1] + side * Math.sin(angle) * size * 0.6, -trunk - size * 0.45, size * 0.65, wild);
			}
		}
		host.arena.getChildren().add(trunks.view(material(Color.web("#4a3424"))));
		for (int index = 0; index < tones.length; index++) {
			PhongMaterial leaves = material(Color.web(tones[index]));
			leaves.setSpecularColor(Color.gray(0.12));
			host.arena.getChildren().add(crowns[index].view(leaves));
		}
	}
}
