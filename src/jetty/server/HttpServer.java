package jetty.server;

import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import jetty.contexts.MainPageContextHandler;
import jetty.contexts.SaveRobotContextHandler;

public class HttpServer {

	public HttpServer() {
		try {
			Server server = new Server(8080);

			URL pagesUrl = this.getClass().getResource("../htmlTemplates");
			URL uploadUrl = this.getClass().getResource("../htmlTemplates");

			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.addHandler(new MainPageContextHandler(server, pagesUrl));
			contexts.addHandler(new SaveRobotContextHandler(server, uploadUrl));

			server.setHandler(contexts);
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}