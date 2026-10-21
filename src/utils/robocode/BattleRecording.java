package utils.robocode;

import java.util.ArrayList;
import java.util.List;

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
	 * A robot of the battle, its colors are ARGB
	 */
	public static final class Robot {
		private final int index;
		private final String name;
		private int bodyColor = 0xff2ec4ff;
		private int gunColor = 0xff2ec4ff;
		private int radarColor = 0xffffd400;

		Robot(int index, String name) {
			this.index = index;
			this.name = name;
		}

		public int getIndex() {
			return this.index;
		}

		/**
		 * As robocode reports it, like "team.Robot 1.0"
		 */
		public String getName() {
			return this.name;
		}

		public int getBodyColor() {
			return this.bodyColor;
		}

		public int getGunColor() {
			return this.gunColor;
		}

		public int getRadarColor() {
			return this.radarColor;
		}
	}

	public record RobotState(int index, double x, double y, double bodyHeading, double gunHeading, double radarHeading, double energy, int state) {
		public boolean isDead() {
			return this.state == ROBOT_DEAD;
		}
	}

	public record Bullet(int id, int owner, double x, double y, double power, int state, int victim) {
		/**
		 * The bullet hit something and explodes in this turn
		 */
		public boolean isHit() {
			return this.state == BULLET_HIT_VICTIM || this.state == BULLET_HIT_BULLET || this.state == BULLET_HIT_WALL;
		}
	}

	public record Turn(int round, int turn, List<RobotState> robots, List<Bullet> bullets) {
	}

	private int width = 800;
	private int height = 600;
	private final List<Robot> robots = new ArrayList<Robot>();
	private final List<List<Turn>> rounds = new ArrayList<List<Turn>>();

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public List<Robot> getRobots() {
		return this.robots;
	}

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
		case 'C' -> {
			String[] fields = body.split(" ");
			Robot robot = this.getRobot(Integer.parseInt(fields[0]));
			if (robot != null) {
				robot.bodyColor = Integer.parseInt(fields[1]);
				robot.gunColor = Integer.parseInt(fields[2]);
				robot.radarColor = Integer.parseInt(fields[3]);
			}
		}
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

	private void parseTurn(String body) {
		String[] parts = body.split("\\|", -1);
		String[] header = parts[0].split(" ");
		int round = Integer.parseInt(header[0]);
		int turn = Integer.parseInt(header[1]);

		List<RobotState> robotStates = new ArrayList<RobotState>();
		if (parts.length > 1 && !parts[1].isEmpty()) {
			for (String robot : parts[1].split(";")) {
				String[] string = robot.split(" ");
				robotStates.add(new RobotState(Integer.parseInt(string[0]), Double.parseDouble(string[1]), Double.parseDouble(string[2]), Double.parseDouble(string[3]), Double.parseDouble(string[4]),
						Double.parseDouble(string[5]), Double.parseDouble(string[6]), Integer.parseInt(string[7])));
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
