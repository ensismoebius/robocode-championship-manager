package jetty.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jetty.servlets.RobotUploads.Upload;
import model.Author;
import model.Opponent;
import utils.configuration.Configuration;
import utils.database.Database;
import utils.text.TextTemplate;
import utils.web.Html;
import utils.web.PasswordHasher;
import utils.web.ServletAuthenticator;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Registration and edition of the teams and their robots. Servlets are shared
 * between requests so this class must not keep per request state.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RobotEditor extends HttpServlet {

	private static final long serialVersionUID = 4848049705802230040L;

	private static final int MAX_NAME_LENGTH = 100;
	private static final int MAX_REGISTRATION_LENGTH = 20;

	private static final String ACTION_NEW = "new";
	private static final String ACTION_EDIT = "edit";
	private static final String ACTION_LOGOUT = "logout";

	/**
	 * Robocode packager generates files like team.RobotName_1.0.jar
	 */
	private static final Pattern JAR_FILE_NAME = Pattern.compile("^([^.]+)\\.(.+?)_[0-9][0-9.]*\\.jar$");
	private static final Pattern TEAM_NAME = Pattern.compile("[A-Za-z0-9_]{1," + MAX_NAME_LENGTH + "}");
	private static final Pattern ROBOT_NAME = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*");
	private static final Pattern REGISTRATION = Pattern.compile("[0-9]{1," + MAX_REGISTRATION_LENGTH + "}");

	/** Shows the robot editor for the logged author: the form of a new robot or of the robot being edited. */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		String action = this.getAction(req);
		Long opponentId = ServletAuthenticator.getOpponentId(req);

		if (ACTION_LOGOUT.equals(action)) {
			ServletAuthenticator.logout(req);
			resp.sendRedirect("/");
			return;
		}

		if (ACTION_EDIT.equals(action)) {
			// If is editing the team/robot and its not authenticated, goto auth screen
			Opponent opponent = opponentId == null ? null : this.loadOpponent(opponentId);
			if (opponent == null) {
				ServletAuthenticator.logout(req);
				resp.sendRedirect("/");
				return;
			}
			String message = req.getParameter("saved") != null ? "Robô salvo com sucesso!" : "";
			this.showRobotEditionScreen(resp, ACTION_EDIT, opponent, message);
			return;
		}

		if (ACTION_NEW.equals(action)) {
			// New robot/team with authentication not allowed, redirecting to edition
			if (opponentId != null) {
				resp.sendRedirect("/robotEditor?action=edit");
				return;
			}
			this.showRobotEditionScreen(resp, ACTION_NEW, new Opponent(), "");
			return;
		}

		// An action MUST be declared
		resp.sendRedirect("/");
	}

	/** Handles the robot editor form: saves the robot, or shows the errors that were found. */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");

		String action = this.getAction(req);
		Long opponentId = ServletAuthenticator.getOpponentId(req);

		// An action MUST be declared
		if (!ACTION_NEW.equals(action) && !ACTION_EDIT.equals(action)) {
			resp.sendRedirect("/");
			return;
		}

		// Logged in teams can only edit their own robot
		if (opponentId != null) {
			action = ACTION_EDIT;
		} else if (ACTION_EDIT.equals(action)) {
			resp.sendRedirect("/");
			return;
		}

		Opponent robot = opponentId == null ? new Opponent() : this.loadOpponent(opponentId);
		if (robot == null) {
			ServletAuthenticator.logout(req);
			resp.sendRedirect("/");
			return;
		}

		// Blocks new robots registration if it is set in the configurations
		if (!Configuration.isEnabledRegistration()) {
			this.showRobotEditionScreen(resp, action, robot, "Cadastro de novos robôs bloqueado!");
			return;
		}

		this.saveRobot(req, resp, action, robot);
	}

	/** Shows a row for each author of the robot, or an empty row when there is none. */
	private static void assignAuthors(TextTemplate template, Opponent opponent) {
		// Always show at least one author
		List<Author> authors = opponent.getAuthors().isEmpty() ? List.of(new Author()) : opponent.getAuthors();
		for (Author author : authors) {
			template.assign("name", Html.escape(author.getName()));
			template.assign("email", Html.escape(author.getMail()));
			template.assign("registration", Html.escape(author.getRegistration()));
			template.parse("author");
		}
	}

	/** The submit button: disabled, with the message, while the registration of new robots is blocked. */
	private static void assignSubmitButton(TextTemplate template, String message) {
		if (Configuration.isEnabledRegistration()) {
			template.assign("submitButton", "Salvar");
			template.assign("submitDisabled", "");
		} else {
			template.assign("submitButton", "Cadastro de novos robôs bloqueado!!");
			template.assign("submitDisabled", "disabled");
			if (message.isEmpty()) {
				template.assign("message", "Cadastro de novos robôs bloqueado!!");
			}
		}
	}

	/**
	 * Fills the robot from the form and saves it. When something is wrong, shows the errors on the
	 * editing screen; when it is saved, logs the author in and redirects to the editing screen.
	 */
	private void saveRobot(HttpServletRequest req, HttpServletResponse resp, String action, Opponent robot) throws ServletException, IOException {
		try {
			List<String> errors = this.fillOpponent(req, robot);

			// If something goes wrong show the errors and stop
			if (!errors.isEmpty()) {
				this.showRobotEditionScreen(resp, action, robot, String.join("<br>", errors.stream().map(Html::escape).toList()));
				return;
			}

			Opponent saved = this.saveOpponent(robot);
			ServletAuthenticator.login(req, saved.getId());

			// Post/Redirect/Get, so reloading the page does not post again
			resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
			resp.setHeader("Location", "/robotEditor?action=edit&saved=1");

		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			this.showRobotEditionScreen(resp, action, robot, Html.escape("Algum erro inesperado aconteceu: " + runtimeException.getMessage()));
		}
	}

	/** The action asked by the form, or an empty text when there is none. */
	private String getAction(HttpServletRequest req) {
		String action = req.getParameter("action");
		return action == null ? "" : action;
	}

	/**
	 * Shows robot edition screen
	 *
	 * @param message
	 *            already escaped HTML
	 */
	private void showRobotEditionScreen(HttpServletResponse resp, String action, Opponent opponent, String message) throws IOException {

		/*
		 * JSP is a better tecnology than this,but looks like jetty does not gives
		 * support to it so i have to use this workaround
		 */
		TextTemplate template = new TextTemplate(getClass().getResourceAsStream("/jetty/htmlTemplates/robotEditor.html"));

		boolean editing = ACTION_EDIT.equals(action);

		template.assign("action", action);
		template.assign("message", message);
		template.assign("login", Html.escape(opponent.getAuthors().isEmpty() ? "" : opponent.getAuthors().get(0).getLogin()));
		template.assign("teamWarcry", Html.escape(opponent.getBattleCry()));

		// When editing the password and files are kept if not informed
		template.assign("passwordRequired", editing ? "" : "required");
		template.assign("passwordPlaceholder", editing ? "Nova senha (deixe em branco para manter)" : "Senha");
		template.assign("fileRequired", editing ? "" : "required");
		template.assign("robotInfo", editing && opponent.getTeam() != null
				? Html.escape("Robô cadastrado: " + opponent.getFullName() + ". Envie novos arquivos apenas se quiser substituí-los.")
				: "");

		assignAuthors(template, opponent);
		assignSubmitButton(template, message);

		template.parse("main");

		// The registration status changes at any time, a cached page would lie about it
		resp.setHeader("Cache-Control", "no-store");
		resp.setContentType("text/html; charset=UTF-8");
		resp.getWriter().write(template.out("main"));
	}

	/**
	 * Checks the files sent with the form: the robot and the team files are required on a new robot, and the
	 * names of the robot and the team come from the .jar file name, when it is sent
	 */
	private Names checkUploads(List<String> errors, boolean isNew, Opponent robot, Upload robotJar, Upload robotJava, Upload teamMusic, Upload teamLogo) throws IOException {
		RobotUploads.requireFiles(errors, isNew, robotJar, robotJava, teamMusic, teamLogo);
		Names names = checkJar(errors, robotJar, robot);
		RobotUploads.checkJava(errors, robotJava);
		RobotUploads.checkMusic(errors, teamMusic);
		RobotUploads.checkLogo(errors, teamLogo);
		return names;
	}

	/**
	 * Fills the robot with the posted data. When editing, the files and password
	 * that were not sent are kept.
	 *
	 * @return the validation errors, empty if everything is fine
	 */
	private List<String> fillOpponent(HttpServletRequest req, Opponent robot) throws IOException, ServletException {

		List<String> errors = new ArrayList<String>();
		boolean isNew = robot.getId() == 0;

		// Retrieving data
		String login = trim(req.getParameter("login"));
		String password = req.getParameter("password");
		String teamWarcry = trim(req.getParameter("teamWarcry"));

		String[] arrName = req.getParameterValues("name[]");
		String[] arrEmail = req.getParameterValues("email[]");
		String[] arrRegistration = req.getParameterValues("registration[]");

		Upload robotJar = RobotUploads.read(req, "robotJarFile");
		Upload robotJava = RobotUploads.read(req, "robotJavaFile");
		Upload teamMusic = RobotUploads.read(req, "teamMusic");
		Upload teamLogo = RobotUploads.read(req, "teamLogo");

		Names names = this.checkUploads(errors, isNew, robot, robotJar, robotJava, teamMusic, teamLogo);

		boolean keepPassword = password == null || password.isBlank();
		checkAccess(errors, login, keepPassword, robot);
		checkWarcry(errors, teamWarcry);
		String[][] authorsForm = authorsForm(errors, arrName, arrEmail, arrRegistration);

		// Kept so the form shows it again if something is wrong
		robot.setBattleCry(teamWarcry);

		if (!errors.isEmpty()) {
			return errors;
		}

		// Nothing is wrong, fill the robot!!
		fillIdentity(robot, names, authorsForm, login, password, keepPassword);
		RobotUploads.copyUploads(robot, robotJar, robotJava, teamMusic, teamLogo);

		return errors;
	}

	/** Fills the team, the robot name and the authors; the password is kept when the form did not send a new one. */
	private static void fillIdentity(Opponent robot, Names names, String[][] authorsForm, String login, String password, boolean keepPassword) {
		String passwordHash = keepPassword ? robot.getAuthors().get(0).getPassword() : PasswordHasher.hash(password);
		fillAuthors(robot, authorsForm[0], authorsForm[1], authorsForm[2], login, passwordHash);

		robot.setTeam(names == null ? robot.getTeam() : names.team());
		robot.setName(names == null ? robot.getName() : names.robot());
	}

	/** The team and robot names read from the .jar file name */
	private record Names(String team, String robot) {
	}

	/**
	 * @return the names in the .jar file name, or null if there is no valid .jar
	 *         (the robot keeps its names then)
	 */
	private Names checkJar(List<String> errors, Upload robotJar, Opponent robot) {
		if (robotJar == null) {
			return null;
		}
		if (robotJar.content().length == 0) {
			errors.add("Arquivo .jar inválido!");
		}

		Matcher matcher = JAR_FILE_NAME.matcher(robotJar.fileName());
		if (!matcher.matches()) {
			errors.add("O nome do arquivo .jar não está no formato correto! Use o empacotador do robocode para gerá-lo!");
			return null;
		}
		Names names = new Names(matcher.group(1), matcher.group(2));

		// The team and robot names must have only numbers and/or alphabetical chars
		if (!TEAM_NAME.matcher(names.team()).matches()) {
			errors.add("O nome do time pode ter APENAS caracteres alfabéticos e numéricos!");
		}
		if (!ROBOT_NAME.matcher(names.robot()).matches() || names.robot().length() > MAX_NAME_LENGTH) {
			errors.add("O nome do robô pode ter APENAS caracteres alfabéticos e numéricos!");
		}
		if (errors.isEmpty() && this.robotNameTaken(names.team(), names.robot(), robot.getId())) {
			errors.add("Já existe um robô chamado " + names.team() + "." + names.robot() + ", escolha outro nome.");
		}
		return names;
	}

	/** Login, and the password (kept when editing and not sent) */
	private void checkAccess(List<String> errors, String login, boolean keepPassword, Opponent robot) {
		if (login.isEmpty()) {
			errors.add("Preencha o usuário");
		} else if (this.loginTaken(login, robot.getId())) {
			// Each team must have its own login
			errors.add("Este login já existe, escolha outro. Geralmente um e-mail é uma boa escolha.");
		}

		if (keepPassword && (robot.getId() == 0 || robot.getAuthors().isEmpty())) {
			errors.add("Preencha a senha");
		}
	}

	/** Adds an error when the battle cry is empty or too long. */
	private static void checkWarcry(List<String> errors, String teamWarcry) {
		if (teamWarcry.isEmpty()) {
			errors.add("Informe seu lema");
		} else if (teamWarcry.length() > Opponent.MAX_BATTLE_CRY_LENGTH) {
			errors.add("O lema pode ter no máximo " + Opponent.MAX_BATTLE_CRY_LENGTH + " caracteres");
		}
	}

	/**
	 * Checks the developers of the form
	 *
	 * @return the names and e-mails to save, both empty if the form has none
	 */
	private static String[][] authorsForm(List<String> errors, String[] arrName, String[] arrEmail, String[] arrRegistration) {
		if (arrName == null || arrEmail == null || arrRegistration == null || arrName.length == 0) {
			errors.add("Informe ao menos uma pessoa desenvolvedora");
			return new String[][] { new String[0], new String[0], new String[0] };
		}
		if (arrEmail.length != arrName.length || arrRegistration.length != arrName.length) {
			errors.add("Cada membro do time deve ter um e-mail e um RM");
		}

		for (String registration : arrRegistration) {
			if (registration == null || !REGISTRATION.matcher(registration.trim()).matches()) {
				errors.add("Informe RMs válidos, apenas com números e até " + MAX_REGISTRATION_LENGTH + " dígitos");
				break;
			}
		}

		for (String mail : arrEmail) {
			if (mail == null || !mail.trim().matches("[^@\\s]+@[^@\\s]+")) {
				errors.add("Informe e-mails válidos");
				break;
			}
		}

		for (String name : arrName) {
			if (name == null || name.trim().isEmpty()) {
				errors.add("Informe nomes válidos");
				break;
			}
		}
		return new String[][] { arrName, arrEmail, arrRegistration };
	}

	/**
	 * Updates existing authors (matched by e-mail), adds the new ones and drops the
	 * ones that were removed from the form
	 */
	private static void fillAuthors(Opponent robot, String[] arrName, String[] arrEmail, String[] arrRegistration, String login, String passwordHash) {
		List<Author> authors = new ArrayList<Author>();
		for (int index = 0; index < arrName.length; index++) {
			String mail = arrEmail[index].trim();

			Author author = robot.getAuthors().stream().filter(first -> first.getMail().equals(mail)).findFirst().orElseGet(Author::new);
			author.setName(arrName[index].trim());
			author.setMail(mail);
			author.setRegistration(arrRegistration[index].trim());
			author.setLogin(login);
			author.setPassword(passwordHash);
			if (!authors.contains(author)) {
				authors.add(author);
			}
		}
		robot.getAuthors().clear();
		robot.getAuthors().addAll(authors);
	}

	/** The text without the spaces around it, or an empty text when it is null. */
	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	/**
	 * @return true if another team already uses this login
	 */
	private boolean loginTaken(String login, long opponentId) {
		List<Long> owners = Database.read(entityManager -> entityManager.createQuery("select o.id from Opponent o join o.authors a where a.login = :login", Long.class)
				.setParameter("login", login).getResultList());
		return owners.stream().anyMatch(identifier -> identifier != opponentId);
	}

	/**
	 * @return true if another team already has a robot with this name
	 */
	private boolean robotNameTaken(String team, String name, long opponentId) {
		List<Long> owners = Database.read(entityManager -> entityManager.createQuery("select o.id from Opponent o where o.team = :team and o.name = :name", Long.class)
				.setParameter("team", team).setParameter("name", name).getResultList());
		return owners.stream().anyMatch(identifier -> identifier != opponentId);
	}

	/**
	 * Loads robot from database
	 *
	 * @return the robot or null if it does not exist
	 */
	private Opponent loadOpponent(long opponentId) {
		return Database.read(entityManager -> entityManager.find(Opponent.class, opponentId));
	}

	/**
	 * Saves robot to database
	 */
	private Opponent saveOpponent(Opponent opponent) {
		return Database.inTransaction(entityManager -> {
			// Saves or update depending if it already exists or not
			if (opponent.getId() == 0) {
				entityManager.persist(opponent);
				return opponent;
			}
			return entityManager.merge(opponent);
		});
	}
}
