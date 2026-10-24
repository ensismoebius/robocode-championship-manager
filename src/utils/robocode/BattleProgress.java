package utils.robocode;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * How far Robocode is in a battle it simulates, read from the lines it prints:
 * a round is over when a robot has lost all its health, so the progress of a
 * round is how much health the weakest robot has lost, and the progress of the
 * battle is the rounds over plus the one that is running, out of all of them.
 * It never goes back, a robot that is healed does not undo what was done.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class BattleProgress {

	/** The energy every robot starts a round with */
	static final double FULL_HEALTH = 100;

	private int rounds = 1;
	private int round;
	private double inRound;
	private double done;

	/**
	 * Takes a line printed by the battle: "#F" tells the rounds, "#T" the health of
	 * the robots in a turn, anything else is ignored.
	 *
	 * @return the progress now, from 0 to 1
	 */
	public double accept(String line) {
		if (line.startsWith("#F ")) {
			this.rounds = Math.max(1, this.rounds(line.substring(3).split(" ")));
		} else if (line.startsWith("#T ")) {
			this.turn(line.substring(3));
		}
		return this.done;
	}

	/** The battle is over: all of it is done */
	public double finish() {
		this.done = 1;
		return this.done;
	}

	/** The rounds of "width height rounds", or the ones already known when the line is not that */
	private int rounds(String[] fields) {
		try {
			return fields.length > 2 ? Integer.parseInt(fields[2].trim()) : this.rounds;
		} catch (NumberFormatException notANumber) {
			return this.rounds;
		}
	}

	/** Reads "round turn|robot;robot|bullets", a robot being "index x y body gun radar energy state" */
	private void turn(String body) {
		String[] parts = body.split("\\|", -1);
		if (parts.length < 2 || parts[1].isEmpty()) {
			return;
		}
		try {
			int current = Integer.parseInt(parts[0].split(" ")[0]);
			if (current != this.round) {
				this.round = current;
				this.inRound = 0;
			}
			double weakest = FULL_HEALTH;
			for (String robot : parts[1].split(";")) {
				weakest = Math.min(weakest, Double.parseDouble(robot.split(" ")[6]));
			}
			this.inRound = Math.max(this.inRound, 1 - Math.max(0, weakest) / FULL_HEALTH);
			this.done = Math.max(this.done, Math.min(1, (this.round + this.inRound) / this.rounds));
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException notATurn) {
			// A line that is not a turn tells nothing
		}
	}
}
