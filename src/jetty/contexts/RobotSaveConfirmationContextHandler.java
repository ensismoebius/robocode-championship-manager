package jetty.contexts;

import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jetty.servlets.ImageUploadServlet;

public class RobotSaveConfirmationContextHandler extends ServletContextHandler {
	public RobotSaveConfirmationContextHandler(Server server, URL resourseFolder) {

		// 1. Create a Servlet Handler
		ServletHandler servletHandler = new ServletHandler();

		// 2. Register our Servlet and its URL mapping
		ServletHolder servletHolder = new ServletHolder();
		servletHandler.addServletWithMapping(ImageUploadServlet.class, "/*");

		// 3. Defining the context path (in this case: http://<address>/
		this.setContextPath("/");

		// 4. Defining the context sub path the servlet will
		// answer (in this case: http://<address>/upload
		this.addServlet(servletHolder, "./robotSaveConfirmation/*");

		// 5. Set from where the files must be fetch (if any)
		this.setResourceBase(resourseFolder.toExternalForm());

		// 6. Set the welcome to display if any
		//this.setWelcomeFiles(new String[] { "robotSaveConfirmation.html" });

		// 7. Binds the servletHandler with the ServletContextHandler
		this.insertHandler(servletHandler);
	}
}
