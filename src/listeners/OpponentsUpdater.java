package listeners;

import java.util.List;

import javax.persistence.EntityManager;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import model.Opponent;
import utils.database.Database;

public class OpponentsUpdater implements ListChangeListener<Opponent> {

	private ObservableList<Opponent> list;

	public OpponentsUpdater(ObservableList<Opponent> list) {
		this.list = list;
	}

	@Override
	public void onChanged(javafx.collections.ListChangeListener.Change<? extends Opponent> c) {

		while (c.next()) {

			// Removes entities
			if (c.getRemoved().size() > 0) {
				this.purgeEntities(c.getRemoved());
				return;
			}

			// Updates entities
			if (c.wasUpdated()) {
				this.updateOpponent(list.get(c.getTo() - 1));
				return;
			}

			// Insert new entities
			if (c.getAddedSubList().size() > 0) {
				this.insertOpponents(c.getAddedSubList());
				return;
			}
		}

	}

	private void purgeEntities(List<? extends Opponent> appointmentsList) {
		EntityManager em = Database.getEntityManager();
		em.getTransaction().begin();

		// Removes entities
		for (Opponent a : appointmentsList) {
			em.remove(a);
		}

		em.getTransaction().commit();
		em.clear();
		em.close();
	}

	private void updateOpponent(Opponent a) {
		EntityManager em = Database.getEntityManager();
		em.getTransaction().begin();
		em.merge(a);
		em.getTransaction().commit();
		em.clear();
		em.close();
	}

	private void insertOpponents(List<? extends Opponent> opponentList) {

		EntityManager em = Database.getEntityManager();
		em.getTransaction().begin();

		for (Opponent a : opponentList) {
			if (a.getId() == 0) em.persist(a);
		}

		em.getTransaction().commit();
		em.clear();
		em.close();
	}

}
