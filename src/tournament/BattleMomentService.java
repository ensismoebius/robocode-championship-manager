package tournament;

import java.util.Collection;
import java.util.List;

import model.BattleMoment;
import utils.database.Database;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Keeps and reads the photos of the key moments of the 3D battles, see
 * {@link BattleMoment}
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class BattleMomentService {

	/**
	 * Saves the photos of a match, the ones it had before are replaced
	 */
	public static void save(long championshipId, String matchCode, List<BattleMoment> moments) {
		Database.inTransaction(em -> {
			em.createQuery("delete from BattleMoment m where m.championshipId = :championship and m.matchCode = :match")
					.setParameter("championship", championshipId).setParameter("match", matchCode).executeUpdate();
			for (BattleMoment moment : moments) {
				em.persist(moment);
			}
			return null;
		});
	}

	/**
	 * Removes the photos of a match, when its result is undone
	 */
	public static void delete(long championshipId, String matchCode) {
		Database.inTransaction(em -> {
			em.createQuery("delete from BattleMoment m where m.championshipId = :championship and m.matchCode = :match")
					.setParameter("championship", championshipId).setParameter("match", matchCode).executeUpdate();
			return null;
		});
	}

	/**
	 * The photos of the battles of the robots, in the order they were taken
	 */
	public static List<BattleMoment> of(long championshipId, Collection<Long> robotIds) {
		if (robotIds.isEmpty()) {
			return List.of();
		}
		return Database.read(em -> em
				.createQuery("select m from BattleMoment m where m.championshipId = :championship and (m.firstId in :robots or m.secondId in :robots) order by m.id", BattleMoment.class)
				.setParameter("championship", championshipId).setParameter("robots", robotIds).getResultList());
	}
}
