/**
 * 
 */
package jetty.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import model.Author;
import utils.database.Database;
import utils.regex.HtmlTemplate;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Main extends HttpServlet {

	private static final long serialVersionUID = -5170201804563458934L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String login = req.getParameter("login");
		String passw = req.getParameter("password");

		if (login == null || passw == null) {
			this.showAuthScreen(resp, "Olá!");
			return;
		}
		if (this.authenticate(req, login, passw)) {
			resp.sendRedirect("/robotEditor");
		}else {
			this.showAuthScreen(resp, "Falha ao autenticar!");
		}
	}

	private boolean authenticate(HttpServletRequest req, String login, String passw) {
		EntityManager entityManager = Database.getEntityManager();

		try {
			@SuppressWarnings("unchecked")
			List<Author> list = entityManager
					.createQuery("from Author a where a.login = :login and a.password = :password ")
					.setParameter("login", login).setParameter("password", passw).getResultList();

			if (list.size() > 0) {
				HttpSession session = req.getSession();
				session.setAttribute("user", list.get(0).getId());
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

	private void showAuthScreen(HttpServletResponse resp, String message) throws IOException {
		Path data = null;
		try {
			data = Paths.get(getClass().getResource("../htmlTemplates/main.html").toURI());
		} catch (URISyntaxException e) {
		}

		String html = String.join("", Files.readAllLines(data));

		/*
		 * JSP is a better tecnology than this,but looks like jetty does not gives
		 * support to it so i have to use this workaround
		 */
		HtmlTemplate template = new HtmlTemplate();
		template.setHtml(html);
		template.assign("message", message);
		template.parse("main");

		// Set response content type
		resp.setContentType("text/html");

		// Get the writer
		PrintWriter out = resp.getWriter();

		out.write(template.out("main"));
	}
}
