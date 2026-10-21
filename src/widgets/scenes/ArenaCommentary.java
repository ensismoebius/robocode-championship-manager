package widgets.scenes;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import static widgets.scenes.BattleArena3D.PLAYER_COLORS;
import static widgets.scenes.BattleArena3D.toCss;
import utils.robocode.BattleRecording;
import widgets.Theme;
import widgets.scenes.BattleArena3D.Phase;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The commentator of the battle arena: the presenter of each event, the big
 * callouts on the side of the robots and the comments at the bottom of the screen.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaCommentary {

	private final BattleArena3D host;

	/** Creates the commentary of the arena: the presenter and the queue of comments. */
	ArenaCommentary(BattleArena3D host) {
		this.host = host;
	}

	/** How long a comment stays, and how long it may wait to be shown */
	private static final double COMMENT_SECONDS = 3.2;
	/**
	 * The commentary only shows up now and then, always with the presenter of the
	 * event: the more important a comment, the sooner after the last one it may
	 * come and the surer it is to come. The ones that do not pass are not told.
	 */
	private static final double[] COMMENT_COOLDOWN = { 15, 15, 12, 12, 8, 8, 8 };
	private static final double[] COMMENT_CHANCE = { 0, 0.5, 0.7, 0.7, 1, 1, 1 };
	private static final double COMMENT_MAX_WAIT = 4;
	/** Callouts shown at the same time on each side */
	private static final int MAX_CALLOUTS = 3;

	/** The presenter of the event being commented */
	private final Presenter presenter = new Presenter(190);
	private final Label liveTag = new Label("●");
	private final StackPane commentaryLayer = new StackPane();
	private final VBox calloutsLeft = new VBox(8);
	private final VBox calloutsRight = new VBox(8);
	private final VBox calloutsCenter = new VBox(8);
	private final HBox ticker = new HBox(14);
	private final Label commentText = new Label();
	private BattleCommentary.Message comment;
	private double commentAge;
	private double commentClock;
	/** When a presenter showed up for the last time */
	private double lastPresented = -1000;
	private final List<QueuedComment> commentQueue = new ArrayList<QueuedComment>();

	/** A comment waiting its turn, with the time since it was queued. */
	private record QueuedComment(BattleCommentary.Message message, double since) {
	}

	/** Clears the comments waiting their turn, on a new round or a new battle */
	void clearQueue() {
		this.commentQueue.clear();
	}

	/**
	 * Callouts on the side of each player, under the energy bars, and the
	 * commentator at the bottom
	 */
	Node build() {
		this.calloutsLeft.setAlignment(Pos.TOP_LEFT);
		this.calloutsRight.setAlignment(Pos.TOP_RIGHT);
		this.calloutsCenter.setAlignment(Pos.TOP_CENTER);
		for (VBox column : List.of(this.calloutsLeft, this.calloutsRight, this.calloutsCenter)) {
			column.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		}
		StackPane.setAlignment(this.calloutsLeft, Pos.TOP_LEFT);
		StackPane.setMargin(this.calloutsLeft, new Insets(130, 0, 0, 40));
		StackPane.setAlignment(this.calloutsRight, Pos.TOP_RIGHT);
		StackPane.setMargin(this.calloutsRight, new Insets(130, 40, 0, 0));
		StackPane.setAlignment(this.calloutsCenter, Pos.TOP_CENTER);
		StackPane.setMargin(this.calloutsCenter, new Insets(130, 0, 0, 0));

		Label tag = this.liveTag;
		tag.getStyleClass().add("arena-comment-tag");
		this.commentText.getStyleClass().add("arena-comment-text");
		this.commentText.setWrapText(true);
		this.ticker.getChildren().addAll(tag, this.commentText);
		this.ticker.setAlignment(Pos.CENTER_LEFT);
		this.ticker.getStyleClass().add("arena-comment");
		this.ticker.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		this.ticker.setOpacity(0);
		// The presenter stands at the left, the comment beside it
		StackPane.setAlignment(this.presenter, Pos.BOTTOM_LEFT);
		StackPane.setMargin(this.presenter, new Insets(0, 0, 40, 28));
		StackPane.setAlignment(this.ticker, Pos.BOTTOM_LEFT);
		StackPane.setMargin(this.ticker, new Insets(0, 0, 64, 250));
		this.ticker.maxWidthProperty().bind(host.widthProperty().subtract(300).multiply(0.9));

		this.commentaryLayer.getChildren().addAll(this.presenter, this.calloutsLeft, this.calloutsRight, this.calloutsCenter, this.ticker);
		this.commentaryLayer.setMouseTransparent(true);
		this.commentaryLayer.setPickOnBounds(false);
		return this.commentaryLayer;
	}

	/**
	 * Shows what the commentary said, nothing during the replay: it was said
	 * already
	 */
	void say(List<BattleCommentary.Message> messages) {
		if (host.phase == Phase.REPLAY) {
			return;
		}
		for (BattleCommentary.Message message : messages) {
			if (message.callout()) {
				this.callout(message);
			} else {
				this.queueComment(message);
			}
		}
	}

	/**
	 * Position of the robot in the battle: 0 is on the left, 1 on the right
	 */
	private int orderOf(int robot) {
		List<BattleRecording.Robot> list = host.recording.getRobots();
		for (int index = 0; index < list.size(); index++) {
			if (list.get(index).getIndex() == robot) {
				return index;
			}
		}
		return -1;
	}

	/** The colour of a comment, by its tone. */
	private Color toneColor(BattleCommentary.Message message) {
		return switch (message.tone()) {
		case DANGER -> Color.web("#ff2a2a");
		case GOLD -> Theme.ACCENT;
		case NEUTRAL -> Color.web("#eef2fa");
		case PLAYER -> {
			int order = this.orderOf(message.robot());
			yield order < 0 ? Theme.ACCENT : PLAYER_COLORS[order % PLAYER_COLORS.length];
		}
		};
	}

	/**
	 * A big word that pops on the side of the robot and goes away
	 */
	private void callout(BattleCommentary.Message message) {
		int order = this.orderOf(message.robot());
		VBox column = order == 0 ? this.calloutsLeft : order == 1 ? this.calloutsRight : this.calloutsCenter;
		Color color = this.toneColor(message);

		Label label = new Label(message.text());
		label.getStyleClass().add("arena-callout");
		label.setTextFill(color);
		label.setEffect(Theme.glow(color, 16));
		label.setRotate(order == 0 ? -4 : order == 1 ? 4 : 0);
		// The same callout again pops again, it is not repeated
		column.getChildren().removeIf(node -> node instanceof Label other && other.getText().equals(message.text()));
		if (column.getChildren().size() >= MAX_CALLOUTS) {
			column.getChildren().remove(0);
		}
		column.getChildren().add(label);

		ScaleTransition pop = new ScaleTransition(Duration.millis(280), label);
		pop.setFromX(2.2);
		pop.setFromY(2.2);
		pop.setToX(1);
		pop.setToY(1);
		pop.setInterpolator(Theme.OVERSHOOT);
		FadeTransition fade = new FadeTransition(Duration.millis(400), label);
		fade.setToValue(0);
		TranslateTransition rise = new TranslateTransition(Duration.millis(400), label);
		rise.setByY(-18);
		SequentialTransition life = new SequentialTransition(pop, new PauseTransition(Duration.millis(message.tone() == BattleCommentary.Tone.DANGER ? 2200 : 1500)),
				new ParallelTransition(fade, rise));
		life.setOnFinished(event -> column.getChildren().remove(label));
		life.play();
		if (message.tone() == BattleCommentary.Tone.DANGER) {
			Timeline blink = new Timeline(new KeyFrame(Duration.millis(160), event -> label.setVisible(!label.isVisible())));
			blink.setCycleCount(12);
			blink.setOnFinished(event -> label.setVisible(true));
			blink.play();
		}
	}

	/**
	 * A more important comment cuts the current one, the others wait their turn
	 */
	private void queueComment(BattleCommentary.Message message) {
		if (this.comment == null || message.priority() > this.comment.priority() && this.commentAge > 0.6) {
			// If it is not time for it, it is dropped
			this.showComment(message);
			return;
		}
		this.commentQueue.add(new QueuedComment(message, this.commentClock));
		this.commentQueue.sort((first, second) -> Integer.compare(second.message().priority(), first.message().priority()));
		while (this.commentQueue.size() > 3) {
			this.commentQueue.remove(this.commentQueue.size() - 1);
		}
	}

	/**
	 * The presenter of a moment of the game (a round that begins, a knock out)
	 * says a line
	 */
	void announce(String text, int priority) {
		this.queueComment(new BattleCommentary.Message(false, text, -1, BattleCommentary.Tone.NEUTRAL, priority, Topic.SHOW));
	}

	/**
	 * Whether it is time for this comment, by its importance and the time since
	 * the last one
	 */
	private boolean commentAllowed(BattleCommentary.Message message) {
		int level = Math.max(0, Math.min(COMMENT_COOLDOWN.length - 1, message.priority()));
		return this.commentClock - this.lastPresented >= COMMENT_COOLDOWN[level] && host.random.nextDouble() < COMMENT_CHANCE[level];
	}

	/**
	 * Shows the comment with the presenter of its topic
	 *
	 * @return false if it was not time for it and it was dropped
	 */
	private boolean showComment(BattleCommentary.Message message) {
		if (!this.commentAllowed(message)) {
			return false;
		}
		this.lastPresented = this.commentClock;
		this.presenter.present(message.topic(), COMMENT_SECONDS + 0.4);
		this.liveTag.setText("● " + message.topic().presenter.toUpperCase());
		this.comment = message;
		this.commentAge = 0;
		this.commentText.setText(message.text());
		Color color = message.robot() < 0 ? Theme.ACCENT : this.toneColor(message);
		this.ticker.setStyle("-fx-border-color: " + toCss(color) + ";");
		this.ticker.setOpacity(1);
		TranslateTransition slide = new TranslateTransition(Duration.millis(220), this.ticker);
		slide.setFromX(-60);
		slide.setToX(0);
		slide.setInterpolator(Interpolator.EASE_OUT);
		slide.play();
		return true;
	}

	/** Moves the commentary by the time that passed: the presenter, the comment and the queue. */
	void tick(double deltaTime) {
		this.commentClock += deltaTime;
		this.presenter.tick(deltaTime);
		if (this.comment != null) {
			this.commentAge += deltaTime;
			if (this.commentAge > COMMENT_SECONDS) {
				this.comment = null;
				FadeTransition fade = new FadeTransition(Duration.millis(250), this.ticker);
				fade.setToValue(0);
				fade.play();
			}
		}
		// Old news are not told
		this.commentQueue.removeIf(queued -> this.commentClock - queued.since() > COMMENT_MAX_WAIT);
		if (this.comment == null && !this.commentQueue.isEmpty() && host.phase != Phase.REPLAY) {
			// Not time for it: dropped, the next one may have its turn
			this.showComment(this.commentQueue.remove(0).message());
		}
	}
}
