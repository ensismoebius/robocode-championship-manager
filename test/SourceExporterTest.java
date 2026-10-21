import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Opponent;
import support.TestConfiguration;
import utils.configuration.Configuration;
import utils.database.Database;

class SourceExporterTest {

	private static Path folder;

	@BeforeAll
	static void emptyDatabase() throws IOException {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
		folder = Path.of("target", "test-home", "export");
		Files.createDirectories(folder);
		Database.inTransaction(entityManager -> {
			Opponent withSource = new Opponent();
			withSource.setTeam("time1");
			withSource.setName("pacote.Robo");
			withSource.setSourceCode("package pacote; public class Robo {}");
			entityManager.persist(withSource);
			Opponent withoutSource = new Opponent();
			withoutSource.setTeam("time2");
			withoutSource.setName("Sem");
			entityManager.persist(withoutSource);
			return null;
		});
	}

	@AfterAll
	static void closeTheDatabase() {
		Database.close();
	}

	@Test
	void theSourceOfEachRobotIsWrittenInItsTeamFolder() throws IOException {
		Path exportDir = folder.resolve("ok");

		assertEquals(2, SourceExporter.export(exportDir));

		assertEquals("package pacote; public class Robo {}", Files.readString(exportDir.resolve("time1").resolve("pacote").resolve("Robo.java")));
		assertFalse(Files.exists(exportDir.resolve("time2")));
	}

	@Test
	void theDefaultFolderIsInTheTemporaryFolder() {
		assertTrue(SourceExporter.defaultDir().endsWith("robochamp-export"));
	}

	@Test
	void aFolderThatCannotBeCreatedFails() throws IOException {
		Path blocked = folder.resolve("blocked");
		Files.writeString(blocked, "um arquivo, não uma pasta");

		assertThrows(IOException.class, () -> SourceExporter.export(blocked));
	}
}
