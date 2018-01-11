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

	public static final String GROUPS_SIZE = "groupsSize";
	public static final String DATABASE_URL = "databaseURL";
	public static final String DATABASE_USER = "databaseUser";
	public static final String DATABASE_PASSWORD = "databasePassword";
	public static final String ROBOCODE_PATH = "robocodePath";

	/**
	 * Returns the properties for the system
	 * 
	 * @return
	 */
	public Properties askForProperties(Properties oldProperties);

}
