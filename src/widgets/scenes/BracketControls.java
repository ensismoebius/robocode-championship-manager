package widgets.scenes;

import javafx.scene.layout.Priority;
import widgets.Sfx;
import widgets.Theme;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;

/**
 * The controls of the bracket: the wheel and the drag move the camera, and the keys zoom, show the overview and move
 */
final class BracketControls {

	private final BracketViewer host;

	BracketControls(BracketViewer host) {
		this.host = host;
	}

	/** Installs the mouse and keyboard controls of the bracket. */
	void install() {
		this.installWheel();
		this.installDrag();
		this.installKeys();
	}

	/** Ctrl + wheel zooms towards the mouse, the wheel alone moves */
	private void installWheel() {
		host.viewport.addEventFilter(ScrollEvent.SCROLL, event -> {
			if (host.replay.skip()) {
				event.consume();
				return;
			}
			if (event.isControlDown()) {
				if (event.getDeltaY() != 0) {
					host.zoomAt(event.getX(), event.getY(), event.getDeltaY() > 0 ? 1.15 : 1 / 1.15);
				}
			} else {
				host.moveTo(host.cameraX.get() - event.getDeltaX() / host.cameraZoom.get(), host.cameraY.get() - event.getDeltaY() / host.cameraZoom.get(), host.cameraZoom.get(), null);
			}
			event.consume();
		});
	}

	/** Dragging with the mouse moves the camera */
	private void installDrag() {
		host.viewport.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			if (host.replay.skip()) {
				event.consume();
				return;
			}
			host.dragX = event.getX();
			host.dragY = event.getY();
		});
		host.viewport.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
			double zoom = host.cameraZoom.get();
			host.moveTo(host.cameraX.get() - (event.getX() - host.dragX) / zoom, host.cameraY.get() - (event.getY() - host.dragY) / zoom, zoom, null);
			host.dragX = event.getX();
			host.dragY = event.getY();
		});
	}

	/** The keys: plus and minus zoom, zero shows the overview, and the arrows move the camera */
	private void installKeys() {
		host.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			// Any key ends the result animation, and still does its job
			host.replay.skip();
			double step = 160 / host.targetZoom;
			switch (event.getCode()) {
			case PLUS, EQUALS, ADD -> host.zoomAt(host.viewport.getWidth() / 2, host.viewport.getHeight() / 2, 1.3);
			case MINUS, SUBTRACT -> host.zoomAt(host.viewport.getWidth() / 2, host.viewport.getHeight() / 2, 1 / 1.3);
			case DIGIT0, NUMPAD0 -> host.moveCamera(host.overview(), Duration.millis(500));
			case LEFT -> host.moveCamera(new double[] { host.targetX - step, host.targetY, host.targetZoom }, Duration.millis(200));
			case RIGHT -> host.moveCamera(new double[] { host.targetX + step, host.targetY, host.targetZoom }, Duration.millis(200));
			case UP -> host.moveCamera(new double[] { host.targetX, host.targetY - step, host.targetZoom }, Duration.millis(200));
			case DOWN -> host.moveCamera(new double[] { host.targetX, host.targetY + step, host.targetZoom }, Duration.millis(200));
			default -> {
				return;
			}
			}
			event.consume();
		});
	}
}
