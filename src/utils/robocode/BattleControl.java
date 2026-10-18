package utils.robocode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import model.Opponent;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class BattleControl {

	/**
	 * A robot, its robocode rank, its total score and the rounds it won (-1 if
	 * the battle starter did not report it)
	 */
	public record BattleResult(Opponent opponent, int rank, int score, int roundsWon) {
	}

	/**
	 * The results, winner first, and the turns of the battle (empty when it ran
	 * in the robocode window)
	 */
	public record BattleOutcome(List<BattleResult> ranking, BattleRecording recording) {
	}

	/**
	 * How the winner of a battle is chosen. The one with more rounds won may
	 * have fewer points.
	 */
	public enum WinnerCriterion {
		/** Rounds won, total score breaks ties */
		RANK,
		/** Total score, rounds won break ties */
		SCORE;

		Comparator<BattleResult> comparator() {
			// The rank robocode reports may be computed before the last round
			// counts, so the rounds won are used when known
			Comparator<BattleResult> byRounds = (a, b) -> a.roundsWon() >= 0 && b.roundsWon() >= 0 ? Integer.compare(b.roundsWon(), a.roundsWon()) : Integer.compare(a.rank(), b.rank());
			Comparator<BattleResult> byScore = Comparator.comparingInt(BattleResult::score).reversed();
			return this == RANK ? byRounds.thenComparing(byScore) : byScore.thenComparing(byRounds);
		}
	}

	/**
	 * Installs the robots in robocode, runs the battle and waits for it to end.
	 * Blocks, so do not call it from the JavaFX thread.
	 *
	 * @param record
	 *            runs without the robocode window, recording every turn to be
	 *            shown in 3D
	 * @return the opponents and scores sorted by the criterion, winner first, and
	 *         the recording
	 * @throws IOException
	 *             if the robots could not be installed or the battle did not
	 *             produce a result
	 */
	public static BattleOutcome startBattle(List<Opponent> opponents, String robocodePath, int rounds, String startScriptPath, WinnerCriterion criterion, boolean record)
			throws IOException, InterruptedException {

		Path robotsDir = Path.of(robocodePath, "robots");
		if (!Files.isDirectory(robotsDir)) {
			throw new IOException("Diretório de robôs do robocode não encontrado: " + robotsDir);
		}

		Path startScript = Path.of(startScriptPath).toAbsolutePath().normalize();
		if (!Files.isRegularFile(startScript)) {
			throw new IOException("Script de batalha não encontrado: " + startScript);
		}

		List<String> command = new ArrayList<String>();
		// Run through sh so it works even if the script lost its executable bit
		if (startScript.toString().endsWith(".sh")) {
			command.add("sh");
		}
		command.add(startScript.toString());
		command.add(robocodePath);
		command.add(String.valueOf(rounds));
		if (record) {
			command.add("-s");
		}

		for (Opponent opponent : opponents) {
			if (opponent.getCompiledRobot() == null || opponent.getCompiledRobot().length == 0) {
				throw new IOException("O robô " + opponent.getFullName() + " não tem arquivo .jar");
			}
			Path jar = robotsDir.resolve(opponent.getFullName() + ".jar");
			System.out.println("Write to " + jar);
			Files.write(jar, opponent.getCompiledRobot());
			command.add(opponent.getFullName());
		}

		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

		List<String> output = new ArrayList<String>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				// The turns are too many to log
				if (!line.startsWith("#")) {
					System.out.println(line);
				}
				output.add(line);
			}
		}
		process.waitFor();

		List<BattleResult> results = BattleControl.parseRanking(output, opponents, criterion);
		if (results.size() != opponents.size()) {
			throw new IOException("A batalha não retornou o resultado esperado:\n" + String.join("\n", output.stream().filter(l -> !l.startsWith("#")).toList()));
		}
		results.sort(criterion.comparator());
		return new BattleOutcome(results, BattleRecording.parse(output));
	}

	/**
	 * Parses the BattleStarter output. Each result line is
	 * "rank\trobotName\tscore\troundsWon" (older starters do not print the
	 * rounds won). The lines are sorted by score, and the rank robocode reports
	 * may be wrong, so the rounds won decide. Other lines (errors, logs, turns)
	 * are ignored.
	 *
	 * @return the opponents and scores sorted by the criterion, winner first
	 */
	public static List<BattleResult> parseRanking(List<String> output, List<Opponent> opponents, WinnerCriterion criterion) {
		List<BattleResult> results = new ArrayList<BattleResult>();

		for (String line : output) {
			String[] fields = line.split("\t");
			if (fields.length < 2 || !fields[0].trim().matches("[0-9]+")) {
				continue;
			}

			String reportedName = BattleControl.robotClassName(fields[1]);
			int score = fields.length > 2 && fields[2].trim().matches("-?[0-9]+") ? Integer.parseInt(fields[2].trim()) : 0;
			int roundsWon = fields.length > 3 && fields[3].trim().matches("[0-9]+") ? Integer.parseInt(fields[3].trim()) : -1;
			for (Opponent opponent : opponents) {
				if (opponent.getFullName().equals(reportedName) && results.stream().noneMatch(r -> r.opponent() == opponent)) {
					results.add(new BattleResult(opponent, Integer.parseInt(fields[0].trim()), score, roundsWon));
					break;
				}
			}
		}
		results.sort(criterion.comparator());
		return results;
	}

	/**
	 * Robocode may append the version, a "*" (development robot) or an instance
	 * number to the name, e.g. "team.Robot 1.0*" or "team.Robot (1)"
	 */
	public static String robotClassName(String reportedName) {
		return reportedName.trim().split("[ (]")[0].replaceAll("\\*$", "");
	}

}
