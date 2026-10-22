package widgets.scenes;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polyline;
import tournament.Match;
import tournament.Slot;
import tournament.Status;

import static widgets.scenes.BracketViewer.CARD_H;
import static widgets.scenes.BracketViewer.CARD_W;
import static widgets.scenes.BracketViewer.COL_GAP;
import static widgets.scenes.BracketViewer.COL_W;
import static widgets.scenes.BracketViewer.ROUND_TITLE_H;
import static widgets.scenes.BracketViewer.SECTION_GAP;
import static widgets.scenes.BracketViewer.SECTION_TITLE_H;
import static widgets.scenes.BracketViewer.SLOT_H;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Places the matches of the {@link BracketViewer} on its canvas: the rounds side
 * by side, the titles of the sections and rounds and the lines that join them.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class BracketLayout {

	private final BracketViewer host;

	/** Creates the layout of the bracket of the host viewer. */
	BracketLayout(BracketViewer host) {
		this.host = host;
	}

	/** Places every match, the titles of the rounds and the standings on the canvas of the bracket. */
	void build() {
		double top = 0;
		List<List<Match>> winners = host.bracket.getWinnersRounds();
		List<List<Match>> losers = host.bracket.getLosersRounds();

		this.addSectionTitle(host.bracket.hasRepechage() ? "Chave principal" : "Chave", top);
		top += SECTION_TITLE_H;
		double winnersHeight = this.layoutRounds(winners, top, host.bracket::winnersRoundName);
		top += ROUND_TITLE_H + winnersHeight;

		double finalsY = host.positions.get(winners.get(winners.size() - 1).get(0))[1];

		if (!losers.isEmpty()) {
			top += SECTION_GAP;
			this.addSectionTitle("Repescagem", top);
			top += SECTION_TITLE_H;
			double losersHeight = this.layoutRounds(losers, top, host.bracket::losersRoundName);

			double losersFinalY = host.positions.get(losers.get(losers.size() - 1).get(0))[1];
			finalsY = (finalsY + losersFinalY) / 2;
			top += ROUND_TITLE_H + losersHeight;
		}

		int columns = Math.max(winners.size(), losers.size());
		this.placeFinals(columns, finalsY);

		// Standings in the free space under the finals
		Node standings = host.buildStandings();
		if (standings != null) {
			standings.relocate(columns * COL_W, finalsY + CARD_H / 2 + 50);
			host.canvas.getChildren().add(standings);
		}

		this.placeCards();

		host.canvas.setMinHeight(top);
	}

	/** The finals, to the right of everything, each one with its round title */
	private void placeFinals(int columns, double finalsY) {
		List<Match> finals = host.bracket.getFinals();
		for (int index = 0; index < finals.size(); index++) {
			double posX = (columns + index) * COL_W;
			this.addRoundTitle(host.bracket.roundName(finals.get(index)), posX, finalsY - CARD_H / 2 - ROUND_TITLE_H);
			host.positions.put(finals.get(index), new double[] { posX, finalsY });
		}
	}

	/** The connectors, then the cards over them */
	private void placeCards() {
		// Lines first so the cards are drawn over them
		for (Match match : host.bracket.getMatches()) {
			this.addConnectors(match);
		}
		for (Match match : host.bracket.getMatches()) {
			double[] position = host.positions.get(match);
			VBox card = host.buildCard(match);
			card.relocate(position[0], position[1] - CARD_H / 2);
			host.canvas.getChildren().add(card);
		}
	}

	/**
	 * Places the rounds side by side, each match centered between the two matches
	 * that feed it
	 *
	 * @return the height used
	 */
	private double layoutRounds(List<List<Match>> rounds, double top, java.util.function.IntFunction<String> roundName) {
		double height = rounds.get(0).size() * SLOT_H;
		for (int row = 0; row < rounds.size(); row++) {
			List<Match> round = rounds.get(row);
			double posX = row * COL_W;
			this.addRoundTitle(roundName.apply(row + 1), posX, top);
			double spacing = height / round.size();
			for (int index = 0; index < round.size(); index++) {
				host.positions.put(round.get(index), new double[] { posX, top + ROUND_TITLE_H + spacing * (index + 0.5) });
			}
		}
		return height;
	}

	/** Adds the title of a section, such as the main bracket or the repechage. */
	private void addSectionTitle(String text, double posY) {
		Label label = new Label(text);
		label.getStyleClass().add("section-title");
		label.relocate(0, posY);
		host.canvas.getChildren().add(label);
	}

	/** Adds the title of a round, at the given place. */
	private void addRoundTitle(String text, double posX, double posY) {
		Label label = new Label(text);
		label.getStyleClass().add("round-title");
		label.relocate(posX + 4, posY);
		host.canvas.getChildren().add(label);
	}

	/**
	 * Elbow lines from the matches whose winners come to this one. Losers coming
	 * from the main bracket are not drawn, their slot tells where they come from
	 */
	private void addConnectors(Match match) {
		double[] toValue = host.positions.get(match);
		for (Slot slot : List.of(match.getA(), match.getB())) {
			Match source = slot.getSource();
			if (source == null || !slot.isSourceWinner()) {
				continue;
			}
			double[] from = host.positions.get(source);
			double x1Value = from[0] + CARD_W;
			double y1Value = from[1];
			double x2Value = toValue[0];
			double y2Value = toValue[1];
			double middle = x2Value - COL_GAP / 2;

			Polyline line = new Polyline(x1Value, y1Value, middle, y1Value, middle, y2Value, x2Value, y2Value);
			line.getStyleClass().add("connector");
			host.lines.put(source.getCode() + ">" + match.getCode(), line);
			if (source.getStatus() == Status.FINISHED || (source.getStatus() == Status.BYE && source.getWinner() != null)) {
				line.getStyleClass().add("done");
			}
			host.canvas.getChildren().add(line);
		}
	}
}
