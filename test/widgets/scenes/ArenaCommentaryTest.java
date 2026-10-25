package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.scene.control.Label;
import support.DialogFields;
import support.FxSupport;
import widgets.scenes.BattleCommentary.Message;
import widgets.scenes.BattleCommentary.Tone;

/** The commentary of the arena: the callouts on the side of the robots, and the comments with their presenter. */
class ArenaCommentaryTest {

	private static Message callout(String text, int robot, Tone tone) {
		return new Message(true, text, robot, tone, 2, Topic.AIM);
	}

	private static Message comment(String text, int priority) {
		return new Message(false, text, -1, Tone.NEUTRAL, priority, Topic.PACE);
	}

	/** How many labels of the arena show the text. */
	private static long labelsShowing(BattleArena3D arena, String text) {
		return FxSupport.run(() -> DialogFields.nodes(arena).stream().filter(node -> node instanceof Label label && text.equals(label.getText())).count());
	}

	/** The arena, playing the sample battle, so its commentary knows the robots. */
	private static BattleArena3D playingArena() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		FxSupport.runVoid(() -> arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
		}));
		return arena;
	}

	@Test
	void calloutsPopOnTheSideOfTheirRobotAndAreNotRepeated() {
		BattleArena3D arena = playingArena();

		FxSupport.runVoid(() -> arena.announcer.say(List.of(callout("SNIPER!", 0, Tone.PLAYER), callout("SNIPER!", 0, Tone.PLAYER), callout("POR UM FIO!", 1, Tone.DANGER),
				callout("GOLPE!", 9, Tone.PLAYER), callout("VIRADA!", 0, Tone.GOLD), callout("PERIGO!", 1, Tone.NEUTRAL))));

		// The same callout again pops again instead of being repeated
		assertEquals(1, labelsShowing(arena, "SNIPER!"));
		assertEquals(1, labelsShowing(arena, "POR UM FIO!"));
		assertEquals(1, labelsShowing(arena, "GOLPE!"));

		// A side holds three callouts at most: the oldest one leaves for the newest
		FxSupport.runVoid(() -> arena.announcer.say(List.of(callout("UM!", 0, Tone.PLAYER), callout("DOIS!", 0, Tone.PLAYER))));
		assertEquals(0, labelsShowing(arena, "SNIPER!"));
		assertEquals(1, labelsShowing(arena, "DOIS!"));
	}

	@Test
	void noCommentIsSaidWhileTheBattleIsReplayed() {
		BattleArena3D arena = playingArena();

		FxSupport.runVoid(() -> {
			arena.phase = Phase.REPLAY;
			arena.announcer.say(List.of(callout("REPLAY!", 0, Tone.PLAYER), comment("Isto já foi dito", 6)));
		});

		assertEquals(0, labelsShowing(arena, "REPLAY!"));
		assertEquals(0, labelsShowing(arena, "Isto já foi dito"));
	}

	@Test
	void aCommentComesWithItsPresenterAndWaitsForItsTurn() {
		BattleArena3D arena = playingArena();

		FxSupport.runVoid(() -> {
			arena.phase = Phase.PLAYING;
			// Priority 6 is always told, and no comment is told before the last one ends
			arena.announcer.say(List.of(comment("Primeiro comentário", 6)));
			arena.announcer.say(List.of(comment("Segundo comentário", 6)));
			arena.announcer.tick(0.1);
		});

		assertEquals(1, labelsShowing(arena, "Primeiro comentário"));
		assertEquals(0, labelsShowing(arena, "Segundo comentário"));

		// The first one ends and the second one is too late for its turn: it is dropped
		FxSupport.runVoid(() -> arena.announcer.tick(3.5));
		assertEquals(0, labelsShowing(arena, "Segundo comentário"));
	}

	@Test
	void aCommentThatIsNotImportantEnoughIsNotTold() {
		BattleArena3D arena = playingArena();

		// Priority 0 is never told: there is no comment to say it in the first place
		FxSupport.runVoid(() -> arena.announcer.say(List.of(comment("Ninguém ouve isto", 0))));

		assertEquals(0, labelsShowing(arena, "Ninguém ouve isto"));
	}

	@Test
	void theAnnouncerSaysAMomentOfTheGame() {
		BattleArena3D arena = playingArena();

		// Long after the last comment, the presenter of the round says its line
		FxSupport.runVoid(() -> {
			arena.announcer.tick(20);
			arena.announcer.announce("Round um, FIGHT!", 6);
		});

		assertEquals(1, labelsShowing(arena, "Round um, FIGHT!"));
	}

	@Test
	void theWaitingCommentsDoNotInterruptTheOneOnScreen() {
		BattleArena3D arena = playingArena();

		FxSupport.runVoid(() -> {
			arena.announcer.say(List.of(comment("Em cena", 6)));
			// Four more while the first one is on: they wait, the queue keeps the three most important
			arena.announcer.say(List.of(comment("Fila 1", 3), comment("Fila 2", 4), comment("Fila 3", 5), comment("Fila 4", 1)));
		});
		assertEquals(1, labelsShowing(arena, "Em cena"));
		assertEquals(0, labelsShowing(arena, "Fila 3"));

		FxSupport.runVoid(() -> {
			arena.announcer.clearQueue();
			arena.announcer.tick(3.5);
		});
		assertEquals(1, labelsShowing(arena, "Em cena"));
		assertEquals(0, labelsShowing(arena, "Fila 3"));
	}
}
