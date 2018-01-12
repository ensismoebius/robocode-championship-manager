package utils.battle;

import java.io.File;
import java.util.ArrayList;

import robocode.control.BattleSpecification;
import robocode.control.BattlefieldSpecification;
import robocode.control.RobocodeEngine;
import robocode.control.events.IBattleListener;
import utils.configuration.Configuration;

public class BattleStarter {

	private static IBattleListener battleListener;

	public static void setBattleListener(IBattleListener battleListener) {
		BattleStarter.battleListener = battleListener;
	}

	public static void start(ArrayList<String> robotsNames, int rounds) {

		if (robotsNames.size() < 2)
			return;

		if (rounds < 1)
			return;

		/**
		 * List of robots names
		 */
		String robotsString = "";

		// Builds the string with robots names
		for (String robotName : robotsNames) {
			robotsString += robotName + ",";
		}

		// Initializing robocode engine
		RobocodeEngine.setLogMessagesEnabled(false);
		RobocodeEngine engine = new RobocodeEngine(new File(Configuration.getRobocodePath()));

		// Add our own battle listener to the RobocodeEngine
		engine.addBattleListener(BattleStarter.battleListener);
		engine.setVisible(true);

		// TODO make screen resolution flexible
		BattleSpecification battleSpec = new BattleSpecification(rounds, new BattlefieldSpecification(800, 600), engine.getLocalRepository(robotsString));

		// Run our specified battle and let it run till it is over
		engine.runBattle(battleSpec, true);
	}
}
