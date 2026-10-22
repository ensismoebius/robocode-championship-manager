package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import support.FxSupport;

class LowPolyTest {

	@Test
	void aTubeWithoutLengthAddsNothingToTheMesh() {
		int points = FxSupport.run(() -> {
			LowPoly shape = new LowPoly();
			// Both ends on the same point: there is no direction to build the tube along
			shape.tube(new double[] { 1, 2, 3 }, new double[] { 1, 2, 3 }, 1, 4);
			return shape.mesh.getPoints().size();
		});

		assertEquals(0, points);
	}

	@Test
	void aTubeWithLengthAddsItsVertices() {
		int points = FxSupport.run(() -> {
			LowPoly shape = new LowPoly();
			shape.tube(new double[] { 0, 0, 0 }, new double[] { 0, 0, 10 }, 1, 4);
			return shape.mesh.getPoints().size();
		});

		assertTrue(points > 0);
	}
}
