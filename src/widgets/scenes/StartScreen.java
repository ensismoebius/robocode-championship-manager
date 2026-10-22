package widgets.scenes;

import java.io.IOException;
import java.io.InputStream;

import javafx.animation.FadeTransition;
import javafx.beans.InvalidationListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import utils.configuration.Configuration;
import utils.network.Network;
import widgets.ArcadeBackground;
import widgets.GameDialog;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The title screen
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class StartScreen extends StackPane {

	private static final String[][] SHORTCUTS = { //
			{ "R", "Chaveamento" }, //
			{ "N", "Próxima partida" }, //
			{ "B", "Tela de versus" }, //
			{ "S", "Inicia a batalha" }, //
			{ "1 2", "Declara o vencedor" }, //
			{ "M", "Robôs da partida" }, //
			{ "^G", "Sorteia um campeonato" }, //
			{ "^Z", "Desfaz o último resultado" }, //
			{ "^S", "Abre/fecha as inscrições" }, //
			{ "X", "Exporta o código fonte" }, //
			{ "C", "Configurações" }, //
			{ "^Q", "Sair" }, //
			{ "I", "Esta tela" } };


	private final Label title = new Label();
	private final Label subtitle = new Label();
	private final Label badge = new Label();
	private VBox controls;
	private HBox controlsHint;
	private FadeTransition controlsFade;

	/**
	 * Shows the controls while Alt is held. It watches the whole scene, so it
	 * works wherever the focus is.
	 */
	/**
	 * Alt released in another window never arrives here
	 */
	private final InvalidationListener focusWatcher = observable -> this.showControls(false);

	private final EventHandler<KeyEvent> altWatcher = event -> {
		if (event.getEventType() == KeyEvent.KEY_PRESSED || event.getEventType() == KeyEvent.KEY_RELEASED) {
			this.showControls(event.isAltDown() && event.getEventType() == KeyEvent.KEY_PRESSED);
		}
	};
	private final Label address = new Label();
	private final VBox registration;
	private final BorderPane layout;

	/**
	 * @param serverPort
	 *            port where the registration web server is listening, 0 if it is
	 *            not running
	 */
	public StartScreen(int serverPort) {
		super();

		ImageView robocodeLogo = logo();
		VBox titleBlock = titleBlock();
		Pane horizonLayout = horizonLayout(robocodeLogo, titleBlock);

		// Top: controls and registration status
		StackPane controlsArea = controlsArea();
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox top = new HBox(controlsArea, spacer, this.badge);
		top.setAlignment(Pos.TOP_LEFT);

		// Bottom: where the teams register their robots
		this.registration = registration();

		this.layout = new BorderPane(null, top, null, null, null);
		this.layout.setPadding(new Insets(28));
		this.refresh(serverPort);

		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		scanlines.setMouseTransparent(true);

		this.getChildren().setAll(new ArcadeBackground(), horizonLayout, this.layout, scanlines);
	}

	/** The robocode logo, which has a black background that blends with the sky */
	private static ImageView logo() {
		ImageView robocodeLogo = new ImageView();
		try (InputStream logo = StartScreen.class.getResourceAsStream("/assets/robocodeLogo.png")) {
			robocodeLogo.setImage(new Image(logo));
		} catch (IOException ioException) {
			GameDialog.error(ioException.getMessage());
		}
		robocodeLogo.setPreserveRatio(true);
		robocodeLogo.setSmooth(true);
		// The logo has a black background, this blends it with the sky
		robocodeLogo.setBlendMode(BlendMode.LIGHTEN);
		return robocodeLogo;
	}

	/** The title, the subtitle and the blinking "press R" */
	private VBox titleBlock() {
		this.title.getStyleClass().add("game-title");
		Theme.pulseGlow(this.title, Theme.ACCENT);

		this.subtitle.getStyleClass().add("game-subtitle");
		// An empty subtitle takes no space
		this.subtitle.managedProperty().bind(this.subtitle.textProperty().isNotEmpty());

		Label key = new Label("R");
		key.getStyleClass().add("keycap");
		Label pressBefore = new Label("PRESSIONE");
		pressBefore.getStyleClass().add("press-start");
		Label pressAfter = new Label("PARA O CHAVEAMENTO");
		pressAfter.getStyleClass().add("press-start");
		HBox pressStart = new HBox(16, pressBefore, key, pressAfter);
		pressStart.setAlignment(Pos.CENTER);
		Theme.blink(pressStart);

		VBox titleBlock = new VBox(14, this.title, this.subtitle, new Region(), pressStart);
		titleBlock.setAlignment(Pos.CENTER);
		return titleBlock;
	}

	/**
	 * The logo stays in the sky, the title on the grid: both fit the screen when
	 * it is resized
	 */
	private static Pane horizonLayout(ImageView robocodeLogo, VBox titleBlock) {
		Pane horizonLayout = new Pane(robocodeLogo, titleBlock) {
			/** Places the background and the stars of the start screen on the horizon. */
			@Override
			protected void layoutChildren() {
				double width = this.getWidth();
				double horizon = this.getHeight() * ArcadeBackground.HORIZON;

				// Fits between the top of the screen and the horizon
				robocodeLogo.setFitWidth(Math.min(560, width * 0.4));
				robocodeLogo.setFitHeight(Math.max(40, horizon * 0.42));
				double logoWidth = robocodeLogo.getLayoutBounds().getWidth();
				double logoHeight = robocodeLogo.getLayoutBounds().getHeight();
				robocodeLogo.relocate((width - logoWidth) / 2, horizon - logoHeight - 24);

				// Long titles shrink to fit the screen
				titleBlock.autosize();
				double scale = Math.min(1, width * 0.92 / titleBlock.getWidth());
				titleBlock.setScaleX(scale);
				titleBlock.setScaleY(scale);
				// Scaling is around the center, keeps the top right below the horizon
				double shrunk = titleBlock.getHeight() * (1 - scale) / 2;
				titleBlock.relocate((width - titleBlock.getWidth()) / 2, horizon + 28 - shrunk);
			}
		};
		horizonLayout.setMouseTransparent(true);
		return horizonLayout;
	}

	/**
	 * The controls panel, shown while Alt is held, with the hint that says so
	 */
	private StackPane controlsArea() {
		GridPane shortcuts = new GridPane();
		shortcuts.getStyleClass().add("shortcuts");
		for (int index = 0; index < SHORTCUTS.length; index++) {
			Label cap = new Label(SHORTCUTS[index][0]);
			cap.getStyleClass().add("keycap");
			Label description = new Label(SHORTCUTS[index][1]);
			description.getStyleClass().add("shortcut-description");
			shortcuts.addRow(index, cap, description);
		}
		Label controlsTitle = new Label("CONTROLES");
		controlsTitle.getStyleClass().add("panel-title");
		this.controls = new VBox(controlsTitle, shortcuts);
		this.controls.getStyleClass().add("panel");
		this.controls.setMaxHeight(Region.USE_PREF_SIZE);
		this.controls.setVisible(false);

		// The controls only show while Alt is held, this tells it
		Label holdBefore = new Label("SEGURE");
		Label holdKey = new Label("ALT");
		holdKey.getStyleClass().add("keycap");
		Label holdAfter = new Label("PARA VER OS CONTROLES");
		holdBefore.getStyleClass().add("hold-hint");
		holdAfter.getStyleClass().add("hold-hint");
		this.controlsHint = new HBox(10, holdBefore, holdKey, holdAfter);
		this.controlsHint.setAlignment(Pos.CENTER_LEFT);
		this.controlsHint.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		StackPane controlsArea = new StackPane(this.controlsHint, this.controls);
		controlsArea.setAlignment(Pos.TOP_LEFT);
		this.watchScene();
		return controlsArea;
	}

	/** Watches the Alt key on the scene the panel is in, and stops watching the scene it leaves */
	private void watchScene() {
		this.sceneProperty().addListener((observable, oldScene, newScene) -> {
			if (oldScene != null) {
				oldScene.removeEventFilter(KeyEvent.ANY, this.altWatcher);
				if (oldScene.getWindow() != null) {
					oldScene.getWindow().focusedProperty().removeListener(this.focusWatcher);
				}
			}
			if (newScene != null) {
				newScene.addEventFilter(KeyEvent.ANY, this.altWatcher);
				if (newScene.getWindow() != null) {
					newScene.getWindow().focusedProperty().addListener(this.focusWatcher);
				}
			}
		});
	}

	/** The address where the teams register their robots */
	private VBox registration() {
		Label caption = new Label("CADASTRE SEU ROBÔ EM");
		caption.getStyleClass().add("address-caption");
		this.address.getStyleClass().add("address");
		this.address.setEffect(Theme.glow(Theme.ACCENT, 14));
		VBox registration = new VBox(caption, this.address);
		registration.getStyleClass().add("address-box");
		registration.setMaxWidth(Region.USE_PREF_SIZE);
		BorderPane.setAlignment(registration, Pos.CENTER);
		return registration;
	}

	/**
	 * Shows the current registration status and web server address
	 *
	 * @param serverPort
	 *            port where the registration web server is listening, 0 if it is
	 *            not running
	 */
	public void refresh(int serverPort) {
		this.title.setText(Configuration.getTitle());
		this.subtitle.setText(Configuration.getSubtitle());

		boolean running = serverPort > 0;
		boolean open = running && Configuration.isEnabledRegistration();

		this.badge.getStyleClass().removeAll("badge-open", "badge-closed");
		if (!running) {
			this.badge.setText("■ SERVIDOR DE INSCRIÇÕES PARADO");
		} else {
			this.badge.setText(open ? "● INSCRIÇÕES ABERTAS" : "■ INSCRIÇÕES ENCERRADAS");
		}
		this.badge.getStyleClass().add(open ? "badge-open" : "badge-closed");

		this.address.setText("http://" + Network.getLocalIpAddress() + ":" + serverPort);
		this.layout.setBottom(open ? this.registration : null);
	}

	/** Shows or hides the panel with the controls of the start screen. */
	private void showControls(boolean show) {
		if (show == this.controls.isVisible()) {
			return;
		}
		if (this.controlsFade != null) {
			this.controlsFade.stop();
		}
		this.controlsHint.setVisible(!show);
		if (show) {
			this.controls.setOpacity(0);
			this.controls.setVisible(true);
			this.controlsFade = new FadeTransition(Duration.millis(150), this.controls);
			this.controlsFade.setToValue(1);
		} else {
			this.controlsFade = new FadeTransition(Duration.millis(120), this.controls);
			this.controlsFade.setToValue(0);
			this.controlsFade.setOnFinished(event -> this.controls.setVisible(false));
		}
		this.controlsFade.play();
	}
}
