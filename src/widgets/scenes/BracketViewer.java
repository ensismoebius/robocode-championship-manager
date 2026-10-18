package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import model.Opponent;
import tournament.Bracket;
import tournament.Bracket.Match;
import tournament.Bracket.Slot;
import tournament.Bracket.Status;
import widgets.ArcadeBackground;
import widgets.Sfx;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Shows the championship bracket: main bracket, repechage, finals and the
 * standings. Click a playable match to select it.
 *
 * It is seen through a camera, like in a game: Ctrl + mouse wheel or +/- zoom
 * smoothly, dragging or the arrows move it, 0 shows the whole bracket. After a
 * battle {@link #playResult(String)} shows the result moving through the
 * bracket and then flies to the next battle.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class BracketViewer extends StackPane {

	private static final double CARD_W = 300;
	private static final double ROW_H = 50;
	private static final double HEADER_H = 24;
	private static final double CARD_H = HEADER_H + 2 * ROW_H + 4;
	private static final double SLOT_H = CARD_H + 26;
	private static final double COL_GAP = 70;
	private static final double COL_W = CARD_W + COL_GAP;
	private static final double ROUND_TITLE_H = 34;
	private static final double SECTION_TITLE_H = 50;
	private static final double SECTION_GAP = 50;

	private final Bracket bracket;
	private final Map<Long, Opponent> opponents;
	private final Consumer<String> onSelect;

	private static final double MIN_ZOOM = 0.15;
	private static final double MAX_ZOOM = 2.5;
	/**
	 * Zoom when the camera goes to a match
	 */
	private static final double MATCH_ZOOM = 1.35;
	/**
	 * Inside sequential animations actions at time zero may not run, so they run
	 * at this time
	 */
	private static final Duration ACTION_TIME = Duration.millis(1);

	private final Pane canvas = new Pane();
	private final VBox content = new VBox(24);
	private final Pane viewport = new Pane();

	private final Map<Match, double[]> positions = new HashMap<Match, double[]>();
	private final Map<String, VBox> cards = new HashMap<String, VBox>();
	private final Map<String, HBox[]> rows = new HashMap<String, HBox[]>();
	private final Map<HBox, Long> rowParticipants = new HashMap<HBox, Long>();
	private final Map<HBox, Label> scores = new HashMap<HBox, Label>();
	private final Map<String, Polyline> lines = new HashMap<String, Polyline>();
	private final Map<Long, Image> logos = new HashMap<Long, Image>();

	private String selected;
	private Label nextTag;

	// The camera: the point of the content in the center of the screen and the
	// zoom. Where it is going is kept apart, so fast wheel turns add up.
	private final DoubleProperty cameraX = new SimpleDoubleProperty();
	private final DoubleProperty cameraY = new SimpleDoubleProperty();
	private final DoubleProperty cameraZoom = new SimpleDoubleProperty(1);
	private double targetX;
	private double targetY;
	private double targetZoom = 1;
	private Timeline cameraMove;
	private final Translate translate = new Translate();
	private final Scale scale = new Scale(1, 1, 0, 0);

	private boolean cameraReady = false;
	private double dragX;
	private double dragY;

	// The result being shown, see playResult
	private String pendingResult;
	private SequentialTransition resultAnimation;
	private final List<Runnable> finalStates = new ArrayList<Runnable>();

	/**
	 * @param selected
	 *            code of the selected match, may be null
	 * @param onSelect
	 *            called with the match code when a playable match is clicked
	 * @param robotsOutside
	 *            registered robots that are not in the championship
	 */
	public BracketViewer(String title, Bracket bracket, Map<Long, Opponent> opponents, String selected, Consumer<String> onSelect, long robotsOutside) {
		super();
		this.bracket = bracket;
		this.opponents = opponents;
		this.selected = selected;
		this.onSelect = onSelect;

		this.getStylesheets().add(getClass().getResource("bracket.css").toExternalForm());

		this.content.setPadding(new Insets(30));
		this.content.getStyleClass().add("bracket-root");
		this.content.getChildren().add(this.buildHeader(title, robotsOutside));
		this.content.getTransforms().setAll(this.translate, this.scale);

		this.layoutBracket();
		this.content.getChildren().add(this.canvas);

		// The camera looks at the content through the viewport
		this.viewport.getChildren().add(this.content);
		this.viewport.getStyleClass().add("bracket-root");
		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(this.viewport.widthProperty());
		clip.heightProperty().bind(this.viewport.heightProperty());
		this.viewport.setClip(clip);

		this.cameraX.addListener(observable -> this.applyCamera());
		this.cameraY.addListener(observable -> this.applyCamera());
		this.cameraZoom.addListener(observable -> this.applyCamera());
		this.viewport.widthProperty().addListener(observable -> this.viewportResized());
		this.viewport.heightProperty().addListener(observable -> this.viewportResized());

		this.installControls();

		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		scanlines.setMouseTransparent(true);

		// Dimmed, so it does not compete with the cards
		ArcadeBackground background = new ArcadeBackground();
		background.setOpacity(0.45);

		StackPane root = new StackPane(background, this.viewport, scanlines);
		root.getStyleClass().add("screen");
		this.getChildren().setAll(root);
	}

	// ------------------------------------------------------------------ header

	private Node buildHeader(String title, long robotsOutside) {
		VBox header = new VBox(8);

		Label titleLabel = new Label(title);
		titleLabel.getStyleClass().add("title");
		header.getChildren().add(titleLabel);

		String format = this.bracket.hasRepechage() ? "Dupla eliminação com repescagem" + (this.bracket.hasFinalReset() ? " e final com revanche" : "") : "Eliminação simples";
		Label subtitle = new Label(this.bracket.getParticipants().size() + " robôs • " + format);
		subtitle.getStyleClass().add("subtitle");
		header.getChildren().add(subtitle);

		Long champion = this.bracket.getChampion();
		if (champion != null) {
			HBox banner = new HBox(16);
			banner.setAlignment(Pos.CENTER_LEFT);
			banner.getStyleClass().add("champion-banner");
			ImageView logo = this.logo(champion, 96, 72);
			if (logo != null) {
				banner.getChildren().add(logo);
			}
			Label championLabel = new Label("CAMPEÃO: " + this.nameOf(champion).toUpperCase());
			banner.getChildren().add(championLabel);
			Theme.pulseGlow(banner, Theme.ACCENT);
			banner.setMaxWidth(Region.USE_PREF_SIZE);
			header.getChildren().add(banner);
		} else {
			Label next = new Label(this.bracket.getNextMatch().map(m -> "Próxima partida: " + m.getCode() + " — " + this.nameOf(m.getA().getParticipant()) + " × " + this.nameOf(m.getB().getParticipant()))
					.orElse(""));
			next.getStyleClass().add("subtitle");
			header.getChildren().add(next);
		}

		if (robotsOutside > 0) {
			Label warning = new Label(robotsOutside + " robô(s) cadastrado(s) depois do sorteio não estão no campeonato. Ctrl + G sorteia um novo campeonato.");
			warning.getStyleClass().add("warning");
			header.getChildren().add(warning);
		}

		Label help = new Label(
				"Clique em uma partida para selecioná-la • B: tela versus • S: inicia a batalha • 1/2: declara o vencedor • Ctrl+Z: desfaz o último resultado • Ctrl+roda ou +/-: zoom • arraste, roda ou setas: move • 0: visão geral");
		help.getStyleClass().add("help");
		header.getChildren().add(help);

		return header;
	}

	// ----------------------------------------------------------------- layout

	private void layoutBracket() {
		double top = 0;
		List<List<Match>> winners = this.bracket.getWinnersRounds();
		List<List<Match>> losers = this.bracket.getLosersRounds();

		this.addSectionTitle(this.bracket.hasRepechage() ? "Chave principal" : "Chave", top);
		top += SECTION_TITLE_H;
		double winnersHeight = this.layoutRounds(winners, top, this.bracket::winnersRoundName);
		top += ROUND_TITLE_H + winnersHeight;

		double finalsY = this.positions.get(winners.get(winners.size() - 1).get(0))[1];

		if (!losers.isEmpty()) {
			top += SECTION_GAP;
			this.addSectionTitle("Repescagem", top);
			top += SECTION_TITLE_H;
			double losersHeight = this.layoutRounds(losers, top, this.bracket::losersRoundName);

			double losersFinalY = this.positions.get(losers.get(losers.size() - 1).get(0))[1];
			finalsY = (finalsY + losersFinalY) / 2;
			top += ROUND_TITLE_H + losersHeight;
		}

		// Finals to the right of everything
		int columns = Math.max(winners.size(), losers.size());
		List<Match> finals = this.bracket.getFinals();
		for (int i = 0; i < finals.size(); i++) {
			double x = (columns + i) * COL_W;
			this.addRoundTitle(this.bracket.roundName(finals.get(i)), x, finalsY - CARD_H / 2 - ROUND_TITLE_H);
			this.positions.put(finals.get(i), new double[] { x, finalsY });
		}

		// Standings in the free space under the finals
		Node standings = this.buildStandings();
		if (standings != null) {
			standings.relocate(columns * COL_W, finalsY + CARD_H / 2 + 50);
			this.canvas.getChildren().add(standings);
		}

		// Lines first so the cards are drawn over them
		for (Match match : this.bracket.getMatches()) {
			this.addConnectors(match);
		}
		for (Match match : this.bracket.getMatches()) {
			double[] position = this.positions.get(match);
			VBox card = this.buildCard(match);
			card.relocate(position[0], position[1] - CARD_H / 2);
			this.canvas.getChildren().add(card);
		}

		this.canvas.setMinHeight(top);
	}

	/**
	 * Places the rounds side by side, each match centered between the two matches
	 * that feed it
	 *
	 * @return the height used
	 */
	private double layoutRounds(List<List<Match>> rounds, double top, java.util.function.IntFunction<String> roundName) {
		double height = rounds.get(0).size() * SLOT_H;
		for (int r = 0; r < rounds.size(); r++) {
			List<Match> round = rounds.get(r);
			double x = r * COL_W;
			this.addRoundTitle(roundName.apply(r + 1), x, top);
			double spacing = height / round.size();
			for (int i = 0; i < round.size(); i++) {
				this.positions.put(round.get(i), new double[] { x, top + ROUND_TITLE_H + spacing * (i + 0.5) });
			}
		}
		return height;
	}

	private void addSectionTitle(String text, double y) {
		Label label = new Label(text);
		label.getStyleClass().add("section-title");
		label.relocate(0, y);
		this.canvas.getChildren().add(label);
	}

	private void addRoundTitle(String text, double x, double y) {
		Label label = new Label(text);
		label.getStyleClass().add("round-title");
		label.relocate(x + 4, y);
		this.canvas.getChildren().add(label);
	}

	/**
	 * Elbow lines from the matches whose winners come to this one. Losers coming
	 * from the main bracket are not drawn, their slot tells where they come from
	 */
	private void addConnectors(Match match) {
		double[] to = this.positions.get(match);
		for (Slot slot : List.of(match.getA(), match.getB())) {
			Match source = slot.getSource();
			if (source == null || !slot.isSourceWinner()) {
				continue;
			}
			double[] from = this.positions.get(source);
			double x1 = from[0] + CARD_W;
			double y1 = from[1];
			double x2 = to[0];
			double y2 = to[1];
			double middle = x2 - COL_GAP / 2;

			Polyline line = new Polyline(x1, y1, middle, y1, middle, y2, x2, y2);
			line.getStyleClass().add("connector");
			this.lines.put(source.getCode() + ">" + match.getCode(), line);
			if (source.getStatus() == Status.FINISHED || (source.getStatus() == Status.BYE && source.getWinner() != null)) {
				line.getStyleClass().add("done");
			}
			this.canvas.getChildren().add(line);
		}
	}

	// ------------------------------------------------------------------ cards

	private VBox buildCard(Match match) {
		VBox card = new VBox();
		card.setPrefSize(CARD_W, CARD_H);
		card.setMinSize(CARD_W, CARD_H);
		card.setMaxSize(CARD_W, CARD_H);
		card.getStyleClass().add("match-card");

		switch (match.getStatus()) {
		case READY -> card.getStyleClass().add("ready");
		case BYE -> card.getStyleClass().add(match.getWinner() == null ? "empty" : "bye");
		case SKIPPED -> card.getStyleClass().add("skipped");
		default -> {
		}
		}

		Label code = new Label(match.getCode());
		code.getStyleClass().add("match-code");
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		Label status = new Label(this.statusText(match));
		status.getStyleClass().add("match-status");
		HBox header = new HBox(code, spacer, status);
		header.setAlignment(Pos.CENTER_LEFT);
		header.setMinHeight(HEADER_H);
		header.getStyleClass().add("match-header");

		HBox rowA = this.buildSlotRow(match, match.getA());
		HBox rowB = this.buildSlotRow(match, match.getB());
		this.rows.put(match.getCode(), new HBox[] { rowA, rowB });
		card.getChildren().addAll(header, rowA, rowB);

		if (match.getStatus() == Status.READY) {
			card.setOnMouseClicked(event -> {
				// The end of a drag is not a click
				if (!event.isStillSincePress()) {
					return;
				}
				Sfx.blip();
				this.select(match.getCode());
				this.onSelect.accept(match.getCode());
			});
		}

		this.cards.put(match.getCode(), card);
		if (match.getCode().equals(this.selected)) {
			this.styleSelected(card, true);
		}
		return card;
	}

	private String statusText(Match match) {
		boolean isNext = this.bracket.getNextMatch().map(m -> m == match).orElse(false);
		return switch (match.getStatus()) {
		case WAITING -> "aguardando";
		case READY -> match.getCode().equals(this.selected) ? "▶ SELECIONADA" : isNext ? "próxima" : "pronta";
		case FINISHED -> "encerrada";
		case BYE -> "folga";
		case SKIPPED -> "não foi necessária";
		};
	}

	private HBox buildSlotRow(Match match, Slot slot) {
		HBox row = new HBox();
		row.getStyleClass().add("slot-row");
		row.setAlignment(Pos.CENTER_LEFT);
		row.setMinHeight(ROW_H);
		row.setMaxHeight(ROW_H);

		Long participant = slot.getParticipant();
		if (participant == null) {
			Label placeholder = new Label(slot.isResolved() ? "— folga —" : slot.describeSource());
			placeholder.getStyleClass().add("placeholder");
			row.getChildren().add(placeholder);
			return row;
		}

		boolean decided = match.getStatus() == Status.FINISHED;
		if (decided) {
			row.getStyleClass().add(participant.equals(match.getWinner()) ? "winner" : "loser");
		}

		StackPane logoBox = new StackPane();
		logoBox.getStyleClass().add("logo-box");
		logoBox.setMinSize(56, 42);
		logoBox.setMaxSize(56, 42);
		ImageView logo = this.logo(participant, 56, 42);
		if (logo != null) {
			logoBox.getChildren().add(logo);
		}

		Opponent opponent = this.opponents.get(participant);
		Label name = new Label(opponent == null ? "?" : opponent.getName());
		name.getStyleClass().add("robot-name");
		Label team = new Label(opponent == null ? "" : opponent.getTeam());
		team.getStyleClass().add("team-name");
		VBox names = new VBox(name, team);
		names.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(names, Priority.ALWAYS);
		names.setMaxWidth(Double.MAX_VALUE);

		row.getChildren().addAll(logoBox, names);
		this.rowParticipants.put(row, participant);

		if (decided) {
			boolean won = participant.equals(match.getWinner());
			Integer score = won ? match.getWinnerScore() : match.getLoserScore();
			Label scoreLabel = new Label(score != null ? String.valueOf(score) : won ? "✔" : "");
			scoreLabel.getStyleClass().add("score");
			row.getChildren().add(scoreLabel);
			this.scores.put(row, scoreLabel);
		}
		return row;
	}

	private ImageView logo(Long participant, double width, double height) {
		Opponent opponent = this.opponents.get(participant);
		if (opponent == null || opponent.getImage() == null) {
			return null;
		}
		Image image = this.logos.computeIfAbsent(participant, id -> new Image(new ByteArrayInputStream(opponent.getImage()), 160, 120, true, true));
		ImageView view = new ImageView(image);
		view.setFitWidth(width);
		view.setFitHeight(height);
		view.setPreserveRatio(true);
		view.setSmooth(true);
		return view;
	}

	private String nameOf(Long participant) {
		Opponent opponent = this.opponents.get(participant);
		return opponent == null ? "?" : opponent.getName() + " (" + opponent.getTeam() + ")";
	}

	// -------------------------------------------------------------- selection

	/**
	 * Highlights the match
	 */
	public void select(String code) {
		VBox previous = this.cards.get(this.selected);
		if (previous != null) {
			this.styleSelected(previous, false);
		}
		this.removeNextTag();
		this.selected = code;
		VBox card = this.cards.get(code);
		if (card != null) {
			this.styleSelected(card, true);
		}
	}

	/**
	 * A blinking "next battle" tag over the selected match
	 */
	private void showNextTag() {
		this.removeNextTag();
		VBox card = this.cards.get(this.selected);
		if (card == null) {
			return;
		}
		this.nextTag = new Label("▼ PRÓXIMA BATALHA ▼");
		this.nextTag.getStyleClass().add("next-tag");
		this.nextTag.setMouseTransparent(true);
		this.canvas.getChildren().add(this.nextTag);
		this.nextTag.applyCss();
		double width = this.nextTag.prefWidth(-1);
		this.nextTag.relocate(card.getLayoutX() + (CARD_W - width) / 2, card.getLayoutY() - 34);
		Theme.blink(this.nextTag);
	}

	private void removeNextTag() {
		if (this.nextTag != null) {
			this.canvas.getChildren().remove(this.nextTag);
			this.nextTag = null;
		}
	}

	private void styleSelected(VBox card, boolean selected) {
		card.getStyleClass().remove("selected");
		if (selected) {
			card.getStyleClass().add("selected");
			Theme.pulseGlow(card, Theme.ACCENT);
		} else {
			card.setEffect(null);
		}
		Label status = (Label) ((HBox) card.getChildren().get(0)).getChildren().get(2);
		Match match = this.bracket.getMatch(this.codeOf(card));
		status.setText(this.statusText(match));
	}

	private String codeOf(VBox card) {
		return this.cards.entrySet().stream().filter(e -> e.getValue() == card).map(Map.Entry::getKey).findFirst().orElseThrow();
	}

	// ----------------------------------------------------------------- camera

	private void installControls() {
		// Ctrl + wheel zooms towards the mouse, the wheel alone moves
		this.viewport.addEventFilter(ScrollEvent.SCROLL, event -> {
			if (this.skipAnimation()) {
				event.consume();
				return;
			}
			if (event.isControlDown()) {
				if (event.getDeltaY() != 0) {
					this.zoomAt(event.getX(), event.getY(), event.getDeltaY() > 0 ? 1.15 : 1 / 1.15);
				}
			} else {
				this.moveTo(this.cameraX.get() - event.getDeltaX() / this.cameraZoom.get(), this.cameraY.get() - event.getDeltaY() / this.cameraZoom.get(), this.cameraZoom.get(), null);
			}
			event.consume();
		});

		// Dragging moves the camera
		this.viewport.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			if (this.skipAnimation()) {
				event.consume();
				return;
			}
			this.dragX = event.getX();
			this.dragY = event.getY();
		});
		this.viewport.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
			double zoom = this.cameraZoom.get();
			this.moveTo(this.cameraX.get() - (event.getX() - this.dragX) / zoom, this.cameraY.get() - (event.getY() - this.dragY) / zoom, zoom, null);
			this.dragX = event.getX();
			this.dragY = event.getY();
		});

		this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			// Any key ends the result animation, and still does its job
			this.skipAnimation();
			double step = 160 / this.targetZoom;
			switch (event.getCode()) {
			case PLUS, EQUALS, ADD -> this.zoomAt(this.viewport.getWidth() / 2, this.viewport.getHeight() / 2, 1.3);
			case MINUS, SUBTRACT -> this.zoomAt(this.viewport.getWidth() / 2, this.viewport.getHeight() / 2, 1 / 1.3);
			case DIGIT0, NUMPAD0 -> this.moveCamera(this.overview(), Duration.millis(500));
			case LEFT -> this.moveCamera(new double[] { this.targetX - step, this.targetY, this.targetZoom }, Duration.millis(200));
			case RIGHT -> this.moveCamera(new double[] { this.targetX + step, this.targetY, this.targetZoom }, Duration.millis(200));
			case UP -> this.moveCamera(new double[] { this.targetX, this.targetY - step, this.targetZoom }, Duration.millis(200));
			case DOWN -> this.moveCamera(new double[] { this.targetX, this.targetY + step, this.targetZoom }, Duration.millis(200));
			default -> {
				return;
			}
			}
			event.consume();
		});
	}

	/**
	 * Puts the camera on the content
	 */
	private void applyCamera() {
		double zoom = this.cameraZoom.get();
		this.scale.setX(zoom);
		this.scale.setY(zoom);
		this.translate.setX(this.viewport.getWidth() / 2 - this.cameraX.get() * zoom);
		this.translate.setY(this.viewport.getHeight() / 2 - this.cameraY.get() * zoom);
	}

	/**
	 * The first time the size is known the camera starts, or the result
	 * animation
	 */
	private void viewportResized() {
		if (this.viewport.getWidth() <= 0 || this.viewport.getHeight() <= 0) {
			return;
		}
		if (this.cameraReady) {
			// Keeps looking at the same place, but inside the content
			this.moveTo(this.cameraX.get(), this.cameraY.get(), this.cameraZoom.get(), null);
			return;
		}
		this.cameraReady = true;
		// The camera needs the size of the content and where the cards are
		this.content.applyCss();
		this.content.autosize();
		this.content.layout();

		if (this.pendingResult != null) {
			this.startResultAnimation(this.pendingResult);
		} else {
			// The whole width, looking at the selected match
			double[] overview = this.overview();
			Point2D selectedCenter = this.centerOf(this.selected);
			this.moveTo(overview[0], selectedCenter == null ? overview[1] : selectedCenter.getY(), overview[2], null);
		}
	}

	/**
	 * Camera position where the whole width of the bracket fits the screen,
	 * looking at its top
	 */
	private double[] overview() {
		double width = this.content.getLayoutBounds().getWidth();
		double zoom = Math.max(MIN_ZOOM, Math.min(1, (this.viewport.getWidth() - 20) / width));
		return new double[] { width / 2, 0, zoom };
	}

	/**
	 * Center of a match card in content coordinates
	 */
	private Point2D centerOf(String code) {
		VBox card = code == null ? null : this.cards.get(code);
		if (card == null) {
			return null;
		}
		return new Point2D(this.canvas.getLayoutX() + card.getLayoutX() + CARD_W / 2, this.canvas.getLayoutY() + card.getLayoutY() + CARD_H / 2);
	}

	/**
	 * Keeps the camera over the content: a dimension smaller than the screen is
	 * centered, a bigger one does not show what is beyond its borders
	 */
	private double[] clamp(double x, double y, double zoom) {
		zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
		double width = this.content.getLayoutBounds().getWidth();
		double height = this.content.getLayoutBounds().getHeight();
		double halfWidth = this.viewport.getWidth() / 2 / zoom;
		double halfHeight = this.viewport.getHeight() / 2 / zoom;
		x = width <= 2 * halfWidth ? width / 2 : Math.max(halfWidth, Math.min(width - halfWidth, x));
		y = height <= 2 * halfHeight ? height / 2 : Math.max(halfHeight, Math.min(height - halfHeight, y));
		return new double[] { x, y, zoom };
	}

	/**
	 * Moves the camera at once (duration null) or smoothly
	 */
	private void moveTo(double x, double y, double zoom, Duration duration) {
		if (duration == null) {
			if (this.cameraMove != null) {
				this.cameraMove.stop();
			}
			double[] target = this.clamp(x, y, zoom);
			this.targetX = target[0];
			this.targetY = target[1];
			this.targetZoom = target[2];
			this.cameraX.set(target[0]);
			this.cameraY.set(target[1]);
			this.cameraZoom.set(target[2]);
		} else {
			this.moveCamera(new double[] { x, y, zoom }, duration);
		}
	}

	private void moveCamera(double[] position, Duration duration) {
		if (this.cameraMove != null) {
			this.cameraMove.stop();
		}
		this.cameraMove = this.cameraAnimation(position, duration);
		this.cameraMove.play();
	}

	/**
	 * An animation that takes the camera there, starting from wherever it is
	 * when it plays
	 */
	private Timeline cameraAnimation(double[] position, Duration duration) {
		double[] target = this.clamp(position[0], position[1], position[2]);
		Timeline move = new Timeline();
		move.getKeyFrames().add(new KeyFrame(ACTION_TIME, event -> {
			this.targetX = target[0];
			this.targetY = target[1];
			this.targetZoom = target[2];
		}));
		move.getKeyFrames().add(new KeyFrame(duration, //
				new KeyValue(this.cameraX, target[0], Interpolator.EASE_BOTH), //
				new KeyValue(this.cameraY, target[1], Interpolator.EASE_BOTH), //
				new KeyValue(this.cameraZoom, target[2], Interpolator.EASE_BOTH)));
		return move;
	}

	/**
	 * Smooth zoom keeping the point under (x, y) of the screen in place
	 */
	private void zoomAt(double x, double y, double factor) {
		double zoom = this.targetZoom;
		// The point of the content under the mouse, where the camera is going
		double pointX = this.targetX + (x - this.viewport.getWidth() / 2) / zoom;
		double pointY = this.targetY + (y - this.viewport.getHeight() / 2) / zoom;
		double newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
		this.moveCamera(new double[] { pointX - (x - this.viewport.getWidth() / 2) / newZoom, pointY - (y - this.viewport.getHeight() / 2) / newZoom, newZoom }, Duration.millis(220));
	}

	/**
	 * Flies to the selected match, zooms in and tags it as the next battle
	 */
	public void focusSelected() {
		Point2D center = this.centerOf(this.selected);
		if (center == null) {
			return;
		}
		if (!this.cameraReady) {
			// Not shown yet, the camera starts there
			this.pendingResult = "";
			return;
		}
		this.moveCamera(new double[] { center.getX(), center.getY(), MATCH_ZOOM }, Duration.millis(900));
		this.showNextTag();
	}

	// ------------------------------------------------------- result animation

	/**
	 * Shows, like in a game, the result of the match that has just been played:
	 * the camera goes to it, the result appears, the robots move to their next
	 * matches and then the camera zooms in the next battle. Any key or click
	 * jumps to the end.
	 */
	public void playResult(String code) {
		this.pendingResult = code;
		if (this.cameraReady) {
			this.startResultAnimation(code);
		}
	}

	private record Arrival(Match match, HBox row, boolean winner) {
	}

	private void startResultAnimation(String code) {
		this.pendingResult = null;
		Match finished = this.bracket.getMatch(code);
		Point2D finishedCenter = this.centerOf(code);
		double[] overview = this.overview();

		// The camera starts showing the whole bracket
		this.moveTo(overview[0], finishedCenter == null ? overview[1] : finishedCenter.getY(), overview[2], null);

		SequentialTransition sequence = new SequentialTransition();
		sequence.getChildren().add(new PauseTransition(Duration.millis(350)));

		if (finished != null && finished.getStatus() == Status.FINISHED && finishedCenter != null) {
			sequence.getChildren().add(this.cameraAnimation(new double[] { finishedCenter.getX(), finishedCenter.getY(), MATCH_ZOOM }, Duration.millis(800)));
			sequence.getChildren().add(this.revealResult(finished));
			sequence.getChildren().add(new PauseTransition(Duration.millis(300)));

			for (Arrival arrival : this.arrivalsFrom(finished)) {
				sequence.getChildren().add(this.moveToNextMatch(finished, arrival));
			}
			sequence.getChildren().add(new PauseTransition(Duration.millis(250)));
		}
		sequence.getChildren().add(this.finale());

		// The next battle is only highlighted at the end
		VBox selectedCard = this.cards.get(this.selected);
		if (selectedCard != null) {
			this.styleSelected(selectedCard, false);
			((Label) ((HBox) selectedCard.getChildren().get(0)).getChildren().get(2)).setText("pronta");
		}
		this.finalStates.add(() -> {
			if (selectedCard != null && !selectedCard.getStyleClass().contains("selected")) {
				this.select(this.selected);
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
		for (Match match : this.bracket.getMatches()) {
			if (match.getStatus() == Status.SKIPPED) {
				continue;
			}
			Slot[] slots = { match.getA(), match.getB() };
			for (int i = 0; i < 2; i++) {
				if (slots[i].getSource() == finished && slots[i].getParticipant() != null) {
					arrivals.add(new Arrival(match, this.rows.get(match.getCode())[i], slots[i].isSourceWinner()));
				}
			}
		}
		arrivals.sort((a, b) -> Boolean.compare(b.winner(), a.winner()));
		return arrivals;
	}

	/**
	 * The winner flashes, the score counts up and the loser is knocked out
	 */
	private Animation revealResult(Match finished) {
		ParallelTransition reveal = new ParallelTransition();

		for (HBox row : this.rows.get(finished.getCode())) {
			Long participant = this.rowParticipants.get(row);
			if (participant == null) {
				continue;
			}
			boolean winner = participant.equals(finished.getWinner());
			String styleClass = winner ? "winner" : "loser";
			Label score = this.scores.get(row);
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

			Timeline show = new Timeline(new KeyFrame(ACTION_TIME, event -> {
				row.getStyleClass().add(styleClass);
				if (score != null) {
					score.setOpacity(1);
				}
			}));
			reveal.getChildren().add(show);

			if (score != null && finalScore.matches("\\d+")) {
				// Arcade score counter
				IntegerProperty counter = new SimpleIntegerProperty(0);
				score.textProperty().bind(counter.asString());
				Timeline count = new Timeline(new KeyFrame(Duration.millis(700), new KeyValue(counter, Integer.parseInt(finalScore), Interpolator.EASE_OUT)));
				count.setOnFinished(event -> score.textProperty().unbind());
				reveal.getChildren().add(count);
			}

			if (winner) {
				DropShadow flash = new DropShadow(0, Color.web("#5af08f"));
				flash.setSpread(0.45);
				row.setEffect(flash);
				Timeline glow = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(flash.radiusProperty(), 0)), new KeyFrame(Duration.millis(220), new KeyValue(flash.radiusProperty(), 40)),
						new KeyFrame(Duration.millis(900), new KeyValue(flash.radiusProperty(), 0)));
				glow.setOnFinished(event -> row.setEffect(null));
				reveal.getChildren().add(glow);
			} else {
				Timeline shake = new Timeline();
				double[] offsets = { 10, -9, 7, -5, 3, -1, 0 };
				for (int i = 0; i < offsets.length; i++) {
					shake.getKeyFrames().add(new KeyFrame(Duration.millis(45 * (i + 1)), new KeyValue(row.translateXProperty(), offsets[i])));
				}
				reveal.getChildren().add(shake);
			}
		}

		reveal.getChildren().add(new Timeline(new KeyFrame(ACTION_TIME, event -> Sfx.slam())));
		return reveal;
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
		Point2D destination = this.centerOf(arrival.match().getCode());
		travel.getChildren().add(this.cameraAnimation(new double[] { destination.getX(), destination.getY(), MATCH_ZOOM * 0.85 }, Duration.millis(800)));

		Polyline line = arrival.winner() ? this.lines.get(finished.getCode() + ">" + arrival.match().getCode()) : null;
		if (line != null) {
			travel.getChildren().add(this.drawLine(line, Duration.millis(800)));
		}

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
			DropShadow flash = new DropShadow(30, arrival.winner() ? Color.web("#5af08f") : Color.web("#ff9f1c"));
			flash.setSpread(0.4);
			row.setEffect(flash);
			Timeline fade = new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(flash.radiusProperty(), 0)));
			fade.setOnFinished(e -> row.setEffect(null));
			fade.play();
		});

		SequentialTransition step = new SequentialTransition(travel, new Timeline(new KeyFrame(ACTION_TIME, event -> Sfx.blip())), arrive, new PauseTransition(Duration.millis(250)));
		return step;
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
		for (int i = 2; i < line.getPoints().size(); i += 2) {
			length += Math.hypot(line.getPoints().get(i) - line.getPoints().get(i - 2), line.getPoints().get(i + 1) - line.getPoints().get(i - 1));
		}
		drawing.getStrokeDashArray().setAll(length, length);
		drawing.setStrokeDashOffset(length);

		Runnable done = () -> {
			this.canvas.getChildren().remove(drawing);
			if (!line.getStyleClass().contains("done")) {
				line.getStyleClass().add("done");
			}
		};
		this.finalStates.add(done);

		Timeline draw = new Timeline(new KeyFrame(ACTION_TIME, event -> {
			if (!this.canvas.getChildren().contains(drawing)) {
				// Over its line, under the cards
				this.canvas.getChildren().add(this.canvas.getChildren().indexOf(line) + 1, drawing);
			}
		}), new KeyFrame(duration, new KeyValue(drawing.strokeDashOffsetProperty(), 0, Interpolator.EASE_BOTH)));
		draw.setOnFinished(event -> done.run());
		return draw;
	}

	/**
	 * Zooms in the next battle, or shows the champion
	 */
	private Animation finale() {
		Point2D next = this.bracket.getChampion() == null ? this.centerOf(this.selected) : null;
		if (next == null) {
			// Champion (or nothing to play): back to the whole bracket, the banner is on top
			return this.cameraAnimation(this.overview(), Duration.millis(900));
		}
		Timeline highlight = new Timeline(new KeyFrame(ACTION_TIME, event -> {
			this.select(this.selected);
			this.showNextTag();
			Sfx.confirm();
		}));
		return new SequentialTransition(this.cameraAnimation(new double[] { next.getX(), next.getY(), MATCH_ZOOM }, Duration.millis(1000)), highlight);
	}

	/**
	 * Jumps to the end of the result animation
	 *
	 * @return true if there was an animation running
	 */
	private boolean skipAnimation() {
		if (this.resultAnimation == null) {
			return false;
		}
		this.resultAnimation.stop();
		this.endAnimation();

		Point2D next = this.bracket.getChampion() == null ? this.centerOf(this.selected) : null;
		if (next == null) {
			double[] overview = this.overview();
			this.moveTo(overview[0], overview[1], overview[2], null);
		} else {
			this.moveTo(next.getX(), next.getY(), MATCH_ZOOM, null);
			this.showNextTag();
		}
		return true;
	}

	private void endAnimation() {
		this.resultAnimation = null;
		for (Runnable finalState : this.finalStates) {
			finalState.run();
		}
		this.finalStates.clear();
	}

	// -------------------------------------------------------------- standings

	private Node buildStandings() {
		Map<Long, Integer> placements = this.bracket.getPlacements();
		if (placements.isEmpty()) {
			return null;
		}

		VBox box = new VBox(10);
		Label title = new Label(this.bracket.isFinished() ? "Classificação final" : "Classificação");
		title.getStyleClass().add("section-title");

		GridPane grid = new GridPane();
		grid.getStyleClass().add("standings");

		// Several columns so long lists do not get too tall
		List<Map.Entry<Long, Integer>> entries = new ArrayList<Map.Entry<Long, Integer>>(placements.entrySet());
		entries.sort(Map.Entry.comparingByValue());
		int perColumn = Math.max(10, (entries.size() + 1) / 2);
		for (int i = 0; i < entries.size(); i++) {
			Map.Entry<Long, Integer> entry = entries.get(i);
			Label place = new Label(entry.getValue() + "º");
			place.getStyleClass().add("place");
			Label name = new Label(this.nameOf(entry.getKey()));
			name.getStyleClass().add("place-name");
			int column = (i / perColumn) * 2;
			grid.add(place, column, i % perColumn);
			grid.add(name, column + 1, i % perColumn);
		}

		box.getChildren().addAll(title, grid);
		return box;
	}
}
