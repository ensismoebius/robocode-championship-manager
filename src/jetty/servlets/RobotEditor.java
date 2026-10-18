package jetty.servlets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import model.Author;
import model.Opponent;
import utils.configuration.Configuration;
import utils.database.Database;
import utils.text.TextTemplate;
import utils.web.Html;
import utils.web.PasswordHasher;
import utils.web.ServletAuthenticator;
import widgets.Sound;

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

	private static final int MAX_FILE_SIZE = 1024 * 1024;
	private static final int MAX_LOGO_WIDTH = 640;
	private static final int MAX_LOGO_HEIGHT = 480;
	private static final int MAX_NAME_LENGTH = 100;

	private static final String ACTION_NEW = "new";
	private static final String ACTION_EDIT = "edit";
	private static final String ACTION_LOGOUT = "logout";

	/**
	 * Robocode packager generates files like team.RobotName_1.0.jar
	 */
	private static final Pattern JAR_FILE_NAME = Pattern.compile("^([^.]+)\\.(.+?)_[0-9][0-9.]*\\.jar$");
	private static final Pattern TEAM_NAME = Pattern.compile("[A-Za-z0-9_]{1," + MAX_NAME_LENGTH + "}");
	private static final Pattern ROBOT_NAME = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*");

	/**
	 * An uploaded file
	 */
	private record Upload(String fileName, byte[] content) {
	}

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

		} catch (RuntimeException e) {
			e.printStackTrace();
			this.showRobotEditionScreen(resp, action, robot, Html.escape("Algum erro inesperado aconteceu: " + e.getMessage()));
		}
	}

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

		// Always show at least one author
		List<Author> authors = opponent.getAuthors().isEmpty() ? List.of(new Author()) : opponent.getAuthors();
		for (Author author : authors) {
			template.assign("name", Html.escape(author.getName()));
			template.assign("email", Html.escape(author.getMail()));
			template.parse("author");
		}

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

		template.parse("main");

		resp.setContentType("text/html; charset=UTF-8");
		resp.getWriter().write(template.out("main"));
	}

	/**
	 * Reads an uploaded file
	 *
	 * @return the file or null if no file was sent
	 */
	private Upload readUpload(HttpServletRequest req, String name) throws IOException, ServletException {
		Part part = req.getPart(name);
		if (part == null || part.getSubmittedFileName() == null || part.getSubmittedFileName().isEmpty()) {
			return null;
		}

		String fileName = Paths.get(part.getSubmittedFileName()).getFileName().toString();
		try (InputStream content = part.getInputStream()) {
			return new Upload(fileName, content.readAllBytes());
		}
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

		Upload robotJar = this.readUpload(req, "robotJarFile");
		Upload robotJava = this.readUpload(req, "robotJavaFile");
		Upload teamMusic = this.readUpload(req, "teamMusic");
		Upload teamLogo = this.readUpload(req, "teamLogo");

		// Validating the files, all of them are needed for new robots
		if (robotJar == null && isNew) {
			errors.add("Envie o arquivo .jar do robô!");
		}
		if (robotJava == null && isNew) {
			errors.add("Envie o arquivo .java do robô!");
		}
		if (teamMusic == null && isNew) {
			errors.add("Envie a música do time!");
		}
		if (teamLogo == null && isNew) {
			errors.add("Envie o logotipo do time!");
		}

		String teamName = robot.getTeam();
		String robotName = robot.getName();

		if (robotJar != null) {
			if (robotJar.content().length == 0) {
				errors.add("Arquivo .jar inválido!");
			}

			// The team and robot names come from the .jar file name
			Matcher matcher = JAR_FILE_NAME.matcher(robotJar.fileName());
			if (!matcher.matches()) {
				errors.add("O nome do arquivo .jar não está no formato correto! Use o empacotador do robocode para gerá-lo!");
			} else {
				teamName = matcher.group(1);
				robotName = matcher.group(2);

				// The team and robot names must have only numbers and/or alphabetical chars
				if (!TEAM_NAME.matcher(teamName).matches()) {
					errors.add("O nome do time pode ter APENAS caracteres alfabéticos e numéricos!");
				}
				if (!ROBOT_NAME.matcher(robotName).matches() || robotName.length() > MAX_NAME_LENGTH) {
					errors.add("O nome do robô pode ter APENAS caracteres alfabéticos e numéricos!");
				}
				if (errors.isEmpty() && this.robotNameTaken(teamName, robotName, robot.getId())) {
					errors.add("Já existe um robô chamado " + teamName + "." + robotName + ", escolha outro nome.");
				}
			}
		}

		if (robotJava != null && robotJava.content().length == 0) {
			errors.add("Arquivo .java inválido!");
		}
		if (robotJava != null && robotJava.content().length > MAX_FILE_SIZE) {
			errors.add("Arquivo .java muito grande!");
		}

		if (teamMusic != null && teamMusic.content().length == 0) {
			errors.add("Arquivo música inválido!");
		}
		if (teamMusic != null && teamMusic.content().length > MAX_FILE_SIZE) {
			errors.add("Arquivo música muito grande!");
		} else if (teamMusic != null && teamMusic.content().length > 0 && !Sound.isPlayable(teamMusic.content())) {
			errors.add("Não foi possível tocar a música, envie um arquivo .mp3 válido!");
		}

		if (teamLogo != null) {
			if (teamLogo.content().length > MAX_FILE_SIZE) {
				errors.add("Arquivo de logotipo muito grande!");
			}

			BufferedImage logo = ImageIO.read(new ByteArrayInputStream(teamLogo.content()));
			if (logo == null) {
				errors.add("Arquivo de logotipo inválido! Use .gif, .jpg ou .png");
			} else {
				if (logo.getWidth() > MAX_LOGO_WIDTH) {
					errors.add("A largura do logotipo é maior que " + MAX_LOGO_WIDTH + " pixels!");
				}
				if (logo.getHeight() > MAX_LOGO_HEIGHT) {
					errors.add("A altura do logotipo é maior que " + MAX_LOGO_HEIGHT + " pixels!");
				}
			}
		}

		if (login.isEmpty()) {
			errors.add("Preencha o usuário");
		} else if (this.loginTaken(login, robot.getId())) {
			// Each team must have its own login
			errors.add("Este login já existe, escolha outro. Geralmente um e-mail é uma boa escolha.");
		}

		boolean keepPassword = password == null || password.isBlank();
		if (keepPassword && (isNew || robot.getAuthors().isEmpty())) {
			errors.add("Preencha a senha");
		}

		if (teamWarcry.isEmpty()) {
			errors.add("Informe seu lema");
		} else if (teamWarcry.length() > Opponent.MAX_BATTLE_CRY_LENGTH) {
			errors.add("O lema pode ter no máximo " + Opponent.MAX_BATTLE_CRY_LENGTH + " caracteres");
		}

		if (arrName == null || arrEmail == null || arrName.length == 0) {
			errors.add("Informe ao menos uma pessoa desenvolvedora");
			arrName = new String[0];
			arrEmail = new String[0];
		} else if (arrEmail.length != arrName.length) {
			errors.add("Cada membro do time deve ter um e-mail");
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

		// Kept so the form shows it again if something is wrong
		robot.setBattleCry(teamWarcry);

		if (!errors.isEmpty()) {
			return errors;
		}

		// Nothing is wrong, fill the robot!!

		String passwordHash = keepPassword ? robot.getAuthors().get(0).getPassword() : PasswordHasher.hash(password);

		// Updates existing authors (matched by e-mail), adds the new ones and drops
		// the ones that were removed from the form
		List<Author> authors = new ArrayList<Author>();
		for (int i = 0; i < arrName.length; i++) {
			String mail = arrEmail[i].trim();

			Author author = robot.getAuthors().stream().filter(a -> a.getMail().equals(mail)).findFirst().orElseGet(Author::new);
			author.setName(arrName[i].trim());
			author.setMail(mail);
			author.setLogin(login);
			author.setPassword(passwordHash);
			if (!authors.contains(author)) {
				authors.add(author);
			}
		}
		robot.getAuthors().clear();
		robot.getAuthors().addAll(authors);

		robot.setTeam(teamName);
		robot.setName(robotName);

		if (robotJar != null) {
			robot.setCompiledRobot(robotJar.content());
		}
		if (robotJava != null) {
			robot.setSourceCode(new String(robotJava.content(), StandardCharsets.UTF_8));
		}
		if (teamMusic != null) {
			robot.setMusic(teamMusic.content());
		}
		if (teamLogo != null) {
			robot.setImage(teamLogo.content());
		}

		return errors;
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	/**
	 * @return true if another team already uses this login
	 */
	private boolean loginTaken(String login, long opponentId) {
		List<Long> owners = Database.read(em -> em.createQuery("select o.id from Opponent o join o.authors a where a.login = :login", Long.class)
				.setParameter("login", login).getResultList());
		return owners.stream().anyMatch(id -> id != opponentId);
	}

	/**
	 * @return true if another team already has a robot with this name
	 */
	private boolean robotNameTaken(String team, String name, long opponentId) {
		List<Long> owners = Database.read(em -> em.createQuery("select o.id from Opponent o where o.team = :team and o.name = :name", Long.class)
				.setParameter("team", team).setParameter("name", name).getResultList());
		return owners.stream().anyMatch(id -> id != opponentId);
	}

	/**
	 * Loads robot from database
	 *
	 * @return the robot or null if it does not exist
	 */
	private Opponent loadOpponent(long opponentId) {
		return Database.read(em -> em.find(Opponent.class, opponentId));
	}

	/**
	 * Saves robot to database
	 */
	private Opponent saveOpponent(Opponent opponent) {
		return Database.inTransaction(em -> {
			// Saves or update depending if it already exists or not
			if (opponent.getId() == 0) {
				em.persist(opponent);
				return opponent;
			}
			return em.merge(opponent);
		});
	}
}
