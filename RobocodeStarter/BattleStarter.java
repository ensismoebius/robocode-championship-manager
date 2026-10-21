import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import robocode.BattleResults;
import robocode.BattleRules;
import robocode.control.BattleSpecification;
import robocode.control.BattlefieldSpecification;
import robocode.control.RobocodeEngine;
import robocode.control.RobotSpecification;
import robocode.control.events.BattleAdaptor;
import robocode.control.events.BattleCompletedEvent;
import robocode.control.events.BattleErrorEvent;
import robocode.control.events.BattleStartedEvent;
import robocode.control.events.RoundEndedEvent;
import robocode.control.events.RoundStartedEvent;
import robocode.control.events.TurnEndedEvent;
import robocode.control.snapshot.IBulletSnapshot;
import robocode.control.snapshot.IRobotSnapshot;
import robocode.control.snapshot.RobotState;
import robocode.control.snapshot.ITurnSnapshot;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Runs a robocode battle and prints the results, one line per robot:
 * "rank\trobotName\tscore\troundsWon". The rank robocode reports is not
 * reliable (it may be computed before the last round counts), use the rounds
 * won.
 *
 * Usage: BattleStarter -p robocodeDir -r rounds [-s] [-k] -o robot robot [robot...]
 *
 * With -k the battle is a knockout, the best of the rounds: it is run one round
 * at a time, the one who wins most of the rounds (rounds / 2 + 1) wins it and the
 * battle ends there. The winner of a round is the only robot alive at its end, a
 * double knockout has none. The scores of the rounds are added. If nobody
 * reaches that many (a double knockout, for instance), all the rounds are run and
 * the scores tell who is ahead.
 *
 * With -s the robocode window is not shown and every turn is printed (lines
 * starting with #) so the championship manager can show the battle in 3D:
 *
 * <pre>
 * #F width height rounds                  battlefield
 * #N index name                           a robot
 * #C index bodyColor gunColor radarColor  colors (ARGB), when they change
 * #R round                                round started
 * #T round turn|robots|bullets            a turn
 *    robot:  index x y bodyHeading gunHeading radarHeading energy state
 *    bullet: id owner x y power state victim
 *    (each separated by ";", the fields by spaces)
 * #E round turns                          round ended
 * </pre>
 *
 * Compile it with: javac --release 11 -cp "robocodeDir/libs/*" BattleStarter.java
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class BattleStarter {

	public static void main(String[] args) {
		String robocodePath = "";
		int rounds = 1;
		boolean stream = false;
		boolean knockout = false;
		List<String> robots = new ArrayList<String>();

		for (int index = 0; index < args.length; index++) {
			switch (args[index]) {
			case "-p":
				robocodePath = args[++index];
				break;
			case "-r":
				rounds = Integer.parseInt(args[++index]);
				break;
			case "-s":
				stream = true;
				break;
			case "-k":
				knockout = true;
				break;
			case "-o":
				// The robots come next
				break;
			default:
				robots.add(args[index]);
			}
		}

		// Robocode writes its logs to System.out, the turns go straight to the
		// output, buffered: there are thousands of them
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 1 << 16), false, StandardCharsets.UTF_8);

		RobocodeEngine.setLogMessagesEnabled(false);
		RobocodeEngine engine = new RobocodeEngine(new File(robocodePath));
		Observer observer = new Observer(out, stream);
		engine.addBattleListener(observer);
		engine.setVisible(!stream);

		RobotSpecification[] selected = engine.getLocalRepository(String.join(",", robots));
		if (knockout) {
			runKnockout(engine, observer, selected, rounds, out);
		} else {
			BattleSpecification battle = new BattleSpecification(rounds, new BattlefieldSpecification(800, 600), selected);
			engine.runBattle(battle, true);
		}
		engine.close();

		out.flush();
		System.exit(0);
	}

	/**
	 * A knockout: the rounds are run one by one until a robot has won most of them.
	 * Prints the results like a battle does, with the rounds won counted here.
	 */
	private static void runKnockout(RobocodeEngine engine, Observer observer, RobotSpecification[] selected, int rounds, PrintStream out) {
		int target = rounds / 2 + 1;
		Map<String, Integer> wins = new LinkedHashMap<String, Integer>();
		Map<String, Integer> scores = new LinkedHashMap<String, Integer>();
		Map<String, String> names = new LinkedHashMap<String, String>();
		observer.knockout(rounds);
		for (int played = 0; played < rounds; played++) {
			observer.nextRound(played);
			engine.runBattle(new BattleSpecification(1, new BattlefieldSpecification(800, 600), selected), true);
			for (BattleResults result : observer.results()) {
				String key = key(result.getTeamLeaderName());
				scores.merge(key, result.getScore(), Integer::sum);
				names.put(key, result.getTeamLeaderName());
				wins.putIfAbsent(key, 0);
			}
			String winner = observer.roundWinner();
			if (winner != null) {
				int total = wins.merge(winner, 1, Integer::sum);
				if (total >= target) {
					break;
				}
			}
		}
		// Most rounds won first, the score breaks the ties
		List<String> sorted = new ArrayList<String>(names.keySet());
		sorted.sort((first, second) -> wins.get(first).equals(wins.get(second)) ? Integer.compare(scores.get(second), scores.get(first)) : Integer.compare(wins.get(second), wins.get(first)));
		for (int index = 0; index < sorted.size(); index++) {
			String key = sorted.get(index);
			out.println((index + 1) + "\t" + names.get(key) + "\t" + scores.get(key) + "\t" + wins.get(key));
		}
		out.flush();
	}

	/** The name of a robot without the version, the "*" or the number of the instance */
	static String key(String name) {
		return name.trim().split("[ (]")[0].replaceAll("\\*$", "");
	}

	static class Observer extends BattleAdaptor {

		private final PrintStream out;
		private final boolean stream;
		private final Map<Integer, String> colors = new HashMap<Integer, String>();
		private boolean namesPrinted = false;
		/** In a knockout the rounds are battles of their own: they are numbered here, and the results are kept, not printed */
		private boolean knockout = false;
		private int totalRounds;
		private int roundOffset;
		private ITurnSnapshot lastTurn;
		private BattleResults[] results = new BattleResults[0];

		Observer(PrintStream out, boolean stream) {
			this.out = out;
			this.stream = stream;
		}

		void knockout(int totalRounds) {
			this.knockout = true;
			this.totalRounds = totalRounds;
		}

		void nextRound(int index) {
			this.roundOffset = index;
			this.lastTurn = null;
		}

		BattleResults[] results() {
			return this.results;
		}

		/** The robot that is the only one alive at the end of the round, or null */
		String roundWinner() {
			if (this.lastTurn == null) {
				return null;
			}
			String winner = null;
			for (IRobotSnapshot robot : this.lastTurn.getRobots()) {
				if (robot.getState() != RobotState.DEAD) {
					if (winner != null) {
						return null;
					}
					winner = key(robot.getName());
				}
			}
			return winner;
		}

		@Override
		public void onBattleStarted(BattleStartedEvent event) {
			// A knockout is many battles, the frame is told once
			if (this.stream && this.roundOffset == 0) {
				BattleRules rules = event.getBattleRules();
				this.out.println("#F " + rules.getBattlefieldWidth() + " " + rules.getBattlefieldHeight() + " " + (this.knockout ? this.totalRounds : rules.getNumRounds()));
			}
		}

		@Override
		public void onRoundStarted(RoundStartedEvent event) {
			if (!this.stream) {
				return;
			}
			ITurnSnapshot start = event.getStartSnapshot();
			if (!this.namesPrinted) {
				for (IRobotSnapshot robot : start.getRobots()) {
					this.out.println("#N " + robot.getContestantIndex() + " " + robot.getName());
				}
				this.namesPrinted = true;
			}
			this.out.println("#R " + (event.getRound() + this.roundOffset));
			this.printTurn(start);
		}

		@Override
		public void onTurnEnded(TurnEndedEvent event) {
			this.lastTurn = event.getTurnSnapshot();
			if (this.stream) {
				this.printTurn(event.getTurnSnapshot());
			}
		}

		@Override
		public void onRoundEnded(RoundEndedEvent event) {
			if (this.stream) {
				this.out.println("#E " + (event.getRound() + this.roundOffset) + " " + event.getTurns());
			}
		}

		private void printTurn(ITurnSnapshot turn) {
			StringBuilder line = new StringBuilder(256);

			for (IRobotSnapshot robot : turn.getRobots()) {
				String color = robot.getBodyColor() + " " + robot.getGunColor() + " " + robot.getRadarColor();
				if (!color.equals(this.colors.get(robot.getContestantIndex()))) {
					this.colors.put(robot.getContestantIndex(), color);
					this.out.println("#C " + robot.getContestantIndex() + " " + color);
				}
			}

			line.append("#T ").append(turn.getRound() + this.roundOffset).append(' ').append(turn.getTurn()).append('|');
			IRobotSnapshot[] robots = turn.getRobots();
			for (int index = 0; index < robots.length; index++) {
				IRobotSnapshot robot = robots[index];
				if (index > 0) {
					line.append(';');
				}
				line.append(robot.getContestantIndex()).append(' ');
				number(line, robot.getX(), 10).append(' ');
				number(line, robot.getY(), 10).append(' ');
				number(line, robot.getBodyHeading(), 1000).append(' ');
				number(line, robot.getGunHeading(), 1000).append(' ');
				number(line, robot.getRadarHeading(), 1000).append(' ');
				number(line, robot.getEnergy(), 10).append(' ');
				line.append(robot.getState().getValue());
			}

			line.append('|');
			boolean first = true;
			for (IBulletSnapshot bullet : turn.getBullets()) {
				// The explosions of dead robots are drawn by the viewer itself
				if (bullet.isExplosion()) {
					continue;
				}
				if (!first) {
					line.append(';');
				}
				first = false;
				line.append(bullet.getBulletId()).append(' ').append(bullet.getOwnerIndex()).append(' ');
				number(line, bullet.getPaintX(), 10).append(' ');
				number(line, bullet.getPaintY(), 10).append(' ');
				number(line, bullet.getPower(), 100).append(' ');
				line.append(bullet.getState().getValue()).append(' ').append(bullet.getVictimIndex());
			}
			this.out.println(line);
		}

		/**
		 * A number with few decimals, fast and without locale
		 */
		private static StringBuilder number(StringBuilder line, double value, int scale) {
			long scaled = Math.round(value * scale);
			if (scaled < 0) {
				line.append('-');
				scaled = -scaled;
			}
			line.append(scaled / scale);
			long fraction = scaled % scale;
			if (fraction != 0) {
				line.append('.');
				String digits = Long.toString(fraction + scale).substring(1);
				line.append(digits.replaceAll("0+$", ""));
			}
			return line;
		}

		@Override
		public void onBattleCompleted(BattleCompletedEvent event) {
			if (this.knockout) {
				this.results = event.getSortedResults();
				return;
			}
			for (BattleResults result : event.getSortedResults()) {
				this.out.println(result.getRank() + "\t" + result.getTeamLeaderName() + "\t" + result.getScore() + "\t" + result.getFirsts());
			}
			this.out.flush();
		}

		@Override
		public void onBattleError(BattleErrorEvent event) {
			this.out.println("Err> " + event.getError());
			this.out.flush();
		}
	}
}
