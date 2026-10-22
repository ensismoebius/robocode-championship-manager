package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.scene.paint.Color;
import support.FxSupport;
import utils.robocode.BattleRecording;
import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;
import widgets.scenes.Phase;

/** The shots of the arena: the bolts from the guns, their hits, the flashes of the force field and the critters they startle. */
class ArenaBulletsTest {

	private static RobotState robot(int index, double x, double y, int state) {
		return new RobotState(index, x, y, 0, 0, 0, 100, state);
	}

	private static Bullet bullet(int id, int owner, double x, double y, double power, int state, int victim) {
		return new Bullet(id, owner, x, y, power, state, victim);
	}

	private static Turn turn(int number, List<RobotState> robots, List<Bullet> bullets) {
		return new Turn(0, number, robots, bullets);
	}

	/** A bolt from robot 0 that flies one step per turn and hits robot 1 on the last turn. */
	private static List<Turn> shot() {
		List<RobotState> robots = List.of(robot(0, 100, 100, 0), robot(1, 220, 100, 0));
		return List.of(
				turn(0, robots, List.of()),
				turn(1, robots, List.of(bullet(5, 0, 130, 100, 2, 1, -1))),
				turn(2, robots, List.of(bullet(5, 0, 160, 100, 2, 1, -1))),
				turn(3, robots, List.of(bullet(5, 0, 215, 100, 2, BattleRecording.BULLET_HIT_VICTIM, 1))));
	}

