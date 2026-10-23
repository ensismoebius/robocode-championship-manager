package widgets.scenes;

import static widgets.scenes.ArenaMaterials.material;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The flags of the Eden Solar arena: poles all around the concourse of the stadium, each with a flag of three
 * bands of color that waves in the breeze.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenFlags {

	private static final int COLUMNS = 7;
	/** The flags are drawn this much bigger than their shape, as the stadium is big */
	private static final double SCALE = 1.4;
	private static final double FLAG_WIDTH = 46 * SCALE;
	private static final double FLAG_HEIGHT = 30 * SCALE;
	private static final double POLE = 120;
	private static final String[] FLAG_COLORS = { "#e63946", "#ffd166", "#06a77d", "#118ab2", "#ef476f", "#f8f9fa", "#7b2cbf", "#ff9f1c" };

	private final BattleArena3D host;
	private final List<Flag> flags = new ArrayList<Flag>();
	private double clock;

	/** The flags of the arena, drawn in the host scene. */
	EdenFlags(BattleArena3D host) {
		this.host = host;
	}

	/** Poles with flags on a ring of the given radius and floor, except in front of the corridors */
	void build(double radius, double floor, Random wild) {
		LowPoly poles = new LowPoly();
		int count = 24;
		for (int index = 0; index < count; index++) {
			double angle = Math.PI * 2 * (index + 0.5) / count;
			if (Math.abs(Math.sin(angle)) < 0.2) {
				continue;
			}
			double reach = radius * (1 + EdenStadium.swell(angle));
			double posX = reach * Math.cos(angle);
			double posZ = reach * Math.sin(angle);
			poles.box(posX, floor - POLE / 2, posZ, 2.6, POLE, 2.6);
			this.flags.add(new Flag(posX + 1.3, floor - POLE + 2, posZ, wild));
		}
		host.arena.getChildren().add(poles.view(material(Color.web("#e8e2cf"))));
	}

	/** The breeze: the flags wave. */
	void update(double deltaTime) {
		this.clock += deltaTime;
		for (Flag flag : this.flags) {
			flag.wave(this.clock);
		}
	}

	/** One flag: three bands of color on a mesh that waves. */
	private final class Flag {
		final TriangleMesh[] bands = new TriangleMesh[3];
		final double posX;
		final double posZ;
		final double top;
		final double phase;

		/** A flag on a pole, with its colors. */
		Flag(double posX, double top, double posZ, Random wild) {
			this.posX = posX;
			this.top = top;
			this.posZ = posZ;
			this.phase = wild.nextDouble() * Math.PI * 2;
			for (int band = 0; band < 3; band++) {
				TriangleMesh mesh = new TriangleMesh();
				mesh.getTexCoords().addAll(0, 0);
				mesh.getPoints().addAll(new float[COLUMNS * 2 * 3]);
				for (int column = 0; column < COLUMNS - 1; column++) {
					int first = column * 2;
					mesh.getFaces().addAll(first, 0, first + 1, 0, first + 3, 0, first, 0, first + 3, 0, first + 2, 0);
				}
				this.bands[band] = mesh;
				MeshView view = new MeshView(mesh);
				view.setMaterial(material(Color.web(FLAG_COLORS[wild.nextInt(FLAG_COLORS.length)])));
				view.setCullFace(CullFace.NONE);
				host.arena.getChildren().add(view);
			}
			this.wave(0);
		}

		/** Puts the points of the flag where the breeze has them at that time. */
		void wave(double time) {
			for (int band = 0; band < 3; band++) {
				float[] points = new float[COLUMNS * 2 * 3];
				for (int column = 0; column < COLUMNS; column++) {
					double along = column * FLAG_WIDTH / (COLUMNS - 1);
					double shape = along / SCALE;
					double sway = Math.sin(shape * 0.16 - time * 3.2 + this.phase) * (1 + shape * 0.12) * SCALE;
					for (int edge = 0; edge < 2; edge++) {
						int point = (column * 2 + edge) * 3;
						points[point] = (float) (this.posX + along);
						points[point + 1] = (float) (this.top + (band + edge) * FLAG_HEIGHT / 3 + Math.cos(shape * 0.2 - time * 2.4 + this.phase) * 0.8 * SCALE);
						points[point + 2] = (float) (this.posZ + sway);
					}
				}
				this.bands[band].getPoints().setAll(points);
			}
		}
	}
}
