/**
 *
 */
package utils.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import utils.robocode.BattleControl.WinnerCriterion;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Configuration {

	public static final String DEFAULT_DATABASE_FILE = "database/robochamp.db";
	public static final int DEFAULT_ROBOCODE_ROUNDS = 5;
	public static final int DEFAULT_WEBSERVER_PORT = 8080;

	private static final Path CONFIG_FILE = Path.of(System.getProperty("user.home"), "robocodeChampManager.config");

	private static Properties configurations;

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
	 * Shows the current configuration (loaded from file if needed) so the user can
	 * review or change it
	 *
	 * @return true if there is a usable configuration afterwards, false if the user
	 *         cancelled and there was no configuration
	 */
	public static synchronized boolean askForProperties() {
		Properties current = Configuration.configurations != null ? Configuration.configurations : Configuration.loadConfigurations();

		Properties answer = Configuration.newConfigurationHandle.askForProperties(current);

		if (answer != null) {
			Configuration.configurations = answer;
			Configuration.saveConfigurations();
		} else {
			Configuration.configurations = current;
		}

		return Configuration.configurations != null;
	}

	/**
	 * Returns any configuration if any
	 *
	 * @param name
	 * @return
	 */
	private static synchronized String getConfiguration(String name) {
		if (Configuration.configurations == null) {
			throw new IllegalStateException("Configuration not loaded, call askForProperties() first");
		}
		return Configuration.configurations.getProperty(name);
	}

	private static int getIntConfiguration(String name, int defaultValue) {
		try {
			return Integer.parseInt(Configuration.getConfiguration(name).trim());
		} catch (NullPointerException | NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Loads the configurations from file
	 *
	 * @return the configurations, or null if there is no configuration file
	 */
	private static Properties loadConfigurations() {
		if (!Files.exists(Configuration.CONFIG_FILE)) {
			return null;
		}

		try (InputStream is = Files.newInputStream(Configuration.CONFIG_FILE)) {
			Properties properties = new Properties();
			properties.load(is);
			return properties;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Save all configurations to a file
	 *
	 * @return
	 */
	private static synchronized boolean saveConfigurations() {
		try (OutputStream os = Files.newOutputStream(Configuration.CONFIG_FILE)) {
			Configuration.configurations.store(os, "RobocodeChampManagerConfiguration");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static synchronized void setEnabledRegistration(boolean b) {
		if (Configuration.configurations == null) {
			throw new IllegalStateException("Configuration not loaded, call askForProperties() first");
		}
		Configuration.configurations.setProperty(INewConfigurationHandle.REGISTRATION_ENABLED, String.valueOf(b));
		Configuration.saveConfigurations();
	}

	public static String getTempDir() {
		return System.getProperty("java.io.tmpdir");
	}

	/**
	 * The SQLite database file
	 */
	public static String getDatabaseFile() {
		String file = Configuration.getConfiguration(INewConfigurationHandle.DATABASE_FILE);
		return file == null || file.isBlank() ? DEFAULT_DATABASE_FILE : file;
	}

	public static String getRobocodePath() {
		String path = Configuration.getConfiguration(INewConfigurationHandle.ROBOCODE_PATH);
		return path == null ? "" : path;
	}

	public static boolean isEnabledRegistration() {
		return Boolean.parseBoolean(Configuration.getConfiguration(INewConfigurationHandle.REGISTRATION_ENABLED));
	}

	public static int getWebserverPort() {
		return Configuration.getIntConfiguration(INewConfigurationHandle.WEBSERVER_PORT, DEFAULT_WEBSERVER_PORT);
	}

	public static int getRobocodeRounds() {
		return Math.max(1, Configuration.getIntConfiguration(INewConfigurationHandle.ROBOCODE_ROUNDS, DEFAULT_ROBOCODE_ROUNDS));
	}

	/**
	 * How the winner of a battle is chosen, robocode rank by default
	 */
	public static WinnerCriterion getWinnerCriterion() {
		String value = Configuration.getConfiguration(INewConfigurationHandle.WINNER_CRITERION);
		try {
			return value == null ? WinnerCriterion.RANK : WinnerCriterion.valueOf(value);
		} catch (IllegalArgumentException e) {
			return WinnerCriterion.RANK;
		}
	}

	public static String getStartBattleScript() {
		String osname = System.getProperty("os.name").toLowerCase(Locale.ROOT);

		if (osname.contains("win")) {
			return "./RobocodeStarter/start.bat";
		}

		// Linux, macOS and other unixes
		return "./RobocodeStarter/start.sh";
	}
}
