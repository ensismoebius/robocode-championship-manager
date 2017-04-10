import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import listeners.OpponentsUpdater;
import model.Opponent;
import utils.database.Database;
import utils.robocode.BattleControl;
import widgets.scenes.BattleViewer;
import widgets.scenes.BattleWinnerUpdater;
import widgets.scenes.EditRobots;
import widgets.scenes.RankViewer;
import widgets.scenes.ShowOponent;
import widgets.scenes.StartScreen;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Main extends Application {

	// Data structures that holds all data
	private HashMap<String, model.Opponent> battleList;
	private ObservableList<model.Opponent> opponentsList = FXCollections.observableArrayList();

	// Point to primary stage
	private Stage primaryStage;

	// The Scenes of application
	private RankViewer rankScene;
	private StartScreen startScene;
	private EditRobots addRobotScene;
	private BattleViewer battleScene;
	private ShowOponent opponentScene;
	private BattleWinnerUpdater battleWinnerUpdater;

	/**
	 * The current opponent index
	 */
	private int opponentIndexIWantToShow;

	/**
	 * Controls the battles
	 */
	private BattleControl battleController;

	/**
	 * The path of the robocode program
	 */
	private String robocodePath;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		primaryStage.setTitle("Robochamp - Gerenciador de campeonatos de robocode");

		// Initializes database
		Database.getEntityManager();

		// Binds a listener that are going to do the database operations
		this.opponentsList.addListener(new OpponentsUpdater(this.opponentsList));

		this.initializeApplicationProperties(primaryStage);

		// Binds the keys to actions
		primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {

				// Keeps everything clean and silent
				Main.this.resetAllScenes();

				// Initial screen
				if (event.getCode() == KeyCode.I)
					Main.this.showStartScreen();

				// Repaint canvas
				if (event.getCode() == KeyCode.R)
					Main.this.showRankAndPopulateOpponentsList();

				// Adds a new robot
				if (event.getCode() == KeyCode.A)
					Main.this.editRobots();

				// Show the battle screen
				if (event.getCode() == KeyCode.B)
					Main.this.showBattle();

				// Starts the battle on robocode!
				if (event.getCode() == KeyCode.S)
					Main.this.startsBattle();

				// Circle through all opponents every time the M key is pressed
				if (event.getCode() == KeyCode.M)
					Main.this.showNextSelectedOpponent();

				// Circle through all opponents every time the M key is pressed
				if (event.getCode() == KeyCode.W)
					Main.this.showWinnerScreen();

			}
		});

		// Actions to do when closing the application
		primaryStage.setOnCloseRequest(event -> {
			Database.close();
			Platform.exit();
		});

		// Shows everything!
		primaryStage.setMaximized(true);
		primaryStage.show();

		// Default scene
		this.showStartScreen();
	}

	/**
	 * 
	 */
	protected void showStartScreen() {

		if (this.startScene == null) {
			this.startScene = new StartScreen();
		}
		this.startScene = new StartScreen();
		this.startScene.playBackgroundMusic();

		this.primaryStage.setScene(this.startScene);
	}

	// FIXME The battle starts fine and all needed files are created but the
	// looks corrupted
	protected void startsBattle() {

		if (this.battleController == null) {
			this.battleController = new BattleControl();
		}

		// Chooses the robocode dir
		if (this.robocodePath.equals("")) {
			DirectoryChooser dc = new DirectoryChooser();
			dc.setTitle("Choose a valid robocode directory");
			this.robocodePath = dc.showDialog(null).getPath();
		}

		// Gets the winner
		Opponent winner = this.battleController.startsBattle(this.battleList, this.robocodePath)[0];

		// Add one more level to the winner!!!
		// And saves to database
		winner.getLevels().add(winner.getLevels().size());
		EntityManager em = Database.getEntityManager();
		em.getTransaction().begin();
		em.merge(winner);
		em.getTransaction().commit();
		em.close();

		// Show the winner at all of his glory!!!
		this.showOpponent(winner);

	}

	/**
	 * Initializes all need properties
	 * 
	 * @param primaryStage
	 */
	private void initializeApplicationProperties(Stage primaryStage) {
		this.opponentIndexIWantToShow = 0;
		this.robocodePath = "";
		this.primaryStage = primaryStage;
		this.battleList = new HashMap<String, model.Opponent>();
	}

	/**
	 * Shows the add robot screen
	 */
	private void editRobots() {
		this.addRobotScene = new EditRobots(this.opponentsList);
		primaryStage.setScene(this.addRobotScene);
	}

	/**
	 * Shows the versus screen
	 */
	private void showBattle() {

		if (this.battleList.size() < 2) {
			new Alert(AlertType.ERROR, "Choose two robots at least!").showAndWait();
			return;
		}

		this.battleScene = new BattleViewer();

		// Get the opponents from list
		model.Opponent opponent1 = (model.Opponent) this.battleList.values().toArray()[0];
		model.Opponent opponent2 = (model.Opponent) this.battleList.values().toArray()[1];

		// Set the data!
		this.battleScene.setOpponents(opponent1, opponent2);
		this.battleScene.play();

		primaryStage.setScene(this.battleScene);
	}

	/**
	 * Updates a winner of the versus screen
	 */
	private void showWinnerScreen() {

		this.battleWinnerUpdater = new BattleWinnerUpdater();

		// Get the opponents from list
		model.Opponent opponent1 = (model.Opponent) this.battleList.values().toArray()[0];
		model.Opponent opponent2 = (model.Opponent) this.battleList.values().toArray()[1];

		// Set the data!
		this.battleWinnerUpdater.setOpponents(opponent1, opponent2);

		primaryStage.setScene(this.battleWinnerUpdater);
	}

	/**
	 * Shows the opponents screen
	 */
	private void showNextSelectedOpponent() {
		// Get next opponent
		Opponent item = (Opponent) Main.this.battleList.values().toArray()[this.opponentIndexIWantToShow];

		this.showOpponent(item);

		// Controls the index values to circle between opponents
		this.opponentIndexIWantToShow = this.opponentIndexIWantToShow == this.battleList.size() - 1 ? 0
				: this.opponentIndexIWantToShow + 1;
	}

	/**
	 * @param item
	 */
	private void showOpponent(Opponent item) {
		// Show the opponent
		try {
			if (this.opponentScene != null)
				this.opponentScene.stop();
			this.opponentScene = null;
			this.opponentScene = new ShowOponent();
			this.opponentScene.setOponent(item);
			this.opponentScene.play();
			primaryStage.setScene(this.opponentScene);
		} catch (Exception e) {
			new Alert(AlertType.ERROR, "Falha ao exibir oponente").showAndWait();
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Shows the rank screen and populate the opponents list
	 */
	private void showRankAndPopulateOpponentsList() {

		// Loads the robots!!
		this.loadData();

		// Every time we show the rank, we must erase all opponents in
		// battleList property otherwise it will become increasingly crowded
		this.battleList.clear();

		// A weird bug demand that we demaximize and maximize the window
		// otherwise scroll bars vanishes
		this.primaryStage.setMaximized(false);

		this.rankScene = new RankViewer(this.battleList, this.opponentsList);
		primaryStage.setScene(this.rankScene);

		// A weird bug demands that we demaximize and maximize the window
		// otherwise scroll bars vanishes
		this.primaryStage.setMaximized(true);
	}

	@SuppressWarnings("unchecked")
	private void loadData() {
		EntityManager entityManager = Database.getEntityManager();

		try {
			entityManager.createQuery("from Opponent").getResultList()
					.forEach(opponent -> this.opponentsList.add((Opponent) opponent));

		} catch (PersistenceException e) {
			this.opponentsList.clear();
		}
		entityManager.clear();
		entityManager.close();
	}

	/**
	 * Keep things clean an silent
	 */
	private void resetAllScenes() {

		// Resets the opponents scene
		if (this.opponentScene != null)
			this.opponentScene.stop();
		this.opponentScene = null;

		// Resets the main Scene
		if (this.battleScene != null)
			this.battleScene.stop();
		this.battleScene = null;

		// Resets initial screen
		if (this.startScene != null)
			this.startScene.stop();
		this.startScene = null;

		// Resets the add robot scene
		this.addRobotScene = null;

		// Resets the rank scene
		this.rankScene = null;

		// Tells java to clean the memory
		System.gc();
	}
}
