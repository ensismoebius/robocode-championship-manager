import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.Parent;
import javafx.util.Duration;
import model.Opponent;
import tournament.Match;
import widgets.scenes.BattleArena3D;
import widgets.scenes.Podium;
import widgets.scenes.ShowOponent;
import widgets.GameWindow;

/**
 * The ceremonies of the main window: the next opponent, the champion and the podium
 */
final class Ceremony {

	private final Main host;

	/** Creates the ceremonies for the given main window. */
	Ceremony(Main host) {
		this.host = host;
	}

	/**
	 * Shows the robots of the selected match, one at a time
	 */
	void showNextSelectedOpponent() {
		Optional<Match> playable = host.playableSelectedMatch();
		if (playable.isEmpty()) {
			return;
		}
		Match match = playable.get();

		Long identifier = host.opponentIndexIWantToShow % 2 == 0 ? match.getA().getParticipant() : match.getB().getParticipant();
		this.showOpponent(host.opponents.get(identifier), null, false);

		// Controls the index values to circle between opponents
		host.opponentIndexIWantToShow++;
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
	void showChampion(boolean transition) {
		Parent current = GameWindow.getScreen();
		if (!transition || current == null) {
			this.openPodium();
			host.podiumScene.startMusic(1);
			host.podiumScene.play();
			return;
		}

		// Nothing else happens while the screen changes
		host.championTransition = true;
		double fadeSeconds = 1.5;
		if (current instanceof BattleArena3D arena) {
			arena.fadeOutAudio(fadeSeconds);
		}
		GameWindow.fadeCurtain(true, fadeSeconds, () -> {
			// Black and silent: the podium is put in place, with its music on at zero
			this.openPodium();
			if (host.podiumScene == null) {
				GameWindow.fadeCurtain(false, 0.6, () -> host.championTransition = false);
				return;
			}
			host.podiumScene.startMusic(0);
			Timeline rise = new Timeline(new KeyFrame(Duration.seconds(2.2), new KeyValue(host.podiumScene.musicVolumeProperty(), 1, Interpolator.EASE_OUT)));
			rise.setOnFinished(event -> {
				// At the maximum, the screen fades in and the champion comes in
				GameWindow.fadeCurtain(false, 1.4, () -> host.championTransition = false);
				PauseTransition wait = new PauseTransition(Duration.seconds(0.5));
				wait.setOnFinished(done -> {
					if (host.podiumScene != null) {
						host.podiumScene.play();
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
	void openPodium() {
		host.resetAllScenes();
		try {
			List<Opponent> podium = new ArrayList<Opponent>();
			List<Long> ids = host.bracket.getPodium();
			for (Long identifier : ids) {
				podium.add(host.opponents.get(identifier));
			}
			host.podiumScene = new Podium(podium, MatchPhotos.photosOf(host.championship.getId(), ids), host.opponents);
			host.show(host.podiumScene);
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			host.podiumScene = null;
			host.showError("Falha ao exibir o campeão");
		}
	}

	/**
	 * @param item
	 */
	void showOpponent(Opponent item, String title, boolean champion) {
		// Show the opponent
		try {
			host.opponentScene = new ShowOponent();
			host.opponentScene.setOponent(item, title, champion);
			host.show(host.opponentScene);
			host.opponentScene.play();
		} catch (IOException | RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			host.showError("Falha ao exibir oponente");
		}
	}
}
