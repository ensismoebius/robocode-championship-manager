package jetty.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.server.session.SessionHandler;

import jetty.contexts.Main;

public class HttpServer {

	public HttpServer() {
		try {
			Server server = new Server(8080);

			HouseKeeper houseKeeper = new HouseKeeper();
			SessionIdManager idmanager = new DefaultSessionIdManager(server);
			idmanager.setSessionHouseKeeper(houseKeeper);
			server.setSessionIdManager(idmanager);

			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.addHandler(new Main(server, new SessionHandler()));

			server.setHandler(contexts);
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}