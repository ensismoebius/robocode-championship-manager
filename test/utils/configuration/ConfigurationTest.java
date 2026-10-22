package utils.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import utils.robocode.BattleControl.WinnerCriterion;

/**
 * The program follows the configuration through the listeners, they must be
 * called on every real change and only then
 */
class ConfigurationTest {

	private final AtomicInteger changes = new AtomicInteger();
	private final Runnable listener = this.changes::incrementAndGet;
	private Properties answer;

	@BeforeEach
	void setUp() throws Exception {
		Files.createDirectories(Path.of(System.getProperty("user.home")));
		this.answer = properties("8080", "true");
		Configuration.setNewConfigurationHandle(old -> this.answer);
		assertTrue(Configuration.askForProperties());
		Configuration.addListener(this.listener);
	}

	@AfterEach
	void tearDown() {
		Configuration.removeListener(this.listener);
	}

	private static Properties properties(String port, String registration) {
		Properties p = new Properties();
		p.setProperty(INewConfigurationHandle.DATABASE_FILE, "database/test.db");
		p.setProperty(INewConfigurationHandle.ROBOCODE_PATH, "/nowhere");
		p.setProperty(INewConfigurationHandle.WEBSERVER_PORT, port);
		p.setProperty(INewConfigurationHandle.REGISTRATION_ENABLED, registration);
		return p;
	}

	@Test
	void notifiesWhenTheWindowChangesSomething() {
		this.answer = properties("9090", "true");
		assertTrue(Configuration.askForProperties());

		assertEquals(1, this.changes.get());
		assertEquals(9090, Configuration.getWebserverPort());
	}

	@Test
	void doesNotNotifyWhenNothingChanged() {
		this.answer = properties("8080", "true");
		assertTrue(Configuration.askForProperties());

		// Cancelled
		this.answer = null;
		assertTrue(Configuration.askForProperties());

		assertEquals(0, this.changes.get());
	}

	@Test
	void titleAndSubtitleHaveDefaults() {
		// Configurations saved before these options existed
		assertEquals(Configuration.DEFAULT_TITLE, Configuration.getTitle());
		assertEquals(Configuration.DEFAULT_SUBTITLE, Configuration.getSubtitle());

		this.answer = properties("8080", "true");
		this.answer.setProperty(INewConfigurationHandle.TITLE, "  Copa da Escola  ");
		this.answer.setProperty(INewConfigurationHandle.SUBTITLE, "");
		assertTrue(Configuration.askForProperties());
		assertEquals("Copa da Escola", Configuration.getTitle());
		// Empty hides the subtitle
		assertEquals("", Configuration.getSubtitle());
		assertEquals(1, this.changes.get());

		// The title can not be empty
		this.answer.setProperty(INewConfigurationHandle.TITLE, " ");
		assertTrue(Configuration.askForProperties());
		assertEquals(Configuration.DEFAULT_TITLE, Configuration.getTitle());
	}

	@Test
	void battleSpeedAndFightTextHaveDefaults() {
		// Configurations saved before these options existed
		assertEquals(Configuration.DEFAULT_FIGHT_TEXT, Configuration.getFightText());
		assertEquals(Configuration.DEFAULT_BATTLE_SPEED, Configuration.getBattleSpeed());

		this.answer = properties("8080", "true");
		this.answer.setProperty(INewConfigurationHandle.FIGHT_TEXT, "  LUTEM!  ");
		this.answer.setProperty(INewConfigurationHandle.BATTLE_SPEED, "4.0");
		assertTrue(Configuration.askForProperties());
		assertEquals("LUTEM!", Configuration.getFightText());
		assertEquals(4, Configuration.getBattleSpeed());

		// Blank text is the default one, an odd speed the closest one
		this.answer.setProperty(INewConfigurationHandle.FIGHT_TEXT, " ");
		this.answer.setProperty(INewConfigurationHandle.BATTLE_SPEED, "3");
		assertTrue(Configuration.askForProperties());
		assertEquals(Configuration.DEFAULT_FIGHT_TEXT, Configuration.getFightText());
		assertEquals(2, Configuration.getBattleSpeed());

		this.answer.setProperty(INewConfigurationHandle.BATTLE_SPEED, "rápido");
		assertTrue(Configuration.askForProperties());
		assertEquals(Configuration.DEFAULT_BATTLE_SPEED, Configuration.getBattleSpeed());
	}

