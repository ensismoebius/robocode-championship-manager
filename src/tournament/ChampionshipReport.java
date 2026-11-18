package tournament;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import model.Author;
import model.Championship;
import model.MatchResult;
import model.Opponent;
import utils.web.Html;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The report of a championship: a page to read or print, with the placements, the robots and every match played,
 * and the same matches as a table for a spreadsheet. The logins and passwords of the teams are never in it.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class ChampionshipReport {

	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	/** Only static methods: this class is not instantiated. */
	private ChampionshipReport() {
	}

	/** The report as a self contained HTML page. */
	public static String html(Championship championship) {
		Bracket bracket = ChampionshipService.bracketOf(championship);
		Map<Long, Opponent> robots = ChampionshipService.opponentsOf(championship);
		Map<Long, Integer> placements = bracket.getPlacements();

		StringBuilder page = new StringBuilder();
		page.append("<!DOCTYPE html>\n<html lang=\"pt-BR\"><head><meta charset=\"UTF-8\"><title>").append(Html.escape(championship.getName())).append("</title>\n");
		page.append("<style>body{font-family:sans-serif;max-width:60em;margin:2em auto;padding:0 1em;color:#222}")
				.append("table{border-collapse:collapse;width:100%;margin-bottom:2em}th,td{border:1px solid #bbb;padding:.35em .6em;text-align:left}")
				.append("th{background:#eee}.champion{font-size:1.4em;font-weight:bold}</style></head><body>\n");
		page.append("<h1>").append(Html.escape(championship.getName())).append("</h1>\n");
		page.append("<p>Sorteado em ").append(championship.getCreatedAt().format(WHEN)).append(" • ").append(robots.size()).append(" robôs • ")
				.append(championship.isRepechage() ? "dupla eliminação (com repescagem)" : "eliminação simples").append(" • ")
				.append(championship.getResults().size()).append(" partidas jogadas</p>\n");

		Long champion = bracket.getChampion();
		page.append(champion == null ? "<p>O campeonato ainda não terminou.</p>\n" : "<p class=\"champion\">Campeão: " + Html.escape(robots.get(champion).getFullName()) + "</p>\n");

		page.append("<h2>Robôs</h2>\n<table><tr><th>Sorteio</th><th>Colocação</th><th>Robô</th><th>Autores</th><th>Vitórias</th><th>Derrotas</th></tr>\n");
		int seed = 0;
		for (Opponent robot : championship.getSeeds()) {
			seed++;
			page.append("<tr><td>").append(seed).append("</td><td>").append(placementOf(robot, placements)).append("</td><td>").append(Html.escape(robot.getFullName())).append("</td><td>")
					.append(Html.escape(authorsOf(robot))).append("</td><td>").append(count(championship.getResults(), robot, true)).append("</td><td>").append(count(championship.getResults(), robot, false)).append("</td></tr>\n");
		}
		page.append("</table>\n");

		page.append("<h2>Partidas</h2>\n<table><tr><th>Partida</th><th>Fase</th><th>Vencedor</th><th>Perdedor</th><th>Placar</th><th>Quando</th></tr>\n");
		for (MatchResult result : championship.getResults()) {
			Match match = bracket.getMatch(result.getMatchCode());
			page.append("<tr><td>").append(Html.escape(result.getMatchCode())).append("</td><td>").append(match == null ? "" : Html.escape(bracket.roundName(match))).append("</td><td>")
					.append(Html.escape(result.getWinner().getFullName())).append("</td><td>").append(Html.escape(result.getLoser().getFullName())).append("</td><td>").append(score(result)).append("</td><td>")
					.append(result.getPlayedAt().format(WHEN)).append("</td></tr>\n");
		}
		page.append("</table>\n</body></html>\n");
		return page.toString();
	}

	/** The matches played, one per line, as comma separated values with a header. */
	public static String csv(Championship championship) {
		StringBuilder table = new StringBuilder("partida,vencedor,perdedor,pontos_vencedor,pontos_perdedor,quando\n");
		for (MatchResult result : championship.getResults()) {
			table.append(field(result.getMatchCode())).append(',').append(field(result.getWinner().getFullName())).append(',').append(field(result.getLoser().getFullName())).append(',')
					.append(result.getWinnerScore() == null ? "" : result.getWinnerScore()).append(',').append(result.getLoserScore() == null ? "" : result.getLoserScore()).append(',')
					.append(result.getPlayedAt()).append('\n');
		}
		return table.toString();
	}

	/** A value of a csv line: quoted when it has a comma, a quote or a line break. */
	static String field(String value) {
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return '"' + value.replace("\"", "\"\"") + '"';
		}
		return value;
	}

	/** The names and registrations of the authors of the robot. */
	static String authorsOf(Opponent robot) {
		List<Author> authors = robot.getAuthors();
		return authors.stream().map(author -> author.getRegistration() == null || author.getRegistration().isBlank() ? author.getName() : author.getName() + " (" + author.getRegistration() + ")")
				.reduce((first, second) -> first + ", " + second).orElse("");
	}

	/** The placement of the robot, a dash while it is still playing. */
	private static String placementOf(Opponent robot, Map<Long, Integer> placements) {
		Integer place = placements.get(robot.getId());
		return place == null ? "—" : place + "º";
	}

	/** How many matches of the results the robot won or lost. */
	private static long count(List<MatchResult> results, Opponent robot, boolean wins) {
		return results.stream().filter(result -> (wins ? result.getWinner() : result.getLoser()).getId() == robot.getId()).count();
	}

	/** The score of the match, or manual when the winner was declared by the organizer. */
	private static String score(MatchResult result) {
		return result.getWinnerScore() == null || result.getLoserScore() == null ? "manual" : result.getWinnerScore() + " x " + result.getLoserScore();
	}
}
