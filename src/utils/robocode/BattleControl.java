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
import java.util.function.DoubleConsumer;

import model.Opponent;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class BattleControl {

	/** Only static methods: this class is not instantiated. */
	private BattleControl() {
	}

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
	 * The two kinds of battle. The one with more rounds won may have fewer points.
	 */
	public enum WinnerCriterion {
		/**
		 * By knockout, the best of the rounds: rounds won, total score breaks ties.
		 * The battle ends as soon as a robot has won most of the rounds. A round
		 * with a double knockout has no winner.
		 */
		RANK,
		/** By points: all the rounds are fought, total score decides, rounds won break ties */
		SCORE;

		/** Whether the battle ends when a robot has won most of the rounds */
		public boolean isKnockout() {
			return this == RANK;
		}

		/** The order of the results of a battle: by the rounds won when they are known, otherwise by the rank that Robocode reports. */
		Comparator<BattleResult> comparator() {
			// The rank robocode reports may be computed before the last round
			// counts, so the rounds won are used when known
			Comparator<BattleResult> byRounds = (first, second) -> first.roundsWon() >= 0 && second.roundsWon() >= 0 ? Integer.compare(second.roundsWon(), first.roundsWon()) : Integer.compare(first.rank(), second.rank());
			Comparator<BattleResult> byScore = Comparator.comparingInt(BattleResult::score).reversed();
			return this == RANK ? byRounds.thenComparing(byScore) : byScore.thenComparing(byRounds);
		}
	}

	/**
	 * In a best of that many rounds, how many a robot has to win to take the
	 * battle: best of 5 takes 3
	 */
	public static int roundsToWin(int rounds) {
		return Math.max(1, rounds) / 2 + 1;
	}

	/**
	 * What follows the robocode directory in the command of the battle starter:
	 * the rounds, then the options
	 */
	static List<String> battleArguments(int rounds, WinnerCriterion criterion, boolean record) {
		List<String> arguments = new ArrayList<String>();
		arguments.add(String.valueOf(rounds));
		if (record) {
			arguments.add("-s");
		}
		if (criterion.isKnockout()) {
			arguments.add("-k");
		}
		return arguments;
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
		return startBattle(opponents, robocodePath, rounds, startScriptPath, criterion, record, progress -> {
		});
	}

	/**
	 * Like the one above, and tells how far the battle is while it runs.
	 *
	 * @param onProgress
	 *            called, from the thread that runs the battle, each time the progress grows by
	 *            at least a hundredth, from 0 to 1, and with 1 when the battle is over
	 */
	public static BattleOutcome startBattle(List<Opponent> opponents, String robocodePath, int rounds, String startScriptPath, WinnerCriterion criterion, boolean record, DoubleConsumer onProgress)
			throws IOException, InterruptedException {
		return startBattle(opponents, robocodePath, rounds, startScriptPath, criterion, record, onProgress, (name, body, gun, radar) -> {
		});
	}

	/**
	 * Like the one above, and tells the colors of each robot as soon as the battle reports them.
	 *
	 * @param onColors
	 *            called, from the thread that runs the battle, with the first colors of each robot
	 */
	public static BattleOutcome startBattle(List<Opponent> opponents, String robocodePath, int rounds, String startScriptPath, WinnerCriterion criterion, boolean record, DoubleConsumer onProgress,
			BattleColors.Listener onColors) throws IOException, InterruptedException {

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
		command.addAll(battleArguments(rounds, criterion, record));

		installRobots(robotsDir, opponents, command);

		List<String> output = runBattleProcess(command, onProgress, new BattleColors(onColors));

		List<BattleResult> results = BattleControl.parseRanking(output, opponents, criterion);
		if (results.size() != opponents.size()) {
			throw new IOException("A batalha não retornou o resultado esperado:\n" + String.join("\n", output.stream().filter(length -> !length.startsWith("#")).toList()));
		}
		results.sort(criterion.comparator());
		return new BattleOutcome(results, BattleRecording.parse(output));
	}

	/** Writes the jar of each opponent in the robots folder of Robocode, and adds its class name to the command. */
	private static void installRobots(Path robotsDir, List<Opponent> opponents, List<String> command) throws IOException {
		for (Opponent opponent : opponents) {
			if (opponent.getCompiledRobot() == null || opponent.getCompiledRobot().length == 0) {
				throw new IOException("O robô " + opponent.getFullName() + " não tem arquivo .jar");
			}
			Path jar = robotsDir.resolve(opponent.getFullName() + ".jar");
			System.out.println("Write to " + jar);
			Files.write(jar, opponent.getCompiledRobot());
			command.add(opponent.getFullName());
		}
	}

	/** Runs the battle command and returns the lines it printed; the turns, which start with #, are not echoed. */
	private static List<String> runBattleProcess(List<String> command, DoubleConsumer onProgress, BattleColors colors) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
		// The script runs java: the one this program runs on, even without java in the PATH
		builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
		Process process = builder.start();

		List<String> output = new ArrayList<String>();
		BattleProgress progress = new BattleProgress();
		double told = -1;
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				// The turns are too many to log
				if (!line.startsWith("#")) {
					System.out.println(line);
				}
				output.add(line);
				colors.accept(line);
				double now = progress.accept(line);
				if (now - told >= 0.01) {
					told = now;
					onProgress.accept(now);
				}
			}
		}
		process.waitFor();
		onProgress.accept(progress.finish());
		return output;
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
				if (opponent.getFullName().equals(reportedName) && results.stream().noneMatch(row -> row.opponent() == opponent)) {
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
