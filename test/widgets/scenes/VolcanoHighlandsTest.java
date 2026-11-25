package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import support.FxSupport;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The highlands of the volcano arena: the lake of lava ends under their shore, the volcanoes stand in the lake, the caves open
 * between the volcanoes, and the tour shows a cave without going through a volcano.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
class VolcanoHighlandsTest {

	private static BattleArena3D volcano() {
		return ArenaSimulation.arena(ArenaSimulation.fighters(), "VOLCANO");
	}

	@Test
	void theLakeOfLavaEndsUnderTheShoreAllAround() {
		for (double angle = 0; angle < Math.PI * 2; angle += 0.01) {
			double shore = VolcanoHighlands.shore(angle);
			assertTrue(shore >= VolcanoHighlands.SHORE, "the shore is never nearer than the volcanoes allow");
			// Along the sides of the lake its edge is nearest: still beyond the shore
			assertTrue(shore + 60 < VolcanoHighlands.LAKE, "the lake goes under the shore at " + angle);
		}
	}

	@Test
	void theVolcanoesAndTheirFallsAreInTheLakeShortOfTheShore() {
		VolcanoHighlands highlands = volcano().volcano.highlands();

		assertFalse(highlands.peaks().isEmpty());
		for (VolcanoHighlands.Peak peak : highlands.peaks()) {
			double distance = Math.hypot(peak.posX(), peak.posZ());
			double shore = VolcanoHighlands.shore(Math.atan2(peak.posZ(), peak.posX()));
			assertTrue(distance + peak.radius() * 1.3 < shore, "a volcano reaches the shore");
		}
	}

	@Test
	void theCavesOpenBetweenTheVolcanoesAtTheFootOfTheCliffs() {
		VolcanoHighlands highlands = volcano().volcano.highlands();

		assertFalse(highlands.caves().isEmpty());
		for (VolcanoHighlands.Cave cave : highlands.caves()) {
			assertTrue(cave.radius() > VolcanoHighlands.shore(cave.angle()) + VolcanoHighlands.CAVE_DEPTH, "the mouth of the cave is on its ledge, not in the lava");
			for (VolcanoHighlands.Peak peak : highlands.peaks()) {
				double[] mouth = VolcanoHighlands.mouth(cave);
				// Seen from the field, no volcano stands in front of the cave
				double apart = Math.abs(Math.atan2(Math.sin(Math.atan2(peak.posZ(), peak.posX()) - cave.angle()), Math.cos(Math.atan2(peak.posZ(), peak.posX()) - cave.angle())));
				double across = Math.sin(apart) * Math.hypot(peak.posX(), peak.posZ());
				assertTrue(apart > Math.PI / 2 || across > peak.radius() + cave.width() / 2 * Math.hypot(peak.posX(), peak.posZ()) / Math.hypot(mouth[0], mouth[2]),
						"a volcano hides a cave");
			}
		}
	}

	@Test
	void theTourOfTheVolcanoShowsACave() {
		BattleArena3D arena = volcano();
		FlyTour tour = arena.flyTour;
		FxSupport.runVoid(() -> tour.plan(arena, 1, 1));
		VolcanoHighlands highlands = arena.volcano.highlands();

		assertNotNull(VolcanoTour.cave(highlands.caves(), highlands.peaks(), new double[] { 1060, -600, 1060 }));
		assertTrue(tour.duration() >= 4.5 + VolcanoTour.CAVE_SECONDS, "the tour has the sweep and the cave");
		// The camera in front of a cave is over the lake, nearer the field than the cave
		for (VolcanoHighlands.Cave cave : highlands.caves()) {
			double[] eye = VolcanoTour.eye(cave, 1);
			assertTrue(Math.hypot(eye[0], eye[2]) < VolcanoHighlands.shore(cave.angle()));
			assertTrue(eye[1] < -VolcanoHighlands.LEDGE_HEIGHT - cave.height() * 0.5, "the camera is above the floor of the cave");
		}
	}
}
