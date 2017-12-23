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

			URL templateMainFolder = this.getClass().getResource("../htmlTemplates/main");
			URL templateSubmissionFolder = this.getClass().getResource("../htmlTemplates/robotSubmissionForm");
			URL templateConfirmationFolder = this.getClass()
					.getResource("../htmlTemplates/robotSaveConfirmationContextHandler");

			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.addHandler(new MainPageContextHandler(server, templateMainFolder));
			contexts.addHandler(new RobotSubmissionFormContextHandler(server, templateSubmissionFolder));
			contexts.addHandler(new RobotSaveConfirmationContextHandler(server, templateConfirmationFolder));

			server.setHandler(contexts);
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}