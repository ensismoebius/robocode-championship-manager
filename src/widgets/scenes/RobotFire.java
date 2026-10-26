package widgets.scenes;

import static widgets.scenes.BattleArena3D.BURN_ENERGY;
import static widgets.scenes.BattleArena3D.BURN_SPOTS;
import widgets.scenes.ArenaFire.Particle;
import widgets.scenes.ArenaFire.ParticleKind;

/**
 * The fire of a robot that is burning: the flames, fixed to their spots of the robot, with their bodies trailing along its way, and the smoke, the embers and the sparks that stay where they are born
 */
final class RobotFire {

	/** The share of the bits that come off the fire that are embers, which rise, and not sparks, which fall */
	private static final double EMBER_SHARE = 0.65;

	/**
	 * Below half of the energy the robot catches fire. The less energy, the more
	 * burning spots, the stronger the flames and the thicker the smoke.
	 *
	 * @param deltaTime
	 *            seconds, slower in the replay
	 */
	static void burn(RobotView view, double deltaTime) {
		double damage = view.dead ? 0 : Math.max(0, Math.min(1, (BURN_ENERGY - view.energy) / BURN_ENERGY));
		if (damage <= 0 || !view.root.isVisible()) {
			view.flameDebt = view.smokeDebt = view.sparkDebt = 0;
			view.lastX = Double.NaN;
			view.trail.clear();
			return;
		}
		// The base of the flames goes with the robot, the smoke stays behind as a trail
		double posX = view.root.getTranslateX();
		double posZ = view.root.getTranslateZ();
		trackSpeed(view, deltaTime, posX, posZ);
		// Where the robot has been, for its flames to trail along
		view.fireClock += deltaTime;
		view.trail.record(view.fireClock, posX, posZ);
		view.lastX = posX;
		view.lastZ = posZ;

		int spots = damage < 0.3 ? 1 : damage < 0.6 ? 2 : 3;
		double heading = Math.toRadians(view.bodyRotate.getAngle());
		double cos = Math.cos(heading);
		double sin = Math.sin(heading);

		view.flameDebt += deltaTime * (9 + 14 * damage);
		view.smokeDebt += deltaTime * (5 + 16 * damage);
		view.sparkDebt += deltaTime * (3 + 10 * damage);
		while (view.flameDebt >= 1 || view.smokeDebt >= 1 || view.sparkDebt >= 1) {
			burnPart(view, posX, posZ, spots, damage, cos, sin);
		}
	}

	/** The speed of the robot, eased over the frames, from how far it moved since the last one */
	private static void trackSpeed(RobotView view, double deltaTime, double posX, double posZ) {
		if (deltaTime > 0) {
			double sxValue = Double.isNaN(view.lastX) ? 0 : (posX - view.lastX) / deltaTime;
			double szValue = Double.isNaN(view.lastX) ? 0 : (posZ - view.lastZ) / deltaTime;
			// A jump (new round, replay) is not a movement
			if (Math.hypot(sxValue, szValue) * deltaTime > 60) {
				sxValue = szValue = 0;
			}
			double ease = 1 - Math.exp(-12 * deltaTime);
			view.speedX += (sxValue - view.speedX) * ease;
			view.speedZ += (szValue - view.speedZ) * ease;
		}
	}

	/** One piece of the fire: a flame, smoke or a spark, born on a burning spot of the hull and left there */
	private static void burnPart(RobotView view, double posX, double posZ, int spots, double damage, double cos, double sin) {
		// A burning spot of the hull, turning with the body
		double[] spot = BURN_SPOTS[view.host.random.nextInt(spots)];
		double lxValue = spot[0] + (view.host.random.nextDouble() - 0.5) * 6;
		double lzValue = spot[2] + (view.host.random.nextDouble() - 0.5) * 6;
		double wxValue = posX + lxValue * cos + lzValue * sin;
		double wzValue = posZ - lxValue * sin + lzValue * cos;
		if (view.flameDebt >= 1) {
			view.flameDebt--;
			// The base of a flame is fixed to its spot of the robot, so its position is relative to it; its body trails along the way of the robot
			Particle flame = view.host.fire.spawn(ParticleKind.FLAME, wxValue - posX, spot[1], wzValue - posZ, damage);
			flame.anchor = view;
		} else if (view.smokeDebt >= 1) {
			view.smokeDebt--;
			view.host.fire.spawn(ParticleKind.SMOKE, wxValue, spot[1] - 6, wzValue, damage).inherit(view.speedX * 0.15, view.speedZ * 0.15);
		} else {
			view.sparkDebt--;
			if (view.host.random.nextDouble() < EMBER_SHARE) {
				// An ember: it comes off the fire and rises on the hot air, drifting, and goes out
				Particle ember = view.host.fire.spawn(ParticleKind.EMBER, wxValue, spot[1] - 6, wzValue, damage);
				ember.life = 0.9 + view.host.random.nextDouble() * 0.9;
			} else {
				view.host.fire.spawn(ParticleKind.SPARK, wxValue, spot[1], wzValue, damage).inherit(view.speedX * 0.3, view.speedZ * 0.3);
			}
		}
	}
}
