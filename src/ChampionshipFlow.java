import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import jakarta.persistence.PersistenceException;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.Championship;
import model.Opponent;
import tournament.Match;
import tournament.Status;
import tournament.ChampionshipService;
import widgets.scenes.BracketViewer;
import widgets.GameDialog;

/**
 * The flow of the championship in the main window: loading and creating it, the bracket, the results and the undo
 */
final class ChampionshipFlow {

	private final Main host;

	/** Creates the flow of the championship for the given main window. */
	ChampionshipFlow(Main host) {
		this.host = host;
	}

	/**
	 * Loads the current championship from the database and keeps the selected
	 * match if it still can be played
	 */
	void loadChampionship() {
		host.championship = ChampionshipService.current();
		this.setChampionship(host.championship);
	}

	/**
	 * Reads the championship again, so the robots of the bracket show the texts that were saved. Does nothing
	 * when there is no championship.
	 */
	void reloadOpponents() {
		if (host.championship != null) {
			this.setChampionship(ChampionshipService.current());
		}
	}

	/** Shows the given championship and its bracket, or nothing when it is null. */
	void setChampionship(Championship championship) {
		host.championship = championship;
		if (championship == null) {
			host.bracket = null;
			host.opponents = Map.of();
			host.selectedMatch = null;
			return;
		}

		host.bracket = ChampionshipService.bracketOf(championship);
		host.opponents = ChampionshipService.opponentsOf(championship);

		Match selected = host.selectedMatch == null ? null : host.bracket.getMatch(host.selectedMatch);
		if (selected == null || selected.getStatus() != Status.READY) {
			host.selectedMatch = host.bracket.getNextMatch().map(Match::getCode).orElse(null);
		}
	}

	/** Draws a new championship from the registered robots. */
	void newChampionship() {
		long robots;
		try {
			robots = ChampionshipService.countOpponents();
		} catch (PersistenceException persistenceException) {
			host.showError("Falha ao ler os robôs:\n" + Main.rootCauseMessage(persistenceException));
			return;
		}

		if (robots < 2) {
			host.showError("São necessários ao menos dois robôs cadastrados para sortear um campeonato.");
			return;
		}

		CheckBox repechage = new CheckBox("Repescagem (dupla eliminação): quem perde uma vez ainda pode ser campeão");
		repechage.setWrapText(true);
		repechage.setSelected(true);

		String warning = host.championship != null && !host.championship.getResults().isEmpty() ? "\n\nATENÇÃO: o campeonato atual já tem resultados e será substituído." : "";
		Label message = new Label("Sortear um novo campeonato com os " + robots + " robôs cadastrados?" + warning);
		message.setWrapText(true);
		message.getStyleClass().add("game-dialog-message");

		if (!GameDialog.confirm("Novo campeonato", new VBox(14, message, repechage))) {
			return;
		}

		try {
			host.selectedMatch = null;
			host.justPlayed = null;
			this.setChampionship(ChampionshipService.create(repechage.isSelected(), new SecureRandom()));
			this.showBracket();
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			host.showError("Falha ao criar o campeonato:\n" + Main.rootCauseMessage(runtimeException));
		}
	}

	/** Shows the bracket of the championship, creating it when needed. */
	void showBracket() {
		try {
			this.loadChampionship();
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			host.showError("Falha ao carregar o campeonato:\n" + Main.rootCauseMessage(runtimeException));
			return;
		}

		if (host.bracket == null) {
			host.showError("Nenhum campeonato criado ainda. Pressione Ctrl + G para sortear um.");
			return;
		}

		host.resetAllScenes(true);

		long outside = Math.max(0, ChampionshipService.countOpponents() - host.opponents.size());
		host.bracketScene = new BracketViewer(host.championship.getName(), host.bracket, host.opponents, host.selectedMatch, code -> {
			host.selectedMatch = code;
			host.opponentIndexIWantToShow = 0;
		}, outside);
		if (host.justPlayed != null) {
			host.bracketScene.playResult(host.justPlayed);
			host.justPlayed = null;
		}

		host.show(host.bracketScene);
		host.playMenuMusic(Main.BRACKET_MUSIC_VOLUME);
	}

	/**
	 * @return the selected match if it can be played, otherwise shows an error
	 */
	Optional<Match> playableSelectedMatch() {
		if (host.bracket == null) {
			host.showError("Nenhum campeonato criado ainda. Pressione Ctrl + G para sortear um.");
			return Optional.empty();
		}
		if (host.bracket.isFinished()) {
			host.showChampion(false);
			return Optional.empty();
		}
		Match match = host.selectedMatch == null ? null : host.bracket.getMatch(host.selectedMatch);
		if (match == null || match.getStatus() != Status.READY) {
			host.showError("Nenhuma partida selecionada. Pressione R e clique em uma partida pronta.");
			return Optional.empty();
		}
		return Optional.of(match);
	}

	/**
	 * Saves the result and shows the winner
	 */
	void recordResult(Match match, long winnerId, Integer winnerScore, Integer loserScore) {
		if (this.saveResult(match, winnerId, winnerScore, loserScore)) {
			this.showResult(winnerId);
		}
	}

	/**
	 * @return false if it could not be saved (the error was shown)
	 */
	boolean saveResult(Match match, long winnerId, Integer winnerScore, Integer loserScore) {
		try {
			this.setChampionship(ChampionshipService.recordResult(host.championship, match.getCode(), winnerId, winnerScore, loserScore));
			host.justPlayed = match.getCode();
			return true;
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			host.showError("Falha ao salvar o resultado:\n" + Main.rootCauseMessage(runtimeException));
			return false;
		}
	}

	/**
	 * The winner of the match, or the champion
	 */
	void showResult(long winnerId) {
		if (host.bracket.isFinished()) {
			host.showChampion(true);
		} else {
			host.resetAllScenes();
			host.showOpponent(host.opponents.get(winnerId), "VENCEU!", false);
		}
	}

	/** Declares the winner of the selected match, the first or the second opponent. */
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

	/** Undoes the last result of the championship. */
	void undoLastResult() {
		if (host.championship == null || host.championship.getResults().isEmpty()) {
			host.showError("Não há resultados para desfazer.");
			return;
		}

		var last = host.championship.getResults().get(host.championship.getResults().size() - 1);
		if (!GameDialog.confirm("Desfazer resultado",
				"Desfazer o resultado da partida " + last.getMatchCode() + " (" + this.nameOf(last.getWinner().getId()) + " venceu " + this.nameOf(last.getLoser().getId()) + ")?")) {
			return;
		}

		try {
			host.selectedMatch = last.getMatchCode();
			host.justPlayed = null;
			this.setChampionship(ChampionshipService.undoLastResult(host.championship));
			MatchPhotos.forget(host.championship.getId(), last.getMatchCode());
			this.showBracket();
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			host.showError("Falha ao desfazer o resultado:\n" + Main.rootCauseMessage(runtimeException));
		}
	}

	/** The name of an opponent for the messages, or a question mark when it is unknown. */
	String nameOf(Long identifier) {
		Opponent opponent = host.opponents.get(identifier);
		return opponent == null ? "?" : opponent.getFullName();
	}
}
