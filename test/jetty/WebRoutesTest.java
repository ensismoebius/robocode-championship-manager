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
import support.MultipartForm;
import support.TestConfiguration;
import utils.configuration.Configuration;
import utils.database.Database;

/** The pages and the files the web server serves besides the robot form. */
class WebRoutesTest {

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

	/** A response without following the redirects, so the status and the location can be checked. */
	private static HttpResponse<String> get(HttpClient client, String path) throws IOException, InterruptedException {
		return client.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofString());
	}

	private static byte[] png() throws IOException {
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB), "png", png);
		return png.toByteArray();
	}

	@Test
	void theStylesheetOfTheGameIsServed() throws Exception {
		HttpResponse<String> response = get(HttpClient.newHttpClient(), "/style.css");

		assertEquals(200, response.statusCode());
		assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/css"));
		assertTrue(response.body().contains("{"));
	}

	@Test
	void theFontsOfTheGameAreServedAndOthersAreNot() throws Exception {
		HttpResponse<String> font = get(HttpClient.newHttpClient(), "/fonts/Rajdhani-Bold.ttf");
		assertEquals(200, font.statusCode());
		assertEquals("font/ttf", font.headers().firstValue("Content-Type").orElse(""));

		assertEquals(404, get(HttpClient.newHttpClient(), "/fonts/Nope.ttf").statusCode());
	}

	@Test
	void aLoggedTeamIsSentToItsEditorWhenItOpensTheLogin() throws Exception {
		HttpClient session = MultipartForm.session();
		byte[] music = WebRoutesTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes();
		String saved = new MultipartForm().field("login", "time9").field("password", "pass1").field("teamWarcry", "Vamos").field("name[]", "Ana")
				.field("email[]", "ana@example.com").file("robotJarFile", "time9.Bot_1.0.jar", new byte[] { 1 })
				.file("robotJavaFile", "Bot.java", "class Bot {}".getBytes(StandardCharsets.UTF_8)).file("teamMusic", "song.mp3", music).file("teamLogo", "logo.png", png())
				.post(session, base + "/robotEditor?action=new");
		assertTrue(saved.contains("Robô salvo com sucesso"), saved);

		HttpResponse<String> login = get(session, "/");

		assertEquals(302, login.statusCode());
		assertEquals("/robotEditor?action=edit", login.headers().firstValue("Location").orElse(""));
	}
}
