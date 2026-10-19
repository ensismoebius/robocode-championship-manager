package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import utils.robocode.BattleRecording;
import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;
import widgets.scenes.BattleCommentary.Headline;
import widgets.scenes.BattleCommentary.Message;

/**
 * Robot 0 (Walls) against robot 1 (SpinBot)
 */
class BattleCommentaryTest {

	private BattleCommentary commentary;
	private Turn previous;
	private int turnNumber;

	@BeforeEach
	void setUp() {
		this.commentary = new BattleCommentary(Map.of(0, "Walls", 1, "SpinBot"), new Random(1));
		this.commentary.startRound();
		this.previous = null;
		this.turnNumber = 0;
	}

	/**
	 * The next turn, robots at 200 units from each other
	 */
	private List<Message> turn(double energy0, double energy1, Bullet... bullets) {
		Turn turn = new Turn(0, ++this.turnNumber, List.of(robot(0, 100, energy0), robot(1, 300, energy1)), List.of(bullets));
		List<Message> messages = this.commentary.turn(turn, this.previous);
		this.previous = turn;
		return messages;
	}

	private static RobotState robot(int index, double x, double energy) {
		return new RobotState(index, x, 100, 0, 0, 0, Math.max(0, energy), energy <= 0 ? BattleRecording.ROBOT_DEAD : 0);
	}

	private static Bullet hit(int id, int owner, double power) {
		return new Bullet(id, owner, 0, 0, power, BattleRecording.BULLET_HIT_VICTIM, 1 - owner);
	}

	private static List<String> callouts(List<Message> messages) {
		return messages.stream().filter(Message::callout).map(Message::text).toList();
	}

	@Test
	void firstHitOnlyOnce() {
		assertEquals(List.of("PRIMEIRO HIT!"), callouts(this.turn(100, 96, hit(1, 0, 1))));
		assertEquals(List.of(), callouts(this.turn(100, 92, hit(2, 0, 1))));
		Message first = this.turn(96, 92, hit(3, 1, 1)).stream().filter(m -> !m.callout()).findFirst().orElse(null);
		// The first hit was told already
		assertNull(first);
	}

	@Test
	void criticalDamageIsTheStrongestBullet() {
		this.turn(100, 96, hit(1, 0, 1));
		assertEquals(List.of(), callouts(this.turn(100, 90, hit(2, 1, 2.5))));
		assertEquals(List.of("DANO CRÍTICO!"), callouts(this.turn(84, 90, hit(3, 1, 3))));
	}

	@Test
	void dangerWhenOneMoreBulletKills() {
		this.turn(100, 20);
		assertEquals(List.of("PERIGO!"), callouts(this.turn(100, 15)));
		assertEquals(0, this.turn(100, 10).size());
	}

	@Test
	void comboOfConsecutiveHits() {
		this.turn(100, 96, hit(1, 0, 1));
		this.turn(100, 92, hit(2, 0, 1));
		assertTrue(callouts(this.turn(100, 88, hit(3, 0, 1))).contains("COMBO x3!"));
		// The victim hits back, the combo ends
		this.turn(96, 88, hit(4, 1, 1));
		assertEquals(List.of(), callouts(this.turn(96, 84, hit(5, 0, 1))));
	}

	@Test
	void replayedTurnsSayNothing() {
		this.turn(100, 96, hit(1, 0, 1));
		Turn again = new Turn(0, 1, List.of(robot(0, 100, 100), robot(1, 300, 96)), List.of(hit(1, 0, 1)));
		assertEquals(0, this.commentary.turn(again, null).size());
	}

	@Test
	void bulletsHittingEachOtherSaidOnce() {
		List<Message> messages = this.turn(100, 100, new Bullet(1, 0, 0, 0, 1, BattleRecording.BULLET_HIT_BULLET, -1),
				new Bullet(2, 1, 0, 0, 1, BattleRecording.BULLET_HIT_BULLET, -1));
		assertEquals(List.of("TIROS ANULADOS!"), callouts(messages));
	}

	@Test
	void comebackWhenTheWinnerWasFarBehind() {
		this.turn(40, 90);
		this.turn(40, 0);
		assertTrue(callouts(this.commentary.roundEnd(0)).contains("VITÓRIA DE VIRADA!"));
	}

	@Test
	void perfectWhenNeverHit() {
		this.turn(100, 96, hit(1, 0, 1));
		this.turn(100, 0);
		List<Message> messages = this.commentary.roundEnd(0);
		assertTrue(callouts(messages).contains("PERFECT!"));
		// The accuracy of the round
		assertTrue(messages.stream().anyMatch(m -> m.text().startsWith("Precisão no round:")));
	}

	@Test
	void doubleKnockOut() {
		this.turn(10, 10);
		assertTrue(callouts(this.turn(0, 0)).contains("DUPLO K.O.!"));
	}

	private Headline battle(int... winners) {
		for (int i = 0; i < winners.length; i++) {
			this.commentary.startRound();
			if (i == winners.length - 1) {
				this.commentary.lastRoundStarts();
			}
			this.commentary.roundEnd(winners[i]);
		}
		return this.commentary.battleEnd();
	}

	@Test
	void comebackBattle() {
		Headline headline = this.battle(1, 0, 0);
		assertEquals("VIRADA HISTÓRICA!", headline.title());
		assertEquals("Walls vence a batalha de virada por 2 a 1", headline.subtitle());
	}

	@Test
	void allRoundsWon() {
		assertEquals("VITÓRIA ESMAGADORA!", this.battle(0, 0, 0).title());
	}

	@Test
	void decidedInTheLastRound() {
		assertEquals("DECIDIDO NO ÚLTIMO ROUND!", this.battle(0, 1, 0).title());
	}

	@Test
	void ordinaryBattlesHaveNoHeadline() {
		assertNull(this.battle(0, 0, 1));
		this.setUp();
		// One round only
		assertNull(this.battle(0));
	}

	@Test
	void namesInTheComments() {
		List<String> comments = new ArrayList<String>();
		for (Message message : this.turn(100, 96, hit(1, 0, 1))) {
			if (!message.callout()) {
				comments.add(message.text());
			}
		}
		assertEquals(1, comments.size());
		assertTrue(comments.get(0).contains("Walls"), comments.get(0));
	}
}
