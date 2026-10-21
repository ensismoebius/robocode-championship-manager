package widgets.scenes;

import static widgets.scenes.BattleArena3D.box;
import static widgets.scenes.BattleArena3D.material;
import static widgets.scenes.BattleArena3D.skyDome;
import static widgets.scenes.BattleArena3D.textured;

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

	JungleArena(BattleArena3D host) {
		this.host = host;
	}

	/**
	 * Crepuscular rays: pale shafts of light slanting through the canopy. Each is
	 * one soft strip that turns around its own axis to face the camera, so they
	 * have no edges and cost little. They do not touch the depth buffer (they are
	 * only a faint haze), and breathe slowly.
	 */
	void buildSunbeams(double w, double h, Random random) {
		host.sunbeams.setDepthTest(DepthTest.DISABLE);
		host.sunbeams.setMouseTransparent(true);
		for (int i = 0; i < 9; i++) {
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
			this.beamSpots.add(new double[] { (random.nextDouble() - 0.5) * (w + 1400), (random.nextDouble() - 0.5) * (h + 1400), 110 + random.nextDouble() * 150 });
			this.beamPhases.add(random.nextDouble() * Math.PI * 2);
		}
		this.beamYaw = Double.NaN;
		host.fireGlow.getScope().add(host.sunbeams);
	}

	/**
	 * The shafts of light slowly get stronger and weaker, out of step, and turn to
	 * the camera
	 */
	void updateSunbeams(double dt) {
		if (this.beamMaterials.isEmpty()) {
			return;
		}
		this.beamClock += dt;
		// A strip that fills the screen is slow to draw, and a haze you are inside of
		// is not seen as a shaft anyway: they fade away as the camera comes close
		Point3D eye = host.camera.localToScene(Point3D.ZERO);
		double top = -1150;
		double bottom = 6;
		double height = bottom - top;
		double along = Math.max(0, Math.min(1, (eye.getY() - top) / height));
		for (int i = 0; i < this.beamMaterials.size(); i++) {
			double[] spot = this.beamSpots.get(i);
			double near = Math.hypot(eye.getX() - (spot[0] + height * 0.38 * along), eye.getZ() - (spot[1] + height * 0.22 * along));
			double away = Math.max(0, Math.min(1, (near - 250) / 700));
			away = away * away * (3 - 2 * away);
			host.sunbeams.getChildren().get(i).setVisible(away > 0.01);
			double breath = 0.5 + 0.5 * Math.sin(this.beamClock * 0.35 + this.beamPhases.get(i));
			this.beamMaterials.get(i).setDiffuseColor(Color.color(1, 1, 1, BEAM_ALPHA * (0.45 + 0.55 * breath) * away));
		}
		double yaw = host.cameraYaw.getAngle();
		if (yaw == this.beamYaw) {
			return;
		}
		this.beamYaw = yaw;
		// The light comes down slanted, from where the key light is
		double dx = height * 0.38;
		double dz = height * 0.22;
		double right = Math.toRadians(yaw);
		for (int i = 0; i < this.beamMeshes.size(); i++) {
			double[] spot = this.beamSpots.get(i);
			double ex = Math.cos(right) * spot[2] / 2;
			double ez = -Math.sin(right) * spot[2] / 2;
			this.beamMeshes.get(i).getPoints().setAll((float) (spot[0] - ex), (float) top, (float) (spot[1] - ez), (float) (spot[0] + ex), (float) top, (float) (spot[1] + ez),
					(float) (spot[0] + ex + dx), (float) bottom, (float) (spot[1] + ez + dz), (float) (spot[0] - ex + dx), (float) bottom, (float) (spot[1] - ez + dz));
		}
	}

	/**
	 * Mossy ground, rivers on both sides, huge trees in the distance, ferns,
	 * rocks and waterfalls. Fireflies float in the air and shafts of light cross the canopy.
	 */
	/** The materials of the jungle, made once for all its parts */
	record JungleMaterials(PhongMaterial moss, PhongMaterial water, PhongMaterial waterfall, PhongMaterial bark, PhongMaterial leaves, PhongMaterial rock) {
	}

	void build(double w, double h) {
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
		JungleMaterials m = new JungleMaterials(moss, water, waterfall, bark, leaves, rock);

		// The sky: a green dome seen from inside
		host.arena.getChildren().add(skyDome(JungleTexture.SKY));
		// The dense forest all around, painted once
		host.arena.getChildren().add(ForestBackdrop.ring());
		this.buildSunbeams(w, h, wild);
		double riverZ = h / 2 + 160;
		double riverWidth = 96;
		this.jungleGround(w, h, m);
		this.jungleRivers(w, h, m, riverZ, riverWidth);
		this.jungleWaterfalls(w, h, wild, m, riverZ, riverWidth);
		this.jungleRocks(w, h, wild, m);
		this.jungleFerns(w, h, wild, riverZ, riverWidth);
		this.jungleTrees(w, h, wild, riverZ, riverWidth, m);
	}

	/** Moss everywhere, a faint grid of lighter moss and a sandy border */
	void jungleGround(double w, double h, JungleMaterials m) {
		PhongMaterial moss = m.moss();
		// Ground: moss everywhere, a bit darker far away (it is the same texture,
		// bigger tiles)
		double far = 5000;
		host.arena.getChildren().add(host.tiledQuad(new double[][] { { -far, 6, -far }, { far, 6, -far }, { far, 6, far }, { -far, 6, far } }, moss, 260));
		host.arena.getChildren().add(host.tiledQuad(new double[][] { { -w / 2 - 20, 0.2, -h / 2 - 20 }, { w / 2 + 20, 0.2, -h / 2 - 20 }, { w / 2 + 20, 0.2, h / 2 + 20 }, { -w / 2 - 20, 0.2, h / 2 + 20 } }, moss, 110));

		// A faint grid of lighter moss and a sandy border, so the field reads
		PhongMaterial line = material(Color.web("#4f8a3c"));
		for (double x = 100; x < w; x += 100) {
			host.arena.getChildren().add(box(1.2, 0.4, h, x - w / 2, -0.2, 0, line));
		}
		for (double y = 100; y < h; y += 100) {
			host.arena.getChildren().add(box(w, 0.4, 1.2, 0, -0.2, y - h / 2, line));
		}
		PhongMaterial sand = material(Color.web("#d6c58a"));
		host.arena.getChildren().addAll(box(w + 4, 0.6, 3, 0, -0.3, -h / 2, sand), box(w + 4, 0.6, 3, 0, -0.3, h / 2, sand), box(3, 0.6, h, -w / 2, -0.3, 0, sand),
				box(3, 0.6, h, w / 2, -0.3, 0, sand));
	}

	/** Rivers along the long sides, flowing in opposite directions */
	void jungleRivers(double w, double h, JungleMaterials m, double riverZ, double riverWidth) {
		// Rivers along the long sides, flowing in opposite directions
		PhongMaterial water = m.water();
		host.lavaFlows.river(-3000, 3000, -1, riverZ, 0, riverWidth, 0.16, water, 120);
		host.lavaFlows.river(3000, -3000, 1, riverZ, 2.3, riverWidth, 0.16, water, 120);
	}

	/** Waterfalls far at both ends, falling from a cliff to a pool */
	void jungleWaterfalls(double w, double h, Random wild, JungleMaterials m, double riverZ, double riverWidth) {
		PhongMaterial water = m.water();
		PhongMaterial waterfall = m.waterfall();
		// Waterfalls far at both ends, falling from a cliff to a pool
		for (int side = -1; side <= 1; side += 2) {
			double x = side * (w / 2 + 1000);
			double z = (wild.nextDouble() - 0.5) * 300;
			// The cliff, its face to the field at x
			this.rockMountain(x, z, side, wild);
			// The water in front of the face, not on it (two surfaces in the same
			// place flicker)
			double front = x - side * 2.5;
			host.lavaFlows.flow(new double[][] { { front, 6 - 450, z - 70 }, { front, 6 - 450, z + 70 }, { front, 5, z + 70 }, { front, 5, z - 70 } }, 0, -0.9, waterfall, 60);
			// The pool at its foot, toward the field
			double poolEnd = x - side * 240;
			double[][] pool = { { x - side * 2.5, 3, z - 150 }, { poolEnd, 3, z - 150 }, { poolEnd, 3, z + 150 }, { x - side * 2.5, 3, z + 150 } };
			host.lavaFlows.flow(pool, 0.025, 0.018, water, 120);
		}
	}

	/** Rocks covered with moss between the field and the rivers */
	void jungleRocks(double w, double h, Random wild, JungleMaterials m) {
		PhongMaterial moss = m.moss();
		PhongMaterial rock = m.rock();
		// Rocks covered with moss between the field and the rivers
		for (int i = 0; i < 16; i++) {
			double radius = 16 + wild.nextDouble() * 24;
			double x = (wild.nextBoolean() ? 1 : -1) * (w / 2 + 40 + wild.nextDouble() * 160);
			double z = (wild.nextDouble() - 0.5) * (h + 100);
			Sphere stone = new Sphere(radius, 8);
			stone.setMaterial(i % 3 == 0 ? moss : rock);
			stone.setTranslateX(x);
			stone.setTranslateZ(z);
			stone.setTranslateY(6 - radius * 0.35);
			stone.setScaleY(0.6);
			host.arena.getChildren().add(stone);
		}
	}

	/** Ferns: all the blades in a few meshes, three tones of green */
	void jungleFerns(double w, double h, Random wild, double riverZ, double riverWidth) {
		// Ferns: all the blades in a few meshes, three tones of green
		String[] tones = { "#2f8f2a", "#3da83a", "#25782a" };
		TriangleMesh[] ferns = new TriangleMesh[tones.length];
		for (int i = 0; i < ferns.length; i++) {
			ferns[i] = new TriangleMesh();
			ferns[i].getTexCoords().addAll(0, 0);
		}
		for (int cluster = 0; cluster < 170; cluster++) {
			double x;
			double z;
			do {
				x = (wild.nextDouble() - 0.5) * (w + 1700);
				z = (wild.nextDouble() - 0.5) * (h + 1300);
				// Not in the field, not in the rivers
			} while (Math.abs(x) < w / 2 + 25 && Math.abs(z) < h / 2 + 25 || Math.abs(Math.abs(z) - riverZ) < riverWidth / 2 + 14 + 80);
			this.fern(ferns[wild.nextInt(ferns.length)], x, z, wild);
		}
		for (int i = 0; i < ferns.length; i++) {
			PhongMaterial tone = new PhongMaterial(Color.web(tones[i]));
			tone.setSpecularColor(Color.gray(0.15));
			MeshView view = new MeshView(ferns[i]);
			view.setMaterial(tone);
			view.setCullFace(CullFace.NONE);
			host.arena.getChildren().add(view);
		}
	}

	/** Huge trees far away, grown by a fractal rule */
	void jungleTrees(double w, double h, Random wild, double riverZ, double riverWidth, JungleMaterials m) {
		PhongMaterial bark = m.bark();
		PhongMaterial leaves = m.leaves();
		// Huge trees far away, grown by a fractal rule: a crooked trunk that forks
		// into branches that fork again, with clumps of leaves at the tips
		Forest forest = new Forest();
		for (int i = 0; i < 32; i++) {
			double x;
			double z;
			do {
				double angle = wild.nextDouble() * Math.PI * 2;
				double distance = 950 + wild.nextDouble() * 1100;
				x = Math.cos(angle) * distance * 1.15;
				z = Math.sin(angle) * distance;
				// Not over the rivers
			} while (Math.abs(Math.abs(z) - riverZ) < riverWidth / 2 + 90);
			forest.addTree(x, 6, z, 0.85 + wild.nextDouble() * 0.5, wild);
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
	void rockMountain(double x, double z, int side, Random wild) {
		Color[] tones = { Color.web("#4b4a45"), Color.web("#5d5850"), Color.web("#3f4640"), Color.web("#6b6358") };
		LowPoly[] stones = new LowPoly[tones.length];
		for (int i = 0; i < stones.length; i++) {
			stones[i] = new LowPoly();
		}
		LowPoly moss = new LowPoly();
		double toward = -side;

		// The body: tiers side by side, tall behind the water and lower at the sides
		for (int i = 0; i < 10; i++) {
			double dz = -250 + i * 55 + (wild.nextDouble() - 0.5) * 30;
			boolean channel = Math.abs(dz) < 105;
			double rx = 55 + wild.nextDouble() * 35;
			double rz = 55 + wild.nextDouble() * 30;
			double height = channel ? 440 + wild.nextDouble() * 70 : 360 - Math.abs(dz) * 0.5 + wild.nextDouble() * 120;
			// Behind the water in the middle, sticking out toward the field at the sides
			double jut = channel ? 0 : wild.nextDouble() * 45;
			double cx = x + side * (rx * 1.25 + 4) + toward * jut;
			stones[wild.nextInt(stones.length)].rock(cx, z + dz, 6, height, rx, rz, 7, 0.45 + wild.nextDouble() * 0.3, 0.6, wild);
			if (!channel) {
				// A ledge on top of it
				double upperHeight = 90 + wild.nextDouble() * 110;
				stones[wild.nextInt(stones.length)].rock(cx + side * 10, z + dz, 6 - height * 0.72, upperHeight, rx * 0.65, rz * 0.65, 6, 0.55, 0.55, wild);
				moss.rock(cx, z + dz, 6 - height * 0.88, 7, rx * 0.5, rz * 0.5, 6, 0.8, 0.4, wild);
			}
		}

		// Buttresses: rocks coming out of the mountain to the sides of the water
		for (int i = 0; i < 8; i++) {
			double dz = (i % 2 == 0 ? 1 : -1) * (100 + wild.nextDouble() * 130);
			double rx = 30 + wild.nextDouble() * 30;
			double rz = 28 + wild.nextDouble() * 24;
			double height = 90 + wild.nextDouble() * 200;
			double cx = x + toward * (12 + wild.nextDouble() * 50);
			stones[wild.nextInt(stones.length)].rock(cx, z + dz, 6, height, rx, rz, 7, 0.4 + wild.nextDouble() * 0.3, 0.6, wild);
			if (wild.nextDouble() < 0.5) {
				moss.rock(cx, z + dz, 6 - height * 0.85, 6, rx * 0.5, rz * 0.5, 6, 0.8, 0.4, wild);
			}
		}

		// A lip of rock over the water: it comes out from under it
		stones[1].rock(x + toward * 16, z, 6 - 400, 80, 70, 100, 8, 0.75, 0.45, wild);
		stones[3].rock(x + toward * 6, z + 55, 6 - 380, 70, 40, 45, 6, 0.7, 0.5, wild);
		stones[0].rock(x + toward * 6, z - 55, 6 - 385, 70, 40, 45, 6, 0.7, 0.5, wild);

		// Boulders at the foot, on both sides of the pool
		for (int i = 0; i < 12; i++) {
			double dz = (wild.nextBoolean() ? 1 : -1) * (95 + wild.nextDouble() * 140);
			double radius = 16 + wild.nextDouble() * 28;
			double cx = x + toward * (20 + wild.nextDouble() * 230);
			stones[wild.nextInt(stones.length)].rock(cx, z + dz, 6, radius * (0.8 + wild.nextDouble() * 0.8), radius, radius * 0.9, 6, 0.6, 0.55, wild);
		}
		// Stones in the water, at the edge of the pool
		for (int i = 0; i < 4; i++) {
			double radius = 10 + wild.nextDouble() * 12;
			stones[wild.nextInt(stones.length)].rock(x + toward * (60 + wild.nextDouble() * 170), z + (wild.nextDouble() - 0.5) * 220, 6, radius, radius, radius, 6, 0.6, 0.5, wild);
		}

		for (int i = 0; i < stones.length; i++) {
			PhongMaterial material = new PhongMaterial(tones[i]);
			material.setSpecularColor(Color.gray(0.08));
			material.setSpecularPower(14);
			host.arena.getChildren().add(stones[i].view(material));
		}
		PhongMaterial mossy = new PhongMaterial(Color.web("#2f5a1f"));
		mossy.setSpecularColor(Color.gray(0.05));
		host.arena.getChildren().add(moss.view(mossy));
	}

	/**
	 * A fern: blades that rise from the ground and bend outwards, added to a mesh
	 */
	void fern(TriangleMesh mesh, double x, double z, Random random) {
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
				double t = (double) segment / segments;
				double reach = length * t;
				// Rises, then bends down at the tip
				double y = 6 - lift * Math.sin(t * Math.PI * 0.8);
				double half = width * (1 - 0.85 * t);
				double cx = x + Math.cos(angle) * reach;
				double cz = z + Math.sin(angle) * reach;
				mesh.getPoints().addAll((float) (cx - sideX * half), (float) y, (float) (cz - sideZ * half), (float) (cx + sideX * half), (float) y, (float) (cz + sideZ * half));
			}
			for (int segment = 0; segment < segments; segment++) {
				int a = first + 2 * segment;
				mesh.getFaces().addAll(a, 0, a + 1, 0, a + 3, 0, a, 0, a + 3, 0, a + 2, 0);
			}
		}
	}
}
