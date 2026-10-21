/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Entry point of the fat jar. The JavaFX launcher refuses to start an
 * Application subclass from the classpath, but works if it is called from
 * another class.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Launcher {

	/** Starts the program. */
	public static void main(String[] args) {
		Main.main(args);
	}
}
