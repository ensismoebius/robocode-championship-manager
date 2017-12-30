/**
 * 
 */
package jetty.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import utils.text.TextTemplate;
import utils.web.ServletAutheticator;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Main extends HttpServlet {

	private static final long serialVersionUID = -5170201804563458934L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {
		try {
			String login = req.getParameter("login");
			String passw = req.getParameter("password");

			if (login == null || passw == null) {
				this.showAuthScreen(resp, "Olá!");
				return;
			}

			ServletAutheticator sa = new ServletAutheticator();
			sa.setRequestObject(req);

			if (sa.authenticate(login, passw)) {
				resp.sendRedirect("/robotEditor?action=edit");
			} else {
				this.showAuthScreen(resp, "Falha ao autenticar!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showAuthScreen(HttpServletResponse resp, String message) throws IOException, URISyntaxException {
		/*
		 * JSP is a better tecnology than this,but looks like jetty does not gives
		 * support to it so i have to use this workaround
		 */
		TextTemplate template = new TextTemplate(getClass().getResource("../htmlTemplates/main.html").toURI());
		template.assign("message", message);
		template.parse("main");

		// Set response content type
		resp.setContentType("text/html");

		// Get the writer
		PrintWriter out = resp.getWriter();

		out.write(template.out("main"));
	}
}
