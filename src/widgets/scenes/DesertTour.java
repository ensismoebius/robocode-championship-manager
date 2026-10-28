package widgets.scenes;

import java.util.List;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The stops of the tour of the desert arena that show its city: the camera starts far behind a skyscraper, looking up at
 * it from above the palms, and then flies down an avenue toward the arena, low between the blocks, to come out over the dunes looking at the
 * courtyard.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DesertTour {

	static final double TOWER_SECONDS = 3;
	static final double AVENUE_SECONDS = 5;
	/** How far the camera is from the foot of the skyscraper when it looks up at it: its crown is in the frame */
	private static final double TOWER_DISTANCE = 850;
	/** The skyscrapers worth the stop are at least this far along the avenue, so the avenue is long enough and ends beyond the dunes */
	private static final double MIN_REACH = 1400;
	/** Where the avenue starts, beyond the foot of the skyscraper, and where it ends, beyond the dunes */
	private static final double AVENUE_START = 1000;
	private static final double AVENUE_END = 1250;

	/** Only static stops: there is nothing to build. */
	private DesertTour() {
	}

	/** The stops that show the city, given the skyscrapers of the desert: none when there is no skyscraper far enough. */
	static List<FlyTour.Stop> stops(List<double[]> towers) {
		double[] chosen = null;
		for (double[] tower : towers) {
			if (Math.abs(tower[1]) >= MIN_REACH && (chosen == null || Math.abs(tower[0]) < Math.abs(chosen[0]))) {
				chosen = tower;
			}
		}
		if (chosen == null) {
			return List.of();
		}
		// The avenue is the street beside the skyscraper on the side of the middle
		double street = chosen[0] > 0 ? chosen[0] - DesertCity.PITCH / 2 : chosen[0] + DesertCity.PITCH / 2;
		return List.of(tower(chosen), avenue(street, chosen[1]));
	}

	/** The camera beyond the skyscraper, looking up at it, circling a little. */
	private static FlyTour.Stop tower(double[] tower) {
		double outward = Math.atan2(tower[1], tower[0]);
		return new FlyTour.Stop(TOWER_SECONDS, 60, (share, eye, look) -> {
			double angle = outward + (share - 0.5) * 0.7;
			eye[0] = tower[0] + Math.cos(angle) * TOWER_DISTANCE;
			eye[1] = -340 - 120 * share;
			eye[2] = tower[1] + Math.sin(angle) * TOWER_DISTANCE;
			look[0] = tower[0];
			look[1] = -380;
			look[2] = tower[1];
		});
	}

	/** The flight down the street at the given x, from beyond the skyscraper at the given z toward the arena, rising at the end to see over the dunes. */
	private static FlyTour.Stop avenue(double street, double towerZ) {
		double inward = -Math.signum(towerZ);
		double fromZ = towerZ - inward * AVENUE_START;
		double toZ = Math.signum(towerZ) * AVENUE_END;
		return new FlyTour.Stop(AVENUE_SECONDS, 66, (share, eye, look) -> {
			double rise = share * share;
			eye[0] = street + 14 * Math.sin(share * 4);
			eye[1] = -60 - 250 * rise;
			eye[2] = fromZ + (toZ - fromZ) * share;
			// Looks down the avenue at first, and at the courtyard by the end
			look[0] = street * (1 - rise);
			look[1] = -100 + 50 * rise;
			look[2] = (eye[2] + inward * 500) * (1 - rise);
		});
	}
}
