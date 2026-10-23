package widgets.scenes;

import static widgets.scenes.ArenaMaterials.material;
import static widgets.scenes.ArenaMaterials.textured;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * A tree of life of the Eden Solar arena, a baobab as in the photo of a Grandidier's baobab: a tall, smooth, copper
 * trunk with rings running around it and a knobby shoulder at the top; a few thick, bent limbs spread wide from the
 * shoulder, thin twigs at their tips and a flat crown of leaves like an umbrella. Its rings glow faintly and its
 * leaves sparkle. It stands on a round white platform with rings of light, and warms the floor around it. One
 * stands at the mouth of each corridor, outside the field.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenTree {

	/** The trunk, from the platform to the knobby shoulder where the limbs begin: height and radius at each level */
	private static final double[] HEIGHTS = { -5, -25, -140, -280, -370, -415, -445, -468, -485 };
	private static final double[] RADII = { 74, 65, 57, 54, 55, 65, 85, 78, 56 };
	/** How much the bark of each level is pushed in or out: the trunk is smooth, the shoulder is knobby */
	private static final double[] KNOBS = { 0, 0.01, 0.02, 0.02, 0.03, 0.08, 0.12, 0.10, 0.06 };
	private static final int SIDES = 28;
	/** How far each limb reaches and how much it rises: five spread to the sides, the last one goes straight up */
	private static final double[] REACHES = { 290, 190, 260, 160, 230, 30 };
	private static final double[] RISES = { 70, 95, 60, 100, 80, 120 };
	private static final double SHOULDER = -465;
	private static final double[] UP = { 0, -1, 0 };

	private final BattleArena3D host;

	/** The trees of the arena, drawn in the host scene. */
	EdenTree(BattleArena3D host) {
		this.host = host;
	}

	/** A tree on the x axis, at the position. */
	void build(double posX, Random wild) {
		this.platform(posX);
		this.trunk(posX, wild);
		LowPoly limbs = new LowPoly();
		List<double[]> ends = new ArrayList<>();
		for (int index = 0; index < REACHES.length; index++) {
			double angle = Math.PI * 2 * (index + wild.nextDouble() * 0.5) / (REACHES.length - 1);
			double[] tip = limb(limbs, posX, angle, REACHES[index], RISES[index], (wild.nextDouble() - 0.5) * 0.8);
			boolean upright = index == REACHES.length - 1;
			ends.add(tip);
			ends.addAll(twigs(limbs, tip, angle, 4, upright ? Math.PI * 2 : 1.6, wild));
		}
		host.arena.getChildren().add(limbs.view(material(Color.web("#8a5a3e"))));
		this.crown(ends, wild);
	}

	/** A flat ring around a point, from one height up to another, added to the shape. */
	private static void ring(LowPoly shape, double centerX, double inner, double outer, double bottom, double top) {
		int segments = 48;
		for (int index = 0; index < segments; index++) {
			double first = Math.PI * 2 * index / segments;
			double second = Math.PI * 2 * (index + 1) / segments;
			shape.frustum(new double[][] { { centerX + inner * Math.cos(first), inner * Math.sin(first) }, { centerX + outer * Math.cos(first), outer * Math.sin(first) },
					{ centerX + outer * Math.cos(second), outer * Math.sin(second) }, { centerX + inner * Math.cos(second), inner * Math.sin(second) } }, bottom, top, 1);
		}
	}

	/** A point on the x axis, at the angle, the radius and the height. */
	private static double[] point(double posX, double angle, double radius, double height) {
		return new double[] { posX + radius * Math.cos(angle), height, radius * Math.sin(angle) };
	}

	/** The white platform with its rings of light, and the warm light it throws on the floor around it */
	private void platform(double posX) {
		SurfaceMesh glow = new SurfaceMesh();
		double reach = 320;
		int segments = 64;
		for (int index = 0; index < segments; index++) {
			double first = Math.PI * 2 * index / segments;
			double second = Math.PI * 2 * (index + 1) / segments;
			double[] left = { posX + reach * Math.cos(first), 4.8, reach * Math.sin(first) };
			double[] right = { posX + reach * Math.cos(second), 4.8, reach * Math.sin(second) };
			glow.triangle(new double[] { posX, 4.8, 0 }, left, right, new float[] { 0.5f, 0.5f, (float) (0.5 + 0.5 * Math.cos(first)), (float) (0.5 + 0.5 * Math.sin(first)), (float) (0.5 + 0.5 * Math.cos(second)),
					(float) (0.5 + 0.5 * Math.sin(second)) }, UP);
		}
		// Clear, but for its light: the light adds to the floor under it
		PhongMaterial light = new PhongMaterial(Color.TRANSPARENT);
		light.setSelfIlluminationMap(EdenTexture.GLOW);

		LowPoly stone = new LowPoly();
		stone.cylinderY(posX, 0, 95, 6, -5, 48);
		LowPoly rings = new LowPoly();
		ring(rings, posX, 95, 103, 6, -7);
		ring(rings, posX, 56, 62, -5, -6.5);
		host.arena.getChildren().addAll(glow.view(light), stone.view(material(Color.web("#e4e1d8"))), rings.view(EdenStadium.glowing("#ffb547")));
	}

	/** A point of the bark of the trunk, at a level and a side, pushed in or out by the noise. */
	private static double[] surface(double posX, int level, int side, double[][] noise) {
		int wrapped = Math.floorMod(side, SIDES);
		double radius = RADII[level] * (1 + KNOBS[level] * noise[level][wrapped]);
		return point(posX, Math.PI * 2 * wrapped / SIDES, radius, HEIGHTS[level]);
	}

	/** The trunk: a smooth copper column, with its rings around it, and the knobby shoulder at the top */
	private void trunk(double posX, Random wild) {
		double[][] noise = new double[HEIGHTS.length][SIDES];
		for (double[] level : noise) {
			for (int side = 0; side < SIDES; side++) {
				level[side] = wild.nextDouble() * 2 - 1;
			}
		}
		SurfaceMesh bark = new SurfaceMesh();
		for (int level = 0; level < HEIGHTS.length - 1; level++) {
			float low = (float) (-HEIGHTS[level] / 190);
			float high = (float) (-HEIGHTS[level + 1] / 190);
			for (int side = 0; side < SIDES; side++) {
				float first = (float) side / SIDES;
				float second = (float) (side + 1) / SIDES;
				double angle = Math.PI * 2 * (side + 0.5) / SIDES;
				bark.quad(surface(posX, level, side, noise), surface(posX, level, side + 1, noise), surface(posX, level + 1, side + 1, noise), surface(posX, level + 1, side, noise),
						new float[] { first, low, second, low, second, high, first, high }, new double[] { Math.cos(angle), 0, Math.sin(angle) });
			}
		}
		// The top of the shoulder, where the limbs leave
		int top = HEIGHTS.length - 1;
		for (int side = 0; side < SIDES; side++) {
			bark.triangle(new double[] { posX, HEIGHTS[top], 0 }, surface(posX, top, side, noise), surface(posX, top, side + 1, noise), new float[] { 0, 0, 0, 0, 0, 0 }, UP);
		}
		PhongMaterial copper = textured(EdenTexture.BARK, Color.gray(0.05));
		copper.setSelfIlluminationMap(EdenTexture.BARK_GLOW);
		host.arena.getChildren().add(bark.view(copper));
	}

	/** How thick a limb is at that share of its length: stout at the shoulder, thin at the tip */
	private static double thickness(double share) {
		return 30 - 22 * share;
	}

	/** A limb goes out and up from the shoulder, bending as it goes, and gets thinner toward its tip; returns the tip */
	private static double[] limb(LowPoly limbs, double posX, double angle, double reach, double rise, double curl) {
		int steps = 6;
		double[] previous = point(posX, angle, 25, SHOULDER);
		for (int step = 1; step <= steps; step++) {
			double share = (double) step / steps;
			double bend = angle + curl * Math.sin(share * Math.PI * 0.8);
			double[] next = point(posX, bend, 25 + (reach - 25) * share, SHOULDER - rise * (1 - Math.pow(1 - share, 2.2)));
			limbs.tube(previous, next, thickness(share - 1.0 / steps), thickness(share), 7);
			previous = next;
		}
		return previous;
	}

	/** Thin twigs that spread from the tip of a limb, a little up; returns where they end */
	private static List<double[]> twigs(LowPoly limbs, double[] tip, double angle, int count, double spread, Random wild) {
		List<double[]> ends = new ArrayList<>();
		for (int twig = 0; twig < count; twig++) {
			double direction = angle + (wild.nextDouble() - 0.5) * spread;
			double length = 45 + wild.nextDouble() * 30;
			double[] end = { tip[0] + Math.cos(direction) * length, tip[1] - 10 - wild.nextDouble() * 14, tip[2] + Math.sin(direction) * length };
			limbs.tube(tip, end, 7, 3, 5);
			ends.add(end);
		}
		return ends;
	}

	/** The crown: wide, flat layers of leaves over the ends of the twigs, like an umbrella */
	private void crown(List<double[]> ends, Random wild) {
		LowPoly layers = new LowPoly();
		for (double[] end : ends) {
			double size = 100 + wild.nextDouble() * 40;
			layers.rock(end[0], end[2], end[1] + 8, 14 + wild.nextDouble() * 9, size, size * (0.8 + wild.nextDouble() * 0.2), 10, 0.85, 0.3, wild);
		}
		PhongMaterial foliage = textured(JungleTexture.LEAVES, Color.gray(0.15));
		foliage.setDiffuseColor(Color.web("#bcd47e"));
		foliage.setSelfIlluminationMap(EdenTexture.SPARKLES);
		host.arena.getChildren().add(layers.view(foliage));
	}
}
