package jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jetty.server.HttpServer;
import model.Opponent;
import support.MultipartForm;
import support.TestConfiguration;
import utils.configuration.Configuration;
import utils.database.Database;

/** The checks of the robot form: every message the form shows for a wrong field, and nothing saved after them. */
class RobotFormValidationTest {

	private static HttpServer server;
	private static String base;

	@BeforeAll
	static void startTheServer() throws Exception {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
		server = new HttpServer(0);
		base = "http://localhost:" + server.getPort();
		// A team that already exists, so the duplicated login and robot name can be checked
		// Logged in after saving, so the cookies of the session are kept for the redirects
		HttpClient session = MultipartForm.session();
		String saved = robot("dono", "Vamos", "dono.Robo_1.0.jar", new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" }).post(session, url("new"));
		assertTrue(saved.contains("Robô salvo com sucesso"), saved);
	}

	@AfterAll
	static void stopTheServer() {
		server.stop();
		Database.close();
	}

	private static String url(String action) {
		return base + "/robotEditor?action=" + action;
	}

	/** Without cookies: the checks run as a visitor who is not logged in. */
	private static HttpClient client() {
		return HttpClient.newHttpClient();
	}

	/** The robot form with the parts given: a null part is left out of the form. */
	private static MultipartForm robot(String login, String warcry, String jarName, byte[] jar, byte[] javaFile, byte[] music, byte[] logo, String[] names, String[] emails) {
		MultipartForm form = new MultipartForm().field("login", login).field("password", "pass1").field("teamWarcry", warcry);
		if (names != null) {
			for (String name : names) {
				form.field("name[]", name);
			}
		}
		if (emails != null) {
			for (String email : emails) {
				form.field("email[]", email).field("registration[]", "12345");
			}
		}
		if (jar != null) {
			form.file("robotJarFile", jarName, jar);
		}
		if (javaFile != null) {
			form.file("robotJavaFile", "MyBot.java", javaFile);
		}
		if (music != null) {
			form.file("teamMusic", "song.mp3", music);
		}
		if (logo != null) {
			form.file("teamLogo", "logo.png", logo);
		}
		return form;
	}

	/** A form with everything right, which the tests change one part at a time. */
	private static MultipartForm valid(String login, String jarName) throws IOException {
		return robot(login, "Vamos", jarName, new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" });
	}

	private static byte[] java() {
		return "class MyBot {}".getBytes(StandardCharsets.UTF_8);
	}

	private static byte[] music() throws IOException {
		return RobotFormValidationTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes();
	}

	private static byte[] png(int width, int height) throws IOException {
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", png);
		return png.toByteArray();
	}

	private static long robotsSaved() {
		return Database.read(em -> em.createQuery("select count(o) from Opponent o", Long.class).getSingleResult());
	}

	@Test
	void theFilesOfANewRobotAreAllNeeded() throws Exception {
		String page = robot("novo", "Vamos", null, null, null, null, null, new String[] { "Ana" }, new String[] { "ana@example.com" }).post(client(), url("new"));

		assertTrue(page.contains("Envie o arquivo .jar do robô!"), page);
		assertTrue(page.contains("Envie o arquivo .java do robô!"), page);
		assertTrue(page.contains("Envie a música do time!"), page);
		assertTrue(page.contains("Envie o logotipo do time!"), page);
	}

	@Test
	void theJarMustHaveContentAndTheRightName() throws Exception {
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[0], java(), music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("Arquivo .jar inválido!"));
		assertTrue(robot("novo", "Vamos", "Bot.jar", new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("não está no formato correto"));
	}

	@Test
	void theTeamAndRobotNamesHaveOnlyLettersAndDigits() throws Exception {
		assertTrue(robot("novo", "Vamos", "te-am.Bot_1.0.jar", new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("O nome do time pode ter APENAS"));
		assertTrue(robot("novo", "Vamos", "time.Bo-t_1.0.jar", new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("O nome do robô pode ter APENAS"));
		String longName = "A".repeat(101);
		assertTrue(robot("novo", "Vamos", "time." + longName + "_1.0.jar", new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("O nome do robô pode ter APENAS"));
	}

	@Test
	void theJavaFileMustHaveContentAndBeSmallEnough() throws Exception {
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, new byte[0], music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("Arquivo .java inválido!"));
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, new byte[1024 * 1024 + 1], music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("Arquivo .java muito grande!"));
	}

	@Test
	void theMusicMustBeEmptyFreeAndPlayable() throws Exception {
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), new byte[0], png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("Arquivo música inválido!"));
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), new byte[1024 * 1024 + 1], png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("Arquivo música muito grande!"));
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), "isso não é áudio".getBytes(StandardCharsets.UTF_8), png(64, 48), new String[] { "Ana" },
				new String[] { "ana@example.com" }).post(client(), url("new")).contains("Não foi possível tocar a música"));
	}

	@Test
	void theLogoMustBeAnImageOfTheRightSize() throws Exception {
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), music(), new byte[0], new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("Arquivo de logotipo inválido!"));
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), music(), new byte[1024 * 1024 + 1], new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("Arquivo de logotipo muito grande!"));
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), music(), png(700, 100), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("A largura do logotipo é maior que 640 pixels!"));
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), music(), png(100, 500), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("A altura do logotipo é maior que 480 pixels!"));
	}

	@Test
	void theLoginIsNeededAndCanBeUsedOnce() throws Exception {
		assertTrue(valid("", "time.Bot_1.0.jar").post(client(), url("new")).contains("Preencha o usuário"));
		assertTrue(valid("dono", "outro.Bot_1.0.jar").post(client(), url("new")).contains("Este login já existe"));
	}

	@Test
	void theRobotNameCanBeUsedOnce() throws Exception {
		assertTrue(valid("outro", "dono.Robo_2.0.jar").post(client(), url("new")).contains("Já existe um robô chamado dono.Robo"));
	}

	@Test
	void theBattleCryIsNeededAndNotTooLong() throws Exception {
		assertTrue(robot("novo", "", "time.Bot_1.0.jar", new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("Informe seu lema"));
		String longCry = robot("novo", "x".repeat(Opponent.MAX_BATTLE_CRY_LENGTH + 1), "time.Bot_1.0.jar", new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "Ana" }, new String[] { "ana@example.com" })
				.post(client(), url("new"));
		assertTrue(longCry.contains("O lema pode ter no máximo"), longCry);
	}

	@Test
	void theDevelopersAreNeededWithNamesAndMails() throws Exception {
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), music(), png(64, 48), null, null).post(client(), url("new"))
				.contains("Informe ao menos uma pessoa desenvolvedora"));
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "Ana", "Bia" }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("Cada membro do time deve ter um e-mail"));
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "Ana" }, new String[] { "nao-e-email" })
				.post(client(), url("new")).contains("Informe e-mails válidos"));
		assertTrue(robot("novo", "Vamos", "time.Bot_1.0.jar", new byte[] { 1 }, java(), music(), png(64, 48), new String[] { "  " }, new String[] { "ana@example.com" })
				.post(client(), url("new")).contains("Informe nomes válidos"));
	}

	@Test
	void whileTheRegistrationIsClosedNoRobotIsAccepted() throws Exception {
		Configuration.setEnabledRegistration(false);
		try {
			String form = MultipartForm.follow(client(), client().send(HttpRequest.newBuilder(URI.create(url("new"))).build(), HttpResponse.BodyHandlers.ofString()));
			assertTrue(form.contains("Cadastro de novos robôs bloqueado!!"), form);

			assertTrue(valid("novo", "time.Bot_1.0.jar").post(client(), url("new")).contains("Cadastro de novos robôs bloqueado!"));
		} finally {
			Configuration.setEnabledRegistration(true);
		}
	}

	@Test
	void noInvalidFormSavesARobot() {
		assertEquals(1, robotsSaved());
	}
}
