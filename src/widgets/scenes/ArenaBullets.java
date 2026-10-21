package widgets.scenes;

import static widgets.scenes.BattleArena3D.FENCE_BASE;
import static widgets.scenes.BattleArena3D.PHOTO_DESTRUCTION_DELAY;
import static widgets.scenes.BattleArena3D.PHOTO_HIT_DELAY;
import static widgets.scenes.BattleArena3D.PHOTO_HIT_POWER;
import static widgets.scenes.ArenaMaterials.energy;
import static widgets.scenes.BattleArena3D.findBullet;
import static widgets.scenes.BattleArena3D.pose;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;
import utils.robocode.BattleRecording;
import widgets.scenes.ArenaFence.FenceSegment;
import widgets.scenes.BattleArena3D.PendingPhoto;
import widgets.scenes.BattleArena3D.Phase;

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

	ArenaBullets(BattleArena3D host) {
		this.host = host;
	}

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
		for (int k = first + 1; k < turns.size(); k++) {
			Bullet now = findBullet(turns.get(k), seen.id());
			if (now == null) {
				break;
			}
			end = k;
			if (now.isHit()) {
				hit = now;
				last = now;
				break;
			}
			previous = now;
			last = now;
		}
		double speed = 20 - 3 * start.power();
		double dx = last.x() - start.x();
		double dy = last.y() - start.y();
		double length = Math.hypot(dx, dy);
		Flight flight = null;
		if (length > 1e-6 && !start.isHit()) {
			dx /= length;
			dy /= length;
			double born = first - 1;
			// Gets there when the last step is done
			double duration = hit != null ? end - 1 - born + Math.hypot(hit.x() - previous.x(), hit.y() - previous.y()) / speed : end - born;
			duration = Math.max(0.01, duration);
			double sx = start.x() - dx * speed;
			double sy = start.y() - dy * speed;
			double ex = last.x();
			double ey = last.y();
			// Where it hits the victim, in the frame of the tank (across and ahead),
			// and where that point of the hull is drawn when the bolt gets there: the
			// bolt flies straight to it
			double side = 0;
			double ahead = 0;
			if (hit != null && hit.state() == BattleRecording.BULLET_HIT_VICTIM) {
				RobotState victim = BattleArena3D.find(turns.get(end).robots(), hit.victim(), null);
				double[] pose = pose(turns, hit.victim(), born + duration);
				if (victim != null && pose != null) {
					double ox = hit.x() - victim.x();
					double oy = hit.y() - victim.y();
					double heading = victim.bodyHeading();
					side = Math.max(-HULL_SIDE, Math.min(HULL_SIDE, ox * Math.cos(heading) - oy * Math.sin(heading)));
					ahead = Math.max(-HULL_AHEAD, Math.min(HULL_AHEAD, ox * Math.sin(heading) + oy * Math.cos(heading)));
					ex = pose[0] + side * Math.cos(pose[2]) + ahead * Math.sin(pose[2]);
					ey = pose[1] - side * Math.sin(pose[2]) + ahead * Math.cos(pose[2]);
				}
			}
			double total = Math.hypot(ex - sx, ey - sy);
			if (total > 1e-6) {
				flight = new Flight(sx, sy, (ex - sx) / total, (ey - sy) / total, total, born, duration, hit, side, ahead);
			}
		}
		// A bullet with no flight is never drawn: it was born hitting
		this.flights.put(seen.id(), flight == null ? new Flight(0, 0, 0, 0, 0, 0, 0, null, 0, 0) : flight);
		return this.flights.get(seen.id());
	}

	void showBullets(List<Turn> turns, int index, double fraction) {
		Turn current = turns.get(index);
		Turn next = turns.get(Math.min(index + 1, turns.size() - 1));
		Set<Integer> flying = new HashSet<Integer>();
		for (Turn turn : next == current ? List.of(current) : List.of(current, next)) {
			for (Bullet bullet : turn.bullets()) {
				if (bullet.isHit() || bullet.state() > BattleRecording.BULLET_HIT_WALL || flying.contains(bullet.id())) {
					continue;
				}
				Flight flight = this.flight(turns, turn == current ? index : index + 1, bullet);
				if (flight.total() <= 0) {
					continue;
				}
				double progress = (host.turnPosition - flight.born()) / flight.duration();
				if (progress < 0) {
					continue;
				}
				if (progress >= 1) {
					if (flight.hit() != null) {
						double[] target = this.target(flight);
						this.bulletHit(flight.hit(), target[0], target[1]);
						continue;
					}
					progress = 1;
				}
				// From the muzzle to the end, the speed is the same all the way
				double distance = flight.total() > MUZZLE ? MUZZLE + (flight.total() - MUZZLE) * progress : flight.total() * progress;
				double x = flight.sx() + flight.ux() * distance;
				double y = flight.sy() + flight.uy() * distance;
				// The ones that hit a tank come down, little by little, from the height of
				// the gun to the middle of the hull
				double height = BARREL_HEIGHT;
				if (flight.hit() != null && flight.hit().state() == BattleRecording.BULLET_HIT_VICTIM) {
					double along = flight.total() > MUZZLE ? (distance - MUZZLE) / (flight.total() - MUZZLE) : progress;
					height += (HULL_HEIGHT - BARREL_HEIGHT) * Math.max(0, Math.min(1, along));
				}
				if (distance >= MUZZLE) {
					this.bulletFired(bullet, flight.sx() + flight.ux() * MUZZLE, flight.sy() + flight.uy() * MUZZLE);
				} else if (host.bulletsSeen.contains(bullet.id()) == false) {
					this.bulletFired(bullet, flight.sx() + flight.ux() * Math.min(MUZZLE, flight.total()), flight.sy() + flight.uy() * Math.min(MUZZLE, flight.total()));
				}
				this.placeBullet(bullet, flying, x, y, flight.ux(), flight.uy(), height);
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

	void placeBullet(Bullet bullet, Set<Integer> flying, double x, double y, double dx, double dy, double height) {
		flying.add(bullet.id());
		Group shot = host.bullets.computeIfAbsent(bullet.id(), id -> this.newBullet(bullet));
		shot.setTranslateX(host.worldX(x));
		shot.setTranslateZ(host.worldZ(y));
		shot.setTranslateY(height);
		// The bolt points to where it goes, turned around its head
		((Rotate) shot.getTransforms().get(0)).setAngle(Math.toDegrees(Math.atan2(dx, dy)));
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
		double x = victim.root.getTranslateX() + host.fieldWidth / 2;
		double y = victim.root.getTranslateZ() + host.fieldHeight / 2;
		double heading = Math.toRadians(victim.bodyRotate.getAngle());
		return new double[] { x + flight.side() * Math.cos(heading) + flight.ahead() * Math.sin(heading), y - flight.side() * Math.sin(heading) + flight.ahead() * Math.cos(heading) };
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
	void bulletFired(Bullet bullet, double x, double y) {
		if (host.bulletsSeen.add(bullet.id())) {
			host.audio.shotAt(bullet.power(), host.worldX(x), host.worldZ(y));
			host.fx.muzzleFlash(host.worldX(x), host.worldZ(y), this.shooterColor(bullet));
			this.startle(host.worldX(x), host.worldZ(y), 150, 0.4);
		}
	}

	/**
	 * The hit, once: the explosion where the bullet stopped and what the victim
	 * feels
	 */
	void bulletHit(Bullet bullet, double x, double y) {
		if (!host.bulletsExploded.add(bullet.id())) {
			return;
		}
		host.fx.explosion(host.worldX(x), host.worldZ(y), 8 + bullet.power() * 6, this.shooterColor(bullet));
		this.startle(host.worldX(x), host.worldZ(y), 220, 0.7);
		RobotView victim = host.robots.get(bullet.victim());
		if (bullet.state() == BattleRecording.BULLET_HIT_VICTIM && victim != null) {
			victim.flash();
			host.audio.hitAt(bullet.power(), host.worldX(x), host.worldZ(y));
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
			host.audio.sparkAt(host.worldX(x), host.worldZ(y));
			if (bullet.state() == BattleRecording.BULLET_HIT_WALL) {
				this.flashFence(bullet.x(), bullet.y());
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
			RobotState before = previous == null ? null : BattleArena3D.find(previous.robots(), state.index(), null);
			RobotView view = host.robots.get(state.index());
			if (view == null) {
				continue;
			}
			if ((state.state() == BattleRecording.ROBOT_HIT_WALL || state.state() == BattleRecording.ROBOT_HIT_ROBOT) && before != null && before.state() != state.state()) {
				host.audio.thudAt(host.worldX(state.x()), host.worldZ(state.y()));
				this.startle(host.worldX(state.x()), host.worldZ(state.y()), 120, 0.3);
				host.shake += 1.5;
				if (state.state() == BattleRecording.ROBOT_HIT_WALL) {
					this.flashFence(state.x(), state.y());
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
	}

	/**
	 * The color of the robot that fired the bullet
	 */
	Color shooterColor(Bullet bullet) {
		RobotView owner = host.robots.get(bullet.owner());
		return owner == null ? Color.web("#38e6ff") : owner.playerColor;
	}

	/**
	 * A plasma bolt: a bright spindle of facets, a halo around it and a long tail
	 * behind, in the color of the robot that fired it. It points to +Z and is
	 * turned to where it goes.
	 */
	Group newBullet(Bullet bullet) {
		Color tint = this.shooterColor(bullet);
		double r = 2.2 + bullet.power() * 0.7;
		LowPoly core = new LowPoly();
		core.spindle(0, 0, 0, r, r * 3.4, r * 2.2, 6);
		LowPoly halo = new LowPoly();
		halo.spindle(0, 0, 0, r * 1.9, r * 3.8, r * 2.8, 6);
		LowPoly tail = new LowPoly();
		tail.spindle(0, 0, -r * 2, r * 0.9, 0, r * 12, 6);

		Color hot = tint.interpolate(Color.WHITE, 0.75);
		PhongMaterial coreMaterial = new PhongMaterial(hot);
		coreMaterial.setSelfIlluminationMap(BattleArena3D.solidMap(hot));
		// The halo and the tail are see through and shine by themselves
		PhongMaterial haloMaterial = new PhongMaterial(Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.28));
		haloMaterial.setSelfIlluminationMap(BattleArena3D.solidMap(tint));
		PhongMaterial tailMaterial = new PhongMaterial(Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.4));
		tailMaterial.setSelfIlluminationMap(BattleArena3D.solidMap(tint));
		Group shot = new Group(tail.view(tailMaterial), halo.view(haloMaterial), core.view(coreMaterial));
		// Turned around the head, where the bullet is. Not with setRotate: it turns
		// around the middle of the bolt, the long tail moves it there, and the head
		// would be drawn up to 40 units away from the bullet, to the side of the
		// robot it hits.
		shot.getTransforms().add(new Rotate(0, Rotate.Y_AXIS));
		shot.setTranslateY(BARREL_HEIGHT);
		host.bulletLayer.getChildren().add(shot);
		return shot;
	}

	void clearBullets() {
		for (Group bullet : host.bullets.values()) {
			host.bulletLayer.getChildren().remove(bullet);
		}
		host.bullets.clear();
		this.flights.clear();
	}

	void whiteFlash() {
		FadeTransition flash = new FadeTransition(Duration.millis(380), host.whiteFlash);
		flash.setFromValue(0.75);
		flash.setToValue(0);
		flash.play();
	}

	/**
	 * The cage lights up where a robot or a bullet hit it, the glow spreads to
	 * the neighboring panels like a shockwave
	 */
	void flashFence(double x, double y) {
		double w = host.fieldWidth;
		double h = host.fieldHeight;
		double cx = Math.max(0, Math.min(w, x));
		double cy = Math.max(0, Math.min(h, y));

		double[] distances = { cy, h - cy, cx, w - cx };
		int wall = 0;
		for (int i = 1; i < distances.length; i++) {
			if (distances[i] < distances[wall]) {
				wall = i;
			}
		}

		double along = wall <= 1 ? cx : cy;
		int count = (int) Math.round((wall <= 1 ? w : h) / 100);
		int index = Math.max(0, Math.min(count - 1, (int) Math.floor(along / 100)));

		this.flashSegment(wall, index, 1.0, Duration.ZERO);
		this.flashSegment(wall, index - 1, 0.6, Duration.millis(60));
		this.flashSegment(wall, index + 1, 0.6, Duration.millis(60));
		this.flashSegment(wall, index - 2, 0.3, Duration.millis(120));
		this.flashSegment(wall, index + 2, 0.3, Duration.millis(120));
	}

	void flashSegment(int wall, int index, double intensity, Duration delay) {
		for (FenceSegment segment : host.fenceSegments) {
			if (segment.wall() != wall || segment.index() != index) {
				continue;
			}
			if (segment.animation != null) {
				segment.animation.stop();
				segment.animation = null;
			}

			segment.box().setVisible(true);

			// Electric force field energy flare: transparent translucent barrier
			double peakAlpha = 0.20 + 0.08 * Math.min(1.0, intensity);
			Color tint = host.style.fence;
			Color shieldBright = Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), peakAlpha);
			Color shieldMid = Color.color(tint.getRed() * 0.6, tint.getGreen() * 0.8, tint.getBlue(), peakAlpha * 0.45);
			Color shieldFade = Color.color(tint.getRed() * 0.4, tint.getGreen() * 0.8, tint.getBlue(), 0.0);

			segment.material().setSpecularColor(Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.20));

			Timeline flash = new Timeline();
			flash.getKeyFrames().addAll(
					new KeyFrame(delay, new KeyValue(segment.material().diffuseColorProperty(), shieldBright)),
					new KeyFrame(delay.add(Duration.millis(90)), new KeyValue(segment.material().diffuseColorProperty(), shieldMid)),
					new KeyFrame(delay.add(Duration.millis(320)), new KeyValue(segment.material().diffuseColorProperty(), shieldFade)));
			flash.setOnFinished(e -> {
				segment.box().setVisible(false);
				segment.material().setDiffuseColor(FENCE_BASE);
				segment.material().setSpecularColor(Color.TRANSPARENT);
				segment.animation = null;
			});
			segment.animation = flash;
			flash.play();

			if (intensity >= 0.8) {
				ScaleTransition pulse = new ScaleTransition(Duration.millis(90), segment.box());
				pulse.setFromX(1);
				pulse.setToX(1.04);
				pulse.setFromY(1);
				pulse.setToY(1.04);
				pulse.setFromZ(1);
				pulse.setToZ(1.04);
				pulse.setCycleCount(2);
				pulse.setAutoReverse(true);
				pulse.play();
			}
			return;
		}
	}

	/** Back to the base look when a new round starts */
	void resetFence() {
		for (FenceSegment segment : host.fenceSegments) {
			if (segment.animation != null) {
				segment.animation.stop();
				segment.animation = null;
			}
			segment.box().setVisible(false);
			segment.material().setDiffuseColor(FENCE_BASE);
			segment.material().setSpecularColor(Color.TRANSPARENT);
			segment.box().setScaleX(1);
			segment.box().setScaleY(1);
			segment.box().setScaleZ(1);
		}
	}
}
