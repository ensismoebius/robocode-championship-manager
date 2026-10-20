/**
 *
 */
package utils.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

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
	public static final String DEFAULT_TITLE = "CHAMPIONSHIP";
	public static final String DEFAULT_SUBTITLE = "GERENCIADOR DE CAMPEONATOS";
	public static final String DEFAULT_FIGHT_TEXT = "FIGHT!";
	/** Speeds of the 3D battle, times robocode's normal speed */
	public static final double[] BATTLE_SPEEDS = { 0.5, 1, 2, 4, 8 };
	public static final double DEFAULT_BATTLE_SPEED = 1;
	/** Volume of the background music, in percent */
	public static final int DEFAULT_MUSIC_VOLUME = 100;
	/** A different arena or song in each battle */
	public static final String RANDOM = "RANDOM";

	private static final Path CONFIG_FILE = Path.of(System.getProperty("user.home"), "robocodeChampManager.config");

	private static Properties configurations;

	/**
	 * If there is no configuration stored call the implementation of this interface
	 * to create new properties
	 */
	private static INewConfigurationHandle newConfigurationHandle;

	/**
	 * Called after the configuration changes, so who shows or uses it can update
	 */
	private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

	public static void addListener(Runnable listener) {
		Configuration.listeners.add(listener);
	}

	public static void removeListener(Runnable listener) {
		Configuration.listeners.remove(listener);
	}

	private static void notifyListeners() {
		for (Runnable listener : Configuration.listeners) {
			listener.run();
		}
	}

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
			if (!answer.equals(current)) {
				Configuration.notifyListeners();
			}
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
		return Configuration.parseInt(Configuration.getConfiguration(name), defaultValue);
	}

	/**
	 * Parses a trimmed integer, the default if null, blank or not a number
	 */
	public static int parseInt(String value, int defaultValue) {
		try {
			return Integer.parseInt(value.trim());
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
		Object old = Configuration.configurations.setProperty(INewConfigurationHandle.REGISTRATION_ENABLED, String.valueOf(b));
		Configuration.saveConfigurations();
		if (!String.valueOf(b).equals(old)) {
			Configuration.notifyListeners();
		}
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

	/**
	 * Title of the start screen, the default one if blank
	 */
	public static String getTitle() {
		String title = Configuration.getConfiguration(INewConfigurationHandle.TITLE);
		return title == null || title.isBlank() ? DEFAULT_TITLE : title.strip();
	}

	/**
	 * Subtitle of the start screen, the default one if not configured, empty to
	 * hide it
	 */
	public static String getSubtitle() {
		String subtitle = Configuration.getConfiguration(INewConfigurationHandle.SUBTITLE);
		return subtitle == null ? DEFAULT_SUBTITLE : subtitle.strip();
	}

	/**
	 * The arena of the 3D battle (a name of widgets.scenes.ArenaStyle), or
	 * {@link #RANDOM}
	 */
	public static String getArena() {
		String arena = Configuration.getConfiguration(INewConfigurationHandle.ARENA);
		return arena == null || arena.isBlank() ? RANDOM : arena.strip();
	}

	/**
	 * The song of the 3D battle (a name of widgets.BattleAudio.Song), or
	 * {@link #RANDOM}
	 */
	public static String getBattleMusic() {
		String music = Configuration.getConfiguration(INewConfigurationHandle.BATTLE_MUSIC);
		return music == null || music.isBlank() ? RANDOM : music.strip();
	}

	/**
	 * If each arena plays its own song, whatever the song configured
	 */
	public static boolean isArenaMusic() {
		return Boolean.parseBoolean(Configuration.getConfiguration(INewConfigurationHandle.ARENA_MUSIC));
	}

	/**
	 * The volume of the background music of the battle, from 0 (mute) to 100
	 */
	public static int getMusicVolume() {
		return Math.max(0, Math.min(100, Configuration.getIntConfiguration(INewConfigurationHandle.MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME)));
	}

	/**
	 * Shown when a round starts, the default one if blank
	 */
	public static String getFightText() {
		String text = Configuration.getConfiguration(INewConfigurationHandle.FIGHT_TEXT);
		return text == null || text.isBlank() ? DEFAULT_FIGHT_TEXT : text.strip();
	}

	/**
	 * The speed the 3D battle starts with, one of {@link #BATTLE_SPEEDS}: the
	 * closest one to the configured value, normal speed if not configured
	 */
	public static double getBattleSpeed() {
		double wanted;
		try {
			String value = Configuration.getConfiguration(INewConfigurationHandle.BATTLE_SPEED);
			wanted = value == null ? DEFAULT_BATTLE_SPEED : Double.parseDouble(value.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			wanted = DEFAULT_BATTLE_SPEED;
		}
		double best = DEFAULT_BATTLE_SPEED;
		for (double speed : BATTLE_SPEEDS) {
			if (Math.abs(speed - wanted) < Math.abs(best - wanted)) {
				best = speed;
			}
		}
		return best;
	}

	/**
	 * The battle is shown in 3D by the program (default), or in the robocode
	 * window ("CLASSIC")
	 */
	public static boolean isBattleView3D() {
		return !"CLASSIC".equals(Configuration.getConfiguration(INewConfigurationHandle.BATTLE_VIEW));
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
