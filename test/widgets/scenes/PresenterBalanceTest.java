package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** The presenters share the screen during a battle: the ones with much to say do not take it from the others. */
class PresenterBalanceTest {

	@Test
	void everyoneIsEqualBeforeAnyoneAppears() {
		PresenterBalance balance = new PresenterBalance();
		balance.offer(Topic.BLOWS);
		balance.offer(Topic.FOULS);

		assertEquals(1, balance.factor(Topic.BLOWS), 1e-9);
		assertEquals(1, balance.factor(Topic.FOULS), 1e-9);
	}

	@Test
	void aPresenterWhoWasOnMoreIsToldLessAndOneWhoWasOnLessIsToldMore() {
		PresenterBalance balance = new PresenterBalance();
		balance.offer(Topic.BLOWS);
		balance.offer(Topic.FOULS);
		for (int i = 0; i < 3; i++) {
			balance.appeared(Topic.BLOWS);
		}
		balance.appeared(Topic.FOULS);
		balance.appeared(Topic.PACE);
		balance.appeared(Topic.AIM);

		// Last on the screen is the one who just came: it is also less welcome right away
		assertTrue(balance.factor(Topic.BLOWS) < 1);
		assertTrue(balance.factor(Topic.HEALTH) > 1);
		assertEquals(3, balance.appearances(Topic.BLOWS));
	}

	@Test
	void theSamePresenterIsHalfAsWelcomeTwiceInARow() {
		PresenterBalance balance = new PresenterBalance();
		balance.offer(Topic.BLOWS);
		balance.offer(Topic.AIM);
		balance.appeared(Topic.BLOWS);
		balance.appeared(Topic.AIM);

		// Both were on the screen once, but the last one was the aim
		assertEquals(1, balance.factor(Topic.BLOWS), 1e-9);
		assertEquals(0.5, balance.factor(Topic.AIM), 1e-9);
	}

	/** Plays a battle where the blows have far more to say than the others, with the chance of the arena, with or without the balance. */
	private static Map<Topic, Integer> battle(boolean balanced, long seed) {
		Topic[] events = { Topic.BLOWS, Topic.BLOWS, Topic.BLOWS, Topic.BLOWS, Topic.AIM, Topic.AIM, Topic.AIM, Topic.FOULS, Topic.HEALTH, Topic.PACE };
		Random random = new Random(seed);
		PresenterBalance balance = new PresenterBalance();
		Map<Topic, Integer> shown = new EnumMap<Topic, Integer>(Topic.class);
		for (int index = 0; index < 400; index++) {
			Topic topic = events[random.nextInt(events.length)];
			balance.offer(topic);
			double chance = balanced ? Math.min(1, 0.7 * balance.factor(topic)) : 0.7;
			if (random.nextDouble() < chance) {
				balance.appeared(topic);
				shown.merge(topic, 1, Integer::sum);
			}
		}
		return shown;
	}

	@Test
	void theBalanceKeepsTheBusiestPresenterCloseToTheOthers() {
		double without = 0;
		double with = 0;
		for (long seed = 1; seed <= 20; seed++) {
			Map<Topic, Integer> plain = battle(false, seed);
			Map<Topic, Integer> kept = battle(true, seed);
			without += (double) plain.get(Topic.BLOWS) / plain.get(Topic.PACE);
			with += (double) kept.get(Topic.BLOWS) / kept.get(Topic.PACE);
		}

		// Without the balance the blows are on the screen about four times as much as the pace; with it, about as much
		assertTrue(without / 20 > 3, "without the balance: " + without / 20);
		assertTrue(with / 20 < 1.8, "with the balance: " + with / 20);
	}
}
