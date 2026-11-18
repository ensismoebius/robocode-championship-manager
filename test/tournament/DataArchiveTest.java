package tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.Author;
import model.Championship;
import model.Opponent;
import support.TestConfiguration;
import utils.configuration.Configuration;
import utils.database.Database;

class DataArchiveTest {

	@BeforeEach
	void aChampionshipWithOneResult() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
		for (int index = 1; index <= 2; index++) {
			int number = index;
			Database.inTransaction(entityManager -> {
				Opponent robot = new Opponent();
				robot.setTeam("time" + number);
				robot.setName("pacote.Robo" + number);
				robot.setSourceCode("class Robo" + number + " {}");
				robot.setCompiledRobot(new byte[] { 1, 2, 3 });
				robot.setImage(new byte[] { (byte) 0x89, 'P', 'N', 'G' });
				Author author = new Author();
				author.setName("Autor " + number);
				author.setLogin("login" + number);
				author.setMail("autor" + number + "@exemplo.com");
				author.setRegistration("12" + number);
				author.setPassword("segredo" + number);
				robot.getAuthors().add(author);
				entityManager.persist(robot);
				return null;
			});
		}
		Championship championship = ChampionshipService.create(true, new Random(1));
		Match ready = ChampionshipService.bracketOf(championship).getNextMatch().orElseThrow();
		ChampionshipService.recordResult(championship, ready.getCode(), ready.getA().getParticipant(), 30, 10);
	}

	@AfterAll
	static void closeTheDatabase() {
		Database.close();
	}

	@Test
	void theReportHasTheChampionshipTheRobotsAndTheMatchesButNoSecrets() throws IOException {
		Championship championship = ChampionshipService.current();

		Path page = DataArchive.report(championship);
		String html = Files.readString(page, StandardCharsets.UTF_8);
		String csv = Files.readString(Path.of(page.toString().replace(".html", ".csv")), StandardCharsets.UTF_8);

		assertTrue(html.contains(championship.getName()));
		assertTrue(html.contains("time1.pacote.Robo1") && html.contains("time2.pacote.Robo2"));
		assertTrue(html.contains("Autor 1"));
		assertTrue(html.contains("30 x 10"));
		assertFalse(html.contains("segredo") || html.contains("login1"));
		assertEquals(2, csv.lines().count());
		assertTrue(csv.contains(",30,10,"));
	}

	@Test
	void theBackupIsADatabaseWithTheSameData() throws Exception {
		Path copy = DataArchive.backup();

		assertTrue(Files.size(copy) > 0);
		try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + copy); var rows = connection.createStatement().executeQuery("select count(*) from Opponent")) {
			rows.next();
			assertEquals(2, rows.getInt(1));
		}
	}

	@Test
	void twoBackupsInTheSameSecondDoNotCollide() throws IOException {
		assertFalse(DataArchive.backup().equals(DataArchive.backup()));
	}

	@Test
	void restoringABackupBringsTheDataBackAndKeepsTheReplacedOne() throws IOException {
		Path copy = DataArchive.backup();
		DataArchive.deleteAll();
		assertEquals(0, ChampionshipService.countOpponents());

		Path safety = DataArchive.restore(copy);

		assertEquals(2, ChampionshipService.countOpponents());
		assertNotNull(ChampionshipService.current());
		assertTrue(Files.size(safety) > 0);
	}

	@Test
	void restoringAnArchiveBringsTheDataBack() throws IOException {
		Path zip = DataArchive.archive(ChampionshipService.current());
		DataArchive.deleteAll();

		DataArchive.restore(zip);

		assertEquals(2, ChampionshipService.countOpponents());
		assertNotNull(ChampionshipService.current());
	}

	@Test
	void aFileThatIsNotABackupChangesNothing() throws IOException {
		Path other = Files.createTempFile("not-a-backup", ".db");
		Files.writeString(other, "not a database");

		assertThrows(IOException.class, () -> DataArchive.restore(other));
		assertEquals(2, ChampionshipService.countOpponents());
		Files.delete(other);
	}

	@Test
	void theArchiveKeepsTheReportTheRobotsAndTheDatabase() throws IOException {
		Path zip = DataArchive.archive(ChampionshipService.current());

		Set<String> names = new HashSet<String>();
		try (ZipFile file = new ZipFile(zip.toFile())) {
			file.stream().map(ZipEntry::getName).forEach(names::add);
		}
		assertTrue(names.contains("relatorio.html") && names.contains("partidas.csv"));
		assertTrue(names.contains("robos/time1/pacote.Robo1/codigo.java"));
		assertTrue(names.contains("robos/time1/pacote.Robo1/robo.jar"));
		assertTrue(names.contains("robos/time1/pacote.Robo1/imagem.png"));
		assertTrue(names.contains("banco/robochamp.db"));
		assertNotNull(ChampionshipService.current());
	}

	@Test
	void deletingTheChampionshipsKeepsTheRobots() {
		DataArchive.deleteChampionships();

		assertNull(ChampionshipService.current());
		assertEquals(2, ChampionshipService.countOpponents());
	}

	@Test
	void deletingEverythingLeavesTheDatabaseEmpty() {
		DataArchive.deleteAll();

		assertNull(ChampionshipService.current());
		assertEquals(0, ChampionshipService.countOpponents());
	}
}
