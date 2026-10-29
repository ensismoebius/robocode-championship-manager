package utils.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import model.Opponent;
import utils.robocode.BattleControl.BattleOutcome;
import utils.robocode.BattleControl.WinnerCriterion;

/** Runs a battle through a script that answers like the BattleStarter, so the whole path of the battle runs without Robocode. */
class BattleControlRunTest {

	private static Path folder(String name) throws IOException {
		Path folder = Path.of("target", "test-home", "battle-" + name);
		Files.createDirectories(folder.resolve("robots"));
		return folder;
	}

	/** A script that prints the lines given as its output, the way the BattleStarter does. */
	private static Path script(String name, String... lines) throws IOException {
		Path script = Path.of("target", "test-home", name + ".sh");
		StringBuilder body = new StringBuilder("#!/bin/sh\n");
		for (String line : lines) {
			body.append("printf '%s\\n' '").append(line.replace("'", "'\\''")).append("'\n");
		}
		Files.writeString(script, body.toString());
		return script;
	}

	private static Opponent robot(String team, String name) {
		Opponent opponent = new Opponent();
		opponent.setTeam(team);
		opponent.setName(name);
		opponent.setCompiledRobot("robo".getBytes());
		return opponent;
	}

	@Test
	void aBattleWithoutRobotsFolderIsRefused() throws IOException {
		Path empty = Path.of("target", "test-home", "no-robocode");
		Files.createDirectories(empty);
		Path script = script("refused");

		assertThrows(IOException.class, () -> BattleControl.startBattle(List.of(robot("sample", "Walls")), empty.toString(), 1, script.toString(), WinnerCriterion.SCORE, false));
	}

	@Test
	void aBattleWithoutItsScriptIsRefused() throws IOException {
		Path folder = folder("no-script");

		assertThrows(IOException.class, () -> BattleControl.startBattle(List.of(robot("sample", "Walls")), folder.toString(), 1, folder.resolve("missing.sh").toString(), WinnerCriterion.SCORE, false));
	}

	@Test
	void aBattleReturnsTheRankingAndTheRecording() throws Exception {
		Path folder = folder("ranking");
		Path script = script("ranking", "#F 800 600 2", "#N 0 sample.Walls", "#R 0", "#T 0 0|0 10 20 0 0 0 100 0;1 30 40 0 0 0 0 3|", "1\tsample.Walls\t216", "2\tsample.SpinBot\t0");
		List<Opponent> fighters = List.of(robot("sample", "Walls"), robot("sample", "SpinBot"));

		BattleOutcome outcome = BattleControl.startBattle(fighters, folder.toString(), 1, script.toString(), WinnerCriterion.SCORE, true);

		assertEquals("sample.Walls", outcome.ranking().get(0).opponent().getFullName());
		assertEquals(216, outcome.ranking().get(0).score());
		assertTrue(Files.exists(folder.resolve("robots").resolve("sample.Walls.jar")));
		assertFalse(outcome.recording().isEmpty());
	}

	@Test
	void aBattleTellsHowFarItIsWhileItRuns() throws Exception {
		Path folder = folder("progress");
		Path script = script("progress", "#F 800 600 1", "#N 0 sample.Walls", "#R 0", "#T 0 0|0 10 20 0 0 0 100 0;1 30 40 0 0 0 100 0|", "#T 0 1|0 10 20 0 0 0 100 0;1 30 40 0 0 0 50 0|",
				"#T 0 2|0 10 20 0 0 0 100 0;1 30 40 0 0 0 0 3|", "1\tsample.Walls\t216", "2\tsample.SpinBot\t0");
		List<Opponent> fighters = List.of(robot("sample", "Walls"), robot("sample", "SpinBot"));
		List<Double> told = new java.util.ArrayList<Double>();

		BattleControl.startBattle(fighters, folder.toString(), 1, script.toString(), WinnerCriterion.SCORE, true, told::add);

		// From the start, half way, all of it, and all of it again when the battle ends; each one only when it grows
		assertEquals(List.of(0.0, 0.5, 1.0, 1.0), told);
	}

	@Test
	void aBattleThatDoesNotReportEveryRobotFails() throws Exception {
		Path folder = folder("incomplete");
		Path script = script("incomplete", "1\tsample.Walls\t216");
		List<Opponent> fighters = List.of(robot("sample", "Walls"), robot("sample", "SpinBot"));

		assertThrows(IOException.class, () -> BattleControl.startBattle(fighters, folder.toString(), 1, script.toString(), WinnerCriterion.SCORE, false));
	}

	@Test
	void aBattleTellsTheColorsOfTheRobotsAsItReportsThem() throws Exception {
		Path folder = folder("colors");
		Path script = script("colors", "#F 800 600 1", "#N 0 sample.Walls 1.0", "#N 1 sample.SpinBot 1.0", "#C 0 -65536 -16711936 -16776961", "#C 1 -1 -2 -3", "#R 0", "#T 0 0|0 10 20 0 0 0 100 0;1 30 40 0 0 0 0 3|",
				"1\tsample.Walls\t216", "2\tsample.SpinBot\t0");
		List<Opponent> fighters = List.of(robot("sample", "Walls"), robot("sample", "SpinBot"));
		List<String> told = new java.util.ArrayList<String>();

		BattleControl.startBattle(fighters, folder.toString(), 1, script.toString(), WinnerCriterion.SCORE, true, progress -> {
		}, (name, body, gun, radar) -> told.add(name + " " + body + " " + gun + " " + radar));

		assertEquals(List.of("sample.Walls 1.0 -65536 -16711936 -16776961", "sample.SpinBot 1.0 -1 -2 -3"), told);
	}
}
