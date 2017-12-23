package jetty.contexts;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;

public class Main extends ServletContextHandler {
	public Main(Server server) {

		// 1. Create a Servlet Handler
		ServletHandler servletHandler = new ServletHandler();

		// 2. Register our Servlet and its URL mapping
		servletHandler.addServletWithMapping(jetty.servlets.Main.class, "/");

		// 3. Binds the servletHandler with the ServletContextHandler
		this.insertHandler(servletHandler);
	}
}
