package widgets.scenes;

import static widgets.scenes.BattleArena3D.FREEZE_SECONDS;
import static widgets.scenes.BattleArena3D.findBullet;
import static widgets.scenes.BattleArena3D.toCss;
import widgets.scenes.BattleArena3D.Phase;
import widgets.scenes.BattleArena3D.Shot;

import java.io.ByteArrayInputStream;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import model.Author;
import model.Opponent;
import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.Turn;
import utils.robocode.BattleRecording;
import widgets.Sfx;
import widgets.Sound;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The presentation of the battle arena: the showcase of each robot before the
 * fight with its card and the turntable, the versus, and the replay of the
 * recorded battle, frozen on the turns that matter.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaShowcase {

	/** Turns of the replay before and after the death */
	private static final int REPLAY_BEFORE = 30;
	private static final int REPLAY_AFTER = 10;
	private int deadRobot = -1;
	private int replayFrom;

	private final BattleArena3D host;

	ArenaShowcase(BattleArena3D host) {
		this.host = host;
	}

	// --------------------------------------------------------------- showcase

	/**
	 * Before the first round each robot is presented alone in the arena, like
	 * the fighters of a fighting game, then both face each other
	 */
	void startShowcase() {
		host.phase = Phase.SHOWCASE;
		host.hudTop.setVisible(false);
		host.hud.showLetterbox(true, false);
		host.audio.setMusicOn(false);
		this.showcaseRobot(0);
	}

	/**
	 * Presents the robot at this position of the battle, or the versus after the
	 * last one
	 */
	void showcaseRobot(int order) {
		host.stopShowcaseMusic();
		List<BattleRecording.Robot> list = host.recording.getRobots();
		if (order >= list.size()) {
			this.startVersus();
			return;
		}
		host.showcaseOrder = order;
		host.phaseTime = 0;

		BattleRecording.Robot robot = list.get(order);
		RobotView view = host.robots.get(robot.getIndex());
		for (RobotView other : host.robots.values()) {
			other.root.setVisible(other == view);
		}
		host.shotSubject = robot.getIndex();
		host.shotSeed = host.random.nextDouble() * Math.PI * 2;
		this.turntable();

		// A spotlight in the color of the player
		host.flashLight.setTranslateX(0);
		host.flashLight.setTranslateY(-160);
		host.flashLight.setTranslateZ(0);
		host.flashLight.setColor(view.playerColor.deriveColor(0, 0.8, 0.9, 1));

		Opponent opponent = host.opponentOf(robot);
		Node card = this.showcaseCard(robot, opponent, view.playerColor, order);
		host.showcaseLayer.getChildren().setAll(card);
		TranslateTransition slide = new TranslateTransition(Duration.millis(550), card);
		double side = order % 2 == 0 ? -1 : 1;
		slide.setFromX(side * Math.max(1200, host.getWidth()));
		slide.setToX(0);
		slide.setDelay(Duration.millis(700));
		slide.setInterpolator(Interpolator.EASE_OUT);
		slide.setOnFinished(event -> Sfx.slam());
		card.setTranslateX(slide.getFromX());
		slide.play();

		host.audio.whoosh();
		host.showcaseMusic = opponent == null ? null : Sound.fromBytes(opponent.getMusic(), opponent.getFullName());
		if (host.showcaseMusic != null) {
			host.showcaseMusic.play();
		}
		host.director.cut();
	}

	/**
	 * Both robots where the battle starts, and the VS
	 */
	void startVersus() {
		host.versus = true;
		host.phaseTime = 0;
		host.showcaseLayer.getChildren().clear();
		host.flashLight.setColor(Color.BLACK);
		for (RobotView view : host.robots.values()) {
			view.root.setVisible(true);
		}
		host.turnPosition = 0;
		host.lastTurnProcessed = -1;
		host.rounds.showTurn(host.turns());

		String names = String.join("  ×  ", host.recording.getRobots().stream().map(r -> host.robots.get(r.getIndex()).displayName.toUpperCase()).toList());
		host.hud.showBanner("VS", names + "\n\nARENA: " + host.style.label.toUpperCase());
		String versusLine = "Frente a frente: " + names.replace("  ×  ", " contra ") + "!";
		host.announcer.announce(switch (host.stageImportance) {
		case SEMIFINAL -> "Atenção, é uma SEMIFINAL! " + versusLine + " Quem vence vai para a final!";
		case FINAL -> "Atenção, é a FINAL! " + versusLine + " Quem vencer leva a glória!";
		default -> versusLine;
		}, 5);
		host.audio.whoosh();
		Sfx.slam();
		host.shot = Shot.WIDE;
		host.shotTime = 0;
		host.director.cut();
	}

	void endShowcase() {
		host.stopShowcaseMusic();
		host.versus = false;
		host.showcaseLayer.getChildren().clear();
		host.flashLight.setColor(Color.BLACK);
		for (RobotView view : host.robots.values()) {
			view.root.setVisible(true);
		}
		host.hud.hideBanner();
		host.stageLabel.setVisible(false);
		host.stageLabel.setManaged(false);
		host.hud.showLetterbox(false);
		host.rounds.startRound(0);
	}

	/**
	 * The presented robot turns slowly in the center of the arena, aiming its gun
	 * and spinning the radar
	 */
	void turntable() {
		RobotView view = host.robots.get(host.shotSubject);
		if (view == null) {
			return;
		}
		double t = host.phaseTime;
		double body = host.shotSeed + t * 0.35;
		view.place(0, 0, body, body + 0.7 * Math.sin(t * 1.4), t * 5);
	}

	/**
	 * Comes down from above to a low hero angle, circling and getting closer. The
	 * robot stays on the side opposite to its card.
	 */
	void showcaseShot(double[] robot) {
		double t = host.phaseTime;
		double crane = Math.max(0, 1 - t / 2.5);
		crane = crane * crane;
		double angle = host.shotSeed + Math.PI - t * 0.3;
		double radius = 175 - Math.min(30, t * 4.5) + 120 * crane;
		double ex = robot[0] + Math.cos(angle) * radius;
		double ez = robot[2] + Math.sin(angle) * radius;
		double ey = -30 - 150 * crane;

		// Screen right of the camera, the look point moves to the side of the card
		double fx = robot[0] - ex;
		double fz = robot[2] - ez;
		double length = Math.max(1, Math.hypot(fx, fz));
		double rightX = fz / length;
		double rightZ = -fx / length;
		double offset = (host.showcaseOrder % 2 == 0 ? -1 : 1) * 34;
		host.director.setShot(ex, ey, ez, robot[0] + rightX * offset, -20, robot[2] + rightZ * offset, 40, 6);
		host.rollTarget = (host.showcaseOrder % 2 == 0 ? 1 : -1) * 2.5 * crane;
	}

	/**
	 * Logo, name, team, battle cry and authors of the robot
	 */
	Node showcaseCard(BattleRecording.Robot robot, Opponent opponent, Color color, int order) {
		boolean left = order % 2 == 0;
		Pos alignment = left ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT;
		VBox card = new VBox(10);
		card.setAlignment(alignment);

		Label tag = new Label("JOGADOR " + (order + 1));
		tag.getStyleClass().add("showcase-section");
		tag.setTextFill(color);
		card.getChildren().add(tag);

		if (opponent != null && opponent.getImage() != null) {
			ImageView logo = new ImageView(new Image(new ByteArrayInputStream(opponent.getImage()), 260, 180, true, true));
			card.getChildren().add(logo);
		}

		Label name = new Label(opponent == null ? robot.getName() : opponent.getName().toUpperCase());
		name.setWrapText(true);
		name.getStyleClass().add("showcase-name");
		name.setTextFill(color);
		name.setEffect(Theme.glow(color, 18));
		card.getChildren().add(name);

		if (opponent != null) {
			Label team = new Label("Time " + opponent.getTeam());
			team.getStyleClass().add("showcase-team");
			card.getChildren().add(team);

			if (opponent.getBattleCry() != null && !opponent.getBattleCry().isBlank()) {
				Label cry = new Label("“" + opponent.getBattleCry() + "”");
				cry.setWrapText(true);
				cry.getStyleClass().add("showcase-cry");
				card.getChildren().add(cry);
			}

			if (!opponent.getAuthors().isEmpty()) {
				Label section = new Label("EQUIPE");
				section.getStyleClass().add("showcase-section");
				card.getChildren().add(section);
				for (Author author : opponent.getAuthors()) {
					Label label = new Label("▸ " + author.getName());
					label.getStyleClass().add("showcase-author");
					card.getChildren().add(label);
				}
			}
		}
		for (Node node : card.getChildren()) {
			if (node instanceof Label label) {
				label.setAlignment(alignment);
				label.setTextAlignment(left ? javafx.scene.text.TextAlignment.LEFT : javafx.scene.text.TextAlignment.RIGHT);
			}
		}

		card.setStyle("-fx-background-color: rgba(8, 10, 20, 0.72); -fx-padding: 28 36 28 36; -fx-border-color: " + toCss(color) + "; -fx-border-width: "
				+ (left ? "0 0 0 6" : "0 6 0 0") + ";");
		card.maxWidthProperty().bind(host.widthProperty().multiply(0.36));
		card.setMaxHeight(Region.USE_PREF_SIZE);
		StackPane.setAlignment(card, alignment);
		StackPane.setMargin(card, new Insets(0, 70, 0, 70));
		return card;
	}

	// ----------------------------------------------------------------- replay

	/**
	 * A robot died: the action stops for a moment, then the last second is
	 * shown again in slow motion
	 */
	void freezeForReplay(int robot, int turn) {
		host.phase = Phase.FREEZE;
		host.phaseTime = 0;
		this.deadRobot = robot;
		host.deathTurn = turn;
		this.replayFrom = Math.max(0, turn - REPLAY_BEFORE);
		host.replayTo = Math.min(host.turns().size() - 1, turn + REPLAY_AFTER);

		// The bullet that killed it, if one did
		host.fatalBullet = -1;
		host.impactTurn = -1;
		List<Turn> turns = host.turns();
		// (a bullet stays in the records for a while after its hit, exploding: the
		// hit is the turn it changes to HIT_VICTIM, the last one before the death)
		for (int t = Math.min(turns.size() - 1, turn + 1); t >= Math.max(1, turn - 3) && host.fatalBullet < 0; t--) {
			for (Bullet bullet : turns.get(t).bullets()) {
				Bullet before = findBullet(turns.get(t - 1), bullet.id());
				if (bullet.state() == BattleRecording.BULLET_HIT_VICTIM && bullet.victim() == robot && (before == null || !before.isHit())) {
					host.fatalBullet = bullet.id();
					host.impactTurn = t;
					break;
				}
			}
		}

		// Holds on the explosion, far enough to see all of it
		host.shot = Shot.IMPACT;
		host.shotSubject = robot;
		host.shotTime = 0;
		host.shotDuration = FREEZE_SECONDS + 1;
		host.closeDistance = 190;
		host.shotSide = host.random.nextBoolean() ? 1 : -1;
	}

	void startReplay() {
		host.phase = Phase.REPLAY;
		host.phaseTime = 0;
		host.replayed = true;
		host.effectRate = 0.3;
		host.audio.whoosh();
		host.audio.setSlowMotion(true);
		host.hud.showLetterbox(true);

		// Back to before the death
		host.turnPosition = this.replayFrom;
		host.lastTurnProcessed = this.replayFrom - 1;
		host.bulletsSeen.clear();
		host.bulletsExploded.clear();
		host.shots.clearBullets();
		host.effects.getChildren().clear();
		for (RobotView view : host.robots.values()) {
			view.reset();
		}
		// Bullets already flying when the replay starts are not new shots, and the
		// ones still exploding there hit before it
		if (this.replayFrom > 0) {
			for (Bullet bullet : host.turns().get(this.replayFrom - 1).bullets()) {
				host.bulletsSeen.add(bullet.id());
				if (bullet.isHit()) {
					host.bulletsExploded.add(bullet.id());
				}
			}
		}

		host.shot = Shot.FATAL;
		host.shotSubject = this.deadRobot;
		host.shotTime = 0;
		host.shotDuration = 1000;
		host.shotSide = host.random.nextBoolean() ? 1 : -1;
		host.rounds.showTurn(host.turns());
		host.director.cut();
	}

	void endReplay() {
		if (host.phase != Phase.REPLAY && host.phase != Phase.FREEZE) {
			return;
		}
		host.replayed = true;
		host.effectRate = 1;
		for (RobotView view : host.robots.values()) {
			view.running.forEach(animation -> animation.setRate(1));
		}
		host.audio.setSlowMotion(false);
		host.hud.showLetterbox(false);

		// Goes on from the end of the replay, with everything in its final state
		host.turnPosition = Math.max(host.turnPosition, host.replayTo);
		List<Turn> turns = host.turns();
		host.rounds.showTurn(turns);
		RobotView dead = host.robots.get(this.deadRobot);
		if (dead != null && !dead.dead) {
			dead.destroy();
		}
		host.phase = Phase.PLAYING;
		host.phaseTime = 0;
		host.director.pickShot(true);
	}
}
