package widgets.scenes;

import java.util.HashMap;

import javax.persistence.EntityManager;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
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

	private int currentOpponentLevel;

	public RankViewer(HashMap<String, model.Opponent> battleList, ObservableList<Opponent> opponentsList) {
		super(new GridPane());

		this.gridPane = new GridPane();
		this.gridPane.setVgap(30);

		this.battleList = battleList;
		this.opponentsList = opponentsList;
		this.loadData();

		this.linha = -1;
		this.currentOpponentLevel = 0;
		this.opponentsList.forEach(opponent -> {

			if (opponent.getLevel() != currentOpponentLevel) {
				currentOpponentLevel = opponent.getLevel();
				linha = 0;
			} else {
				linha++;
			}

			RobotBasicViewer robotViewer = new RobotBasicViewer(opponent);
			robotViewer.setRobotMarkedEventListener(this);

			GridPane.setRowSpan(robotViewer, currentOpponentLevel + 1);
			this.gridPane.add(robotViewer, opponent.getLevel(), linha * (1 + currentOpponentLevel));

		});

		ScrollPane sp = new ScrollPane(this.gridPane);
		sp.setPannable(true);  
		sp.setStyle("-fx-background: #010;");
		// Adding zoom!!!!
		sp.addEventFilter(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {

				double zoomFactor = 1.05;
				double deltaY = event.getDeltaY();
				if (deltaY < 0) {
					zoomFactor = 2.0 - zoomFactor;
				}
				RankViewer.this.gridPane.setScaleX(RankViewer.this.gridPane.getScaleX() * zoomFactor);
				RankViewer.this.gridPane.setScaleY(RankViewer.this.gridPane.getScaleY() * zoomFactor);

				event.consume();
			}
		});
		
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
