package jetty.contexts;

import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;

public class RobotSubmissionFormContextHandler extends ContextHandler {
	public RobotSubmissionFormContextHandler(Server server) {

		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(false);

		this.setWelcomeFiles(new String[] { "index.html" });
		this.setContextPath("/robotSubmissionForm");
		this.setHandler(resourceHandler);
	}
}
