import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.concurrent.Task;
import model.Opponent;
import tournament.Match;
import utils.configuration.Configuration;
import utils.robocode.BattleControl.WinnerCriterion;
import utils.robocode.BattleControl;
import utils.robocode.BattleControl.BattleOutcome;
import utils.robocode.BattleControl.BattleResult;
import utils.robocode.BattleRecording;
import widgets.scenes.BattleArena3D;
import widgets.GameWindow;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Starts the battles of the selected match on Robocode, in the background, and shows their result.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class BattleRunner {

	private final Main host;

	/** Creates the runner of the battles, for the host window. */
	BattleRunner(Main host) {
		this.host = host;
	}

	/** Asks for the Robocode folder and starts a battle of the selected match. */
	void startsBattle() {
		if (host.battleRunning || !this.chooseRobocodeDir()) {
			return;
		}

		Optional<Match> playable = host.playableSelectedMatch();
		if (playable.isEmpty()) {
			return;
		}
		Match match = playable.get();

		List<Opponent> fighters = List.of(host.opponents.get(match.getA().getParticipant()), host.opponents.get(match.getB().getParticipant()));
		String robocodePath = Configuration.getRobocodePath();
		int rounds = Configuration.getRobocodeRounds();
		String startScript = Configuration.getStartBattleScript();
		var criterion = Configuration.getWinnerCriterion();

		// In 3D robocode runs without its window and the battle is replayed here
		boolean in3D = Configuration.isBattleView3D() && Platform.isSupported(ConditionalFeature.SCENE3D);
		BattleArena3D arena = this.openArena(match, fighters, in3D);

		Task<BattleOutcome> battle = this.battleTask(fighters, robocodePath, rounds, startScript, criterion, in3D, arena);
		battle.setOnSucceeded(event -> this.onBattleSucceeded(battle.getValue(), match, fighters, arena));
		battle.setOnFailed(event -> this.onBattleFailed(battle.getException(), arena));

		host.battleRunning = true;
		Thread thread = new Thread(battle, "robocode-battle");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * The battle on Robocode, to run in a background thread: it runs in another
	 * process, waiting for it in the JavaFX thread would freeze the screen. While it
	 * runs, the loading of the arena shows how far it is.
	 */
	private Task<BattleOutcome> battleTask(List<Opponent> fighters, String robocodePath, int rounds, String startScript, WinnerCriterion criterion, boolean in3D, BattleArena3D arena) {
		return new Task<BattleOutcome>() {
			/** Runs the battle on Robocode in a background thread. */
			@Override
			protected BattleOutcome call() throws Exception {
				return BattleControl.startBattle(fighters, robocodePath, rounds, startScript, criterion, in3D, progress -> {
					if (arena != null) {
						Platform.runLater(() -> arena.setLoadingProgress(progress));
					}
				});
			}
		};
	}

	/**
	 * Shows the 3D arena of a match, when the battle is viewed in 3D.
	 *
	 * @return the arena, or null when the battle is not viewed in 3D
	 */
	BattleArena3D openArena(Match match, List<Opponent> fighters, boolean in3D) {
		if (!in3D) {
			return null;
		}
		BattleArena3D arena = new BattleArena3D(fighters, host.bracket.importance(match));
		arena.setStage(host.bracket.roundName(match), host.bracket.importance(match));
		host.resetAllScenes();
		host.show(arena);
		return arena;
	}

	/**
	 * Robocode is needed to fight. Asks for its folder if it is not where the
	 * configuration says.
	 *
	 * @return false if there is no robocode folder, the battle can not start
	 */
	boolean chooseRobocodeDir() {
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
	void onBattleSucceeded(BattleOutcome outcome, Match match, List<Opponent> fighters, BattleArena3D arena) {
		List<BattleResult> ranking = outcome.ranking();
		BattleRecording recording = outcome.recording();
		long winner = ranking.get(0).opponent().getId();

		// Saved at once, then shown when the replay ends
		if (!host.saveResult(match, winner, ranking.get(0).score(), ranking.get(1).score())) {
			host.battleRunning = false;
			return;
		}
		if (arena != null && GameWindow.getScreen() == arena && !recording.isEmpty()) {
			arena.play(recording, () -> {
				host.battleRunning = false;
				MatchPhotos.keep(host.championship.getId(), arena, match, fighters);
				host.showResult(winner);
			});
		} else {
			host.battleRunning = false;
			host.showResult(winner);
		}
	}

	/**
	 * Tells the operator what went wrong and that the winner can be declared by hand
	 */
	void onBattleFailed(Throwable failure, BattleArena3D arena) {
		host.battleRunning = false;
		failure.printStackTrace();
		host.showError("Falha ao executar a batalha:\n" + failure.getMessage() + "\n\nVocê pode informar o vencedor com as teclas 1 ou 2.");
		if (arena != null && GameWindow.getScreen() == arena) {
			host.showBattle();
		}
	}
}
