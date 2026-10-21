package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The ice arena: plates of ice with bright seams, crystals on the horizon, a
 * frozen sea with penguins and the aurora that waves over the sky. It builds into
 * the arena of its host and animates the aurora every frame.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class IceArena {

	/** Seconds between two paintings of the aurora */
	private static final double AURORA_STEP = 0.05;

	private final BattleArena3D host;
	private Aurora aurora;
	private double auroraClock;
	private double auroraWait;
	private double seaX;
	private double seaZ;
	private double seaRadius;

	IceArena(BattleArena3D host) {
		this.host = host;
	}

	/** Where the sea is, its radius is 0 when there is none */
	double seaX() {
		return this.seaX;
	}

	double seaZ() {
		return this.seaZ;
	}

	double seaRadius() {
		return this.seaRadius;
	}

	/** Paints the aurora again a few times a second, as the battle goes on */
	void update(double dt) {
		if (this.aurora != null) {
			this.auroraClock += dt;
			this.auroraWait += dt;
			if (this.auroraWait >= AURORA_STEP) {
				this.auroraWait = 0;
				this.aurora.update(this.auroraClock);
			}
		}
	}

	/**
	 * Plates of ice with bright seams, and crystals of ice on the horizon
	 */
	void build(double w, double h) {
		Random crystals = new Random(23);
		// The sky keeps its color and has an aurora that waves over it
		this.aurora = new Aurora(host.style.sky);
		host.arena.getChildren().add(BattleArena3D.skyDome(this.aurora.image()));
		Box ground = new Box(w * 6, 2, h * 6);
		ground.setMaterial(BattleArena3D.material(Color.web("#8ea4b8")));
		ground.setTranslateY(6);

		// The seams: a light floor under the plates
		Box seams = new Box(w + 40, 4, h + 40);
		seams.setMaterial(BattleArena3D.material(Color.web("#e4f3ff")));
		seams.setTranslateY(2.6);
		host.arena.getChildren().addAll(ground, seams);

		// Each plate is a block of ice with its own veins, frost and cracks
		PhongMaterial[] ice = new PhongMaterial[3];
		int[] tones = { 0x7fa9cc, 0x8cb8dc, 0x6f98c0 };
		for (int i = 0; i < ice.length; i++) {
			ice[i] = BattleArena3D.textured(IceTexture.block(tones[i], 5 + i), Color.WHITE);
			ice[i].setSpecularPower(90);
		}
		for (double x = 0; x < w; x += 100) {
			for (double z = 0; z < h; z += 100) {
				Box plate = BattleArena3D.box(Math.min(100, w - x) - 2, 4, Math.min(100, h - z) - 2, x + Math.min(100, w - x) / 2 - w / 2, 2, z + Math.min(100, h - z) / 2 - h / 2,
						ice[crystals.nextInt(ice.length)]);
				host.arena.getChildren().add(plate);
			}
		}

		// The glacial sea west of the field, beyond the snow of its edges
		double seaRadius = 600;
		double seaX = -(w / 2 + 60 + 1.25 * seaRadius);
		double seaZ = h * 0.1;
		this.buildSea(seaX, seaZ, seaRadius, crystals);
		this.seaX = seaX;
		this.seaZ = seaZ;
		this.seaRadius = seaRadius;

		// Snow piled outside the plates, with shards of ice standing in it
		PhongMaterial snow = BattleArena3D.material(Color.web("#eef6fc"));
		snow.setSpecularPower(20);
		PhongMaterial shardMaterial = BattleArena3D.material(Color.web("#d6f1ff"));
		shardMaterial.setSpecularColor(Color.WHITE);
		shardMaterial.setSpecularPower(100);
		TriangleMesh shards = new TriangleMesh();
		shards.getTexCoords().addAll(0, 0);
		for (int i = 0; i < 90; i++) {
			// A point just outside one of the four edges of the field
			int side = crystals.nextInt(4);
			double off = 8 + crystals.nextDouble() * 40;
			double along = crystals.nextDouble() - 0.5;
			double x = side < 2 ? (side == 0 ? -1 : 1) * (w / 2 + off) : along * (w + 80);
			double z = side < 2 ? along * (h + 80) : (side == 2 ? -1 : 1) * (h / 2 + off);
			if (Math.hypot(x - seaX, z - seaZ) < seaRadius * 1.3) {
				continue;
			}
			if (i % 3 == 0) {
				// A drift of snow, half sunk in the ground
				double radius = 6 + crystals.nextDouble() * 14;
				Sphere drift = new Sphere(radius, 10);
				drift.setMaterial(snow);
				drift.setScaleY(0.45);
				drift.setTranslateX(x);
				drift.setTranslateY(5 - radius * 0.2);
				drift.setTranslateZ(z);
				host.arena.getChildren().add(drift);
			} else {
				IceArena.crystal(shards, x, 5, z, 2 + crystals.nextDouble() * 3, 8 + crystals.nextDouble() * 14, 5, crystals);
			}
		}
		this.addIce(shards, shardMaterial);

		// Clusters of crystals far away, out of the sea
		TriangleMesh far = new TriangleMesh();
		far.getTexCoords().addAll(0, 0);
		for (int i = 0; i < 12; i++) {
			double angle = i * Math.PI * 2 / 12 + crystals.nextDouble() * 0.3;
			double distance = 900 + crystals.nextDouble() * 800;
			double cx = Math.cos(angle) * distance;
			double cz = Math.sin(angle) * distance;
			if (Math.hypot(cx - seaX, cz - seaZ) < seaRadius * 1.4) {
				continue;
			}
			int count = 3 + crystals.nextInt(3);
			for (int k = 0; k < count; k++) {
				double height = 90 + crystals.nextDouble() * 240;
				IceArena.crystal(far, cx + (crystals.nextDouble() - 0.5) * 120, 8, cz + (crystals.nextDouble() - 0.5) * 120, 18 + crystals.nextDouble() * 28, height, 6 + crystals.nextInt(2), crystals);
			}
		}
		PhongMaterial crystalMaterial = new PhongMaterial(Color.web("#a9dcf5"));
		crystalMaterial.setSpecularColor(Color.WHITE);
		crystalMaterial.setSpecularPower(100);
		this.addIce(far, crystalMaterial);
	}

	/**
	 * A glacial sea: open dark water with a wavy shore, floes of cracked ice drifting
	 * over it, icebergs standing in it and snow along the shore
	 */
	private void buildSea(double centreX, double centreZ, double radius, Random random) {
		double surface = 4.6;
		int sides = 72;
		TriangleMesh water = new TriangleMesh();
		water.getTexCoords().addAll(0, 0);
		water.getPoints().addAll((float) centreX, (float) surface, (float) centreZ);
		for (int i = 0; i < sides; i++) {
			double angle = i * Math.PI * 2 / sides;
			double r = shore(angle, radius);
			water.getPoints().addAll((float) (centreX + Math.cos(angle) * r), (float) surface, (float) (centreZ + Math.sin(angle) * r));
		}
		for (int i = 0; i < sides; i++) {
			int a = 1 + i;
			int b = 1 + (i + 1) % sides;
			water.getFaces().addAll(0, 0, a, 0, b, 0);
		}
		PhongMaterial sea = BattleArena3D.material(Color.web("#14365e"));
		sea.setSpecularColor(Color.WHITE);
		sea.setSpecularPower(90);
		this.addIce(water, sea);

		// The floes: rough discs of cracked ice, in several sizes, kept off the shore
		TriangleMesh floes = new TriangleMesh();
		for (int i = 0; i < 150; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = Math.sqrt(random.nextDouble()) * shore(angle, radius) * 0.85;
			double fx = centreX + Math.cos(angle) * distance;
			double fz = centreZ + Math.sin(angle) * distance;
			double size = 14 + random.nextDouble() * 32;
			int floeSides = 7 + random.nextInt(3);
			int offset = floes.getPoints().size() / 3;
			floes.getPoints().addAll((float) fx, (float) (surface - 0.2), (float) fz);
			floes.getTexCoords().addAll(0.5f, 0.5f);
			for (int k = 0; k < floeSides; k++) {
				double a = k * Math.PI * 2 / floeSides + random.nextDouble() * 0.3;
				double r = size * (0.7 + 0.35 * random.nextDouble());
				double cos = Math.cos(a);
				double sin = Math.sin(a);
				floes.getPoints().addAll((float) (fx + cos * r), (float) (surface - 0.2 - random.nextDouble() * 0.3), (float) (fz + sin * r));
				floes.getTexCoords().addAll((float) (0.5 + cos * 0.5), (float) (0.5 + sin * 0.5));
			}
			for (int k = 0; k < floeSides; k++) {
				int a = offset + 1 + k;
				int b = offset + 1 + (k + 1) % floeSides;
				floes.getFaces().addAll(offset, offset, a, a, b, b);
			}
		}
		PhongMaterial floeIce = BattleArena3D.textured(IceTexture.block(0xc4ecfa, 11), Color.WHITE);
		floeIce.setSpecularPower(60);
		this.addIce(floes, floeIce);

		// Icebergs standing in the water
		TriangleMesh bergs = new TriangleMesh();
		bergs.getTexCoords().addAll(0, 0);
		for (int i = 0; i < 7; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = Math.sqrt(random.nextDouble()) * shore(angle, radius) * 0.6;
			IceArena.crystal(bergs, centreX + Math.cos(angle) * distance, surface + 0.4, centreZ + Math.sin(angle) * distance, 40 + random.nextDouble() * 40, 30 + random.nextDouble() * 50, 8, random);
		}
		PhongMaterial berg = BattleArena3D.material(Color.web("#e8f8ff"));
		berg.setSpecularColor(Color.WHITE);
		berg.setSpecularPower(100);
		this.addIce(bergs, berg);

		// Snow along the shore
		PhongMaterial snow = BattleArena3D.material(Color.web("#eef6fc"));
		snow.setSpecularPower(20);
		for (int i = 0; i < sides; i += 3) {
			double angle = i * Math.PI * 2 / sides;
			double r = shore(angle, radius) * 1.03;
			Sphere rim = new Sphere(9 + random.nextDouble() * 10, 8);
			rim.setMaterial(snow);
			rim.setScaleY(0.5);
			rim.setTranslateX(centreX + Math.cos(angle) * r);
			rim.setTranslateY(surface - 0.4);
			rim.setTranslateZ(centreZ + Math.sin(angle) * r);
			host.arena.getChildren().add(rim);
		}
	}

	/** The distance from the middle of the sea to its shore, at an angle: wavy, never a circle */
	static double shore(double angle, double radius) {
		return radius * (1 + 0.10 * Math.sin(3 * angle + 0.7) + 0.06 * Math.sin(7 * angle + 2.1) + 0.03 * Math.sin(13 * angle + 0.4));
	}

	/** Adds a mesh of ice, with its material, to the arena */
	private void addIce(TriangleMesh mesh, PhongMaterial material) {
		MeshView view = new MeshView(mesh);
		view.setMaterial(material);
		view.setCullFace(CullFace.NONE);
		host.arena.getChildren().add(view);
	}

	/**
	 * A crystal of ice: a faceted spike on its base, leaning, its tip off the middle.
	 * It is added to the mesh, so many crystals make one mesh
	 */
	static void crystal(TriangleMesh mesh, double x, double baseY, double z, double radius, double height, int sides, Random random) {
		int offset = mesh.getPoints().size() / 3;
		double tipX = x + (random.nextDouble() - 0.5) * radius;
		double tipZ = z + (random.nextDouble() - 0.5) * radius;
		double turn = random.nextDouble() * Math.PI * 2;
		double middleX = x + (tipX - x) * 0.5;
		double middleZ = z + (tipZ - z) * 0.5;
		for (int i = 0; i < sides; i++) {
			double angle = turn + i * Math.PI * 2 / sides;
			double r = radius * (0.75 + 0.35 * random.nextDouble());
			mesh.getPoints().addAll((float) (x + Math.cos(angle) * r), (float) baseY, (float) (z + Math.sin(angle) * r));
		}
		for (int i = 0; i < sides; i++) {
			double angle = turn + (i + 0.5) * Math.PI * 2 / sides;
			double r = radius * (0.5 + 0.3 * random.nextDouble());
			mesh.getPoints().addAll((float) (middleX + Math.cos(angle) * r), (float) (baseY - height * 0.5), (float) (middleZ + Math.sin(angle) * r));
		}
		mesh.getPoints().addAll((float) tipX, (float) (baseY - height), (float) tipZ);
		int tip = offset + 2 * sides;
		for (int i = 0; i < sides; i++) {
			int base = offset + i;
			int nextBase = offset + (i + 1) % sides;
			int middle = offset + sides + i;
			int nextMiddle = offset + sides + (i + 1) % sides;
			mesh.getFaces().addAll(base, 0, nextBase, 0, nextMiddle, 0, base, 0, nextMiddle, 0, middle, 0);
			mesh.getFaces().addAll(middle, 0, nextMiddle, 0, tip, 0);
		}
	}

}