	@Test
	void musicVolumeIsFullByDefaultAndKeptInRange() {
		assertEquals(100, Configuration.getMusicVolume());

		this.answer = properties("8080", "true");
		this.answer.setProperty(INewConfigurationHandle.MUSIC_VOLUME, "35");
		assertTrue(Configuration.askForProperties());
		assertEquals(35, Configuration.getMusicVolume());

		this.answer.setProperty(INewConfigurationHandle.MUSIC_VOLUME, "250");
		assertTrue(Configuration.askForProperties());
		assertEquals(100, Configuration.getMusicVolume());

		this.answer.setProperty(INewConfigurationHandle.MUSIC_VOLUME, "-5");
		assertTrue(Configuration.askForProperties());
		assertEquals(0, Configuration.getMusicVolume());
	}

	@Test
	void arenaAndMusicAreRandomByDefault() {
		assertEquals(Configuration.RANDOM, Configuration.getArena());
		assertEquals(Configuration.RANDOM, Configuration.getBattleMusic());

		this.answer = properties("8080", "true");
		this.answer.setProperty(INewConfigurationHandle.ARENA, "ICE");
		this.answer.setProperty(INewConfigurationHandle.BATTLE_MUSIC, "SYNTHWAVE");
		assertTrue(Configuration.askForProperties());
		assertEquals("ICE", Configuration.getArena());
		assertEquals("SYNTHWAVE", Configuration.getBattleMusic());
	}

	@Test
	void antialiasingIsOnByDefault() {
		assertTrue(Configuration.isAntialiasing());

		this.answer = properties("8080", "true");
		this.answer.setProperty(INewConfigurationHandle.ANTIALIASING, "false");
		assertTrue(Configuration.askForProperties());
		assertFalse(Configuration.isAntialiasing());

		this.answer.setProperty(INewConfigurationHandle.ANTIALIASING, "true");
		assertTrue(Configuration.askForProperties());
		assertTrue(Configuration.isAntialiasing());
	}

	@Test
	void arenaMusicIsOffByDefault() {
		assertFalse(Configuration.isArenaMusic());

		this.answer = properties("8080", "true");
		this.answer.setProperty(INewConfigurationHandle.ARENA_MUSIC, "true");
		assertTrue(Configuration.askForProperties());
		assertTrue(Configuration.isArenaMusic());
	}

	@Test
	void notifiesWhenRegistrationIsToggled() {
		Configuration.setEnabledRegistration(false);
		assertFalse(Configuration.isEnabledRegistration());
		assertEquals(1, this.changes.get());

		// Already closed
		Configuration.setEnabledRegistration(false);
		assertEquals(1, this.changes.get());
	}

	/** The configurations kept in memory, read and set the way the program holds them, so a test can restore them. */
	private static Properties keptConfigurations() throws ReflectiveOperationException {
		Field field = Configuration.class.getDeclaredField("configurations");
		field.setAccessible(true);
		return (Properties) field.get(null);
	}

	private static void keepConfigurations(Properties configurations) throws ReflectiveOperationException {
		Field field = Configuration.class.getDeclaredField("configurations");
		field.setAccessible(true);
		field.set(null, configurations);
	}

	@Test
	void theStartScriptIsTheOneOfTheRobocodeStarterFolder() {
		// The project folder has the script, the one next to the program is found first
		assertTrue(Configuration.getStartBattleScript().endsWith("start.sh"), Configuration.getStartBattleScript());
	}

