package utils.database;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import utils.configuration.Configuration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Database {

	/** Only static methods: this class is not instantiated. */
	private Database() {
	}

	private static EntityManagerFactory entityManagerFactory;

	/** The entity manager of the database, created the first time it is needed. */
	public static synchronized EntityManager getEntityManager() {

		if (Database.entityManagerFactory == null) {

			// SQLite creates the file but not its directory
			Path file = Path.of(Configuration.getDatabaseFile()).toAbsolutePath();
			try {
				Files.createDirectories(file.getParent());
			} catch (IOException ioException) {
				throw new UncheckedIOException(ioException);
			}

			Map<String, Object> persistenceMap = new HashMap<String, Object>();
			persistenceMap.put("jakarta.persistence.jdbc.url", "jdbc:sqlite:" + file);

			Database.entityManagerFactory = Persistence.createEntityManagerFactory("PURobocode", persistenceMap);
		}

		return Database.entityManagerFactory.createEntityManager();
	}

	/**
	 * Runs the work inside a transaction, committing on success and rolling back on
	 * failure. The entity manager is always closed, so returned entities are
	 * detached.
	 */
	public static <T> T inTransaction(Function<EntityManager, T> work) {
		EntityManager entityManager = Database.getEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			transaction.begin();
			T result = work.apply(entityManager);
			transaction.commit();
			return result;
		} catch (RuntimeException runtimeException) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw runtimeException;
		} finally {
			entityManager.close();
		}
	}

	/**
	 * Runs a read only work. The entity manager is always closed, so returned
	 * entities are detached.
	 */
	public static <T> T read(Function<EntityManager, T> work) {
		EntityManager entityManager = Database.getEntityManager();
		try {
			return work.apply(entityManager);
		} finally {
			entityManager.close();
		}
	}

	/** Closes the database, if it is open. */
	public static synchronized void close() {
		if (Database.entityManagerFactory != null && Database.entityManagerFactory.isOpen()) {
			Database.entityManagerFactory.close();
		}
		Database.entityManagerFactory = null;
	}
}
