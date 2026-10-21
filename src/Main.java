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
import tournament.Bracket.Match;
import tournament.Bracket.Status;
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
	private Championship championship;
	private Bracket bracket;
	private Map<Long, Opponent> opponents = Map.of();

	/**
	 * Code of the match chosen in the bracket screen
	 */
	String selectedMatch;

	/**
	 * Match whose result was just recorded, the bracket shows it moving through
	 * the bracket the next time it appears
	 */
	private String justPlayed;

	// Point to primary stage
	private Stage primaryStage;

	// The Scenes of application
	BracketViewer bracketScene;
	private StartScreen startScene;
	/** The default music of the program: the start screen and the bracket play it */
	private Sound menuMusic;
	private static final double START_MUSIC_VOLUME = 0.15;
	private static final double BRACKET_MUSIC_VOLUME = 0.10;
	private BattleViewer battleScene;
	private ShowOponent opponentScene;
	Podium podiumScene;
	/** The screen is fading to the champion: the keys wait */
	boolean championTransition;

	/**
	 * Which robot of the selected match the M key shows
	 */
	private int opponentIndexIWantToShow = 0;

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
	private boolean battleRunning = false;
	private final KeyBindings keys = new KeyBindings(this);

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;

		// The only window, always in full screen. Dialogs are panels inside it.
		GameWindow.init(primaryStage, "Robochamp - Gerenciador de campeonatos de robocode");
		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		GameWindow.setScreen(new StackPane(new ArcadeBackground(), scanlines));

		Configuration.setNewConfigurationHandle(new NewConfigurationHandleWindow());
		if (!Configuration.askForProperties()) {
			// The user cancelled the configuration and there is none stored
			GameWindow.quit();
			return;
		}

		// Initializes database
		try {
			this.databaseFile = Main.configuredDatabaseFile();
			Database.getEntityManager().close();
			this.loadChampionship();
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			this.showError("Falha ao abrir o banco de dados " + Configuration.getDatabaseFile() + ":\n" + rootCauseMessage(runtimeException));
			GameWindow.quit();
			return;
		}

		// Binds the keys to actions
		primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, this.keys::handle);

		// Start the web server for remote robots uploads
		this.startWebServer();

		// Default scene
		this.showStartScreen();

		// From now on the program follows every change of the configuration
		Configuration.addListener(this::applyConfiguration);
	}

	@Override
	public void stop() {
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

	private static Path configuredDatabaseFile() {
		return Path.of(Configuration.getDatabaseFile()).toAbsolutePath().normalize();
	}

	// ---------------------------------------------------------- configuration

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

	/**
	 * Loads the current championship from the database and keeps the selected
	 * match if it still can be played
	 */
	private void loadChampionship() {
		this.championship = ChampionshipService.current();
		this.setChampionship(this.championship);
	}

	private void setChampionship(Championship championship) {
		this.championship = championship;
		if (championship == null) {
			this.bracket = null;
			this.opponents = Map.of();
			this.selectedMatch = null;
			return;
		}

		this.bracket = ChampionshipService.bracketOf(championship);
		this.opponents = ChampionshipService.opponentsOf(championship);

		Match selected = this.selectedMatch == null ? null : this.bracket.getMatch(this.selectedMatch);
		if (selected == null || selected.getStatus() != Status.READY) {
			this.selectedMatch = this.bracket.getNextMatch().map(Match::getCode).orElse(null);
		}
	}

	void newChampionship() {
		long robots;
		try {
			robots = ChampionshipService.countOpponents();
		} catch (PersistenceException persistenceException) {
			this.showError("Falha ao ler os robôs:\n" + rootCauseMessage(persistenceException));
			return;
		}

		if (robots < 2) {
			this.showError("São necessários ao menos dois robôs cadastrados para sortear um campeonato.");
			return;
		}

		CheckBox repechage = new CheckBox("Repescagem (dupla eliminação): quem perde uma vez ainda pode ser campeão");
		repechage.setWrapText(true);
		repechage.setSelected(true);

		String warning = this.championship != null && !this.championship.getResults().isEmpty() ? "\n\nATENÇÃO: o campeonato atual já tem resultados e será substituído." : "";
		Label message = new Label("Sortear um novo campeonato com os " + robots + " robôs cadastrados?" + warning);
		message.setWrapText(true);
		message.getStyleClass().add("game-dialog-message");

		if (!GameDialog.confirm("Novo campeonato", new VBox(14, message, repechage))) {
			return;
		}

		try {
			this.selectedMatch = null;
			this.justPlayed = null;
			this.setChampionship(ChampionshipService.create(repechage.isSelected(), new SecureRandom()));
			this.showBracket();
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			this.showError("Falha ao criar o campeonato:\n" + rootCauseMessage(runtimeException));
		}
	}

	void showBracket() {
		try {
			this.loadChampionship();
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			this.showError("Falha ao carregar o campeonato:\n" + rootCauseMessage(runtimeException));
			return;
		}

		if (this.bracket == null) {
			this.showError("Nenhum campeonato criado ainda. Pressione Ctrl + G para sortear um.");
			return;
		}

		this.resetAllScenes(true);

		long outside = Math.max(0, ChampionshipService.countOpponents() - this.opponents.size());
		this.bracketScene = new BracketViewer(this.championship.getName(), this.bracket, this.opponents, this.selectedMatch, code -> {
			this.selectedMatch = code;
			this.opponentIndexIWantToShow = 0;
		}, outside);
		if (this.justPlayed != null) {
			this.bracketScene.playResult(this.justPlayed);
			this.justPlayed = null;
		}

		this.show(this.bracketScene);
		this.playMenuMusic(BRACKET_MUSIC_VOLUME);
	}

	/**
	 * @return the selected match if it can be played, otherwise shows an error
	 */
	private Optional<Match> playableSelectedMatch() {
		if (this.bracket == null) {
			this.showError("Nenhum campeonato criado ainda. Pressione Ctrl + G para sortear um.");
			return Optional.empty();
		}
		if (this.bracket.isFinished()) {
			this.showChampion(false);
			return Optional.empty();
		}
		Match match = this.selectedMatch == null ? null : this.bracket.getMatch(this.selectedMatch);
		if (match == null || match.getStatus() != Status.READY) {
			this.showError("Nenhuma partida selecionada. Pressione R e clique em uma partida pronta.");
			return Optional.empty();
		}
		return Optional.of(match);
	}

	/**
	 * Saves the result and shows the winner
	 */
	private void recordResult(Match match, long winnerId, Integer winnerScore, Integer loserScore) {
		if (this.saveResult(match, winnerId, winnerScore, loserScore)) {
			this.showResult(winnerId);
		}
	}

	/**
	 * @return false if it could not be saved (the error was shown)
	 */
	private boolean saveResult(Match match, long winnerId, Integer winnerScore, Integer loserScore) {
		try {
			this.setChampionship(ChampionshipService.recordResult(this.championship, match.getCode(), winnerId, winnerScore, loserScore));
			this.justPlayed = match.getCode();
			return true;
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			this.showError("Falha ao salvar o resultado:\n" + rootCauseMessage(runtimeException));
			return false;
		}
	}

	/**
	 * The winner of the match, or the champion
	 */
	private void showResult(long winnerId) {
		if (this.bracket.isFinished()) {
			this.showChampion(true);
		} else {
			this.resetAllScenes();
			this.showOpponent(this.opponents.get(winnerId), "VENCEU!", false);
		}
	}

	void declareWinner(boolean slotA) {
		Optional<Match> playable = this.playableSelectedMatch();
		if (playable.isEmpty()) {
			return;
		}
		Match match = playable.get();
		long winner = slotA ? match.getA().getParticipant() : match.getB().getParticipant();

		if (GameDialog.confirm("Resultado manual", "Declarar " + this.nameOf(winner) + " vencedor da partida " + match.getCode() + "?")) {
			this.recordResult(match, winner, null, null);
		}
	}

	void undoLastResult() {
		if (this.championship == null || this.championship.getResults().isEmpty()) {
			this.showError("Não há resultados para desfazer.");
			return;
		}

		var last = this.championship.getResults().get(this.championship.getResults().size() - 1);
		if (!GameDialog.confirm("Desfazer resultado",
				"Desfazer o resultado da partida " + last.getMatchCode() + " (" + this.nameOf(last.getWinner().getId()) + " venceu " + this.nameOf(last.getLoser().getId()) + ")?")) {
			return;
		}

		try {
			this.selectedMatch = last.getMatchCode();
			this.justPlayed = null;
			this.setChampionship(ChampionshipService.undoLastResult(this.championship));
			MatchPhotos.forget(this.championship.getId(), last.getMatchCode());
			this.showBracket();
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			this.showError("Falha ao desfazer o resultado:\n" + rootCauseMessage(runtimeException));
		}
	}

	private String nameOf(Long identifier) {
		Opponent opponent = this.opponents.get(identifier);
		return opponent == null ? "?" : opponent.getFullName();
	}

	// ---------------------------------------------------------------- screens

	/**
	 *
	 */
	/**
	 * Plays the default music, or only changes its volume when it is already playing
	 */
	private void playMenuMusic(double volume) {
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

	protected void showStartScreen() {
		this.startScene = new StartScreen(this.runningWebserverPort());

		this.show(this.startScene);
		this.playMenuMusic(START_MUSIC_VOLUME);
	}

	protected void startsBattle() {
		if (this.battleRunning || !this.chooseRobocodeDir()) {
			return;
		}

		Optional<Match> playable = this.playableSelectedMatch();
		if (playable.isEmpty()) {
			return;
		}
		Match match = playable.get();

		List<Opponent> fighters = List.of(this.opponents.get(match.getA().getParticipant()), this.opponents.get(match.getB().getParticipant()));
		String robocodePath = Configuration.getRobocodePath();
		int rounds = Configuration.getRobocodeRounds();
		String startScript = Configuration.getStartBattleScript();
		var criterion = Configuration.getWinnerCriterion();

		// In 3D robocode runs without its window and the battle is replayed here
		boolean in3D = Configuration.isBattleView3D() && Platform.isSupported(ConditionalFeature.SCENE3D);
		BattleArena3D arena = in3D ? new BattleArena3D(fighters) : null;
		if (arena != null) {
			arena.setStage(this.bracket.roundName(match), this.bracket.importance(match));
			this.resetAllScenes();
			this.show(arena);
		}

		// Robocode runs in another process, waiting for it in the JavaFX thread
		// would freeze the screen
		Task<BattleOutcome> battle = new Task<BattleOutcome>() {
			@Override
			protected BattleOutcome call() throws Exception {
				return BattleControl.startBattle(fighters, robocodePath, rounds, startScript, criterion, in3D);
			}
		};
		battle.setOnSucceeded(event -> this.onBattleSucceeded(battle.getValue(), match, fighters, arena));
		battle.setOnFailed(event -> this.onBattleFailed(battle.getException(), arena));

		this.battleRunning = true;
		Thread thread = new Thread(battle, "robocode-battle");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Robocode is needed to fight. Asks for its folder if it is not where the
	 * configuration says.
	 *
	 * @return false if there is no robocode folder, the battle can not start
	 */
	private boolean chooseRobocodeDir() {
		// Comes first because the configuration may change the database and so the match.
		if (Files.isDirectory(Path.of(Configuration.getRobocodePath(), "robots"))) {
			return true;
		}
		Configuration.askForProperties();
		return Files.isDirectory(Path.of(Configuration.getRobocodePath(), "robots"));
	}

	/**
	 * Saves the result, then shows it when the replay of the 3D battle ends
	 */
	private void onBattleSucceeded(BattleOutcome outcome, Match match, List<Opponent> fighters, BattleArena3D arena) {
		List<BattleResult> ranking = outcome.ranking();
		BattleRecording recording = outcome.recording();
		long winner = ranking.get(0).opponent().getId();

		// Saved at once, then shown when the replay ends
		if (!this.saveResult(match, winner, ranking.get(0).score(), ranking.get(1).score())) {
			this.battleRunning = false;
			return;
		}
		if (arena != null && GameWindow.getScreen() == arena && !recording.isEmpty()) {
			arena.play(recording, () -> {
				this.battleRunning = false;
				MatchPhotos.keep(this.championship.getId(), arena, match, fighters);
				this.showResult(winner);
			});
		} else {
			this.battleRunning = false;
			this.showResult(winner);
		}
	}

	/**
	 * Tells the operator what went wrong and that the winner can be declared by hand
	 */
	private void onBattleFailed(Throwable failure, BattleArena3D arena) {
		this.battleRunning = false;
		failure.printStackTrace();
		this.showError("Falha ao executar a batalha:\n" + failure.getMessage() + "\n\nVocê pode informar o vencedor com as teclas 1 ou 2.");
		if (arena != null && GameWindow.getScreen() == arena) {
			this.showBattle();
		}
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
	 * Shows the robots of the selected match, one at a time
	 */
	void showNextSelectedOpponent() {
		Optional<Match> playable = this.playableSelectedMatch();
		if (playable.isEmpty()) {
			return;
		}
		Match match = playable.get();

		Long identifier = this.opponentIndexIWantToShow % 2 == 0 ? match.getA().getParticipant() : match.getB().getParticipant();
		this.showOpponent(this.opponents.get(identifier), null, false);

		// Controls the index values to circle between opponents
		this.opponentIndexIWantToShow++;
	}

	/**
	 * The champion, at once and with everything. The operator controls the rest:
	 * Space shows the third place, then the second and then the first again.
	 *
	 * @param transition
	 *            comes from a battle: the screen and the sound fade to black and
	 *            silence, the default music of the program rises to the maximum and
	 *            only then the screen fades in on the champion
	 */
	private void showChampion(boolean transition) {
		Parent current = GameWindow.getScreen();
		if (!transition || current == null) {
			this.openPodium();
			this.podiumScene.startMusic(1);
			this.podiumScene.play();
			return;
		}

		// Nothing else happens while the screen changes
		this.championTransition = true;
		double fadeSeconds = 1.5;
		if (current instanceof BattleArena3D arena) {
			arena.fadeOutAudio(fadeSeconds);
		}
		GameWindow.fadeCurtain(true, fadeSeconds, () -> {
			// Black and silent: the podium is put in place, with its music on at zero
			this.openPodium();
			if (this.podiumScene == null) {
				GameWindow.fadeCurtain(false, 0.6, () -> this.championTransition = false);
				return;
			}
			this.podiumScene.startMusic(0);
			Timeline rise = new Timeline(new KeyFrame(Duration.seconds(2.2), new KeyValue(this.podiumScene.musicVolumeProperty(), 1, Interpolator.EASE_OUT)));
			rise.setOnFinished(event -> {
				// At the maximum, the screen fades in and the champion comes in
				GameWindow.fadeCurtain(false, 1.4, () -> this.championTransition = false);
				PauseTransition wait = new PauseTransition(Duration.seconds(0.5));
				wait.setOnFinished(done -> {
					if (this.podiumScene != null) {
						this.podiumScene.play();
					}
				});
				wait.play();
			});
			rise.play();
		});
	}

	/**
	 * Shows the podium of the championship (nothing happens on it yet)
	 */
	private void openPodium() {
		this.resetAllScenes();
		try {
			List<Opponent> podium = new ArrayList<Opponent>();
			List<Long> ids = this.bracket.getPodium();
			for (Long identifier : ids) {
				podium.add(this.opponents.get(identifier));
			}
			this.podiumScene = new Podium(podium, MatchPhotos.photosOf(this.championship.getId(), ids), this.opponents);
			this.show(this.podiumScene);
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			this.podiumScene = null;
			this.showError("Falha ao exibir o campeão");
		}
	}

	/**
	 * @param item
	 */
	private void showOpponent(Opponent item, String title, boolean champion) {
		// Show the opponent
		try {
			this.opponentScene = new ShowOponent();
			this.opponentScene.setOponent(item, title, champion);
			this.show(this.opponentScene);
			this.opponentScene.play();
		} catch (IOException | RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			this.showError("Falha ao exibir oponente");
		}
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
	private void resetAllScenes(boolean keepMenuMusic) {

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
	private void show(Parent screen) {
		GameWindow.setScreen(screen);
		Theme.enter(screen);
	}

	private void showError(String message) {
		GameDialog.error(message);
	}

	private static String rootCauseMessage(Throwable throwable) {
		Throwable cause = throwable;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause.getMessage();
	}
}
