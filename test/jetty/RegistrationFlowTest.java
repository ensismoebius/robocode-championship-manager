package jetty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import jetty.server.HttpServer;
import model.Opponent;
import model.Championship;
import tournament.Bracket;
import tournament.Match;
import tournament.Status;
import tournament.ChampionshipService;
import utils.configuration.Configuration;
import utils.configuration.INewConfigurationHandle;
import utils.database.Database;

/**
 * Registers, logs in and edits a team through the real web server and database
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegistrationFlowTest {

	private static HttpServer server;
	private static String base;
	private static byte[] logo;

	private static Path databaseFile;
	private static byte[] music;

	@BeforeAll
	static void start() throws Exception {
		Files.createDirectories(Path.of(System.getProperty("user.home")));
		databaseFile = Files.createTempDirectory(Path.of(System.getProperty("user.home")), "db").resolve("sub/dir/robochamp.db");
		music = RegistrationFlowTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes();

		Configuration.setNewConfigurationHandle(old -> {
			Properties p = new Properties();
			p.setProperty(INewConfigurationHandle.DATABASE_FILE, databaseFile.toString());
			p.setProperty(INewConfigurationHandle.ROBOCODE_PATH, "/nowhere");
			p.setProperty(INewConfigurationHandle.REGISTRATION_ENABLED, "true");
			return p;
		});
		assertTrue(Configuration.askForProperties());

		server = new HttpServer(0);
		base = "http://localhost:" + server.getPort();

		BufferedImage image = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(image, "png", png);
		logo = png.toByteArray();
	}

	@AfterAll
	static void stop() {
		server.stop();
		Database.close();
	}

	private static HttpClient newClient() {
		return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build();
	}

	/**
	 * Follows redirects like a browser: always with a plain GET
	 */
	private static String send(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		while (response.statusCode() / 100 == 3) {
			URI location = response.request().uri().resolve(response.headers().firstValue("Location").orElseThrow());
			response = client.send(HttpRequest.newBuilder(location).build(), HttpResponse.BodyHandlers.ofString());
		}
		return response.body();
	}

	private static String get(HttpClient client, String path) throws IOException, InterruptedException {
		return send(client, HttpRequest.newBuilder(URI.create(base + path)).build());
	}

	private static String login(HttpClient client, String login, String password) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/")).header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString("login=" + login + "&password=" + password)).build();
		return send(client, request);
	}

	/**
	 * Minimal multipart/form-data encoder
	 */
	private static class Form {
		private final String boundary = "----" + UUID.randomUUID();
		private final List<byte[]> parts = new ArrayList<byte[]>();

		Form field(String name, String value) {
			this.parts.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
			return this;
		}

		Form file(String name, String fileName, byte[] content) {
			this.parts.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName
					+ "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
			this.parts.add(content);
			this.parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
			return this;
		}

		String post(HttpClient client, String action) throws IOException, InterruptedException {
			ByteArrayOutputStream body = new ByteArrayOutputStream();
			for (byte[] part : this.parts) {
				body.writeBytes(part);
			}
			body.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

			HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/robotEditor?action=" + action)).header("Content-Type", "multipart/form-data; boundary=" + boundary)
					.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
			return send(client, request);
		}
	}

	private static Form newRobotForm(String login, String jarName) {
		return new Form().field("login", login).field("password", "pass1").field("teamWarcry", "Vamos <script>alert(1)</script>").field("name[]", "Ana")
				.field("email[]", "ana@example.com").file("robotJarFile", jarName, new byte[] { 1, 2, 3 }).file("robotJavaFile", "MyBot.java", "class MyBot {}".getBytes())
				.file("teamMusic", "song.mp3", music).file("teamLogo", "logo.png", logo);
	}

	private static long count(String entity) {
		Long count = Database.read(em -> em.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult());
		return count;
	}

	private static Opponent findOpponent(String team) {
		return Database.read(em -> em.createQuery("select o from Opponent o where o.team = :team", Opponent.class).setParameter("team", team).getSingleResult());
	}

	@Test
	@Order(1)
	void newRobotFormShowsOneAuthorRow() throws Exception {
		String page = get(newClient(), "/robotEditor?action=new");
		assertTrue(page.contains("name=\"name[]\""), page);
		assertTrue(page.contains("charset=\"utf-8\""), page);
		assertTrue(page.contains("href=\"/style.css\""), page);

		// The login page tells if robots are accepted
		assertTrue(get(newClient(), "/").contains("Inscrições abertas"));
		Configuration.setEnabledRegistration(false);
		try {
			assertTrue(get(newClient(), "/").contains("Inscrições encerradas"));
		} finally {
			Configuration.setEnabledRegistration(true);
		}

		// The status changes at any time, browsers must not cache these pages
		HttpResponse<String> loginPage = newClient().send(HttpRequest.newBuilder(URI.create(base + "/")).build(), HttpResponse.BodyHandlers.ofString());
		assertEquals("no-store", loginPage.headers().firstValue("Cache-Control").orElse(""));
		HttpResponse<String> editorPage = newClient().send(HttpRequest.newBuilder(URI.create(base + "/robotEditor?action=new")).build(), HttpResponse.BodyHandlers.ofString());
		assertEquals("no-store", editorPage.headers().firstValue("Cache-Control").orElse(""));
		assertTrue(get(newClient(), "/style.css").contains("--rc-bg"));
	}

	@Test
	@Order(1)
	void servesOnlyTheBundledFonts() throws Exception {
		HttpClient client = newClient();
		HttpResponse<byte[]> font = client.send(HttpRequest.newBuilder(URI.create(base + "/fonts/PressStart2P-Regular.ttf")).build(), HttpResponse.BodyHandlers.ofByteArray());
		assertEquals(200, font.statusCode());
		assertEquals("font/ttf", font.headers().firstValue("Content-Type").orElse(""));
		assertTrue(font.body().length > 1000);

		HttpResponse<String> other = client.send(HttpRequest.newBuilder(URI.create(base + "/fonts/theme.css")).build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(404, other.statusCode());
	}

	@Test
	@Order(2)
	void registersNewTeam() throws Exception {
		String page = newRobotForm("team1", "team1.MyBot_1.0.jar").post(newClient(), "new");

		assertTrue(page.contains("Robô salvo com sucesso"), page);
		assertTrue(page.contains("team1.MyBot"), page);
		// Battle cry is escaped
		assertTrue(page.contains("&lt;script&gt;"), page);
		assertFalse(page.contains("<script>alert"), page);
		// The password is never sent back
		assertFalse(page.contains("pass1"), page);

		Opponent saved = findOpponent("team1");
		assertEquals("MyBot", saved.getName());
		assertArrayEquals(new byte[] { 1, 2, 3 }, saved.getCompiledRobot());
		assertTrue(saved.getAuthors().get(0).getPassword().startsWith("pbkdf2$"));
	}

	@Test
	@Order(3)
	void rejectsDuplicatedLoginAndRobotName() throws Exception {
		String page = newRobotForm("team1", "team2.Other_1.0.jar").post(newClient(), "new");
		assertTrue(page.contains("Este login já existe"), page);

		page = newRobotForm("someoneElse", "team1.MyBot_2.0.jar").post(newClient(), "new");
		assertTrue(page.contains("Já existe um robô chamado team1.MyBot"), page);

		page = newRobotForm("bad", "MyBot.jar").post(newClient(), "new");
		assertTrue(page.contains("não está no formato correto"), page);
	}

	@Test
	@Order(4)
	void logsInAndEditsKeepingFilesAndPassword() throws Exception {
		HttpClient client = newClient();

		assertTrue(login(client, "team1", "wrong").contains("Falha ao autenticar"));
		assertTrue(login(client, "team1", "pass1").contains("team1.MyBot"));

		// No files and no password: they are kept. Adds a second author
		String page = new Form().field("login", "team1").field("password", "").field("teamWarcry", "New cry").field("name[]", "Ana").field("email[]", "ana@example.com")
				.field("name[]", "Bia").field("email[]", "bia@example.com").post(client, "edit");
		assertTrue(page.contains("Robô salvo com sucesso"), page);

		Opponent saved = findOpponent("team1");
		assertEquals("New cry", saved.getBattleCry());
		assertEquals(2, saved.getAuthors().size());
		assertArrayEquals(new byte[] { 1, 2, 3 }, saved.getCompiledRobot());

		// Removing an author deletes it, so it can not be used to log in anymore
		page = new Form().field("login", "team1").field("password", "pass2").field("teamWarcry", "New cry").field("name[]", "Bia").field("email[]", "bia@example.com").post(client,
				"edit");
		assertTrue(page.contains("Robô salvo com sucesso"), page);
		assertEquals(1L, count("Author"));

		assertTrue(login(newClient(), "team1", "pass1").contains("Falha ao autenticar"));
		assertTrue(login(newClient(), "team1", "pass2").contains("team1.MyBot"));
	}

	@Test
	@Order(5)
	void logoutEndsTheSession() throws Exception {
		HttpClient client = newClient();
		login(client, "team1", "pass2");
		get(client, "/robotEditor?action=logout");
		assertTrue(get(client, "/robotEditor?action=edit").contains("Se você já cadastrou seu time"));
	}

	@Test
	@Order(6)
	void rejectsMusicThatCanNotBePlayed() throws Exception {
		String page = new Form().field("login", "noise").field("password", "x").field("teamWarcry", "x").field("name[]", "Ana").field("email[]", "ana@example.com")
				.file("robotJarFile", "noise.Bot_1.0.jar", new byte[] { 1 }).file("robotJavaFile", "Bot.java", "class Bot {}".getBytes())
				.file("teamMusic", "song.mp3", "not really a song".getBytes()).file("teamLogo", "logo.png", logo).post(newClient(), "new");
		assertTrue(page.contains("Não foi possível tocar a música"), page);
	}

	@Test
	@Order(7)
	void championshipIsSavedAndRebuilt() throws Exception {
		assertTrue(newRobotForm("team3", "team3.Bot_1.0.jar").post(newClient(), "new").contains("Robô salvo com sucesso"));
		assertTrue(newRobotForm("team4", "team4.Bot_1.0.jar").post(newClient(), "new").contains("Robô salvo com sucesso"));

		assertNull(ChampionshipService.current());
		Championship championship = ChampionshipService.create(true, new Random(7));
		assertEquals(3, championship.getSeeds().size());

		// Plays until the end, recording every result in the database
		Bracket bracket = ChampionshipService.bracketOf(ChampionshipService.current());
		while (bracket.getNextMatch().isPresent()) {
			Match next = bracket.getNextMatch().get();
			championship = ChampionshipService.recordResult(championship, next.getCode(), next.getA().getParticipant(), 3000, 1000);
			bracket = ChampionshipService.bracketOf(ChampionshipService.current());
		}
		assertTrue(bracket.isFinished());
		int results = ChampionshipService.current().getResults().size();
		assertEquals(results, bracket.getMatches().stream().filter(m -> m.getStatus() == Status.FINISHED).count());

		// Undo brings the last match back
		Championship undone = ChampionshipService.undoLastResult(ChampionshipService.current());
		assertEquals(results - 1, undone.getResults().size());
		assertFalse(ChampionshipService.bracketOf(ChampionshipService.current()).isFinished());

		// Results can not be recorded for a match that is not ready
		Championship current = ChampionshipService.current();
		assertThrows(IllegalStateException.class, () -> ChampionshipService.recordResult(current, "V1-1", current.getSeeds().get(0).getId(), null, null));

		// A new draw becomes the current championship
		Championship newer = ChampionshipService.create(false, new Random(8));
		assertEquals(newer.getId(), ChampionshipService.current().getId());
		assertTrue(ChampionshipService.current().getResults().isEmpty());
	}

	@Test
	@Order(8)
	void sqliteIsConfiguredForConcurrentAccess() {
		Object journal = Database.read(em -> em.createNativeQuery("PRAGMA journal_mode").getSingleResult());
		assertEquals("wal", journal.toString().toLowerCase());
		Object foreignKeys = Database.read(em -> em.createNativeQuery("PRAGMA foreign_keys").getSingleResult());
		assertEquals("1", foreignKeys.toString());
		assertTrue(Files.exists(databaseFile));
	}
}
