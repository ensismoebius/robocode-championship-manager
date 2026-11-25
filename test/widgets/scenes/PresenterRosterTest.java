package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** In a championship every presenter is on the screen the same number of times, whatever the battles have to say. */
class PresenterRosterTest {

	/** The difference between who was on the screen the most and who was on the least. */
	private static int spread(PresenterRoster roster) {
		int most = 0;
		int least = Integer.MAX_VALUE;
		for (Topic topic : Topic.values()) {
			most = Math.max(most, roster.appearances(topic));
			least = Math.min(least, roster.appearances(topic));
		}
		return most - least;
	}

	@Test
	void theOwnPresenterSaysTheCommentWhileNobodyWasOnLess() {
		PresenterRoster roster = new PresenterRoster();
		assertEquals(Topic.BLOWS, roster.speaker(Topic.BLOWS));
		roster.appeared(Topic.BLOWS);

		// Dandara was on once, the others never: another blow goes to the next one who was not on yet
		Topic next = roster.speaker(Topic.BLOWS);
		assertNotEquals(Topic.BLOWS, next);
		assertEquals(0, roster.appearances(next));
		// A topic whose presenter was not on yet is said by its own presenter
		assertEquals(Topic.STATS, roster.speaker(Topic.STATS));
	}

	@Test
	void inAWholeChampionshipNobodyIsOnMoreThanOnceMoreThanTheOthers() {
		// The blows have far more to say than the rest, and the stats almost nothing
		Topic[] events = { Topic.BLOWS, Topic.BLOWS, Topic.BLOWS, Topic.BLOWS, Topic.BLOWS, Topic.AIM, Topic.AIM, Topic.SHOW, Topic.SHOW, Topic.FOULS, Topic.HEALTH, Topic.PACE };
		Random random = new Random(7);
		String saved = null;
		int total = 0;
		for (int battle = 0; battle < 15; battle++) {
			// Each battle goes on from the count saved with the championship
			PresenterRoster roster = PresenterRoster.parse(saved);
			int comments = 10 + random.nextInt(30);
			for (int index = 0; index < comments; index++) {
				roster.appeared(roster.speaker(events[random.nextInt(events.length)]));
				assertTrue(spread(roster) <= 1);
			}
			total += comments;
			saved = roster.format();
		}
		PresenterRoster end = PresenterRoster.parse(saved);
		int sum = 0;
		for (Topic topic : Topic.values()) {
			sum += end.appearances(topic);
		}
		assertEquals(total, sum);
		assertTrue(spread(end) <= 1);
		// When the comments are a multiple of the presenters, the count is exactly the same for all of them
		PresenterRoster even = new PresenterRoster();
		for (int index = 0; index < Topic.values().length * 4; index++) {
			even.appeared(even.speaker(Topic.BLOWS));
		}
		assertEquals(0, spread(even));
	}

	@Test
	void theSamePresenterDoesNotComeTwiceInARowWhenAnotherCan() {
		PresenterRoster roster = new PresenterRoster();
		Topic last = null;
		for (int index = 0; index < 50; index++) {
			Topic speaker = roster.speaker(Topic.AIM);
			assertNotEquals(last, speaker);
			roster.appeared(speaker);
			last = speaker;
		}
	}

	@Test
	void aBrokenOrOldCountIsReadAsWellAsItCanBe() {
		PresenterRoster roster = PresenterRoster.parse("BLOWS=3;NOBODY=9;AIM=x;FOULS;SHOW=2");
		assertEquals(3, roster.appearances(Topic.BLOWS));
		assertEquals(2, roster.appearances(Topic.SHOW));
		assertEquals(0, roster.appearances(Topic.AIM));
		assertEquals(roster.format(), PresenterRoster.parse(roster.format()).format());
		assertEquals(0, PresenterRoster.parse(null).appearances(Topic.BLOWS));
	}
}
