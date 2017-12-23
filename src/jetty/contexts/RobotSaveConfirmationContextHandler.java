package jetty.contexts;

import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jetty.servlets.RobotSubmissionServlet;

public class RobotSaveConfirmationContextHandler extends ServletContextHandler {
	public RobotSaveConfirmationContextHandler(Server server) {

		// 1. Create a Servlet Handler
		ServletHandler servletHandler = new ServletHandler();

		// 2. Register our Servlet and its URL mapping
		ServletHolder servletHolder = new ServletHolder();
		servletHandler.addServletWithMapping(RobotSubmissionServlet.class, "/*");

		// 7. Binds the servletHandler with the ServletContextHandler
		this.insertHandler(servletHandler);
	}
}
