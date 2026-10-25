package widgets.scenes;

import java.util.Random;
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
	private static final double SEA_SURFACE = 4.6;
	private static final int SEA_SIDES = 72;

	private final BattleArena3D host;
	private Aurora aurora;
	private double auroraClock;
	private double auroraWait;
	private double seaX;
	private double seaZ;
	private double seaRadius;

	/** Creates the ice of the arena, drawn in the host scene. */
	IceArena(BattleArena3D host) {
		this.host = host;
	}

	/** Where the sea is, its radius is 0 when there is none */
	double seaX() {
		return this.seaX;
	}

	/** The z coordinate of the centre of the sea. */
	double seaZ() {
		return this.seaZ;
	}

	/** The radius of the sea. */
	double seaRadius() {
		return this.seaRadius;
	}

	/** Paints the aurora again a few times a second, as the battle goes on */
	void update(double deltaTime) {
		if (this.aurora != null) {
			this.auroraClock += deltaTime;
			this.auroraWait += deltaTime;
			if (this.auroraWait >= AURORA_STEP) {
				this.auroraWait = 0;
				this.aurora.update(this.auroraClock);
			}
		}
	}

	/**
	 * Plates of ice with bright seams, and crystals of ice on the horizon
	 */
	void build(double width, double height) {
		Random crystals = new Random(23);
		// The sky keeps its color and has an aurora that waves over it
		this.aurora = new Aurora(host.style.sky);
		host.arena.getChildren().add(ArenaMaterials.skyDome(this.aurora.image()));
		this.floor(width, height);
		this.plates(width, height, crystals);
		// The glacial sea west of the field, beyond the snow of its edges
		double seaRadius = 600;
		double seaX = -(width / 2 + 60 + 1.25 * seaRadius);
		double seaZ = height * 0.1;
		this.buildSea(seaX, seaZ, seaRadius, crystals);
		this.seaX = seaX;
		this.seaZ = seaZ;
		this.seaRadius = seaRadius;
		this.shards(width, height, crystals);
		this.farCrystals(crystals);
	}

	/** The ground around the field and the light seams under the plates */
	private void floor(double width, double height) {
		Box ground = new Box(width * 6, 2, height * 6);
		ground.setMaterial(ArenaMaterials.material(Color.web("#8ea4b8")));
		ground.setTranslateY(6);

		// The seams: a light floor under the plates
		Box seams = new Box(width + 40, 4, height + 40);
		seams.setMaterial(ArenaMaterials.material(Color.web("#e4f3ff")));
		seams.setTranslateY(2.6);
		host.arena.getChildren().addAll(ground, seams);
	}

	/** Each plate is a block of ice with its own veins, frost and cracks */
	private void plates(double width, double height, Random crystals) {
		// Each plate is a block of ice with its own veins, frost and cracks
		PhongMaterial[] ice = new PhongMaterial[3];
		int[] tones = { 0x7fa9cc, 0x8cb8dc, 0x6f98c0 };
		for (int index = 0; index < ice.length; index++) {
			ice[index] = ArenaMaterials.textured(IceTexture.block(tones[index], 5 + index), Color.WHITE);
			ice[index].setSpecularPower(90);
		}
		for (double posX = 0; posX < width; posX += 100) {
			for (double posZ = 0; posZ < height; posZ += 100) {
				Box plate = ArenaMaterials.box(Math.min(100, width - posX) - 2, 4, Math.min(100, height - posZ) - 2, posX + Math.min(100, width - posX) / 2 - width / 2, 2, posZ + Math.min(100, height - posZ) / 2 - height / 2,
						ice[crystals.nextInt(ice.length)]);
				host.arena.getChildren().add(plate);
			}
		}
	}

	/** Snow piled outside the plates, with shards of ice standing in it */
	private void shards(double width, double height, Random crystals) {
		// Snow piled outside the plates, with shards of ice standing in it
		PhongMaterial snow = ArenaMaterials.material(Color.web("#eef6fc"));
		snow.setSpecularPower(20);
		PhongMaterial shardMaterial = ArenaMaterials.material(Color.web("#d6f1ff"));
		shardMaterial.setSpecularColor(Color.WHITE);
		shardMaterial.setSpecularPower(100);
		TriangleMesh shards = new TriangleMesh();
		shards.getTexCoords().addAll(0, 0);
		for (int index = 0; index < 90; index++) {
			// A point just outside one of the four edges of the field
			int side = crystals.nextInt(4);
			double off = 8 + crystals.nextDouble() * 40;
			double along = crystals.nextDouble() - 0.5;
			double posX = side < 2 ? (side == 0 ? -1 : 1) * (width / 2 + off) : along * (width + 80);
			double posZ = side < 2 ? along * (height + 80) : (side == 2 ? -1 : 1) * (height / 2 + off);
			if (Math.hypot(posX - this.seaX, posZ - this.seaZ) < this.seaRadius * 1.3) {
				continue;
			}
			if (index % 3 == 0) {
				// A drift of snow, half sunk in the ground
				double radius = 6 + crystals.nextDouble() * 14;
				Sphere drift = new Sphere(radius, 10);
				drift.setMaterial(snow);
				drift.setScaleY(0.45);
				drift.setTranslateX(posX);
				drift.setTranslateY(5 - radius * 0.2);
				drift.setTranslateZ(posZ);
				host.arena.getChildren().add(drift);
			} else {
				IceArena.crystal(shards, posX, 5, posZ, 2 + crystals.nextDouble() * 3, 8 + crystals.nextDouble() * 14, 5, crystals);
			}
		}
		this.addIce(shards, shardMaterial);
	}

	/** Clusters of crystals far away, out of the sea */
	private void farCrystals(Random crystals) {
		// Clusters of crystals far away, out of the sea
		TriangleMesh far = new TriangleMesh();
		far.getTexCoords().addAll(0, 0);
		for (int index = 0; index < 12; index++) {
			double angle = index * Math.PI * 2 / 12 + crystals.nextDouble() * 0.3;
			double distance = 900 + crystals.nextDouble() * 800;
			double cxValue = Math.cos(angle) * distance;
			double czValue = Math.sin(angle) * distance;
			if (Math.hypot(cxValue - this.seaX, czValue - this.seaZ) < this.seaRadius * 1.4) {
				continue;
			}
			int count = 3 + crystals.nextInt(3);
			for (int deepIndex = 0; deepIndex < count; deepIndex++) {
				double height = 90 + crystals.nextDouble() * 240;
				IceArena.crystal(far, cxValue + (crystals.nextDouble() - 0.5) * 120, 8, czValue + (crystals.nextDouble() - 0.5) * 120, 18 + crystals.nextDouble() * 28, height, 6 + crystals.nextInt(2), crystals);
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
	/**
	 * A glacial sea: open dark water with a wavy shore, floes of cracked ice drifting
	 * over it, icebergs standing in it and snow along the shore
	 */
	private void buildSea(double centreX, double centreZ, double radius, Random random) {
		buildWater(centreX, centreZ, radius);
		buildFloes(centreX, centreZ, radius, random);
		buildBergs(centreX, centreZ, radius, random);
		buildSnow(centreX, centreZ, radius, random);
	}

	/** The flat dark water, a fan from the middle to the wavy shore */
	private void buildWater(double centreX, double centreZ, double radius) {
		TriangleMesh water = new TriangleMesh();
		water.getTexCoords().addAll(0, 0);
		water.getPoints().addAll((float) centreX, (float) SEA_SURFACE, (float) centreZ);
		for (int index = 0; index < SEA_SIDES; index++) {
			double angle = index * Math.PI * 2 / SEA_SIDES;
			double row = shore(angle, radius);
			water.getPoints().addAll((float) (centreX + Math.cos(angle) * row), (float) SEA_SURFACE, (float) (centreZ + Math.sin(angle) * row));
		}
		for (int index2 = 0; index2 < SEA_SIDES; index2++) {
			int first = 1 + index2;
			int second = 1 + (index2 + 1) % SEA_SIDES;
			water.getFaces().addAll(0, 0, first, 0, second, 0);
		}
		PhongMaterial sea = ArenaMaterials.material(Color.web("#14365e"));
		sea.setSpecularColor(Color.WHITE);
		sea.setSpecularPower(90);
		this.addIce(water, sea);
	}

	/** The floes: rough discs of cracked ice, in several sizes, kept off the shore */
	private void buildFloes(double centreX, double centreZ, double radius, Random random) {
		TriangleMesh floes = new TriangleMesh();
		for (int index3 = 0; index3 < 150; index3++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = Math.sqrt(random.nextDouble()) * shore(angle, radius) * 0.85;
			double fxValue = centreX + Math.cos(angle) * distance;
			double fzValue = centreZ + Math.sin(angle) * distance;
			double size = 14 + random.nextDouble() * 32;
			int floeSides = 7 + random.nextInt(3);
			int offset = floes.getPoints().size() / 3;
			floes.getPoints().addAll((float) fxValue, (float) (SEA_SURFACE - 0.2), (float) fzValue);
			floes.getTexCoords().addAll(0.5f, 0.5f);
			for (int deepIndex = 0; deepIndex < floeSides; deepIndex++) {
				double first2 = deepIndex * Math.PI * 2 / floeSides + random.nextDouble() * 0.3;
				double row2 = size * (0.7 + 0.35 * random.nextDouble());
				double cos = Math.cos(first2);
				double sin = Math.sin(first2);
				floes.getPoints().addAll((float) (fxValue + cos * row2), (float) (SEA_SURFACE - 0.2 - random.nextDouble() * 0.3), (float) (fzValue + sin * row2));
				floes.getTexCoords().addAll((float) (0.5 + cos * 0.5), (float) (0.5 + sin * 0.5));
			}
			for (int deepIndex2 = 0; deepIndex2 < floeSides; deepIndex2++) {
				int first3 = offset + 1 + deepIndex2;
				int second2 = offset + 1 + (deepIndex2 + 1) % floeSides;
				floes.getFaces().addAll(offset, offset, first3, first3, second2, second2);
			}
		}
		PhongMaterial floeIce = ArenaMaterials.textured(IceTexture.block(0xc4ecfa, 11), Color.WHITE);
		floeIce.setSpecularPower(60);
		this.addIce(floes, floeIce);
	}

	/** Icebergs standing in the water */
	private void buildBergs(double centreX, double centreZ, double radius, Random random) {
		TriangleMesh bergs = new TriangleMesh();
		bergs.getTexCoords().addAll(0, 0);
		for (int index4 = 0; index4 < 7; index4++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = Math.sqrt(random.nextDouble()) * shore(angle, radius) * 0.6;
			IceArena.crystal(bergs, centreX + Math.cos(angle) * distance, SEA_SURFACE + 0.4, centreZ + Math.sin(angle) * distance, 40 + random.nextDouble() * 40, 30 + random.nextDouble() * 50, 8, random);
		}
		PhongMaterial berg = ArenaMaterials.material(Color.web("#e8f8ff"));
		berg.setSpecularColor(Color.WHITE);
		berg.setSpecularPower(100);
		this.addIce(bergs, berg);
	}

	/** Snow along the shore: a row of flattened spheres */
	private void buildSnow(double centreX, double centreZ, double radius, Random random) {
		PhongMaterial snow = ArenaMaterials.material(Color.web("#eef6fc"));
		snow.setSpecularPower(20);
		for (int index5 = 0; index5 < SEA_SIDES; index5 += 3) {
			double angle = index5 * Math.PI * 2 / SEA_SIDES;
			double row3 = shore(angle, radius) * 1.03;
			Sphere rim = new Sphere(9 + random.nextDouble() * 10, 8);
			rim.setMaterial(snow);
			rim.setScaleY(0.5);
			rim.setTranslateX(centreX + Math.cos(angle) * row3);
			rim.setTranslateY(SEA_SURFACE - 0.4);
			rim.setTranslateZ(centreZ + Math.sin(angle) * row3);
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
	static void crystal(TriangleMesh mesh, double posX, double baseY, double posZ, double radius, double height, int sides, Random random) {
		int offset = mesh.getPoints().size() / 3;
		double tipX = posX + (random.nextDouble() - 0.5) * radius;
		double tipZ = posZ + (random.nextDouble() - 0.5) * radius;
		double turn = random.nextDouble() * Math.PI * 2;
		double middleX = posX + (tipX - posX) * 0.5;
		double middleZ = posZ + (tipZ - posZ) * 0.5;
		for (int index = 0; index < sides; index++) {
			double angle = turn + index * Math.PI * 2 / sides;
			double row = radius * (0.75 + 0.35 * random.nextDouble());
			mesh.getPoints().addAll((float) (posX + Math.cos(angle) * row), (float) baseY, (float) (posZ + Math.sin(angle) * row));
		}
		for (int index2 = 0; index2 < sides; index2++) {
			double angle = turn + (index2 + 0.5) * Math.PI * 2 / sides;
			double row2 = radius * (0.5 + 0.3 * random.nextDouble());
			mesh.getPoints().addAll((float) (middleX + Math.cos(angle) * row2), (float) (baseY - height * 0.5), (float) (middleZ + Math.sin(angle) * row2));
		}
		mesh.getPoints().addAll((float) tipX, (float) (baseY - height), (float) tipZ);
		int tip = offset + 2 * sides;
		for (int index3 = 0; index3 < sides; index3++) {
			int base = offset + index3;
			int nextBase = offset + (index3 + 1) % sides;
			int middle = offset + sides + index3;
			int nextMiddle = offset + sides + (index3 + 1) % sides;
			mesh.getFaces().addAll(base, 0, nextBase, 0, nextMiddle, 0, base, 0, nextMiddle, 0, middle, 0);
			mesh.getFaces().addAll(middle, 0, nextMiddle, 0, tip, 0);
		}
	}

}
