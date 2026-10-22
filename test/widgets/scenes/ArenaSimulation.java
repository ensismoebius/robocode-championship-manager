package widgets.scenes;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import model.Opponent;
import tournament.Bracket;
import utils.configuration.Configuration;
import utils.robocode.BattleRecording;
import support.FxSupport;
import support.TestConfiguration;

/**
 * Drives a {@link BattleArena3D} through a whole battle without a window: the
 * frames come from the arena's own timer, so every phase of the scene runs.
 */
public final class ArenaSimulation {

	/** The turns of a battle, copied from a real run of the BattleStarter. */
	public static final List<String> OUTPUT = List.of( //
			"#F 800 600 2", //
			"#N 0 sample.Walls", //
			"#N 1 sample.SpinBot", //
			"#R 0", //
			"#C 0 -16777216 -16777216 -14336", //
			"#C 1 -16776961 -16776961 -16777216", //
			"#T 0 0|0 603 477.9 0.627 0.627 0.627 100 0;1 358.1 273.8 2.972 2.972 2.972 100 0|", //
			"some robocode log line", //
			"#T 0 199|0 450 18 4.712 6.283 6.283 94 0;1 270.3 277.8 6.026 6.026 6.026 91 0|4 0 163.3 374.3 2 1 -1;10004 1 654 93.3 3 2 0", //
			"#E 0 1495", //
			"#R 1", //
			"#T 1 0|0 10 20 0 0 0 100 0;1 30 40 0 0 0 0 3|", //
			"#T broken line", //
			"1\tsample.Walls\t216", //
			"2\tsample.SpinBot\t0");

	private ArenaSimulation() {
	}

	/** The two robots of {@link #OUTPUT}, as the championship knows them. */
	public static List<Opponent> fighters() {
		return List.of(opponent("sample", "Walls"), opponent("sample", "SpinBot"));
	}

	static Opponent opponent(String team, String name) {
		Opponent opponent = new Opponent();
		opponent.setTeam(team);
		opponent.setName(name);
		opponent.setBattleCry("Ataque!");
		return opponent;
	}

	/** Builds the arena of the fighters on the JavaFX thread, with a random style. */
	public static BattleArena3D arena(List<Opponent> fighters) {
		return arena(fighters, Configuration.RANDOM);
	}

	/** Builds the arena of the fighters on the JavaFX thread, with the given style (a name of {@link ArenaStyle}). */
	static BattleArena3D arena(List<Opponent> fighters, String style) {
		TestConfiguration.load(style, "/nowhere");
		return FxSupport.run(() -> new BattleArena3D(fighters));
	}

	/** Parses the turns of a battle. */
	public static BattleRecording recording(List<String> output) {
		return BattleRecording.parse(output);
	}

	/**
	 * Plays the recording on the arena and waits until the callback of the end
	 * of the battle runs, feeding the frames from the arena's own timer.
	 *
	 * @return true when the battle finished within the frames given
	 */
	static boolean battle(BattleArena3D arena, BattleRecording recording, int frames, double seconds) {
		return battle(arena, recording, frames, seconds, "Final", Bracket.Importance.FINAL);
	}

	/** Plays the recording as a stage of the championship, with its name and importance. */
	static boolean battle(BattleArena3D arena, BattleRecording recording, int frames, double seconds, String stage, Bracket.Importance importance) {
		CountDownLatch finished = new CountDownLatch(1);
		FxSupport.runVoid(() -> {
			arena.setStage(stage, importance);
			arena.play(recording, finished::countDown);
		});
		frames(arena, frames, seconds);
		try {
			return finished.await(0, TimeUnit.SECONDS);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * The clock of the frames. The arena also gets the real pulses of JavaFX, which
	 * come from the system clock, so each call starts at the later of the two: the
	 * time never runs backwards for the arena.
	 */
	private static long clock;

	/** Advances the arena by frames of the given length, on the JavaFX thread. */
	static void frames(BattleArena3D arena, int frames, double seconds) {
		FxSupport.runVoid(() -> {
			clock = Math.max(clock, System.nanoTime());
			for (int index = 0; index < frames; index++) {
				arena.timer.handle(clock);
				clock += (long) (seconds * 1e9);
			}
		});
	}
}
