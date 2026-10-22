package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import support.FxSupport;

/** The materials and shapes that the arena and its parts share. */
class ArenaMaterialsTest {

	@Test
	void theSkyIsAHugeDomeSeenFromInside() {
		Image picture = FxSupport.run(() -> ArenaHelpers.solidMap(Color.NAVY));

		Sphere sky = FxSupport.run(() -> ArenaMaterials.skyDome(picture));

		assertEquals(14000, sky.getRadius());
		assertEquals(CullFace.FRONT, sky.getCullFace());
	}

	@Test
	void distanceBetweenPointsIsSquared() {
		assertEquals(9, ArenaMaterials.distance2(new double[] { 0, 0, 0 }, new double[] { 1, 2, 2 }), 1e-9);
	}

	@Test
	void materialsKeepTheirLookAndTheirColor() {
		FxSupport.runVoid(() -> {
			PhongMaterial texture = ArenaMaterials.textured(ArenaHelpers.solidMap(Color.GREEN), Color.WHITE);
			assertEquals(40, texture.getSpecularPower(), 1e-9);
			assertEquals(Color.WHITE, texture.getSpecularColor());

			PhongMaterial shiny = ArenaMaterials.material(Color.RED);
			assertEquals(Color.RED, shiny.getDiffuseColor());
			assertEquals(24, shiny.getSpecularPower(), 1e-9);

			// A dark paint keeps a floor of light so its shape still shows
			PhongMaterial dark = ArenaMaterials.paint(Color.BLACK);
			assertEquals(0.38, dark.getSpecularColor().getRed(), 1e-6);
			assertEquals(36, dark.getSpecularPower(), 1e-9);
			assertEquals(1, ArenaMaterials.paint(Color.WHITE).getSpecularColor().getRed(), 1e-9);
		});
	}

	@Test
	void seeThroughMaterialsHaveTheirOpacity() {
		FxSupport.runVoid(() -> {
			PhongMaterial bubble = ArenaMaterials.bubble(Color.CYAN);
			assertEquals(0.3, bubble.getDiffuseColor().getOpacity(), 1e-6);

			PhongMaterial energy = ArenaMaterials.energy(Color.ORANGE, 0.5);
			assertEquals(0.5, energy.getDiffuseColor().getOpacity(), 1e-9);
			assertNotNull(energy.getSelfIlluminationMap());
		});
	}

	@Test
	void shapesAndPiecesAreBuiltWhereTheyAreAsked() {
		FxSupport.runVoid(() -> {
			PhongMaterial paint = ArenaMaterials.material(Color.BLUE);
			Cylinder cylinder = ArenaMaterials.cylinder(5, 20, 12, 1, 2, 3, new Point3D(0, 1, 0), 90, paint);
			assertEquals(5, cylinder.getRadius(), 1e-9);
			assertEquals(1, cylinder.getTranslateX(), 1e-9);
			assertEquals(3, cylinder.getTranslateZ(), 1e-9);
			assertEquals(90, cylinder.getRotate(), 1e-9);

			Box box = ArenaMaterials.box(4, 6, 8, 10, 20, 30, paint);
			assertEquals(8, box.getDepth(), 1e-9);
			assertEquals(20, box.getTranslateY(), 1e-9);

			MeshView shape = ArenaMaterials.shape(ArenaMaterials.CRYSTAL, paint);
			assertEquals(CullFace.NONE, shape.getCullFace());
			assertTrue(ArenaMaterials.CRYSTAL.getPoints().size() > 0);
		});
	}

	@Test
	void theScaleOfAPieceGrowsAndVanishes() {
		FxSupport.runVoid(() -> {
			Box box = ArenaMaterials.box(1, 1, 1, 0, 0, 0, ArenaMaterials.material(Color.WHITE));

			var grow = ArenaMaterials.grow(box, 0.5, 2, 300);
			assertEquals(0.5, grow.getFromX(), 1e-9);
			assertEquals(2, grow.getToZ(), 1e-9);

			var vanish = ArenaMaterials.shrink(box, 50, 100);
			assertEquals(50, vanish.getDelay().toMillis(), 1e-9);
			assertEquals(0, vanish.getToY(), 1e-9);
		});
	}

	@Test
	void theRobotColorsAreTheArgbOnesOrTheFallback() {
		assertEquals(Color.RED, ArenaMaterials.argb(0, Color.RED));

		Color robot = ArenaMaterials.argb(0xFF102030, Color.RED);
		assertEquals(16 / 255.0, robot.getRed(), 1e-6);
		assertEquals(32 / 255.0, robot.getGreen(), 1e-6);
		assertEquals(48 / 255.0, robot.getBlue(), 1e-6);
	}
}
