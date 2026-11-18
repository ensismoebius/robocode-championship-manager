package widgets.scenes;

import static widgets.scenes.ArenaMaterials.material;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The buildings of the solarpunk African city around the Eden Solar stadium: the baobab towers, whose trunks swell and
 * whose crowns are gardens crowned with solar leaves; the towers of green terraces with walls of Ndebele geometry; the
 * mud towers of the Sahel with their beams and their solar caps; the round houses with domes of solar panels; the solar
 * trees and the groves; the bridges between towers. Every form is built into one of the meshes of this kit, each with its
 * own material, so the whole city is a handful of nodes.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenForms {

	/** The height of the ground, in the units of the arena, where up is negative. */
	static final double GROUND = 6;
	/** How many units of a wall one repetition of its picture covers. */
	private static final double REPEAT = 128;

	final LowPoly mud = new LowPoly();
	final LowPoly leaves = new LowPoly();
	final LowPoly fresh = new LowPoly();
	final LowPoly solar = new LowPoly();
	final LowPoly wood = new LowPoly();
	final LowPoly cyan = new LowPoly();
	final LowPoly warm = new LowPoly();
	final LowPoly glass = new LowPoly();
	/** The walls painted with the Ndebele geometry, and the walls of mud. */
	final SurfaceMesh painted = new SurfaceMesh();
	final SurfaceMesh adobe = new SurfaceMesh();
	/** The glass skin of the tall towers. */
	final SurfaceMesh curtain = new SurfaceMesh();

	/** How many buildings of each kind were built: the city tells them to the tests. */
	int baobabs;
	int terraces;
	int spires;
	int houses;
	int trees;
	int skyscrapers;

	/** The corners {x, z} of a square, a rectangle of half sizes, around a middle, turned by the angle. */
	static double[][] rectangle(double posX, double posZ, double halfAlong, double halfAcross, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double[][] corners = { { -halfAlong, -halfAcross }, { halfAlong, -halfAcross }, { halfAlong, halfAcross }, { -halfAlong, halfAcross } };
		for (double[] corner : corners) {
			double along = corner[0];
			corner[0] = posX + along * cos - corner[1] * sin;
			corner[1] = posZ + along * sin + corner[1] * cos;
		}
		return corners;
	}

	/** The corners {x, z} of a regular polygon around a middle. */
	static double[][] polygon(double posX, double posZ, double radius, int sides, double phase) {
		double[][] corners = new double[sides][];
		for (int index = 0; index < sides; index++) {
			double angle = phase + Math.PI * 2 * index / sides;
			corners[index] = new double[] { posX + radius * Math.cos(angle), posZ + radius * Math.sin(angle) };
		}
		return corners;
	}

	/** The walls between two outlines, the bottom one at the height given and the top one at another, facing out, with the picture repeating. */
	static void walls(SurfaceMesh mesh, double[][] bottom, double bottomY, double[][] top, double topY) {
		int count = bottom.length;
		double midX = 0;
		double midZ = 0;
		for (double[] corner : bottom) {
			midX += corner[0] / count;
			midZ += corner[1] / count;
		}
		float rise = (float) ((bottomY - topY) / REPEAT);
		for (int index = 0; index < count; index++) {
			int next = (index + 1) % count;
			float run = (float) (Math.hypot(bottom[next][0] - bottom[index][0], bottom[next][1] - bottom[index][1]) / REPEAT);
			double[] facing = { (bottom[index][0] + bottom[next][0]) / 2 - midX, 0, (bottom[index][1] + bottom[next][1]) / 2 - midZ };
			mesh.quad(new double[] { bottom[index][0], bottomY, bottom[index][1] }, new double[] { bottom[next][0], bottomY, bottom[next][1] }, new double[] { top[next][0], topY, top[next][1] },
					new double[] { top[index][0], topY, top[index][1] }, new float[] { 0, rise, run, rise, run, 0, 0, 0 }, facing);
		}
	}

	/**
	 * A tower of green terraces: tiers that step back as they rise, their walls painted with Ndebele diamonds or of mud, a garden
	 * on each step with a bush and a strip of warm light under it, and at the top a solar canopy and, on the tall ones, a finial.
	 */
	void terraceTower(double posX, double posZ, double angle, double size, double height, Random wild) {
		this.terraces++;
		int tiers = Math.max(3, Math.min(9, (int) (height / 110)));
		boolean paint = wild.nextDouble() < 0.6;
		double sum = 0;
		for (int tier = 0; tier < tiers; tier++) {
			sum += 1.3 - 0.18 * tier;
		}
		double half = size / 2;
		double base = GROUND;
		for (int tier = 0; tier < tiers; tier++) {
			double tall = height * (1.3 - 0.18 * tier) / sum;
			double[][] outline = rectangle(posX, posZ, half, half, angle);
			double top = base - tall;
			walls(paint ? this.painted : this.adobe, outline, base, outline, top);
			this.fresh.frustum(rectangle(posX, posZ, half + 2, half + 2, angle), top + 1, top - 4, 0.97);
			this.warm.frustum(rectangle(posX, posZ, half + 1, half + 1, angle), top + 7, top + 4.5, 1);
			// A bush on the step the next tier leaves free
			double reach = half * 0.9;
			double side = angle + wild.nextInt(4) * Math.PI / 2;
			this.leaves.rock(posX + Math.cos(side) * reach, posZ + Math.sin(side) * reach, top - 3, 9, 6, 6, 6, 0.6, 0.5, wild);
			base = top;
			half *= 0.84;
		}
		this.solar.frustum(rectangle(posX, posZ, half * 1.4, half * 1.4, angle), base - 12, base - 15, 1);
		this.wood.box(posX, base - 6, posZ, 3, 12, 3);
		if (height > 260) {
			this.cyan.tube(new double[] { posX, base - 15, posZ }, new double[] { posX, base - 15 - height * 0.12, posZ }, 1.6, 6);
		}
	}

	/**
	 * A baobab tower: a trunk of mud with its root buttress that narrows, swells again and carries its crown, bands of cyan light
	 * around it, a few branches that end in gardens of leaves under a plate of solar cells.
	 */
	void baobabTower(double posX, double posZ, double size, double height, Random wild) {
		this.baobabs++;
		double radius = size / 2;
		double[] shares = { 0, 0.08, 0.4, 0.7, 0.82 };
		double[] widths = { 1.5, 1.0, 0.78, 0.95, 0.7 };
		for (int level = 0; level < shares.length - 1; level++) {
			this.mud.tube(new double[] { posX, GROUND - height * shares[level], posZ }, new double[] { posX, GROUND - height * shares[level + 1], posZ }, radius * widths[level], radius * widths[level + 1], 6);
		}
		this.cyan.tube(new double[] { posX, GROUND - height * 0.45, posZ }, new double[] { posX, GROUND - height * 0.45 - 3, posZ }, radius * 0.85, radius * 0.85, 6);
		double crownY = GROUND - height * 0.82;
		this.crown(posX, posZ, crownY, radius * 1.3, radius * 0.7);
		int branches = 3 + wild.nextInt(2);
		double turn = wild.nextDouble() * Math.PI;
		for (int branch = 0; branch < branches; branch++) {
			double angle = turn + Math.PI * 2 * branch / branches;
			double reach = radius * (1.6 + wild.nextDouble() * 0.6);
			double endX = posX + Math.cos(angle) * reach;
			double endZ = posZ + Math.sin(angle) * reach;
			double endY = crownY - height * (0.05 + wild.nextDouble() * 0.07);
			this.wood.tube(new double[] { posX, crownY, posZ }, new double[] { endX, endY, endZ }, 5, 3, 6);
			this.crown(endX, endZ, endY, radius * 0.7, radius * 0.38);
		}
	}

	/** A round garden of leaves on its stalk with a plate of solar cells over it. */
	private void crown(double posX, double posZ, double height, double garden, double plate) {
		this.leaves.frustum(polygon(posX, posZ, garden, 6, 0), height + 4, height - 12, 0.8);
		this.solar.frustum(polygon(posX, posZ, plate, 5, 0.3), height - 15, height - 18, 1);
		this.cyan.cylinderY(posX, posZ, plate * 0.12, height - 12, height - 15, 4);
	}

	/**
	 * A tower of mud of the Sahel: it narrows as it rises, rows of beams stand out of its walls, it has slits of warm light, and
	 * a cap of solar panels with a finial of light.
	 */
	void spireTower(double posX, double posZ, double angle, double size, double height) {
		this.spires++;
		double half = size / 2;
		double[][] bottom = rectangle(posX, posZ, half, half, angle);
		double[][] top = rectangle(posX, posZ, half * 0.5, half * 0.5, angle);
		walls(this.adobe, bottom, GROUND, top, GROUND - height);
		for (double share : new double[] { 0.35, 0.7 }) {
			double wide = half * (1 - 0.5 * share);
			for (int side = 0; side < 4; side++) {
				double facing = angle + side * Math.PI / 2;
				this.wood.frustum(rectangle(posX + Math.cos(facing) * wide, posZ + Math.sin(facing) * wide, 7, 2, facing), GROUND - height * share + 2, GROUND - height * share - 2, 1);
			}
		}
		this.solar.frustum(rectangle(posX, posZ, half * 0.8, half * 0.8, angle), GROUND - height, GROUND - height - 14, 0.3);
		this.cyan.tube(new double[] { posX, GROUND - height - 14, posZ }, new double[] { posX, GROUND - height - 40, posZ }, 1.4, 5);
	}

	/**
	 * A tower of the city of 2187: a shaft of glass with lit rooms that narrows and turns a little at every section, rings of cyan
	 * light between the sections, sky gardens that stand out every other section, fins of solar cells up its sides and, at the top,
	 * a crown of solar petals and a needle of light.
	 */
	void skyscraper(double posX, double posZ, double angle, double size, double height, Random wild) {
		this.skyscrapers++;
		int sides = wild.nextBoolean() ? 6 : 8;
		int sections = 4 + (int) (height / 280);
		double twist = (wild.nextDouble() - 0.5) * 0.4;
		double radius = size * 0.5;
		double tall = height * 0.94 / sections;
		double base = GROUND;
		double near = radius;
		for (int section = 0; section < sections; section++) {
			double phase = angle + twist * section;
			double far = radius * (1 - 0.5 * (section + 1.0) / sections);
			double top = base - tall;
			walls(this.curtain, polygon(posX, posZ, near, sides, phase), base, polygon(posX, posZ, far, sides, phase), top + 4);
			this.cyan.frustum(polygon(posX, posZ, far * 1.05, sides, phase), top + 4, top + 1, 1);
			if (section % 2 == 1) {
				this.fresh.frustum(polygon(posX, posZ, far * 1.2, sides, phase), top + 1, top - 5, 0.9);
			} else {
				this.solar.frustum(polygon(posX, posZ, far * 1.12, sides, phase), top + 1, top - 3, 1);
			}
			// Fins of solar cells on two opposite corners, every other section
			for (int side = 0; side < 2 && section % 2 == 0; side++) {
				double corner = phase + side * Math.PI;
				double finX = posX + Math.cos(corner) * (near + far) / 2;
				double finZ = posZ + Math.sin(corner) * (near + far) / 2;
				this.solar.frustum(rectangle(finX, finZ, 10, 2, corner), base - 6, top + 10, 1);
			}
			base = top;
			near = far;
		}
		this.solar.frustum(polygon(posX, posZ, near * 1.6, sides, angle), base, base - 8, 0.4);
		this.cyan.tube(new double[] { posX, base - 8, posZ }, new double[] { posX, base - 8 - height * 0.08, posZ }, 2.4, 0.6, 6);
	}

	/** A round house with its dome covered in solar panels and a lit door. */
	void domeHouse(double posX, double posZ, double size, double facing, Random wild) {
		this.houses++;
		double radius = size * 0.42;
		double wall = 26 + wild.nextDouble() * 18;
		this.mud.cylinderY(posX, posZ, radius, GROUND, GROUND - wall, 6);
		this.solar.frustum(polygon(posX, posZ, radius * 1.02, 6, 0), GROUND - wall, GROUND - wall - radius * 0.5, 0.62);
		this.solar.frustum(polygon(posX, posZ, radius * 0.63, 6, 0), GROUND - wall - radius * 0.5, GROUND - wall - radius * 0.85, 0.3);
		this.warm.frustum(rectangle(posX + Math.cos(facing) * radius, posZ + Math.sin(facing) * radius, 1.5, 5, facing), GROUND, GROUND - 17, 1);
	}

	/** A solar tree: a slim trunk with plates of solar cells like leaves, narrower toward the top, and a bulb of light. */
	void solarTree(double posX, double posZ, double height) {
		this.trees++;
		this.wood.tube(new double[] { posX, GROUND, posZ }, new double[] { posX, GROUND - height, posZ }, 3, 1.8, 4);
		for (int leaf = 0; leaf < 2; leaf++) {
			double share = 0.65 + 0.3 * leaf;
			this.solar.frustum(polygon(posX, posZ, 26 - 8 * leaf, 5, leaf), GROUND - height * share + 2, GROUND - height * share - 1, 1);
		}
		this.cyan.cylinderY(posX, posZ, 3, GROUND - height, GROUND - height - 7, 4);
	}

	/** A few round trees together, the plain greenery of the city. */
	void grove(double posX, double posZ, double spread, Random wild) {
		for (int tree = 0; tree < 2; tree++) {
			double angle = wild.nextDouble() * Math.PI * 2;
			double treeX = posX + Math.cos(angle) * spread * wild.nextDouble();
			double treeZ = posZ + Math.sin(angle) * spread * wild.nextDouble();
			double trunk = 18 + wild.nextDouble() * 10;
			this.wood.cylinderY(treeX, treeZ, 3, GROUND, GROUND - trunk, 4);
			double size = 17 + wild.nextDouble() * 8;
			(wild.nextBoolean() ? this.leaves : this.fresh).rock(treeX, treeZ, GROUND - trunk + 4, size * 1.6, size, size, 6, 0.55, 0.4, wild);
		}
	}

	/** A bridge of glass between two points, with a thread of light under it. */
	void bridge(double[] from, double[] end) {
		this.glass.tube(from, end, 4, 5);
		this.cyan.tube(new double[] { from[0], from[1] + 3, from[2] }, new double[] { end[0], end[1] + 3, end[2] }, 1.2, 4);
	}

	/** The meshes that have something in them, each with its material. */
	List<MeshView> views() {
		List<MeshView> views = new ArrayList<>();
		PhongMaterial paint = ArenaMaterials.textured(EdenCityTexture.NDEBELE, Color.gray(0.05));
		paint.setSelfIlluminationMap(EdenCityTexture.NDEBELE_LIGHT);
		PhongMaterial clay = ArenaMaterials.textured(EdenCityTexture.ADOBE, Color.gray(0.05));
		clay.setSelfIlluminationMap(EdenCityTexture.ADOBE_LIGHT);
		PhongMaterial skin = ArenaMaterials.textured(EdenCityTexture.CURTAIN, Color.gray(0.6));
		skin.setSelfIlluminationMap(EdenCityTexture.CURTAIN_LIGHT);
		PhongMaterial panels = material(Color.web("#12345a"));
		panels.setSelfIlluminationMap(ArenaHelpers.solidMap(Color.web("#06223a")));
		PhongMaterial pane = material(Color.web("#bfe3e0"));
		pane.setSpecularColor(Color.WHITE);
		views.add(this.painted.view(paint));
		views.add(this.adobe.view(clay));
		views.add(this.curtain.view(skin));
		views.add(this.mud.view(material(Color.web("#b9683d"))));
		views.add(this.leaves.view(material(Color.web("#2f7a2a"))));
		views.add(this.fresh.view(material(Color.web("#4aa83c"))));
		views.add(this.solar.view(panels));
		views.add(this.wood.view(material(Color.web("#6b4a2c"))));
		views.add(this.cyan.view(EdenStadium.glowing("#3be0d0")));
		views.add(this.warm.view(EdenStadium.glowing("#ffb24a")));
		views.add(this.glass.view(pane));
		return views;
	}
}
