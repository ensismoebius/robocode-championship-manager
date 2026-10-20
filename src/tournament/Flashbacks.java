package tournament;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.BattleMoment;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Chooses the photos of the battles shown for a robot on the podium
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class Flashbacks {

	/**
	 * The most photos a robot has in its flashback
	 */
	public static final int LIMIT = 12;

	private Flashbacks() {
	}

	/**
	 * The photos of the battles a robot fought, in the order they were taken. Each
	 * battle keeps its photos: when there are more than the limit, each battle
	 * gives evenly spaced ones, so no battle of the robot is left out.
	 */
	public static List<BattleMoment> of(List<BattleMoment> moments, long robotId, int limit) {
		Map<String, List<BattleMoment>> battles = new LinkedHashMap<String, List<BattleMoment>>();
		for (BattleMoment moment : moments) {
			if (moment.getImage() != null && involves(moment, robotId)) {
				battles.computeIfAbsent(moment.getChampionshipId() + " " + moment.getMatchCode(), key -> new ArrayList<BattleMoment>()).add(moment);
			}
		}

		int quota = Math.max(1, limit / Math.max(1, battles.size()));
		List<BattleMoment> chosen = new ArrayList<BattleMoment>();
		for (List<BattleMoment> battle : battles.values()) {
			chosen.addAll(spread(battle, quota));
		}
		return chosen;
	}

	/**
	 * If the robot fought in the battle of the photo
	 */
	public static boolean involves(BattleMoment moment, long robotId) {
		return moment.getFirstId() == robotId || moment.getSecondId() == robotId;
	}

	/**
	 * The other robot of the battle of a photo, seen from one of its robots
	 */
	public static long opponentOf(BattleMoment moment, long robotId) {
		return moment.getFirstId() == robotId ? moment.getSecondId() : moment.getFirstId();
	}

	/**
	 * At most count items, evenly spaced and in order, the last one included when
	 * only one is taken (the end of a battle is usually the decisive moment)
	 */
	static <T> List<T> spread(List<T> items, int count) {
		List<T> chosen = new ArrayList<T>();
		if (items.size() <= count) {
			chosen.addAll(items);
		} else if (count == 1) {
			chosen.add(items.get(items.size() - 1));
		} else {
			for (int i = 0; i < count; i++) {
				chosen.add(items.get((int) Math.round(i * (items.size() - 1) / (double) (count - 1))));
			}
		}
		return chosen;
	}
}
