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

	@Test
	void aJarThatSaysNothingIsNotRefused() {
		assertTrue(RobotJar.declaredRobots(new byte[] { 1, 2, 3 }).isEmpty());
		assertNull(RobotJar.mismatch(new byte[] { 1, 2, 3 }, "x", "q"));
	}
}
