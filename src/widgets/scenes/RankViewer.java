package widgets.scenes;

import java.util.HashMap;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import model.Opponent;
import widgets.IRobotMarkedEvent;
import widgets.RobotBasicViewer;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RankViewer extends Scene implements IRobotMarkedEvent {

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

		this.linha = -1;
		this.currentOpponentLevel = 0;
		this.opponentsList.forEach(opponent -> {

			int opponentLevel = opponent.getLevels().get(opponent.getLevels().size() - 1);

			if (opponentLevel != currentOpponentLevel) {
				currentOpponentLevel = opponentLevel;
				linha = 0;
			} else {
				linha++;
			}

			RobotBasicViewer robotViewer = new RobotBasicViewer(opponent);
			robotViewer.setRobotMarkedEventListener(this);

			GridPane.setRowSpan(robotViewer, currentOpponentLevel + 1);
			this.gridPane.add(robotViewer, opponentLevel, linha * (1 + currentOpponentLevel));

		});

		ScrollPane sp = new ScrollPane(this.gridPane);
		sp.setPannable(true);
		sp.setStyle("-fx-background: #000;");
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
