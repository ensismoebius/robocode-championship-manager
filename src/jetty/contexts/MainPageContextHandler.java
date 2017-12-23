package jetty.contexts;

import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jetty.servlets.MainServlet;
import jetty.servlets.RobotSubmissionServlet;

public class MainPageContextHandler extends ServletContextHandler {
	public MainPageContextHandler(Server server) {

		// 1. Create a Servlet Handler
		ServletHandler servletHandler = new ServletHandler();

		// 2. Register our Servlet and its URL mapping
		servletHandler.addServletWithMapping(MainServlet.class, "/");

		// 3. Binds the servletHandler with the ServletContextHandler
		this.insertHandler(servletHandler);
	}
}
