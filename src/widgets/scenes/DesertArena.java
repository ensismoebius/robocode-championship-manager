package widgets.scenes;

import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The desert arena of the Middle East: a courtyard of mosaic tiles with eight-pointed stars, low walls of sandstone
 * around it, a plain of sand with oases of palms, lanterns on tall posts at the corners, a town in the Islamic manner
 * (houses with pointed arches, domes and wind towers, mosques with their minarets, gates between round towers) on the
 * dunes, and the dunes that rise to the horizon under a sunset sky. It builds into the host scene.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertArena {

	/** The side of one tile of the mosaic, in the units of the arena */
	static final double TILE = 100;
	/** How far from the courtyard the lanterns stand: beyond the reach of the creatures */
	private static final double LANTERN_REACH = 440;
	/** How tall the posts of the lanterns are */
	private static final double POST = 120;

	private final BattleArena3D host;

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
		new DesertTown(host).build(width, height, wild);
		new DesertPalms(host).build(width, height, wild);
	}

	/** The mosaic of the courtyard, under the battle, and its low walls of sandstone around it */
	private void courtyard(double width, double height) {
		PhongMaterial mosaic = ArenaMaterials.textured(DesertTexture.MOSAIC, Color.gray(0.2));
		host.arena.getChildren().add(host.tiledQuad(new double[][] { { -width / 2, 0, -height / 2 }, { width / 2, 0, -height / 2 }, { width / 2, 0, height / 2 }, { -width / 2, 0, height / 2 } }, mosaic, TILE));
		LowPoly walls = new LowPoly();
		double thick = 16;
		walls.box(0, 0, -height / 2 - thick / 2, width + 2 * thick, 12, thick);
		walls.box(0, 0, height / 2 + thick / 2, width + 2 * thick, 12, thick);
		walls.box(-width / 2 - thick / 2, 0, 0, thick, 12, height);
		walls.box(width / 2 + thick / 2, 0, 0, thick, 12, height);
		host.arena.getChildren().add(walls.view(ArenaMaterials.textured(DesertTexture.SANDSTONE, Color.gray(0.05))));
	}

	/** Lanterns on tall sandstone posts at the corners of the plain: eight sided, their glass lit from inside, under a dome of gold with its spire */
	private void lanterns(double width, double height) {
		IslamicForms forms = new IslamicForms();
		LowPoly glass = new LowPoly();
		for (int sideX = -1; sideX <= 1; sideX += 2) {
			for (int sideZ = -1; sideZ <= 1; sideZ += 2) {
				double posX = sideX * (width / 2 + LANTERN_REACH);
				double posZ = sideZ * (height / 2 + LANTERN_REACH);
				double ground = DesertDunes.PLAIN - DesertDunes.height(posX, posZ, width, height);
				double lamp = ground - POST;
				forms.stone.box(posX, ground - 6, posZ, 26, 12, 26);
				forms.stone.cylinderY(posX, posZ, 9, ground, lamp, 8);
				glass.cylinderY(posX, posZ, 12, lamp, lamp - 30, 8);
				forms.stone.cylinderY(posX, posZ, 15, lamp - 30, lamp - 34, 8);
				forms.dome(forms.gold, posX, posZ, lamp - 34, 15, 22, 8);
				forms.spire(posX, posZ, lamp - 55, 12, 1.5);
			}
		}
		PhongMaterial light = new PhongMaterial(Color.BLACK);
		light.setSelfIlluminationMap(ArenaHelpers.solidMap(Color.web("#ffb55a")));
		host.arena.getChildren().addAll(forms.views());
		host.arena.getChildren().add(glass.view(light));
	}
}
