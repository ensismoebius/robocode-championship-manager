package utils.robocode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

	/**
	 * The .jar with its classes marked for the Java that runs the battles, when they were compiled for a newer one: a robot compiled with
	 * Java 25 has classes of version 69, which Java 21 refuses before reading them. The code of a robot is the same in those versions,
	 * only the number in the header of each class changes. Classes that use preview features of their Java are left as they are, and so
	 * is a .jar that cannot be read, or that needs no change.
	 *
	 * @param newest
	 *            the newest version of class the Java of the battles reads (65 for Java 21)
	 */
	public static byte[] forJava(byte[] jar, int newest) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		boolean changed = false;
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(jar)); ZipOutputStream out = new ZipOutputStream(bytes)) {
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				byte[] content = in.readAllBytes();
				if (entry.getName().endsWith(".class") && tooNew(content, newest)) {
					content[6] = (byte) (newest >> 8);
					content[7] = (byte) newest;
					changed = true;
				}
				ZipEntry copy = new ZipEntry(entry.getName());
				copy.setTime(entry.getTime());
				out.putNextEntry(copy);
				out.write(content);
				out.closeEntry();
			}
		} catch (IOException | IllegalArgumentException unreadable) {
			return jar;
		}
		return changed ? bytes.toByteArray() : jar;
	}

	/** Whether the bytes are a class of a version newer than the one given, not one with preview features. */
	private static boolean tooNew(byte[] content, int newest) {
		if (content.length < 8 || (content[0] & 0xff) != 0xca || (content[1] & 0xff) != 0xfe || (content[2] & 0xff) != 0xba || (content[3] & 0xff) != 0xbe) {
			return false;
		}
		int minor = (content[4] & 0xff) << 8 | content[5] & 0xff;
		int major = (content[6] & 0xff) << 8 | content[7] & 0xff;
		return major > newest && minor == 0;
	}

	/**
	 * Why the robot of the .jar cannot run on the Java that runs the battles, which is the Java of this program, or null when it can. A
	 * robot compiled with a newer Java runs once its classes are marked for this one, unless it uses preview features, which Robocode
	 * does not turn on, or something of the Java library that only the newer Java has. A .jar that cannot be read says nothing.
	 */
	public static String incompatibility(byte[] jar) {
		int java = Runtime.version().feature();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(jar))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (!entry.getName().endsWith(".class")) {
					continue;
				}
				ClassCheck.Report report = ClassCheck.read(zip.readAllBytes());
				if (report == null) {
					continue;
				}
				int compiled = report.major() - 44;
				if (report.minor() == ClassCheck.PREVIEW) {
					return "O robô usa recursos experimentais (preview) do Java " + compiled + ", que o Robocode não liga. Compile-o sem --enable-preview, com a opção --release " + java + ".";
				}
				if (report.missing() != null) {
					return "O robô usa " + report.missing() + ", que não existe no Java " + java + " que roda as batalhas (o robô foi compilado com o Java " + compiled + "). Compile-o com a opção --release " + java + ".";
				}
			}
		} catch (IOException | IllegalArgumentException unreadable) {
			return null;
		}
		return null;
	}
}
