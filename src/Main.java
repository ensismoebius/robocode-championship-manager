import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import jetty.server.HttpServer;
import model.Opponent;
import model.OpponentGroup;
import utils.battle.OpponentGroupFactory;
import utils.configuration.Configuration;
import utils.database.Database;
import utils.robocode.BattleControl;
import widgets.scenes.BattleViewer;
import widgets.scenes.RankViewer;
import widgets.scenes.ShowOponent;
import widgets.scenes.StartScreen;
import widgets.windows.NewConfigurationHandleWindow;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Main extends Application {

	// Data structures that holds all data
	private HashMap<String, model.Opponent> battleList;
	private ObservableMap<String, model.OpponentGroup> groupsList = FXCollections.observableHashMap();

	// Point to primary stage
	private Stage primaryStage;

	// The Scenes of application
	private RankViewer rankScene;
	private StartScreen startScene;
	private BattleViewer battleScene;
	private ShowOponent opponentScene;

	/**
	 * The current opponent index
	 */
	private int opponentIndexIWantToShow;

	/**
	 * Controls the battles
	 */
	private BattleControl battleController;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		Configuration.setNewConfigurationHandle(new NewConfigurationHandleWindow());
		Configuration.askForProperties();

		primaryStage.setTitle("Robochamp - Gerenciador de campeonatos de robocode");

		// Initializes database
		Database.getEntityManager();

		this.initializeApplicationProperties(primaryStage);

		// Binds the keys to actions
		primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {

				// Keeps everything clean and silent
				Main.this.resetAllScenes();

				// All events with control key
				if (event.isControlDown()) {

					// Blocks or liberate the robots registration
					if (event.getCode() == KeyCode.S) {
						if (Configuration.isEnabledRegistration()) {
							Configuration.setEnabledRegistration(false);
							return;
						}
						Configuration.setEnabledRegistration(true);
					}

					// Create groups and show rank
					if (event.getCode() == KeyCode.G) {
						Main.this.createGroups();
						Main.this.showRankAndPopulateOpponentsList();
					}
					// If control is down block the access for another keys
					return;
				}

				// Initial screen
				if (event.getCode() == KeyCode.I)
					Main.this.showStartScreen();

				// Repaint canvas
				if (event.getCode() == KeyCode.R)
					Main.this.showRankAndPopulateOpponentsList();

				// Show the battle screen
				if (event.getCode() == KeyCode.B)
					Main.this.showBattle();

				// Starts the battle on robocode!
				if (event.getCode() == KeyCode.S)
					Main.this.startsBattle();

				// Circle through all opponents every time the M key is pressed
				if (event.getCode() == KeyCode.M)
					Main.this.showNextSelectedOpponent();

				// Exports all robots to files
				if (event.getCode() == KeyCode.X)
					Main.this.exportToFiles();
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

		// Start the web server for remote robots uploads
		this.startWebServer();
	}

	/**
	 * Start the web server for remote robots uploads
	 */
	private void startWebServer() {
		Task<Void> serverTask = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				new HttpServer();
				return null;
			}

		};
		new Thread(serverTask).start();
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

	protected void startsBattle() {

		if (this.battleController == null) {
			this.battleController = new BattleControl();
		}

		// Chooses the robocode dir
		if (Configuration.getRobocodePath().equals("")) {
			Configuration.askForProperties();
		}

		// Do the battle an gets the results
		ArrayList<Opponent> opponents = this.battleController.startsBattle(this.battleList, Configuration.getRobocodePath(), Configuration.getRobocodeRounds(), Configuration.getStartBattleScript());

		// Gets the winner
		Opponent winner = opponents.get(0);
		Integer winnerLevel = winner.getLevels().get(winner.getLevels().size() - 1);

		// Gets the loser
		Opponent loser = opponents.get(1);
		Integer loserLevel = loser.getLevels().get(loser.getLevels().size() - 1);

		// Only opponents with lower or same level can level up.
		// This is done because sometimes we have to make lower
		// levels robots compete with higher levels robots
		if (winnerLevel <= loserLevel) {
			// Add one more level to the winner!!!
			// And saves to database
			winner.getLevels().add(winner.getLevels().size());
			EntityManager em = Database.getEntityManager();
			em.getTransaction().begin();
			em.merge(winner);
			em.getTransaction().commit();
			em.close();
		}

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
		this.primaryStage = primaryStage;
		this.battleList = new HashMap<String, model.Opponent>();
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
	 * Shows the opponents screen
	 */
	private void showNextSelectedOpponent() {
		// Get next opponent
		Opponent item = (Opponent) Main.this.battleList.values().toArray()[this.opponentIndexIWantToShow];

		this.showOpponent(item);

		// Controls the index values to circle between opponents
		this.opponentIndexIWantToShow = this.opponentIndexIWantToShow == this.battleList.size() - 1 ? 0 : this.opponentIndexIWantToShow + 1;
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

		this.rankScene = new RankViewer(this.battleList, this.groupsList);
		primaryStage.setScene(this.rankScene);

		// A weird bug demands that we demaximize and maximize the window
		// otherwise scroll bars vanishes
		this.primaryStage.setMaximized(true);
	}

	private ObservableMap<String, model.OpponentGroup> createGroups() {

		// delete all existent groups
		EntityManager entityManager = Database.getEntityManager();
		entityManager.getTransaction().begin();
		entityManager.createQuery("UPDATE Opponent SET groupId = null").executeUpdate();
		entityManager.createQuery("delete OpponentGroup").executeUpdate();
		entityManager.getTransaction().commit();
		entityManager.clear();
		entityManager.close();

		entityManager = Database.getEntityManager();
		entityManager.getTransaction().begin();
		@SuppressWarnings("unchecked")
		List<Opponent> opponents = (List<Opponent>) entityManager.createQuery("from Opponent").getResultList();

		ObservableMap<String, OpponentGroup> groups = OpponentGroupFactory.createBattleGroups(opponents.size(), Integer.parseInt(Configuration.getGroupSize()));

		// Put all the opponents in a group
		for (int i = 0; i < opponents.size(); i++) {

			// Iterates over all created group and populates it
			for (Entry<String, OpponentGroup> opponentGroup : groups.entrySet()) {

				try {

					// While the group is not full and there is opponents put an opponent
					while (!opponentGroup.getValue().isFull()) {
						opponentGroup.getValue().getOpponents().add(opponents.get(i));
						i++;
					}

					// Saves the group
					entityManager.persist(opponentGroup.getValue());

				} catch (IndexOutOfBoundsException e) {
					entityManager.persist(opponentGroup.getValue());
					break;
				}

			}
		}

		entityManager.getTransaction().commit();
		entityManager.clear();
		entityManager.close();

		return groups;
	}

	@SuppressWarnings("unchecked")
	private void loadData() {

		this.groupsList.clear();

		EntityManager entityManager = Database.getEntityManager();

		List<OpponentGroup> opponentsGroups = entityManager.createQuery("from OpponentGroup").getResultList();

		try {
			opponentsGroups.forEach(opponentGroup -> {
				this.groupsList.put(((OpponentGroup) opponentGroup).getName(), (OpponentGroup) opponentGroup);
			});

		} catch (PersistenceException e) {
			this.groupsList.clear();
			e.printStackTrace();
		}
		entityManager.clear();
		entityManager.close();
	}

	@SuppressWarnings("unchecked")
	private void exportToFiles() {

		EntityManager entityManager = Database.getEntityManager();

		try {
			entityManager.createQuery("from Opponent").getResultList().forEach(opponent -> {

				String filename = ((Opponent) opponent).getName();
				String sourceCode = ((Opponent) opponent).getSourceCode();

				try {
					FileWriter fw = new FileWriter(Configuration.getTempDir() + "/" + filename + ".java");
					fw.write(sourceCode);
					fw.flush();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (PersistenceException e) {
			e.printStackTrace();
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

		// Resets the rank scene
		this.rankScene = null;

		// Tells java to clean the memory
		System.gc();
	}
}
