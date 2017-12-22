package jetty.server;

import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import jetty.contexts.MainPageContextHandler;
import jetty.contexts.RobotSaveConfirmationContextHandler;
import jetty.contexts.RobotSubmissionFormContextHandler;

public class HttpServer {

	public HttpServer() {
		try {
			Server server = new Server(8080);

			URL templateFolder = this.getClass().getResource("../htmlTemplates");

			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.addHandler(new MainPageContextHandler(server, templateFolder));
			contexts.addHandler(new RobotSubmissionFormContextHandler(server, templateFolder));
			contexts.addHandler(new RobotSaveConfirmationContextHandler(server, templateFolder));

			server.setHandler(contexts);
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}