package widgets.scenes;

import static widgets.scenes.ArenaMaterials.material;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The outside of the Eden Solar stadium, which the city sees: a wall all the way round the back of the stands, from the ground
 * to the roof, facing out. At the foot an arcade of mud with its lit slits, over it a band of Ndebele diamonds alternating with
 * green walls of leaves and flowers, between pilasters of mud, and at the top a band of solar panels with a garden on the cornice.
 * It faces out only, so from inside the stadium, and from a camera above it, nothing of it is drawn.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenFacade {

	/** How many pieces a pilaster is apart, and a green wall is wide, in pieces of the wall. */
	private static final int PILASTER_EVERY = 16;
	private static final int PAINTED_PIECES = 10;
	/** The heights of the bands, from the ground: the arcade, and the band of solar panels under the roof. */
	private static final double ARCADE = 120;
	private static final double SOLAR_BAND = 110;
	private static final double PILASTER_DEPTH = 22;

	private final BattleArena3D host;
	private final double edge;

	/** The facade of a stadium whose outer wall is at the given radius, before its plan swells. */
	EdenFacade(BattleArena3D host, double edge) {
		this.host = host;
		this.edge = edge;
	}

	/** The wall between two heights, in one piece of the ring, facing out, with the picture repeating along it. */
	private void piece(SurfaceMesh mesh, int segment, double bottom, double top) {
		double first = EdenStadium.angle(segment);
		double second = EdenStadium.angle(segment + 1);
		double[] lowFirst = EdenStadium.point(first, this.edge, bottom);
		double[] lowSecond = EdenStadium.point(second, this.edge, bottom);
		double[] highSecond = EdenStadium.point(second, this.edge, top);
		double[] highFirst = EdenStadium.point(first, this.edge, top);
		float run = (float) (Math.hypot(lowSecond[0] - lowFirst[0], lowSecond[2] - lowFirst[2]) / 128);
		float rise = (float) ((bottom - top) / 128);
		double middle = (first + second) / 2;
		mesh.quad(lowFirst, lowSecond, highSecond, highFirst, new float[] { 0, rise, run, rise, run, 0, 0, 0 }, new double[] { Math.cos(middle), 0, Math.sin(middle) });
	}

	/** Builds the wall, the pilasters and the garden on the cornice. */
	void build() {
		SurfaceMesh arcade = new SurfaceMesh();
		SurfaceMesh painted = new SurfaceMesh();
		SurfaceMesh green = new SurfaceMesh();
		SurfaceMesh solar = new SurfaceMesh();
		SurfaceMesh cornice = new SurfaceMesh();
		double ground = EdenForms.GROUND;
		double roof = EdenStadium.roofBase();
		for (int segment = 0; segment < EdenStadium.SEGMENTS; segment++) {
			this.piece(arcade, segment, ground, ground - ARCADE);
			this.piece(segment % PILASTER_EVERY < PAINTED_PIECES ? painted : green, segment, ground - ARCADE, roof + SOLAR_BAND);
			this.piece(solar, segment, roof + SOLAR_BAND, roof);
			double[] first = EdenStadium.point(EdenStadium.angle(segment), this.edge, roof);
			double[] second = EdenStadium.point(EdenStadium.angle(segment + 1), this.edge, roof);
			double[] outerFirst = EdenStadium.point(EdenStadium.angle(segment), this.edge + 18, roof);
			double[] outerSecond = EdenStadium.point(EdenStadium.angle(segment + 1), this.edge + 18, roof);
			cornice.quad(first, second, outerSecond, outerFirst, new float[] { 0, 0, 0.4f, 0, 0.4f, 0.15f, 0, 0.15f }, new double[] { 0, -1, 0 });
		}
		LowPoly pilasters = new LowPoly();
		for (int segment = 0; segment < EdenStadium.SEGMENTS; segment += PILASTER_EVERY) {
			double angle = EdenStadium.angle(segment);
			double half = 22 / (this.edge * (1 + EdenStadium.swell(angle)));
			// They start just outside the wall: inside it they would cross the band of the screens, seen from the stands
			pilasters.frustum(new double[][] { EdenStadium.flat(angle - half, this.edge + 0.5), EdenStadium.flat(angle + half, this.edge + 0.5), EdenStadium.flat(angle + half, this.edge + PILASTER_DEPTH), EdenStadium.flat(angle - half, this.edge + PILASTER_DEPTH) }, ground,
					roof, 1);
		}
		PhongMaterial clay = ArenaMaterials.textured(EdenCityTexture.ADOBE, Color.gray(0.05));
		clay.setSelfIlluminationMap(EdenCityTexture.ADOBE_LIGHT);
		PhongMaterial paint = ArenaMaterials.textured(EdenCityTexture.NDEBELE, Color.gray(0.05));
		paint.setSelfIlluminationMap(EdenCityTexture.NDEBELE_LIGHT);
		PhongMaterial leaves = ArenaMaterials.textured(EdenCityTexture.LEAVES, Color.gray(0.05));
		PhongMaterial panels = ArenaMaterials.textured(EdenCityTexture.SOLAR, Color.gray(0.4));
		panels.setSelfIlluminationMap(EdenCityTexture.SOLAR_LIGHT);
		host.arena.getChildren().addAll(arcade.view(clay), painted.view(paint), green.view(leaves), solar.view(panels), cornice.view(leaves), pilasters.view(material(Color.web("#b9683d"))));
	}
}
