package widgets.scenes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import listeners.OpponentsUpdater;
import model.Opponent;
import utils.database.Database;
import widgets.Oponent;
/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RankViewer extends Scene {

	private Canvas oponentsPane;
	private AnchorPane checkBoxesPane;
	private StackPane graphicsAndCheckboxesPane;

	private EntityManager entityManager;

	private HashMap<Long, Opponent> battleList;
	private ObservableList<Opponent> opponentsList;

	public RankViewer(HashMap<Long, model.Opponent> battleList, ObservableList<Opponent> opponentsList) {
		super(new ScrollPane());

		this.battleList = battleList;
		this.opponentsList = opponentsList;

		this.checkBoxesPane = new AnchorPane();
		this.oponentsPane = new Canvas(2000, 3000);
		this.graphicsAndCheckboxesPane = new StackPane();
		this.oponentsPane.getGraphicsContext2D().clearRect(0, 0, this.oponentsPane.getWidth(),
				this.oponentsPane.getHeight());

		this.loadData();
		this.createAllLevels(oponentsPane, this.opponentsList);

		this.graphicsAndCheckboxesPane.getChildren().clear();
		this.graphicsAndCheckboxesPane.getChildren().addAll(this.oponentsPane, this.checkBoxesPane);

		this.setRoot(new ScrollPane(this.graphicsAndCheckboxesPane));
	}

	@SuppressWarnings("unchecked")
	private void loadData() {
		this.entityManager = Database.getEntityManager();
		this.entityManager.createQuery("from Opponent order by level").getResultList()
				.forEach(opponent -> this.opponentsList.add((Opponent) opponent));
		this.opponentsList.addListener(new OpponentsUpdater(this.opponentsList));
		this.entityManager.close();
	}

	/**
	 * Create all rank levels
	 * 
	 * @param root
	 * @param oponents
	 */
	private void createAllLevels(Canvas root, ObservableList<model.Opponent> oponents) {

		int currentLevelIndex = 0;

		int i = 0;

		List<List<model.Opponent>> levels = new ArrayList<List<model.Opponent>>();

		List<model.Opponent> currentLevel = new ArrayList<model.Opponent>();

		for (i = 0; i < oponents.size(); i++) {

			if (currentLevelIndex != oponents.get(i).getLevel()) {
				levels.add(currentLevel);

				currentLevelIndex = oponents.get(i).getLevel();

				currentLevel = new ArrayList<model.Opponent>();
				currentLevel.add(oponents.get(i));
			} else {
				currentLevel.add(oponents.get(i));
			}
		}

		if (currentLevel.size() > 0)
			levels.add(currentLevel);

		for (List<model.Opponent> level : levels) {
			if (levels.size() - 1 == level.get(0).getLevel()) {
				this.createLevel(root, level, level.get(0).getLevel(), true);
				continue;
			}
			this.createLevel(root, level, level.get(0).getLevel(), false);
		}

	}

	/**
	 * Create a single level
	 * 
	 * @param root
	 * @param oponents
	 * @param level
	 * @param last
	 */
	private void createLevel(Canvas root, List<model.Opponent> oponents, int level, boolean last) {
		boolean top = true;
		GraphicsContext gc = root.getGraphicsContext2D();
		WritableImage img = new Oponent(oponents.get(0), top, 0, false).snapshot(null, null);

		int initialYOffset = 0;
		int initialXOffset = level * (int) img.getWidth();

		for (model.Opponent oponent : oponents) {

			Oponent oponentWidget = new Oponent(oponent, top, level, last);
			img = oponentWidget.snapshot(null, null);
			gc.drawImage(img, initialXOffset, initialYOffset);
			CheckBox c = new CheckBox();
			c.setLayoutX(initialXOffset + Oponent.squareWidth - 20);
			c.setLayoutY(initialYOffset + oponentWidget.getInitialY() + 3);
			this.checkBoxesPane.getChildren().add(c);

			c.selectedProperty().addListener((obsV, oldVal, newVal) -> {
				if (newVal) {
					this.addOponentToBattleField(oponent);
				} else {
					this.removeOponentFromBattleField(oponent);
				}
			});

			initialYOffset += img.getHeight();

			top = !top;
		}

	}

	/**
	 * Add opponents to the battle list!
	 * 
	 * @param o
	 */
	private void addOponentToBattleField(model.Opponent o) {
		this.battleList.put(o.getId(), o);
		System.out.println(o.getId() + " - " + o.getName());
	}

	/**
	 * Remove opponents from battle list!
	 * 
	 * @param o
	 */
	private void removeOponentFromBattleField(model.Opponent o) {
		this.battleList.remove(o.getId());
		System.out.println(o.getId() + " - " + o.getName());
	}
}
