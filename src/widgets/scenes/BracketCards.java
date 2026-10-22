package widgets.scenes;

import javafx.scene.layout.Priority;
import widgets.Sfx;
import widgets.Theme;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.Opponent;
import tournament.Match;
import tournament.Slot;
import tournament.Status;
import static widgets.scenes.BracketViewer.CARD_W;
import static widgets.scenes.BracketViewer.ROW_H;
import static widgets.scenes.BracketViewer.HEADER_H;
import static widgets.scenes.BracketViewer.CARD_H;

/**
 * The cards of the matches: the two slots of each match, its status and its code
 */
final class BracketCards {

	private final BracketViewer host;

	/** Creates the cards of the given bracket viewer. */
	BracketCards(BracketViewer host) {
		this.host = host;
	}

	/** A card with the two slots of a match, its status and its code. */
	VBox buildCard(Match match) {
		VBox card = new VBox();
		card.setPrefSize(CARD_W, CARD_H);
		card.setMinSize(CARD_W, CARD_H);
		card.setMaxSize(CARD_W, CARD_H);
		card.getStyleClass().add("match-card");

		switch (match.getStatus()) {
		case READY -> card.getStyleClass().add("ready");
		case BYE -> card.getStyleClass().add(match.getWinner() == null ? "empty" : "bye");
		default -> {
		}
		}

		HBox header = this.statusHeader(match);

		HBox rowA = this.buildSlotRow(match, match.getA());
		HBox rowB = this.buildSlotRow(match, match.getB());
		host.rows.put(match.getCode(), new HBox[] { rowA, rowB });
		card.getChildren().addAll(header, rowA, rowB);

		if (match.getStatus() == Status.READY) {
			this.selectOnClick(card, match);
		}

		host.cards.put(match.getCode(), card);
		if (match.getCode().equals(host.selected)) {
			host.styleSelected(card, true);
		}
		return card;
	}

	/** The header of a card: its code on the left and its status on the right */
	private HBox statusHeader(Match match) {
		Label code = new Label(match.getCode());
		code.getStyleClass().add("match-code");
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		Label status = new Label(this.statusText(match));
		status.getStyleClass().add("match-status");
		HBox header = new HBox(code, spacer, status);
		header.setAlignment(Pos.CENTER_LEFT);
		header.setMinHeight(HEADER_H);
		header.getStyleClass().add("match-header");
		return header;
	}

	/** A click on a card that can be played selects its match, and tells the owner of the viewer */
	private void selectOnClick(VBox card, Match match) {
		card.setOnMouseClicked(event -> {
			// The end of a drag is not a click
			if (!event.isStillSincePress()) {
				return;
			}
			Sfx.blip();
			host.select(match.getCode());
			host.onSelect.accept(match.getCode());
		});
	}

	/** The status of a match, as the word shown in its card. */
	String statusText(Match match) {
		boolean isNext = host.bracket.getNextMatch().map(matrix -> matrix == match).orElse(false);
		return switch (match.getStatus()) {
		case WAITING -> "aguardando";
		case READY -> match.getCode().equals(host.selected) ? "▶ SELECIONADA" : isNext ? "próxima" : "pronta";
		case FINISHED -> "encerrada";
		case BYE -> "folga";
		};
	}

	/** A row of a slot, with the logo and the name of the robot in it. */
	private HBox buildSlotRow(Match match, Slot slot) {
		HBox row = new HBox();
		row.getStyleClass().add("slot-row");
		row.setAlignment(Pos.CENTER_LEFT);
		row.setMinHeight(ROW_H);
		row.setMaxHeight(ROW_H);

		Long participant = slot.getParticipant();
		if (participant == null) {
			Label placeholder = new Label(slot.isResolved() ? "— folga —" : slot.describeSource());
			placeholder.getStyleClass().add("placeholder");
			row.getChildren().add(placeholder);
			return row;
		}

		boolean decided = match.getStatus() == Status.FINISHED;
		if (decided) {
			row.getStyleClass().add(participant.equals(match.getWinner()) ? "winner" : "loser");
		}

		row.getChildren().addAll(this.logoBox(participant), this.nameBlock(host.opponents.get(participant)));
		host.rowParticipants.put(row, participant);

		if (decided) {
			boolean won = participant.equals(match.getWinner());
			Integer score = won ? match.getWinnerScore() : match.getLoserScore();
			Label scoreLabel = new Label(score != null ? String.valueOf(score) : won ? "✔" : "");
			scoreLabel.getStyleClass().add("score");
			row.getChildren().add(scoreLabel);
			host.scores.put(row, scoreLabel);
		}
		return row;
	}

	/** The box with the logo of a robot, empty when it has none */
	private StackPane logoBox(Long participant) {
		StackPane logoBox = new StackPane();
		logoBox.getStyleClass().add("logo-box");
		logoBox.setMinSize(56, 42);
		logoBox.setMaxSize(56, 42);
		ImageView logo = host.logo(participant, 56, 42);
		if (logo != null) {
			logoBox.getChildren().add(logo);
		}
		return logoBox;
	}

	/** The name and the team of a robot, shown as "?" and nothing when the opponent is unknown */
	private VBox nameBlock(Opponent opponent) {
		Label name = new Label(opponent == null ? "?" : opponent.getName());
		name.getStyleClass().add("robot-name");
		Label team = new Label(opponent == null ? "" : opponent.getTeam());
		team.getStyleClass().add("team-name");
		VBox names = new VBox(name, team);
		names.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(names, Priority.ALWAYS);
		names.setMaxWidth(Double.MAX_VALUE);
		return names;
	}
}
