package widgets.scenes;

import static widgets.scenes.ArenaMaterials.box;
import static widgets.scenes.ArenaMaterials.material;
import static widgets.scenes.ArenaMaterials.skyDome;
import static widgets.scenes.ArenaMaterials.textured;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The jungle arena: the mossy ground between the rivers, the waterfalls, the
 * rocks, the ferns and the huge trees, the walls of mountains around the field
 * and the shafts of light through the canopy.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class JungleArena {

	private final List<PhongMaterial> beamMaterials = new ArrayList<PhongMaterial>();
	private final List<Double> beamPhases = new ArrayList<Double>();
	private final List<TriangleMesh> beamMeshes = new ArrayList<TriangleMesh>();
	/** x, z and width of each */
	private final List<double[]> beamSpots = new ArrayList<double[]>();
	private double beamYaw;
	private double beamClock;
	/** Lights of the neon arena that breathe, with their colors and phases */
	private static final double BEAM_ALPHA = 0.07;

	private final BattleArena3D host;

	/** Creates the jungle of the arena, drawn in the host scene. */
	JungleArena(BattleArena3D host) {
		this.host = host;
	}

	/**
	 * Crepuscular rays: pale shafts of light slanting through the canopy. Each is
	 * one soft strip that turns around its own axis to face the camera, so they
	 * have no edges and cost little. They do not touch the depth buffer (they are
	 * only a faint haze), and breathe slowly.
	 */
	void buildSunbeams(double width, double height, Random random) {
		host.sunbeams.setDepthTest(DepthTest.DISABLE);
		host.sunbeams.setMouseTransparent(true);
		for (int index = 0; index < 9; index++) {
			TriangleMesh mesh = new TriangleMesh();
			mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
			mesh.getPoints().addAll(new float[12]);
			mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
			PhongMaterial material = new PhongMaterial(Color.color(1, 1, 1, BEAM_ALPHA));
			material.setDiffuseMap(JungleTexture.BEAM);
			MeshView view = new MeshView(mesh);
			view.setMaterial(material);
			view.setCullFace(CullFace.NONE);
			host.sunbeams.getChildren().add(view);
			this.beamMeshes.add(mesh);
			this.beamMaterials.add(material);
			this.beamSpots.add(new double[] { (random.nextDouble() - 0.5) * (width + 1400), (random.nextDouble() - 0.5) * (height + 1400), 110 + random.nextDouble() * 150 });
			this.beamPhases.add(random.nextDouble() * Math.PI * 2);
		}
		this.beamYaw = Double.NaN;
		host.fireGlow.getScope().add(host.sunbeams);
	}

	/**
	 * The shafts of light slowly get stronger and weaker, out of step, and turn to
	 * the camera
	 */
	void updateSunbeams(double deltaTime) {
		if (this.beamMaterials.isEmpty()) {
			return;
		}
		this.beamClock += deltaTime;
		// A strip that fills the screen is slow to draw, and a haze you are inside of
		// is not seen as a shaft anyway: they fade away as the camera comes close
		Point3D eye = host.camera.localToScene(Point3D.ZERO);
		double top = -1150;
		double bottom = 6;
		double height = bottom - top;
		double along = Math.max(0, Math.min(1, (eye.getY() - top) / height));
		for (int index = 0; index < this.beamMaterials.size(); index++) {
			double[] spot = this.beamSpots.get(index);
			double near = Math.hypot(eye.getX() - (spot[0] + height * 0.38 * along), eye.getZ() - (spot[1] + height * 0.22 * along));
			double away = Math.max(0, Math.min(1, (near - 250) / 700));
			away = away * away * (3 - 2 * away);
			host.sunbeams.getChildren().get(index).setVisible(away > 0.01);
			double breath = 0.5 + 0.5 * Math.sin(this.beamClock * 0.35 + this.beamPhases.get(index));
			this.beamMaterials.get(index).setDiffuseColor(Color.color(1, 1, 1, BEAM_ALPHA * (0.45 + 0.55 * breath) * away));
		}
		double yaw = host.cameraYaw.getAngle();
		if (yaw == this.beamYaw) {
			return;
		}
		this.beamYaw = yaw;
		// The light comes down slanted, from where the key light is
		double deltaX = height * 0.38;
		double deltaZ = height * 0.22;
		double right = Math.toRadians(yaw);
		for (int index2 = 0; index2 < this.beamMeshes.size(); index2++) {
			double[] spot = this.beamSpots.get(index2);
			double exValue = Math.cos(right) * spot[2] / 2;
			double ezValue = -Math.sin(right) * spot[2] / 2;
			this.beamMeshes.get(index2).getPoints().setAll((float) (spot[0] - exValue), (float) top, (float) (spot[1] - ezValue), (float) (spot[0] + exValue), (float) top, (float) (spot[1] + ezValue),
					(float) (spot[0] + exValue + deltaX), (float) bottom, (float) (spot[1] + ezValue + deltaZ), (float) (spot[0] - exValue + deltaX), (float) bottom, (float) (spot[1] - ezValue + deltaZ));
		}
	}

	/**
	 * Mossy ground, rivers on both sides, huge trees in the distance, ferns,
	 * rocks and waterfalls. Fireflies float in the air and shafts of light cross the canopy.
	 * The materials of the jungle, made once for all its parts.
	 */
	record JungleMaterials(PhongMaterial moss, PhongMaterial water, PhongMaterial waterfall, PhongMaterial bark, PhongMaterial leaves, PhongMaterial rock) {
	}

	/** Builds the jungle terrain over the given size of the arena. */
	void build(double width, double height) {
		Random wild = new Random(31);
		PhongMaterial moss = textured(JungleTexture.MOSS, Color.gray(0.05));
		// The relief of the water makes the sun glint on it
		PhongMaterial water = textured(JungleTexture.WATER, Color.web("#d8fff4"));
		water.setBumpMap(JungleTexture.WATER_RELIEF);
		water.setSpecularPower(70);
		PhongMaterial waterfall = textured(JungleTexture.WATERFALL, Color.web("#d8fff4"));
		waterfall.setBumpMap(JungleTexture.WATERFALL_RELIEF);
		waterfall.setSpecularPower(50);
		PhongMaterial bark = textured(JungleTexture.BARK, Color.gray(0.05));
		PhongMaterial leaves = textured(JungleTexture.LEAVES, Color.gray(0.1));
		PhongMaterial rock = textured(JungleTexture.BARK, Color.gray(0.1));
		JungleMaterials jungleMaterials = new JungleMaterials(moss, water, waterfall, bark, leaves, rock);

		// The sky: a green dome seen from inside
		host.arena.getChildren().add(skyDome(JungleTexture.SKY));
		// The dense forest all around, painted once
		host.arena.getChildren().add(ForestBackdrop.ring());
		this.buildSunbeams(width, height, wild);
		double riverZ = height / 2 + 160;
		double riverWidth = 96;
		this.jungleGround(width, height, jungleMaterials);
		this.jungleRivers(width, height, jungleMaterials, riverZ, riverWidth);
		this.jungleWaterfalls(width, height, wild, jungleMaterials, riverZ, riverWidth);
		this.jungleRocks(width, height, wild, jungleMaterials);
		this.jungleFerns(width, height, wild, riverZ, riverWidth);
		this.jungleTrees(width, height, wild, riverZ, riverWidth, jungleMaterials);
	}

	/** Moss everywhere, a faint grid of lighter moss and a sandy border */
	void jungleGround(double width, double height, JungleMaterials jungleMaterials) {
		PhongMaterial moss = jungleMaterials.moss();
		// Ground: moss everywhere, a bit darker far away (it is the same texture,
		// bigger tiles)
		double far = ArenaMaterials.GROUND_REACH;
		host.arena.getChildren().add(host.tiledQuad(new double[][] { { -far, 6, -far }, { far, 6, -far }, { far, 6, far }, { -far, 6, far } }, moss, 260));
		host.arena.getChildren().add(host.tiledQuad(new double[][] { { -width / 2 - 20, 0.2, -height / 2 - 20 }, { width / 2 + 20, 0.2, -height / 2 - 20 }, { width / 2 + 20, 0.2, height / 2 + 20 }, { -width / 2 - 20, 0.2, height / 2 + 20 } }, moss, 110));

		// A faint grid of lighter moss and a sandy border, so the field reads
		PhongMaterial line = material(Color.web("#4f8a3c"));
		for (double posX = 100; posX < width; posX += 100) {
			host.arena.getChildren().add(box(1.2, 0.4, height, posX - width / 2, -0.2, 0, line));
		}
		for (double posY = 100; posY < height; posY += 100) {
			host.arena.getChildren().add(box(width, 0.4, 1.2, 0, -0.2, posY - height / 2, line));
		}
		PhongMaterial sand = material(Color.web("#d6c58a"));
		host.arena.getChildren().addAll(box(width + 4, 0.6, 3, 0, -0.3, -height / 2, sand), box(width + 4, 0.6, 3, 0, -0.3, height / 2, sand), box(3, 0.6, height, -width / 2, -0.3, 0, sand),
				box(3, 0.6, height, width / 2, -0.3, 0, sand));
	}

	/** Rivers along the long sides, flowing in opposite directions */
	void jungleRivers(double width, double height, JungleMaterials jungleMaterials, double riverZ, double riverWidth) {
		// Rivers along the long sides, flowing in opposite directions
		PhongMaterial water = jungleMaterials.water();
		host.lavaFlows.river(-3000, 3000, -1, riverZ, 0, riverWidth, 0.16, water, 120);
		host.lavaFlows.river(3000, -3000, 1, riverZ, 2.3, riverWidth, 0.16, water, 120);
	}

	/** Waterfalls far at both ends, falling from a cliff to a pool */
	void jungleWaterfalls(double width, double height, Random wild, JungleMaterials jungleMaterials, double riverZ, double riverWidth) {
		PhongMaterial water = jungleMaterials.water();
		PhongMaterial waterfall = jungleMaterials.waterfall();
		// Waterfalls far at both ends, falling from a cliff to a pool
		for (int side = -1; side <= 1; side += 2) {
			double posX = side * (width / 2 + 1000);
			double posZ = (wild.nextDouble() - 0.5) * 300;
			// The cliff, its face to the field at x
			this.rockMountain(posX, posZ, side, wild);
			// The water in front of the face, not on it (two surfaces in the same
			// place flicker)
			double front = posX - side * 2.5;
			host.lavaFlows.flow(new double[][] { { front, 6 - 450, posZ - 70 }, { front, 6 - 450, posZ + 70 }, { front, 5, posZ + 70 }, { front, 5, posZ - 70 } }, 0, -0.9, waterfall, 60);
			// The pool at its foot, toward the field
			double poolEnd = posX - side * 240;
			double[][] pool = { { posX - side * 2.5, 3, posZ - 150 }, { poolEnd, 3, posZ - 150 }, { poolEnd, 3, posZ + 150 }, { posX - side * 2.5, 3, posZ + 150 } };
			host.lavaFlows.flow(pool, 0.025, 0.018, water, 120);
		}
	}

	/** Rocks covered with moss between the field and the rivers */
	void jungleRocks(double width, double height, Random wild, JungleMaterials jungleMaterials) {
		PhongMaterial moss = jungleMaterials.moss();
		PhongMaterial rock = jungleMaterials.rock();
		// Rocks covered with moss between the field and the rivers
		for (int index = 0; index < 16; index++) {
			double radius = 16 + wild.nextDouble() * 24;
			double posX = (wild.nextBoolean() ? 1 : -1) * (width / 2 + 40 + wild.nextDouble() * 160);
			double posZ = (wild.nextDouble() - 0.5) * (height + 100);
			Sphere stone = new Sphere(radius, 8);
			stone.setMaterial(index % 3 == 0 ? moss : rock);
			stone.setTranslateX(posX);
			stone.setTranslateZ(posZ);
			stone.setTranslateY(6 - radius * 0.35);
			stone.setScaleY(0.6);
			host.arena.getChildren().add(stone);
		}
	}

	/** Ferns: all the blades in a few meshes, three tones of green */
	void jungleFerns(double width, double height, Random wild, double riverZ, double riverWidth) {
		// Ferns: all the blades in a few meshes, three tones of green
		String[] tones = { "#2f8f2a", "#3da83a", "#25782a" };
		TriangleMesh[] ferns = new TriangleMesh[tones.length];
		for (int index = 0; index < ferns.length; index++) {
			ferns[index] = new TriangleMesh();
			ferns[index].getTexCoords().addAll(0, 0);
		}
		for (int cluster = 0; cluster < 170; cluster++) {
			double posX;
			double posZ;
			do {
				posX = (wild.nextDouble() - 0.5) * (width + 1700);
				posZ = (wild.nextDouble() - 0.5) * (height + 1300);
				// Not in the field, not in the rivers
			} while (Math.abs(posX) < width / 2 + 25 && Math.abs(posZ) < height / 2 + 25 || Math.abs(Math.abs(posZ) - riverZ) < riverWidth / 2 + 14 + 80);
			this.fern(ferns[wild.nextInt(ferns.length)], posX, posZ, wild);
		}
		for (int index2 = 0; index2 < ferns.length; index2++) {
			PhongMaterial tone = new PhongMaterial(Color.web(tones[index2]));
			tone.setSpecularColor(Color.gray(0.15));
			MeshView view = new MeshView(ferns[index2]);
			view.setMaterial(tone);
			view.setCullFace(CullFace.NONE);
			host.arena.getChildren().add(view);
		}
	}

	/** Huge trees far away, grown by a fractal rule */
	void jungleTrees(double width, double height, Random wild, double riverZ, double riverWidth, JungleMaterials jungleMaterials) {
		PhongMaterial bark = jungleMaterials.bark();
		PhongMaterial leaves = jungleMaterials.leaves();
		// Huge trees far away, grown by a fractal rule: a crooked trunk that forks
		// into branches that fork again, with clumps of leaves at the tips
		Forest forest = new Forest();
		for (int index = 0; index < 32; index++) {
			double posX;
			double posZ;
			do {
				double angle = wild.nextDouble() * Math.PI * 2;
				double distance = 950 + wild.nextDouble() * 1100;
				posX = Math.cos(angle) * distance * 1.15;
				posZ = Math.sin(angle) * distance;
				// Not over the rivers
			} while (Math.abs(Math.abs(posZ) - riverZ) < riverWidth / 2 + 90);
			forest.addTree(posX, 6, posZ, 0.85 + wild.nextDouble() * 0.5, wild);
		}
		MeshView trunks = new MeshView(forest.bark);
		trunks.setMaterial(bark);
		trunks.setCullFace(CullFace.NONE);
		MeshView crowns = new MeshView(forest.leaves);
		crowns.setMaterial(leaves);
		crowns.setCullFace(CullFace.BACK);
		host.arena.getChildren().addAll(trunks, crowns);
	}

	/**
	 * The mountain the waterfall falls from: tiers of irregular rocks, higher in
	 * the middle, with buttresses and ledges on both sides of the water, a rock
	 * lip over it, boulders at its foot and moss on the tops. The water is a flat
	 * strip in front of x, so the rocks behind it stay behind x.
	 *
	 * @param side
	 *            1 or -1: which end of the arena, the mountain goes away from the
	 *            field
	 */
	void rockMountain(double posX, double posZ, int side, Random wild) {
		Color[] tones = { Color.web("#4b4a45"), Color.web("#5d5850"), Color.web("#3f4640"), Color.web("#6b6358") };
		LowPoly[] stones = new LowPoly[tones.length];
		for (int index = 0; index < stones.length; index++) {
			stones[index] = new LowPoly();
		}
		LowPoly moss = new LowPoly();
		rockBody(posX, posZ, side, stones, moss, wild);
		rockButtresses(posX, posZ, side, stones, moss, wild);
		rockBoulders(posX, posZ, side, stones, wild);

		for (int index6 = 0; index6 < stones.length; index6++) {
			PhongMaterial material = new PhongMaterial(tones[index6]);
			material.setSpecularColor(Color.gray(0.08));
			material.setSpecularPower(14);
			host.arena.getChildren().add(stones[index6].view(material));
		}
		PhongMaterial mossy = new PhongMaterial(Color.web("#2f5a1f"));
		mossy.setSpecularColor(Color.gray(0.05));
		host.arena.getChildren().add(moss.view(mossy));
	}

	/** The body of the mountain: tiers side by side, tall behind the water and lower at the sides */
	private void rockBody(double posX, double posZ, int side, LowPoly[] stones, LowPoly moss, Random wild) {
		double toward = -side;
		for (int index2 = 0; index2 < 10; index2++) {
			double deltaZ = -250 + index2 * 55 + (wild.nextDouble() - 0.5) * 30;
			boolean channel = Math.abs(deltaZ) < 105;
			double rxValue = 55 + wild.nextDouble() * 35;
			double rzValue = 55 + wild.nextDouble() * 30;
			double height = channel ? 440 + wild.nextDouble() * 70 : 360 - Math.abs(deltaZ) * 0.5 + wild.nextDouble() * 120;
			// Behind the water in the middle, sticking out toward the field at the sides
			double jut = channel ? 0 : wild.nextDouble() * 45;
			double cxValue = posX + side * (rxValue * 1.25 + 4) + toward * jut;
			stones[wild.nextInt(stones.length)].rock(cxValue, posZ + deltaZ, 6, height, rxValue, rzValue, 7, 0.45 + wild.nextDouble() * 0.3, 0.6, wild);
			if (!channel) {
				// A ledge on top of it
				double upperHeight = 90 + wild.nextDouble() * 110;
				stones[wild.nextInt(stones.length)].rock(cxValue + side * 10, posZ + deltaZ, 6 - height * 0.72, upperHeight, rxValue * 0.65, rzValue * 0.65, 6, 0.55, 0.55, wild);
				moss.rock(cxValue, posZ + deltaZ, 6 - height * 0.88, 7, rxValue * 0.5, rzValue * 0.5, 6, 0.8, 0.4, wild);
			}
		}
	}

	/** Buttresses: rocks coming out of the mountain to the sides of the water, and a lip of rock over the water */
	private void rockButtresses(double posX, double posZ, int side, LowPoly[] stones, LowPoly moss, Random wild) {
		double toward = -side;
		for (int index3 = 0; index3 < 8; index3++) {
			double deltaZ2 = (index3 % 2 == 0 ? 1 : -1) * (100 + wild.nextDouble() * 130);
			double rxValue2 = 30 + wild.nextDouble() * 30;
			double rzValue2 = 28 + wild.nextDouble() * 24;
			double height = 90 + wild.nextDouble() * 200;
			double cxValue2 = posX + toward * (12 + wild.nextDouble() * 50);
			stones[wild.nextInt(stones.length)].rock(cxValue2, posZ + deltaZ2, 6, height, rxValue2, rzValue2, 7, 0.4 + wild.nextDouble() * 0.3, 0.6, wild);
			if (wild.nextDouble() < 0.5) {
				moss.rock(cxValue2, posZ + deltaZ2, 6 - height * 0.85, 6, rxValue2 * 0.5, rzValue2 * 0.5, 6, 0.8, 0.4, wild);
			}
		}

		// A lip of rock over the water: it comes out from under it
		stones[1].rock(posX + toward * 16, posZ, 6 - 400, 80, 70, 100, 8, 0.75, 0.45, wild);
		stones[3].rock(posX + toward * 6, posZ + 55, 6 - 380, 70, 40, 45, 6, 0.7, 0.5, wild);
		stones[0].rock(posX + toward * 6, posZ - 55, 6 - 385, 70, 40, 45, 6, 0.7, 0.5, wild);
	}

	/** Boulders at the foot on both sides of the pool, and a few stones in the water at its edge */
	private void rockBoulders(double posX, double posZ, int side, LowPoly[] stones, Random wild) {
		double toward = -side;
		for (int index4 = 0; index4 < 12; index4++) {
			double deltaZ3 = (wild.nextBoolean() ? 1 : -1) * (95 + wild.nextDouble() * 140);
			double radius = 16 + wild.nextDouble() * 28;
			double cxValue3 = posX + toward * (20 + wild.nextDouble() * 230);
			stones[wild.nextInt(stones.length)].rock(cxValue3, posZ + deltaZ3, 6, radius * (0.8 + wild.nextDouble() * 0.8), radius, radius * 0.9, 6, 0.6, 0.55, wild);
		}
		for (int index5 = 0; index5 < 4; index5++) {
			double radius = 10 + wild.nextDouble() * 12;
			stones[wild.nextInt(stones.length)].rock(posX + toward * (60 + wild.nextDouble() * 170), posZ + (wild.nextDouble() - 0.5) * 220, 6, radius, radius, radius, 6, 0.6, 0.5, wild);
		}
	}

	/**
	 * A fern: blades that rise from the ground and bend outwards, added to a mesh
	 */
	void fern(TriangleMesh mesh, double posX, double posZ, Random random) {
		int blades = 8 + random.nextInt(4);
		int segments = 4;
		double scale = 0.8 + random.nextDouble() * 0.8;
		for (int blade = 0; blade < blades; blade++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double length = (30 + random.nextDouble() * 22) * scale;
			double lift = (18 + random.nextDouble() * 14) * scale;
			double width = (3.5 + random.nextDouble() * 2) * scale;
			double sideX = -Math.sin(angle);
			double sideZ = Math.cos(angle);
			int first = mesh.getPoints().size() / 3;
			for (int segment = 0; segment <= segments; segment++) {
				double time = (double) segment / segments;
				double reach = length * time;
				// Rises, then bends down at the tip
				double posY = 6 - lift * Math.sin(time * Math.PI * 0.8);
				double half = width * (1 - 0.85 * time);
				double cxValue = posX + Math.cos(angle) * reach;
				double czValue = posZ + Math.sin(angle) * reach;
				mesh.getPoints().addAll((float) (cxValue - sideX * half), (float) posY, (float) (czValue - sideZ * half), (float) (cxValue + sideX * half), (float) posY, (float) (czValue + sideZ * half));
			}
			for (int segment = 0; segment < segments; segment++) {
				int first2 = first + 2 * segment;
				mesh.getFaces().addAll(first2, 0, first2 + 1, 0, first2 + 3, 0, first2, 0, first2 + 3, 0, first2 + 2, 0);
			}
		}
	}
}
