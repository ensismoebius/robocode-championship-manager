package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
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
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import model.Opponent;
import tournament.Bracket;
import tournament.Bracket.Match;
import tournament.Bracket.Slot;
import tournament.Bracket.Status;
import widgets.ArcadeBackground;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Shows the championship bracket: main bracket, repechage, finals and the
 * standings. Click a playable match to select it. Ctrl + mouse wheel or +/-
 * zoom, 0 fits the bracket in the screen.
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

	private final Pane canvas = new Pane();
	private final VBox content = new VBox(24);
	private final ScrollPane scrollPane;

	private final Map<Match, double[]> positions = new HashMap<Match, double[]>();
	private final Map<String, VBox> cards = new HashMap<String, VBox>();
	private final Map<Long, Image> logos = new HashMap<Long, Image>();

	private String selected;
	private double zoom = 1;
	private boolean autoFit = true;
	private final Scale scale = new Scale(1, 1, 0, 0);

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
		this.content.getTransforms().add(this.scale);

		this.layoutBracket();
		this.content.getChildren().add(this.canvas);

		// The group makes the scroll area follow the zoom
		this.scrollPane = new ScrollPane(new Group(this.content));
		this.scrollPane.setPannable(true);
		this.scrollPane.getStyleClass().add("bracket-root");

		this.scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
			if (event.isControlDown()) {
				this.autoFit = false;
				this.setZoom(this.zoom * (event.getDeltaY() > 0 ? 1.1 : 1 / 1.1));
				event.consume();
			}
		});
		this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			switch (event.getCode()) {
			case PLUS, EQUALS, ADD -> {
				this.autoFit = false;
				this.setZoom(this.zoom * 1.15);
			}
			case MINUS, SUBTRACT -> {
				this.autoFit = false;
				this.setZoom(this.zoom / 1.15);
			}
			case DIGIT0, NUMPAD0 -> {
				this.autoFit = true;
				this.fitToScreen();
			}
			default -> {
				return;
			}
			}
			event.consume();
		});

		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		scanlines.setMouseTransparent(true);

		// Dimmed, so it does not compete with the cards
		ArcadeBackground background = new ArcadeBackground();
		background.setOpacity(0.45);

		StackPane root = new StackPane(background, this.scrollPane, scanlines);
		root.getStyleClass().add("screen");
		this.getChildren().setAll(root);

		// The size is only known after the scene is shown. Keeps fitting the
		// bracket to the window until the user zooms
		this.widthProperty().addListener((observable, oldWidth, newWidth) -> {
			if (this.autoFit && newWidth.doubleValue() > 0) {
				this.fitToScreen();
				Platform.runLater(this::scrollToSelected);
			}
		});
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
				"Clique em uma partida para selecioná-la • B: tela versus • S: inicia a batalha • 1/2: declara o vencedor • Ctrl+Z: desfaz o último resultado • Ctrl+roda ou +/-: zoom • 0: ajusta à tela");
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

		card.getChildren().addAll(header, this.buildSlotRow(match, match.getA()), this.buildSlotRow(match, match.getB()));

		if (match.getStatus() == Status.READY) {
			card.setOnMouseClicked(event -> {
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

		if (decided) {
			boolean won = participant.equals(match.getWinner());
			Integer score = won ? match.getWinnerScore() : match.getLoserScore();
			Label scoreLabel = new Label(score != null ? String.valueOf(score) : won ? "✔" : "");
			scoreLabel.getStyleClass().add("score");
			row.getChildren().add(scoreLabel);
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
		this.selected = code;
		VBox card = this.cards.get(code);
		if (card != null) {
			this.styleSelected(card, true);
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

	// ------------------------------------------------------------------- zoom

	private void setZoom(double zoom) {
		this.zoom = Math.max(0.2, Math.min(2.5, zoom));
		this.scale.setX(this.zoom);
		this.scale.setY(this.zoom);
	}

	/**
	 * Zooms out until the whole bracket width fits the screen
	 */
	private void fitToScreen() {
		this.content.applyCss();
		this.content.layout();
		double available = this.getWidth() > 0 ? this.getWidth() : Screen.getPrimary().getVisualBounds().getWidth();
		double width = this.content.prefWidth(-1);
		// Leaves room for the vertical scroll bar
		this.setZoom(width > available ? Math.max(0.35, (available - 40) / width) : 1);
	}

	private void scrollToSelected() {
		VBox card = this.cards.get(this.selected);
		if (card == null) {
			return;
		}
		double contentWidth = this.content.getWidth() * this.zoom;
		double contentHeight = this.content.getHeight() * this.zoom;
		double viewWidth = this.scrollPane.getViewportBounds().getWidth();
		double viewHeight = this.scrollPane.getViewportBounds().getHeight();

		// Card position inside the content, without zoom
		double x = (this.canvas.getBoundsInParent().getMinX() + card.getLayoutX() + CARD_W / 2) * this.zoom;
		double y = (this.canvas.getBoundsInParent().getMinY() + card.getLayoutY() + CARD_H / 2) * this.zoom;

		if (contentWidth > viewWidth) {
			this.scrollPane.setHvalue(Math.max(0, Math.min(1, (x - viewWidth / 2) / (contentWidth - viewWidth))));
		}
		if (contentHeight > viewHeight) {
			this.scrollPane.setVvalue(Math.max(0, Math.min(1, (y - viewHeight / 2) / (contentHeight - viewHeight))));
		}
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
