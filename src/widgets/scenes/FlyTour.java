package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The tour at the start of the fly by: a list of stops where the camera stays for a while, each looking at
 * something, before the trip to where the battle begins. In most arenas there are two stops: a sweep around the arena
 * from above, and a critter. The desert goes first to its city, to a skyscraper and down an avenue. In the Eden Solar
 * arena the tour goes first to the baobab, a symbol of Africa, then to three critters, then up through the opening of the roof and
 * out to the city: a great baobab tower and an avenue, and ends with a view over the whole region. There the camera keeps clear of
 * the buildings and of the roof: on the way between two stops it rises over them.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class FlyTour {

	/**
	 * Where the camera is and where it looks at a share of a stop, from 0 to 1. While one stop gives way to the next the
	 * share goes a little under 0 and over 1, so a pose goes on in the same way a little after its end and a little before its start.
	 */
	@FunctionalInterface
	interface Pose {
		void at(double share, double[] eye, double[] look);
	}

	/** One stop of the tour: how long it lasts, the field of view of the camera, and where the camera is. */
	record Stop(double seconds, double fov, Pose pose) {
	}

	/** What the camera must stay above in an arena with scenery it could go into: the buildings of a city, the wall and the roof of a stadium. */
	interface Clearance {
		/**
		 * The lowest the camera may be over the spot {x, z}, as a height (negative up), or positive infinity where it is free; when the way
		 * crosses the wall of the stadium, the wall and the roof count too.
		 */
		double floor(double posX, double posZ, boolean crossing);

		/** Whether the spot {x, z} is inside the stadium. */
		boolean inside(double posX, double posZ);
	}

	private static final double CRITTER_SECONDS = 2;
	private static final double BAOBAB_SECONDS = 3;
	private static final double OVERVIEW_SECONDS = 2.5;
	private static final double PANORAMA_SECONDS = 4.5;
	/** How far beyond the fence the sweep goes, and how far in the jungle, where the huge trees stand closer and the camera would be in their leaves */
	private static final double PANORAMA_REACH = 1000;
	private static final double JUNGLE_REACH = 250;
	private static final int CRITTERS = 3;

	/**
	 * From one stop to the next the camera does not cut: the two poses are crossfaded. It lasts as long as the distance takes at
	 * this many units for each second (at least and at most this many seconds), so the camera never rushes, and the stops that are
	 * too short for the crossfades around them are made longer.
	 */
	private static final double CROSSFADE_UNITS = 700;
	private static final double CROSSFADE_MIN = 1.2;
	private static final double CROSSFADE_MAX = 5;
	/**
	 * The fastest the camera goes in a crossfade, in units for each second: a long way, or a high rise over the scenery, makes the
	 * crossfade longer than the most above, so the camera soars and does not jump
	 */
	private static final double CROSSFADE_SPEED = 1600;
	/** While it crossfades the camera rises over the way between the two poses, by this share of the distance at most, so it does not go through the scenery */
	private static final double LIFT_SHARE = 0.15;
	private static final double LIFT_MAX = 450;
	/** The most the camera is raised to clear the scenery on the way between two stops, and how many points of the way are checked */
	private static final double CLEAR_MAX = 3000;
	private static final int CLEAR_SAMPLES = 32;

	private final List<Stop> stops = new ArrayList<>();
	/** How long the crossfade lasts from each stop to the next, the last one to the first */
	private double[] fades = new double[0];
	/** How high the camera has to rise, at the middle of the way, to clear the scenery between each stop and the next */
	private double[] lifts = new double[0];
	private Clearance clearance;
	private final double[] otherEye = new double[3];
	private final double[] otherLook = new double[3];
	/** Where the camera is and looks now, and its field of view: what the tour last gave. */
	final double[] eye = new double[3];
	final double[] look = new double[3];
	double fov = 62;

	/** Whether the tour has no stop, so the fly by goes straight to its trip. */
	boolean isEmpty() {
		return this.stops.isEmpty();
	}

	/** When a pass of the tour ends on its last stop, in seconds from its start: just before the last stop gives way to the first, when it goes around. */
	double passEnd() {
		return this.duration() - (this.fades.length == 0 ? 0 : this.fades[this.fades.length - 1] / 2);
	}

	/** How long the whole tour takes, in seconds. */
	double duration() {
		return this.stops.stream().mapToDouble(Stop::seconds).sum();
	}

	/** Plans the tour of the arena of the host: the sides it looks from are the ones given, 1 or -1 on each axis. */
	void plan(BattleArena3D host, double mirrorX, double mirrorZ) {
		this.stops.clear();
		this.clearance = host.style == ArenaStyle.EDEN_SOLAR && host.eden.city() != null ? host.eden : null;
		if (host.style == ArenaStyle.EDEN_SOLAR) {
			double[] tree = host.eden.baobab((int) mirrorX);
			this.stops.add(baobab(tree));
			if (host.critters != null) {
				// The birds of that baobab: some perched on it, some flying around it
				for (Supplier<double[]> source : host.critters.trackNear(tree, CRITTERS)) {
					this.stops.add(bird(source, tree));
				}
			}
			// The city outside the stadium: a great baobab and an avenue, before the view from above
			this.stops.addAll(EdenTour.stops(host.eden.city(), mirrorX, mirrorZ));
			this.stops.add(overview(mirrorX, mirrorZ));
		} else {
			Stop sweep = panorama(host.fieldWidth, host.fieldHeight, host.style == ArenaStyle.JUNGLE ? JUNGLE_REACH : PANORAMA_REACH, mirrorX, mirrorZ);
			if (host.style == ArenaStyle.MIDDLE_EAST) {
				// The city, then the arena, so the camera comes from the avenue to the critter by the courtyard and the tour that goes around does not have a long way to go back
				this.stops.addAll(DesertTour.stops(host.desert.towers()));
				this.critterStop(host, mirrorX, mirrorZ);
				this.stops.add(sweep);
			} else {
				this.stops.add(sweep);
				if (host.style == ArenaStyle.VOLCANO) {
					// A cave of the highlands, where the bats live, before the critter by the field
					double[] eye = new double[3];
					sweep.pose().at(1, eye, new double[3]);
					Stop cave = VolcanoTour.cave(host.volcano.highlands().caves(), host.volcano.highlands().peaks(), eye);
					if (cave != null) {
						this.stops.add(cave);
					}
				}
				this.critterStop(host, mirrorX, mirrorZ);
			}
		}
		this.pace();
	}

	/** Works out the crossfades from the distance between the stops, and makes longer the stops that are too short to have them around. */
	private void pace() {
		int count = this.stops.size();
		this.fades = new double[count];
		this.lifts = new double[count];
		for (int index = 0; index < count && count > 1; index++) {
			this.lifts[index] = this.liftBetween(this.stops.get(index), this.stops.get((index + 1) % count));
			this.fades[index] = this.fadeBetween(this.stops.get(index), this.stops.get((index + 1) % count), this.lifts[index]);
		}
		for (int index = 0; count > 1 && index < count; index++) {
			Stop stop = this.stops.get(index);
			double needed = (this.fades[(index + count - 1) % count] + this.fades[index]) / 2;
			if (needed > stop.seconds()) {
				this.stops.set(index, new Stop(needed, stop.fov(), stop.pose()));
			}
		}
	}

	/** How long the crossfade takes from the end of a stop to the start of the next, by how far the camera, or what it looks at, has to go, up and over what is in the way too. */
	private double fadeBetween(Stop from, Stop next, double lift) {
		from.pose().at(1, this.eye, this.look);
		next.pose().at(0, this.otherEye, this.otherLook);
		double far = Math.max(Math.max(distance(this.eye, this.otherEye), distance(this.look, this.otherLook)), 2 * lift);
		// The fastest moment of the crossfade is its middle, where the ease is steepest (1.5 times the average) and the rise too
		double rise = Math.max(Math.min(LIFT_MAX, LIFT_SHARE * Math.hypot(this.eye[0] - this.otherEye[0], this.eye[2] - this.otherEye[2])), lift);
		double fastest = 1.5 * Math.max(Math.hypot(distance(this.eye, this.otherEye), Math.PI * rise), distance(this.look, this.otherLook));
		return Math.max(Math.max(CROSSFADE_MIN, Math.min(CROSSFADE_MAX, far / CROSSFADE_UNITS)), fastest / CROSSFADE_SPEED);
	}

	/**
	 * How high the camera has to rise, at the middle of the way from the end of a stop to the start of the next, so that on the
	 * whole way, raised the way a crossfade raises it, it is over the scenery: none where the arena has nothing to clear.
	 */
	private double liftBetween(Stop from, Stop next) {
		if (this.clearance == null) {
			return 0;
		}
		from.pose().at(1, this.eye, this.look);
		next.pose().at(0, this.otherEye, this.otherLook);
		boolean crossing = this.clearance.inside(this.eye[0], this.eye[2]) != this.clearance.inside(this.otherEye[0], this.otherEye[2]);
		double needed = 0;
		for (int sample = 1; sample < CLEAR_SAMPLES; sample++) {
			double mix = (double) sample / CLEAR_SAMPLES;
			double floor = this.clearance.floor(this.eye[0] + (this.otherEye[0] - this.eye[0]) * mix, this.eye[2] + (this.otherEye[2] - this.eye[2]) * mix, crossing);
			double height = this.eye[1] + (this.otherEye[1] - this.eye[1]) * mix;
			if (height > floor) {
				needed = Math.max(needed, (height - floor) / Math.sin(Math.PI * mix));
			}
		}
		return Math.min(CLEAR_MAX, needed);
	}

	/**
	 * The stop with the camera raised, all the time by the same height, as much as it needs to be over the scenery on the whole stop:
	 * the same stop where nothing is in the way.
	 */
	static Stop cleared(Stop stop, Clearance clearance) {
		double[] eye = new double[3];
		double[] look = new double[3];
		double raise = 0;
		for (int sample = 0; sample <= CLEAR_SAMPLES; sample++) {
			stop.pose().at((double) sample / CLEAR_SAMPLES, eye, look);
			raise = Math.max(raise, eye[1] - clearance.floor(eye[0], eye[2], false));
		}
		if (raise <= 0) {
			return stop;
		}
		double lift = Math.min(CLEAR_MAX, raise);
		return new Stop(stop.seconds(), stop.fov(), (share, at, sight) -> {
			stop.pose().at(share, at, sight);
			at[1] -= lift;
		});
	}

	/** The distance between two points. */
	private static double distance(double[] first, double[] second) {
		return Math.sqrt(ArenaMaterials.distance2(first, second));
	}

	/** The stop on a critter of the arena, when it has one. */
	private void critterStop(BattleArena3D host, double mirrorX, double mirrorZ) {
		if (host.critters != null) {
			Supplier<double[]> source = host.critters.track(-0.8 * host.fieldWidth * mirrorX, -host.fieldHeight * mirrorZ);
			if (source != null) {
				this.stops.add(critter(source));
			}
		}
	}

	/** Puts the camera where the tour is at that time, in seconds from its start; at its end it stays on the last stop. */
	void update(double time) {
		this.update(time, false);
	}

	/**
	 * Puts the camera where the tour is at that time, in seconds from its start. Between stops the poses are crossfaded, so
	 * the way is smooth. At its end the camera stays on the last stop, or, when the tour goes around, the last stop gives way to the first.
	 */
	void update(double time, boolean cyclic) {
		double length = this.duration();
		int last = this.stops.size() - 1;
		double fade = this.crossfade(last);
		if (cyclic && last > 0 && time >= length - fade / 2) {
			double inside = time % length;
			if (inside >= length - fade / 2) {
				this.blend(last, 0, inside, inside - length, (inside - (length - fade / 2)) / fade);
				return;
			}
			if (inside < fade / 2) {
				this.blend(last, 0, inside + length, inside, (inside + fade / 2) / fade);
				return;
			}
			this.plain(inside);
			return;
		}
		this.plain(time);
	}

	/** The camera at that time of the tour, where it is not between two stops of a crossfade, or in one when it is. */
	private void plain(double time) {
		double start = 0;
		int last = this.stops.size() - 1;
		for (int index = 0; index <= last; index++) {
			Stop stop = this.stops.get(index);
			double end = start + stop.seconds();
			double fade = index < last ? this.crossfade(index) : 0;
			if (index < last && time >= end - fade / 2 && time < end + fade / 2) {
				this.blend(index, index + 1, time, time, (time - (end - fade / 2)) / fade);
				return;
			}
			if (time < end || index == last) {
				stop.pose().at(Math.max(0, Math.min(1, (time - start) / stop.seconds())), this.eye, this.look);
				this.fov = stop.fov();
				return;
			}
			start = end;
		}
	}

	/** How long the crossfade lasts from a stop to the next one, the first after the last one: it is around the end of the stop. */
	private double crossfade(int index) {
		return this.fades[index];
	}

	/** The time at which a stop starts, in seconds from the start of the tour. */
	private double startOf(int index) {
		double start = 0;
		for (int before = 0; before < index; before++) {
			start += this.stops.get(before).seconds();
		}
		return start;
	}

	/** The camera between two stops: the pose of each one at its own time, mixed by the weight, from 0 to 1, with its ends eased, and raised on the way. */
	private void blend(int from, int next, double timeFrom, double timeTo, double weight) {
		Stop first = this.stops.get(from);
		Stop second = this.stops.get(next);
		first.pose().at((timeFrom - this.startOf(from)) / first.seconds(), this.eye, this.look);
		second.pose().at((timeTo - this.startOf(next)) / second.seconds(), this.otherEye, this.otherLook);
		double mix = weight * weight * (3 - 2 * weight);
		double rise = Math.max(Math.min(LIFT_MAX, LIFT_SHARE * Math.hypot(this.eye[0] - this.otherEye[0], this.eye[2] - this.otherEye[2])), this.lifts[from]);
		double lift = rise * Math.sin(Math.PI * mix);
		for (int axis = 0; axis < 3; axis++) {
			this.eye[axis] += (this.otherEye[axis] - this.eye[axis]) * mix;
			this.look[axis] += (this.otherLook[axis] - this.look[axis]) * mix;
		}
		this.eye[1] -= lift;
		this.fov = first.fov() + (second.fov() - first.fov()) * mix;
	}

	/** A stop on a critter, following it: the camera is close to it on its outer side, a little above. */
	private static Stop critter(Supplier<double[]> source) {
		return new Stop(CRITTER_SECONDS, 62, (share, eye, look) -> {
			double[] spot = source.get();
			double length = Math.max(1, Math.hypot(spot[0], spot[2]));
			eye[0] = spot[0] + spot[0] / length * 45;
			eye[1] = spot[1] - 22;
			eye[2] = spot[2] + spot[2] / length * 45;
			System.arraycopy(spot, 0, look, 0, 3);
		});
	}

	/**
	 * A stop on a bird of a baobab, following it. A bird perched on the crown, where the layers of leaves are, is seen
	 * from above them, close, on the side away from the tree. A bird flying below the crown is chased from behind and
	 * a little above, so the camera is never inside the leaves and the bird is always in front of it.
	 */
	private static Stop bird(Supplier<double[]> source, double[] tree) {
		double[] last = new double[3];
		double[] way = new double[2];
		return new Stop(CRITTER_SECONDS, 58, (share, eye, look) -> {
			double[] spot = source.get();
			double away = Math.atan2(spot[2] - tree[2], spot[0] - tree[0]);
			double moved = Math.hypot(spot[0] - last[0], spot[2] - last[2]);
			if (moved > 0.2 && moved < 50) {
				way[0] = (spot[0] - last[0]) / moved;
				way[1] = (spot[2] - last[2]) / moved;
			} else if (way[0] == 0 && way[1] == 0) {
				// Not seen moving yet: the birds circle the tree, the way the angle grows
				way[0] = -Math.sin(away);
				way[1] = Math.cos(away);
			}
			System.arraycopy(spot, 0, last, 0, 3);
			if (spot[1] < -520) {
				eye[0] = spot[0] + Math.cos(away) * 70;
				eye[1] = -700;
				eye[2] = spot[2] + Math.sin(away) * 70;
			} else {
				eye[0] = spot[0] - way[0] * 90;
				eye[1] = spot[1] - 25;
				eye[2] = spot[2] - way[1] * 90;
			}
			System.arraycopy(spot, 0, look, 0, 3);
		});
	}

	/** A stop on the baobab: the camera is on the side of the field, a little above the ground, and circles slowly. */
	private static Stop baobab(double[] tree) {
		double toward = Math.atan2(-tree[2], -tree[0]);
		return new Stop(BAOBAB_SECONDS, 62, (share, eye, look) -> {
			double angle = toward + (share - 0.5) * 0.9;
			eye[0] = tree[0] + Math.cos(angle) * 640;
			eye[1] = -150 - 60 * share;
			eye[2] = tree[2] + Math.sin(angle) * 640;
			look[0] = tree[0];
			look[1] = -320;
			look[2] = tree[2];
		});
	}

	/**
	 * A stop that circles a spot {x, z} on the ground, from the side that faces away from the middle of the arena and through a part of a
	 * turn, rising a little, looking at the given height: the tower of a city.
	 */
	static Stop circling(double seconds, double fov, double[] spot, double distance, double turn, double eyeHeight, double eyeRise, double lookHeight) {
		double outward = Math.atan2(spot[1], spot[0]);
		return new Stop(seconds, fov, (share, eye, look) -> {
			double angle = outward + (share - 0.5) * turn;
			eye[0] = spot[0] + Math.cos(angle) * distance;
			eye[1] = eyeHeight + eyeRise * share;
			eye[2] = spot[1] + Math.sin(angle) * distance;
			look[0] = spot[0];
			look[1] = lookHeight;
			look[2] = spot[1];
		});
	}

	/** A stop that sweeps around the arena from above, from the corner it looks from, coming down slowly: the scenery beyond the fence goes by. */
	private static Stop panorama(double width, double height, double reach, double mirrorX, double mirrorZ) {
		double start = Math.atan2(-mirrorZ * height, -mirrorX * width);
		double radius = Math.hypot(width, height) / 2 + reach;
		return new Stop(PANORAMA_SECONDS, 64, (share, eye, look) -> {
			double angle = start + (share - 0.5) * 1.3 * mirrorX * mirrorZ;
			eye[0] = Math.cos(angle) * radius;
			eye[1] = -950 + 350 * share;
			eye[2] = Math.sin(angle) * radius;
			look[0] = 0;
			look[1] = -60;
			look[2] = 0;
		});
	}

	/** A stop high above one corner, on the side of the baobab the tour begins at, looking over the whole stadium and its garden, turning slowly. */
	private static Stop overview(double mirrorX, double mirrorZ) {
		return new Stop(OVERVIEW_SECONDS, 64, (share, eye, look) -> {
			double turn = (share - 0.5) * 0.35;
			double startX = 2200 * mirrorX;
			double startZ = -2600 * mirrorZ;
			eye[0] = startX * Math.cos(turn) - startZ * Math.sin(turn);
			eye[1] = -2400 + 150 * share;
			eye[2] = startX * Math.sin(turn) + startZ * Math.cos(turn);
			look[0] = 0;
			look[1] = -150;
			look[2] = 0;
		});
	}
}
