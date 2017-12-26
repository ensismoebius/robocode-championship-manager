package jetty.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Request;

import model.Opponent;
import utils.database.Database;
import utils.text.TextTemplate;

public class RobotEditor extends HttpServlet {

	private static final long serialVersionUID = 4848049705802230040L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		MultipartConfigElement multipartConfig = new MultipartConfigElement("/tmp");
		req.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfig);

		resp.setContentType("text/plain");
		PrintWriter out = resp.getWriter();

		String user = req.getParameter("user");
		String password = req.getParameter("password");

		String[] arrName = req.getParameterValues("name");
		String[] arrEmail = req.getParameterValues("email");

		String robotName = req.getParameter("robotName");
		Part filePart = req.getPart("robotJarFile");
		String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

		InputStream fileContent = filePart.getInputStream();

		byte[] buffer = new byte[fileContent.available()];
		fileContent.read(buffer);

		// TODO save all in database
		// File targetFile = new File("/tmp/");
		// OutputStream outStream = new FileOutputStream(targetFile);
		// outStream.write(buffer);

		// for (Part part : req.getParts()) {

		// out.printf(req.getParameter("password"));

		// for (String header : part.getHeaderNames()) {
		// String headerName = part.getHeader(header) == null ? "" :
		// part.getHeader(header);
		// }

		// out.printf("Got part: name=%s, size=%d%n", part.getName(), part.getSize());
		// part.write(String.format("part-%02d.dat", i++));
		// }
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			HttpSession session = req.getSession();
			this.showRobotEditionScreen(resp, session.getAttribute("userId").toString());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	private void showRobotEditionScreen(HttpServletResponse resp, String userId)
			throws IOException, URISyntaxException {

		List<String> authorNames = new ArrayList<String>();
		List<String> authorMails = new ArrayList<String>();

		Opponent opponent = this.loadOpponent(userId);
		opponent.getAuthors().forEach(author -> {
			authorNames.add(author.getName());
			authorNames.add(author.getMail());
		});

		/*
		 * JSP is a better tecnology than this,but looks like jetty does not gives
		 * support to it so i have to use this workaround
		 */
		TextTemplate template = new TextTemplate(getClass().getResource("../htmlTemplates/robotEditor.html").toURI());
		template.assign("teamName", opponent.getTeam());
		template.assign("teamWarcry", opponent.getBattleCry());
		template.assign("name", authorNames);
		template.assign("email", authorMails);
		template.assign("robotName", opponent.getName());

		template.parse("main");

		// Set response content type
		resp.setContentType("text/html");

		// Get the writer and write
		resp.getWriter().write(template.out("main"));
	}

	private Opponent loadOpponent(String userId) {

		if (userId == null)
			return new Opponent();

		EntityManager entityManager = Database.getEntityManager();

		@SuppressWarnings("unchecked")
		List<Opponent> list = entityManager.createQuery("from Opponent o inner join o.authors a where a.id = :authorId")
				.setParameter("authorId", Long.parseLong(userId)).getResultList();

		Opponent o = list.size() > 0 ? list.get(0) : new Opponent();

		entityManager.clear();
		entityManager.close();

		return o;
	}
}
