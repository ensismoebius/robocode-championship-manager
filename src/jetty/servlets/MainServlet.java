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
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import utils.regex.HtmlTemplate;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class MainServlet extends HttpServlet {

	private static final long serialVersionUID = -5170201804563458934L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		Path data = null;
		try {
			data = Paths.get(getClass().getResource("../htmlTemplates/main/main.jsp").toURI());
		} catch (URISyntaxException e) {
		}

		String html = String.join("", Files.readAllLines(data));

		/*
		 * JSP is a better tecnology than this,but looks like jetty does 
		 * not gives support to it so i have to use this workaround
		 */
		HtmlTemplate template = new HtmlTemplate();
		template.setHtml(html);
		template.parse("main");

		// Set response content type
		resp.setContentType("text/html");

		// Get the writer
		PrintWriter out = resp.getWriter();

		out.write(template.out("main"));

	}
}
