package utils.robocode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Every turn of a battle, as printed by RobocodeStarter/BattleStarter with -s,
 * so it can be replayed in 3D. Robocode coordinates: origin at the bottom left
 * corner, headings in radians, 0 is north and they grow clockwise.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class BattleRecording {

	public static final int ROBOT_DEAD = 3;
	public static final int ROBOT_HIT_WALL = 1;
	public static final int ROBOT_HIT_ROBOT = 2;

	public static final int BULLET_HIT_VICTIM = 2;
	public static final int BULLET_HIT_BULLET = 3;
	public static final int BULLET_HIT_WALL = 4;

	/**
	 * A robot of the battle, its colors are ARGB: the first ones it showed, the
	 * colors of each turn are in its {@link RobotState}
	 */
	public static final class Robot {
		private final int index;
		private final String name;
		private int bodyColor = 0xff2ec4ff;
		private int gunColor = 0xff2ec4ff;
		private int radarColor = 0xffffd400;
		/** Whether the battle already reported its colors */
		private boolean colored;

		/** A robot of the battle, with its index and name. */
		Robot(int index, String name) {
			this.index = index;
			this.name = name;
		}

		/** A robot with its name and its colors, to show its model outside a battle. */
		public static Robot named(String name, int bodyColor, int gunColor, int radarColor) {
			Robot robot = new Robot(0, name);
			robot.bodyColor = bodyColor;
			robot.gunColor = gunColor;
			robot.radarColor = radarColor;
			return robot;
		}

		/** Returns the index. */
		public int getIndex() {
			return this.index;
		}

		/**
		 * As robocode reports it, like "team.Robot 1.0"
		 */
		public String getName() {
			return this.name;
		}

		/** Returns the body color. */
		public int getBodyColor() {
			return this.bodyColor;
		}

		/** Returns the gun color. */
		public int getGunColor() {
			return this.gunColor;
		}

		/** Returns the radar color. */
		public int getRadarColor() {
			return this.radarColor;
		}
	}

	/**
	 * The state of a robot in a turn: its position, its heading, its energy and
	 * its colors (ARGB), which a robot can change at any time
	 */
	public record RobotState(int index, double x, double y, double bodyHeading, double gunHeading, double radarHeading, double energy, int state, int bodyColor, int gunColor, int radarColor) {

		/** A state without colors (0): the robot shows its default ones. */
		public RobotState(int index, double x, double y, double bodyHeading, double gunHeading, double radarHeading, double energy, int state) {
			this(index, x, y, bodyHeading, gunHeading, radarHeading, energy, state, 0, 0, 0);
		}

		/** Tells whether the robot is dead. */
		public boolean isDead() {
			return this.state == ROBOT_DEAD;
		}
	}

	/** A bullet in a turn: its owner, its position, its power and its state. */
	public record Bullet(int id, int owner, double x, double y, double power, int state, int victim) {
		/**
		 * The bullet hit something and explodes in this turn
		 */
		public boolean isHit() {
			return this.state == BULLET_HIT_VICTIM || this.state == BULLET_HIT_BULLET || this.state == BULLET_HIT_WALL;
		}
	}

	/** The robots and the bullets of a turn of a round. */
	public record Turn(int round, int turn, List<RobotState> robots, List<Bullet> bullets) {
	}

	private int width = 800;
	private int height = 600;
	private final List<Robot> robots = new ArrayList<Robot>();
	private final List<List<Turn>> rounds = new ArrayList<List<Turn>>();
	/** The colors each robot has now, while reading: body, gun and radar */
	private final Map<Integer, int[]> colors = new HashMap<Integer, int[]>();

	/** Returns the width. */
	public int getWidth() {
		return this.width;
	}

	/** Returns the height. */
	public int getHeight() {
		return this.height;
	}

	/** Returns the robots. */
	public List<Robot> getRobots() {
		return this.robots;
	}

	/** The robot with the given index, or null when there is none. */
	public Robot getRobot(int index) {
		for (Robot robot : this.robots) {
			if (robot.index == index) {
				return robot;
			}
		}
		return null;
	}

	/**
	 * The turns of each round
	 */
	public List<List<Turn>> getRounds() {
		return this.rounds;
	}

	/** Tells whether the recording has no rounds with turns. */
	public boolean isEmpty() {
		return this.rounds.stream().allMatch(List::isEmpty);
	}

	/**
	 * Reads the lines starting with # of the BattleStarter output, the others are
	 * ignored
	 *
	 * @return the recording, empty if the battle was not streamed
	 */
	public static BattleRecording parse(List<String> output) {
		BattleRecording recording = new BattleRecording();
		for (String line : output) {
			if (line.length() < 3 || line.charAt(0) != '#') {
				continue;
			}
			try {
				recording.parseLine(line);
			} catch (RuntimeException runtimeException) {
				// A broken line costs a turn, not the battle
				System.err.println("Linha da batalha ignorada: " + line);
			}
		}
		return recording;
	}

	/** Reads a line of the recording: a frame of the battle, a round or a turn. */
	private void parseLine(String line) {
		String body = line.substring(3);
		switch (line.charAt(1)) {
		case 'F' -> {
			String[] fields = body.split(" ");
			this.width = Integer.parseInt(fields[0]);
			this.height = Integer.parseInt(fields[1]);
		}
		case 'N' -> {
			int space = body.indexOf(' ');
			this.robots.add(new Robot(Integer.parseInt(body.substring(0, space)), body.substring(space + 1)));
		}
		case 'C' -> this.parseColors(body);
		case 'R' -> {
			int round = Integer.parseInt(body.trim());
			while (this.rounds.size() <= round) {
				this.rounds.add(new ArrayList<Turn>());
			}
		}
		case 'T' -> this.parseTurn(body);
		default -> {
		}
		}
	}

	/** Reads a change of the colors of a robot: they hold from the next turn on; the first ones are the colors of the robot. */
	private void parseColors(String body) {
		String[] fields = body.split(" ");
		int index = Integer.parseInt(fields[0]);
		int[] color = { Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), Integer.parseInt(fields[3]) };
		this.colors.put(index, color);
		Robot robot = this.getRobot(index);
		if (robot != null && !robot.colored) {
			robot.colored = true;
			robot.bodyColor = color[0];
			robot.gunColor = color[1];
			robot.radarColor = color[2];
		}
	}

	/** The colors the robot has now: the last ones reported, or its own when there were none */
	private int[] colorsOf(int index) {
		int[] color = this.colors.get(index);
		if (color != null) {
			return color;
		}
		Robot robot = this.getRobot(index);
		return robot == null ? new int[3] : new int[] { robot.bodyColor, robot.gunColor, robot.radarColor };
	}

	/** Reads the robots and the bullets of a turn of the recording. */
	private void parseTurn(String body) {
		String[] parts = body.split("\\|", -1);
		String[] header = parts[0].split(" ");
		int round = Integer.parseInt(header[0]);
		int turn = Integer.parseInt(header[1]);

		List<RobotState> robotStates = new ArrayList<RobotState>();
		if (parts.length > 1 && !parts[1].isEmpty()) {
			for (String robot : parts[1].split(";")) {
				String[] string = robot.split(" ");
				int index = Integer.parseInt(string[0]);
				int[] color = this.colorsOf(index);
				robotStates.add(new RobotState(index, Double.parseDouble(string[1]), Double.parseDouble(string[2]), Double.parseDouble(string[3]), Double.parseDouble(string[4]), Double.parseDouble(string[5]),
						Double.parseDouble(string[6]), Integer.parseInt(string[7]), color[0], color[1], color[2]));
			}
		}

		List<Bullet> bullets = new ArrayList<Bullet>();
		if (parts.length > 2 && !parts[2].isEmpty()) {
			for (String bullet : parts[2].split(";")) {
				String[] string2 = bullet.split(" ");
				bullets.add(new Bullet(Integer.parseInt(string2[0]), Integer.parseInt(string2[1]), Double.parseDouble(string2[2]), Double.parseDouble(string2[3]), Double.parseDouble(string2[4]),
						Integer.parseInt(string2[5]), Integer.parseInt(string2[6])));
			}
		}

		while (this.rounds.size() <= round) {
			this.rounds.add(new ArrayList<Turn>());
		}
		this.rounds.get(round).add(new Turn(round, turn, robotStates, bullets));
	}
}
