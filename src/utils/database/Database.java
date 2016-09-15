package utils.database;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

public class Database {

	private static EntityManagerFactory entityManagerFactory;

	public static EntityManager getEntityManager() {

		if (Database.entityManagerFactory == null) {
			Database.entityManagerFactory = Persistence.createEntityManagerFactory("PURobocode");
		}

		return Database.entityManagerFactory.createEntityManager();
	}

	public static Query getQueryObject(String sql) {
		return Database.entityManagerFactory.createEntityManager().createQuery(sql);
	}
	
	public static void close(){
		Database.entityManagerFactory.close();
	}
}
