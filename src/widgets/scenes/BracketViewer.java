package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import model.Opponent;
import tournament.Bracket;
import tournament.Match;
import widgets.ArcadeBackground;
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

	static final double CARD_W = 300;
	static final double ROW_H = 50;
	static final double HEADER_H = 24;
	static final double CARD_H = HEADER_H + 2 * ROW_H + 4;
	static final double SLOT_H = CARD_H + 26;
	static final double COL_GAP = 70;
	static final double COL_W = CARD_W + COL_GAP;
	static final double ROUND_TITLE_H = 34;
	static final double SECTION_TITLE_H = 50;
	static final double SECTION_GAP = 50;

	final Bracket bracket;
	final Map<Long, Opponent> opponents;
	final Consumer<String> onSelect;

	private static final double MIN_ZOOM = 0.15;
	private static final double MAX_ZOOM = 2.5;
	/**
	 * Zoom when the camera goes to a match
	 */
	static final double MATCH_ZOOM = 1.35;
	/**
	 * Inside sequential animations actions at time zero may not run, so they run
	 * at this time
	 */
	static final Duration ACTION_TIME = Duration.millis(1);

	final Pane canvas = new Pane();
	private final VBox content = new VBox(24);
	final Pane viewport = new Pane();

	final Map<Match, double[]> positions = new HashMap<Match, double[]>();
	final Map<String, VBox> cards = new HashMap<String, VBox>();
	final Map<String, HBox[]> rows = new HashMap<String, HBox[]>();
	final Map<HBox, Long> rowParticipants = new HashMap<HBox, Long>();
	final Map<HBox, Label> scores = new HashMap<HBox, Label>();
	final Map<String, Polyline> lines = new HashMap<String, Polyline>();
	private final Map<Long, Image> logos = new HashMap<Long, Image>();

	String selected;
	private Label nextTag;

	// The camera: the point of the content in the center of the screen and the
	// zoom. Where it is going is kept apart, so fast wheel turns add up.
	final DoubleProperty cameraX = new SimpleDoubleProperty();
	final DoubleProperty cameraY = new SimpleDoubleProperty();
	final DoubleProperty cameraZoom = new SimpleDoubleProperty(1);
	double targetX;
	double targetY;
	double targetZoom = 1;
	private Timeline cameraMove;
	private final Translate translate = new Translate();
	private final Scale scale = new Scale(1, 1, 0, 0);
	final BracketReplay replay = new BracketReplay(this);

	private boolean cameraReady = false;
	double dragX;
	double dragY;

	// The result being shown, see playResult
	private String pendingResult;

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
		this.content.getChildren().add(new BracketHeader(this).build(title, robotsOutside));
		this.content.getTransforms().setAll(this.translate, this.scale);

		new BracketLayout(this).build();
		this.content.getChildren().add(this.canvas);

		// The camera looks at the content through the viewport
		this.viewport.getChildren().add(this.content);
		this.viewport.getStyleClass().add("bracket-root");
		this.bindCamera();

		new BracketControls(this).install();
		this.getChildren().setAll(this.screen());
	}

	/** Clips the viewport to its size, and moves the content when the camera or the size of the viewport changes */
	private void bindCamera() {
		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(this.viewport.widthProperty());
		clip.heightProperty().bind(this.viewport.heightProperty());
		this.viewport.setClip(clip);

		this.cameraX.addListener(observable -> this.applyCamera());
		this.cameraY.addListener(observable -> this.applyCamera());
		this.cameraZoom.addListener(observable -> this.applyCamera());
		this.viewport.widthProperty().addListener(observable -> this.viewportResized());
		this.viewport.heightProperty().addListener(observable -> this.viewportResized());
	}

	/** The screen: the viewport with the scanlines over it, on a dimmed arcade background */
	private StackPane screen() {
		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		scanlines.setMouseTransparent(true);

		// Dimmed, so it does not compete with the cards
		ArcadeBackground background = new ArcadeBackground();
		background.setOpacity(0.45);

		StackPane root = new StackPane(background, this.viewport, scanlines);
		root.getStyleClass().add("screen");
		return root;
	}

	// ------------------------------------------------------------------ header

	// ------------------------------------------------------------------ cards

	/** The picture of an opponent scaled to the given size, or null when it has none. */
	ImageView logo(Long participant, double width, double height) {
		Opponent opponent = this.opponents.get(participant);
		if (opponent == null || opponent.getImage() == null) {
			return null;
		}
		Image image = this.logos.computeIfAbsent(participant, identifier -> new Image(new ByteArrayInputStream(opponent.getImage()), 160, 120, true, true));
		ImageView view = new ImageView(image);
		view.setFitWidth(width);
		view.setFitHeight(height);
		view.setPreserveRatio(true);
		view.setSmooth(true);
		return view;
	}

	/** A card with the two slots of a match, its status and its code. */
	VBox buildCard(Match match) {
		return new BracketCards(this).buildCard(match);
	}

	/** The name and team of an opponent, or a question mark when it is unknown. */

	String nameOf(Long participant) {
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
	void showNextTag() {
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

	/** Removes the tag that shows the next match, if it is shown. */
	private void removeNextTag() {
		if (this.nextTag != null) {
			this.canvas.getChildren().remove(this.nextTag);
			this.nextTag = null;
		}
	}

	/** Highlights a card as the selected match, or removes the highlight. */
	void styleSelected(VBox card, boolean selected) {
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

	/** The status of a match, as the word shown in its card. */
	String statusText(Match match) {
		return new BracketCards(this).statusText(match);
	}

	/** The code of the match of a card. */
	private String codeOf(VBox card) {
		return this.cards.entrySet().stream().filter(event -> event.getValue() == card).map(Map.Entry::getKey).findFirst().orElseThrow();
	}

	// ----------------------------------------------------------------- camera

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
			// The whole width first, then the camera zooms in the next battle
			double[] overview = this.overview();
			this.moveTo(overview[0], overview[1], overview[2], null);
			PauseTransition wait = new PauseTransition(Duration.millis(400));
			wait.setOnFinished(event -> this.focusSelected());
			wait.play();
		}
	}

	/**
	 * Camera position where the whole width of the bracket fits the screen,
	 * looking at its top
	 */
	double[] overview() {
		double width = this.content.getLayoutBounds().getWidth();
		double zoom = Math.max(MIN_ZOOM, Math.min(1, (this.viewport.getWidth() - 20) / width));
		return new double[] { width / 2, 0, zoom };
	}

	/**
	 * Center of a match card in content coordinates
	 */
	Point2D centerOf(String code) {
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
	private double[] clamp(double posX, double posY, double zoom) {
		zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
		double width = this.content.getLayoutBounds().getWidth();
		double height = this.content.getLayoutBounds().getHeight();
		double halfWidth = this.viewport.getWidth() / 2 / zoom;
		double halfHeight = this.viewport.getHeight() / 2 / zoom;
		posX = width <= 2 * halfWidth ? width / 2 : Math.max(halfWidth, Math.min(width - halfWidth, posX));
		posY = height <= 2 * halfHeight ? height / 2 : Math.max(halfHeight, Math.min(height - halfHeight, posY));
		return new double[] { posX, posY, zoom };
	}

	/**
	 * Moves the camera at once (duration null) or smoothly
	 */
	void moveTo(double posX, double posY, double zoom, Duration duration) {
		if (duration == null) {
			if (this.cameraMove != null) {
				this.cameraMove.stop();
			}
			double[] target = this.clamp(posX, posY, zoom);
			this.targetX = target[0];
			this.targetY = target[1];
			this.targetZoom = target[2];
			this.cameraX.set(target[0]);
			this.cameraY.set(target[1]);
			this.cameraZoom.set(target[2]);
		} else {
			this.moveCamera(new double[] { posX, posY, zoom }, duration);
		}
	}

	/** Moves the camera to the position in the given time, stopping the move in progress. */
	void moveCamera(double[] position, Duration duration) {
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
	Timeline cameraAnimation(double[] position, Duration duration) {
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
	void zoomAt(double posX, double posY, double factor) {
		double zoom = this.targetZoom;
		// The point of the content under the mouse, where the camera is going
		double pointX = this.targetX + (posX - this.viewport.getWidth() / 2) / zoom;
		double pointY = this.targetY + (posY - this.viewport.getHeight() / 2) / zoom;
		double newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
		this.moveCamera(new double[] { pointX - (posX - this.viewport.getWidth() / 2) / newZoom, pointY - (posY - this.viewport.getHeight() / 2) / newZoom, newZoom }, Duration.millis(220));
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

	/** Starts the animation of the result of a match, dropping the pending one. */
	private void startResultAnimation(String code) {
		this.pendingResult = null;
		this.replay.start(code);
	}


	// -------------------------------------------------------------- standings

	/** The table of placements, or null when there are none yet. */
	Node buildStandings() {
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
		for (int index = 0; index < entries.size(); index++) {
			Map.Entry<Long, Integer> entry = entries.get(index);
			Label place = new Label(entry.getValue() + "º");
			place.getStyleClass().add("place");
			Label name = new Label(this.nameOf(entry.getKey()));
			name.getStyleClass().add("place-name");
			int column = (index / perColumn) * 2;
			grid.add(place, column, index % perColumn);
			grid.add(name, column + 1, index % perColumn);
		}

		box.getChildren().addAll(title, grid);
		return box;
	}
}
