package widgets.scenes;

import static widgets.scenes.BattleArena3D.PHOTO_DESTRUCTION_DELAY;
import static widgets.scenes.BattleArena3D.PHOTO_HIT_DELAY;
import static widgets.scenes.BattleArena3D.PHOTO_HIT_POWER;
import static widgets.scenes.ArenaHelpers.findBullet;
import static widgets.scenes.ArenaHelpers.pose;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;
import utils.robocode.BattleRecording;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The shots of the battle arena: the bullets that fly from the guns of the
 * robots, the hits and the impacts they make, the flashes of the force field
 * they cross, and the startled critters around them.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaBullets {

	/**
	 * Robocode hits with a square of 36 that does not turn with the robot, the
	 * tank is narrower (and longer): a bullet that grazes a corner of the square
	 * would pass by the side of the tank. The point of the hit is brought well
	 * inside the hull (13.5 to each side, 22 to the front and to the back), so
	 * the bolt always goes into it.
	 */
	private static final double HULL_SIDE = 8;
	private static final double HULL_AHEAD = 15;
	/** The height of the gun, where the bolts leave */
	private static final double BARREL_HEIGHT = -17.2;
	/**
	 * The height of the middle of the hull (from -7 to -13.5): the tank is low,
	 * at the height of the gun a bolt would pass over it
	 */
	private static final double HULL_HEIGHT = -10.5;
	/** How far the end of the gun is from the center of the tank */
	private static final double MUZZLE = 44;
	private final Map<Integer, Flight> flights = new HashMap<Integer, Flight>();

	private final BattleArena3D host;

	/** Creates the bullets of the arena, drawn in the host scene. */
	ArenaBullets(BattleArena3D host) {
		this.host = host;
	}

	/** The path of a bullet in the arena: where it starts, where it goes and when it lands. */
	record Flight(double sx, double sy, double ux, double uy, double total, double born, double duration, Bullet hit, double side, double ahead) {
	}

	/**
	 * The records only say where a bullet is at the end of each turn, one step
	 * after it left the center of the shooter, and the last one is the point of
	 * the hit (that follows the victim). So the flight is made from the whole
	 * path: the bolt comes out of the muzzle of the gun and goes straight to the
	 * point of the hit, always at the same pace, and explodes when it gets there.
	 */
	Flight flight(List<Turn> turns, int index, Bullet seen) {
		Flight known = this.flights.get(seen.id());
		if (known != null) {
			return known;
		}
		Flight flight = this.newFlight(turns, this.path(turns, index, seen));
		// A bullet with no flight is never drawn: it was born hitting
		this.flights.put(seen.id(), flight == null ? new Flight(0, 0, 0, 0, 0, 0, 0, null, 0, 0) : flight);
		return this.flights.get(seen.id());
	}

	/** Where a bullet is in the turns: its first and last records, the hit if it has one, and the last turn it is seen in */
	record Path(int first, Bullet start, Bullet last, Bullet previous, Bullet hit, int end) {
	}

	/** Follows a bullet through the turns, from the first one it is seen in until it hits or is no longer recorded */
	Path path(List<Turn> turns, int index, Bullet seen) {
		int first = index;
		while (first > 0 && findBullet(turns.get(first - 1), seen.id()) != null) {
			first--;
		}
		Bullet start = findBullet(turns.get(first), seen.id());
		if (start == null) {
			first = index + 1;
			start = seen;
		}
		Bullet last = start;
		Bullet previous = start;
		Bullet hit = null;
		int end = first;
		for (int deepIndex = first + 1; deepIndex < turns.size(); deepIndex++) {
			Bullet now = findBullet(turns.get(deepIndex), seen.id());
			if (now == null) {
				break;
			}
			end = deepIndex;
			if (now.isHit()) {
				hit = now;
				last = now;
				break;
			}
			previous = now;
			last = now;
		}
		return new Path(first, start, last, previous, hit, end);
	}

	/** The flight of a bullet along its path, or null when the path has no length or the bullet was born hitting */
	Flight newFlight(List<Turn> turns, Path path) {
		Bullet start = path.start();
		double speed = 20 - 3 * start.power();
		double deltaX = path.last().x() - start.x();
		double deltaY = path.last().y() - start.y();
		double length = Math.hypot(deltaX, deltaY);
		if (length <= 1e-6 || start.isHit()) {
			return null;
		}
		deltaX /= length;
		deltaY /= length;
		double born = path.first() - 1;
		Bullet hit = path.hit();
		// Gets there when the last step is done
		double duration = hit != null ? path.end() - 1 - born + Math.hypot(hit.x() - path.previous().x(), hit.y() - path.previous().y()) / speed : path.end() - born;
		duration = Math.max(0.01, duration);
		double sxValue = start.x() - deltaX * speed;
		double syValue = start.y() - deltaY * speed;
		double[] aim = this.aim(turns, path, born + duration);
		double total = Math.hypot(aim[2] - sxValue, aim[3] - syValue);
		if (total <= 1e-6) {
			return null;
		}
		return new Flight(sxValue, syValue, (aim[2] - sxValue) / total, (aim[3] - syValue) / total, total, born, duration, hit, aim[0], aim[1]);
	}

	/**
	 * Where the bolt ends, as {side, ahead, x, y}. When it hits a tank, that is the point of the hit
	 * in the frame of the tank (across and ahead), drawn where the hull is when the bolt gets there:
	 * the bolt flies straight to it. Otherwise it is the end of the path.
	 */
	private double[] aim(List<Turn> turns, Path path, double when) {
		Bullet hit = path.hit();
		double exValue = path.last().x();
		double eyValue = path.last().y();
		double side = 0;
		double ahead = 0;
		if (hit != null && hit.state() == BattleRecording.BULLET_HIT_VICTIM) {
			RobotState victim = ArenaHelpers.find(turns.get(path.end()).robots(), hit.victim(), null);
			double[] pose = pose(turns, hit.victim(), when);
			if (victim != null && pose != null) {
				double oxValue = hit.x() - victim.x();
				double oyValue = hit.y() - victim.y();
				double heading = victim.bodyHeading();
				side = Math.max(-HULL_SIDE, Math.min(HULL_SIDE, oxValue * Math.cos(heading) - oyValue * Math.sin(heading)));
				ahead = Math.max(-HULL_AHEAD, Math.min(HULL_AHEAD, oxValue * Math.sin(heading) + oyValue * Math.cos(heading)));
				exValue = pose[0] + side * Math.cos(pose[2]) + ahead * Math.sin(pose[2]);
				eyValue = pose[1] - side * Math.sin(pose[2]) + ahead * Math.cos(pose[2]);
			}
		}
		return new double[] { side, ahead, exValue, eyValue };
	}

	/** Shows the bullets of a turn, placed between this turn and the next one by the fraction of the turn. */
	void showBullets(List<Turn> turns, int index, double fraction) {
		Turn current = turns.get(index);
		Turn next = turns.get(Math.min(index + 1, turns.size() - 1));
		Set<Integer> flying = new HashSet<Integer>();
		for (Turn turn : next == current ? List.of(current) : List.of(current, next)) {
			for (Bullet bullet : turn.bullets()) {
				if (bullet.isHit() || bullet.state() > BattleRecording.BULLET_HIT_WALL || flying.contains(bullet.id())) {
					continue;
				}
				this.showBullet(turns, turn == current ? index : index + 1, bullet, flying);
			}
		}
		host.bullets.entrySet().removeIf(entry -> {
			if (!flying.contains(entry.getKey())) {
				host.bulletLayer.getChildren().remove(entry.getValue());
				return true;
			}
			return false;
		});
	}

	/** One bullet at the moment of the battle: it is still on the way, it hits, or it is over */
	private void showBullet(List<Turn> turns, int flightIndex, Bullet bullet, Set<Integer> flying) {
		Flight flight = this.flight(turns, flightIndex, bullet);
		if (flight.total() <= 0) {
			return;
		}
		double progress = (host.turnPosition - flight.born()) / flight.duration();
		if (progress < 0) {
			return;
		}
		if (progress >= 1) {
			if (flight.hit() != null) {
				double[] target = this.target(flight);
				this.bulletHit(flight.hit(), target[0], target[1]);
				return;
			}
			progress = 1;
		}
		// From the muzzle to the end, the speed is the same all the way
		double distance = flight.total() > MUZZLE ? MUZZLE + (flight.total() - MUZZLE) * progress : flight.total() * progress;
		double posX = flight.sx() + flight.ux() * distance;
		double posY = flight.sy() + flight.uy() * distance;
		double height = this.heightOf(flight, distance, progress);
		this.muzzle(bullet, flight, distance);
		this.placeBullet(bullet, flying, posX, posY, flight.ux(), flight.uy(), height);
		// The bolt comes out of the barrel: its tail is never behind the muzzle
		Group shot = host.bullets.get(bullet.id());
		double flown = Math.max(0, distance - MUZZLE) + 4;
		((Scale) shot.getTransforms().get(1)).setZ(Math.max(0.05, Math.min(1, flown / boltLength(bullet.power()))));
	}

	/** The shot, when the bolt is still at the muzzle: the sound and the flash come out of the gun */
	private void muzzle(Bullet bullet, Flight flight, double distance) {
		if (distance >= MUZZLE) {
			this.bulletFired(bullet, flight.sx() + flight.ux() * MUZZLE, flight.sy() + flight.uy() * MUZZLE);
		} else if (host.bulletsSeen.contains(bullet.id()) == false) {
			this.bulletFired(bullet, flight.sx() + flight.ux() * Math.min(MUZZLE, flight.total()), flight.sy() + flight.uy() * Math.min(MUZZLE, flight.total()));
		}
	}

	/**
	 * The height of the bolt: the ones that hit a tank come down, little by little, from the height
	 * of the gun to the middle of the hull
	 */
	private double heightOf(Flight flight, double distance, double progress) {
		double height = BARREL_HEIGHT;
		if (flight.hit() != null && flight.hit().state() == BattleRecording.BULLET_HIT_VICTIM) {
			double along = flight.total() > MUZZLE ? (distance - MUZZLE) / (flight.total() - MUZZLE) : progress;
			height += (HULL_HEIGHT - BARREL_HEIGHT) * Math.max(0, Math.min(1, along));
		}
		return height;
	}

	/** Places a bullet at the given position, turned to the direction it flies. */
	void placeBullet(Bullet bullet, Set<Integer> flying, double posX, double posY, double deltaX, double deltaY, double height) {
		flying.add(bullet.id());
		Group shot = host.bullets.computeIfAbsent(bullet.id(), identifier -> this.newBullet(bullet));
		shot.setTranslateX(host.worldX(posX));
		shot.setTranslateZ(host.worldZ(posY));
		shot.setTranslateY(height);
		// The bolt points to where it goes, turned around its head
		((Rotate) shot.getTransforms().get(0)).setAngle(Math.toDegrees(Math.atan2(deltaX, deltaY)));
	}

	/**
	 * Where the flight ends now: the point of the hit on the victim as it is drawn
	 * at this moment, or the end of the path
	 */
	double[] target(Flight flight) {
		Bullet hit = flight.hit();
		RobotView victim = hit == null || hit.state() != BattleRecording.BULLET_HIT_VICTIM ? null : host.robots.get(hit.victim());
		if (victim == null) {
			return new double[] { flight.sx() + flight.ux() * flight.total(), flight.sy() + flight.uy() * flight.total() };
		}
		double posX = victim.root.getTranslateX() + host.fieldWidth / 2;
		double posY = victim.root.getTranslateZ() + host.fieldHeight / 2;
		double heading = Math.toRadians(victim.bodyRotate.getAngle());
		return new double[] { posX + flight.side() * Math.cos(heading) + flight.ahead() * Math.sin(heading), posY - flight.side() * Math.sin(heading) + flight.ahead() * Math.cos(heading) };
	}

	/** Something happens in the battle at this world point: the critters nearby react to it */
	void startle(double worldX, double worldZ, double radius, double strength) {
		if (host.critters != null) {
			host.critters.alarm(worldX, worldZ, radius, strength);
		}
	}

	/**
	 * The shot, once: the sound and the flash at the gun
	 */
	void bulletFired(Bullet bullet, double posX, double posY) {
		if (host.bulletsSeen.add(bullet.id())) {
			host.audio.shotAt(bullet.power(), host.worldX(posX), host.worldZ(posY));
			host.fx.muzzleFlash(host.worldX(posX), host.worldZ(posY), this.shooterColor(bullet));
			RobotView shooter = host.robots.get(bullet.owner());
			if (shooter != null) {
				shooter.recoil(bullet.power());
			}
			this.startle(host.worldX(posX), host.worldZ(posY), 150, 0.4);
		}
	}

	/**
	 * The hit, once: the explosion where the bullet stopped and what the victim
	 * feels
	 */
	void bulletHit(Bullet bullet, double posX, double posY) {
		if (!host.bulletsExploded.add(bullet.id())) {
			return;
		}
		host.fx.explosion(host.worldX(posX), host.worldZ(posY), 8 + bullet.power() * 6, this.shooterColor(bullet));
		this.startle(host.worldX(posX), host.worldZ(posY), 220, 0.7);
		RobotView victim = host.robots.get(bullet.victim());
		if (bullet.state() == BattleRecording.BULLET_HIT_VICTIM && victim != null) {
			victim.flash();
			victim.rock(host.worldX(posX), host.worldZ(posY), bullet.power());
			host.audio.hitAt(bullet.power(), host.worldX(posX), host.worldZ(posY));
			host.hitTension = Math.min(0.25, host.hitTension + 0.08 + bullet.power() * 0.03);
			host.shake += 3 + bullet.power() * 3;
			host.director.impactCut(bullet.victim(), bullet.power());
			// The first hit that hurts of the round is kept as a photo
			RobotView shooter = host.robots.get(bullet.owner());
			if (host.phase == Phase.PLAYING && !host.hitPhotoTaken && bullet.power() >= PHOTO_HIT_POWER && shooter != null) {
				host.hitPhotoTaken = true;
				host.pendingPhotos.add(new PendingPhoto(PHOTO_HIT_DELAY, host.round + 1, false, shooter.opponent, bullet.power()));
			}
		} else {
			host.audio.sparkAt(host.worldX(posX), host.worldZ(posY));
			if (bullet.state() == BattleRecording.BULLET_HIT_WALL) {
				host.forceField.flashFence(bullet.x(), bullet.y());
			}
		}
	}

	/**
	 * Shots, hits and deaths happen once, when their turn is reached
	 */
	void turnEvents(Turn turn, Turn previous, int turnIndex) {
		host.announcer.say(host.commentary.turn(turn, previous));
		for (Bullet bullet : turn.bullets()) {
			if (!bullet.isHit()) {
				this.bulletFired(bullet, bullet.x(), bullet.y());
			} else {
				host.bulletsSeen.add(bullet.id());
				this.bulletHit(bullet, bullet.x(), bullet.y());
			}
		}
		for (RobotState state : turn.robots()) {
			this.robotEvents(state, previous, turnIndex);
		}
	}

	/** What happens to one robot in a turn: it hits a wall or a robot, runs low on energy, or is destroyed */
	private void robotEvents(RobotState state, Turn previous, int turnIndex) {
		RobotState before = previous == null ? null : ArenaHelpers.find(previous.robots(), state.index(), null);
		RobotView view = host.robots.get(state.index());
		if (view == null) {
			return;
		}
		if ((state.state() == BattleRecording.ROBOT_HIT_WALL || state.state() == BattleRecording.ROBOT_HIT_ROBOT) && before != null && before.state() != state.state()) {
			host.audio.thudAt(host.worldX(state.x()), host.worldZ(state.y()));
			this.startle(host.worldX(state.x()), host.worldZ(state.y()), 120, 0.3);
			host.shake += 1.5;
			if (state.state() == BattleRecording.ROBOT_HIT_WALL) {
				host.forceField.flashFence(state.x(), state.y());
			}
		}
		if (!state.isDead() && state.energy() < 20 && host.lowEnergyWarned.add(state.index())) {
			host.audio.lowEnergyAt(view.position()[0], view.position()[2]);
		}
		if (state.isDead() && before != null && !before.isDead() && !view.dead) {
			view.destroy();
			host.audio.destroyedAt(view.position()[0], view.position()[2]);
			this.startle(view.position()[0], view.position()[2], 340, 1);
			host.shake += 22;
			this.whiteFlash();
			if (host.phase == Phase.PLAYING) {
				host.pendingPhotos.add(new PendingPhoto(PHOTO_DESTRUCTION_DELAY, host.round + 1, true, view.opponent, 0));
			}
			if (host.phase == Phase.PLAYING && !host.replayed) {
				host.showcase.freezeForReplay(state.index(), turnIndex);
			}
		}
	}

	/**
	 * The color of the robot that fired the bullet
	 */
	Color shooterColor(Bullet bullet) {
		RobotView owner = host.robots.get(bullet.owner());
		return owner == null ? Color.web("#38e6ff") : owner.playerColor;
	}

	/**
	 * A bolt is always as thin, whatever its power: the power shows in how long it is and in how much it glows. Its length for no
	 * power and for each unit of power, and the radius of its white core
	 */
	private static final double BOLT_LENGTH = 15;
	private static final double BOLT_LENGTH_POWER = 15;
	private static final double CORE_RADIUS = 0.3;

	/** How long the bolt of a shot of that power is. */
	static double boltLength(double power) {
		return BOLT_LENGTH + BOLT_LENGTH_POWER * power;
	}

	/** How strong a shot of that power is, from 0 (the weakest, 0.1) to 1 (the strongest, 3): how much its bolt glows. */
	static double strength(double power) {
		return Math.max(0, Math.min(1, (power - 0.1) / 2.9));
	}

	/**
	 * A blaster bolt, a shot of a plasma cannon: a long, thin rod of light with rounded ends, its core white hot, wrapped in layers
	 * of glow that grow wider and fainter, in the color of the robot that fired it. A strong shot is longer and its glow is denser
	 * and brighter; a weak one is short and its glow is faint. Its head is the bullet and it trails behind, pointing to +Z, and is
	 * turned to where it goes.
	 */
	Group newBullet(Bullet bullet) {
		Color tint = this.shooterColor(bullet);
		double radius = CORE_RADIUS;
		double length = boltLength(bullet.power());
		double strength = strength(bullet.power());
		// The core is drawn first, so the glow is seen over it
		Group shot = new Group(rod(radius, length).view(light(Color.WHITE, 1)), rod(radius * 2, length * 1.01).view(light(tint.interpolate(Color.WHITE, 0.15 + 0.3 * strength), 0.4 + 0.4 * strength)),
				rod(radius * 3.4, length * 1.03).view(light(tint, 0.12 + 0.28 * strength)), rod(radius * 5.2, length * 1.05).view(light(tint, 0.04 + 0.12 * strength)));
		// Turned around the head, where the bullet is, then made as long as the way it has flown: it comes out of the barrel. Not
		// with setRotate: it turns around the middle of the bolt, and the head would be drawn far from the bullet.
		shot.getTransforms().addAll(new Rotate(0, Rotate.Y_AXIS), new Scale(1, 1, 0.05));
		shot.setTranslateY(BARREL_HEIGHT);
		host.bulletLayer.getChildren().add(shot);
		return shot;
	}

	/**
	 * A material that is only light, of the color, as see through as the opacity: no light of the scene shades it and nothing shines
	 * on it, so the bolt has no faces, it glows.
	 */
	private static PhongMaterial light(Color color, double opacity) {
		PhongMaterial material = new PhongMaterial(Color.color(0, 0, 0, opacity));
		material.setSpecularColor(Color.BLACK);
		material.setSelfIlluminationMap(ArenaHelpers.solidMap(color));
		return material;
	}

	/** A rod along z, from its head at 0 back to the length, as thick as the radius, with its ends rounded. */
	private static LowPoly rod(double radius, double length) {
		LowPoly rod = new LowPoly();
		rod.tube(new double[] { 0, 0, -radius }, new double[] { 0, 0, -length + radius }, radius, 12);
		rod.spindle(0, 0, -radius, radius, radius * 0.7, 0.01, 12);
		rod.spindle(0, 0, -length + radius, radius, 0.01, radius * 0.7, 12);
		return rod;
	}

	/** Removes every bullet from the arena. */
	void clearBullets() {
		for (Group bullet : host.bullets.values()) {
			host.bulletLayer.getChildren().remove(bullet);
		}
		host.bullets.clear();
		this.flights.clear();
	}

	/** Flashes the arena white and fades it out. */
	void whiteFlash() {
		FadeTransition flash = new FadeTransition(Duration.millis(380), host.whiteFlash);
		flash.setFromValue(0.75);
		flash.setToValue(0);
		flash.play();
	}

}
