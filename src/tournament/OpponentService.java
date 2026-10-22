package tournament;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jakarta.persistence.EntityManager;
import model.Author;
import model.Championship;
import model.Opponent;
import utils.database.Database;
import utils.web.PasswordHasher;
import widgets.Sound;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The robots registered for the championship, seen, added, changed and removed by the organizer. The rules are
 * the ones of the registration on the web.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class OpponentService {

	private static final int MAX_NAME_LENGTH = 100;
	private static final int MAX_REGISTRATION_LENGTH = 20;
	private static final int MAX_FILE_SIZE = 1024 * 1024;
	private static final int MAX_JAR_SIZE = 8 * 1024 * 1024;
	private static final int MAX_LOGO_WIDTH = 640;
	private static final int MAX_LOGO_HEIGHT = 480;
	private static final double MAX_MUSIC_START = 3600;

	private static final Pattern TEAM_NAME = Pattern.compile("[A-Za-z0-9_]{1," + MAX_NAME_LENGTH + "}");
	private static final Pattern ROBOT_NAME = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*");
	private static final Pattern REGISTRATION = Pattern.compile("[0-9]{1," + MAX_REGISTRATION_LENGTH + "}");
	private static final Pattern MAIL = Pattern.compile("[^@\\s]+@[^@\\s]+");

	/**
	 * An author as the editor shows it. The id is 0 for an author added now.
	 */
	public record AuthorData(long id, String name, String registration, String mail) {
	}

	/**
	 * Everything of a robot that the organizer can change. A file that is null stays as it was; a new robot needs
	 * all of them. A password that is blank stays as it was; a new robot needs one.
	 */
	public record RobotData(String team, String name, String login, String password, String battleCry, double musicStart, String sourceCode,
			byte[] image, byte[] music, byte[] jar, List<AuthorData> authors) {
	}

	/** Only static methods: this class is not instantiated. */
	private OpponentService() {
	}

	/** All the robots registered, in the order they registered. */
	public static List<Opponent> all() {
		return new ArrayList<Opponent>(Database.read(entityManager -> entityManager.createQuery("select o from Opponent o order by o.id", Opponent.class).getResultList()));
	}

	/**
	 * Adds a robot, or saves the changes of one.
	 *
	 * @param opponentId
	 *            the robot to change, 0 to add a new one
	 * @return the robot as it was saved
	 * @throws IllegalArgumentException
	 *             with the message to show when something is wrong
	 */
	public static Opponent save(long opponentId, RobotData data) {
		boolean isNew = opponentId == 0;
		checkIdentity(data, isNew);
		checkAuthors(data.authors());
		checkFiles(data, isNew);

		return Database.inTransaction(entityManager -> {
			Opponent robot = isNew ? new Opponent() : entityManager.find(Opponent.class, opponentId);
			if (robot == null) {
				throw new IllegalArgumentException("Robô não encontrado, talvez ele tenha sido removido");
			}
			checkTaken(entityManager, opponentId, data);

			List<Author> authors = authorsOf(robot, data);
			fill(robot, data);
			robot.getAuthors().clear();
			robot.getAuthors().addAll(authors);

			if (isNew) {
				entityManager.persist(robot);
			}
			return robot;
		});
	}

	/** How many championships have the robot drawn: they go away with it, see {@link #delete(long)}. */
	public static long championshipsOf(long opponentId) {
		return Database.read(entityManager -> entityManager.createQuery("select count(c) from Championship c join c.seeds s where s.id = :id", Long.class).setParameter("id", opponentId).getSingleResult());
	}

	/**
	 * Removes a robot. A championship cannot go on without one of its robots, so the championships that have it
	 * drawn are removed too, with their results.
	 */
	public static void delete(long opponentId) {
		Database.inTransaction(entityManager -> {
			Opponent robot = entityManager.find(Opponent.class, opponentId);
			if (robot == null) {
				return null;
			}
			List<Championship> drawn = entityManager.createQuery("select c from Championship c join c.seeds s where s.id = :id", Championship.class).setParameter("id", opponentId).getResultList();
			for (Championship championship : drawn) {
				entityManager.remove(championship);
			}
			entityManager.remove(robot);
			return null;
		});
	}

	/**
	 * The authors of the form as entities. They all share the login and the password of the team: the new one
	 * when it was typed, or else the one the team already has.
	 */
	private static List<Author> authorsOf(Opponent robot, RobotData data) {
		String hash = data.password() == null || data.password().isBlank() ? null : PasswordHasher.hash(data.password());
		String passwordHash = hash != null ? hash : robot.getAuthors().isEmpty() ? null : robot.getAuthors().get(0).getPassword();
		if (passwordHash == null) {
			throw new IllegalArgumentException("Informe a senha");
		}

		List<Author> authors = new ArrayList<Author>();
		for (AuthorData author : data.authors()) {
			Author entity = author.id() == 0 ? new Author() : findAuthor(robot, author.id());
			entity.setName(author.name().trim());
			entity.setRegistration(author.registration().trim());
			entity.setMail(author.mail().trim());
			entity.setLogin(data.login().trim());
			entity.setPassword(passwordHash);
			authors.add(entity);
		}
		return authors;
	}

	/** The texts of the robot, and the files that were sent: the others stay as they are. */
	private static void fill(Opponent robot, RobotData data) {
		robot.setTeam(data.team().trim());
		robot.setName(data.name().trim());
		robot.setBattleCry(data.battleCry().trim());
		robot.setMusicStart(data.musicStart());
		robot.setSourceCode(data.sourceCode());
		if (data.image() != null) {
			robot.setImage(data.image());
		}
		if (data.music() != null) {
			robot.setMusic(data.music());
		}
		if (data.jar() != null) {
			robot.setCompiledRobot(data.jar());
		}
	}

	/** The names, the login, the battle cry and the start of the music. */
	private static void checkIdentity(RobotData data, boolean isNew) {
		if (data.team() == null || !TEAM_NAME.matcher(data.team().trim()).matches()) {
			throw new IllegalArgumentException("O time pode ter apenas letras sem acento, números e _, com até " + MAX_NAME_LENGTH + " caracteres");
		}
		if (data.name() == null || data.name().trim().length() > MAX_NAME_LENGTH || !ROBOT_NAME.matcher(data.name().trim()).matches()) {
			throw new IllegalArgumentException("O robô pode ter apenas letras sem acento, números, _ e pontos entre as partes, com até " + MAX_NAME_LENGTH + " caracteres");
		}
		if (data.login() == null || data.login().isBlank()) {
			throw new IllegalArgumentException("Preencha o login");
		}
		if (isNew && (data.password() == null || data.password().isBlank())) {
			throw new IllegalArgumentException("Preencha a senha");
		}
		String cry = data.battleCry() == null ? "" : data.battleCry().trim();
		if (cry.isEmpty()) {
			throw new IllegalArgumentException("Informe o lema do time");
		}
		if (cry.length() > Opponent.MAX_BATTLE_CRY_LENGTH) {
			throw new IllegalArgumentException("O lema pode ter no máximo " + Opponent.MAX_BATTLE_CRY_LENGTH + " caracteres");
		}
		if (data.musicStart() < 0 || data.musicStart() > MAX_MUSIC_START) {
			throw new IllegalArgumentException("O início da música deve estar entre 0 e " + (int) MAX_MUSIC_START + " segundos");
		}
	}

	/** Checks the authors of the form, with the same rules of the registration on the web. */
	private static void checkAuthors(List<AuthorData> authors) {
		if (authors.isEmpty()) {
			throw new IllegalArgumentException("Informe ao menos uma pessoa desenvolvedora");
		}
		Set<String> mails = new HashSet<String>();
		for (AuthorData author : authors) {
			if (author.name() == null || author.name().isBlank()) {
				throw new IllegalArgumentException("Informe o nome de cada pessoa desenvolvedora");
			}
			if (author.registration() == null || !REGISTRATION.matcher(author.registration().trim()).matches()) {
				throw new IllegalArgumentException("O RM deve ter apenas números, com até " + MAX_REGISTRATION_LENGTH + " dígitos");
			}
			if (author.mail() == null || !MAIL.matcher(author.mail().trim()).matches()) {
				throw new IllegalArgumentException("Informe um e-mail válido, no formato nome@dominio");
			}
			if (!mails.add(author.mail().trim())) {
				throw new IllegalArgumentException("O e-mail " + author.mail().trim() + " aparece mais de uma vez");
			}
		}
	}

	/** The files: all of them for a new robot, and the ones that were sent for the others. */
	private static void checkFiles(RobotData data, boolean isNew) {
		if (data.sourceCode() == null || data.sourceCode().isBlank()) {
			throw new IllegalArgumentException("Informe o código fonte do robô");
		}
		if (data.sourceCode().getBytes(StandardCharsets.UTF_8).length > MAX_FILE_SIZE) {
			throw new IllegalArgumentException("O código fonte tem mais de 1 MB");
		}
		if (isNew && (data.image() == null || data.music() == null || data.jar() == null)) {
			throw new IllegalArgumentException("Um robô novo precisa do .jar, do logotipo e da música do time");
		}
		if (data.jar() != null && (data.jar().length == 0 || data.jar().length > MAX_JAR_SIZE)) {
			throw new IllegalArgumentException("O arquivo .jar está vazio ou tem mais de " + MAX_JAR_SIZE / 1024 / 1024 + " MB");
		}
		if (data.music() != null) {
			checkMusic(data.music());
		}
		if (data.image() != null) {
			checkLogo(data.image());
		}
	}

	/** The music has to be a playable file of at most 1 MB. */
	private static void checkMusic(byte[] music) {
		if (music.length == 0 || music.length > MAX_FILE_SIZE) {
			throw new IllegalArgumentException("A música está vazia ou tem mais de 1 MB");
		}
		if (!Sound.isPlayable(music)) {
			throw new IllegalArgumentException("Não foi possível tocar a música, use um arquivo .mp3 válido");
		}
	}

	/** The logo has to be an image of at most 1 MB and 640 x 480 pixels. */
	private static void checkLogo(byte[] image) {
		if (image.length == 0 || image.length > MAX_FILE_SIZE) {
			throw new IllegalArgumentException("O logotipo está vazio ou tem mais de 1 MB");
		}
		BufferedImage logo;
		try {
			logo = ImageIO.read(new ByteArrayInputStream(image));
		} catch (IOException ioException) {
			logo = null;
		}
		if (logo == null) {
			throw new IllegalArgumentException("Logotipo inválido, use .gif, .jpg ou .png");
		}
		if (logo.getWidth() > MAX_LOGO_WIDTH || logo.getHeight() > MAX_LOGO_HEIGHT) {
			throw new IllegalArgumentException("O logotipo deve ter no máximo " + MAX_LOGO_WIDTH + " x " + MAX_LOGO_HEIGHT + " pixels");
		}
	}

	/** The name of the robot and the login belong to a single team each. */
	private static void checkTaken(EntityManager entityManager, long opponentId, RobotData data) {
		boolean nameTaken = entityManager.createQuery("select o.id from Opponent o where o.team = :team and o.name = :name", Long.class)
				.setParameter("team", data.team().trim()).setParameter("name", data.name().trim()).getResultList().stream().anyMatch(id -> id != opponentId);
		if (nameTaken) {
			throw new IllegalArgumentException("Já existe um robô chamado " + data.team().trim() + "." + data.name().trim());
		}
		boolean loginTaken = entityManager.createQuery("select o.id from Opponent o join o.authors a where a.login = :login", Long.class)
				.setParameter("login", data.login().trim()).getResultList().stream().anyMatch(id -> id != opponentId);
		if (loginTaken) {
			throw new IllegalArgumentException("Este login já é de outro time, escolha outro");
		}
	}

	/** The author of the robot with this id. */
	private static Author findAuthor(Opponent robot, long authorId) {
		return robot.getAuthors().stream().filter(author -> author.getId() == authorId).findFirst().orElseThrow(() -> new IllegalArgumentException("Pessoa desenvolvedora não encontrada, recarregue o robô"));
	}
}
