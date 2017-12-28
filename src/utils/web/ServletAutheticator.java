/**
 * 
 */
package utils.web;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import model.Author;
import utils.database.Database;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class ServletAutheticator {

	private HttpServletRequest requestObject;

	private final String ERROR_MESSAGE = "Must set the requestObject!";

	public HttpServletRequest getRequestObject() {
		return requestObject;
	}

	public void setRequestObject(HttpServletRequest requestObject) {
		this.requestObject = requestObject;
	}

	public String getId() throws Exception {

		if (this.requestObject == null)
			throw new Exception(ERROR_MESSAGE);

		if (this.isAuthenticated()) {
			HttpSession session = this.requestObject.getSession();
			return session.getAttribute("userId").toString();
		}
		return null;
	}

	public boolean isAuthenticated() throws Exception {

		if (this.requestObject == null)
			throw new Exception(ERROR_MESSAGE);

		HttpSession session = this.requestObject.getSession();

		Object id = session.getAttribute("userId");

		return id == null ? false : true;
	}

	public boolean authenticate(String login, String passw) throws Exception {

		if (this.requestObject == null)
			throw new Exception(ERROR_MESSAGE);

		if (this.isAuthenticated()) {
			return true;
		}

		EntityManager entityManager = Database.getEntityManager();

		try {
			@SuppressWarnings("unchecked")
			List<Author> list = entityManager
					.createQuery("from Author a where a.login = :login and a.password = :password ")
					.setParameter("login", login).setParameter("password", passw).getResultList();

			if (list.size() > 0) {
				HttpSession session = this.requestObject.getSession();
				session.setAttribute("userId", list.get(0).getId());
				entityManager.clear();
				entityManager.close();
				return true;
			}

		} catch (PersistenceException e) {
			entityManager.clear();
			entityManager.close();
			return false;
		}
		entityManager.clear();
		entityManager.close();
		return false;
	}

}
