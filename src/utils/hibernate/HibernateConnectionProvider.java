package utils.hibernate;
import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class HibernateConnectionProvider implements Work {

	private EntityManager entityManager;
	private Connection connection;

	public HibernateConnectionProvider(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Connection getConnectionObject() {
		Session session = this.entityManager.unwrap(org.hibernate.Session.class);
		session.doWork(this);
		return this.connection;
	}

	@Override
	public void execute(Connection connection) throws SQLException {
		this.connection = connection;

	}

}
