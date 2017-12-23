package jetty.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import jetty.contexts.MainPageContextHandler;
import jetty.contexts.RobotSaveConfirmationContextHandler;
import jetty.contexts.RobotSubmissionFormContextHandler;

public class HttpServer {

	public HttpServer() {
		try {
			Server server = new Server(8080);

			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.addHandler(new MainPageContextHandler(server));
			contexts.addHandler(new RobotSubmissionFormContextHandler(server));
			contexts.addHandler(new RobotSaveConfirmationContextHandler(server));

			server.setHandler(contexts);
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}