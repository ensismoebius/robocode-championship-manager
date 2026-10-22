package widgets.scenes;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.util.Duration;
import tournament.Match;
import tournament.Slot;
import tournament.Status;
import widgets.Sfx;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The animation of a finished battle in the {@link BracketViewer}: the result is
 * revealed, the robots go to their next matches and the camera flies to the
 * next battle.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class BracketReplay {

	private final BracketViewer host;
	private SequentialTransition resultAnimation;
	private final List<Runnable> finalStates = new ArrayList<Runnable>();

	/** Creates the replay of a result on the bracket. */
	BracketReplay(BracketViewer host) {
		this.host = host;
	}

	/** A match that moves to its next place in the bracket, with its row and whether it is the winner. */
	private record Arrival(Match match, HBox row, boolean winner) {
	}

	/** Starts the animation of the result of the match with the given code. */
	void start(String code) {
		Match finished = host.bracket.getMatch(code);
		Point2D finishedCenter = host.centerOf(code);
		double[] overview = host.overview();

		// The camera starts showing the whole bracket
		host.moveTo(overview[0], finishedCenter == null ? overview[1] : finishedCenter.getY(), overview[2], null);

		SequentialTransition sequence = new SequentialTransition();
		sequence.getChildren().add(new PauseTransition(Duration.millis(350)));

		if (finished != null && finished.getStatus() == Status.FINISHED && finishedCenter != null) {
			sequence.getChildren().add(host.cameraAnimation(new double[] { finishedCenter.getX(), finishedCenter.getY(), BracketViewer.MATCH_ZOOM }, Duration.millis(800)));
			sequence.getChildren().add(this.revealResult(finished));
			sequence.getChildren().add(new PauseTransition(Duration.millis(300)));

			for (Arrival arrival : this.arrivalsFrom(finished)) {
				sequence.getChildren().add(this.moveToNextMatch(finished, arrival));
			}
			sequence.getChildren().add(new PauseTransition(Duration.millis(250)));
		}
		sequence.getChildren().add(this.finale());

		// The next battle is only highlighted at the end
		VBox selectedCard = host.cards.get(host.selected);
		if (selectedCard != null) {
			host.styleSelected(selectedCard, false);
			((Label) ((HBox) selectedCard.getChildren().get(0)).getChildren().get(2)).setText("pronta");
		}
		this.finalStates.add(() -> {
			if (selectedCard != null && !selectedCard.getStyleClass().contains("selected")) {
				host.select(host.selected);
			}
		});

		sequence.setOnFinished(event -> this.endAnimation());
		this.resultAnimation = sequence;
		sequence.play();
	}

	/**
	 * Where the robots of the match go: the winner to its next match, the loser
	 * to the repechage. The winner first.
	 */
	private List<Arrival> arrivalsFrom(Match finished) {
		List<Arrival> arrivals = new ArrayList<Arrival>();
		for (Match match : host.bracket.getMatches()) {
			Slot[] slots = { match.getA(), match.getB() };
			for (int index = 0; index < 2; index++) {
				if (slots[index].getSource() == finished && slots[index].getParticipant() != null) {
					arrivals.add(new Arrival(match, host.rows.get(match.getCode())[index], slots[index].isSourceWinner()));
				}
			}
		}
		arrivals.sort((first, second) -> Boolean.compare(second.winner(), first.winner()));
		return arrivals;
	}

	/**
	 * The winner flashes, the score counts up and the loser is knocked out
	 */
	private Animation revealResult(Match finished) {
		ParallelTransition reveal = new ParallelTransition();

		for (HBox row : host.rows.get(finished.getCode())) {
			Long participant = host.rowParticipants.get(row);
			if (participant == null) {
				continue;
			}
			boolean winner = participant.equals(finished.getWinner());
			this.revealRow(reveal, row, host.scores.get(row), winner);
		}

		reveal.getChildren().add(new Timeline(new KeyFrame(BracketViewer.ACTION_TIME, event -> Sfx.slam())));
		return reveal;
	}

	/** One row of the match: hidden until the action, then shown with its final score and the effect of its result */
	private void revealRow(ParallelTransition reveal, HBox row, Label score, boolean winner) {
		String styleClass = winner ? "winner" : "loser";
		String finalScore = score == null ? null : score.getText();

		// Hidden until the reveal
		row.getStyleClass().remove(styleClass);
		if (score != null) {
			score.setOpacity(0);
		}
		Runnable done = () -> {
			if (!row.getStyleClass().contains(styleClass)) {
				row.getStyleClass().add(styleClass);
			}
			if (score != null) {
				score.textProperty().unbind();
				score.setText(finalScore);
				score.setOpacity(1);
			}
			row.setTranslateX(0);
			row.setEffect(null);
		};
		this.finalStates.add(done);

		Timeline show = new Timeline(new KeyFrame(BracketViewer.ACTION_TIME, event -> {
			row.getStyleClass().add(styleClass);
			if (score != null) {
				score.setOpacity(1);
			}
		}));
		reveal.getChildren().add(show);

		this.countScore(reveal, score, finalScore);
		if (winner) {
			this.flashWinner(reveal, row);
		} else {
			this.shakeLoser(reveal, row);
		}
	}

	/** The arcade counter that counts the score up to its final value */
	private void countScore(ParallelTransition reveal, Label score, String finalScore) {
		if (score != null && finalScore.matches("\\d+")) {
			IntegerProperty counter = new SimpleIntegerProperty(0);
			score.textProperty().bind(counter.asString());
			Timeline count = new Timeline(new KeyFrame(Duration.millis(700), new KeyValue(counter, Integer.parseInt(finalScore), Interpolator.EASE_OUT)));
			count.setOnFinished(event -> score.textProperty().unbind());
			reveal.getChildren().add(count);
		}
	}

	/** The card of the winner glows green, then goes back to normal */
	private void flashWinner(ParallelTransition reveal, HBox row) {
		DropShadow flash = new DropShadow(0, Color.web("#5af08f"));
		flash.setSpread(0.45);
		row.setEffect(flash);
		Timeline glow = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(flash.radiusProperty(), 0)), new KeyFrame(Duration.millis(220), new KeyValue(flash.radiusProperty(), 40)),
				new KeyFrame(Duration.millis(900), new KeyValue(flash.radiusProperty(), 0)));
		glow.setOnFinished(event -> row.setEffect(null));
		reveal.getChildren().add(glow);
	}

	/** The card of the loser shakes sideways and settles */
	private void shakeLoser(ParallelTransition reveal, HBox row) {
		Timeline shake = new Timeline();
		double[] offsets = { 10, -9, 7, -5, 3, -1, 0 };
		for (int index = 0; index < offsets.length; index++) {
			shake.getKeyFrames().add(new KeyFrame(Duration.millis(45 * (index + 1)), new KeyValue(row.translateXProperty(), offsets[index])));
		}
		reveal.getChildren().add(shake);
	}

	/**
	 * The camera follows the robot to its next match, the winner along its
	 * line, and the robot pops in its new place
	 */
	private Animation moveToNextMatch(Match finished, Arrival arrival) {
		HBox row = arrival.row();
		row.setOpacity(0);
		this.finalStates.add(() -> {
			row.setOpacity(1);
			row.setScaleX(1);
			row.setScaleY(1);
		});

		ParallelTransition travel = new ParallelTransition();
		Point2D destination = host.centerOf(arrival.match().getCode());
		travel.getChildren().add(host.cameraAnimation(new double[] { destination.getX(), destination.getY(), BracketViewer.MATCH_ZOOM * 0.85 }, Duration.millis(800)));

		Polyline line = arrival.winner() ? host.lines.get(finished.getCode() + ">" + arrival.match().getCode()) : null;
		if (line != null) {
			travel.getChildren().add(this.drawLine(line, Duration.millis(800)));
		}

		SequentialTransition step = new SequentialTransition(travel, new Timeline(new KeyFrame(BracketViewer.ACTION_TIME, event -> Sfx.blip())), this.arrive(row, arrival.winner()),
				new PauseTransition(Duration.millis(250)));
		return step;
	}

	/** The card pops in at its new place, and blinks where the robot arrives */
	private ParallelTransition arrive(HBox row, boolean winner) {
		ScaleTransition pop = new ScaleTransition(Duration.millis(320), row);
		pop.setFromX(0.4);
		pop.setFromY(0.4);
		pop.setToX(1);
		pop.setToY(1);
		pop.setInterpolator(Theme.OVERSHOOT);
		FadeTransition appear = new FadeTransition(Duration.millis(200), row);
		appear.setFromValue(0);
		appear.setToValue(1);
		ParallelTransition arrive = new ParallelTransition(pop, appear);
		arrive.setOnFinished(event -> {
			// Blink of the card that received the robot
			DropShadow flash = new DropShadow(30, winner ? Color.web("#5af08f") : Color.web("#ff9f1c"));
			flash.setSpread(0.4);
			row.setEffect(flash);
			Timeline fade = new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(flash.radiusProperty(), 0)));
			fade.setOnFinished(event2 -> row.setEffect(null));
			fade.play();
		});
		return arrive;
	}

	/**
	 * A neon line drawn from the finished match to the next one
	 */
	private Animation drawLine(Polyline line, Duration duration) {
		line.getStyleClass().remove("done");
		Polyline drawing = new Polyline();
		drawing.getPoints().setAll(line.getPoints());
		drawing.getStyleClass().addAll("connector", "done", "drawing");

		double length = 0;
		for (int index = 2; index < line.getPoints().size(); index += 2) {
			length += Math.hypot(line.getPoints().get(index) - line.getPoints().get(index - 2), line.getPoints().get(index + 1) - line.getPoints().get(index - 1));
		}
		drawing.getStrokeDashArray().setAll(length, length);
		drawing.setStrokeDashOffset(length);

		Runnable done = () -> {
			host.canvas.getChildren().remove(drawing);
			if (!line.getStyleClass().contains("done")) {
				line.getStyleClass().add("done");
			}
		};
		this.finalStates.add(done);

		Timeline draw = new Timeline(new KeyFrame(BracketViewer.ACTION_TIME, event -> {
			if (!host.canvas.getChildren().contains(drawing)) {
				// Over its line, under the cards
				host.canvas.getChildren().add(host.canvas.getChildren().indexOf(line) + 1, drawing);
			}
		}), new KeyFrame(duration, new KeyValue(drawing.strokeDashOffsetProperty(), 0, Interpolator.EASE_BOTH)));
		draw.setOnFinished(event -> done.run());
		return draw;
	}

	/**
	 * Zooms in the next battle, or shows the champion
	 */
	private Animation finale() {
		Point2D next = host.bracket.getChampion() == null ? host.centerOf(host.selected) : null;
		if (next == null) {
			// Champion (or nothing to play): back to the whole bracket, the banner is on top
			return host.cameraAnimation(host.overview(), Duration.millis(900));
		}
		Timeline highlight = new Timeline(new KeyFrame(BracketViewer.ACTION_TIME, event -> {
			host.select(host.selected);
			host.showNextTag();
			Sfx.confirm();
		}));
		return new SequentialTransition(host.cameraAnimation(new double[] { next.getX(), next.getY(), BracketViewer.MATCH_ZOOM }, Duration.millis(1000)), highlight);
	}

	/**
	 * Jumps to the end of the result animation
	 *
	 * @return true if there was an animation running
	 */
	boolean skip() {
		if (this.resultAnimation == null) {
			return false;
		}
		this.resultAnimation.stop();
		this.endAnimation();

		Point2D next = host.bracket.getChampion() == null ? host.centerOf(host.selected) : null;
		if (next == null) {
			double[] overview = host.overview();
			host.moveTo(overview[0], overview[1], overview[2], null);
		} else {
			host.moveTo(next.getX(), next.getY(), BracketViewer.MATCH_ZOOM, null);
			host.showNextTag();
		}
		return true;
	}

	/** Ends the animation: the final states are applied and the animation is cleared. */
	private void endAnimation() {
		this.resultAnimation = null;
		for (Runnable finalState : this.finalStates) {
			finalState.run();
		}
		this.finalStates.clear();
	}
}
