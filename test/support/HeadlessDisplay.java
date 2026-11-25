package support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The virtual screen the tests run on, so their windows do not open on the desktop of whoever runs them. The build (the profile
 * headless of the pom) points the tests to the display in the system property test.xvfb; before the first test this starts an Xvfb
 * there, if none is running yet. It is stopped only after the tests have ended, by a watcher that waits for their process to end,
 * even when it crashes: stopped while JavaFX is still on it, the toolkit would abort the process. Without the property the tests
 * use the screen they are given.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class HeadlessDisplay implements LauncherSessionListener {

	/** How long Xvfb has to come up */
	private static final long START_MILLIS = 15000;

	/** The watcher that runs Xvfb and stops it when the tests end: it ends with it */
	private Process server;

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		String display = System.getProperty("test.xvfb");
		if (display == null || display.isBlank() || this.server != null) {
			return;
		}
		Path socket = socketOf(display);
		if (Files.exists(socket)) {
			// Another run of the tests already has it up: it is shared
			return;
		}
		try {
			String watch = "Xvfb \"$1\" -screen 0 1920x1080x24 -nolisten tcp & screen=$!; "
					+ "while kill -0 \"$2\" 2>/dev/null && kill -0 $screen 2>/dev/null; do sleep 1; done; kill $screen 2>/dev/null; wait $screen";
			this.server = new ProcessBuilder("sh", "-c", watch, "xvfb-watch", display, Long.toString(ProcessHandle.current().pid())).redirectErrorStream(true)
					.redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
			long deadline = System.currentTimeMillis() + START_MILLIS;
			while (!Files.exists(socket) && this.server.isAlive() && System.currentTimeMillis() < deadline) {
				Thread.sleep(50);
			}
			if (!Files.exists(socket)) {
				throw new IllegalStateException("O Xvfb não abriu a tela " + display + " para os testes");
			}
		} catch (IOException couldNotStart) {
			throw new IllegalStateException("Não foi possível iniciar o Xvfb para os testes (use mvn test -Dgui para usar a tela)", couldNotStart);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/** The socket of the X display (:87 is /tmp/.X11-unix/X87). */
	private static Path socketOf(String display) {
		String number = display.substring(display.indexOf(':') + 1);
		int dot = number.indexOf('.');
		return Path.of("/tmp/.X11-unix", "X" + (dot < 0 ? number : number.substring(0, dot)));
	}
}
