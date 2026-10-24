package widgets.scenes;

import java.util.Map;
import static widgets.scenes.BattleArena3D.FLYBY_SECONDS;
import static widgets.scenes.BattleArena3D.FREEZE_SECONDS;
import static widgets.scenes.BattleArena3D.LAVA_DIM;
import static widgets.scenes.BattleArena3D.LAVA_FULL;
import static widgets.scenes.BattleArena3D.SHOWCASE_SECONDS;
import static widgets.scenes.BattleArena3D.VERSUS_SECONDS;

/**
 * The clock of the arena: each frame it moves the phases of the battle, the surfaces, the sounds and the effects
 */
final class ArenaTicker {

	private final BattleArena3D host;

	/** Creates the clock of the given arena. */
	ArenaTicker(BattleArena3D host) {
		this.host = host;
	}

	/** Advances the battle state machine by the time since the last frame. */
	void update(double deltaTime) {
		host.phaseTime += deltaTime;
		this.advancePhase(deltaTime);

		host.rounds.updateTension(deltaTime);
		host.announcer.tick(deltaTime);
		host.director.updateCamera(deltaTime);
		host.clock += deltaTime * host.effectRate;
		this.updateSurfaces();
		this.updateAudio(deltaTime);
		this.updateEffects(deltaTime);
	}

	/** What the current phase does this frame, and when its time is over */
	void advancePhase(double deltaTime) {
		switch (host.phase) {
		case LOADING -> {
		}
		case SHOWCASE -> {
			if (host.versus) {
				if (host.phaseTime > VERSUS_SECONDS) {
					host.showcase.endShowcase();
				}
			} else {
				host.showcase.turntable();
				if (host.phaseTime > SHOWCASE_SECONDS) {
					host.showcase.showcaseRobot(host.showcaseOrder + 1);
				}
			}
		}
		case FLYBY -> {
			if (host.phaseTime > host.rounds.flyByHold() + FLYBY_SECONDS) {
				host.rounds.endFlyBy();
			}
		}
		case INTRO -> host.rounds.updateIntro();
		case PLAYING -> host.rounds.updatePlaying(deltaTime);
		case FREEZE -> {
			if (host.phaseTime > FREEZE_SECONDS) {
				host.showcase.startReplay();
			}
		}
		case REPLAY -> host.rounds.updateReplay(deltaTime);
		case ROUND_END -> host.rounds.updateRoundEnd();
		case FINISHED -> {
		}
		}
	}

	/** The lava breathes and its flows slide */
	void updateSurfaces() {
	if (host.lava != null) {
		host.lava.setDiffuseColor(LAVA_DIM.interpolate(LAVA_FULL, 0.5 + 0.5 * Math.sin(host.clock * 2.1) * Math.sin(host.clock * 0.7 + 1)));
	}
	if (!host.flows.isEmpty()) {
		host.lavaFlows.updateFlows();
	}
	}

	/** The ears of the sound are the camera; each robot makes its machine sound */
	void updateAudio(double deltaTime) {
	// The ears are the camera: the sounds come from the side of the screen they are on
	host.audio.setListener(host.eye[0], host.eye[2], host.look[0] - host.eye[0], host.look[2] - host.eye[2]);
	for (Map.Entry<Integer, RobotView> entry : host.robots.entrySet()) {
		RobotView robotView = entry.getValue();
		robotView.roll(deltaTime);
		double[] atValue = robotView.position();
		host.audio.setMachine(entry.getKey(), this.heard(robotView.treadSpeed), this.heard(robotView.turretSpeed), atValue[0], atValue[2]);
	}
	}

	/**
	 * The machines are heard only while the battle goes on: the tanks turning on the
	 * stage of the presentation, or put at their places for the start, make no sound
	 */
	double heard(double speed) {
		return switch (host.phase) {
		case PLAYING, FREEZE, REPLAY -> speed;
		default -> 0;
		};
	}

	/** The fire, the sunbeams, the neon and the ice, the critters, the balloons and the photos */
	void updateEffects(double deltaTime) {
	host.fire.update(deltaTime * host.effectRate);
	host.jungle.updateSunbeams(deltaTime);
	host.eden.update(deltaTime);
	host.neon.update(deltaTime);
	host.ice.update(deltaTime);
	if (host.critters != null) {
		host.critters.update(deltaTime);
	}
	host.director.placeBalloons();
	host.photographer.flushPhotos(deltaTime);
	}
}
