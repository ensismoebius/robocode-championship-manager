import java.util.ArrayList;
import java.util.List;

import model.Opponent;
import model.BattleMoment;
import tournament.BattleMomentService;
import tournament.Bracket.Match;
import widgets.scenes.BattleArena3D;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The photos of the key moments of the 3D battles, kept with their matches.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class MatchPhotos {

	private MatchPhotos() {
	}

	/**
	 * Keeps the photos of the key moments of a 3D battle with its match. They are
	 * a bonus: when they fail, the battle still counts
	 */
	static void keep(long championshipId, BattleArena3D arena, Match match, List<Opponent> fighters) {
		List<BattleMoment> moments = new ArrayList<BattleMoment>();
		for (BattleArena3D.BattlePhoto photo : arena.photos()) {
			if (photo.subject() == null) {
				continue;
			}
			BattleMoment moment = new BattleMoment();
			moment.setChampionshipId(championshipId);
			moment.setMatchCode(match.getCode());
			moment.setRoundNumber(photo.round());
			moment.setDestruction(photo.destruction());
			moment.setSubjectId(photo.subject().getId());
			moment.setPower(photo.power());
			moment.setFirstId(fighters.get(0).getId());
			moment.setSecondId(fighters.get(1).getId());
			moment.setImage(photo.jpeg());
			moments.add(moment);
		}
		try {
			BattleMomentService.save(championshipId, match.getCode(), moments);
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
		}
	}

	/**
	 * Removes the photos of a match whose result was undone
	 */
	static void forget(long championshipId, String matchCode) {
		try {
			BattleMomentService.delete(championshipId, matchCode);
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
		}
	}

	/**
	 * The photos of the battles of the robots, when they can be read
	 */
	static List<BattleMoment> photosOf(long championshipId, List<Long> robots) {
		try {
			return BattleMomentService.photosOf(championshipId, robots);
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			return List.of();
		}
	}
}
