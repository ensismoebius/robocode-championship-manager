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
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jetty.server.HttpServer;
import support.MultipartForm;
import support.TestConfiguration;
import utils.configuration.Configuration;
import utils.database.Database;

/** Where the robot editor sends each request: to the editor, to the start page, or saving the robot of the team logged in. */
class RobotEditorRoutesTest {

	private static HttpServer server;
	private static String base;

	@BeforeAll
	static void startTheServer() throws Exception {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
		server = new HttpServer(0);
		base = "http://localhost:" + server.getPort();
	}

	@AfterAll
	static void stopTheServer() {
		server.stop();
		Database.close();
	}

	/** A request without a body, without following its redirects, so the status and the location can be checked. */
	private static HttpResponse<String> get(HttpClient client, String path) throws IOException, InterruptedException {
		return client.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofString());
	}

	private static HttpResponse<String> post(HttpClient client, String path) throws IOException, InterruptedException {
		return client.send(HttpRequest.newBuilder(URI.create(base + path)).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
	}

	private static void assertRedirect(HttpResponse<String> response, String location) {
		assertEquals(302, response.statusCode());
		assertEquals(location, response.headers().firstValue("Location").orElse(""));
	}

	/** A robot form with everything right, and the password and the battle cry given by the test. */
	private static MultipartForm form(String login, String password, String warcry, String jarName) throws IOException {
		return new MultipartForm().field("login", login).field("password", password).field("teamWarcry", warcry).field("name[]", "Ana").field("email[]", "ana@example.com").field("registration[]", "12345")
				.file("robotJarFile", jarName, new byte[] { 1 }).file("robotJavaFile", "MyBot.java", "class MyBot {}".getBytes(StandardCharsets.UTF_8))
				.file("teamMusic", "song.mp3", RobotEditorRoutesTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes()).file("teamLogo", "logo.png", png());
	}

	private static byte[] png() throws IOException {
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB), "png", png);
		return png.toByteArray();
	}

	/** Saves a team with its robot and returns the session of the team, logged in. */
	private static HttpClient teamLoggedIn(String login, String jarName) throws Exception {
		HttpClient session = MultipartForm.session();
		String saved = form(login, "pass1", "Vamos", jarName).post(session, base + "/robotEditor?action=new");
		assertTrue(saved.contains("Robô salvo com sucesso"), saved);
		return session;
	}

	@Test
	void aVisitorIsSentHomeWhenNoActionIsAskedOrTheTeamIsNotLoggedIn() throws Exception {
		HttpClient visitor = HttpClient.newHttpClient();

		assertRedirect(get(visitor, "/robotEditor"), "/");
		assertRedirect(get(visitor, "/robotEditor?action=edit"), "/");
	}

	@Test
	void theLogoutEndsTheSession() throws Exception {
		HttpClient session = teamLoggedIn("sair", "sair.Bot_1.0.jar");

		assertRedirect(get(session, "/robotEditor?action=logout"), "/");
		assertRedirect(get(session, "/robotEditor?action=edit"), "/");
	}

	@Test
	void aTeamThatIsLoggedInAsksForANewRobotAndIsSentToItsEditor() throws Exception {
		HttpClient session = teamLoggedIn("novo", "novo.Bot_1.0.jar");

		assertRedirect(get(session, "/robotEditor?action=new"), "/robotEditor?action=edit");
	}

	@Test
	void aPostWithoutAKnownActionIsSentHome() throws Exception {
		HttpClient visitor = HttpClient.newHttpClient();

		assertRedirect(post(visitor, "/robotEditor?action=nope"), "/");
		assertRedirect(post(visitor, "/robotEditor?action=edit"), "/");
	}

	@Test
	void aTeamLoggedInEditsItsRobotWhateverTheActionOfTheForm() throws Exception {
		HttpClient session = teamLoggedIn("edita", "edita.Bot_1.0.jar");

		// The form says new, but the team is logged in: its robot is the one saved
		String saved = form("edita", "pass1", "Novo lema", "edita.Bot_1.0.jar").post(session, base + "/robotEditor?action=new");

		assertTrue(saved.contains("Robô salvo com sucesso"), saved);
		assertTrue(saved.contains("Novo lema"), saved);
	}

	@Test
	void aTeamWhoseRobotWasRemovedIsSentHome() throws Exception {
		HttpClient session = teamLoggedIn("sumiu", "sumiu.Bot_1.0.jar");
		Database.inTransaction(entityManager -> entityManager.createQuery("delete from Opponent o where o.team = :team").setParameter("team", "sumiu").executeUpdate());

		assertRedirect(post(session, "/robotEditor?action=edit"), "/");
	}

	@Test
	void aNewTeamNeedsAPassword() throws Exception {
		String page = form("sem.senha", "", "Vamos", "sem.Bot_1.0.jar").post(HttpClient.newHttpClient(), base + "/robotEditor?action=new");

		assertTrue(page.contains("Preencha a senha"), page);
	}

	@Test
	void anUnexpectedProblemIsShownOnTheEditor() throws Exception {
		Path blocker = Path.of("target", "test-home", "editor-blocker");
		Files.createDirectories(blocker.getParent());
		Files.writeString(blocker, "um arquivo, não uma pasta");
		try {
			// The database can not be read: checking the login of the form fails
			TestConfiguration.load(Configuration.RANDOM, "/nowhere", blocker.resolve("robochamp.db").toString());
			Database.close();

			String page = form("falha", "pass1", "Vamos", "falha.Bot_1.0.jar").post(HttpClient.newHttpClient(), base + "/robotEditor?action=new");

			assertTrue(page.contains("Algum erro inesperado aconteceu"), page);
		} finally {
			TestConfiguration.load(Configuration.RANDOM, "/nowhere");
			TestConfiguration.freshDatabase();
		}
	}
}
