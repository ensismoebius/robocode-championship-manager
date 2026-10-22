package widgets.scenes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import widgets.GameWindow;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The files the organizer gives to the robots editor: asking for them and reading them.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class RobotFiles {

	/** Robocode packager generates files like team.RobotName_1.0.jar */
	private static final Pattern JAR_FILE_NAME = Pattern.compile("^([^.]+)\\.(.+?)_[0-9][0-9.]*\\.jar$");

	/** The files are not instantiated. */
	private RobotFiles() {
	}

	/** Asks for a file, null when the organizer gives up. */
	static File pick(String caption, String description, String... extensions) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(caption);
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensions));
		return chooser.showOpenDialog(GameWindow.getWindow());
	}

	/** The bytes of a file, null (with the reason shown in the label) when it cannot be read or when there is no file. */
	static byte[] read(File file, Label error) {
		if (file == null) {
			return null;
		}
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException ioException) {
			error.setText("Não foi possível ler " + file.getName() + ": " + ioException.getMessage());
			return null;
		}
	}

	/** The team and the robot named by a .jar of the packager, or null when its name is another one. */
	static String[] namesOf(String jarName) {
		Matcher matcher = JAR_FILE_NAME.matcher(jarName);
		return matcher.matches() ? new String[] { matcher.group(1), matcher.group(2) } : null;
	}
}
