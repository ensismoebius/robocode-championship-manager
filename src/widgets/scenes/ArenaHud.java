package widgets.scenes;

import tournament.Importance;

import static widgets.scenes.BattleArena3D.PLAYER_COLORS;
import static widgets.scenes.BattleArena3D.SPEEDS;
import static widgets.scenes.ArenaHelpers.toCss;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.Opponent;
import tournament.Bracket;
import utils.configuration.Configuration;
import utils.robocode.BattleControl;
import utils.robocode.BattleRecording;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The HUD of the battle arena: the energy bars of the robots with their lag
 * bars, the round and the stage, the speed and the view, the letterbox and the
 * banners. It reads the state of the {@link BattleArena3D} and draws on its
 * screen.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaHud {

	private final BattleArena3D host;

	/** Creates the head-up display of the arena, drawn over the 3D scene by the host. */
	ArenaHud(BattleArena3D host) {
		this.host = host;
	}

	/** A health bar: the energy, the light trail of the damage just taken, the number and the won rounds */
	record HudPlayer(int index, Region energy, Region lag, Label energyText, HBox pips) {
	}

	/** Width of the health bars */
	private static final double HUD_BAR_WIDTH = 420;

	/** The damage trails draining, one per player */
	private final Map<Integer, Timeline> lagDrains = new HashMap<>();

	/** Builds the head-up display: the banners, the labels and the player panels. */
	Node buildHud() {
		host.banner.getStyleClass().add("arena-banner");
		host.subBanner.getStyleClass().add("arena-sub-banner");
		host.stageLabel.getStyleClass().add("arena-stage-big");
		host.stageLabel.setVisible(false);
		host.stageLabel.setManaged(false);
		VBox center = new VBox(10, host.stageLabel, host.loading, host.banner, host.subBanner);
		center.setAlignment(Pos.CENTER);
		center.setMouseTransparent(true);

		host.roundLabel.getStyleClass().add("arena-round");
		host.hudTop.getChildren().add(host.roundLabel);
		host.hudTop.setAlignment(Pos.TOP_CENTER);
		host.hudTop.setPadding(new Insets(18, 24, 0, 24));

		host.speedLabel.getStyleClass().add("arena-info");
		host.viewLabel.getStyleClass().add("arena-info");
		Label help = new Label("V: CÂMERA • ESPAÇO: PAUSA • ← →: VELOCIDADE • ENTER: PULAR • ARRASTE: GIRAR • RODA: ZOOM");
		help.getStyleClass().add("arena-help");
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox bottom = new HBox(18, help, spacer, host.viewLabel, host.speedLabel);
		bottom.setAlignment(Pos.CENTER_LEFT);
		bottom.setPadding(new Insets(0, 24, 16, 24));

		BorderPane hud = new BorderPane(center, host.hudTop, null, bottom, null);
		hud.setPickOnBounds(false);
		this.updateInfo();
		return hud;
	}

	/**
	 * Black bars of the replay, like a movie, with its tag
	 */
	Node buildLetterbox() {
		for (Rectangle bar : List.of(host.letterboxTop, host.letterboxBottom)) {
			bar.setFill(Color.BLACK);
			bar.widthProperty().bind(host.widthProperty());
			bar.setHeight(0);
			bar.setManaged(false);
		}
		host.letterboxBottom.layoutYProperty().bind(host.heightProperty().subtract(host.letterboxBottom.heightProperty()));

		Label dot = new Label("●");
		dot.getStyleClass().add("arena-replay-dot");
		Theme.blink(dot);
		Label replay = new Label("REPLAY");
		replay.getStyleClass().add("arena-replay");
		Label slow = new Label("CÂMERA LENTA");
		slow.getStyleClass().add("arena-replay-slow");
		host.replayTag.getChildren().addAll(dot, replay, slow);
		host.replayTag.setAlignment(Pos.CENTER_LEFT);
		// Managed, so the pane gives it its size (unmanaged, the texts were cut)
		host.replayTag.relocate(36, 26);
		host.replayTag.setVisible(false);

		Pane frame = new Pane(host.letterboxTop, host.letterboxBottom, host.replayTag);
		frame.setMouseTransparent(true);
		return frame;
	}

	/** Shows or hides the black bars of the letterbox, at the top and at the bottom. */
	void showLetterbox(boolean show) {
		this.showLetterbox(show, show);
	}

	/**
	 * @param tag
	 *            shows the replay tag
	 */
	void showLetterbox(boolean show, boolean tag) {
		double height = show ? host.getHeight() * 0.12 : 0;
		Timeline bars = new Timeline(new KeyFrame(Duration.millis(450), new KeyValue(host.letterboxTop.heightProperty(), height, Interpolator.EASE_BOTH),
				new KeyValue(host.letterboxBottom.heightProperty(), height, Interpolator.EASE_BOTH)));
		bars.play();
		host.replayTag.setVisible(tag);
		host.hudTop.setVisible(!show);
	}

	/**
	 * Fighting game health bars, P1 on the left and P2 on the right
	 */
	void buildHudPlayers() {
		host.hudTop.getChildren().clear();

		List<BattleRecording.Robot> list = host.recording.getRobots();
		for (int index = 0; index < list.size() && index < 2; index++) {
			VBox panel = this.playerPanel(index, list.get(index));
			if (index == 1) {
				host.hudTop.getChildren().addAll(spacer(), host.roundLabel, spacer());
			}
			host.hudTop.getChildren().add(panel);
		}
		if (list.size() < 2) {
			host.hudTop.getChildren().add(host.roundLabel);
		}
		this.updatePips();
	}

	/** The name, the team, the energy bar and the pips of one player, and the registration of its bar */
	private VBox playerPanel(int index, BattleRecording.Robot robot) {
		Opponent opponent = host.opponentOf(robot);
		Color color = PLAYER_COLORS[index];
		String css = toCss(color);

		Label name = new Label(opponent == null ? robot.getName() : opponent.getName().toUpperCase());
		name.getStyleClass().add("arena-player-name");
		name.setTextFill(color);
		Label team = new Label(opponent == null ? "" : opponent.getTeam());
		team.getStyleClass().add("arena-player-team");

		Region lag = new Region();
		lag.getStyleClass().add("arena-energy-lag");
		Region energy = new Region();
		energy.getStyleClass().add("arena-energy");
		energy.setStyle("-fx-background-color: " + css + ";");
		StackPane frame = this.energyBar(index, color, lag, energy);
		Label energyText = new Label("100");
		energyText.getStyleClass().add("arena-energy-text");
		energyText.setStyle("-fx-text-fill: " + css + "; -fx-border-color: " + css + ";");
		HBox pips = new HBox(6);

		HBox nameRow = index == 0 ? new HBox(10, name, team) : new HBox(10, team, name);
		nameRow.setAlignment(index == 0 ? Pos.BOTTOM_LEFT : Pos.BOTTOM_RIGHT);
		HBox barRow = index == 0 ? new HBox(10, energyText, frame) : new HBox(10, frame, energyText);
		barRow.setAlignment(Pos.CENTER);
		VBox panel = new VBox(6, nameRow, barRow, pips);
		panel.setAlignment(index == 0 ? Pos.TOP_LEFT : Pos.TOP_RIGHT);

		host.hudPlayers.add(new HudPlayer(robot.getIndex(), energy, lag, energyText, pips));
		return panel;
	}

	/**
	 * The energy bar of a player: a flat futuristic bar, slanted at its outer end, with solid colors, a thin
	 * outline, cells every 5 %, and the light trail (lag) that shows the damage just taken before it drains away
	 */
	private StackPane energyBar(int index, Color color, Region lag, Region energy) {
		double height = 14;
		double slant = 8;
		String css = toCss(color);
		Region back = new Region();
		back.setStyle("-fx-background-color: derive(" + css + ", -80%);");
		Region cells = new Region();
		cells.getStyleClass().add("arena-energy-cells");
		StackPane bar = new StackPane(back, lag, energy, cells);
		bar.setMinSize(HUD_BAR_WIDTH, height);
		bar.setPrefSize(HUD_BAR_WIDTH, height);
		bar.setMaxSize(HUD_BAR_WIDTH, height);
		Pos anchor = index == 0 ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;
		StackPane.setAlignment(lag, anchor);
		StackPane.setAlignment(energy, anchor);
		double[] corners = index == 0 ? new double[] { slant, 0, HUD_BAR_WIDTH, 0, HUD_BAR_WIDTH, height, 0, height }
				: new double[] { 0, 0, HUD_BAR_WIDTH - slant, 0, HUD_BAR_WIDTH, height, 0, height };
		bar.setClip(new Polygon(corners));
		Polygon outline = new Polygon(corners);
		outline.setFill(Color.TRANSPARENT);
		outline.setStroke(color);
		outline.setStrokeWidth(1.5);
		outline.setMouseTransparent(true);
		return new StackPane(bar, outline);
	}

	/** A region that takes the free space of a row. */
	static Region spacer() {
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		return spacer;
	}

	/**
	 * How many rounds the robots play for: all of the recording in a battle by
	 * points, the rounds to win in a knockout (best of 5, 3), which may end earlier
	 */
	int roundsToWin() {
		int rounds = host.recording == null ? 0 : host.recording.getRounds().size();
		if (!Configuration.getWinnerCriterion().isKnockout()) {
			return rounds;
		}
		return Math.max(1, BattleControl.roundsToWin(Configuration.getRobocodeRounds()));
	}

	/** Draws the pips of each player: one for each round it has won. */
	void updatePips() {
		int rounds = this.roundsToWin();
		for (HudPlayer player : host.hudPlayers) {
			player.pips().getChildren().clear();
			int wins = host.roundWins.getOrDefault(player.index(), 0);
			for (int index2 = 0; index2 < rounds; index2++) {
				Label pip = new Label("◆");
				pip.getStyleClass().add(index2 < wins ? "arena-pip-won" : "arena-pip");
				player.pips().getChildren().add(pip);
			}
		}
	}

	/** Updates the energy bars of the players from the energy of each robot. */
	void updateHud(Map<Integer, Double> energies) {
		for (HudPlayer player : host.hudPlayers) {
			double energy = Math.max(0, energies.getOrDefault(player.index(), 0.0));
			double width = HUD_BAR_WIDTH * Math.min(1, energy / 100);
			double before = player.energy().getMaxWidth();
			player.energy().setMaxWidth(width);
			if (width >= player.lag().getMaxWidth()) {
				// Healed, or a replay went back: the trail follows at once
				Timeline drain = this.lagDrains.remove(player.index());
				if (drain != null) {
					drain.stop();
				}
				player.lag().setMaxWidth(width);
			} else if (width < before) {
				// The damage stays lit for a moment, then drains down to the new energy
				Timeline old = this.lagDrains.get(player.index());
				if (old != null) {
					old.stop();
				}
				Timeline drain = new Timeline(new KeyFrame(Duration.seconds(0.45), new KeyValue(player.lag().maxWidthProperty(), player.lag().getMaxWidth())),
						new KeyFrame(Duration.seconds(1.0), new KeyValue(player.lag().maxWidthProperty(), width, Interpolator.EASE_IN)));
				this.lagDrains.put(player.index(), drain);
				drain.play();
			}
			// Low energy: the bar pulses
			player.energy().setOpacity(energy < 25 ? 0.6 + 0.4 * Math.abs(Math.sin(System.nanoTime() / 1e9 * 5)) : 1);
			player.energyText().setText(String.valueOf((int) Math.ceil(energy)));
		}
	}

	/** Shows the speed, the paused state and the camera view. */
	void updateInfo() {
		host.speedLabel.setText(host.paused ? "❚❚ PAUSA" : "▶ " + formatSpeed(SPEEDS[host.speed]) + "x");
		host.viewLabel.setText("◉ " + host.viewMode.label);
		if (host.recording != null) {
			boolean knockout = Configuration.getWinnerCriterion().isKnockout();
			String stage = host.stageImportance == Importance.REGULAR ? "" : host.stageName + " · ";
			host.roundLabel.setText(stage + "ROUND " + (host.round + 1) + (knockout ? " · MELHOR DE " + Configuration.getRobocodeRounds() : "/" + host.recording.getRounds().size()));
		}
	}

	/** The speed as it is shown: whole numbers without decimals. */
	static String formatSpeed(double speed) {
		return speed == Math.rint(speed) ? String.valueOf((int) speed) : String.valueOf(speed);
	}

	/** Shows a banner with a text and an optional sub-text. */
	void showBanner(String text, String sub) {
		host.banner.setStyle("");
		host.banner.setText(text);
		host.subBanner.setText(sub == null ? "" : sub);
		host.banner.setVisible(!text.isEmpty());
		host.subBanner.setVisible(sub != null);
		ScaleTransition pop = new ScaleTransition(Duration.millis(350), host.banner);
		pop.setFromX(2.5);
		pop.setFromY(2.5);
		pop.setToX(1);
		pop.setToY(1);
		pop.setInterpolator(Theme.OVERSHOOT);
		pop.play();
	}

	/** Hides the banner and its sub-text. */
	void hideBanner() {
		host.banner.setVisible(false);
		host.subBanner.setVisible(false);
	}
}
