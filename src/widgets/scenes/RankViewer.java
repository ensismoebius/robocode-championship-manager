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
import widgets.IRobotMarkedEvent;
import widgets.RobotBasicViewer;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RankViewer extends Scene implements IRobotMarkedEvent {

	private EntityManager entityManager;

	private HashMap<String, Opponent> battleList;
	private ObservableList<Opponent> opponentsList;

	private GridPane gridPane;

	private int linha;

	public RankViewer(HashMap<String, model.Opponent> battleList, ObservableList<Opponent> opponentsList) {
		super(new GridPane());

		this.gridPane = new GridPane();
		this.gridPane.setVgap(30);

		this.battleList = battleList;
		this.opponentsList = opponentsList;
		this.loadData();

		this.linha = 0;
		this.opponentsList.forEach(opponent -> {

			RobotBasicViewer robotViewer = new RobotBasicViewer(opponent);
			robotViewer.setRobotMarkedEventListener(this);

			GridPane.setRowSpan(robotViewer, opponent.getLevel() + 1);
			this.gridPane.add(robotViewer, opponent.getLevel(), linha - (opponent.getLevel() * 2));
			linha++;
		});

		ScrollPane sp = new ScrollPane(this.gridPane);
		sp.setStyle("-fx-background: #010;");
		this.setRoot(sp);
	}

	@SuppressWarnings("unchecked")
	private void loadData() {
		this.entityManager = Database.getEntityManager();
		this.entityManager.createQuery("from Opponent order by level").getResultList()
				.forEach(opponent -> this.opponentsList.add((Opponent) opponent));
		this.opponentsList.addListener(new OpponentsUpdater(this.opponentsList));
		this.entityManager.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see widgets.IMarkedEvent#marked(boolean, model.Opponent)
	 */
	@Override
	public void robotMarked(boolean marked, Opponent robot) {
		if (marked) {
			this.battleList.put(robot.getName(), robot);
			return;
		}
		this.battleList.remove(robot.getName());
	}
}
