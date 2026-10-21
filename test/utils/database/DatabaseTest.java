package utils.database;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import support.TestConfiguration;
import utils.configuration.Configuration;

class DatabaseTest {

	@BeforeAll
	static void emptyDatabase() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
	}

	@AfterAll
	static void restoreTheDatabase() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
		Database.close();
	}

	@Test
	void aFailingTransactionIsRolledBackAndTheErrorGoesOn() {
		assertThrows(IllegalStateException.class, () -> Database.inTransaction(entityManager -> {
			throw new IllegalStateException("falhou no meio do trabalho");
		}));
	}

	@Test
	void aDatabaseInsideAFileCanNotBeOpened() throws IOException {
		Path blocker = Path.of("target", "test-home", "database-blocker");
		Files.createDirectories(blocker.getParent());
		Files.writeString(blocker, "um arquivo, não uma pasta");
		Database.close();
		TestConfiguration.load(Configuration.RANDOM, "/nowhere", blocker.resolve("robochamp.db").toString());

		assertThrows(UncheckedIOException.class, Database::getEntityManager);
	}
}
