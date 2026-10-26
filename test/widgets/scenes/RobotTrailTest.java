package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The way a robot went in the last moments, for its flames to trail along. */
class RobotTrailTest {

	@Test
	void nothingIsKnownBeforeTheFirstPlace() {
		assertNull(new RobotTrail().placeAt(1));
	}

	@Test
	void theRobotIsWhereItWasAtThatMoment() {
		RobotTrail trail = new RobotTrail();
		trail.record(0, 0, 10);
		trail.record(1, 10, 10);
		trail.record(2, 30, 10);

		assertArrayEquals(new double[] { 10, 10 }, trail.placeAt(1), 1e-9);
		// Between two places it is on the line between them
		assertArrayEquals(new double[] { 20, 10 }, trail.placeAt(1.5), 1e-9);
		assertArrayEquals(new double[] { 30, 10 }, trail.placeAt(2), 1e-9);
	}

	@Test
	void aMomentLaterThanTheNewestIsTheNewestAndOlderThanTheOldestIsTheOldest() {
		RobotTrail trail = new RobotTrail();
		trail.record(5, 1, 2);
		trail.record(6, 3, 4);

		assertArrayEquals(new double[] { 3, 4 }, trail.placeAt(100), 1e-9);
		assertArrayEquals(new double[] { 1, 2 }, trail.placeAt(0), 1e-9);
	}

	@Test
	void aJumpMakesItForgetTheWayBefore() {
		RobotTrail trail = new RobotTrail();
		trail.record(0, 0, 0);
		trail.record(1, 5, 0);
		// A new round: far away at once, and nothing of the old way is on the new one
		trail.record(2, 500, 400);

		assertArrayEquals(new double[] { 500, 400 }, trail.placeAt(0), 1e-9);
		assertArrayEquals(new double[] { 500, 400 }, trail.placeAt(1), 1e-9);
	}

	@Test
	void theOldestPlacesAreForgottenWhenThereAreTooMany() {
		RobotTrail trail = new RobotTrail();
		for (int index = 0; index < 500; index++) {
			trail.record(index, index * 0.1, 0);
		}

		// The first ones are long gone: the oldest known place is one of the last ones
		double[] oldest = trail.placeAt(0);
		assertArrayEquals(new double[] { 40.4, 0 }, oldest, 1.0);
		assertArrayEquals(new double[] { 49.9, 0 }, trail.placeAt(499), 1e-9);
	}

	@Test
	void clearingForgetsEverything() {
		RobotTrail trail = new RobotTrail();
		trail.record(0, 1, 1);
		trail.clear();

		assertNull(trail.placeAt(0));
	}
}
