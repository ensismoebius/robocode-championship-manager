package tournament;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import model.Author;
import model.Opponent;
import utils.database.Database;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The robots registered for the championship, seen and edited by the organizer. Only the texts can change
 * here: the team and robot names come from the .jar file, and the files stay as the team sent them.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class OpponentService {

	private static final int MAX_REGISTRATION_LENGTH = 20;
	private static final Pattern REGISTRATION = Pattern.compile("[0-9]{1," + MAX_REGISTRATION_LENGTH + "}");
	private static final Pattern MAIL = Pattern.compile("[^@\\s]+@[^@\\s]+");

	/**
	 * An author as the editor shows it. The id is 0 for an author added now.
	 */
	public record AuthorData(long id, String name, String registration, String mail) {
	}

	/** Only static methods: this class is not instantiated. */
	private OpponentService() {
	}

	/** All the robots registered, in the order they registered. */
	public static List<Opponent> all() {
		return new ArrayList<Opponent>(Database.read(entityManager -> entityManager.createQuery("select o from Opponent o order by o.id", Opponent.class).getResultList()));
	}

	/**
	 * Saves the battle cry and the authors of a robot. The authors that are not in the list are removed, the
	 * new ones get the login and the password of the team.
	 *
	 * @return the robot as it was saved
	 * @throws IllegalArgumentException
	 *             with the message to show when something is wrong
	 */
	public static Opponent update(long opponentId, String battleCry, List<AuthorData> authors) {
		String cry = battleCry == null ? "" : battleCry.trim();
		if (cry.isEmpty()) {
			throw new IllegalArgumentException("Informe o lema do time");
		}
		if (cry.length() > Opponent.MAX_BATTLE_CRY_LENGTH) {
			throw new IllegalArgumentException("O lema pode ter no máximo " + Opponent.MAX_BATTLE_CRY_LENGTH + " caracteres");
		}
		checkAuthors(authors);

		return Database.inTransaction(entityManager -> {
			Opponent robot = entityManager.find(Opponent.class, opponentId);
			if (robot == null) {
				throw new IllegalArgumentException("Robô não encontrado, talvez ele tenha sido removido");
			}
			if (robot.getAuthors().isEmpty()) {
				throw new IllegalStateException("O robô não tem login: cadastre-o de novo");
			}
			Author team = robot.getAuthors().get(0);

			List<Author> saved = new ArrayList<Author>();
			for (AuthorData data : authors) {
				Author author = data.id() == 0 ? newAuthor(team) : findAuthor(robot, data.id());
				author.setName(data.name().trim());
				author.setRegistration(data.registration().trim());
				author.setMail(data.mail().trim());
				saved.add(author);
			}

			robot.setBattleCry(cry);
			robot.getAuthors().clear();
			robot.getAuthors().addAll(saved);
			return robot;
		});
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

	/** An author that shares the login and the password of the team. */
	private static Author newAuthor(Author team) {
		Author author = new Author();
		author.setLogin(team.getLogin());
		author.setPassword(team.getPassword());
		return author;
	}

	/** The author of the robot with this id. */
	private static Author findAuthor(Opponent robot, long authorId) {
		return robot.getAuthors().stream().filter(author -> author.getId() == authorId).findFirst().orElseThrow(() -> new IllegalArgumentException("Pessoa desenvolvedora não encontrada, recarregue o robô"));
	}
}
