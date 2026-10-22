package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import support.FxSupport;

/** The armor of the robots: the plates come off as the energy goes, and come back only for a new round or the replay. */
class RobotArmorTest {

	/** A robot of the sample battle, whole again, so each call of the test is the only change. */
	private static RobotView wholeRobot() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		FxSupport.runVoid(() -> arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
		}));
		return FxSupport.run(() -> {
			RobotView robot = arena.robots.get(0);
			robot.reset();
			robot.setEnergy(RobotArmor.FULL_ENERGY);
			return robot;
		});
	}

	@Test
	void aRobotAtFullEnergyHasAllOfItsArmor() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			// 12 steel plates under the skirts, 22 on the skirts and 6 on the turret
			assertEquals(40, robot.armor.plates.size());
			assertEquals(40, robot.armor.remaining());
		});
	}

	@Test
	void thePlatesComeOffWithTheEnergy() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.setEnergy(75);
			assertEquals(30, robot.armor.remaining());
			robot.setEnergy(50);
			assertEquals(20, robot.armor.remaining());
			robot.setEnergy(1);
			assertEquals(1, robot.armor.remaining());
			robot.setEnergy(0);
			assertEquals(0, robot.armor.remaining());
		});
	}

	@Test
	void theArmorDoesNotGrowBackWhenTheRobotGainsEnergy() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.setEnergy(40);
			// A hit gives energy back, not the plates
			robot.setEnergy(70);
			assertEquals(16, robot.armor.remaining());
		});
	}

	@Test
	void theLostPlatesAreThrownDuringTheBattle() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			int before = robot.host.effects.getChildren().size();
			robot.setEnergy(90);
			// 4 plates come off and fly away over the arena
			assertEquals(4, robot.armor.thrown.size());
			assertEquals(before + 4, robot.host.effects.getChildren().size());
		});
	}

	@Test
	void aNewRoundBringsTheWholeArmorBack() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			int before = robot.host.effects.getChildren().size();
			robot.setEnergy(20);
			robot.reset();

			assertEquals(40, robot.armor.remaining());
			assertTrue(robot.armor.thrown.isEmpty());
			assertEquals(before, robot.host.effects.getChildren().size());
		});
	}

	@Test
	void theReplayStartsWithThePlatesAlreadyGone() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.reset();
			// The first energy after the reset is where the replay starts: nothing is thrown
			robot.setEnergy(30);
			assertEquals(12, robot.armor.remaining());
			assertTrue(robot.armor.thrown.isEmpty());
		});
	}

	@Test
	void theSameRobotLosesTheSamePlatesInEveryReplay() {
		RobotView first = wholeRobot();
		RobotView second = wholeRobot();

		FxSupport.runVoid(() -> {
			first.setEnergy(50);
			second.setEnergy(50);
			for (int index = 0; index < first.armor.plates.size(); index++) {
				assertEquals(first.armor.plates.get(index).isVisible(), second.armor.plates.get(index).isVisible());
			}
		});
	}
}
