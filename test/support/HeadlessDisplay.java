package support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import widgets.AudioOut;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * What the tests need before the first one runs, however they are run (by Maven or by an editor): no sound, a home of their own (so
 * they never touch the configuration of whoever runs them), and the virtual screen they run on, so their windows do not open on the
 * desktop. The display comes from the environment (DISPLAY), which the build (the profile headless of the pom) and the test
 * configuration of the editor (.vscode/settings.json) point to the display in the system property test.xvfb; this starts an Xvfb
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
		quietAndAtHome();
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

	/**
	 * The tests play no sound, and their home is target/test-home of the project, unless who runs them says otherwise: the
	 * configuration and the database of the program are in the home, and the tests write them.
	 */
	private static void quietAndAtHome() {
		if (System.getProperty(AudioOut.PROPERTY) == null) {
			System.setProperty(AudioOut.PROPERTY, "off");
		}
		String home = System.getenv("HOME");
		if (home == null || Path.of(System.getProperty("user.home")).toAbsolutePath().equals(Path.of(home).toAbsolutePath())) {
			Path own = Path.of("target", "test-home").toAbsolutePath();
			try {
				Files.createDirectories(own);
			} catch (IOException ignored) {
				// The tests that write there will say so
			}
			System.setProperty("user.home", own.toString());
		}
	}

	/** The socket of the X display (:87 is /tmp/.X11-unix/X87). */
	private static Path socketOf(String display) {
		String number = display.substring(display.indexOf(':') + 1);
		int dot = number.indexOf('.');
		return Path.of("/tmp/.X11-unix", "X" + (dot < 0 ? number : number.substring(0, dot)));
	}
}
