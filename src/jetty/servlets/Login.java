/**
 *
 */
package jetty.servlets;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.text.TextTemplate;
import utils.configuration.Configuration;
import utils.web.Html;
import utils.web.ServletAuthenticator;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Login extends HttpServlet {

	private static final long serialVersionUID = -5170201804563458934L;

	/** Shows the login screen, or goes to the robot editor when the author is already logged in. */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (ServletAuthenticator.isAuthenticated(req)) {
			resp.sendRedirect("/robotEditor?action=edit");
			return;
		}
		this.showAuthScreen(resp, "Olá!");
	}

	/** Checks the login and the password of the form: logs the author in, or shows the screen again with the error. */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		req.setCharacterEncoding("UTF-8");

		String login = req.getParameter("login");
		String passw = req.getParameter("password");

		if (ServletAuthenticator.authenticate(req, login, passw)) {
			resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
			resp.setHeader("Location", "/robotEditor?action=edit");
		} else {
			this.showAuthScreen(resp, "Falha ao autenticar!");
		}
	}

	/** Writes the login screen with a message, such as an error. */
	private void showAuthScreen(HttpServletResponse resp, String message) throws IOException {
		/*
		 * JSP is a better tecnology than this,but looks like jetty does not gives
		 * support to it so i have to use this workaround
		 */
		TextTemplate template = new TextTemplate(getClass().getResourceAsStream("/jetty/htmlTemplates/main.html"));
		template.assign("message", Html.escape(message));
		boolean open = Configuration.isEnabledRegistration();
		template.assign("registrationClass", open ? "open" : "closed");
		template.assign("registrationStatus", open ? "● Inscrições abertas: cadastre seu robô" : "■ Inscrições encerradas: novos robôs não são aceitos");
		template.parse("main");

		// The registration status changes at any time, a cached page would lie about it
		resp.setHeader("Cache-Control", "no-store");
		resp.setContentType("text/html; charset=UTF-8");
		resp.getWriter().write(template.out("main"));
	}
}
