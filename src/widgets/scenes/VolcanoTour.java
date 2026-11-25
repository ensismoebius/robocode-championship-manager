package widgets.scenes;

import java.util.List;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The stop of the tour of the volcano arena that shows a cave of the highlands: the camera glides over the lake toward its mouth,
 * where the fungi glow and the bats circle. The cave is one the camera reaches from the sweep, and leaves for the field, without
 * going through a volcano.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class VolcanoTour {

	static final double CAVE_SECONDS = 3.5;
	/** How far in front of the mouth the camera starts, and how much nearer it comes */
	private static final double START = 430;
	private static final double APPROACH = 150;
	/** How far the way of the camera passes from the foot of a volcano, at the least */
	private static final double CLEAR = 80;

	/** Only static stops: there is nothing to build. */
	private VolcanoTour() {
	}

	/**
	 * The stop on a cave, coming from where the camera is ({x, y, z}) and going on to the field afterward; null when every way would
	 * pass through a volcano, or there is no cave.
	 */
	static FlyTour.Stop cave(List<VolcanoHighlands.Cave> caves, List<VolcanoHighlands.Peak> peaks, double[] from) {
		VolcanoHighlands.Cave chosen = null;
		double best = CLEAR;
		for (VolcanoHighlands.Cave cave : caves) {
			double[] first = eye(cave, 0);
			double[] last = eye(cave, 1);
			double clear = Math.min(clearance(from, first, peaks), clearance(last, new double[] { 0, 0, 0 }, peaks));
			if (clear > best) {
				best = clear;
				chosen = cave;
			}
		}
		if (chosen == null) {
			return null;
		}
		VolcanoHighlands.Cave cave = chosen;
		double[] mouth = VolcanoHighlands.mouth(cave);
		return new FlyTour.Stop(CAVE_SECONDS, 58, (share, eye, look) -> {
			System.arraycopy(eye(cave, share), 0, eye, 0, 3);
			System.arraycopy(mouth, 0, look, 0, 3);
		});
	}

	/** Where the camera is, so far along the stop: in front of the mouth, a little above it, coming nearer and lower, drifting aside. */
	static double[] eye(VolcanoHighlands.Cave cave, double share) {
		VolcanoHighlands.Cave drifted = new VolcanoHighlands.Cave(cave.angle() + (share - 0.5) * 0.05, cave.radius(), cave.width(), cave.height());
		return drifted.at(0, VolcanoHighlands.LEDGE_HEIGHT + cave.height() * 0.55 + 70 + 40 * (1 - share), cave.radius() - START + APPROACH * share);
	}

	/** How far the straight way between two points passes from the foot of the nearest volcano, seen from above. */
	private static double clearance(double[] from, double[] to, List<VolcanoHighlands.Peak> peaks) {
		double least = Double.POSITIVE_INFINITY;
		double alongX = to[0] - from[0];
		double alongZ = to[2] - from[2];
		double length2 = Math.max(1e-9, alongX * alongX + alongZ * alongZ);
		for (VolcanoHighlands.Peak peak : peaks) {
			double share = Math.max(0, Math.min(1, ((peak.posX() - from[0]) * alongX + (peak.posZ() - from[2]) * alongZ) / length2));
			double apart = Math.hypot(from[0] + alongX * share - peak.posX(), from[2] + alongZ * share - peak.posZ());
			// The falls of lava go a little beyond the foot of the cone
			least = Math.min(least, apart - peak.radius() * 1.3);
		}
		return least;
	}
}
