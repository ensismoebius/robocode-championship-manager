package widgets;

import java.util.ArrayDeque;
import java.util.Deque;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.EventTarget;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The only window of the program. It is always in full screen, and dialogs are
 * panels shown above the current screen, like the menus of a game, not other
 * windows.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class GameWindow {

	private static Stage stage;

	private static final StackPane screenLayer = new StackPane();
	private static final StackPane overlayLayer = new StackPane();
	/** Black, over everything: it fades in and out between two screens */
	private static final Region curtain = new Region();

	/**
	 * Open modal panels (their backdrops), the top one first
	 */
	private static final Deque<Node> modals = new ArrayDeque<>();

	private static Runnable exitAction = Platform::exit;
	private static boolean closing = false;
	private static boolean fullScreenRequested = false;

	/** The window is static: this class is not instantiated. */
	private GameWindow() {
	}

	/**
	 * Shows the window in full screen and keeps it so
	 */
	public static void init(Stage primaryStage, String title) {
		GameWindow.stage = primaryStage;

		GameWindow.curtain.setStyle("-fx-background-color: black;");
		GameWindow.curtain.setOpacity(0);
		GameWindow.curtain.setMouseTransparent(true);

		// When empty the overlay lets the clicks reach the screen
		GameWindow.overlayLayer.setPickOnBounds(false);
		Scene scene = Theme.apply(new Scene(new StackPane(GameWindow.screenLayer, GameWindow.overlayLayer, GameWindow.curtain), 1280, 800));

		// While a panel is open, the keys are only for it
		scene.addEventFilter(KeyEvent.ANY, event -> {
			Node top = GameWindow.modals.peek();
			if (top != null && !GameWindow.isInside(event.getTarget(), top)) {
				event.consume();
			}
		});

		primaryStage.setScene(scene);
		primaryStage.setTitle(title);
		lockFullScreen(primaryStage);
		primaryStage.setOnCloseRequest(event -> GameWindow.quit());
		watchSize(primaryStage);

		primaryStage.show();
		// Asked after the window appears: some window managers (like XWayland
		// ones) ignore the request made before
		PauseTransition mapped = new PauseTransition(Duration.millis(300));
		mapped.setOnFinished(event -> {
			GameWindow.fullScreenRequested = true;
			primaryStage.setFullScreen(true);
		});
		mapped.play();
	}

	/** No way out of the full screen, not even with Escape: when the window leaves it, it goes back */
	private static void lockFullScreen(Stage primaryStage) {
		primaryStage.setFullScreenExitHint("");
		primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
		primaryStage.fullScreenProperty().addListener((observable, wasFullScreen, fullScreen) -> {
			if (!fullScreen && !GameWindow.closing) {
				Platform.runLater(() -> {
					if (primaryStage.isShowing() && !GameWindow.closing) {
						primaryStage.setFullScreen(true);
					}
				});
			}
		});
	}

	/** The window manager may take the window out of the full screen without JavaFX noticing, so the size is watched too */
	private static void watchSize(Stage primaryStage) {
		PauseTransition check = new PauseTransition(Duration.millis(400));
		check.setOnFinished(event -> GameWindow.keepFullScreen());
		primaryStage.widthProperty().addListener(observable -> check.playFromStart());
		primaryStage.heightProperty().addListener(observable -> check.playFromStart());
	}

	/**
	 * Asks the full screen again if the window does not cover its screen
	 */
	private static void keepFullScreen() {
		Stage window = GameWindow.stage;
		if (!GameWindow.fullScreenRequested || GameWindow.closing || !window.isShowing()) {
			return;
		}
		Rectangle2D screen = Screen.getScreensForRectangle(window.getX(), window.getY(), Math.max(1, window.getWidth()), Math.max(1, window.getHeight())).stream().findFirst()
				.orElse(Screen.getPrimary()).getBounds();
		if (window.getWidth() < screen.getWidth() - 2 || window.getHeight() < screen.getHeight() - 2) {
			// JavaFX may still think it is in full screen, then asking again
			// would do nothing
			window.setFullScreen(false);
			window.setFullScreen(true);
		}
	}

	/** Returns the window. */
	public static Window getWindow() {
		return GameWindow.stage;
	}

	/**
	 * Fades the whole window to black, or from black back to the screen
	 *
	 * @param then
	 *            runs when the fade ends, may be null
	 */
	public static void fadeCurtain(boolean toBlack, double seconds, Runnable then) {
		FadeTransition fade = new FadeTransition(Duration.seconds(seconds), GameWindow.curtain);
		fade.setFromValue(GameWindow.curtain.getOpacity());
		fade.setToValue(toBlack ? 1 : 0);
		fade.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
		fade.setOnFinished(event -> {
			if (then != null) {
				then.run();
			}
		});
		fade.play();
	}

	/**
	 * Black at once, no animation
	 */
	public static void setCurtain(boolean black) {
		GameWindow.curtain.setOpacity(black ? 1 : 0);
	}

	/** Shows a screen in the window, in place of the one that was there, and gives it the focus. */
	public static void setScreen(Parent screen) {
		GameWindow.screenLayer.getChildren().setAll(screen);
		if (GameWindow.modals.isEmpty()) {
			screen.requestFocus();
		}
	}

	/** Returns the screen. */
	public static Parent getScreen() {
		return GameWindow.screenLayer.getChildren().isEmpty() ? null : (Parent) GameWindow.screenLayer.getChildren().get(0);
	}

	/**
	 * Ends the program, closing the open panels first: each one keeps a nested
	 * event loop running, which would keep the program alive
	 */
	public static void quit() {
		GameWindow.closing = true;
		while (!GameWindow.modals.isEmpty()) {
			Platform.exitNestedEventLoop(GameWindow.modals.pop(), null);
		}
		GameWindow.exitAction.run();
	}

	/**
	 * Replaces what stops the program when it quits: the tests change it, since the
	 * toolkit can be stopped only once. The window is open again for the next
	 * program, so the dialogs answer as usual.
	 */
	public static void setExitAction(Runnable action) {
		GameWindow.exitAction = action;
		GameWindow.closing = false;
	}

	/**
	 * Shows the panel above the screen and waits, like showAndWait, until
	 * {@link #close(Node, Object)} is called for it. Enter fires the focused
	 * button or the default one, Escape the cancel one.
	 *
	 * @param defaultButton
	 *            gets the focus, may be null
	 * @param cancelButton
	 *            fired by Escape, may be null
	 * @return the result given to close, null if the program is closing
	 */
	public static Object showModal(Region panel, Button defaultButton, Button cancelButton) {
		if (GameWindow.closing) {
			return null;
		}

		panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		StackPane backdrop = new StackPane(panel);
		backdrop.getStyleClass().add("overlay-backdrop");

		panelKeys(panel, defaultButton, cancelButton);

		GameWindow.modals.push(backdrop);
		GameWindow.overlayLayer.getChildren().add(backdrop);
		appear(backdrop, panel);

		if (defaultButton != null) {
			defaultButton.requestFocus();
		} else {
			panel.requestFocus();
		}

		return Platform.enterNestedEventLoop(backdrop);
	}

	/**
	 * Shows a page that covers the whole window, like a screen of its own, and waits until it is closed with
	 * {@link #close(Region, Object)}. The screen below stays where it is and the keys are only for the page.
	 *
	 * @return the result given to close, null if the program is closing
	 */
	public static Object showPage(Region page, Button defaultButton, Button cancelButton) {
		if (GameWindow.closing) {
			return null;
		}

		StackPane backdrop = new StackPane(page);
		// Found like any panel of the game, but the page itself is the background: nothing dims, no margin
		backdrop.getStyleClass().addAll("overlay-backdrop", "full-page");
		backdrop.prefWidthProperty().bind(GameWindow.overlayLayer.widthProperty());
		backdrop.prefHeightProperty().bind(GameWindow.overlayLayer.heightProperty());
		page.prefWidthProperty().bind(backdrop.widthProperty());
		page.prefHeightProperty().bind(backdrop.heightProperty());

		panelKeys(page, defaultButton, cancelButton);

		GameWindow.modals.push(backdrop);
		GameWindow.overlayLayer.getChildren().add(backdrop);
		if (defaultButton != null) {
			defaultButton.requestFocus();
		} else {
			page.requestFocus();
		}

		return Platform.enterNestedEventLoop(backdrop);
	}

	/** Enter fires the default button (or the focused one), Escape fires the cancel button; nothing else reaches the screens */
	private static void panelKeys(Region panel, Button defaultButton, Button cancelButton) {
		panel.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER) {
				Node focused = panel.getScene().getFocusOwner();
				if (focused instanceof Button button) {
					button.fire();
				} else if (defaultButton != null) {
					defaultButton.fire();
				}
			} else if (event.getCode() == KeyCode.ESCAPE && cancelButton != null) {
				cancelButton.fire();
			}
		});
		// Nothing typed in the panel reaches the shortcuts of the screens
		panel.addEventHandler(KeyEvent.ANY, KeyEvent::consume);
	}

	/** The panel and its backdrop fade in, the panel pops up with a small overshoot */
	private static void appear(StackPane backdrop, Region panel) {
		FadeTransition fade = new FadeTransition(Duration.millis(160), backdrop);
		fade.setFromValue(0);
		fade.setToValue(1);
		ScaleTransition pop = new ScaleTransition(Duration.millis(220), panel);
		pop.setFromX(0.85);
		pop.setFromY(0.85);
		pop.setToX(1);
		pop.setToY(1);
		pop.setInterpolator(Theme.OVERSHOOT);
		new ParallelTransition(fade, pop).play();
	}

	/**
	 * Closes a panel shown by {@link #showModal(Region, Button, Button)}, which
	 * then returns the result
	 */
	public static void close(Region panel, Object result) {
		Node backdrop = panel.getParent();
		if (backdrop == null || !GameWindow.modals.remove(backdrop)) {
			return;
		}
		GameWindow.overlayLayer.getChildren().remove(backdrop);

		// The focus goes back to the panel below or to the screen
		Node top = GameWindow.modals.peek();
		if (top != null) {
			top.requestFocus();
		} else if (GameWindow.getScreen() != null) {
			GameWindow.getScreen().requestFocus();
		}

		Platform.exitNestedEventLoop(backdrop, result);
	}

	/**
	 * Shows something above everything without blocking, like a toast
	 */
	public static void showNotice(Node notice) {
		notice.setMouseTransparent(true);
		GameWindow.overlayLayer.getChildren().add(notice);
	}

	/** Removes a notice from the window. */
	public static void removeNotice(Node notice) {
		GameWindow.overlayLayer.getChildren().remove(notice);
	}

	/** Tells whether a node is the ancestor or lies inside it. */
	private static boolean isInside(EventTarget target, Node ancestor) {
		if (!(target instanceof Node node)) {
			return false;
		}
		for (Node current = node; current != null; current = current.getParent()) {
			if (current == ancestor) {
				return true;
			}
		}
		return false;
	}
}
