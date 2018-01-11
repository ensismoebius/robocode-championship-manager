package widgets.scenes;

import java.util.HashMap;

import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import model.Opponent;
import model.OpponentGroup;
import widgets.IRobotMarkedEvent;
import widgets.RobotBasicViewer;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RankViewer extends Scene implements IRobotMarkedEvent {

	/**
	 * Holds the opponents must clash
	 */
	private HashMap<String, Opponent> battleList;
	private ObservableMap<String, OpponentGroup> groupsList;

	private VBox vbox;

	public RankViewer(HashMap<String, Opponent> battleList, ObservableMap<String, OpponentGroup> groupsList) {
		super(new VBox());

		this.vbox = new VBox(20);

		this.battleList = battleList;
		this.groupsList = groupsList;

		// Create a rank for each group
		this.groupsList.entrySet().forEach(group -> {

			int[] rankGridRowNumber = new int[50];

			// Holds the rank of the group
			GridPane groupRank = new GridPane();
			groupRank.setVgap(10);
			groupRank.setHgap(10);

			// Sets the title of group
			Label groupTitle = new Label(group.getValue().getName());
			this.vbox.getChildren().add(groupTitle);

			// Iterates over group members
			group.getValue().getOpponents().forEach(opponent -> {

				for (int level : opponent.getLevels()) {

					RobotBasicViewer robotViewer = new RobotBasicViewer(opponent.getImage(), group.getValue().getName(), opponent.getName());
					robotViewer.setRobotMarkedEventListener(this);

					groupRank.add(robotViewer, level + 1, rankGridRowNumber[level]);
					rankGridRowNumber[level]++;
				}
			});

			this.vbox.getChildren().add(groupRank);
		});

		ScrollPane sp = new ScrollPane(this.vbox);
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
				RankViewer.this.vbox.setScaleX(RankViewer.this.vbox.getScaleX() * zoomFactor);
				RankViewer.this.vbox.setScaleY(RankViewer.this.vbox.getScaleY() * zoomFactor);

				event.consume();
			}
		});

		this.setRoot(sp);
	}

	@Override
	public void robotMarked(boolean marked, String groupName, String robotName) {
		this.groupsList.get(groupName).getOpponents().forEach(robot -> {
			if (robot.getName().equals(robotName)) {
				if (marked) {
					this.battleList.put(robotName, robot);
					return;
				}
				this.battleList.remove(robotName);
				return;
			}
		});
	}
}
