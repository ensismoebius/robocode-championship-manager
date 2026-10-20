package widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RotationTest {

	@Test
	void usesAllBeforeRepeating() {
		Rotation<String> rotation = new Rotation<String>("a", "b", "c", "d");
		Random random = new Random(1);
		for (int deck = 0; deck < 50; deck++) {
			Set<String> seen = new HashSet<String>();
			for (int i = 0; i < 4; i++) {
				seen.add(rotation.next(random));
			}
			assertEquals(4, seen.size());
		}
	}

	@Test
	void neverRepeatsTheLastOne() {
		for (int seed = 0; seed < 200; seed++) {
			Rotation<Integer> rotation = new Rotation<Integer>(1, 2, 3);
			Random random = new Random(seed);
			List<Integer> picks = new ArrayList<Integer>();
			for (int i = 0; i < 60; i++) {
				picks.add(rotation.next(random));
			}
			for (int i = 1; i < picks.size(); i++) {
				assertNotEquals(picks.get(i - 1), picks.get(i), "seed " + seed + " at " + i);
			}
		}
	}

	@Test
	void aSingleOptionIsAlwaysThatOne() {
		Rotation<String> rotation = new Rotation<String>("only");
		assertEquals("only", rotation.next(new Random(1)));
		assertEquals("only", rotation.next(new Random(1)));
	}
}
