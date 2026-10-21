package widgets.scenes;

import static widgets.scenes.BattleArena3D.LAVA_FULL;
import static widgets.scenes.BattleArena3D.LAVA_TILE;
import static widgets.scenes.ArenaMaterials.box;
import static widgets.scenes.ArenaMaterials.material;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The volcano arena: the field of gray basalt plates over the lava sea, the moat
 * and the rivers of lava, and the volcanoes around the field. The lava flows
 * themselves are shared with the jungle rivers and are drawn by the
 * {@link BattleArena3D}.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class VolcanoArena {

	private final BattleArena3D host;

	VolcanoArena(BattleArena3D host) {
		this.host = host;
	}

	private static final Image LAVA_TEXTURE = LavaTexture.create(0);

	/** Far away the lava has cooled, only its veins glow */
	private static final Image CRUST_TEXTURE = LavaTexture.create(0.45);

	/** Under the field of the battle the lava glows little, it shows in the cracks of the gray plates: the robots are what should stand out */
	private static final Image FIELD_TEXTURE = LavaTexture.floor(0.6, 0.5, 0x120403);


	/**
	 * Basalt plates (Voronoi cells) with glowing lava in the cracks, a moat of
	 * lava around it and
	 * volcanoes on the horizon
	 */
	void build(double w, double h) {
		Random rocks = new Random(17);
		this.volcanoGround(w, h);
		this.basaltPlates(w, h, rocks);
		this.moat(w, h);
		this.rivers(w, h, rocks);
		this.lavaSea();
		this.volcanoes(rocks);
	}

	/** A plateau of rock in a sea of lava, with the lava under the field */
	void volcanoGround(double w, double h) {
		// A plateau of rock in a sea of lava
		double plateau = 380;
		Box ground = new Box(w + 2 * plateau, 2, h + 2 * plateau);
		ground.setMaterial(material(Color.web("#100706")));
		ground.setTranslateY(6);

		// Lava: its texture glows by itself (self illumination), with no light
		host.lava = new PhongMaterial(BattleArena3D.LAVA_FULL);
		host.lava.setDiffuseMap(LAVA_TEXTURE);
		host.lava.setSelfIlluminationMap(LAVA_TEXTURE);
		host.lava.setSpecularColor(Color.BLACK);

		PhongMaterial basalt = new PhongMaterial(Color.web("#4a4a52"));
		basalt.setSpecularColor(Color.web("#6a6a72"));
		basalt.setSpecularPower(10);
		Box floor = new Box(w + 40, 4, h + 40);
		floor.setMaterial(basalt);
		floor.setTranslateY(2);
		host.arena.getChildren().addAll(ground, floor);

		// Lava under the whole field, slowly moving, it shows in the cracks. It glows little, much less
		// than the moat and the sea, so the robots stand out against the field
		PhongMaterial fieldLava = new PhongMaterial(Color.WHITE);
		fieldLava.setDiffuseMap(FIELD_TEXTURE);
		fieldLava.setSelfIlluminationMap(FIELD_TEXTURE);
		fieldLava.setSpecularColor(Color.BLACK);
		host.lavaFlows.flow(new double[][] { { -w / 2, -0.3, -h / 2 }, { w / 2, -0.3, -h / 2 }, { w / 2, -0.3, h / 2 }, { -w / 2, -0.3, h / 2 } }, 0.015, 0.025, fieldLava, LAVA_TILE);
	}

	/** Basalt plates (Voronoi cells) with glowing lava in the cracks, and the border of the field */
	void basaltPlates(double w, double h, Random rocks) {
		// Basalt plates over it: Voronoi cells of jittered seeds, each shrunk a
		// bit so the lava glows between them, in cracks of different widths
		double cell = 68;
		List<double[]> seeds = new ArrayList<double[]>();
		for (double x = cell / 2; x < w; x += cell) {
			for (double z = cell / 2; z < h; z += cell) {
				seeds.add(new double[] { x - w / 2 + (rocks.nextDouble() - 0.5) * cell * 0.9, z - h / 2 + (rocks.nextDouble() - 0.5) * cell * 0.9 });
			}
		}
		double[] gaps = new double[seeds.size()];
		for (int i = 0; i < gaps.length; i++) {
			gaps[i] = 0.9 + rocks.nextDouble() * rocks.nextDouble() * 2.6;
		}
		List<double[][]> cells = Voronoi.cells(seeds.toArray(new double[0][]), gaps, -w / 2, -h / 2, w / 2, h / 2);

		// One mesh for each tone of basalt
		String[] tones = { "#4a4a52", "#41414a", "#55555d" };
		TriangleMesh[] plates = new TriangleMesh[tones.length];
		for (int i = 0; i < plates.length; i++) {
			plates[i] = new TriangleMesh();
			plates[i].getTexCoords().addAll(0, 0);
		}
		for (double[][] polygon : cells) {
			if (polygon.length < 3) {
				continue;
			}
			TriangleMesh mesh = plates[rocks.nextInt(plates.length)];
			int first = mesh.getPoints().size() / 3;
			for (double[] point : polygon) {
				mesh.getPoints().addAll((float) point[0], -0.6f, (float) point[1]);
			}
			// A fan, so the normal points up (to -Y)
			for (int k = 1; k + 1 < polygon.length; k++) {
				mesh.getFaces().addAll(first, 0, first + k + 1, 0, first + k, 0);
			}
		}
		for (int i = 0; i < plates.length; i++) {
			PhongMaterial basaltTone = new PhongMaterial(Color.web(tones[i]));
			basaltTone.setSpecularColor(Color.web("#6a6a72"));
			basaltTone.setSpecularPower(10);
			MeshView view = new MeshView(plates[i]);
			view.setMaterial(basaltTone);
			view.setCullFace(CullFace.NONE);
			host.arena.getChildren().add(view);
		}

		PhongMaterial crack = new PhongMaterial(Color.web("#a8380e"));
		crack.setSpecularColor(Color.web("#7a4a30"));

		// The field border
		host.glowing.getChildren().addAll(box(w, 0.5, 2, 0, -0.3, -h / 2, crack), box(w, 0.5, 2, 0, -0.3, h / 2, crack), box(2, 0.5, h, -w / 2, -0.3, 0, crack),
				box(2, 0.5, h, w / 2, -0.3, 0, crack));
	}

	/** A moat of lava around the field, flowing clockwise seen from above */
	void moat(double w, double h) {
		// A moat of lava, a bit lower than the floor
		double edge = 20;
		double moat = 70;
		// It flows around the field, clockwise seen from above
		double outerX = w / 2 + edge + moat;
		double outerZ = h / 2 + edge + moat;
		double middleX = w / 2 + edge + moat / 2;
		double middleZ = h / 2 + edge + moat / 2;
		host.lavaFlows.flowStrip(-outerX, -middleZ, outerX, -middleZ, moat, 3, 0.18);
		host.lavaFlows.flowStrip(middleX, -outerZ, middleX, outerZ, moat, 3, 0.18);
		host.lavaFlows.flowStrip(outerX, middleZ, -outerX, middleZ, moat, 3, 0.18);
		host.lavaFlows.flowStrip(-middleX, outerZ, -middleX, -outerZ, moat, 3, 0.18);
	}

	/** Rivers of lava from the moat to the sea, with a bend */
	void rivers(double w, double h, Random rocks) {
		double plateau = 380;
		double outerX = w / 2 + 20 + 70;
		double outerZ = h / 2 + 20 + 70;
		// Rivers from the moat to the sea, with a bend
		double[][] rivers = { { -w / 4, -1, 0 }, { w / 3, 1, 0 }, { h / 5, 1, 1 }, { -h / 4, -1, 1 } };
		for (double[] river : rivers) {
			boolean alongX = river[2] == 1;
			double side = river[1];
			double start = (alongX ? outerX : outerZ) - 4;
			double end = (alongX ? w : h) / 2 + plateau + 20;
			double middle = (start + end) / 2;
			double bend = river[0] + (rocks.nextDouble() - 0.5) * 140;
			double[][] points = { { river[0], start }, { bend, middle }, { bend + (rocks.nextDouble() - 0.5) * 120, end } };
			for (int k = 0; k + 1 < points.length; k++) {
				double[] a = points[k];
				double[] b = points[k + 1];
				if (alongX) {
					host.lavaFlows.flowStrip(side * a[1], a[0], side * b[1], b[0], 34, 4.6, 0.35);
				} else {
					host.lavaFlows.flowStrip(a[0], side * a[1], b[0], side * b[1], 34, 4.6, 0.35);
				}
			}
		}
	}

	/** The sea of lava that surrounds everything, drifting, with a cooler crust */
	void lavaSea() {
		// The sea of lava, drifting, with a cooler crust: bright, it would steal the
		// show and shimmer far away
		PhongMaterial crust = new PhongMaterial(Color.WHITE);
		crust.setDiffuseMap(CRUST_TEXTURE);
		crust.setSelfIlluminationMap(CRUST_TEXTURE);
		crust.setSpecularColor(Color.BLACK);
		double sea = 5000;
		host.lavaFlows.flow(new double[][] { { -sea, 7, -sea }, { sea, 7, -sea }, { sea, 7, sea }, { -sea, 7, sea } }, 0.012, 0.007, crust, 420);
	}

	/** Volcanoes on the horizon: stacked tiers make a cone, glowing on the top */
	void volcanoes(Random rocks) {
		// Volcanoes on the horizon: stacked tiers make a cone, glowing on the top
		PhongMaterial rock = new PhongMaterial(Color.web("#1c110e"));
		rock.setSpecularColor(Color.web("#2c1a14"));
		for (int i = 0; i < 9; i++) {
			double angle = i * Math.PI * 2 / 9 + rocks.nextDouble() * 0.4;
			double distance = 1400 + rocks.nextDouble() * 700;
			double height = 220 + rocks.nextDouble() * 420;
			double radius = 110 + rocks.nextDouble() * 130;
			double x = Math.cos(angle) * distance;
			double z = Math.sin(angle) * distance;
			int tiers = 8;
			for (int tier = 0; tier < tiers; tier++) {
				Cylinder part = new Cylinder(radius * (1 - tier * 0.1), height / tiers, 9);
				part.setMaterial(rock);
				part.setTranslateX(x);
				part.setTranslateZ(z);
				part.setTranslateY(5 - height / tiers * (tier + 0.5));
				host.arena.getChildren().add(part);
			}
			// Lava falls down the slopes, a bit off them so the tiers do not cover them
			for (int fall = 0; fall < 3; fall++) {
				double side = (fall + rocks.nextDouble() * 0.6) * Math.PI * 2 / 3;
				double dx = Math.cos(side);
				double dz = Math.sin(side);
				double top = radius * 0.3 * 1.12;
				double bottom = radius * 1.12;
				double topWidth = radius * 0.15;
				double bottomWidth = radius * 0.42;
				host.lavaFlows.flow(new double[][] { //
						{ x + dx * top - dz * topWidth, 5 - height, z + dz * top + dx * topWidth }, //
						{ x + dx * top + dz * topWidth, 5 - height, z + dz * top - dx * topWidth }, //
						{ x + dx * bottom + dz * bottomWidth, 5, z + dz * bottom - dx * bottomWidth }, //
						{ x + dx * bottom - dz * bottomWidth, 5, z + dz * bottom + dx * bottomWidth } }, 0, -0.4);
			}
			Cylinder crater = new Cylinder(radius * 0.3, 4, 7);
			crater.setMaterial(host.lava);
			crater.setTranslateX(x);
			crater.setTranslateZ(z);
			crater.setTranslateY(5 - height - 1);
			host.glowing.getChildren().add(crater);
		}
	}
}
