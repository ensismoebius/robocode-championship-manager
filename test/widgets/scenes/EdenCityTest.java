package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import support.FxSupport;

/** The solarpunk African city outside the stadium of the Eden Solar. */
class EdenCityTest {

	/** How many triangles the meshes under the node have. */
	private static int triangles(Node node) {
		int count = node instanceof MeshView view ? ((TriangleMesh) view.getMesh()).getFaces().size() / 6 : 0;
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				count += triangles(child);
			}
		}
		return count;
	}

	@Test
	void theEdenHasACityWithEveryKindOfBuilding() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");
		EdenForms forms = arena.eden.city().forms();

		assertTrue(forms.houses > 20, "houses: " + forms.houses);
		assertTrue(forms.terraces > 20, "towers of terraces: " + forms.terraces);
		assertTrue(forms.spires > 5, "towers of mud: " + forms.spires);
		assertTrue(forms.baobabs > 10, "baobab towers: " + forms.baobabs);
		assertTrue(forms.trees > 50, "solar trees: " + forms.trees);
	}

	@Test
	void fourGreatBaobabsStandOverTheCityBetweenAvenues() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");

		assertEquals(4, arena.eden.city().grandBaobabs().size());
		for (double[] baobab : arena.eden.city().grandBaobabs()) {
			assertTrue(baobab[2] >= 800, "a great baobab is tall: " + baobab[2]);
			double angle = Math.floorMod(Math.round(Math.toDegrees(Math.atan2(baobab[1], baobab[0]))), 90);
			assertTrue(Math.abs(angle - 60) <= 5, "between two avenues, at 60 degrees of its quarter: " + angle);
		}
	}

	@Test
	void theCityIsBeyondTheWallOfTheStadium() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");

		for (double[] baobab : arena.eden.city().grandBaobabs()) {
			double angle = Math.atan2(baobab[1], baobab[0]);
			double wall = 1948 * (1 + EdenStadium.swell(angle));
			assertTrue(Math.hypot(baobab[0], baobab[1]) > wall + 400, "a great baobab is outside the stadium");
		}
	}

	@Test
	void theCityIsLightEnoughToDrawInSoftware() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");
		BattleArena3D desert = ArenaSimulation.arena(ArenaSimulation.fighters(), "MIDDLE_EAST");

		int eden = triangles(arena.arena);
		assertTrue(eden < 400_000, "the Eden has " + eden + " triangles");
		assertTrue(triangles(desert.arena) > 0);
	}

	@Test
	void theAirshipsDriftAroundTheStadium() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");
		EdenAir air = arena.eden.air();
		assertEquals(5, air.count());

		Node ship = arena.arena.getChildren().stream().filter(node -> node instanceof javafx.scene.Group group && group.getChildren().size() == 4 && group.getTranslateY() < -1000).findFirst().orElseThrow();
		double before = ship.getTranslateX() + ship.getTranslateZ();
		FxSupport.runVoid(() -> air.update(30));

		assertFalse(before == ship.getTranslateX() + ship.getTranslateZ(), "the airship did not move");
	}

	@Test
	void otherArenasHaveNoCity() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "JUNGLE");

		assertNull(arena.eden.city());
		assertNotNull(arena.eden);
	}

	@Test
	void theTourOfTheCityRisesOverTheRoofGoesToAGreatBaobabAndDownAnAvenue() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");
		java.util.List<FlyTour.Stop> stops = EdenTour.stops(arena.eden.city(), 1, 1);

		assertEquals(3, stops.size());
		double[] eye = new double[3];
		double[] look = new double[3];
		stops.get(0).pose().at(0.5, eye, look);
		assertTrue(eye[1] < EdenStadium.rim(), "rises out of the stadium over its roof");
		stops.get(1).pose().at(0.5, eye, look);
		assertTrue(look[1] < -400, "looks up at the crown of the great baobab");
		for (double share = 0; share <= 1; share += 0.05) {
			stops.get(1).pose().at(share, eye, look);
			assertTrue(eye[1] <= arena.eden.city().clearance(eye[0], eye[2]), "the camera goes into a building of the city");
		}
		stops.get(2).pose().at(0, eye, look);
		double start = Math.hypot(eye[0], eye[2]);
		stops.get(2).pose().at(1, eye, look);
		assertTrue(Math.hypot(eye[0], eye[2]) < start - 1500, "the avenue goes toward the stadium");
		assertTrue(EdenTour.stops(null, 1, 1).isEmpty());
	}
}
