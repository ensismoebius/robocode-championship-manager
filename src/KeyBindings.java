import javafx.scene.input.KeyEvent;
import widgets.Sfx;
import widgets.GameDialog;
import widgets.GameWindow;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The keys of the championship manager: the plain keys work on the screen on
 * show, the keys with control change the championship. On the trophies ceremony
 * the keys go to the podium.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class KeyBindings {

	private final Main host;

	/** Creates the key bindings of the host window. */
	KeyBindings(Main host) {
		this.host = host;
	}

	/** Handles a key press: the keys with control, the keys of the ceremony and the plain keys of the screen. */
	void handle(KeyEvent event) {
		if (host.championTransition) {
			return;
		}
		// All events with control key
		if (event.isControlDown()) {
			this.handleControlKey(event);
			return;
		}
		// The trophies ceremony: the operator controls it
		if (this.handlePodiumKey(event)) {
			return;
		}
		this.handleScreenKey(event);
		this.handleOperatorKey(event);
	}

	/** The keys that change the screen: the initial screen, the bracket, the next match, the battle and the next opponent */
	private void handleScreenKey(KeyEvent event) {
		switch (event.getCode()) {
		// Initial screen
		case I -> {
			host.resetAllScenes();
			host.showStartScreen();
		}
		// The bracket
		case R -> host.showBracket();
		// Selects the next match to be played
		case N -> {
			Sfx.blip();
			host.selectedMatch = null;
			host.showBracket();
			if (host.bracketScene != null) {
				host.bracketScene.focusSelected();
			}
		}
		// Show the battle screen
		case B -> {
			host.resetAllScenes();
			host.showBattle();
		}
		// Circle through the opponents of the match every time the M key is pressed
		case M -> {
			host.resetAllScenes();
			host.showNextSelectedOpponent();
		}
		default -> {
		}
		}
	}

	/** The keys of the operator: the battle, who won, the export and the configuration */
	private void handleOperatorKey(KeyEvent event) {
		switch (event.getCode()) {
		// Starts the battle on robocode!
		case S -> host.battles.startsBattle();
		// The operator says who won
		case DIGIT1, NUMPAD1 -> host.declareWinner(true);
		case DIGIT2, NUMPAD2 -> host.declareWinner(false);
		// Exports all robots to files
		case X -> host.exportToFiles();
		// Reviews or changes the configuration
		case C -> host.openConfiguration();
		default -> {
		}
		}
	}

	/**
	 * The keys with control: blocks the registration, draws a championship, undoes
	 * a result or quits
	 */
	private void handleControlKey(KeyEvent event) {
		switch (event.getCode()) {
		// Blocks or liberate the robots registration
		case S -> host.toggleRegistration();
		// Draws a new championship
		case G -> host.newChampionship();
		// Undo the last result
		case Z -> host.undoLastResult();
		// Ends the program, the window has no close button in full screen
		case Q -> {
			if (GameDialog.confirm("Sair", "Fechar o gerenciador de campeonatos?")) {
				GameWindow.quit();
			}
		}
		// If control is down block the access for another keys
		default -> {
		}
		}
	}

	/**
	 * On the trophies ceremony the keys go to the podium
	 *
	 * @return true if the key was used by the ceremony
	 */
	private boolean handlePodiumKey(KeyEvent event) {
		if (host.podiumScene == null || GameWindow.getScreen() != host.podiumScene) {
			return false;
		}
		switch (event.getCode()) {
		case SPACE, ENTER, RIGHT -> {
			host.podiumScene.next();
			return true;
		}
		case LEFT -> {
			host.podiumScene.previous();
			return true;
		}
		default -> {
			return false;
		}
		}
	}
}
