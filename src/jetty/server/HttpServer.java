package jetty.server;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import jakarta.servlet.MultipartConfigElement;
import jetty.servlets.Login;
import jetty.servlets.RobotEditor;
import jetty.servlets.Style;
import utils.configuration.Configuration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The web server where the teams register their robots
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class HttpServer {

	/**
	 * Biggest accepted upload, the four files of the robot editor together
	 */
	private static final long MAX_REQUEST_SIZE = 10L * 1024 * 1024;

	private final Server server;

	/**
	 * Starts the server, does not block
	 */
	public HttpServer(int port) throws Exception {
		this.server = new Server(port);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");

		context.addServlet(Login.class, "/");
		context.addServlet(Style.class, "/style.css");
		context.addServlet(Style.class, "/fonts/*");

		ServletHolder robotEditor = context.addServlet(RobotEditor.class, "/robotEditor");
		robotEditor.getRegistration().setMultipartConfig(new MultipartConfigElement(Configuration.getTempDir(), MAX_REQUEST_SIZE, MAX_REQUEST_SIZE, 0));

		this.server.setHandler(context);
		this.server.start();
	}

	/**
	 * The port the server is listening, useful when it was started on port 0
	 */
	public int getPort() {
		return ((ServerConnector) this.server.getConnectors()[0]).getLocalPort();
	}

	public void stop() {
		try {
			this.server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
