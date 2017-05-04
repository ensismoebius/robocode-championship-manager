package listeners;

import javax.persistence.EntityManager;

import javafx.collections.MapChangeListener;
import model.Opponent;
import utils.database.Database;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class OpponentsUpdater implements MapChangeListener<String, Opponent> {

	@Override
	public void onChanged(javafx.collections.MapChangeListener.Change<? extends String, ? extends Opponent> c) {

		// Removes entities
		if (c.wasRemoved()) {
			this.purgeEntities(c.getValueRemoved());
			return;
		}

		// Insert new entities
		if (c.wasAdded()) {
			this.insertOpponents(c.getValueAdded());
			return;
		}

	}

	private void purgeEntities(Opponent apponent) {
		EntityManager em = Database.getEntityManager();
		em.getTransaction().begin();

		// Removes entities
		em.remove(apponent);

		em.getTransaction().commit();
		em.clear();
		em.close();
	}

	private void insertOpponents(Opponent opponent) {

		EntityManager em = Database.getEntityManager();
		em.getTransaction().begin();

		em.persist(opponent);

		em.getTransaction().commit();
		em.clear();
		em.close();
	}

}
