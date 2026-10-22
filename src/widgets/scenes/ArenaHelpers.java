package widgets.scenes;

import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;
import static widgets.scenes.ArenaHelpers.find;
import static widgets.scenes.ArenaHelpers.findBullet;
import static widgets.scenes.ArenaHelpers.lerp;
import static widgets.scenes.ArenaHelpers.lerpAngle;
import static widgets.scenes.ArenaHelpers.pose;
import static widgets.scenes.ArenaHelpers.solidMap;
import static widgets.scenes.ArenaHelpers.toCss;

/**
 * Small helpers of the arena: the colors and the pictures of the arena, the pose of a robot in a turn, and the interpolation of the camera
 */
final class ArenaHelpers {

	/** The CSS colour of a colour, as #rrggbb. */
	static String toCss(Color color) {
		return String.format("#%02x%02x%02x", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
	}

	/**
	 * Where a robot is drawn at a moment (in turns, with the fraction) and its
	 * heading, as showTurn places it
	 */
	static double[] pose(List<Turn> turns, int robot, double time) {
		int index = Math.max(0, Math.min((int) Math.floor(time), turns.size() - 1));
		double fraction = Math.max(0, Math.min(1, time - index));
		RobotState state = find(turns.get(index).robots(), robot, null);
		if (state == null) {
			return null;
		}
		RobotState after = find(turns.get(Math.min(index + 1, turns.size() - 1)).robots(), robot, state);
		return new double[] { lerp(state.x(), after.x(), fraction), lerp(state.y(), after.y(), fraction), lerpAngle(state.bodyHeading(), after.bodyHeading(), fraction) };
	}

	/** The bullet with the given identifier in a turn, or null when there is none. */
	static Bullet findBullet(Turn turn, int identifier) {
		for (Bullet bullet : turn.bullets()) {
			if (bullet.id() == identifier) {
				return bullet;
			}
		}
		return null;
	}

	/** The state of the robot with the given index, or the fallback when it is not in the list. */
	static RobotState find(List<RobotState> states, int index, RobotState fallback) {
		for (RobotState state : states) {
			if (state.index() == index) {
				return state;
			}
		}
		return fallback;
	}

	/**
	 * A small picture of one color, for materials that shine by themselves
	 */
	static WritableImage solidMap(Color color) {
		WritableImage map = new WritableImage(2, 2);
		for (int posY = 0; posY < 2; posY++) {
			for (int posX = 0; posX < 2; posX++) {
				map.getPixelWriter().setColor(posX, posY, color);
			}
		}
		return map;
	}

	/** Linear interpolation between two values. */
	static double lerp(double first, double second, double time) {
		return first + (second - first) * time;
	}

	/**
	 * Interpolates angles (radians) the short way
	 */
	static double lerpAngle(double first, double second, double time) {
		double difference = Math.IEEEremainder(second - first, Math.PI * 2);
		return first + difference * time;
	}
}
