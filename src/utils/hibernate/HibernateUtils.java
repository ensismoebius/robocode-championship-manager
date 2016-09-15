package utils.hibernate;

import java.sql.Connection;

import javax.persistence.EntityManager;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class HibernateUtils {

	public static Connection getJDBCConnectionObject(EntityManager entityManager) {
		return new HibernateConnectionProvider(entityManager).getConnectionObject();
	}

}