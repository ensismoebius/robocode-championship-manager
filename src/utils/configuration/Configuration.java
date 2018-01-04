/**
 * 
 */
package utils.configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Configuration {

	/**
	 * Indicates if this is the first time the system runs
	 */
	private static boolean firstRun = false;

	private static Properties configurations;
	private static String configFileLocation;
	private static String tempDir;

	/**
	 * If there is no configuration stored call the implementation of this interface
	 * to create new properties
	 */
	private static INewConfigurationHandle newConfigurationHandle;

	/**
	 * Sets the implementation of {@link INewConfigurationHandle} to create new
	 * properties
	 * 
	 * @see INewConfigurationHandle
	 */
	public static void setNewConfigurationHandle(INewConfigurationHandle newConfigurationHandle) {
		Configuration.newConfigurationHandle = newConfigurationHandle;
	}

	/**
	 * Initialize if need the {@link Configuration} class
	 */
	private static void init() {
		Configuration.tempDir = System.getProperty("java.io.tmpdir");
		Configuration.configFileLocation = System.getProperty("user.home") + "/robocodeChampManager.config";

		if (!Configuration.loadConfigurations()) {
			Configuration.firstRun = true;
			Configuration.askForProperties();
		}
	}

	public static void askForProperties() {
		Configuration.configurations = Configuration.newConfigurationHandle.askForProperties(Configuration.configurations);
		Configuration.saveConfigurations();
	}

	/**
	 * Returns any configuration if any
	 * 
	 * @param name
	 * @return
	 */
	private static String getConfiguration(String name) {

		while (Configuration.configurations == null) {
			Configuration.init();
		}

		return Configuration.configurations.getProperty(name);
	}

	/**
	 * Loads the configurations from file
	 * 
	 * @return
	 */
	private static boolean loadConfigurations() {
		InputStream is;
		try {
			is = new FileInputStream(Configuration.configFileLocation);
			Configuration.configurations = new Properties();
			Configuration.configurations.load(is);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Save all configurations to a file
	 * 
	 * @return
	 */
	private static boolean saveConfigurations() {
		FileOutputStream os;
		try {
			os = new FileOutputStream(Configuration.configFileLocation);
			Configuration.configurations.store(os, "RobocodeChampManagerConfiguration");
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean isFirstRun() {
		return Configuration.firstRun;
	}

	public static String getTempDir() {
		return Configuration.tempDir;
	}

	public static String getDatabaseURL() {
		return Configuration.getConfiguration(INewConfigurationHandle.DATABASE_URL);
	}

	public static String getDatabaseUser() {
		return Configuration.getConfiguration(INewConfigurationHandle.DATABASE_USER);
	}

	public static String getDatabasePassword() {
		return Configuration.getConfiguration(INewConfigurationHandle.DATABASE_PASSWORD);
	}

	public static String getRobocodePath() {
		return Configuration.getConfiguration(INewConfigurationHandle.ROBOCODE_PATH);
	}

}
