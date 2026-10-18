import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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
import robocode.control.snapshot.ITurnSnapshot;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Runs a robocode battle and prints the results, one line per robot:
 * "rank\trobotName\tscore\troundsWon". The rank robocode reports is not
 * reliable (it may be computed before the last round counts), use the rounds
 * won.
 *
 * Usage: BattleStarter -p robocodeDir -r rounds [-s] -o robot robot [robot...]
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
		List<String> robots = new ArrayList<String>();

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-p":
				robocodePath = args[++i];
				break;
			case "-r":
				rounds = Integer.parseInt(args[++i]);
				break;
			case "-s":
				stream = true;
				break;
			case "-o":
				// The robots come next
				break;
			default:
				robots.add(args[i]);
			}
		}

		// Robocode writes its logs to System.out, the turns go straight to the
		// output, buffered: there are thousands of them
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 1 << 16), false, StandardCharsets.UTF_8);

		RobocodeEngine.setLogMessagesEnabled(false);
		RobocodeEngine engine = new RobocodeEngine(new File(robocodePath));
		engine.addBattleListener(new Observer(out, stream));
		engine.setVisible(!stream);

		RobotSpecification[] selected = engine.getLocalRepository(String.join(",", robots));
		BattleSpecification battle = new BattleSpecification(rounds, new BattlefieldSpecification(800, 600), selected);
		engine.runBattle(battle, true);
		engine.close();

		out.flush();
		System.exit(0);
	}

	static class Observer extends BattleAdaptor {

		private final PrintStream out;
		private final boolean stream;
		private final Map<Integer, String> colors = new HashMap<Integer, String>();
		private boolean namesPrinted = false;

		Observer(PrintStream out, boolean stream) {
			this.out = out;
			this.stream = stream;
		}

		@Override
		public void onBattleStarted(BattleStartedEvent event) {
			if (this.stream) {
				BattleRules rules = event.getBattleRules();
				this.out.println("#F " + rules.getBattlefieldWidth() + " " + rules.getBattlefieldHeight() + " " + rules.getNumRounds());
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
			this.out.println("#R " + event.getRound());
			this.printTurn(start);
		}

		@Override
		public void onTurnEnded(TurnEndedEvent event) {
			if (this.stream) {
				this.printTurn(event.getTurnSnapshot());
			}
		}

		@Override
		public void onRoundEnded(RoundEndedEvent event) {
			if (this.stream) {
				this.out.println("#E " + event.getRound() + " " + event.getTurns());
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

			line.append("#T ").append(turn.getRound()).append(' ').append(turn.getTurn()).append('|');
			IRobotSnapshot[] robots = turn.getRobots();
			for (int i = 0; i < robots.length; i++) {
				IRobotSnapshot robot = robots[i];
				if (i > 0) {
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
