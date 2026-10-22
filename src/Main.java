import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.PersistenceException;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import jetty.server.HttpServer;
import model.Championship;
import model.Opponent;
import tournament.Bracket;
import tournament.Match;
import tournament.Status;
import tournament.ChampionshipService;
import utils.configuration.Configuration;
import utils.database.Database;
import utils.robocode.BattleControl;
import utils.robocode.BattleControl.BattleOutcome;
import utils.robocode.BattleControl.BattleResult;
import utils.robocode.BattleRecording;
import widgets.scenes.BattleArena3D;
import widgets.scenes.BattleViewer;
import widgets.scenes.BracketViewer;
import widgets.scenes.Podium;
import widgets.scenes.ShowOponent;
import widgets.scenes.StartScreen;
import widgets.ArcadeBackground;
import widgets.GameDialog;
import widgets.GameWindow;
import widgets.Sound;
import widgets.Theme;
import widgets.windows.NewConfigurationHandleWindow;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Main extends Application {

	// The current championship, its bracket and robots
	Championship championship;
	Bracket bracket;
	Map<Long, Opponent> opponents = Map.of();

	/**
	 * Code of the match chosen in the bracket screen
	 */
	String selectedMatch;

	/**
	 * Match whose result was just recorded, the bracket shows it moving through
	 * the bracket the next time it appears
	 */
	String justPlayed;

	// Point to primary stage
	private Stage primaryStage;

	// The Scenes of application
	BracketViewer bracketScene;
	private StartScreen startScene;
	/** The default music of the program: the start screen and the bracket play it */
	private Sound menuMusic;
	private static final double START_MUSIC_VOLUME = 0.15;
	static final double BRACKET_MUSIC_VOLUME = 0.10;
	private BattleViewer battleScene;
	ShowOponent opponentScene;
	Podium podiumScene;
	/** The screen is fading to the champion: the keys wait */
	boolean championTransition;

	/**
	 * Which robot of the selected match the M key shows
	 */
	int opponentIndexIWantToShow = 0;

	/**
	 * Web server for remote robots uploads
	 */
	private HttpServer httpServer;

	/**
	 * The configuration in use, to know what changed when it changes
	 */
	private Path databaseFile;
	private int webserverPort;

	/**
	 * True while robocode is running a battle
	 */
	boolean battleRunning = false;
	private final KeyBindings keys = new KeyBindings(this);
	private final ChampionshipFlow flow = new ChampionshipFlow(this);
	private final Ceremony ceremony = new Ceremony(this);
	/** Follows the configuration while the program runs, see stop */
	private final Runnable configurationListener = this::applyConfiguration;
	final BattleRunner battles = new BattleRunner(this);

	/** Starts the application. */
	public static void main(String[] args) {
		launch(args);
	}

	/** Builds the only window, opens the start screen and wires the keys. */
	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.openWindow();

		Configuration.setNewConfigurationHandle(new NewConfigurationHandleWindow());
		if (!Configuration.askForProperties()) {
			// The user cancelled the configuration and there is none stored
			GameWindow.quit();
			return;
		}
		if (!this.openDatabase()) {
			return;
		}

		// Binds the keys to actions
		primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, this.keys::handle);

		// Start the web server for remote robots uploads
		this.startWebServer();

		// Default scene
		this.showStartScreen();

		// From now on the program follows every change of the configuration
		Configuration.addListener(this.configurationListener);
	}

	/** Opens the only window, always in full screen, with the background of the start screen. */
	private void openWindow() {
		// The only window, always in full screen. Dialogs are panels inside it.
		GameWindow.init(this.primaryStage, "Robochamp - Gerenciador de campeonatos de robocode");
		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		GameWindow.setScreen(new StackPane(new ArcadeBackground(), scanlines));
	}

	/**
	 * Opens the database and loads the championship.
	 *
	 * @return false when the database could not be opened, after showing the error and quitting
	 */
	private boolean openDatabase() {
		// Initializes database
		try {
			this.databaseFile = Main.configuredDatabaseFile();
			Database.getEntityManager().close();
			this.loadChampionship();
			return true;
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			this.showError("Falha ao abrir o banco de dados " + Configuration.getDatabaseFile() + ":\n" + rootCauseMessage(runtimeException));
			GameWindow.quit();
			return false;
		}
	}

	/** Stops the web server and clears the scenes when the application closes. */
	@Override
	public void stop() {
		Configuration.removeListener(this.configurationListener);
		this.resetAllScenes();
		if (this.httpServer != null) {
			this.httpServer.stop();
		}
		Database.close();
	}

	/**
	 * Start the web server for remote robots uploads
	 */
	private void startWebServer() {
		this.webserverPort = Configuration.getWebserverPort();
		try {
			this.httpServer = new HttpServer(this.webserverPort);
		} catch (Exception exception) {
			exception.printStackTrace();
			this.httpServer = null;
			this.showError("Falha ao iniciar o servidor web na porta " + this.webserverPort + ":\n" + rootCauseMessage(exception));
		}
	}

	/**
	 * Port where the web server is listening, 0 if it is not running
	 */
	private int runningWebserverPort() {
		return this.httpServer == null ? 0 : this.httpServer.getPort();
	}

	/** The database file named in the configuration, as an absolute path. */
	private static Path configuredDatabaseFile() {
		return Path.of(Configuration.getDatabaseFile()).toAbsolutePath().normalize();
	}

	// ---------------------------------------------------------- configuration

	/** Opens the configuration window, unless a battle is running. */
	void openConfiguration() {
		if (this.battleRunning) {
			this.showError("Aguarde a batalha terminar para mudar as configurações.");
			return;
		}
		Configuration.askForProperties();
	}

	/**
	 * Makes the program follow the configuration: reopens the database and
	 * restarts the web server when they change and updates the screen. Rounds,
	 * winner criterion and robocode path are read when each battle starts.
	 */
	private void applyConfiguration() {
		boolean databaseChanged = !Main.configuredDatabaseFile().equals(this.databaseFile);
		if (databaseChanged) {
			this.databaseFile = Main.configuredDatabaseFile();
			Database.close();
			this.selectedMatch = null;
			this.justPlayed = null;
			try {
				this.loadChampionship();
			} catch (RuntimeException runtimeException) {
				runtimeException.printStackTrace();
				this.setChampionship(null);
				this.showError("Falha ao abrir o banco de dados " + this.databaseFile + ":\n" + rootCauseMessage(runtimeException));
			}
		}

		if (this.httpServer == null || Configuration.getWebserverPort() != this.webserverPort) {
			if (this.httpServer != null) {
				this.httpServer.stop();
				this.httpServer = null;
			}
			this.startWebServer();
		}

		// Updates what is on the screen
		Parent screen = GameWindow.getScreen();
		if (screen == this.startScene && this.startScene != null) {
			this.startScene.refresh(this.runningWebserverPort());
		} else if (databaseChanged) {
			// Brackets and matches of the other database are gone
			if (this.bracket == null) {
				this.resetAllScenes();
				this.showStartScreen();
			} else {
				this.showBracket();
			}
		}
	}

	/** Opens or closes the robot registration and tells the operator. */
	void toggleRegistration() {
		boolean enabled = !Configuration.isEnabledRegistration();
		Configuration.setEnabledRegistration(enabled);
		GameDialog.toast(enabled ? "● INSCRIÇÕES ABERTAS: CADASTRO DE ROBÔS LIBERADO" : "■ INSCRIÇÕES ENCERRADAS: CADASTRO DE ROBOTS BLOQUEADO", enabled);
		// Refresh start screen if it is currently displayed
		if (this.startScene != null && GameWindow.getScreen() == this.startScene) {
			this.startScene.refresh(this.runningWebserverPort());
		}
	}

	// ----------------------------------------------------------- championship

	// ---------------------------------------------------------------- screens

	/**
	 *
	 */
	/**
	 * Plays the default music, or only changes its volume when it is already playing
	 */
	void playMenuMusic(double volume) {
		if (this.menuMusic == null) {
			this.menuMusic = Sound.fromResource(StartScreen.class, "versusMusic.mp3");
			if (this.menuMusic != null) {
				this.menuMusic.setLoop(true);
			}
		}
		if (this.menuMusic != null) {
			this.menuMusic.setVolume(volume);
			this.menuMusic.play();
		}
	}

	/**
	 * A stopped sound can not play again, so the next time a new one is made
	 */
	private void stopMenuMusic() {
		if (this.menuMusic != null) {
			this.menuMusic.stop();
			this.menuMusic = null;
		}
	}

	/** Shows the start screen with the web server address. */
	protected void showStartScreen() {
		this.startScene = new StartScreen(this.runningWebserverPort());

		this.show(this.startScene);
		this.playMenuMusic(START_MUSIC_VOLUME);
	}

	/**
	 * Shows the versus screen
	 */
	void showBattle() {
		Optional<Match> playable = this.playableSelectedMatch();
		if (playable.isEmpty()) {
			return;
		}
		Match match = playable.get();

		this.battleScene = new BattleViewer();

		// Set the data!
		this.battleScene.setOpponents(this.opponents.get(match.getA().getParticipant()), this.opponents.get(match.getB().getParticipant()), this.bracket.roundName(match), match.getCode());
		this.show(this.battleScene);
		// After show, so the animation knows the size of the screen
		this.battleScene.play();
	}

	/**
	 * Saves the source code of every robot as temp/robochamp-export/team/RobotName.java
	 */
	void exportToFiles() {
		Path exportDir = SourceExporter.defaultDir();
		try {
			int exported = SourceExporter.export(exportDir);
			GameDialog.info("Exportação", exported + " robôs exportados para " + exportDir);
		} catch (IOException | PersistenceException persistenceException) {
			persistenceException.printStackTrace();
			this.showError("Falha ao exportar os robôs:\n" + persistenceException.getMessage());
		}
	}

	/**
	 * Keep things clean an silent
	 */
	void resetAllScenes() {
		this.resetAllScenes(false);
	}

	/**
	 * @param keepMenuMusic
	 *            the default music goes on, the next screen plays it
	 */
	void resetAllScenes(boolean keepMenuMusic) {

		// Resets the podium
		if (this.podiumScene != null)
			this.podiumScene.stop();
		this.podiumScene = null;

		// Resets the opponents scene
		if (this.opponentScene != null)
			this.opponentScene.stop();
		this.opponentScene = null;

		// Resets the main Scene
		if (this.battleScene != null)
			this.battleScene.stop();
		this.battleScene = null;

		// Resets initial screen
		this.startScene = null;

		// The default music stops with the screen that played it, unless the next one plays it too
		if (!keepMenuMusic) {
			this.stopMenuMusic();
		}

		// Resets the bracket scene
		this.bracketScene = null;
	}

	/**
	 * Changes the screen. The window keeps a single scene and only its content
	 * changes, so the window never gets resized to the size a new scene prefers
	 * (it happens with tiling window managers)
	 */
	void show(Parent screen) {
		GameWindow.setScreen(screen);
		Theme.enter(screen);
	}

	/** Shows an error message in a dialog. */
	void showError(String message) {
		GameDialog.error(message);
	}

	/** The message of the innermost cause of the exception. */
	static String rootCauseMessage(Throwable throwable) {
		Throwable cause = throwable;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause.getMessage();
	}

	/** Loads the current championship and keeps the selected match when it can still be played. */
	private void loadChampionship() {
		flow.loadChampionship();
	}

	/** Shows the given championship and its bracket, or nothing when it is null. */
	private void setChampionship(Championship championship) {
		flow.setChampionship(championship);
	}

	/** Draws a new championship from the registered robots. */
	void newChampionship() {
		flow.newChampionship();
	}

	/** Shows the bracket of the championship, creating it when needed. */
	void showBracket() {
		flow.showBracket();
	}

	/** The selected match when it can be played; otherwise the reason is shown and nothing is returned. */
	Optional<Match> playableSelectedMatch() {
		return flow.playableSelectedMatch();
	}

	/** Saves the result of a match and shows the winner. */
	private void recordResult(Match match, long winnerId, Integer winnerScore, Integer loserScore) {
		flow.recordResult(match, winnerId, winnerScore, loserScore);
	}

	/** Saves the result of a match; false when it could not be saved, and the error was shown. */
	boolean saveResult(Match match, long winnerId, Integer winnerScore, Integer loserScore) {
		return flow.saveResult(match, winnerId, winnerScore, loserScore);
	}

	/** Shows the winner of the match, or the champion when the bracket is finished. */
	void showResult(long winnerId) {
		flow.showResult(winnerId);
	}

	/** Declares the first or the second opponent of the selected match as the winner. */
	void declareWinner(boolean slotA) {
		flow.declareWinner(slotA);
	}

	/** Undoes the last result of the championship. */
	void undoLastResult() {
		flow.undoLastResult();
	}

	/** The name of an opponent for the messages, or a question mark when it is unknown. */
	private String nameOf(Long identifier) {
		return flow.nameOf(identifier);
	}

	/** Shows the robots of the selected match, one at a time. */
	void showNextSelectedOpponent() {
		ceremony.showNextSelectedOpponent();
	}

	/** Shows the champion, after the transition of a battle when there is one. */
	void showChampion(boolean transition) {
		ceremony.showChampion(transition);
	}

	/** Opens the podium of the championship. */
	private void openPodium() {
		ceremony.openPodium();
	}

	/** Shows the card of an opponent with the given title. */
	void showOpponent(Opponent item, String title, boolean champion) {
		ceremony.showOpponent(item, title, champion);
	}
}
