package jetty.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Request;

import model.Author;
import model.Opponent;
import utils.database.Database;
import utils.text.TextTemplate;
import utils.web.ServletAutheticator;

public class RobotEditor extends HttpServlet {

	private static final long serialVersionUID = 4848049705802230040L;
	private TextTemplate template;
	private List<String> errors = new ArrayList<String>();
	private ServletAutheticator servletAutheticator;

	public void initialize() {
		try {
			this.servletAutheticator = new ServletAutheticator();
			this.template = new TextTemplate(getClass().getResource("../htmlTemplates/robotEditor.html").toURI());
		} catch (IOException | URISyntaxException e) {
			System.exit(1);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.initialize();

		// This must be executed BEFORE any request reading otherwise all data get
		// missing
		MultipartConfigElement multipartConfig = new MultipartConfigElement("/tmp");
		req.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfig);

		// An action MUST be declared
		if (this.getAction(req) == null || this.getAction(req).equals("")) {
			resp.sendRedirect("/");
		}

		// Set response content type
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();

		try {
			Opponent o = this.buildOpponentObject(req);

			// If something goes wrong show the erros and stop
			if (o == null) {
				template.assign("action", this.getAction(req));
				template.assign("message", String.join("<br>", this.errors));
				template.parse("main");
				out.write(template.out("main"));
				return;
			}

			o = this.saveOpponent(o, this.getAction(req).equals("edit"));
			this.servletAutheticator.authenticate(o.getAuthors().get(0).getLogin(),
					o.getAuthors().get(0).getPassword());
			template.assign("action", "edit");
			template.assign("message", "Robô salvo com sucesso! Para editar entre com o login senha.");

			String userId = String.valueOf(o.getAuthors().get(0).getId());
			this.showRobotEditionScreen(resp, req, userId);

		} catch (Exception e) {
			template.assign("action", "new");
			template.assign("message", "Algum erro inesperado aconteceu: " + e.getMessage());
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		this.initialize();

		try {
			// An action MUST be declared
			if (this.getAction(req) == null || this.getAction(req).equals("")) {
				resp.sendRedirect("/");
			}

			this.servletAutheticator.setRequestObject(req);

			if (this.getAction(req).equals("logout")) {
				this.servletAutheticator.logout();
				resp.sendRedirect("/");
				return;
			}

			// If is editing the team/robot and its not authenticated, goto auth screen
			if (this.getAction(req).equals("edit") && !this.servletAutheticator.isAuthenticated()) {
				resp.sendRedirect("/");
				return;
			}
			// New robot/team with authentication not allowed, redirecting to edition
			if (this.getAction(req).equals("new") && this.servletAutheticator.isAuthenticated()) {
				resp.sendRedirect("/robotEditor?action=edit");
				return;
			}

			// Is editing and authenticated, its fine
			if (this.getAction(req).equals("edit") && this.servletAutheticator.isAuthenticated()) {
				this.template.assign("action", "edit");
				this.showRobotEditionScreen(resp, req, this.servletAutheticator.getId());
				return;
			}

			// New robot/team with no authentication, its fine
			if (this.getAction(req).equals("new") && !this.servletAutheticator.isAuthenticated()) {
				this.template.assign("action", "new");
				this.showRobotEditionScreen(resp, req, this.servletAutheticator.getId());
				return;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getAction(HttpServletRequest req) {
		return req.getParameter("action");
	}

	/**
	 * Shows robot edition screen
	 * 
	 * @param resp
	 * @param userId
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private void showRobotEditionScreen(HttpServletResponse resp, HttpServletRequest req, String userId)
			throws IOException, URISyntaxException {

		Opponent opponent = this.loadOpponent(userId);

		/*
		 * JSP is a better tecnology than this,but looks like jetty does not gives
		 * support to it so i have to use this workaround
		 */
		if (opponent.getAuthors().size() > 0) {
			template.assign("login", opponent.getAuthors().get(0).getLogin());
			template.assign("password", opponent.getAuthors().get(0).getPassword());
		}
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
	 * @throws Exception
	 */
	private Opponent buildOpponentObject(HttpServletRequest req) throws Exception {

		this.errors.clear();

		// Retrieving data
		String login = req.getParameter("login");
		String password = req.getParameter("password");

		String teamWarcry = req.getParameter("teamWarcry");

		String[] arrName = req.getParameterValues("name[]");
		String[] arrEmail = req.getParameterValues("email[]");

		// Holds the file name which determines The team and robot name
		String jarFileName = null;

		// Validating data
		Part robotJarfilePart;
		byte[] robotJarBuffer = null;
		try {
			robotJarfilePart = req.getPart("robotJarFile");
			jarFileName = Paths.get(robotJarfilePart.getSubmittedFileName()).getFileName().toString();
			InputStream fileContent = robotJarfilePart.getInputStream();
			robotJarBuffer = new byte[fileContent.available()];
			fileContent.read(robotJarBuffer);

			if (robotJarBuffer.length == 0) {
				this.errors.add("Arquivo .jar inválido!");
			}
		} catch (IOException | ServletException e) {
			e.printStackTrace();
		}

		Part robotJavafilePart;
		byte[] robotJavaBuffer = null;
		try {
			robotJavafilePart = req.getPart("robotJavaFile");
			// String fileName =
			// Paths.get(robotJavafilePart.getSubmittedFileName()).getFileName().toString();
			InputStream fileContent = robotJavafilePart.getInputStream();
			robotJavaBuffer = new byte[fileContent.available()];
			fileContent.read(robotJavaBuffer);

			if (robotJavaBuffer.length == 0) {
				this.errors.add("Arquivo .java inválido!");
			}
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
			fileContent.read(teamMusicBuffer);

			if (teamMusicBuffer.length == 0) {
				this.errors.add("Arquivo música inválido!");
			}

			if (teamMusicBuffer.length > 1024 * 1024) {
				this.errors.add("Arquivo música muito grande!");
			}
		} catch (IOException | ServletException e) {
			e.printStackTrace();
		}

		Part teamLogofilePart;
		byte[] teamLogoBuffer = null;
		try {
			teamLogofilePart = req.getPart("teamLogo");
			// String fileName =
			// Paths.get(teamLogofilePart.getSubmittedFileName()).getFileName().toString();
			InputStream fileContent = teamLogofilePart.getInputStream();
			teamLogoBuffer = new byte[fileContent.available()];
			fileContent.read(teamLogoBuffer);

			if (teamLogoBuffer.length == 0) {
				this.errors.add("Arquivo de logotipo inválido!");
			}

			if (teamLogoBuffer.length > 1024 * 1024) {
				this.errors.add("Arquivo de logotipo muito grande!");
			}
		} catch (IOException | ServletException e) {
			e.printStackTrace();
		}

		if (login == null || login.trim().length() == 0) {
			this.errors.add("Preencha o usuário");
		}

		if (password == null || password.trim().length() == 0) {
			this.errors.add("Preencha a senha");
		}

		Pattern robotTeamAndName = Pattern.compile("^([^\\.]*)\\.([^\\_]*).*(\\.jar)$");
		Matcher robotTeamAndNameMatcher = robotTeamAndName.matcher(jarFileName);

		if (!robotTeamAndNameMatcher.find()) {
			this.errors.add(
					"O nome do arquivo .jar não está no formato correto! Use o empacotador do robocode para gerá-lo!");
		}

		String teamName = robotTeamAndNameMatcher.group(1);
		String robotName = robotTeamAndNameMatcher.group(2);

		// The team and robot names must have only numbers and/or alphabetical chars
		Pattern alowedChars = Pattern.compile("[^a-zA-Z0-9_\\.]{1,50}", Pattern.CASE_INSENSITIVE);

		Matcher m = alowedChars.matcher(robotName);
		if (m.find()) {
			this.errors.add("O nome do robô pode ter APENAS caracteres alfabéticos e numéricos!");
		}

		m = alowedChars.matcher(teamName);
		if (m.find()) {
			this.errors.add("O nome do time pode ter APENAS caracteres alfabéticos e numéricos!");
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

		// If we want create a new team and the login existis cancel and call for
		// another login
		if (this.getAction(req).equals("new") && this.loginExistis(login)) {
			this.errors.add("Este login já existe, escolha outro. Geralmente um e-mail é uma boa escolha.");
		}

		// If something is wrong returns null
		if (this.errors.size() > 0) {
			return null;
		}

		// Nothing is wrong build the objects!!

		// Create the robot!!!
		this.servletAutheticator.setRequestObject(req);
		this.servletAutheticator.authenticate(login, password);
		Opponent robot = this.loadOpponent(this.servletAutheticator.getId());

		// Create and store authors
		List<Author> arrAuthors = new ArrayList<Author>();
		for (int i = 0; i < arrName.length; i++) {

			boolean updateAuthor = false;

			String name = arrName[i];
			String mail = arrEmail[i];

			Author a = new Author();

			for (Author author : robot.getAuthors()) {

				// If there is an existing author updates it
				if (author.getMail().equals(mail)) {
					a.setId(author.getId());

					a.setLogin(login);
					a.setPassword(password);

					a.setName(name);
					a.setMail(mail);
					a.setObservations("");
					updateAuthor = true;
					break;
				}
			}

			// Adds the updated author
			if (updateAuthor) {
				arrAuthors.add(a);
				continue;
			}

			// Adds new author
			a.setName(name);
			a.setMail(mail);
			a.setLogin(login);
			a.setPassword(password);
			a.setObservations("");
			arrAuthors.add(a);
		}

		if (robot.getLevels().size() == 0) {
			robot.getLevels().add(0);
		}
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

	private boolean loginExistis(String login) throws Exception {

		EntityManager entityManager = Database.getEntityManager();

		try {
			@SuppressWarnings("unchecked")
			List<Author> list = entityManager.createQuery("from Author a where a.login = :login")
					.setParameter("login", login).getResultList();

			if (list.size() > 0) {
				entityManager.clear();
				entityManager.close();
				return true;
			}

		} catch (PersistenceException e) {
			entityManager.clear();
			entityManager.close();
			return false;
		}
		entityManager.clear();
		entityManager.close();
		return false;
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
		List<Object> list = entityManager.createQuery("from Opponent o inner join o.authors a where a.id = :authorId")
				.setParameter("authorId", Long.parseLong(userId)).getResultList();

		Object[] result = (Object[]) list.get(0);
		Opponent o = list.size() > 0 ? (Opponent) result[0] : new Opponent();

		entityManager.clear();
		entityManager.close();

		return o;
	}

	/**
	 * Saves robot to database
	 * 
	 * @param o
	 */
	private Opponent saveOpponent(Opponent o, boolean editing) {

		EntityManager entityManager = Database.getEntityManager();
		entityManager.getTransaction().begin();

		// Saves or update depending if it already exists or not
		if (editing) {
			o = entityManager.merge(o);
		} else {
			entityManager.persist(o);
		}
		entityManager.getTransaction().commit();
		entityManager.clear();
		entityManager.close();

		return o;
	}
}
