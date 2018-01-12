package jetty.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.server.session.SessionHandler;

import jetty.contexts.ServletContextsHandler;

public class HttpServer {

	private Server server;

	public HttpServer() {
		try {
			this.server = new Server(8080);

			this.server.setSessionIdManager(this.generateIdManager(server));

			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.addHandler(new ServletContextsHandler(server, new SessionHandler()));

			this.server.setHandler(contexts);
			this.server.start();
			this.server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			this.server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private SessionIdManager generateIdManager(Server server) {
		HouseKeeper houseKeeper = new HouseKeeper();
		SessionIdManager idmanager = new DefaultSessionIdManager(server);
		idmanager.setSessionHouseKeeper(houseKeeper);
		return idmanager;
	}
}