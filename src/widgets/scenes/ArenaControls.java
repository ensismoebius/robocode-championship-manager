package widgets.scenes;

import static widgets.scenes.BattleArena3D.SPEEDS;
import widgets.scenes.BattleArena3D.Phase;
import widgets.scenes.BattleArena3D.ViewMode;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import widgets.Sfx;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The keys and the mouse of the battle arena: the play and the pause, the speed,
 * the views, skipping the moments and the finish of the battle.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaControls {

	private double lastDragX;
	private double lastDragY;

	private final BattleArena3D host;

	/** Creates the controls of the arena: the keys, the mouse and the views. */
	ArenaControls(BattleArena3D host) {
		this.host = host;
	}

	// ---------------------------------------------------------------- controls

	/** Installs the keyboard and mouse controls of the arena. */
	void installControls() {
		host.setFocusTraversable(true);
		host.addEventFilter(KeyEvent.KEY_PRESSED, this::keyPressed);
		host.addEventFilter(MouseEvent.MOUSE_PRESSED, this::mousePressed);
		host.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
		host.addEventFilter(ScrollEvent.SCROLL, this::scrolled);
	}

	/** The keys of the arena: the shortcuts of the battle, the quit key is left to the window */
	private void keyPressed(KeyEvent event) {
		// Quitting still works, the other shortcuts wait for the battle
		if (event.isControlDown() && event.getCode() == KeyCode.Q) {
			return;
		}
		switch (event.getCode()) {
		case V -> host.cycleView();
		case SPACE -> {
			host.paused = !host.paused;
			Sfx.blip();
		}
		case RIGHT, PLUS, EQUALS, ADD -> host.speed = Math.min(SPEEDS.length - 1, host.speed + 1);
		case LEFT, MINUS, SUBTRACT -> host.speed = Math.max(0, host.speed - 1);
		case ENTER -> this.skip(false);
		case ESCAPE -> this.skip(true);
		default -> {
		}
		}
		host.hud.updateInfo();
		// Nothing else happens during the battle (like declaring a winner by
		// accident)
		event.consume();
	}

	/** A press of the mouse: the drag starts from here, and the arena takes the focus */
	private void mousePressed(MouseEvent event) {
		this.lastDragX = event.getX();
		this.lastDragY = event.getY();
		host.yawSpeed = 0;
		host.requestFocus();
	}

	/** Dragging turns the camera around the field, when the view is free */
	private void mouseDragged(MouseEvent event) {
		// Dragging takes the camera from the director
		if (host.viewMode == ViewMode.CINEMATIC) {
			this.setView(ViewMode.FREE);
		}
		if (host.viewMode != ViewMode.FREE) {
			return;
		}
		double deltaX = event.getX() - this.lastDragX;
		double deltaY = event.getY() - this.lastDragY;
		this.lastDragX = event.getX();
		this.lastDragY = event.getY();
		host.targetYaw += deltaX * 0.3;
		host.yawSpeed = deltaX * 0.3 * 60;
		host.freePitch = Math.max(-85, Math.min(-8, host.freePitch + deltaY * 0.2));
	}

	/** The wheel zooms the camera in and out, within its limits */
	private void scrolled(ScrollEvent event) {
		host.targetZoom = Math.max(0.45, Math.min(2.5, host.targetZoom * (event.getDeltaY() > 0 ? 0.9 : 1.1)));
	}

	/** Changes the view of the camera; the free view starts from where the director was. */
	void setView(ViewMode mode) {
		if (mode == ViewMode.FREE && host.viewMode == ViewMode.CINEMATIC) {
			// Starts from where the director was
			host.targetYaw = host.yaw = Math.toDegrees(Math.atan2(host.look[0] - host.eye[0], host.look[2] - host.eye[2]));
		}
		host.viewMode = mode;
		host.targetYaw = switch (mode) {
		case ISOMETRIC -> Math.round((host.targetYaw - 45) / 90) * 90 + 45;
		case TOP_DOWN -> Math.round(host.targetYaw / 360) * 360;
		default -> host.targetYaw;
		};
		host.yawSpeed = 0;
		if (mode == ViewMode.CINEMATIC) {
			host.director.pickShot(true);
		}
		host.hud.updateInfo();
	}

	/**
	 * Enter: the next robot of the presentation, ends the replay, or the whole
	 * battle. Escape skips the whole presentation.
	 *
	 * @param all
	 *            skips the whole presentation
	 */
	void skip(boolean all) {
		if (host.phase == Phase.SHOWCASE) {
			if (all || host.versus) {
				host.showcase.endShowcase();
			} else {
				host.showcase.showcaseRobot(host.showcaseOrder + 1);
			}
		} else if (host.phase == Phase.FLYBY) {
			host.rounds.endFlyBy();
		} else if (host.phase == Phase.FREEZE || host.phase == Phase.REPLAY) {
			host.showcase.endReplay();
		} else if (host.phase != Phase.LOADING && host.phase != Phase.FINISHED) {
			this.finish();
		}
	}

	/** Ends the battle: stops the timer and the showcase music. */
	void finish() {
		host.phase = Phase.FINISHED;
		host.timer.stop();
		host.stopShowcaseMusic();
		if (host.onFinished != null) {
			// The screen decides what comes next: the sound goes on until the arena
			// leaves the screen, or fades out with {@link #fadeOutAudio}
			Runnable callback = host.onFinished;
			host.onFinished = null;
			callback.run();
		} else {
			host.audio.stop();
		}
	}
}
