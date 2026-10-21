package support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.BattleMoment;
import model.Opponent;
import tournament.Bracket;
import tournament.Bracket.Match;
import tournament.Bracket.Status;

/** Robots, brackets and battle moments for the screens, built the way the championship builds them. */
public final class ChampionshipFixture {

	private ChampionshipFixture() {
	}

	/** Robots with ids from 1, each with its team, name, battle cry and the logo of the program. */
	public static List<Opponent> opponents(int count) {
		List<Opponent> opponents = new ArrayList<Opponent>();
		for (int index = 1; index <= count; index++) {
			Opponent opponent = new Opponent();
			opponent.setId(index);
			opponent.setTeam("team" + index);
			opponent.setName("Robo" + index);
			opponent.setBattleCry("Grito " + index);
			opponent.setImage(logo());
			opponents.add(opponent);
		}
		return opponents;
	}

	/** The robots by id, as the screens take them. */
	public static Map<Long, Opponent> byId(List<Opponent> opponents) {
		Map<Long, Opponent> byId = new HashMap<Long, Opponent>();
		for (Opponent opponent : opponents) {
			byId.put(opponent.getId(), opponent);
		}
		return byId;
	}

	/** A bracket of the robots where every match is played, the first slot always wins. */
	public static Bracket finished(List<Opponent> opponents, boolean repechage) {
		List<Long> seeds = new ArrayList<Long>();
		for (Opponent opponent : opponents) {
			seeds.add(opponent.getId());
		}
		Bracket bracket = new Bracket(seeds, repechage);
		while (!bracket.isFinished()) {
			for (Match match : bracket.getMatches()) {
				if (match.getStatus() == Status.READY) {
					bracket.record(match.getCode(), match.getA().getParticipant(), 3, 1);
				}
			}
		}
		return bracket;
	}

	/** A bracket of the robots where nothing is played yet. */
	public static Bracket fresh(List<Opponent> opponents, boolean repechage) {
		List<Long> seeds = new ArrayList<Long>();
		for (Opponent opponent : opponents) {
			seeds.add(opponent.getId());
		}
		return new Bracket(seeds, repechage);
	}

	/** A moment of a battle between two robots, with a picture. */
	public static BattleMoment moment(String matchCode, long first, long second, int round, boolean destruction) {
		BattleMoment moment = new BattleMoment();
		moment.setChampionshipId(1);
		moment.setMatchCode(matchCode);
		moment.setFirstId(first);
		moment.setSecondId(second);
		moment.setRoundNumber(round);
		moment.setDestruction(destruction);
		moment.setSubjectId(first);
		moment.setPower(2);
		moment.setImage(logo());
		return moment;
	}

	/** The picture of the program, used as the logo of the robots. */
	public static byte[] logo() {
		try {
			return Files.readAllBytes(Path.of("src", "widgets", "scenes", "robocodeLogo.png"));
		} catch (IOException unreadable) {
			throw new IllegalStateException(unreadable);
		}
	}
}
