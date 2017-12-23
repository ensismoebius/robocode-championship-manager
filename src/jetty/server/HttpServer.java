package jetty.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import jetty.contexts.Main;
import jetty.contexts.RobotEditor;

public class HttpServer {

	public HttpServer() {
		try {
			Server server = new Server(8080);

			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.addHandler(new Main(server));
			contexts.addHandler(new RobotEditor(server));

			server.setHandler(contexts);
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}