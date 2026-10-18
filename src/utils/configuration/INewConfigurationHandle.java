/**
 *
 */
package utils.configuration;

import java.util.Properties;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Implement this interface to load new configuration if not any
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public interface INewConfigurationHandle {

	public static final String DATABASE_FILE = "databaseFile";
	public static final String ROBOCODE_PATH = "robocodePath";
	public static final String REGISTRATION_ENABLED = "registrationEnabled";
	public static final String ROBOCODE_ROUNDS = "robocodeRounds";
	public static final String WEBSERVER_PORT = "webserverPort";
	public static final String WINNER_CRITERION = "winnerCriterion";

	/**
	 * Returns the properties for the system
	 *
	 * @param oldProperties
	 *            the current properties, or null if there is none
	 * @return the new properties, or null if the user cancelled
	 */
	public Properties askForProperties(Properties oldProperties);

}
