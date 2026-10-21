package support;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import widgets.GameWindow;

/**
 * Starts the JavaFX toolkit once for the whole test run, and runs code on its
 * thread, so the scenes and controls can be built and tested.
 */
public final class FxSupport {

	private static boolean started;
	private static boolean gameWindowReady;

	private FxSupport() {
	}

	/** Starts the JavaFX toolkit, if it is not started yet. */
	public static synchronized void start() {
		if (started) {
			return;
		}
		// Closing the last window must not stop the toolkit: the other tests still need it
		Platform.setImplicitExit(false);
		CountDownLatch latch = new CountDownLatch(1);
		try {
			Platform.startup(latch::countDown);
		} catch (IllegalStateException alreadyStarted) {
			latch.countDown();
		}
		try {
			if (!latch.await(30, TimeUnit.SECONDS)) {
				throw new IllegalStateException("JavaFX did not start");
			}
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(interrupted);
		}
		started = true;
	}

	/** Runs the code on the JavaFX thread and returns its result. */
	public static <T> T run(Supplier<T> code) {
		start();
		AtomicReference<T> result = new AtomicReference<T>();
		AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
		CountDownLatch done = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				result.set(code.get());
			} catch (Throwable thrown) {
				failure.set(thrown);
			} finally {
				done.countDown();
			}
		});
		try {
			if (!done.await(60, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Timed out on the JavaFX thread");
			}
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(interrupted);
		}
		if (failure.get() != null) {
			throw new IllegalStateException(failure.get());
		}
		return result.get();
	}

	/** Shows the node in a real window, which stays open until {@link #close(Stage)}. */
	public static Stage show(Node node) {
		return run(() -> {
			Stage window = new Stage();
			window.setScene(new Scene(new StackPane(node), 800, 600));
			window.show();
			return window;
		});
	}

	/** Closes a window shown by {@link #show(Node)}. */
	public static void close(Stage window) {
		runVoid(window::close);
	}

	/**
	 * Shows the node in a real window for the given time, so the animations of
	 * the node run their frames, then closes the window.
	 */
	public static void showFor(Node node, long millis) {
		Stage stage = show(node);
		sleep(millis);
		close(stage);
	}

	/** Lets the animations run for the given time, on the test thread. */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/** Opens the game window the program uses, once for the whole run, so the screens and dialogs of the program can be shown. */
	public static synchronized void gameWindow() {
		if (gameWindowReady) {
			return;
		}
		runVoid(() -> {
			Stage stage = new Stage();
			GameWindow.init(stage, "Robochamp - Testes");
			stage.show();
		});
		gameWindowReady = true;
	}

	/**
	 * Answers the next dialog of the game window: the dialogs wait for their
	 * answer, so this closes the first one that shows up, from another thread.
	 */
	public static void answerNextDialog(Object answer) {
		onNextDialog(panel -> GameWindow.close(panel, answer));
	}

	/**
	 * Runs the action on the panel of the next dialog of the game window, from
	 * another thread, when it shows up. The action must close the dialog, the
	 * dialogs wait for it.
	 */
	public static void onNextDialog(Consumer<Region> action) {
		onNextDialogs(action);
	}

	/**
	 * Runs the actions on the dialogs that show up one after another: each action
	 * runs on the next dialog, once the dialog before it was closed.
	 */
	@SafeVarargs
	public static void onNextDialogs(Consumer<Region>... actions) {
		Thread answering = new Thread(() -> {
			for (Consumer<Region> action : actions) {
				for (int attempt = 0; attempt < 200; attempt++) {
					sleep(50);
					if (runOnDialog(action)) {
						break;
					}
				}
			}
		}, "answer-dialog");
		answering.setDaemon(true);
		answering.start();
	}

	/** Runs the action on the dialog shown in the game window, on the JavaFX thread, if there is one. */
	private static boolean runOnDialog(Consumer<Region> action) {
		return run(() -> {
			// The window may not exist yet: the program opens it after this was asked
			if (GameWindow.getWindow() == null || GameWindow.getWindow().getScene() == null) {
				return false;
			}
			Node backdrop = dialogBackdrop(GameWindow.getWindow().getScene().getRoot());
			if (backdrop == null) {
				return false;
			}
			action.accept((Region) ((Parent) backdrop).getChildrenUnmodifiable().get(0));
			return true;
		});
	}

	/** The backdrop of the dialogs, the first one found in the tree of nodes, or null. */
	private static Node dialogBackdrop(Node node) {
		if (node.getStyleClass().contains("overlay-backdrop")) {
			return node;
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				Node found = dialogBackdrop(child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/** Tells whether a dialog of the game window is open. */
	public static boolean dialogShowing() {
		return run(() -> GameWindow.getWindow() != null && dialogBackdrop(GameWindow.getWindow().getScene().getRoot()) != null);
	}

	/** Presses a key of the program: the key goes to the screen, the window hands it to the program. */
	public static void press(KeyCode code, boolean control) {
		runVoid(() -> GameWindow.getScreen().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, control, false, false)));
	}

	/** Presses the key of a dialog that is going to show: the dialog gets the answer of the button. */
	public static void pressAndAnswer(KeyCode code, boolean control, String answer) {
		onNextDialog(dialog -> DialogFields.button(dialog, answer).fire());
		press(code, control);
	}

	/** Runs the code on the JavaFX thread, without a result. */
	public static void runVoid(Runnable code) {
		run(() -> {
			code.run();
			return null;
		});
	}
}
