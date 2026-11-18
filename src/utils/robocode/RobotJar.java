package utils.robocode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * What the .jar of a robot says it holds. Robocode finds a robot by the name of its class (team.Robot), the one the packager
 * writes in the .properties of the .jar: a robot registered with another name is never found when the battle starts.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class RobotJar {

	/** Only static methods: this class is not instantiated. */
	private RobotJar() {
	}

	/**
	 * The names of the robots (team.Robot) that the .jar declares in the .properties of the packager. Empty when the .jar cannot be read or
	 * has no .properties, which is not enough to say that the robot is not there.
	 */
	public static Set<String> declaredRobots(byte[] jar) {
		Set<String> names = new LinkedHashSet<String>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(jar))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.getName().endsWith(".properties")) {
					Properties properties = new Properties();
					properties.load(new java.io.StringReader(new String(zip.readAllBytes(), StandardCharsets.ISO_8859_1)));
					String declared = properties.getProperty("robot.classname");
					if (declared != null && !declared.isBlank()) {
						names.add(declared.trim());
					}
				}
			}
		} catch (IOException | IllegalArgumentException unreadable) {
			return new LinkedHashSet<String>();
		}
		return names;
	}

	/**
	 * The team and the robot of the .jar: the only robot it declares, split at the first dot (team.Robot, the robot may have more dots).
	 * Null when the .jar declares none, or more than one, or one with no team.
	 */
	public static String[] names(byte[] jar) {
		Set<String> declared = declaredRobots(jar);
		if (declared.size() != 1) {
			return null;
		}
		String full = declared.iterator().next();
		int dot = full.indexOf('.');
		return dot <= 0 || dot == full.length() - 1 ? null : new String[] { full.substring(0, dot), full.substring(dot + 1) };
	}

	/** The reason the .jar is not the robot team.name, or null when it is, or when the .jar does not say which robots it holds. */
	public static String mismatch(byte[] jar, String team, String name) {
		Set<String> declared = declaredRobots(jar);
		String wanted = team + "." + name;
		if (declared.isEmpty() || declared.contains(wanted)) {
			return null;
		}
		return "O .jar tem o robô " + String.join(", ", declared) + ", mas o cadastro é " + wanted + ". O Robocode só acha o robô pelo nome da classe: use o mesmo nome nos dois.";
	}
}
