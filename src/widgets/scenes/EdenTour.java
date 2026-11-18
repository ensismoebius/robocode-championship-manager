package widgets.scenes;

import java.util.List;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The stops of the tour of the Eden Solar arena that show its city: the camera rises out of the stadium through the opening of its
 * roof, circles one of the great baobab towers, looking up at its crown, and then flies down an avenue toward the stadium, low
 * between the towers, rising over the canal to look at the wall of the stadium. It is kept clear of the buildings.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenTour {

	static final double TOWER_SECONDS = 3.5;
	static final double AVENUE_SECONDS = 5;
	static final double RISE_SECONDS = 2;
	/** How far the camera is from the great baobab, and how much of a circle it goes round. */
	private static final double TOWER_DISTANCE = 760;
	private static final double TOWER_TURN = 0.9;

	/** Only static stops: there is nothing to build. */
	private EdenTour() {
	}

	/** The stops that show the city, for the corner the tour looks from: none when the city has no great baobab. */
	static List<FlyTour.Stop> stops(EdenCity city, double mirrorX, double mirrorZ) {
		if (city == null || city.grandBaobabs().isEmpty()) {
			return List.of();
		}
		double corner = Math.atan2(-mirrorZ, mirrorX);
		double[] chosen = null;
		for (double[] baobab : city.grandBaobabs()) {
			double away = Math.abs(Math.atan2(Math.sin(Math.atan2(baobab[1], baobab[0]) - corner), Math.cos(Math.atan2(baobab[1], baobab[0]) - corner)));
			if (chosen == null || away < Math.abs(Math.atan2(Math.sin(Math.atan2(chosen[1], chosen[0]) - corner), Math.cos(Math.atan2(chosen[1], chosen[0]) - corner)))) {
				chosen = baobab;
			}
		}
		return List.of(rise(chosen), FlyTour.cleared(tower(chosen), city), avenue(city, EdenCity.nearestAvenue(Math.atan2(chosen[1], chosen[0]) + Math.PI / 12)));
	}

	/**
	 * The camera rises out of the stadium through the opening of its roof, high over the garden, turning toward the great baobab: so
	 * the way from the birds inside to the city outside goes up and over the roof, not through it.
	 */
	private static FlyTour.Stop rise(double[] baobab) {
		double toward = Math.atan2(baobab[1], baobab[0]);
		return new FlyTour.Stop(RISE_SECONDS, 62, (share, eye, look) -> {
			eye[0] = Math.cos(toward) * 250 * share;
			eye[1] = -1150 - 250 * share;
			eye[2] = Math.sin(toward) * 250 * share;
			look[0] = baobab[0];
			look[1] = -baobab[2] * 0.6;
			look[2] = baobab[1];
		});
	}

	/** The camera round the baobab, looking up at its crown, rising a little. */
	private static FlyTour.Stop tower(double[] baobab) {
		return FlyTour.circling(TOWER_SECONDS, 62, baobab, TOWER_DISTANCE, TOWER_TURN, -180, -140, -baobab[2] * 0.72);
	}

	/** The flight down the avenue at the angle, from the last ring toward the stadium, rising at the end to look at its wall over the canal. */
	private static FlyTour.Stop avenue(EdenCity city, double angle) {
		double[] reach = city.avenueReach();
		return new FlyTour.Stop(AVENUE_SECONDS, 66, (share, eye, look) -> {
			double rise = share * share;
			double radius = reach[1] - 150 - (reach[1] - 150 - (reach[0] + 90)) * share;
			double[] here = EdenStadium.point(angle, radius, -60 - 230 * rise);
			double[] ahead = EdenStadium.point(angle, radius - 700, -110 - 60 * rise);
			double[] wall = EdenStadium.point(angle, reach[0] - 20, -330);
			eye[0] = here[0];
			eye[1] = here[1];
			eye[2] = here[2];
			look[0] = ahead[0] + (wall[0] - ahead[0]) * rise;
			look[1] = ahead[1] + (wall[1] - ahead[1]) * rise;
			look[2] = ahead[2] + (wall[2] - ahead[2]) * rise;
		});
	}
}
