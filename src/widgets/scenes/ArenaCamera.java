package widgets.scenes;

import static widgets.scenes.BattleArena3D.FLYBY_SECONDS;
import static widgets.scenes.BattleArena3D.energy;

import java.util.List;
import java.util.Map;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import widgets.scenes.BattleArena3D.Phase;
import widgets.scenes.BattleArena3D.Shot;
import widgets.scenes.BattleArena3D.ViewMode;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The director of the battle arena camera: the shots of each moment (the
 * fighters, the bullets, the fatal shot), the free camera the operator flies,
 * the framing of the fighters and the balloons that follow the robots.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaCamera {

	private final BattleArena3D host;

	ArenaCamera(BattleArena3D host) {
		this.host = host;
	}

	// ----------------------------------------------------------------- camera

	/**
	 * The camera jumps to the shot, like a cut in a movie
	 */
	void cut() {
		this.directShot(0);
		System.arraycopy(host.eyeTarget, 0, host.eye, 0, 3);
		System.arraycopy(host.lookTarget, 0, host.look, 0, 3);
		host.fov = host.fovTarget;
		host.roll = host.rollTarget;
	}

	/**
	 * Chooses the next shot: faster cuts and closer shots when the fight is
	 * tense
	 */
	void pickShot(boolean cut) {
		List<Integer> alive = host.robots.entrySet().stream().filter(e -> !e.getValue().dead).map(Map.Entry::getKey).toList();
		double tension = Math.min(1, 0.55 * host.hitTension * 4 + this.lowestEnergyTension());

		Shot[] options = { Shot.WIDE, Shot.CHASE, Shot.CHASE, Shot.LOW_TRACK, Shot.LOW_TRACK, Shot.CLOSE, Shot.CRANE };
		Shot next;
		do {
			next = options[host.random.nextInt(options.length)];
			if (tension > 0.5 && next == Shot.WIDE) {
				next = Shot.CLOSE;
			}
		} while (next == host.shot && host.random.nextDouble() < 0.8);

		host.shot = next;
		host.shotTime = 0;
		host.shotDuration = 5.5 - 3 * tension + host.random.nextDouble() * 1.5;
		host.shotSubject = alive.isEmpty() ? 0 : alive.get(host.random.nextInt(alive.size()));
		host.shotSide = host.random.nextBoolean() ? 1 : -1;
		host.shotSeed = host.random.nextDouble() * Math.PI * 2;
		host.closeDistance = 100;
		if (cut) {
			this.cut();
		}
	}

	double lowestEnergyTension() {
		double lowest = 100;
		for (RobotView view : host.robots.values()) {
			if (!view.dead) {
				lowest = Math.min(lowest, view.energy);
			}
		}
		return 1 - Math.max(0, Math.min(100, lowest)) / 100;
	}

	/**
	 * A strong hit may cut to the victim
	 */
	void impactCut(int victim, double power) {
		if (host.viewMode != ViewMode.CINEMATIC || host.phase != Phase.PLAYING || host.shotTime < 1.2 || host.random.nextDouble() > 0.25 + power * 0.15) {
			return;
		}
		host.shot = Shot.IMPACT;
		host.shotSubject = victim;
		host.shotTime = 0;
		host.shotDuration = 1.6;
		host.closeDistance = 90;
	}

	void updateCamera(double dt) {
		host.shotTime += dt;
		if (host.viewMode == ViewMode.CINEMATIC || host.phase == Phase.REPLAY || host.phase == Phase.FREEZE || host.phase == Phase.SHOWCASE || host.phase == Phase.FLYBY) {
			if (host.shotTime > host.shotDuration && host.phase == Phase.PLAYING) {
				this.pickShot(false);
			}
			this.directShot(dt);
		} else {
			this.freeCamera(dt);
		}

		double ease = 1 - Math.exp(-host.follow * dt);
		for (int i = 0; i < 3; i++) {
			host.eye[i] += (host.eyeTarget[i] - host.eye[i]) * ease;
			host.look[i] += (host.lookTarget[i] - host.look[i]) * ease;
		}
		host.fov += (host.fovTarget - host.fov) * ease;
		host.roll += (host.rollTarget - host.roll) * ease;

		// Shake of the explosions
		host.shake = Math.min(40, host.shake) * Math.exp(-5 * dt);
		double sx = (host.random.nextDouble() - 0.5) * host.shake;
		double sy = (host.random.nextDouble() - 0.5) * host.shake;
		double sz = (host.random.nextDouble() - 0.5) * host.shake;

		double fx = host.look[0] - host.eye[0];
		double fy = host.look[1] - host.eye[1];
		double fz = host.look[2] - host.eye[2];
		double distance = Math.max(1, Math.sqrt(fx * fx + fy * fy + fz * fz));

		host.camera.setFieldOfView(host.fov);
		host.cameraLook.setX(host.look[0] + sx);
		host.cameraLook.setY(host.look[1] + sy);
		host.cameraLook.setZ(host.look[2] + sz);
		host.cameraYaw.setAngle(Math.toDegrees(Math.atan2(fx, fz)));
		host.cameraPitch.setAngle(Math.toDegrees(Math.asin(-fy / distance)));
		host.cameraRoll.setAngle(host.roll + (host.random.nextDouble() - 0.5) * host.shake * 0.1);
		host.cameraDistance.setZ(-distance);
	}

	/**
	 * Free (orbit), isometric and top views
	 */
	void freeCamera(double dt) {
		if (host.viewMode == ViewMode.FREE && Math.abs(host.yawSpeed) > 0.1) {
			// Momentum after a drag
			host.targetYaw += host.yawSpeed * dt;
			host.yawSpeed *= Math.exp(-4 * dt);
		}
		double ease = 1 - Math.exp(-5 * dt);
		host.yaw += (host.targetYaw - host.yaw) * ease;
		double wantedPitch = host.viewMode == ViewMode.FREE ? host.freePitch : host.viewMode.pitch;
		host.pitch += (wantedPitch - host.pitch) * ease;
		host.zoom += (host.targetZoom - host.zoom) * ease;

		double aspect = Math.max(0.5, host.getWidth() / Math.max(1, host.getHeight()));
		double tangent = Math.tan(Math.toRadians(host.viewMode.fov / 2));
		double distance;
		if (host.viewMode == ViewMode.TOP_DOWN) {
			distance = Math.max(host.fieldHeight / 2 * 1.3, host.fieldWidth / 2 * 1.08 / aspect) / tangent;
		} else {
			distance = 0.5 * Math.hypot(host.fieldWidth, host.fieldHeight) * 0.78 / tangent / Math.min(1, aspect);
		}
		distance *= host.zoom;

		double yawRad = Math.toRadians(host.yaw);
		double pitchRad = Math.toRadians(host.pitch);
		// Forward of the camera for this yaw and pitch
		double fx = Math.cos(pitchRad) * Math.sin(yawRad);
		double fy = -Math.sin(pitchRad);
		double fz = Math.cos(pitchRad) * Math.cos(yawRad);
		host.lookTarget[0] = 0;
		host.lookTarget[1] = -10;
		host.lookTarget[2] = 0;
		host.eyeTarget[0] = -fx * distance;
		host.eyeTarget[1] = -10 - fy * distance;
		host.eyeTarget[2] = -fz * distance;
		host.fovTarget = host.viewMode.fov;
		host.rollTarget = 0;
		host.follow = 6;
	}

	/**
	 * Where the cinematic camera is and looks, for the current shot
	 */
	void directShot(double dt) {
		RobotView subject = host.robots.get(host.shotSubject);
		RobotView other = host.robots.values().stream().filter(v -> v != subject).findFirst().orElse(subject);
		double[] a = subject == null ? new double[] { 0, 0, 0 } : subject.position();
		double[] b = other == null ? a : other.position();
		host.rollTarget = 0;

		if (host.phase == Phase.FLYBY) {
			// The camera stays on the critter, following it, and then the trip begins
			double hold = host.flyByHold();
			if (host.phaseTime < hold) {
				host.flyFocus = host.flyFocusSource.get();
			}
			host.flyByPose(Math.max(0, Math.min(1, (host.phaseTime - hold) / FLYBY_SECONDS)));
			return;
		}
		if (host.phase == Phase.SHOWCASE && !host.versus) {
			host.showcaseShot(subject == null ? new double[] { 0, 0, 0 } : subject.position());
			return;
		}

		if (host.shot == Shot.FATAL) {
			this.fatalShot(a);
			this.keepClear();
			host.eyeTarget[1] = Math.min(host.eyeTarget[1], -10);
			return;
		}

		// When a bot wins, focus the camera directly on the victor!
		List<RobotView> livingRobots = host.robots.values().stream().filter(v -> !v.dead).toList();
		boolean roundOver = host.phase == Phase.ROUND_END || host.phase == Phase.FINISHED;
		RobotView winner = null;
		// Not while a death is held on screen: the wreck is framed then, see freezeForReplay
		if (host.robots.size() >= 2 && livingRobots.size() == 1 && host.phase != Phase.FREEZE) {
			winner = livingRobots.get(0);
		} else if (roundOver && host.robots.containsKey(host.shotSubject)) {
			winner = host.robots.get(host.shotSubject);
		}

		if (winner != null) {
			host.cinematicOrbitAngle += dt * 0.35;
			double[] winPos = winner.position();
			double winDist = 175.0; // Heroic victory framing
			double winHeight = -42.0; // Elevated view looking proud at the victor
			double winAngle = host.cinematicOrbitAngle;

			this.setShot(winPos[0] + Math.cos(winAngle) * winDist, winHeight, winPos[2] + Math.sin(winAngle) * winDist,
					winPos[0], -18, winPos[2], 44.0, 3.5);
			host.rollTarget = 0;
			this.keepClear();
			host.eyeTarget[1] = Math.min(host.eyeTarget[1], -10);
			return;
		}

		this.frameFighters(dt, a, b);
	}

	/**
	 * Frames both fighters: the camera circles them on a flank, looking at the
	 * middle between them, far enough away to keep both in view
	 */
	void frameFighters(double dt, double[] a, double[] b) {
		double[] mid = { (a[0] + b[0]) / 2, -16, (a[2] + b[2]) / 2 };
		double dx = b[0] - a[0];
		double dz = b[2] - a[2];
		double separation = Math.max(10, Math.hypot(dx, dz));
		double ux = dx / separation;
		double uz = dz / separation;
		// Perpendicular to the line between them
		double px = -uz * host.shotSide;
		double pz = ux * host.shotSide;
		host.cinematicOrbitAngle += dt * 0.25;

		// Calculate distance required to guarantee both robots are comfortably inside the field of view
		// Fit the bounding circle encompassing both combatants to the horizontal field of view
		double aspect = Math.max(0.5, host.getWidth() / Math.max(1.0, host.getHeight()));
		double fovVal = 48.0;
		double vFovRad = Math.toRadians(fovVal);
		double hFovRad = 2.0 * Math.atan(Math.tan(vFovRad / 2.0) * aspect);

		// Boxing referee camera: continuously seeks the best angle of the fight,
		// circling the combatants on the flank to keep both clearly in sight.
		double curDx = host.eye[0] - mid[0];
		double curDz = host.eye[2] - mid[2];
		double curAngle = Math.atan2(curDz, curDx);

		// The two flanks perpendicular to the combat line
		double flankAngle1 = Math.atan2(pz, px);
		double flankAngle2 = Math.atan2(-pz, -px);

		// Maintain continuity with the referee's current position around the fighters
		double diff1 = Math.abs(Math.atan2(Math.sin(flankAngle1 - curAngle), Math.cos(flankAngle1 - curAngle)));
		double diff2 = Math.abs(Math.atan2(Math.sin(flankAngle2 - curAngle), Math.cos(flankAngle2 - curAngle)));
		double baseFlank = diff1 <= diff2 ? flankAngle1 : flankAngle2;

		// Dynamic 3/4 angle & referee footwork: gently pacing to keep an unobstructed view
		double pacing = Math.sin(host.cinematicOrbitAngle) * 0.16;
		double bestAngle = baseFlank + pacing;

		// Frame both combatants with generous margins so neither leaves the view nor feels cramped
		double sphereRadius = separation * 0.5 + 80.0;
		double fitDist = sphereRadius / Math.tan(hFovRad * 0.35);

		// Well-balanced ringside distance: neither too zoomed out nor right in the robot treads
		double targetDist = Math.max(420.0, fitDist);
		if (host.shot == Shot.IMPACT || host.hitTension > 0.15) {
			targetDist *= 0.95;
		}

		// Elevation: ringside referee perspective looking slightly down into the arena
		double eyeY = -Math.max(65.0, targetDist * 0.23);

		// Roll target: subtle dynamic tilt on impacts, otherwise level
		host.rollTarget = (host.shot == Shot.IMPACT ? 2.0 : 0.5) * Math.sin(host.cinematicOrbitAngle);

		// Aim directly at the midpoint between the fighters
		this.setShot(mid[0] + Math.cos(bestAngle) * targetDist, eyeY, mid[2] + Math.sin(bestAngle) * targetDist,
				mid[0], -16, mid[2], fovVal, 3.5);
		this.keepClear();
		// Never under the floor
		host.eyeTarget[1] = Math.min(host.eyeTarget[1], -10);
	}

	/**
	 * The camera never goes inside a robot: if one is too close, the camera
	 * moves away from it and up
	 */
	void keepClear() {
		for (RobotView view : host.robots.values()) {
			double[] position = view.position();
			double dx = host.eyeTarget[0] - position[0];
			double dz = host.eyeTarget[2] - position[2];
			double distance = Math.hypot(dx, dz);
			double minimum = 65;
			if (distance < minimum && host.eyeTarget[1] > -80) {
				if (distance < 1) {
					dx = 1;
					dz = 0;
					distance = 1;
				}
				host.eyeTarget[0] = position[0] + dx / distance * minimum;
				host.eyeTarget[2] = position[2] + dz / distance * minimum;
				host.eyeTarget[1] = Math.min(host.eyeTarget[1], -45);
			}
		}
	}

	/**
	 * The replay: the camera rides behind the fatal bullet and then watches the
	 * robot explode from the side
	 */
	void fatalShot(double[] victim) {
		Group bullet = host.bullets.get(host.fatalBullet);
		double turn = host.turnPosition;
		if (bullet != null && host.impactTurn >= 0 && turn < host.impactTurn) {
			double bx = bullet.getTranslateX();
			double bz = bullet.getTranslateZ();
			double dx = victim[0] - bx;
			double dz = victim[2] - bz;
			double length = Math.max(1, Math.hypot(dx, dz));
			double ux = dx / length;
			double uz = dz / length;
			this.setShot(bx - ux * 120 - uz * 22 * host.shotSide, -30, bz - uz * 120 + ux * 22 * host.shotSide, bx + ux * 140, -20, bz + uz * 140, 38, 9);
			host.rollTarget = -5 * host.shotSide;
		} else {
			// Side view of the victim, slowly getting closer
			double angle = host.shotSeed + host.phaseTime * 0.15;
			double radius = 175 - Math.min(35, host.phaseTime * 5);
			this.setShot(victim[0] + Math.cos(angle) * radius, -26, victim[2] + Math.sin(angle) * radius, victim[0], -18, victim[2], 36, 4);
			host.rollTarget = 4 * host.shotSide;
		}
	}

	void setShot(double ex, double ey, double ez, double lx, double ly, double lz, double fov, double follow) {
		host.eyeTarget[0] = ex;
		host.eyeTarget[1] = ey;
		host.eyeTarget[2] = ez;
		host.lookTarget[0] = lx;
		host.lookTarget[1] = ly;
		host.lookTarget[2] = lz;
		host.fovTarget = fov;
		host.follow = follow;
	}

	/**
	 * The name and energy balloons follow their robots on the screen
	 */
	void placeBalloons() {
		boolean show = host.phase != Phase.LOADING && host.phase != Phase.REPLAY && host.phase != Phase.SHOWCASE && host.phase != Phase.FLYBY;
		for (RobotView view : host.robots.values()) {
			Point3D scene = view.root.localToScene(new Point3D(0, -62, 0), true);
			if (scene == null || !show) {
				view.balloon.setVisible(false);
				continue;
			}
			Point2D local = host.balloons.sceneToLocal(scene.getX(), scene.getY());
			boolean inside = local != null && local.getX() > -100 && local.getY() > -100 && local.getX() < host.getWidth() + 100 && local.getY() < host.getHeight() + 100;
			view.balloon.setVisible(inside);
			if (inside) {
				view.balloon.autosize();
				view.balloon.relocate(local.getX() - view.balloon.getWidth() / 2, local.getY() - view.balloon.getHeight());
			}
		}
	}
}
