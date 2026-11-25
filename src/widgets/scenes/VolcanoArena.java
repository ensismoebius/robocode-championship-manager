package widgets.scenes;

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
 * The volcano arena: the field of gray basalt plates over the lake of lava, the moat
 * and the rivers of lava, the volcanoes around the field, the cliffs and the caves of
 * the highlands around the lake (see {@link VolcanoHighlands}), and a sky of ash lit
 * red by the lava. The lava flows
 * themselves are shared with the jungle rivers and are drawn by the
 * {@link BattleArena3D}.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class VolcanoArena {

	private final BattleArena3D host;
	/** The shore, the cliffs, the caves and the mountains around the lake */
	private final VolcanoHighlands highlands;
	/** The embers in the air, in layers that rise each at its own pace and fade in and out, so they never jump back in sight */
	private final List<MeshView> emberLayers = new ArrayList<>();
	private final List<PhongMaterial> emberLights = new ArrayList<>();
	private double[] emberPhases = new double[0];
	/** How many layers of embers, how many in each, how high a layer rises before it fades away and in how many seconds */
	private static final int EMBER_LAYERS = 10;
	private static final int EMBERS = 70;
	private static final double EMBER_RISE = 260;
	private static final double EMBER_SECONDS = 40;

	/** Creates the volcano of the arena, drawn in the host scene. */
	VolcanoArena(BattleArena3D host) {
		this.host = host;
		this.highlands = new VolcanoHighlands(host);
	}

	/** The highlands around the lake, with their caves. */
	VolcanoHighlands highlands() {
		return this.highlands;
	}

	private static final Image LAVA_TEXTURE = LavaTexture.create(0);

	/** Far away the lava has cooled, only its veins glow */
	private static final Image CRUST_TEXTURE = LavaTexture.create(0.45);

	/** Under the field of the battle the lava glows little, it shows in the cracks of the gray plates: the robots are what should stand out */
	private static final Image FIELD_TEXTURE = LavaTexture.floor(0.6, 0.5, 0x120403);
	private static final String[] BASALT_TONES = { "#4a4a52", "#41414a", "#55555d" };


	/**
	 * The sky of ash, basalt plates (Voronoi cells) with glowing lava in the
	 * cracks, a moat of lava around it and volcanoes on the horizon
	 */
	void build(double width, double height) {
		Random rocks = new Random(17);
		host.arena.getChildren().add(ArenaMaterials.skyDome(VolcanoTexture.SKY));
		this.volcanoGround(width, height);
		this.basaltPlates(width, height, rocks);
		this.moat(width, height);
		this.rivers(width, height, rocks);
		this.lavaSea();
		List<VolcanoHighlands.Peak> peaks = this.volcanoes(rocks);
		this.embers(width, height, rocks);
		// Its own randomness, so the volcanoes and the embers stay where they were
		this.highlands.build(width, height, peaks, new Random(29));
	}

	/**
	 * Embers in the air around the field, specks of fire that float up from the lava: small and sharp near, smaller far away, none
	 * over the field. In layers, each of its own color, that rise slowly and fade in and out by turns.
	 */
	private void embers(double width, double height, Random rocks) {
		this.emberLayers.clear();
		this.emberLights.clear();
		this.emberPhases = new double[EMBER_LAYERS];
		double clear = Math.hypot(width, height) / 2 + 120;
		String[] colors = { "#ff7a24", "#ff9a3a", "#ffd27a", "#ff5a1a" };
		for (int layer = 0; layer < EMBER_LAYERS; layer++) {
			LowPoly specks = new LowPoly();
			for (int ember = 0; ember < EMBERS; ember++) {
				double angle = rocks.nextDouble() * Math.PI * 2;
				// Over the lake, short of the cliffs, where they would be hidden
				double distance = clear + Math.pow(rocks.nextDouble(), 1.5) * (VolcanoHighlands.SHORE - 100 - clear);
				double size = 1.6 + distance * 0.0011;
				specks.box(Math.cos(angle) * distance, -(20 + rocks.nextDouble() * 650), Math.sin(angle) * distance, size, size, size);
			}
			PhongMaterial light = new PhongMaterial(Color.color(0, 0, 0, 0));
			light.setSpecularColor(Color.BLACK);
			light.setSelfIlluminationMap(ArenaHelpers.solidMap(Color.web(colors[layer % colors.length])));
			MeshView view = specks.view(light);
			this.emberLayers.add(view);
			this.emberLights.add(light);
			this.emberPhases[layer] = (double) layer / EMBER_LAYERS;
			host.arena.getChildren().add(view);
		}
		this.update(0);
	}

	/** The embers rise, each layer fading in as it starts and out as it ends, and starting again from below, unseen. */
	void update(double deltaTime) {
		this.highlands.update(deltaTime);
		for (int layer = 0; layer < this.emberLayers.size(); layer++) {
			// The time can step back, when the replay goes back: the phase stays between 0 and 1
			double phase = ((this.emberPhases[layer] + deltaTime / EMBER_SECONDS) % 1 + 1) % 1;
			this.emberPhases[layer] = phase;
			this.emberLayers.get(layer).setTranslateY(-phase * EMBER_RISE);
			this.emberLights.get(layer).setDiffuseColor(Color.color(0, 0, 0, Math.max(0, Math.min(1, Math.sin(Math.PI * phase)))));
		}
	}

	/** A plateau of rock in a sea of lava, with the lava under the field */
	void volcanoGround(double width, double height) {
		// A plateau of rock in a sea of lava
		double plateau = 380;
		Box ground = new Box(width + 2 * plateau, 2, height + 2 * plateau);
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
		Box floor = new Box(width + 40, 4, height + 40);
		floor.setMaterial(basalt);
		floor.setTranslateY(2);
		host.arena.getChildren().addAll(ground, floor);

		// Lava under the whole field, slowly moving, it shows in the cracks. It glows little, much less
		// than the moat and the sea, so the robots stand out against the field
		PhongMaterial fieldLava = new PhongMaterial(Color.WHITE);
		fieldLava.setDiffuseMap(FIELD_TEXTURE);
		fieldLava.setSelfIlluminationMap(FIELD_TEXTURE);
		fieldLava.setSpecularColor(Color.BLACK);
		host.lavaFlows.flow(new double[][] { { -width / 2, -0.3, -height / 2 }, { width / 2, -0.3, -height / 2 }, { width / 2, -0.3, height / 2 }, { -width / 2, -0.3, height / 2 } }, 0.015, 0.025, fieldLava, LAVA_TILE);
	}

	/** Basalt plates (Voronoi cells) with glowing lava in the cracks, and the border of the field */
	void basaltPlates(double width, double height, Random rocks) {
		TriangleMesh[] plates = basaltMeshes(basaltCells(width, height, rocks), rocks);
		for (int index3 = 0; index3 < plates.length; index3++) {
			PhongMaterial basaltTone = new PhongMaterial(Color.web(BASALT_TONES[index3]));
			basaltTone.setSpecularColor(Color.web("#6a6a72"));
			basaltTone.setSpecularPower(10);
			MeshView view = new MeshView(plates[index3]);
			view.setMaterial(basaltTone);
			view.setCullFace(CullFace.NONE);
			host.arena.getChildren().add(view);
		}
		fieldBorder(width, height);
	}

	/**
	 * Voronoi cells of jittered seeds over the field, each shrunk a bit so the lava
	 * glows between them, in cracks of different widths
	 */
	private static List<double[][]> basaltCells(double width, double height, Random rocks) {
		double cell = 68;
		List<double[]> seeds = new ArrayList<double[]>();
		for (double posX = cell / 2; posX < width; posX += cell) {
			for (double posZ = cell / 2; posZ < height; posZ += cell) {
				seeds.add(new double[] { posX - width / 2 + (rocks.nextDouble() - 0.5) * cell * 0.9, posZ - height / 2 + (rocks.nextDouble() - 0.5) * cell * 0.9 });
			}
		}
		double[] gaps = new double[seeds.size()];
		for (int index = 0; index < gaps.length; index++) {
			gaps[index] = 0.9 + rocks.nextDouble() * rocks.nextDouble() * 2.6;
		}
		return Voronoi.cells(seeds.toArray(new double[0][]), gaps, -width / 2, -height / 2, width / 2, height / 2);
	}

	/** One mesh for each tone of basalt, each cell going to one of them as a fan of triangles */
	private static TriangleMesh[] basaltMeshes(List<double[][]> cells, Random rocks) {
		TriangleMesh[] plates = new TriangleMesh[BASALT_TONES.length];
		for (int index2 = 0; index2 < plates.length; index2++) {
			plates[index2] = new TriangleMesh();
			plates[index2].getTexCoords().addAll(0, 0);
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
			for (int deepIndex = 1; deepIndex + 1 < polygon.length; deepIndex++) {
				mesh.getFaces().addAll(first, 0, first + deepIndex + 1, 0, first + deepIndex, 0);
			}
		}
		return plates;
	}

	/** The border of the field: glowing crack lines along its four edges */
	private void fieldBorder(double width, double height) {
		PhongMaterial crack = new PhongMaterial(Color.web("#a8380e"));
		crack.setSpecularColor(Color.web("#7a4a30"));
		host.glowing.getChildren().addAll(box(width, 0.5, 2, 0, -0.3, -height / 2, crack), box(width, 0.5, 2, 0, -0.3, height / 2, crack), box(2, 0.5, height, -width / 2, -0.3, 0, crack),
				box(2, 0.5, height, width / 2, -0.3, 0, crack));
	}

	/** A moat of lava around the field, flowing clockwise seen from above */
	void moat(double width, double height) {
		// A moat of lava, a bit lower than the floor
		double edge = 20;
		double moat = 70;
		// It flows around the field, clockwise seen from above
		double outerX = width / 2 + edge + moat;
		double outerZ = height / 2 + edge + moat;
		double middleX = width / 2 + edge + moat / 2;
		double middleZ = height / 2 + edge + moat / 2;
		host.lavaFlows.flowStrip(-outerX, -middleZ, outerX, -middleZ, moat, 3, 0.18);
		host.lavaFlows.flowStrip(middleX, -outerZ, middleX, outerZ, moat, 3, 0.18);
		host.lavaFlows.flowStrip(outerX, middleZ, -outerX, middleZ, moat, 3, 0.18);
		host.lavaFlows.flowStrip(-middleX, outerZ, -middleX, -outerZ, moat, 3, 0.18);
	}

	/** Rivers of lava from the moat to the sea, with a bend */
	void rivers(double width, double height, Random rocks) {
		double plateau = 380;
		double outerX = width / 2 + 20 + 70;
		double outerZ = height / 2 + 20 + 70;
		// Rivers from the moat to the sea, with a bend
		double[][] rivers = { { -width / 4, -1, 0 }, { width / 3, 1, 0 }, { height / 5, 1, 1 }, { -height / 4, -1, 1 } };
		for (double[] river : rivers) {
			boolean alongX = river[2] == 1;
			double side = river[1];
			double start = (alongX ? outerX : outerZ) - 4;
			double end = (alongX ? width : height) / 2 + plateau + 20;
			double middle = (start + end) / 2;
			double bend = river[0] + (rocks.nextDouble() - 0.5) * 140;
			double[][] points = { { river[0], start }, { bend, middle }, { bend + (rocks.nextDouble() - 0.5) * 120, end } };
			for (int deepIndex = 0; deepIndex + 1 < points.length; deepIndex++) {
				double[] first = points[deepIndex];
				double[] second = points[deepIndex + 1];
				if (alongX) {
					host.lavaFlows.flowStrip(side * first[1], first[0], side * second[1], second[0], 34, 4.6, 0.35);
				} else {
					host.lavaFlows.flowStrip(first[0], side * first[1], second[0], side * second[1], 34, 4.6, 0.35);
				}
			}
		}
	}

	/** The lake of lava around the plateau, drifting, with a cooler crust; it ends under the shore of the highlands */
	void lavaSea() {
		// Drifting, with a cooler crust: bright, it would steal the show and shimmer far away
		PhongMaterial crust = new PhongMaterial(Color.WHITE);
		crust.setDiffuseMap(CRUST_TEXTURE);
		crust.setSelfIlluminationMap(CRUST_TEXTURE);
		crust.setSpecularColor(Color.BLACK);
		double sea = VolcanoHighlands.LAKE;
		host.lavaFlows.flow(new double[][] { { -sea, 7, -sea }, { sea, 7, -sea }, { sea, 7, sea }, { -sea, 7, sea } }, 0.012, 0.007, crust, 420);
	}

	/**
	 * Volcanoes in the lake, short of its shore: stacked tiers make a cone, glowing on the top. Far enough from the field that the
	 * sweep of the tour around it passes beside them, not through them.
	 *
	 * @return where each one stands and how wide its foot is
	 */
	List<VolcanoHighlands.Peak> volcanoes(Random rocks) {
		List<VolcanoHighlands.Peak> peaks = new ArrayList<>();
		PhongMaterial rock = new PhongMaterial(Color.web("#1c110e"));
		rock.setSpecularColor(Color.web("#2c1a14"));
		for (int index = 0; index < 9; index++) {
			double angle = index * Math.PI * 2 / 9 + rocks.nextDouble() * 0.4;
			double distance = 1700 + rocks.nextDouble() * 350;
			double height = 220 + rocks.nextDouble() * 420;
			double radius = 110 + rocks.nextDouble() * 130;
			double posX = Math.cos(angle) * distance;
			double posZ = Math.sin(angle) * distance;
			cone(posX, posZ, radius, height, rock);
			lavaFalls(posX, posZ, radius, height, rocks);
			Cylinder crater = new Cylinder(radius * 0.3, 4, 7);
			crater.setMaterial(host.lava);
			crater.setTranslateX(posX);
			crater.setTranslateZ(posZ);
			crater.setTranslateY(5 - height - 1);
			host.glowing.getChildren().add(crater);
			peaks.add(new VolcanoHighlands.Peak(posX, posZ, radius));
		}
		return peaks;
	}

	/** The cone of a volcano: eight tiers, each one a little narrower than the one below */
	private void cone(double posX, double posZ, double radius, double height, PhongMaterial rock) {
		int tiers = 8;
		for (int tier = 0; tier < tiers; tier++) {
			Cylinder part = new Cylinder(radius * (1 - tier * 0.1), height / tiers, 9);
			part.setMaterial(rock);
			part.setTranslateX(posX);
			part.setTranslateZ(posZ);
			part.setTranslateY(5 - height / tiers * (tier + 0.5));
			host.arena.getChildren().add(part);
		}
	}

	/** Lava falling down three slopes of a volcano, a bit off them so the tiers do not cover them */
	private void lavaFalls(double posX, double posZ, double radius, double height, Random rocks) {
		for (int fall = 0; fall < 3; fall++) {
			double side = (fall + rocks.nextDouble() * 0.6) * Math.PI * 2 / 3;
			double deltaX = Math.cos(side);
			double deltaZ = Math.sin(side);
			double top = radius * 0.3 * 1.12;
			double bottom = radius * 1.12;
			double topWidth = radius * 0.15;
			double bottomWidth = radius * 0.42;
			host.lavaFlows.flow(new double[][] { //
					{ posX + deltaX * top - deltaZ * topWidth, 5 - height, posZ + deltaZ * top + deltaX * topWidth }, //
					{ posX + deltaX * top + deltaZ * topWidth, 5 - height, posZ + deltaZ * top - deltaX * topWidth }, //
					{ posX + deltaX * bottom + deltaZ * bottomWidth, 5, posZ + deltaZ * bottom - deltaX * bottomWidth }, //
					{ posX + deltaX * bottom - deltaZ * bottomWidth, 5, posZ + deltaZ * bottom + deltaX * bottomWidth } }, 0, -0.4);
		}
	}
}
