package tournament;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.Championship;
import model.MatchResult;
import model.Opponent;
import support.TestConfiguration;
import tournament.OpponentService.AuthorData;
import tournament.OpponentService.RobotData;
import utils.configuration.Configuration;
import utils.database.Database;
import utils.web.PasswordHasher;

class OpponentServiceTest {

	@BeforeEach
	void emptyDatabase() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		TestConfiguration.freshDatabase();
	}

	@AfterAll
	static void closeTheDatabase() {
		Database.close();
	}

	private static byte[] png(int width, int height) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
		return out.toByteArray();
	}

	private static byte[] tone() throws IOException {
		return OpponentServiceTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes();
	}

	/** A complete robot, with every file. */
	private static RobotData robot(String team, String name, String login) throws IOException {
		return new RobotData(team, name, login, "segredo", "Grito de guerra", 1.5, "class Robo {}", png(40, 30), tone(), new byte[] { 1, 2, 3 },
				List.of(new AuthorData(0, "Ada Lovelace", "123456", "ada@time.org")));
	}

	@Test
	void aNewRobotNeedsEverythingAndIsSavedWithTheHashOfThePassword() throws IOException {
		Opponent saved = OpponentService.save(0, robot("alpha", "Corners", "alpha-login"));

		Opponent read = OpponentService.all().get(0);
		assertEquals(saved.getId(), read.getId());
		assertEquals("alpha.Corners", read.getFullName());
		assertEquals("123456", read.getAuthors().get(0).getRegistration());
		assertEquals("alpha-login", read.getAuthors().get(0).getLogin());
		assertTrue(PasswordHasher.verify("segredo", read.getAuthors().get(0).getPassword()));

		RobotData withoutFiles = new RobotData("beta", "Walls", "beta-login", "segredo", "Grito", 0, "class Robo {}", null, null, null, robot("a", "b", "c").authors());
		assertThrows(IllegalArgumentException.class, () -> OpponentService.save(0, withoutFiles));
	}

	private static byte[] noise(int width, int height) throws IOException {
		java.util.Random random = new java.util.Random(3);
		BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int posY = 0; posY < height; posY++) {
			for (int posX = 0; posX < width; posX++) {
				picture.setRGB(posX, posY, random.nextInt());
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(picture, "png", out);
		return out.toByteArray();
	}

	@Test
	void aLogoAndASongLargerThanTheLimitAreFitInsteadOfRefused() throws IOException {
		ByteArrayOutputStream song = new ByteArrayOutputStream();
		for (int copy = 0; copy < 140; copy++) {
			song.write(tone());
		}
		byte[] bigSong = song.toByteArray();
		byte[] bigLogo = noise(1400, 1000);
		assertTrue(bigSong.length > 1024 * 1024 && bigLogo.length > 1024 * 1024);
		RobotData base = robot("alpha", "Corners", "alpha-login");

		Opponent saved = OpponentService.save(0, new RobotData(base.team(), base.name(), base.login(), base.password(), base.battleCry(), 3000, base.sourceCode(), bigLogo, bigSong, base.jar(), base.authors()));

		Opponent read = OpponentService.all().stream().filter(robot -> robot.getId() == saved.getId()).findFirst().orElseThrow();
		assertTrue(read.getMusic().length <= 1024 * 1024);
		assertTrue(read.getMusic().length > bigSong.length / 1000);
		assertTrue(read.getImage().length <= 1024 * 1024);
		BufferedImage logo = ImageIO.read(new java.io.ByteArrayInputStream(read.getImage()));
		assertTrue(logo.getWidth() <= 640 && logo.getHeight() <= 480);
		// The start of the song was past what is kept, so it plays from the beginning
		assertEquals(0, read.getMusicStart());
	}

	@Test
	void theTeamAndTheRobotAreTheOnesTheJarHoldsWhateverWasTyped() throws IOException {
		RobotData base = robot("alpha", "Corners", "alpha-login");

		Opponent saved = OpponentService.save(0, new RobotData("x", "q", base.login(), base.password(), base.battleCry(), 0, base.sourceCode(), base.image(), base.music(),
				utils.robocode.RobotJarTest.jarOf("charlie.Walls"), base.authors()));

		assertEquals("charlie.Walls", saved.getFullName());
		assertEquals("charlie.Walls", OpponentService.all().get(0).getFullName());
	}

	@Test
	void aJarThatHoldsManyRobotsKeepsTheNamesAndIsCheckedAgainstThem() throws IOException {
		RobotData base = robot("alpha", "Corners", "alpha-login");
		java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bytes)) {
			for (String robot : List.of("one.A", "one.B")) {
				zip.putNextEntry(new java.util.zip.ZipEntry(robot.replace('.', '/') + ".properties"));
				zip.write(("robot.classname=" + robot).getBytes());
				zip.closeEntry();
			}
		}

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> OpponentService.save(0, new RobotData("x", "q", base.login(), base.password(), base.battleCry(), 0,
				base.sourceCode(), base.image(), base.music(), bytes.toByteArray(), base.authors())));

		assertTrue(refused.getMessage().contains("one.A"), refused.getMessage());
	}

	@Test
	void theNameOfTheRobotAndTheLoginBelongToASingleTeam() throws IOException {
		OpponentService.save(0, robot("alpha", "Corners", "alpha-login"));

		assertThrows(IllegalArgumentException.class, () -> OpponentService.save(0, robot("alpha", "Corners", "other-login")));
		assertThrows(IllegalArgumentException.class, () -> OpponentService.save(0, robot("beta", "Walls", "alpha-login")));
		OpponentService.save(0, robot("beta", "Walls", "beta-login"));
		assertEquals(2, OpponentService.all().size());
	}

	@Test
	void changingARobotKeepsTheFilesAndThePasswordThatWereNotSent() throws IOException {
		long id = OpponentService.save(0, robot("alpha", "Corners", "alpha-login")).getId();
		Opponent before = OpponentService.all().get(0);
		String hash = before.getAuthors().get(0).getPassword();
		long authorId = before.getAuthors().get(0).getId();

		RobotData change = new RobotData("alpha", "Corners2", "novo-login", "", "Outro grito", 2, "class Novo {}", null, null, null,
				List.of(new AuthorData(authorId, "Ada L.", "654321", "ada@time.org"), new AuthorData(0, "Alan Turing", "111", "alan@time.org")));
		OpponentService.save(id, change);

		Opponent after = OpponentService.all().get(0);
		assertEquals("alpha.Corners2", after.getFullName());
		assertEquals("Outro grito", after.getBattleCry());
		assertEquals("class Novo {}", after.getSourceCode());
		assertArrayEquals(before.getImage(), after.getImage());
		assertArrayEquals(before.getMusic(), after.getMusic());
		assertArrayEquals(before.getCompiledRobot(), after.getCompiledRobot());
		assertEquals(2, after.getAuthors().size());
		for (var author : after.getAuthors()) {
			assertEquals("novo-login", author.getLogin());
			assertEquals(hash, author.getPassword());
		}

		// A new password is hashed for the whole team
		OpponentService.save(id, new RobotData("alpha", "Corners2", "novo-login", "nova-senha", "Outro grito", 2, "class Novo {}", null, null, null, change.authors()));
		assertTrue(PasswordHasher.verify("nova-senha", OpponentService.all().get(0).getAuthors().get(0).getPassword()));
	}

	@Test
	void theRulesOfTheRegistrationHoldHere() throws IOException {
		RobotData base = robot("alpha", "Corners", "alpha-login");
		assertThrows(IllegalArgumentException.class, () -> OpponentService.save(0, new RobotData("al pha", base.name(), base.login(), base.password(), base.battleCry(), 0, base.sourceCode(), base.image(), base.music(), base.jar(), base.authors())));
		assertThrows(IllegalArgumentException.class, () -> OpponentService.save(0, new RobotData(base.team(), base.name(), base.login(), base.password(), "", 0, base.sourceCode(), base.image(), base.music(), base.jar(), base.authors())));
		assertThrows(IllegalArgumentException.class, () -> OpponentService.save(0, new RobotData(base.team(), base.name(), base.login(), base.password(), base.battleCry(), 0, base.sourceCode(), base.image(), new byte[] { 1, 2 }, base.jar(), base.authors())));
		assertThrows(IllegalArgumentException.class, () -> OpponentService.save(0, new RobotData(base.team(), base.name(), base.login(), base.password(), base.battleCry(), 0, base.sourceCode(), base.image(), base.music(), base.jar(),
				List.of(new AuthorData(0, "Ada", "12a", "ada@time.org")))));
		assertEquals(0, OpponentService.all().size());
	}

	@Test
	void aRobotThatIsInNoChampionshipIsJustRemoved() throws IOException {
		long id = OpponentService.save(0, robot("alpha", "Corners", "alpha-login")).getId();
		assertEquals(0, OpponentService.championshipsOf(id));

		OpponentService.delete(id);

		assertEquals(0, OpponentService.all().size());
	}

	@Test
	void removingARobotRemovesTheChampionshipsItIsDrawnInWithTheirResults() throws IOException {
		long first = OpponentService.save(0, robot("alpha", "Corners", "alpha-login")).getId();
		long second = OpponentService.save(0, robot("beta", "Walls", "beta-login")).getId();
		long third = OpponentService.save(0, robot("gama", "Fire", "gama-login")).getId();
		Championship championship = ChampionshipService.create(false, new Random(1));
		assertEquals(1, OpponentService.championshipsOf(first));

		Database.inTransaction(entityManager -> {
			MatchResult result = new MatchResult();
			result.setChampionship(entityManager.find(Championship.class, championship.getId()));
			result.setMatchCode("A-1");
			result.setWinner(entityManager.find(Opponent.class, first));
			result.setLoser(entityManager.find(Opponent.class, second));
			result.setWinnerScore(1);
			result.setLoserScore(0);
			result.setPlayedAt(LocalDateTime.now());
			entityManager.persist(result);
			return null;
		});

		OpponentService.delete(first);

		assertEquals(List.of(second, third), OpponentService.all().stream().map(Opponent::getId).toList());
		assertNull(ChampionshipService.current());
		assertEquals(0L, (long) Database.read(entityManager -> entityManager.createQuery("select count(r) from MatchResult r", Long.class).getSingleResult()));
	}
}