	@Test
	void theBoltsOfABattleAreDrawnAndExplode() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 4000, 0.05);
		List<Turn> turns = ArenaSimulation.recording(ArenaSimulation.OUTPUT).getRounds().get(0);

		// Every point of the battle, from before the first turn to after the last one
		FxSupport.runVoid(() -> {
			for (int index = 0; index < turns.size(); index++) {
				for (double offset : new double[] { -1, 0, 0.5, 0.99, 1 }) {
					arena.turnPosition = index + offset;
					arena.shots.showBullets(turns, index, offset);
				}
			}
		});
	}

	@Test
	void aBoltFliesFromTheGunToTheHullOfItsVictim() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		List<Turn> turns = shot();

		FxSupport.runVoid(() -> {
			ArenaBullets shots = new ArenaBullets(arena);
			ArenaBullets.Flight flight = shots.flight(turns, 3, turns.get(3).bullets().get(0));
			assertNotNull(flight.hit());
			assertTrue(flight.total() > 0);
			// The same bolt comes from the cache of the flights
			assertSame(flight, shots.flight(turns, 2, turns.get(2).bullets().get(0)));

			// From before the shot to long after the hit, when the bolt is already gone
			for (double position = -1; position <= 8; position += 0.5) {
				arena.turnPosition = position;
				arena.shots.showBullets(turns, (int) Math.max(0, Math.min(3, Math.floor(position))), 0.5);
			}
			// The bolt is due at the hull while the turn before the hit is shown
			arena.turnPosition = 6;
			arena.shots.showBullets(turns, 2, 0.5);
			assertTrue(arena.bullets.isEmpty());
			arena.shots.clearBullets();
		});
	}

	@Test
	void aBoltThatMissesFliesToTheEndOfItsPath() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		List<RobotState> robots = List.of(robot(0, 100, 100, 0));
		List<Turn> miss = List.of(
				turn(0, robots, List.of()),
				turn(1, robots, List.of(bullet(8, 0, 130, 100, 2, 1, -1))),
				turn(2, robots, List.of(bullet(8, 0, 160, 100, 2, 1, -1))));

		FxSupport.runVoid(() -> {
			// Long after the bolt should have landed, it stays drawn where its path ends
			for (double position = 0; position <= 6; position += 0.5) {
				arena.turnPosition = position;
				arena.shots.showBullets(miss, (int) Math.min(1, Math.floor(position)), 0.5);
			}
			assertTrue(arena.bullets.containsKey(8));
		});
	}

	@Test
	void aBoltWithoutMovementOrAlreadyHitHasNoFlight() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		List<RobotState> robots = List.of(robot(0, 100, 100, 0));
		// Still: the same place in both turns
		List<Turn> still = List.of(turn(0, robots, List.of(bullet(6, 0, 50, 50, 2, 1, -1))), turn(1, robots, List.of(bullet(6, 0, 50, 50, 2, 1, -1))));
		// Born hitting: the first record of the bolt is already a hit
		List<Turn> hitting = List.of(turn(0, robots, List.of(bullet(7, 0, 50, 50, 2, BattleRecording.BULLET_HIT_WALL, -1))), turn(1, robots, List.of(bullet(7, 0, 60, 50, 2, 1, -1))));

		FxSupport.runVoid(() -> {
			ArenaBullets shots = new ArenaBullets(arena);
			assertEquals(0, shots.flight(still, 1, still.get(1).bullets().get(0)).total());
			assertEquals(0, shots.flight(hitting, 1, hitting.get(1).bullets().get(0)).total());
		});
	}

	@Test
	void aBoltSeenWithoutItsStartIsFlownFromWhereItIsSeen() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		List<Turn> turns = shot();
		Bullet seen = bullet(9, 0, 10, 10, 2, 1, -1);

		FxSupport.runVoid(() -> {
			ArenaBullets.Flight flight = new ArenaBullets(arena).flight(turns, 1, seen);
			assertEquals(0, flight.total());
		});
	}

	@Test
	void aBoltAimedAtAVictimThatIsNotOnTheFieldHasItsEndWhereItWas() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		List<RobotState> onlyOwner = List.of(robot(0, 100, 100, 0));
		List<Turn> turns = List.of(
				turn(0, onlyOwner, List.of()),
				turn(1, onlyOwner, List.of(bullet(5, 0, 130, 100, 2, 1, -1))),
				turn(2, onlyOwner, List.of(bullet(5, 0, 215, 100, 2, BattleRecording.BULLET_HIT_VICTIM, 1))));

		FxSupport.runVoid(() -> {
			ArenaBullets.Flight flight = new ArenaBullets(arena).flight(turns, 2, turns.get(2).bullets().get(0));
			assertNotNull(flight.hit());
			assertEquals(0, flight.side());
			assertEquals(0, flight.ahead());
		});
	}

	@Test
	void theTargetOfAHitIsTheHullOfTheVictimAndAMissIsTheEndOfThePath() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 4000, 0.05);
		List<Turn> turns = shot();

		FxSupport.runVoid(() -> {
			ArenaBullets shots = new ArenaBullets(arena);
			ArenaBullets.Flight hit = shots.flight(turns, 3, turns.get(3).bullets().get(0));
			double[] target = shots.target(hit);
			assertEquals(2, target.length);

			// A bolt that hits nothing ends where its path ends
			List<Turn> miss = List.of(turn(0, List.of(robot(0, 100, 100, 0)), List.of()), turn(1, List.of(robot(0, 100, 100, 0)), List.of(bullet(8, 0, 130, 100, 2, 1, -1))), turn(2, List.of(robot(0, 100, 100, 0)), List.of(bullet(8, 0, 160, 100, 2, 1, -1))));
			ArenaBullets.Flight path = shots.flight(miss, 2, miss.get(2).bullets().get(0));
			assertNotNull(path);
			double[] end = shots.target(path);
			assertEquals(path.sx() + path.ux() * path.total(), end[0], 1e-9);
			assertEquals(path.sy() + path.uy() * path.total(), end[1], 1e-9);
		});
	}

	@Test
	void theShotsAndHitsHappenOnceAndTheHitsOfTheVictimAreFelt() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 4000, 0.05);

		FxSupport.runVoid(() -> {
			ArenaBullets shots = new ArenaBullets(arena);
			assertEquals(Color.web("#38e6ff"), shots.shooterColor(bullet(30, 99, 0, 0, 2, 1, -1)));
			assertNotNull(shots.shooterColor(bullet(31, 0, 0, 0, 2, 1, -1)));

			// A strong hit on the victim is a photo of the round, once
			arena.phase = Phase.PLAYING;
			arena.hitPhotoTaken = false;
			Bullet strong = bullet(32, 0, 400, 300, 3, BattleRecording.BULLET_HIT_VICTIM, 1);
			shots.bulletHit(strong, 400, 300);
			assertTrue(arena.hitPhotoTaken);
			shots.bulletHit(strong, 400, 300);

			// A weak hit on the victim, a hit on a wall and a bullet that meets another one
			shots.bulletHit(bullet(33, 0, 400, 300, 1, BattleRecording.BULLET_HIT_VICTIM, 1), 400, 300);
			shots.bulletHit(bullet(34, 0, 0, 300, 2, BattleRecording.BULLET_HIT_WALL, -1), 0, 300);
			shots.bulletHit(bullet(35, 0, 400, 0, 2, BattleRecording.BULLET_HIT_BULLET, -1), 400, 0);
			shots.bulletFired(bullet(36, 0, 120, 100, 2, 1, -1), 120, 100);
			shots.bulletFired(bullet(36, 0, 120, 100, 2, 1, -1), 120, 100);
		});
	}

	@Test
	void theWallsFlashWhereTheyAreHitAndTheFlashesAreCleared() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 4000, 0.05);

		FxSupport.runVoid(() -> {
			ArenaBullets shots = new ArenaBullets(arena);
			// Each wall, a corner and the middle of the field
			arena.forceField.flashFence(0, 300);
			arena.forceField.flashFence(800, 300);
			arena.forceField.flashFence(400, 0);
			arena.forceField.flashFence(400, 600);
			arena.forceField.flashFence(-50, 900);
			arena.forceField.flashFence(400, 300);
			shots.whiteFlash();
			arena.forceField.resetFence();
			shots.startle(100, 100, 50, 0.2);
		});
		FxSupport.sleep(500);
	}

	@Test
	void theTurnsOfTheBattleCarryTheShotsHitsWallsAndDeaths() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		ArenaSimulation.battle(arena, ArenaSimulation.recording(ArenaSimulation.OUTPUT), 4000, 0.05);
		List<RobotState> alive = List.of(robot(0, 100, 100, 0), robot(1, 220, 100, 0));
		List<RobotState> wallHit = List.of(robot(0, 0, 100, BattleRecording.ROBOT_HIT_WALL), robot(1, 220, 100, 0));
		List<RobotState> lowEnergy = List.of(new RobotState(0, 100, 100, 0, 0, 0, 10, 0), robot(1, 220, 100, 0));
		List<RobotState> dead = List.of(robot(0, 100, 100, 0), robot(1, 220, 100, BattleRecording.ROBOT_DEAD));

		FxSupport.runVoid(() -> {
			ArenaBullets shots = new ArenaBullets(arena);
			Turn before = turn(0, alive, List.of());
			arena.phase = Phase.PLAYING;
			arena.replayed = false;
			// A shot and a hit in the turn, the robot runs into a wall and it gets low on energy
			shots.turnEvents(turn(1, wallHit, List.of(bullet(40, 0, 130, 100, 2, 1, -1), bullet(41, 0, 220, 100, 2, BattleRecording.BULLET_HIT_VICTIM, 1))), before, 1);
			shots.turnEvents(turn(2, lowEnergy, List.of()), turn(1, alive, List.of()), 2);
			// The victim dies: its explosion, the photo of the destruction and the replay of the death
			shots.turnEvents(turn(3, dead, List.of()), turn(2, alive, List.of()), 3);
			// The first turn of the battle has no previous one
			shots.turnEvents(turn(0, alive, List.of()), null, 0);
			// A robot the field does not know has no picture to react with
			shots.turnEvents(turn(1, List.of(robot(5, 10, 10, 0)), List.of()), null, 1);
		});
	}
}
