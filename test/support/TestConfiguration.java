package support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Stream;

import utils.configuration.Configuration;
import utils.database.Database;
import utils.configuration.INewConfigurationHandle;

/** The configuration the tests run with: complete, without asking anyone, with the database under target. */
public final class TestConfiguration {

	/** The database of the tests, kept under target so the project stays clean. */
	public static final String DATABASE_FILE = "target/test-db/robochamp.db";

	private TestConfiguration() {
	}

	/** Loads a complete configuration, as the first start of the program does, with the given arena and Robocode folder. */
	public static synchronized void load(String arena, String robocodePath) {
		load(arena, robocodePath, DATABASE_FILE);
	}

	/** Loads the same configuration with the database file given. */
	public static synchronized void load(String arena, String robocodePath, String databaseFile) {
		load(arena, robocodePath, databaseFile, 8080);
	}

	/** Loads the same configuration with the database file and the port of the web server given. */
	public static synchronized void load(String arena, String robocodePath, String databaseFile, int port) {
		Properties p = new Properties();
		p.setProperty(INewConfigurationHandle.DATABASE_FILE, databaseFile);
		p.setProperty(INewConfigurationHandle.ROBOCODE_PATH, robocodePath);
		p.setProperty(INewConfigurationHandle.REGISTRATION_ENABLED, "true");
		p.setProperty(INewConfigurationHandle.ROBOCODE_ROUNDS, "1");
		p.setProperty(INewConfigurationHandle.WEBSERVER_PORT, String.valueOf(port));
		p.setProperty(INewConfigurationHandle.WINNER_CRITERION, "SCORE");
		p.setProperty(INewConfigurationHandle.TITLE, "Campeonato");
		p.setProperty(INewConfigurationHandle.SUBTITLE, "Testes");
		p.setProperty(INewConfigurationHandle.BATTLE_VIEW, "3D");
		p.setProperty(INewConfigurationHandle.BATTLE_SPEED, "1");
		p.setProperty(INewConfigurationHandle.FIGHT_TEXT, "LUTA!");
		p.setProperty(INewConfigurationHandle.ARENA, arena);
		p.setProperty(INewConfigurationHandle.BATTLE_MUSIC, "RANDOM");
		p.setProperty(INewConfigurationHandle.MUSIC_VOLUME, "50");
		p.setProperty(INewConfigurationHandle.ARENA_MUSIC, "true");
		p.setProperty(INewConfigurationHandle.ANTIALIASING, "true");
		Configuration.setNewConfigurationHandle(old -> p);
		Configuration.askForProperties();
	}

	/** Empties the database of the tests, so the test starts without any championship. */
	public static synchronized void freshDatabase() {
		Database.close();
		Path folder = Path.of("target", "test-db");
		if (!Files.exists(folder)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(folder)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		} catch (IOException ioException) {
			throw new UncheckedIOException(ioException);
		}
	}
}
