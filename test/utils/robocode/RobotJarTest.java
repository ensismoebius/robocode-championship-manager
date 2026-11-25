package utils.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The robot a .jar holds, which is what Robocode looks for when a battle starts.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RobotJarTest {

	/** A jar of the packager: the class of the robot and its .properties. */
	public static byte[] jarOf(String className) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			String path = className.replace('.', '/');
			zip.putNextEntry(new ZipEntry(path + ".class"));
			zip.write(new byte[] { 1 });
			zip.closeEntry();
			zip.putNextEntry(new ZipEntry(path + ".properties"));
			zip.write(("robot.classname=" + className + "\nrobot.version=1.0\n").getBytes(StandardCharsets.ISO_8859_1));
			zip.closeEntry();
		}
		return bytes.toByteArray();
	}

	@Test
	void theRobotOfTheJarIsTheOneItsPropertiesDeclare() throws IOException {
		assertEquals(Set.of("charlie.Walls"), RobotJar.declaredRobots(jarOf("charlie.Walls")));
		assertNull(RobotJar.mismatch(jarOf("charlie.Walls"), "charlie", "Walls"));
	}

	@Test
	void aRobotRegisteredWithAnotherNameIsRefusedWithTheNameOfTheJar() throws IOException {
		String reason = RobotJar.mismatch(jarOf("charlie.Walls"), "x", "q");

		assertNotNull(reason);
		assertTrue(reason.contains("charlie.Walls") && reason.contains("x.q"), reason);
	}

	/** A jar with one class of the version given, as javac writes its header. */
	private static byte[] jarWithClassVersion(int major, int minor) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			zip.putNextEntry(new ZipEntry("team/Bot.class"));
			zip.write(new byte[] { (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, (byte) (minor >> 8), (byte) minor, (byte) (major >> 8), (byte) major, 1, 2, 3 });
			zip.closeEntry();
		}
		return bytes.toByteArray();
	}

	/** The header of the only class of a jar. */
	private static byte[] classOf(byte[] jar) throws IOException {
		try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(jar))) {
			zip.getNextEntry();
			return zip.readAllBytes();
		}
	}

	@Test
	void classesCompiledForANewerJavaAreMarkedForTheJavaOfTheBattles() throws IOException {
		byte[] fitted = RobotJar.forJava(jarWithClassVersion(69, 0), 65);

		byte[] header = classOf(fitted);
		assertEquals(65, (header[6] & 0xff) << 8 | header[7] & 0xff);
		assertEquals(3, header[10]);
	}

	@Test
	void classesTheJavaReadsPreviewClassesAndUnreadableJarsAreLeftAsTheyAre() throws IOException {
		byte[] old = jarWithClassVersion(61, 0);
		byte[] preview = jarWithClassVersion(69, 0xffff);
		byte[] garbage = { 1, 2, 3 };

		assertTrue(RobotJar.forJava(old, 65) == old);
		assertTrue(RobotJar.forJava(preview, 65) == preview);
		assertTrue(RobotJar.forJava(garbage, 65) == garbage);
	}

	/** Where the robots compiled by the tests are written, removed after them. */
	@org.junit.jupiter.api.io.TempDir
	static java.nio.file.Path temporary;

	/** A robot compiled here, for Java 21, that calls String.indent: its class as javac wrote it. */
	private static byte[] compiledRobot() throws IOException {
		java.nio.file.Path folder = java.nio.file.Files.createTempDirectory(temporary, "robot");
		java.nio.file.Path source = folder.resolve("Bot.java");
		java.nio.file.Files.writeString(source, "package team; public class Bot { public String look(String text) { return text.indent(2); } }");
		javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
		assertEquals(0, compiler.run(null, null, null, "--release", "21", "-d", folder.toString(), source.toString()));
		return java.nio.file.Files.readAllBytes(folder.resolve("team").resolve("Bot.class"));
	}

	/** The class in a jar, as team/Bot.class, marked with the version given. */
	private static byte[] jarOfClass(byte[] content, int major, int minor) throws IOException {
		byte[] marked = content.clone();
		marked[4] = (byte) (minor >> 8);
		marked[5] = (byte) minor;
		marked[6] = (byte) (major >> 8);
		marked[7] = (byte) major;
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			zip.putNextEntry(new ZipEntry("team/Bot.class"));
			zip.write(marked);
			zip.closeEntry();
		}
		return bytes.toByteArray();
	}

	/** The bytes with the text replaced by another of the same length, as a newer Java would name a method this one does not have. */
	private static byte[] renamed(byte[] content, String from, String to) {
		byte[] wanted = from.getBytes(StandardCharsets.UTF_8);
		byte[] result = content.clone();
		for (int index = 0; index + wanted.length <= result.length; index++) {
			if (java.util.Arrays.equals(result, index, index + wanted.length, wanted, 0, wanted.length)) {
				System.arraycopy(to.getBytes(StandardCharsets.UTF_8), 0, result, index, wanted.length);
			}
		}
		return result;
	}

	@Test
	void aRobotOfANewerJavaThatUsesOnlyWhatThisJavaHasRuns() throws IOException {
		assertNull(RobotJar.incompatibility(jarOfClass(compiledRobot(), 69, 0)));
	}

	@Test
	void aRobotThatUsesSomethingThisJavaDoesNotHaveIsNamed() throws IOException {
		String reason = RobotJar.incompatibility(jarOfClass(renamed(compiledRobot(), "indent", "indexx"), 69, 0));

		assertNotNull(reason);
		assertTrue(reason.contains("java.lang.String.indexx()") && reason.contains("Java 25") && reason.contains("--release " + Runtime.version().feature()), reason);
	}

	@Test
	void aRobotThatUsesPreviewFeaturesIsNamed() throws IOException {
		String reason = RobotJar.incompatibility(jarOfClass(compiledRobot(), 69, 0xffff));

		assertNotNull(reason);
		assertTrue(reason.contains("preview"), reason);
	}

	@Test
	void aJarThatSaysNothingIsNotRefused() {
		assertTrue(RobotJar.declaredRobots(new byte[] { 1, 2, 3 }).isEmpty());
		assertNull(RobotJar.mismatch(new byte[] { 1, 2, 3 }, "x", "q"));
	}
}
