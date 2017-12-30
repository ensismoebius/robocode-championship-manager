package widgets.scenes;

import java.util.HashMap;

import javafx.collections.ObservableMap;
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
	private ObservableMap<String, Opponent> opponentsList;

	private GridPane gridPane;

	private int rankGridRowNumber;

	public RankViewer(HashMap<String, model.Opponent> battleList, ObservableMap<String, Opponent> opponentsList) {
		super(new GridPane());

		this.gridPane = new GridPane();
		this.gridPane.setVgap(30);

		this.battleList = battleList;
		this.opponentsList = opponentsList;

		this.rankGridRowNumber = 1;
		this.opponentsList.entrySet().forEach(opponent -> {

			for (int level : opponent.getValue().getLevels()) {

				RobotBasicViewer robotViewer = new RobotBasicViewer(opponent.getValue().getImage(),
						opponent.getValue().getName());
				robotViewer.setRobotMarkedEventListener(this);

				GridPane.setRowSpan(robotViewer, level + 1);
				this.gridPane.add(robotViewer, level + 1, this.rankGridRowNumber);
				this.rankGridRowNumber++;
			}
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
	public void robotMarked(boolean marked, String robotName) {
		if (marked) {
			this.battleList.put(robotName, this.opponentsList.get(robotName));
			return;
		}
		this.battleList.remove(robotName);
	}
}
