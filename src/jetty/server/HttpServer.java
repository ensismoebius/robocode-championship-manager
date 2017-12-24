package jetty.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.server.session.SessionHandler;

import jetty.contexts.MainContextHandler;

public class HttpServer {

	public HttpServer() {
		try {
			Server server = new Server(8080);

			server.setSessionIdManager(this.generateIdManager(server));

			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.addHandler(new MainContextHandler(server, new SessionHandler()));

			server.setHandler(contexts);
			server.start();
			server.join();
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