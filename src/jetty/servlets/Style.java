package jetty.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Serves the stylesheet of the registration pages and its fonts (the same ones
 * of the program)
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Style extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Only these files are served, nothing else of the classpath
	 */
	private static final Map<String, String> FONTS = Map.of( //
			"/PressStart2P-Regular.ttf", "/widgets/fonts/PressStart2P-Regular.ttf", //
			"/Rajdhani-Medium.ttf", "/widgets/fonts/Rajdhani-Medium.ttf", //
			"/Rajdhani-SemiBold.ttf", "/widgets/fonts/Rajdhani-SemiBold.ttf", //
			"/Rajdhani-Bold.ttf", "/widgets/fonts/Rajdhani-Bold.ttf");

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String resource;
		if ("/fonts".equals(req.getServletPath())) {
			resource = FONTS.get(req.getPathInfo());
			if (resource == null) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			resp.setContentType("font/ttf");
			resp.setHeader("Cache-Control", "max-age=86400");
		} else {
			resource = "/jetty/htmlTemplates/style.css";
			resp.setContentType("text/css; charset=UTF-8");
			resp.setHeader("Cache-Control", "max-age=300");
		}

		try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
			if (inputStream == null) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			inputStream.transferTo(resp.getOutputStream());
		}
	}
}
