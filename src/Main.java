import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.PersistenceException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
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
import utils.robocode.BattleControl.BattleResult;
import widgets.scenes.BattleViewer;
import widgets.scenes.BracketViewer;
import widgets.scenes.ShowOponent;
import widgets.scenes.StartScreen;
import widgets.Sfx;
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
	private String selectedMatch;

	// Point to primary stage
	private Stage primaryStage;

	/**
	 * The only scene of the window, see {@link #show(Parent)}
	 */
	private Scene mainScene;

	// The Scenes of application
	private BracketViewer bracketScene;
	private StartScreen startScene;
	private BattleViewer battleScene;
	private ShowOponent opponentScene;

	/**
	 * Which robot of the selected match the M key shows
	 */
	private int opponentIndexIWantToShow = 0;

	/**
	 * Web server for remote robots uploads
	 */
	private HttpServer httpServer;

	/**
	 * True while robocode is running a battle
	 */
	private boolean battleRunning = false;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;

		Configuration.setNewConfigurationHandle(new NewConfigurationHandleWindow());
		if (!Configuration.askForProperties()) {
			// The user cancelled the configuration and there is none stored
			Platform.exit();
			return;
		}

		primaryStage.setTitle("Robochamp - Gerenciador de campeonatos de robocode");

		// Initializes database
		try {
			Database.getEntityManager().close();
			this.loadChampionship();
		} catch (RuntimeException e) {
			e.printStackTrace();
			this.showError("Falha ao abrir o banco de dados " + Configuration.getDatabaseFile() + ":\n" + rootCauseMessage(e));
			Platform.exit();
			return;
		}

		// Binds the keys to actions
		primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKey);

		// Actions to do when closing the application
		primaryStage.setOnCloseRequest(event -> Platform.exit());

		this.mainScene = Theme.apply(new Scene(new StackPane(), 1280, 800));
		primaryStage.setScene(this.mainScene);

		// Shows everything!
		primaryStage.setMaximized(true);
		primaryStage.show();

		// Default scene
		this.showStartScreen();

		// Start the web server for remote robots uploads
		this.startWebServer();
	}

	@Override
	public void stop() {
		this.resetAllScenes();
		if (this.httpServer != null) {
			this.httpServer.stop();
		}
		Database.close();
	}

	private void handleKey(KeyEvent event) {

		// All events with control key
		if (event.isControlDown()) {
			switch (event.getCode()) {
			// Blocks or liberate the robots registration
			case S -> this.toggleRegistration();
			// Draws a new championship
			case G -> this.newChampionship();
			// Undo the last result
			case Z -> this.undoLastResult();
			default -> {
				// If control is down block the access for another keys
			}
			}
			return;
		}

		switch (event.getCode()) {
		// Initial screen
		case I -> {
			this.resetAllScenes();
			this.showStartScreen();
		}
		// The bracket
		case R -> this.showBracket();
		// Selects the next match to be played
		case N -> {
			Sfx.blip();
			this.selectedMatch = null;
			this.showBracket();
		}
		// Show the battle screen
		case B -> {
			this.resetAllScenes();
			this.showBattle();
		}
		// Starts the battle on robocode!
		case S -> this.startsBattle();
		// The operator says who won
		case DIGIT1, NUMPAD1 -> this.declareWinner(true);
		case DIGIT2, NUMPAD2 -> this.declareWinner(false);
		// Circle through the opponents of the match every time the M key is pressed
		case M -> {
			this.resetAllScenes();
			this.showNextSelectedOpponent();
		}
		// Exports all robots to files
		case X -> this.exportToFiles();
		default -> {
		}
		}
	}

	/**
	 * Start the web server for remote robots uploads
	 */
	private void startWebServer() {
		try {
			this.httpServer = new HttpServer(Configuration.getWebserverPort());
		} catch (Exception e) {
			e.printStackTrace();
			this.showError("Falha ao iniciar o servidor web na porta " + Configuration.getWebserverPort() + ":\n" + rootCauseMessage(e));
		}
	}

	private void toggleRegistration() {
		boolean enabled = !Configuration.isEnabledRegistration();
		Configuration.setEnabledRegistration(enabled);
		Theme.alert(AlertType.INFORMATION, enabled ? "Cadastro de novos robôs liberado" : "Cadastro de novos robôs bloqueado").show();
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

	private void newChampionship() {
		long robots;
		try {
			robots = ChampionshipService.countOpponents();
		} catch (PersistenceException e) {
			this.showError("Falha ao ler os robôs:\n" + rootCauseMessage(e));
			return;
		}

		if (robots < 2) {
			this.showError("São necessários ao menos dois robôs cadastrados para sortear um campeonato.");
			return;
		}

		CheckBox repechage = new CheckBox("Repescagem (dupla eliminação): quem perde uma vez ainda pode ser campeão");
		repechage.setWrapText(true);
		repechage.setSelected(true);
		CheckBox finalReset = new CheckBox("Final com revanche se o campeão da repescagem vencer a grande final");
		finalReset.setSelected(true);
		finalReset.setWrapText(true);
		finalReset.disableProperty().bind(repechage.selectedProperty().not());

		String warning = this.championship != null && !this.championship.getResults().isEmpty() ? "\n\nATENÇÃO: o campeonato atual já tem resultados e será substituído." : "";
		Label message = new Label("Sortear um novo campeonato com os " + robots + " robôs cadastrados?" + warning);
		message.setWrapText(true);

		Alert dialog = Theme.alert(AlertType.CONFIRMATION, null);
		dialog.setTitle("Novo campeonato");
		dialog.setHeaderText("Novo campeonato");
		dialog.getDialogPane().setContent(new VBox(14, message, repechage, finalReset));
		dialog.getDialogPane().setPrefWidth(640);

		if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
			return;
		}

		try {
			this.selectedMatch = null;
			this.setChampionship(ChampionshipService.create(repechage.isSelected(), finalReset.isSelected(), new SecureRandom()));
			this.showBracket();
		} catch (RuntimeException e) {
			e.printStackTrace();
			this.showError("Falha ao criar o campeonato:\n" + rootCauseMessage(e));
		}
	}

	private void showBracket() {
		try {
			this.loadChampionship();
		} catch (RuntimeException e) {
			e.printStackTrace();
			this.showError("Falha ao carregar o campeonato:\n" + rootCauseMessage(e));
			return;
		}

		if (this.bracket == null) {
			this.showError("Nenhum campeonato criado ainda. Pressione Ctrl + G para sortear um.");
			return;
		}

		this.resetAllScenes();

		long outside = Math.max(0, ChampionshipService.countOpponents() - this.opponents.size());
		this.bracketScene = new BracketViewer(this.championship.getName(), this.bracket, this.opponents, this.selectedMatch, code -> {
			Sfx.blip();
			this.selectedMatch = code;
			this.opponentIndexIWantToShow = 0;
		}, outside);

		this.show(this.bracketScene);
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
			this.showChampion();
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
		try {
			this.setChampionship(ChampionshipService.recordResult(this.championship, match.getCode(), winnerId, winnerScore, loserScore));
		} catch (RuntimeException e) {
			e.printStackTrace();
			this.showError("Falha ao salvar o resultado:\n" + rootCauseMessage(e));
			return;
		}

		this.resetAllScenes();
		if (this.bracket.isFinished()) {
			this.showChampion();
		} else {
			this.showOpponent(this.opponents.get(winnerId), "VENCEU!", false);
		}
	}

	private void declareWinner(boolean slotA) {
		Optional<Match> playable = this.playableSelectedMatch();
		if (playable.isEmpty()) {
			return;
		}
		Match match = playable.get();
		long winner = slotA ? match.getA().getParticipant() : match.getB().getParticipant();

		Alert confirm = Theme.alert(AlertType.CONFIRMATION, "Declarar " + this.nameOf(winner) + " vencedor da partida " + match.getCode() + "?");
		confirm.setHeaderText("Resultado manual");
		if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
			this.recordResult(match, winner, null, null);
		}
	}

	private void undoLastResult() {
		if (this.championship == null || this.championship.getResults().isEmpty()) {
			this.showError("Não há resultados para desfazer.");
			return;
		}

		var last = this.championship.getResults().get(this.championship.getResults().size() - 1);
		Alert confirm = Theme.alert(AlertType.CONFIRMATION,
				"Desfazer o resultado da partida " + last.getMatchCode() + " (" + this.nameOf(last.getWinner().getId()) + " venceu " + this.nameOf(last.getLoser().getId()) + ")?");
		confirm.setHeaderText("Desfazer resultado");
		if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
			return;
		}

		try {
			this.selectedMatch = last.getMatchCode();
			this.setChampionship(ChampionshipService.undoLastResult(this.championship));
			this.showBracket();
		} catch (RuntimeException e) {
			e.printStackTrace();
			this.showError("Falha ao desfazer o resultado:\n" + rootCauseMessage(e));
		}
	}

	private String nameOf(Long id) {
		Opponent opponent = this.opponents.get(id);
		return opponent == null ? "?" : opponent.getFullName();
	}

	// ---------------------------------------------------------------- screens

	/**
	 *
	 */
	protected void showStartScreen() {
		this.startScene = new StartScreen();
		this.startScene.playBackgroundMusic();

		this.show(this.startScene);
	}

	protected void startsBattle() {

		if (this.battleRunning) {
			return;
		}

		Optional<Match> playable = this.playableSelectedMatch();
		if (playable.isEmpty()) {
			return;
		}
		Match match = playable.get();

		// Chooses the robocode dir
		if (!Files.isDirectory(Path.of(Configuration.getRobocodePath(), "robots"))) {
			Configuration.askForProperties();
		}

		List<Opponent> fighters = List.of(this.opponents.get(match.getA().getParticipant()), this.opponents.get(match.getB().getParticipant()));
		String robocodePath = Configuration.getRobocodePath();
		int rounds = Configuration.getRobocodeRounds();
		String startScript = Configuration.getStartBattleScript();
		var criterion = Configuration.getWinnerCriterion();

		// Robocode runs in another process, waiting for it in the JavaFX thread
		// would freeze the screen
		Task<List<BattleResult>> battle = new Task<List<BattleResult>>() {
			@Override
			protected List<BattleResult> call() throws Exception {
				return BattleControl.startBattle(fighters, robocodePath, rounds, startScript, criterion);
			}
		};

		battle.setOnSucceeded(event -> {
			this.battleRunning = false;
			List<BattleResult> ranking = battle.getValue();
			this.recordResult(match, ranking.get(0).opponent().getId(), ranking.get(0).score(), ranking.get(1).score());
		});

		battle.setOnFailed(event -> {
			this.battleRunning = false;
			battle.getException().printStackTrace();
			this.showError("Falha ao executar a batalha:\n" + battle.getException().getMessage() + "\n\nVocê pode informar o vencedor com as teclas 1 ou 2.");
		});

		this.battleRunning = true;
		Thread thread = new Thread(battle, "robocode-battle");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Shows the versus screen
	 */
	private void showBattle() {
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
	private void showNextSelectedOpponent() {
		Optional<Match> playable = this.playableSelectedMatch();
		if (playable.isEmpty()) {
			return;
		}
		Match match = playable.get();

		Long id = this.opponentIndexIWantToShow % 2 == 0 ? match.getA().getParticipant() : match.getB().getParticipant();
		this.showOpponent(this.opponents.get(id), null, false);

		// Controls the index values to circle between opponents
		this.opponentIndexIWantToShow++;
	}

	private void showChampion() {
		this.resetAllScenes();
		this.showOpponent(this.opponents.get(this.bracket.getChampion()), "CAMPEÃO!", true);
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
		} catch (IOException | RuntimeException e) {
			e.printStackTrace();
			this.showError("Falha ao exibir oponente");
		}
	}

	/**
	 * Saves the source code of every robot as
	 * temp/robochamp-export/team/RobotName.java
	 */
	private void exportToFiles() {

		Path exportDir = Path.of(Configuration.getTempDir(), "robochamp-export");

		try {
			List<Opponent> all = Database.read(em -> em.createQuery("select o from Opponent o", Opponent.class).getResultList());

			for (Opponent opponent : all) {
				if (opponent.getSourceCode() == null) {
					continue;
				}
				Path file = exportDir.resolve(opponent.getTeam()).resolve(opponent.getName().replace('.', '/') + ".java");
				Files.createDirectories(file.getParent());
				Files.writeString(file, opponent.getSourceCode());
			}

			Theme.alert(AlertType.INFORMATION, all.size() + " robôs exportados para " + exportDir).show();

		} catch (IOException | PersistenceException e) {
			e.printStackTrace();
			this.showError("Falha ao exportar os robôs:\n" + e.getMessage());
		}
	}

	/**
	 * Keep things clean an silent
	 */
	private void resetAllScenes() {

		// Resets the opponents scene
		if (this.opponentScene != null)
			this.opponentScene.stop();
		this.opponentScene = null;

		// Resets the main Scene
		if (this.battleScene != null)
			this.battleScene.stop();
		this.battleScene = null;

		// Resets initial screen
		if (this.startScene != null)
			this.startScene.stop();
		this.startScene = null;

		// Resets the bracket scene
		this.bracketScene = null;
	}

	/**
	 * Changes the screen. The window keeps a single scene and only its content
	 * changes, so the window never gets resized to the size a new scene prefers
	 * (it happens with tiling window managers)
	 */
	private void show(Parent screen) {
		this.mainScene.setRoot(screen);
		screen.requestFocus();
		Theme.enter(screen);
	}

	private void showError(String message) {
		Theme.alert(AlertType.ERROR, message).showAndWait();
	}

	private static String rootCauseMessage(Throwable e) {
		Throwable cause = e;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause.getMessage();
	}
}
