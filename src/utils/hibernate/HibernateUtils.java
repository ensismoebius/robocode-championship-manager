package utils.hibernate;

import java.sql.Connection;

import javax.persistence.EntityManager;

public class HibernateUtils {

	public static Connection getJDBCConnectionObject(EntityManager entityManager) {
		return new HibernateConnectionProvider(entityManager).getConnectionObject();
	}

}