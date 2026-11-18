package tournament;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import model.Championship;
import model.Opponent;
import utils.configuration.Configuration;
import utils.database.Database;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * What the organizer does with the data of a championship besides playing it: a report to read, an archive with
 * everything to keep, a backup of the database and the removal of the data. The files go to folders next to the
 * database file, so they stay with the data they came from.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class DataArchive {

	/** Where the archive keeps its copy of the database. */
	private static final String ARCHIVED_DATABASE = "banco/robochamp.db";

	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	/** Only static methods: this class is not instantiated. */
	private DataArchive() {
	}

	/** The folder of the reports, the archives or the backups: the one with that name next to the database file. */
	public static Path folder(String name) {
		return Path.of(Configuration.getDatabaseFile()).toAbsolutePath().getParent().resolve(name);
	}

	/** The date and time to put in the name of a file. */
	private static String stamp() {
		return LocalDateTime.now().format(STAMP);
	}

	/**
	 * Writes the report of the championship (the page and the table of matches) in the folder reports
	 *
	 * @return the page
	 */
	public static Path report(Championship championship) throws IOException {
		Path folder = folder("reports");
		Files.createDirectories(folder);
		String base = "relatorio-" + championship.getId() + "-" + stamp();
		Files.writeString(folder.resolve(base + ".csv"), ChampionshipReport.csv(championship), StandardCharsets.UTF_8);
		return Files.writeString(folder.resolve(base + ".html"), ChampionshipReport.html(championship), StandardCharsets.UTF_8);
	}

	/**
	 * Copies the whole database to a file in the folder backups, a consistent copy even while the program uses it
	 *
	 * @return the copy
	 */
	public static Path backup() throws IOException {
		Path folder = folder("backups");
		Files.createDirectories(folder);
		// Two backups in the same second get different names
		String base = "robochamp-" + stamp();
		Path copy = folder.resolve(base + ".db");
		for (int again = 2; Files.exists(copy); again++) {
			copy = folder.resolve(base + "-" + again + ".db");
		}
		copyDatabaseTo(copy);
		return copy;
	}

	/** SQLite writes a consistent copy of the database with VACUUM INTO, which needs a connection that is not in a transaction. */
	private static void copyDatabaseTo(Path copy) throws IOException {
		Path file = Path.of(Configuration.getDatabaseFile()).toAbsolutePath();
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file); Statement statement = connection.createStatement()) {
			statement.execute("VACUUM INTO '" + copy.toString().replace("'", "''") + "'");
		} catch (SQLException sqlException) {
			throw new IOException("Falha ao copiar o banco de dados: " + sqlException.getMessage(), sqlException);
		}
	}

	/**
	 * Keeps the championship and the robots in a zip file in the folder archives: the report, the matches, the files
	 * of every robot and a copy of the database, which has everything. Nothing is removed.
	 *
	 * @param championship
	 *            may be null when there is no championship, then the archive has only the robots
	 * @return the zip file
	 */
	public static Path archive(Championship championship) throws IOException {
		Path folder = folder("archives");
		Files.createDirectories(folder);
		Path zip = folder.resolve("arquivo-" + stamp() + ".zip");
		Path copy = Files.createTempFile("robochamp-archive", ".db");
		try {
			Files.delete(copy);
			copyDatabaseTo(copy);
			List<Opponent> robots = Database.read(entityManager -> entityManager.createQuery("select o from Opponent o order by o.id", Opponent.class).getResultList());
			try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
				if (championship != null) {
					put(out, "relatorio.html", ChampionshipReport.html(championship).getBytes(StandardCharsets.UTF_8));
					put(out, "partidas.csv", ChampionshipReport.csv(championship).getBytes(StandardCharsets.UTF_8));
				}
				for (Opponent robot : robots) {
					putRobot(out, robot);
				}
				put(out, ARCHIVED_DATABASE, Files.readAllBytes(copy));
			}
		} catch (IOException | RuntimeException failure) {
			Files.deleteIfExists(zip);
			throw failure;
		} finally {
			Files.deleteIfExists(copy);
		}
		return zip;
	}

	/** Puts the files of the robot in its own folder of the zip. */
	private static void putRobot(ZipOutputStream out, Opponent robot) throws IOException {
		String folder = "robos/" + robot.getTeam() + "/" + robot.getName() + "/";
		put(out, folder + "info.txt", ("Robô: " + robot.getFullName() + "\nAutores: " + ChampionshipReport.authorsOf(robot) + "\nGrito de guerra: " + (robot.getBattleCry() == null ? "" : robot.getBattleCry()) + "\n")
				.getBytes(StandardCharsets.UTF_8));
		if (robot.getSourceCode() != null) {
			put(out, folder + "codigo.java", robot.getSourceCode().getBytes(StandardCharsets.UTF_8));
		}
		if (robot.getCompiledRobot() != null) {
			put(out, folder + "robo.jar", robot.getCompiledRobot());
		}
		if (robot.getImage() != null) {
			put(out, folder + "imagem" + imageExtension(robot.getImage()), robot.getImage());
		}
		if (robot.getMusic() != null) {
			put(out, folder + "musica.mp3", robot.getMusic());
		}
	}

	/** The extension of the image by its first bytes. */
	static String imageExtension(byte[] image) {
		if (image.length > 3 && (image[0] & 0xFF) == 0x89 && image[1] == 'P') {
			return ".png";
		}
		if (image.length > 3 && (image[0] & 0xFF) == 0xFF && (image[1] & 0xFF) == 0xD8) {
			return ".jpg";
		}
		if (image.length > 3 && image[0] == 'G' && image[1] == 'I') {
			return ".gif";
		}
		return ".img";
	}

	/** Puts a file in the zip. */
	private static void put(ZipOutputStream out, String name, byte[] content) throws IOException {
		out.putNextEntry(new ZipEntry(name));
		out.write(content);
		out.closeEntry();
	}

	/** Removes every championship, with its results and photos. The robots stay. */
	public static void deleteChampionships() {
		Database.inTransaction(entityManager -> {
			entityManager.createQuery("delete from BattleMoment").executeUpdate();
			for (Championship championship : entityManager.createQuery("select c from Championship c", Championship.class).getResultList()) {
				entityManager.remove(championship);
			}
			return null;
		});
	}

	/** Removes everything: the championships and the robots, with their authors. */
	public static void deleteAll() {
		deleteChampionships();
		Database.inTransaction(entityManager -> {
			for (Opponent robot : entityManager.createQuery("select o from Opponent o", Opponent.class).getResultList()) {
				entityManager.remove(robot);
			}
			return null;
		});
	}

	/**
	 * Brings the data back from a backup (a database file) or from an archive (the .zip, which has the database
	 * inside). The current data is backed up first, so even a wrong restore can be undone. The database is closed
	 * and opens again, with the restored data, the next time it is used.
	 *
	 * @return the backup of the data that was replaced
	 * @throws IOException
	 *             when the file is not a backup of this program or cannot be read; nothing changes then
	 */
	public static Path restore(Path source) throws IOException {
		Path database = source;
		boolean temporary = false;
		if (source.toString().toLowerCase().endsWith(".zip")) {
			database = Files.createTempFile("robochamp-restore", ".db");
			temporary = true;
			try (ZipFile zip = new ZipFile(source.toFile())) {
				ZipEntry entry = zip.getEntry(ARCHIVED_DATABASE);
				if (entry == null) {
					throw new IOException("O arquivo não tem o banco de dados (" + ARCHIVED_DATABASE + ")");
				}
				Files.copy(zip.getInputStream(entry), database, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		try {
			checkIsBackup(database);
			Path safety = backup();
			Path file = Path.of(Configuration.getDatabaseFile()).toAbsolutePath();
			Database.close();
			// The log files belong to the database that goes away
			Files.deleteIfExists(Path.of(file + "-wal"));
			Files.deleteIfExists(Path.of(file + "-shm"));
			Files.copy(database, file, StandardCopyOption.REPLACE_EXISTING);
			return safety;
		} finally {
			if (temporary) {
				Files.deleteIfExists(database);
			}
		}
	}

	/** A backup of this program is a database with the tables of the robots and of the championships. */
	private static void checkIsBackup(Path database) throws IOException {
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database); Statement statement = connection.createStatement();
				ResultSet tables = statement.executeQuery("select count(*) from sqlite_master where type = 'table' and name in ('Opponent', 'Championship')")) {
			tables.next();
			if (tables.getInt(1) != 2) {
				throw new IOException("O arquivo não é um backup deste programa");
			}
		} catch (SQLException sqlException) {
			throw new IOException("O arquivo não é um banco de dados válido: " + sqlException.getMessage(), sqlException);
		}
	}
}
