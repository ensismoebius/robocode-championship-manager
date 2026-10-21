package widgets.scenes;

import static widgets.scenes.BattleArena3D.FLYBY_SECONDS;
import static widgets.scenes.ArenaMaterials.energy;

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

	/** Creates the camera director of the arena, which chooses and moves the camera shots. */
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
		List<Integer> alive = host.robots.entrySet().stream().filter(event -> !event.getValue().dead).map(Map.Entry::getKey).toList();
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

	/** The lowest energy among the robots still alive: the lower it is, the more tense the battle. */
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

	/** Moves the camera to the shot of the moment, or lets it follow the free view. */
	void updateCamera(double deltaTime) {
		host.shotTime += deltaTime;
		if (host.viewMode == ViewMode.CINEMATIC || host.phase == Phase.REPLAY || host.phase == Phase.FREEZE || host.phase == Phase.SHOWCASE || host.phase == Phase.FLYBY) {
			if (host.shotTime > host.shotDuration && host.phase == Phase.PLAYING) {
				this.pickShot(false);
			}
			this.directShot(deltaTime);
		} else {
			this.freeCamera(deltaTime);
		}

		double ease = 1 - Math.exp(-host.follow * deltaTime);
		for (int index = 0; index < 3; index++) {
			host.eye[index] += (host.eyeTarget[index] - host.eye[index]) * ease;
			host.look[index] += (host.lookTarget[index] - host.look[index]) * ease;
		}
		host.fov += (host.fovTarget - host.fov) * ease;
		host.roll += (host.rollTarget - host.roll) * ease;

		// Shake of the explosions
		host.shake = Math.min(40, host.shake) * Math.exp(-5 * deltaTime);
		double sxValue = (host.random.nextDouble() - 0.5) * host.shake;
		double syValue = (host.random.nextDouble() - 0.5) * host.shake;
		double szValue = (host.random.nextDouble() - 0.5) * host.shake;

		double fxValue = host.look[0] - host.eye[0];
		double fyValue = host.look[1] - host.eye[1];
		double fzValue = host.look[2] - host.eye[2];
		double distance = Math.max(1, Math.sqrt(fxValue * fxValue + fyValue * fyValue + fzValue * fzValue));

		host.camera.setFieldOfView(host.fov);
		host.cameraLook.setX(host.look[0] + sxValue);
		host.cameraLook.setY(host.look[1] + syValue);
		host.cameraLook.setZ(host.look[2] + szValue);
		host.cameraYaw.setAngle(Math.toDegrees(Math.atan2(fxValue, fzValue)));
		host.cameraPitch.setAngle(Math.toDegrees(Math.asin(-fyValue / distance)));
		host.cameraRoll.setAngle(host.roll + (host.random.nextDouble() - 0.5) * host.shake * 0.1);
		host.cameraDistance.setZ(-distance);
	}

	/**
	 * Free (orbit), isometric and top views
	 */
	void freeCamera(double deltaTime) {
		if (host.viewMode == ViewMode.FREE && Math.abs(host.yawSpeed) > 0.1) {
			// Momentum after a drag
			host.targetYaw += host.yawSpeed * deltaTime;
			host.yawSpeed *= Math.exp(-4 * deltaTime);
		}
		double ease = 1 - Math.exp(-5 * deltaTime);
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
		double fxValue = Math.cos(pitchRad) * Math.sin(yawRad);
		double fyValue = -Math.sin(pitchRad);
		double fzValue = Math.cos(pitchRad) * Math.cos(yawRad);
		host.lookTarget[0] = 0;
		host.lookTarget[1] = -10;
		host.lookTarget[2] = 0;
		host.eyeTarget[0] = -fxValue * distance;
		host.eyeTarget[1] = -10 - fyValue * distance;
		host.eyeTarget[2] = -fzValue * distance;
		host.fovTarget = host.viewMode.fov;
		host.rollTarget = 0;
		host.follow = 6;
	}

	/**
	 * Where the cinematic camera is and looks, for the current shot
	 */
	void directShot(double deltaTime) {
		RobotView subject = host.robots.get(host.shotSubject);
		RobotView other = host.robots.values().stream().filter(vCoord -> vCoord != subject).findFirst().orElse(subject);
		double[] first = subject == null ? new double[] { 0, 0, 0 } : subject.position();
		double[] second = other == null ? first : other.position();
		host.rollTarget = 0;

		if (host.phase == Phase.FLYBY) {
			// The camera stays on the critter, following it, and then the trip begins
			double hold = host.rounds.flyByHold();
			if (host.phaseTime < hold) {
				host.flyFocus = host.flyFocusSource.get();
			}
			host.rounds.flyByPose(Math.max(0, Math.min(1, (host.phaseTime - hold) / FLYBY_SECONDS)));
			return;
		}
		if (host.phase == Phase.SHOWCASE && !host.versus) {
			host.showcase.showcaseShot(subject == null ? new double[] { 0, 0, 0 } : subject.position());
			return;
		}

		if (host.shot == Shot.FATAL) {
			this.fatalShot(first);
			this.keepClear();
			host.eyeTarget[1] = Math.min(host.eyeTarget[1], -10);
			return;
		}

		// When a bot wins, focus the camera directly on the victor!
		List<RobotView> livingRobots = host.robots.values().stream().filter(vCoord2 -> !vCoord2.dead).toList();
		boolean roundOver = host.phase == Phase.ROUND_END || host.phase == Phase.FINISHED;
		RobotView winner = null;
		// Not while a death is held on screen: the wreck is framed then, see freezeForReplay
		if (host.robots.size() >= 2 && livingRobots.size() == 1 && host.phase != Phase.FREEZE) {
			winner = livingRobots.get(0);
		} else if (roundOver && host.robots.containsKey(host.shotSubject)) {
			winner = host.robots.get(host.shotSubject);
		}

		if (winner != null) {
			host.cinematicOrbitAngle += deltaTime * 0.35;
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

		this.frameFighters(deltaTime, first, second);
	}

	/**
	 * Frames both fighters: the camera circles them on a flank, looking at the
	 * middle between them, far enough away to keep both in view
	 */
	void frameFighters(double deltaTime, double[] first, double[] second) {
		double[] mid = { (first[0] + second[0]) / 2, -16, (first[2] + second[2]) / 2 };
		double deltaX = second[0] - first[0];
		double deltaZ = second[2] - first[2];
		double separation = Math.max(10, Math.hypot(deltaX, deltaZ));
		double uxValue = deltaX / separation;
		double uzValue = deltaZ / separation;
		// Perpendicular to the line between them
		double posX = -uzValue * host.shotSide;
		double posZ = uxValue * host.shotSide;
		host.cinematicOrbitAngle += deltaTime * 0.25;

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
		double flankAngle1 = Math.atan2(posZ, posX);
		double flankAngle2 = Math.atan2(-posZ, -posX);

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
			double deltaX = host.eyeTarget[0] - position[0];
			double deltaZ = host.eyeTarget[2] - position[2];
			double distance = Math.hypot(deltaX, deltaZ);
			double minimum = 65;
			if (distance < minimum && host.eyeTarget[1] > -80) {
				if (distance < 1) {
					deltaX = 1;
					deltaZ = 0;
					distance = 1;
				}
				host.eyeTarget[0] = position[0] + deltaX / distance * minimum;
				host.eyeTarget[2] = position[2] + deltaZ / distance * minimum;
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
			double bxValue = bullet.getTranslateX();
			double bzValue = bullet.getTranslateZ();
			double deltaX = victim[0] - bxValue;
			double deltaZ = victim[2] - bzValue;
			double length = Math.max(1, Math.hypot(deltaX, deltaZ));
			double uxValue = deltaX / length;
			double uzValue = deltaZ / length;
			this.setShot(bxValue - uxValue * 120 - uzValue * 22 * host.shotSide, -30, bzValue - uzValue * 120 + uxValue * 22 * host.shotSide, bxValue + uxValue * 140, -20, bzValue + uzValue * 140, 38, 9);
			host.rollTarget = -5 * host.shotSide;
		} else {
			// Side view of the victim, slowly getting closer
			double angle = host.shotSeed + host.phaseTime * 0.15;
			double radius = 175 - Math.min(35, host.phaseTime * 5);
			this.setShot(victim[0] + Math.cos(angle) * radius, -26, victim[2] + Math.sin(angle) * radius, victim[0], -18, victim[2], 36, 4);
			host.rollTarget = 4 * host.shotSide;
		}
	}

	/** Sets the eye and the look targets of the camera shot, with its field of view and how it follows. */
	void setShot(double eyeX, double eyeY, double eyeZ, double lookX, double lookY, double lookZ, double fov, double follow) {
		host.eyeTarget[0] = eyeX;
		host.eyeTarget[1] = eyeY;
		host.eyeTarget[2] = eyeZ;
		host.lookTarget[0] = lookX;
		host.lookTarget[1] = lookY;
		host.lookTarget[2] = lookZ;
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
