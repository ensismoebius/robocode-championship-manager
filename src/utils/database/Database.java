package utils.database;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import utils.configuration.Configuration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Database {

	private static EntityManagerFactory entityManagerFactory;

	public static EntityManager getEntityManager() {

		if (Database.entityManagerFactory == null) {

			Map<String, String> persistenceMap = new HashMap<String, String>();

			persistenceMap.put("javax.persistence.jdbc.url", Configuration.getDatabaseURL());
			persistenceMap.put("javax.persistence.jdbc.user", Configuration.getDatabaseUser());
			persistenceMap.put("javax.persistence.jdbc.password", Configuration.getDatabasePassword());

			// Used to create the database, only if there is no config file
			if (Configuration.isFirstRun()) {
				persistenceMap.put("javax.persistence.schema-generation.database.action", "drop-and-create");
			}

			Database.entityManagerFactory = Persistence.createEntityManagerFactory("PURobocode", persistenceMap);
		}

		return Database.entityManagerFactory.createEntityManager();
	}

	public static Query getQueryObject(String sql) {
		return Database.entityManagerFactory.createEntityManager().createQuery(sql);
	}

	public static void close() {
		Database.entityManagerFactory.close();
	}
}
