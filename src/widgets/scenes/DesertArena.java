package widgets.scenes;

import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The desert arena of the Middle East, in a future that kept its architecture: a courtyard of zellij tiles, the star and
 * cross with its eight-pointed stars, whose lines of plaster glow, low walls of ceramic around it, a plain of sand with
 * oases of palms, lanterns of light on tall posts at the corners, a city in the Islamic manner but of ceramic, glass and
 * light (houses with pointed arches, domes and wind towers, mosques with their minarets, gates between round towers) and
 * skyscrapers in the style of the contemporary Arab cities, laid out on the grid of a city with its streets, its
 * gardens and its plazas, beyond a ring of dunes that stays close to the courtyard, under a sunset sky. It builds into the
 * host scene.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertArena {

	/** The side of one tile of the mosaic, in the units of the arena */
	static final double TILE = 100;
	/** The width of the border of small stars around the field */
	private static final double BORDER = 50;
	/** How far from the courtyard the lanterns stand: beyond the reach of the creatures */
	private static final double LANTERN_REACH = 440;
	/** How tall the posts of the lanterns are */
	private static final double POST = 120;

	private final BattleArena3D host;
	private DesertCity city;

	/** The desert of the arena, drawn in the host scene. */
	DesertArena(BattleArena3D host) {
		this.host = host;
	}

	/** Builds the whole desert over a courtyard of the given size. */
	void build(double width, double height) {
		Random wild = new Random(1001);
		host.arena.getChildren().add(ArenaMaterials.skyDome(DesertTexture.SKY));
		this.courtyard(width, height);
		DesertDunes.build(host, width, height);
		this.lanterns(width, height);
		this.city = new DesertCity(width, height);
		this.city.build(host);
		new DesertTown(host).build(width, height, wild, this.city);
		new DesertSkyline(host).build(width, height, this.city.towers());
		new DesertPalms(host).build(width, height, wild);
	}

	/** The middles {x, z} of the plazas of the skyscrapers of the city, none before the desert is built. */
	List<double[]> towers() {
		return this.city == null ? List.of() : this.city.towers();
	}

	/** The mosaic of the courtyard, under the battle, and its low walls of sandstone around it */
	private void courtyard(double width, double height) {
		PhongMaterial mosaic = ArenaMaterials.textured(DesertTexture.MOSAIC, Color.gray(0.2));
		mosaic.setSelfIlluminationMap(DesertTexture.FLOOR_LIGHT);
		// The field is a panel of tiles of the star and cross, framed by a border of the same pattern in a smaller size, as the panels of old tile work
		double left = -width / 2;
		double right = width / 2;
		double near = -height / 2;
		double far = height / 2;
		host.arena.getChildren().add(host.tiledQuad(rectangle(left + BORDER, near + BORDER, right - BORDER, far - BORDER), mosaic, TILE));
		host.arena.getChildren().add(host.tiledQuad(rectangle(left, near, right, near + BORDER), mosaic, BORDER));
		host.arena.getChildren().add(host.tiledQuad(rectangle(left, far - BORDER, right, far), mosaic, BORDER));
		host.arena.getChildren().add(host.tiledQuad(rectangle(left, near + BORDER, left + BORDER, far - BORDER), mosaic, BORDER));
		host.arena.getChildren().add(host.tiledQuad(rectangle(right - BORDER, near + BORDER, right, far - BORDER), mosaic, BORDER));
		LowPoly walls = new LowPoly();
		double thick = 16;
		walls.box(0, 0, -height / 2 - thick / 2, width + 2 * thick, 12, thick);
		walls.box(0, 0, height / 2 + thick / 2, width + 2 * thick, 12, thick);
		walls.box(-width / 2 - thick / 2, 0, 0, thick, 12, height);
		walls.box(width / 2 + thick / 2, 0, 0, thick, 12, height);
		host.arena.getChildren().add(walls.view(ArenaMaterials.material(Color.web("#c9d7dd"))));
	}

	/** The corners of a rectangle of the floor, in the order a tiled quad takes them */
	private static double[][] rectangle(double fromX, double fromZ, double toX, double toZ) {
		return new double[][] { { fromX, 0, fromZ }, { toX, 0, fromZ }, { toX, 0, toZ }, { fromX, 0, toZ } };
	}

	/** Lanterns on tall ceramic posts at the corners of the plain: eight sided, their glass lit from inside, under a dome of gold with its spire and a halo of light */
	private void lanterns(double width, double height) {
		IslamicForms forms = new IslamicForms();
		LowPoly glass = new LowPoly();
		for (int sideX = -1; sideX <= 1; sideX += 2) {
			for (int sideZ = -1; sideZ <= 1; sideZ += 2) {
				double posX = sideX * (width / 2 + LANTERN_REACH);
				double posZ = sideZ * (height / 2 + LANTERN_REACH);
				double ground = DesertDunes.PLAIN - DesertDunes.height(posX, posZ, width, height);
				double lamp = ground - POST;
				forms.ceramic.box(posX, ground - 6, posZ, 26, 12, 26);
				forms.ceramic.cylinderY(posX, posZ, 9, ground, lamp, 8);
				glass.cylinderY(posX, posZ, 12, lamp, lamp - 30, 8);
				forms.ceramic.cylinderY(posX, posZ, 15, lamp - 30, lamp - 34, 8);
				forms.dome(forms.gold, posX, posZ, lamp - 34, 15, 22, 8);
				forms.spire(posX, posZ, lamp - 55, 12, 1.5);
				forms.halo(posX, posZ, lamp - 18, 30);
			}
		}
		PhongMaterial light = new PhongMaterial(Color.BLACK);
		light.setSelfIlluminationMap(ArenaHelpers.solidMap(Color.web("#ffb55a")));
		host.arena.getChildren().addAll(forms.views());
		host.arena.getChildren().add(glass.view(light));
	}
}
