package utils.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** The colors of the robots are told while the battle is fought, once for each robot, from the first ones it reports. */
class BattleColorsTest {

	private record Told(String name, int body, int gun, int radar) {
	}

	private static List<Told> told(String... lines) {
		List<Told> told = new ArrayList<Told>();
		BattleColors colors = new BattleColors((name, body, gun, radar) -> told.add(new Told(name, body, gun, radar)));
		for (String line : lines) {
			colors.accept(line);
		}
		return told;
	}

	@Test
	void eachRobotIsToldOnceWithItsFirstColors() {
		List<Told> told = told("#F 800 600 3", "#N 0 sample.Walls 1.0", "#N 1 sample.SpinBot 1.0", "#C 1 -65536 -16711936 -16776961", "#C 0 -1 -2 -3", "#C 1 -9 -9 -9", "#T 0 0|0 1 1 0 0 0 100 0;1 2 2 0 0 0 100 0|");

		assertEquals(List.of(new Told("sample.SpinBot 1.0", -65536, -16711936, -16776961), new Told("sample.Walls 1.0", -1, -2, -3)), told);
	}

	@Test
	void aRobotWithoutNameOrWithABrokenLineIsIgnored() {
		assertEquals(List.of(), told("#C 0 1 2 3", "#N x", "#C 0 broken", "not a line", "#C"));
	}
}
