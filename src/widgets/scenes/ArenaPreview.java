package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.paint.Color;
import model.Opponent;
import utils.robocode.BattleControl;
import utils.robocode.BattleRecording;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The robots of a battle before it is fought: their models are made from the fighters, in the colors of their players, so
 * they can be shown while the battle is simulated. When the battle is recorded the models become the ones of its robots, with
 * the colors the robots chose.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaPreview {

	/** The colors each robot was seen with in a battle of this session, by its full name: body, gun and radar. A robot keeps its colors from a battle to the next. */
	private static final Map<String, int[]> KNOWN = new ConcurrentHashMap<String, int[]>();

	private final List<BattleRecording.Robot> robots = new ArrayList<BattleRecording.Robot>();

	private final BattleArena3D host;

	/** The previews of the robots of the arena. */
	ArenaPreview(BattleArena3D host) {
		this.host = host;
	}

	/** The robots as they are presented before the battle is recorded, in the order of the fighters. */
	List<BattleRecording.Robot> robots() {
		return this.robots;
	}

	/** Makes the models of the fighters, in the colors of their players, and puts them in the arena, hidden. */
	void build() {
		this.robots.clear();
		for (int order = 0; order < host.fighters.size(); order++) {
			Opponent fighter = host.fighters.get(order);
			Color tint = BattleArena3D.PLAYER_COLORS[order % BattleArena3D.PLAYER_COLORS.length];
			int argb = 0xff000000 | (int) Math.round(tint.getRed() * 255) << 16 | (int) Math.round(tint.getGreen() * 255) << 8 | (int) Math.round(tint.getBlue() * 255);
			int[] known = KNOWN.get(fighter.getFullName());
			BattleRecording.Robot robot = known == null ? BattleRecording.Robot.named(order, fighter.getFullName(), argb, argb, 0xffffd400) : BattleRecording.Robot.named(order, fighter.getFullName(), known[0], known[1], known[2]);
			RobotView view = new RobotView(host, robot, fighter, order);
			view.root.setVisible(false);
			host.robots.put(order, view);
			host.robotLayer.getChildren().add(view.root);
			host.balloons.getChildren().add(view.balloon);
			this.robots.add(robot);
		}
	}

	/**
	 * The battle tells the colors of a robot while it is fought, named as robocode reports it: the model of the robot takes them at once,
	 * and they are remembered for the next battles of the robot.
	 */
	void paint(String robotName, int body, int gun, int radar) {
		String name = BattleControl.robotClassName(robotName);
		for (RobotView view : host.robots.values()) {
			if (view.opponent != null && view.opponent.getFullName().equals(name)) {
				view.setColors(body, gun, radar);
				KNOWN.put(name, new int[] { body, gun, radar });
			}
		}
	}

	/** Forgets the colors the robots were seen with. */
	static void forget() {
		KNOWN.clear();
	}

	/**
	 * The battle is fought and recorded: the models that were presented are the ones of its robots, with the colors they chose. A
	 * model that is not the robot of its place in the recording is replaced by one that is.
	 */
	void adopt(BattleRecording recording) {
		List<RobotView> shown = new ArrayList<RobotView>();
		for (int order = 0; order < this.robots.size(); order++) {
			shown.add(host.robots.get(order));
		}
		host.robots.clear();
		int order = 0;
		for (BattleRecording.Robot robot : recording.getRobots()) {
			Opponent opponent = host.opponentOf(robot);
			RobotView view = order < shown.size() && shown.get(order) != null && shown.get(order).opponent == opponent ? shown.get(order) : null;
			if (view == null) {
				view = new RobotView(host, robot, opponent, order);
				view.root.setVisible(false);
				host.robotLayer.getChildren().add(view.root);
				host.balloons.getChildren().add(view.balloon);
			} else {
				view.setColors(robot.getBodyColor(), robot.getGunColor(), robot.getRadarColor());
			}
			if (opponent != null && robot.isColored()) {
				KNOWN.put(opponent.getFullName(), new int[] { robot.getBodyColor(), robot.getGunColor(), robot.getRadarColor() });
			}
			host.robots.put(robot.getIndex(), view);
			host.roundWins.put(robot.getIndex(), 0);
			order++;
		}
		for (RobotView old : shown) {
			if (old != null && !host.robots.containsValue(old)) {
				host.robotLayer.getChildren().remove(old.root);
				host.balloons.getChildren().remove(old.balloon);
			}
		}
		this.followPresentation(recording);
	}

	/** If a robot is being presented, it is the one of the recording at its place that is presented from now on, and no other is on the screen. */
	private void followPresentation(BattleRecording recording) {
		if (host.phase == Phase.SHOWCASE && !host.versus && host.showcaseOrder < recording.getRobots().size()) {
			host.shotSubject = recording.getRobots().get(host.showcaseOrder).getIndex();
			for (RobotView view : host.robots.values()) {
				view.root.setVisible(view == host.robots.get(host.shotSubject));
			}
		}
	}
}