	@Test
	void theFolderOfTheProgramIsTheFolderOfItsJar(@TempDir Path folder) throws Exception {
		// The classes of this build, packed in a jar of their own
		Path classes = Path.of(Configuration.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Path jar = folder.resolve("program.jar");
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar)); Stream<Path> files = Files.walk(classes)) {
			for (Path file : files.filter(Files::isRegularFile).toList()) {
				output.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
				Files.copy(file, output);
				output.closeEntry();
			}
		}

		// Loaded from the jar, the class tells where the jar is
		try (URLClassLoader loader = new URLClassLoader(new URL[] { jar.toUri().toURL() }, ClassLoader.getPlatformClassLoader())) {
			Object found = loader.loadClass(Configuration.class.getName()).getMethod("applicationDirectory").invoke(null);

			assertEquals(folder, found);
		}
	}

	@Test
	void aConfigurationThatIsNotLoadedIsSaidSo() throws ReflectiveOperationException {
		Properties loaded = keptConfigurations();
		try {
			keepConfigurations(null);

			assertThrows(IllegalStateException.class, Configuration::getWebserverPort);
			assertThrows(IllegalStateException.class, () -> Configuration.setEnabledRegistration(true));
		} finally {
			keepConfigurations(loaded);
		}
	}

	@Test
	void aConfigurationFileThatCanNotBeReadIsReplacedByTheAnswer() throws Exception {
		Path file = Path.of(System.getProperty("user.home"), "robocodeChampManager.config");
		Files.deleteIfExists(file);
		// A folder where the file should be: it can be neither read nor written
		Files.createDirectory(file);
		keepConfigurations(null);
		try {
			this.answer = properties("8181", "true");

			assertTrue(Configuration.askForProperties());
			assertEquals(8181, Configuration.getWebserverPort());
		} finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	void theSavedConfigurationIsLoadedBeforeItIsShown() throws Exception {
		Path file = Path.of(System.getProperty("user.home"), "robocodeChampManager.config");
		Files.deleteIfExists(file);
		try (OutputStream output = Files.newOutputStream(file)) {
			properties("8282", "false").store(output, null);
		}
		keepConfigurations(null);
		AtomicReference<Properties> shown = new AtomicReference<Properties>();
		Configuration.setNewConfigurationHandle(old -> {
			shown.set(old);
			// Cancelled: the saved one is still there
			return null;
		});
		try {
			assertTrue(Configuration.askForProperties());
			assertEquals("8282", shown.get().getProperty(INewConfigurationHandle.WEBSERVER_PORT));
		} finally {
			Configuration.setNewConfigurationHandle(old -> this.answer);
			Files.deleteIfExists(file);
		}
	}

	@Test
	void theOptionsMissingFromTheConfigurationHaveDefaults() {
		this.answer = properties("8080", "true");
		this.answer.remove(INewConfigurationHandle.ROBOCODE_PATH);
		this.answer.remove(INewConfigurationHandle.DATABASE_FILE);
		this.answer.remove(INewConfigurationHandle.ROBOCODE_ROUNDS);
		this.answer.remove(INewConfigurationHandle.WINNER_CRITERION);
		this.answer.remove(INewConfigurationHandle.BATTLE_VIEW);
		assertTrue(Configuration.askForProperties());

		assertEquals("", Configuration.getRobocodePath());
		assertEquals(Configuration.DEFAULT_DATABASE_FILE, Configuration.getDatabaseFile());
		assertEquals(5, Configuration.getRobocodeRounds());
		assertEquals(WinnerCriterion.RANK, Configuration.getWinnerCriterion());
		assertTrue(Configuration.isBattleView3D());
	}

	@Test
	void theOptionsOfTheBattleAreReadAsTheyAreWritten() {
		this.answer = properties("8080", "true");
		this.answer.setProperty(INewConfigurationHandle.DATABASE_FILE, "  ");
		this.answer.setProperty(INewConfigurationHandle.ROBOCODE_ROUNDS, "0");
		this.answer.setProperty(INewConfigurationHandle.WINNER_CRITERION, "SCORE");
		this.answer.setProperty(INewConfigurationHandle.BATTLE_VIEW, "CLASSIC");
		assertTrue(Configuration.askForProperties());

		assertEquals(Configuration.DEFAULT_DATABASE_FILE, Configuration.getDatabaseFile());
		// There is at least one round
		assertEquals(1, Configuration.getRobocodeRounds());
		assertEquals(WinnerCriterion.SCORE, Configuration.getWinnerCriterion());
		assertFalse(Configuration.isBattleView3D());
		assertEquals(System.getProperty("java.io.tmpdir"), Configuration.getTempDir());
	}

	@Test
	void aMissingConfigurationFileStartsFromNothing() throws Exception {
		Path file = Path.of(System.getProperty("user.home"), "robocodeChampManager.config");
		Files.deleteIfExists(file);
		keepConfigurations(null);
		AtomicReference<Properties> shown = new AtomicReference<Properties>(properties("1", "1"));
		Configuration.setNewConfigurationHandle(old -> {
			shown.set(old);
			return null;
		});
		try {
			assertFalse(Configuration.askForProperties());
			assertNull(shown.get());
		} finally {
			Configuration.setNewConfigurationHandle(old -> this.answer);
		}
	}

	@Test
	void anUnknownWinnerCriterionFallsBackToTheRank() {
		this.answer = properties("8080", "true");
		this.answer.setProperty(INewConfigurationHandle.WINNER_CRITERION, "NOPE");
		assertTrue(Configuration.askForProperties());

		assertEquals(WinnerCriterion.RANK, Configuration.getWinnerCriterion());
	}
}
