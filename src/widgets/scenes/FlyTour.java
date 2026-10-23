package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The tour at the start of the fly by: a list of stops where the camera stays for a while, each looking at
 * something, before the trip to where the battle begins. In most arenas there is one stop, on a critter. In the
 * Eden Solar arena the tour goes first to the baobab, a symbol of Africa, then to three critters, and ends with a
 * view over the whole region.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class FlyTour {

	/** Where the camera is and where it looks at a share of a stop, from 0 to 1 */
	@FunctionalInterface
	interface Pose {
		void at(double share, double[] eye, double[] look);
	}

	/** One stop of the tour: how long it lasts, the field of view of the camera, and where the camera is. */
	record Stop(double seconds, double fov, Pose pose) {
	}

	private static final double CRITTER_SECONDS = 2;
	private static final double BAOBAB_SECONDS = 3;
	private static final double OVERVIEW_SECONDS = 2.5;
	private static final int CRITTERS = 3;

	private final List<Stop> stops = new ArrayList<>();
	/** Where the camera is and looks now, and its field of view: what the tour last gave. */
	final double[] eye = new double[3];
	final double[] look = new double[3];
	double fov = 62;

	/** Whether the tour has no stop, so the fly by goes straight to its trip. */
	boolean isEmpty() {
		return this.stops.isEmpty();
	}

	/** How long the whole tour takes, in seconds. */
	double duration() {
		return this.stops.stream().mapToDouble(Stop::seconds).sum();
	}

	/** Plans the tour of the arena of the host: the sides it looks from are the ones given, 1 or -1 on each axis. */
	void plan(BattleArena3D host, double mirrorX, double mirrorZ) {
		this.stops.clear();
		if (host.style == ArenaStyle.EDEN_SOLAR) {
			double[] tree = host.eden.baobab((int) mirrorX);
			this.stops.add(baobab(tree));
			if (host.critters != null) {
				// The birds of that baobab: some perched on it, some flying around it
				for (Supplier<double[]> source : host.critters.trackNear(tree, CRITTERS)) {
					this.stops.add(bird(source, tree));
				}
			}
			this.stops.add(overview(mirrorX, mirrorZ));
		} else if (host.critters != null) {
			Supplier<double[]> source = host.critters.track(-0.8 * host.fieldWidth * mirrorX, -host.fieldHeight * mirrorZ);
			if (source != null) {
				this.stops.add(critter(source));
			}
		}
	}

	/** Puts the camera where the tour is at that time, in seconds from its start; at its end it stays on the last stop. */
	void update(double time) {
		double start = 0;
		for (int index = 0; index < this.stops.size(); index++) {
			Stop stop = this.stops.get(index);
			if (time < start + stop.seconds() || index == this.stops.size() - 1) {
				stop.pose().at(Math.max(0, Math.min(1, (time - start) / stop.seconds())), this.eye, this.look);
				this.fov = stop.fov();
				return;
			}
			start += stop.seconds();
		}
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

	/** A stop high above one corner, looking over the whole stadium and its garden, turning slowly. */
	private static Stop overview(double mirrorX, double mirrorZ) {
		return new Stop(OVERVIEW_SECONDS, 64, (share, eye, look) -> {
			double turn = (share - 0.5) * 0.35;
			double startX = -2200 * mirrorX;
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
