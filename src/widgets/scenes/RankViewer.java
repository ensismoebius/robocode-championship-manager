package widgets.scenes;

import java.util.HashMap;

import javax.persistence.EntityManager;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import listeners.OpponentsUpdater;
import model.Opponent;
import utils.database.Database;
import widgets.RobotBasicViewer;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RankViewer extends Scene {

	private EntityManager entityManager;

	private HashMap<Long, Opponent> battleList;
	private ObservableList<Opponent> opponentsList;

	private GridPane gridPane;

	public RankViewer(HashMap<Long, model.Opponent> battleList, ObservableList<Opponent> opponentsList) {
		super(new GridPane());

		
		this.gridPane = new GridPane();
		this.gridPane.setVgap(30);

		this.battleList = battleList;
		this.opponentsList = opponentsList;
		this.loadData();
		RobotBasicViewer rbv = new RobotBasicViewer(opponentsList.get(0));
		RobotBasicViewer rbv1 = new RobotBasicViewer(opponentsList.get(1));

		this.gridPane.add(rbv, 0, 0);
		this.gridPane.add(rbv1, 0, 1);


		this.setRoot(new ScrollPane(this.gridPane));
	}

	@SuppressWarnings("unchecked")
	private void loadData() {
		this.entityManager = Database.getEntityManager();
		this.entityManager.createQuery("from Opponent order by level").getResultList()
				.forEach(opponent -> this.opponentsList.add((Opponent) opponent));
		this.opponentsList.addListener(new OpponentsUpdater(this.opponentsList));
		this.entityManager.close();
	}
}
