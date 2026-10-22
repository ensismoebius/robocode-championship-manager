package widgets.scenes;

import static widgets.scenes.BattleArena3D.COUNTDOWN_SECONDS;
import static widgets.scenes.BattleArena3D.SPEEDS;
import static widgets.scenes.ArenaMaterials.energy;
import static widgets.scenes.BattleArena3D.find;
import static widgets.scenes.ArenaMaterials.grow;
import static widgets.scenes.BattleArena3D.lerp;
import static widgets.scenes.BattleArena3D.lerpAngle;
import widgets.scenes.BattleArena3D.Phase;
import widgets.scenes.BattleArena3D.Shot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.configuration.Configuration;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;
import widgets.Sfx;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The rounds of the battle arena: the start of each round with its intro and
 * fly by, the play of the turns, the end of the round with its tension and the
 * replay of the battle. The shots of the turns are shown by the bullets.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaRounds {

	/** Robocode's default speed, turns per second */
	private static final double TPS = 30;
	/** The fly by starts looking at a critter for this long before it goes on */
	private static final double FLYBY_FOCUS_SECONDS = 2;
	private static final double INTRO_SECONDS = COUNTDOWN_SECONDS + 1.1;
	private static final double ROUND_END_SECONDS = 2.8;
	/** The headline of a special battle, after the last round */
	private static final double HEADLINE_SECONDS = 3.4;
	/** Slow motion speeds: before the impact, close to it, after it */
	private static final double REPLAY_SPEED = 0.28;
	private static final double REPLAY_IMPACT_SPEED = 0.07;
	private static final double REPLAY_AFTER_SPEED = 0.22;
	/** Shown when the round starts, configurable */
	private final String fightText = Configuration.getFightText();
	private boolean fightShown = false;
	/** Where the fly by ends: the camera at the start of the battle */
	private final double[] flyEyeEnd = new double[3];
	/** The glacial sea of the ice arena: its middle and its radius, 0 when there is none */
	private final double[] flyLookEnd = new double[3];
	private double flyFovEnd;
	private double flyMirrorX = 1;
	private double flyMirrorZ = 1;
	/** The number of the countdown on the screen, 0 if none yet */
	private int countdown;
	private String roundTitle = "";
	private String roundSubtitle;
	private boolean headlineShown = false;

	private final BattleArena3D host;

	/** Creates the round flow of the arena: the flyby, the intro, the battle and the end of each round. */
	ArenaRounds(BattleArena3D host) {
		this.host = host;
	}

	/** The countdown before the round: the banner shows the numbers, then the fight starts */
	void updateIntro() {
		int remaining = (int) Math.ceil(COUNTDOWN_SECONDS - host.phaseTime);
		if (remaining >= 1 && remaining != this.countdown) {
			this.countdown = remaining;
			host.hud.showBanner(String.valueOf(remaining), this.roundTitle + (this.roundSubtitle == null ? "" : " - " + this.roundSubtitle));
			host.audio.countdown(remaining == 1);
		}
		if (host.phaseTime > COUNTDOWN_SECONDS && !this.fightShown) {
			this.fightShown = true;
			host.hud.showBanner(this.fightText, null);
			host.audio.fight();
			host.audio.setMusicOn(true);
		}
		if (host.phaseTime > INTRO_SECONDS) {
			host.hud.hideBanner();
			host.phase = Phase.PLAYING;
		}
	}

	/** The round is played: the turns shown as the battle goes on */
	void updatePlaying(double deltaTime) {
		if (!host.paused) {
			host.turnPosition += deltaTime * TPS * SPEEDS[host.speed];
		}
		List<Turn> turns = host.turns();
		if (host.turnPosition >= turns.size() - 1) {
			host.turnPosition = turns.size() - 1;
			this.showTurn(turns);
			if (host.phase == Phase.PLAYING) {
				this.endRound(turns);
			}
		} else {
			this.showTurn(turns);
		}
	}

	/** The replay of the moments before a robot is destroyed, slowed down */
	void updateReplay(double deltaTime) {
		int turn = (int) host.turnPosition;
		double rate = turn >= host.deathTurn ? REPLAY_AFTER_SPEED : host.impactTurn >= 0 && host.impactTurn - turn <= 3 ? REPLAY_IMPACT_SPEED : REPLAY_SPEED;
		host.turnPosition += deltaTime * TPS * rate;
		if (host.turnPosition >= host.replayTo) {
			host.turnPosition = host.replayTo;
			this.showTurn(host.turns());
			host.showcase.endReplay();
		} else {
			this.showTurn(host.turns());
		}
	}

	/** The round is over: the next round, the headline of a special battle, or the end */
	void updateRoundEnd() {
		if (host.phaseTime > (this.headlineShown ? HEADLINE_SECONDS : ROUND_END_SECONDS)) {
			if (host.round + 1 < host.recording.getRounds().size()) {
				this.startRound(host.round + 1);
			} else if (host.headline != null && !this.headlineShown) {
				// A special battle gets its headline
				this.headlineShown = true;
				host.phaseTime = 0;
				host.hud.showBanner(host.headline.title(), host.headline.subtitle());
				host.announcer.announce(host.headline.subtitle(), 6);
				host.banner.setStyle("-fx-font-size: 46px;");
				Sfx.fanfare();
			} else {
				host.controls.finish();
			}
		}
	}

	/** Starts a round: its turns are played from the beginning. */
	void startRound(int round) {
		this.resetRound(round);

		// Skips empty rounds
		List<Turn> turns = host.turns();
		if (turns.isEmpty()) {
			host.phase = Phase.ROUND_END;
			host.phaseTime = ROUND_END_SECONDS;
			return;
		}
		this.showTurn(turns);
		this.craneShot(round);
	}

	/** Puts everything of the arena back to the start of the round, and the robots where they begin */
	private void resetRound(int round) {
		host.round = round;
		host.turnPosition = 0;
		host.lastTurnProcessed = -1;
		host.replayed = false;
		host.hitPhotoTaken = false;
		host.bulletsSeen.clear();
		host.bulletsExploded.clear();
		host.lowEnergyWarned.clear();
		host.shots.clearBullets();
		host.effects.getChildren().clear();
		host.fire.clear();
		host.forceField.resetFence();
		if (host.commentary != null) {
			host.commentary.startRound();
			if (round > 0 && round + 1 == host.recording.getRounds().size()) {
				host.commentary.lastRoundStarts();
			}
		}
		host.announcer.clearQueue();
		for (RobotView view : host.robots.values()) {
			view.reset();
		}
	}

	/** The camera at the start of a round: a crane shot down to the arena, and before the first battle, the flight over it */
	private void craneShot(int round) {
		host.shot = Shot.CRANE;
		host.shotTime = 0;
		host.shotDuration = INTRO_SECONDS + 2.5;
		host.shotSeed = host.random.nextDouble() * Math.PI * 2;
		host.director.cut();
		if (round == 0) {
			// Before the first battle the camera flies over the arena first
			this.beginFlyBy();
		} else {
			this.beginIntro();
		}
	}

	/**
	 * The round title, the countdown and the fight
	 */
	void beginIntro() {
		host.phase = Phase.INTRO;
		host.phaseTime = 0;
		this.fightShown = false;
		this.countdown = 0;
		this.roundTitle = "ROUND " + (host.round + 1);
		boolean decisive = host.round + 1 == host.recording.getRounds().size() && host.round > 0;
		this.roundSubtitle = decisive ? "ROUND DECISIVO" : null;
		host.hud.showBanner(this.roundTitle, this.roundSubtitle);
		host.audio.roundCall();
		host.audio.setMusicOn(true);
		host.announcer.announce(decisive ? "Round decisivo! Quem vencer leva a batalha!" : "Round " + (host.round + 1) + "! Preparem os motores!", 5);
		host.hud.updateInfo();
	}

	/**
	 * The camera flies over the arena, low and fast past the walls, and ends
	 * where the battle starts
	 */
	void beginFlyBy() {
		this.flyMirrorX = host.random.nextBoolean() ? 1 : -1;
		this.flyMirrorZ = host.random.nextBoolean() ? 1 : -1;
		host.phase = Phase.FLYBY;
		host.phaseTime = 0;
		host.hud.hideBanner();
		host.hud.showLetterbox(true, false);
		host.audio.setMusicOn(true);
		// The fly by ends framing both robots, where the battle starts
		RobotView subject = host.robots.get(host.shotSubject);
		RobotView other = host.robots.values().stream().filter(vCoord -> vCoord != subject).findFirst().orElse(subject);
		double[] first = subject == null ? new double[] { 0, 0, 0 } : subject.position();
		host.director.frameFighters(0, first, other == null ? first : other.position());
		System.arraycopy(host.eyeTarget, 0, this.flyEyeEnd, 0, 3);
		System.arraycopy(host.lookTarget, 0, this.flyLookEnd, 0, 3);
		this.flyFovEnd = host.fovTarget;
		// The first look is at a critter, from outside the field: the fly by starts on it
		host.flyFocusSource = host.critters == null ? null : host.critters.track(-0.8 * host.fieldWidth * this.flyMirrorX, -host.fieldHeight * this.flyMirrorZ);
		host.flyFocus = host.flyFocusSource == null ? null : host.flyFocusSource.get();
		// Starts at the first point of the path, no sliding to it
		this.flyByPose(0);
		System.arraycopy(host.eyeTarget, 0, host.eye, 0, 3);
		System.arraycopy(host.lookTarget, 0, host.look, 0, 3);
		host.fov = host.fovTarget;
		host.hud.updateInfo();
	}

	/** How long the fly by stays on the critter before the trip: nothing if there is no critter */
	double flyByHold() {
		return host.flyFocusSource == null ? 0 : FLYBY_FOCUS_SECONDS;
	}

	/** Ends the flyby: hides the letterbox and starts the intro. */
	void endFlyBy() {
		host.hud.showLetterbox(false);
		this.beginIntro();
	}

	/**
	 * Puts the camera where the fly by is at the moment: through points around
	 * the arena, low, that end framing both robots, where the battle starts
	 */
	void flyByPose(double progress) {
		double width = host.fieldWidth;
		double height = host.fieldHeight;
		double mxValue = this.flyMirrorX;
		double mzValue = this.flyMirrorZ;
		double[][] eyes = { { -0.8 * width * mxValue, -120, -1.0 * height * mzValue }, { -0.05 * width * mxValue, -50, -0.85 * height * mzValue }, { 0.85 * width * mxValue, -75, -0.1 * height * mzValue }, this.flyEyeEnd };
		double[][] looks = { { 0.1 * width * mxValue, -40, 0.2 * height * mzValue }, { 0.25 * width * mxValue, -25, 0.1 * height * mzValue }, { -0.2 * width * mxValue, -20, 0 }, this.flyLookEnd };
		if (host.flyFocus != null) {
			// Starts looking at the critter, from its outer side, a little above it
			double fxValue = host.flyFocus[0];
			double fzValue = host.flyFocus[2];
			double length = Math.max(1, Math.hypot(fxValue, fzValue));
			eyes[0] = new double[] { fxValue + fxValue / length * 45, host.flyFocus[1] - 22, fzValue + fzValue / length * 45 };
			looks[0] = host.flyFocus;
		}
		// Slow at the start and at the end, quick in the middle
		double smooth = progress * progress * (3 - 2 * progress);
		double uCoord = 0.3 * progress + 0.7 * smooth;
		catmull(eyes, uCoord, host.eyeTarget);
		catmull(looks, uCoord, host.lookTarget);
		host.fovTarget = 62 + (this.flyFovEnd - 62) * smooth;
		host.rollTarget = 3 * Math.sin(progress * Math.PI * 2) * (1 - smooth);
		host.follow = 40;
	}

	/**
	 * A point along a smooth curve that goes through all the points, u from 0 to 1
	 */
	static void catmull(double[][] points, double uCoord, double[] out) {
		int segments = points.length - 1;
		double position = Math.max(0, Math.min(1, uCoord)) * segments;
		int index = Math.min(segments - 1, (int) position);
		double time = position - index;
		double[] p0Value = points[Math.max(0, index - 1)];
		double[] p1Value = points[index];
		double[] p2Value = points[index + 1];
		double[] p3Value = points[Math.min(segments, index + 2)];
		for (int deepIndex = 0; deepIndex < 3; deepIndex++) {
			out[deepIndex] = 0.5 * (2 * p1Value[deepIndex] + (p2Value[deepIndex] - p0Value[deepIndex]) * time + (2 * p0Value[deepIndex] - 5 * p1Value[deepIndex] + 4 * p2Value[deepIndex] - p3Value[deepIndex]) * time * time + (3 * p1Value[deepIndex] - p0Value[deepIndex] - 3 * p2Value[deepIndex] + p3Value[deepIndex]) * time * time * time);
		}
	}

	/** Ends a round: the phase goes to the end of the round, and its turns are the ones given. */
	void endRound(List<Turn> turns) {
		host.phase = Phase.ROUND_END;
		host.phaseTime = 0;

		List<RobotState> alive = turns.get(turns.size() - 1).robots().stream().filter(row -> !row.isDead()).toList();
		host.announcer.say(host.commentary.roundEnd(alive.size() == 1 ? alive.get(0).index() : -1));
		if (host.round + 1 == host.recording.getRounds().size()) {
			host.headline = host.commentary.battleEnd();
		}
		if (alive.size() == 1) {
			int winner = alive.get(0).index();
			host.roundWins.merge(winner, 1, Integer::sum);
			RobotView view = host.robots.get(winner);
			host.hud.showBanner("K.O.!", (view == null ? "" : view.displayName) + " vence o round");
			host.announcer.announce("K.O.! " + (view == null ? "" : view.displayName) + " vence o round!", 5);
			host.audio.knockOut();
			// The winner, from close
			host.shot = Shot.CLOSE;
			host.shotSubject = winner;
			host.shotTime = 0;
			host.shotDuration = ROUND_END_SECONDS + 1;
			host.shotSide = host.random.nextBoolean() ? 1 : -1;
			host.closeDistance = 110;
		} else {
			host.hud.showBanner("FIM DO ROUND", null);
		}
		host.audio.setMusicOn(false);
		host.hud.updatePips();
	}

	/**
	 * How tense the fight is: low energy, the end of the round, the deciding
	 * round and recent hits make the music grow
	 */
	void updateTension(double deltaTime) {
		host.hitTension *= Math.exp(-0.5 * deltaTime);
		if (host.recording == null || host.phase == Phase.LOADING) {
			host.audio.setIntensity(0);
			return;
		}
		double lowest = 100;
		for (RobotView view : host.robots.values()) {
			if (!view.dead) {
				lowest = Math.min(lowest, view.energy);
			}
		}
		double progress = Math.min(1, host.turnPosition / Math.max(1, host.turns().size()));
		boolean deciding = host.round + 1 == host.recording.getRounds().size() && host.recording.getRounds().size() > 1;
		double tension = 0.12 + 0.55 * (1 - Math.max(0, Math.min(100, lowest)) / 100) + 0.15 * progress + (deciding ? 0.15 : 0) + host.hitTension;
		if (host.phase == Phase.INTRO || host.phase == Phase.SHOWCASE || host.phase == Phase.FLYBY) {
			tension = 0.1;
		}
		host.audio.setIntensity(tension);
	}

	/**
	 * Puts everything where it is at the current (fractional) turn and fires the
	 * events of the turns just reached
	 */
	void showTurn(List<Turn> turns) {
		int index = Math.min((int) Math.floor(host.turnPosition), turns.size() - 1);
		double fraction = host.turnPosition - index;
		Turn current = turns.get(index);
		Turn next = turns.get(Math.min(index + 1, turns.size() - 1));

		for (int time = host.lastTurnProcessed + 1; time <= index; time++) {
			host.shots.turnEvents(turns.get(time), time == 0 ? null : turns.get(time - 1), time);
		}
		host.lastTurnProcessed = Math.max(host.lastTurnProcessed, index);

		Map<Integer, Double> energies = new HashMap<Integer, Double>();
		for (RobotState state : current.robots()) {
			RobotState after = find(next.robots(), state.index(), state);
			RobotView view = host.robots.get(state.index());
			if (view == null) {
				continue;
			}
			view.place(host.worldX(lerp(state.x(), after.x(), fraction)), host.worldZ(lerp(state.y(), after.y(), fraction)), lerpAngle(state.bodyHeading(), after.bodyHeading(), fraction),
					lerpAngle(state.gunHeading(), after.gunHeading(), fraction), lerpAngle(state.radarHeading(), after.radarHeading(), fraction));
			double energy = lerp(state.energy(), after.energy(), fraction);
			view.setEnergy(state.isDead() ? 0 : energy);
			energies.put(state.index(), state.isDead() ? 0 : energy);
		}
		host.hud.updateHud(energies);

		host.shots.showBullets(turns, index, fraction);
	}
}
