/**
 *
 */
package utils.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import model.Author;
import utils.database.Database;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Keeps the id of the logged in team (the {@link model.Opponent}) in the
 * session
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class ServletAuthenticator {

	private static final String SESSION_KEY = "opponentId";

	/**
	 * @return the id of the logged in opponent, or null if not authenticated
	 */
	public static Long getOpponentId(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object object = session.getAttribute(SESSION_KEY);
		return object instanceof Long ? (Long) object : null;
	}

	public static boolean isAuthenticated(HttpServletRequest request) {
		return ServletAuthenticator.getOpponentId(request) != null;
	}

	/**
	 * Checks the login and password and, if they are right, logs the team in
	 */
	public static boolean authenticate(HttpServletRequest request, String login, String password) {
		if (login == null || password == null) {
			return false;
		}

		List<Object[]> rows = Database.read(emValue -> emValue
				.createQuery("select o.id, a from Opponent o join o.authors a where a.login = :login", Object[].class)
				.setParameter("login", login).getResultList());

		for (Object[] row : rows) {
			Author author = (Author) row[1];
			if (PasswordHasher.verify(password, author.getPassword())) {
				ServletAuthenticator.login(request, (Long) row[0]);
				return true;
			}
		}
		return false;
	}

	/**
	 * Logs the team in without checking the password, used right after a new team
	 * registers itself
	 */
	public static void login(HttpServletRequest request, long opponentId) {
		request.getSession(true);
		// Prevents session fixation
		request.changeSessionId();
		request.getSession().setAttribute(SESSION_KEY, opponentId);
	}

	public static void logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}

}
