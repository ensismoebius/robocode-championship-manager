package jetty.servlets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import model.Opponent;
import widgets.Sound;

/**
 * The files sent with the robot editor form: they are read, checked and stored on the robot
 */
final class RobotUploads {

	private static final int MAX_FILE_SIZE = 1024 * 1024;
	private static final int MAX_LOGO_WIDTH = 640;
	private static final int MAX_LOGO_HEIGHT = 480;

	/**
	 * An uploaded file
	 */
	record Upload(String fileName, byte[] content) {
	}

	/**
	 * Reads an uploaded file
	 *
	 * @return the file or null if no file was sent
	 */
	static Upload read(HttpServletRequest req, String name) throws IOException, ServletException {
		Part part = req.getPart(name);
		if (part == null || part.getSubmittedFileName() == null || part.getSubmittedFileName().isEmpty()) {
			return null;
		}

		String fileName = Paths.get(part.getSubmittedFileName()).getFileName().toString();
		try (InputStream content = part.getInputStream()) {
			return new Upload(fileName, content.readAllBytes());
		}
	}

	/** All of them are needed for new robots */
	static void requireFiles(List<String> errors, boolean isNew, Upload robotJar, Upload robotJava, Upload teamMusic, Upload teamLogo) throws IOException {
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
	}

	/** Stores the files that were sent on the robot: the jar, the source, the music and the logo. */
	static void copyUploads(Opponent robot, Upload robotJar, Upload robotJava, Upload teamMusic, Upload teamLogo) throws IOException {
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
	}

	/** Adds an error when the uploaded Java file is empty. */
	static void checkJava(List<String> errors, Upload robotJava) {
		if (robotJava != null && robotJava.content().length == 0) {
			errors.add("Arquivo .java inválido!");
		}
		if (robotJava != null && robotJava.content().length > MAX_FILE_SIZE) {
			errors.add("Arquivo .java muito grande!");
		}
	}

	/** Adds an error when the uploaded music is empty. */
	static void checkMusic(List<String> errors, Upload teamMusic) {
		if (teamMusic != null && teamMusic.content().length == 0) {
			errors.add("Arquivo música inválido!");
		}
		if (teamMusic != null && teamMusic.content().length > MAX_FILE_SIZE) {
			errors.add("Arquivo música muito grande!");
		} else if (teamMusic != null && teamMusic.content().length > 0 && !Sound.isPlayable(teamMusic.content())) {
			errors.add("Não foi possível tocar a música, envie um arquivo .mp3 válido!");
		}
	}

	/** Checks the uploaded logo: its size, that it is an image and that it is not too large. */
	static void checkLogo(List<String> errors, Upload teamLogo) throws IOException {
		if (teamLogo == null) {
			return;
		}
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
}
