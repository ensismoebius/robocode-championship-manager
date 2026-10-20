package utils.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
