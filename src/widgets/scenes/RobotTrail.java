package widgets.scenes;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Where a robot has been in the last moments, for its fire: a flame is made of
 * the hot gas that leaves its spot and stays where it was left, while it goes up,
 * so the higher a part of the flame is, the older it is and the farther back on the
 * way of the robot it stands.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class RobotTrail {

	/** How many places are kept, the oldest ones are forgotten */
	private static final int CAPACITY = 96;
	/** A step this long is a jump (a new round, the replay) and not a movement */
	private static final double JUMP = 60;

	private final double[] times = new double[CAPACITY];
	private final double[] places = new double[2 * CAPACITY];
	private int count;
	/** The index of the newest place */
	private int newest;

	/** Forgets where the robot was. */
	void clear() {
		this.count = 0;
	}

	/**
	 * Keeps the place of the robot at a moment, the moments always growing. A jump of
	 * the robot makes it forget the way before it.
	 */
	void record(double time, double posX, double posZ) {
		if (this.count > 0 && Math.hypot(posX - this.places[2 * this.newest], posZ - this.places[2 * this.newest + 1]) >= JUMP) {
			this.count = 0;
		}
		this.newest = (this.newest + 1) % CAPACITY;
		this.times[this.newest] = time;
		this.places[2 * this.newest] = posX;
		this.places[2 * this.newest + 1] = posZ;
		this.count = Math.min(this.count + 1, CAPACITY);
	}

	/**
	 * Where the robot was at a moment, between the places kept: x and z. A moment older
	 * than all of them gives the oldest place.
	 *
	 * @return the place, or null when nothing is kept
	 */
	double[] placeAt(double time) {
		if (this.count == 0) {
			return null;
		}
		int index = this.newest;
		for (int step = 0; step < this.count; step++) {
			if (this.times[index] <= time) {
				if (step == 0) {
					return new double[] { this.places[2 * index], this.places[2 * index + 1] };
				}
				int later = (index + 1) % CAPACITY;
				double span = this.times[later] - this.times[index];
				double fraction = span > 1e-9 ? (time - this.times[index]) / span : 1;
				return new double[] { this.places[2 * index] + (this.places[2 * later] - this.places[2 * index]) * fraction,
						this.places[2 * index + 1] + (this.places[2 * later + 1] - this.places[2 * index + 1]) * fraction };
			}
			index = (index - 1 + CAPACITY) % CAPACITY;
		}
		int oldest = (this.newest - this.count + 1 + CAPACITY) % CAPACITY;
		return new double[] { this.places[2 * oldest], this.places[2 * oldest + 1] };
	}
}
