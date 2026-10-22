package widgets.scenes;

import tournament.Importance;

import javafx.scene.layout.Priority;
import widgets.Sfx;
import widgets.Theme;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tournament.Bracket;

/**
 * The header of the bracket: its title, the champion or the next match, and the notice of robots that are out
 */
final class BracketHeader {

	private final BracketViewer host;

	BracketHeader(BracketViewer host) {
		this.host = host;
	}

	/** The title of the bracket with the robots that are not in it. */
	Node build(String title, long robotsOutside) {
		VBox header = new VBox(8);

		Label titleLabel = new Label(title);
		titleLabel.getStyleClass().add("title");
		header.getChildren().add(titleLabel);

		String format = host.bracket.hasRepechage() ? "Dupla eliminação com repescagem" : "Eliminação simples";
		Label subtitle = new Label(host.bracket.getParticipants().size() + " robôs • " + format);
		subtitle.getStyleClass().add("subtitle");
		header.getChildren().add(subtitle);

		Long champion = host.bracket.getChampion();
		if (champion != null) {
			header.getChildren().add(this.championBanner(champion));
		} else {
			header.getChildren().add(this.nextMatchLabel());
		}

		if (robotsOutside > 0) {
			Label warning = new Label(robotsOutside + " robô(s) cadastrado(s) depois do sorteio não estão no campeonato. Ctrl + G sorteia um novo campeonato.");
			warning.getStyleClass().add("warning");
			header.getChildren().add(warning);
		}

		Label help = new Label(
				"Clique em uma partida para selecioná-la • B: tela versus • S: inicia a batalha • 1/2: declara o vencedor • Ctrl+Z: desfaz o último resultado • Ctrl+roda ou +/-: zoom • arraste, roda ou setas: move • 0: visão geral");
		help.getStyleClass().add("help");
		header.getChildren().add(help);

		return header;
	}

	/** The banner that says who won the championship, with the logo of the champion */
	private HBox championBanner(Long champion) {
		HBox banner = new HBox(16);
		banner.setAlignment(Pos.CENTER_LEFT);
		banner.getStyleClass().add("champion-banner");
		ImageView logo = host.logo(champion, 96, 72);
		if (logo != null) {
			banner.getChildren().add(logo);
		}
		Label championLabel = new Label("CAMPEÃO: " + host.nameOf(champion).toUpperCase());
		banner.getChildren().add(championLabel);
		Theme.pulseGlow(banner, Theme.ACCENT);
		banner.setMaxWidth(Region.USE_PREF_SIZE);
		return banner;
	}

	/** The next match, with its stage marked by a star when it is a decisive one */
	private Label nextMatchLabel() {
		Label next = new Label(host.bracket.getNextMatch().map(matrix -> {
			Importance importance = host.bracket.importance(matrix);
			String stage = importance == Importance.REGULAR ? "" : "★ " + host.bracket.roundName(matrix).toUpperCase() + " ★  ";
			return stage + "Próxima partida: " + matrix.getCode() + " — " + host.nameOf(matrix.getA().getParticipant()) + " × " + host.nameOf(matrix.getB().getParticipant());
		}).orElse(""));
		next.getStyleClass().add("subtitle");
		if (host.bracket.getNextMatch().map(matrix2 -> host.bracket.importance(matrix2) != Importance.REGULAR).orElse(false)) {
			next.setStyle("-fx-text-fill: #ffd54a;");
		}
		return next;
	}
}
