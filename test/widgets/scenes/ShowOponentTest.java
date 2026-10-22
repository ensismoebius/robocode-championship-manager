package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import model.Author;
import model.Opponent;
import support.ChampionshipFixture;
import support.FxSupport;
import widgets.Confetti;

class ShowOponentTest {

	private static Opponent robot() throws IOException {
		Opponent opponent = ChampionshipFixture.opponents(1).get(0);
		opponent.setMusic(ShowOponentTest.class.getResourceAsStream("/fixtures/tone.mp3").readAllBytes());
		opponent.setSourceCode("public class Robo1 extends Robot {}");
		return opponent;
	}

	/** Fills the card with the robot on the JavaFX thread, the way the screens do. */
	private static ShowOponent card(Opponent opponent, String title, boolean champion) {
		ShowOponent card = FxSupport.run(ShowOponent::new);
		FxSupport.runVoid(() -> {
			try {
				card.setOponent(opponent, title, champion);
			} catch (IOException ioException) {
				throw new IllegalStateException(ioException);
			}
		});
		FxSupport.show(card);
		return card;
	}

	private static void playFor(ShowOponent card, long millis) {
		FxSupport.runVoid(card::play);
		FxSupport.sleep(millis);
		FxSupport.runVoid(card::stop);
	}

	@Test
	void theCardOfTheWinnerShowsTheRobotAndItsPrompt() throws IOException {
		Opponent winner = robot();
		Author author = new Author();
		author.setName("Ana Autora");
		winner.setAuthors(List.of(author));

		ShowOponent card = card(winner, "VENCEU!", false);
		playFor(card, 200);

		// background, source, the card, the scanlines and the prompt of the bracket
		assertEquals(5, FxSupport.run(() -> card.getChildren().size()));
		assertFalse(FxSupport.run(() -> card.getChildren().stream().anyMatch(child -> child instanceof Confetti)));
	}

	@Test
	void theCardOfTheChampionCelebratesWithConfetti() throws IOException {
		ShowOponent card = card(robot(), "CAMPEÃO!", true);
		playFor(card, 200);

		assertTrue(FxSupport.run(() -> card.getChildren().stream().anyMatch(child -> child instanceof Confetti)));
	}

	@Test
	void theCardCanBeSetWithoutTitleOrWithOne() throws IOException {
		Opponent plain = robot();
		Opponent titled = robot();
		ShowOponent card = FxSupport.run(ShowOponent::new);

		FxSupport.runVoid(() -> {
			try {
				card.setOponent(plain);
				card.setOponent(titled, "Vencedor");
			} catch (IOException ioException) {
				throw new IllegalStateException(ioException);
			}
		});

		// The card has its parts: the picture, the name and the rest of the robot
		assertTrue(FxSupport.run(() -> card.getChildren().size()) > 0);
	}

	@Test
	void theCardWithoutTitleHasNoPrompt() throws IOException {
		ShowOponent card = card(robot(), null, false);
		playFor(card, 100);

		// background, source, the card and the scanlines
		assertEquals(4, FxSupport.run(() -> card.getChildren().size()));
	}

	@Test
	void aCardOfARobotWithoutPictureOrSourceStillShows() throws IOException {
		Opponent bare = robot();
		bare.setImage(null);
		bare.setSourceCode(null);

		ShowOponent card = card(bare, null, false);

		assertFalse(FxSupport.run(() -> card.getChildren().isEmpty()));
	}
}
