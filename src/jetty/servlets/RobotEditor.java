package jetty.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
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

import model.Author;
import model.Opponent;
import utils.database.Database;
import utils.text.TextTemplate;

public class RobotEditor extends HttpServlet {

	private static final long serialVersionUID = 4848049705802230040L;
	private TextTemplate template;
	private List<String> errors = new ArrayList<String>();

	public RobotEditor() throws IOException, URISyntaxException {
		template = new TextTemplate(getClass().getResource("../htmlTemplates/robotEditor.html").toURI());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		MultipartConfigElement multipartConfig = new MultipartConfigElement("/tmp");
		req.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfig);

		resp.setContentType("text/plain");
		PrintWriter out = resp.getWriter();

		Opponent o = this.buildOpponentObject(req);

		// If something goes wrong show the erros and stop
		if (o == null) {
			template.assign("message", String.join("<br>", this.errors));
			template.parse("main");
			out.write(template.out("main"));
			return;
		}

		this.saveOpponent(o);
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

	/**
	 * Shows robot edition screen
	 * 
	 * @param resp
	 * @param userId
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private void showRobotEditionScreen(HttpServletResponse resp, String userId)
			throws IOException, URISyntaxException {

		Opponent opponent = this.loadOpponent(userId);

		/*
		 * JSP is a better tecnology than this,but looks like jetty does not gives
		 * support to it so i have to use this workaround
		 */
		template.assign("teamName", opponent.getTeam());
		template.assign("teamWarcry", opponent.getBattleCry());
		template.assign("robotName", opponent.getName());

		opponent.getAuthors().forEach(author -> {
			template.assign("name", author.getName());
			template.assign("email", author.getMail());
			template.parse("author");
		});

		template.parse("main");

		// Set response content type
		resp.setContentType("text/html");

		// Get the writer and write
		resp.getWriter().write(template.out("main"));
	}

	/**
	 * Builds the robot based on the posted data Updates the errors list if any
	 * error occurs
	 * 
	 * @see errors
	 * @param req
	 * @return
	 */
	private Opponent buildOpponentObject(HttpServletRequest req) {

		this.errors.clear();

		// Retrieving data
		String user = req.getParameter("user");
		String password = req.getParameter("password");

		String teamName = req.getParameter("teamName");

		String teamWarcry = req.getParameter("teamWarcry");

		String[] arrName = req.getParameterValues("name");
		String[] arrEmail = req.getParameterValues("email");

		String robotName = req.getParameter("robotName");

		// Validating data
		Part robotJarfilePart;
		byte[] robotJarBuffer = null;
		try {
			robotJarfilePart = req.getPart("robotJarFile");
			// String fileName =
			// Paths.get(robotJarfilePart.getSubmittedFileName()).getFileName().toString();
			InputStream fileContent = robotJarfilePart.getInputStream();
			robotJarBuffer = new byte[fileContent.available()];

			if (robotJarBuffer.length == 0) {
				this.errors.add("Arquivo .jar inválido!");
			}
			// fileContent.read(buffer);
		} catch (IOException | ServletException e) {
			e.printStackTrace();
		}

		Part robotJavafilePart;
		byte[] robotJavaBuffer = null;
		try {
			robotJavafilePart = req.getPart("teamMusic");
			// String fileName =
			// Paths.get(robotJavafilePart.getSubmittedFileName()).getFileName().toString();
			InputStream fileContent = robotJavafilePart.getInputStream();
			robotJavaBuffer = new byte[fileContent.available()];

			if (robotJavaBuffer.length == 0) {
				this.errors.add("Arquivo .java inválido!");
			}
			// fileContent.read(buffer);
		} catch (IOException | ServletException e) {
			e.printStackTrace();
		}

		Part teamMusicfilePart;
		byte[] teamMusicBuffer = null;
		try {
			teamMusicfilePart = req.getPart("teamMusic");
			// String fileName =
			// Paths.get(teamMusicfilePart.getSubmittedFileName()).getFileName().toString();
			InputStream fileContent = teamMusicfilePart.getInputStream();
			teamMusicBuffer = new byte[fileContent.available()];

			if (teamMusicBuffer.length == 0) {
				this.errors.add("Arquivo música inválido!");
			}
			// fileContent.read(buffer);
		} catch (IOException | ServletException e) {
			e.printStackTrace();
		}

		Part teamLogofilePart;
		byte[] teamLogoBuffer = null;
		try {
			teamLogofilePart = req.getPart("teamMusic");
			// String fileName =
			// Paths.get(teamLogofilePart.getSubmittedFileName()).getFileName().toString();
			InputStream fileContent = teamLogofilePart.getInputStream();
			teamLogoBuffer = new byte[fileContent.available()];

			if (teamLogoBuffer.length == 0) {
				this.errors.add("Arquivo logotipo inválido!");
			}
			// fileContent.read(buffer);
		} catch (IOException | ServletException e) {
			e.printStackTrace();
		}

		if (user == null || user.trim().length() == 0) {
			this.errors.add("Preencha o usuário");
		}

		if (password == null || password.trim().length() == 0) {
			this.errors.add("Preencha a senha");
		}

		if (robotName == null || robotName.trim().length() == 0) {
			this.errors.add("Informe o nome do seu robô");
		}

		if (teamName == null || teamName.trim().length() == 0) {
			this.errors.add("Informe o nome do seu time");
		}

		if (teamWarcry == null || teamWarcry.trim().length() == 0) {
			this.errors.add("Informe seu lema");
		}

		for (String mail : arrEmail) {
			if (mail == null || mail.trim().length() == 0) {
				this.errors.add("Informe e-mails válidos");
			}
		}

		for (String name : arrName) {
			if (name == null || name.trim().length() == 0) {
				this.errors.add("Informe nomes válidos");
			}
		}

		if (arrEmail.length != arrName.length) {
			this.errors.add("Cada membro do time deve ter um e-mail");
		}

		// If something is wrong returns null
		if (this.errors.size() > 0) {
			return null;
		}

		// Nothing is wrong build the objects!!

		// Create and store authors
		List<Author> arrAuthors = new ArrayList<Author>();
		for (int i = 0; i < arrName.length; i++) {

			String name = arrName[i];
			String mail = arrEmail[i];

			Author a = new Author();
			a.setName(name);
			a.setMail(mail);

			arrAuthors.add(a);
		}

		// Create the robot!!!
		String userId = req.getSession().getAttribute("userId").toString();
		Opponent robot = this.loadOpponent(userId);

		robot.setAuthors(arrAuthors);
		robot.setBattleCry(teamWarcry);
		robot.setCompiledRobot(robotJarBuffer);
		robot.setImage(teamLogoBuffer);
		robot.setMusic(teamMusicBuffer);
		robot.setName(robotName);
		robot.setSourceCode(new String(robotJavaBuffer));
		robot.setTeam(teamName);

		return robot;
	}

	/**
	 * Loads robot from database
	 * 
	 * @param userId
	 * @return
	 */
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

	/**
	 * Saves robot to database
	 * 
	 * @param o
	 */
	private void saveOpponent(Opponent o) {

		EntityManager entityManager = Database.getEntityManager();
		entityManager.getTransaction().begin();
		entityManager.persist(o);

		// Saves or update depending if it already exists or not
		if (o.getId() == 0) {
			entityManager.persist(o);
		} else {
			entityManager.merge(o);
		}
		entityManager.getTransaction().commit();
		entityManager.clear();
		entityManager.close();
	}
}
