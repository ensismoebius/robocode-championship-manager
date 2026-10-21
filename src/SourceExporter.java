import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jakarta.persistence.PersistenceException;
import model.Opponent;
import utils.configuration.Configuration;
import utils.database.Database;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Writes the source code of the robots to files, so they can be read outside the
 * program.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class SourceExporter {

	private SourceExporter() {
	}

	/**
	 * The folder of the export, in the temporary folder of the system
	 */
	static Path defaultDir() {
		return Path.of(Configuration.getTempDir(), "robochamp-export");
	}

	/**
	 * Saves the source code of every robot as exportDir/team/RobotName.java; the
	 * robots without source code are skipped
	 *
	 * @return how many robots there are in the database
	 */
	static int export(Path exportDir) throws IOException, PersistenceException {
		List<Opponent> all = Database.read(emValue -> emValue.createQuery("select o from Opponent o", Opponent.class).getResultList());

		for (Opponent opponent : all) {
			if (opponent.getSourceCode() == null) {
				continue;
			}
			Path file = exportDir.resolve(opponent.getTeam()).resolve(opponent.getName().replace('.', '/') + ".java");
			Files.createDirectories(file.getParent());
			Files.writeString(file, opponent.getSourceCode());
		}
		return all.size();
	}
}
