package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import model.Opponent;
import support.ChampionshipFixture;
import support.FxSupport;

/** The robots of the arena: how they move, turn, burn, flash, blow up and come back for the next round. */
class RobotViewTest {

	/** The arena with the sample battle played on it, without its timer, so each call of the test is the only change. */
	private static BattleArena3D playedArena() {
		List<Opponent> fighters = ArenaSimulation.fighters();
		// With the logo of the team, as the balloon of the robot shows it
		for (Opponent fighter : fighters) {
			fighter.setImage(ChampionshipFixture.logo());
		}
		BattleArena3D arena = ArenaSimulation.arena(fighters);
		FxSupport.runVoid(() -> arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
		}));
		return arena;
	}

	@Test
	void aRobotIsPlacedWhereItIsTold() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(0);
			robot.place(120, -80, 0.5, 0.2, 0.1);

			assertEquals(120, robot.position()[0], 1e-9);
			assertEquals(-80, robot.position()[2], 1e-9);
			assertEquals(Math.toDegrees(0.5), robot.bodyRotate.getAngle(), 1e-9);
		});
	}

	@Test
	void aDamagedRobotCatchesFireAndSmokes() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(0);
			robot.place(0, 0, 0, 0, 0);
			// The more damage, the more flames, the sparks and the smoke
			robot.setEnergy(45);
			robot.burn(0.1);
			robot.setEnergy(30);
			robot.burn(0.1);
			robot.setEnergy(5);
			robot.burn(0.1);
			// A jump of the robot (a new round, the replay) is not a movement of the flames
			robot.place(500, 500, 0, 0, 0);
			robot.burn(0.1);
			// Without time passed there is nothing to move
			robot.burn(0);
			robot.setEnergy(90);
			robot.burn(0.1);
		});

		assertTrue(FxSupport.run(() -> arena.robots.get(0).energy) > 0);
	}

	@Test
	void aHiddenOrDeadRobotDoesNotBurn() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(1);
			robot.setEnergy(5);
			robot.root.setVisible(false);
			robot.burn(0.1);
			robot.root.setVisible(true);
			robot.burn(0.1);
		});
	}

	@Test
	void theTracksAndTheTurretFollowTheMovementOfTheRobot() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(0);
			robot.place(0, 0, 0, 0, 0);
			robot.roll(0.05);
			// Driving forward, then turning on the spot, then the turret turning while the hull stays
			robot.place(10, 0, 0, 0, 0);
			robot.roll(0.05);
			robot.place(10, 0, 1.0, 0, 0);
			robot.roll(0.05);
			robot.place(10, 0, 1.0, 1.0, 0);
			robot.roll(0.05);
			// A jump is not a movement, and a turret turned the long way round is not a turn
			robot.place(900, 900, 1.0, 3.0, 0);
			robot.roll(0.05);
			robot.roll(0);

			assertTrue(robot.treadSpeed >= 0);
			assertTrue(robot.turretSpeed >= 0);
		});
	}

	@Test
	void aRobotFlashesWhenHitAndGlowsWhenItIsGlowing() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(0);
			robot.flash();
			assertEquals(Color.WHITE, robot.bodyMaterial.getDiffuseColor());
			robot.setGlowing(true);
			assertNotNull(robot.glow(Color.CYAN));
			robot.setGlowing(false);
		});
		// The flash goes back to the color of the robot
		FxSupport.sleep(400);
		assertEquals(FxSupport.run(() -> arena.robots.get(0).bodyColor), FxSupport.run(() -> arena.robots.get(0).bodyMaterial.getDiffuseColor()));
	}

	@Test
	void aDestroyedRobotIsThrownAndComesBackForTheNextRound() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(1);
			robot.destroy();
			assertTrue(robot.dead);
			// Dead, it does not move, turn or burn, and it is not hit again
			robot.place(10, 10, 1, 1, 1);
			robot.roll(0.05);
			robot.burn(0.1);
			robot.flash();
			robot.destroy();
		});
		// The explosion plays out, the parts fly away
		FxSupport.sleep(600);
		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(1);
			robot.reset();
			assertFalse(robot.dead);
			// The name of the robot is back, without the mark of the dead
			assertEquals(robot.displayName, robot.nameLabel.getText());
		});
	}

	@Test
	void aRobotTakesTheColorsItChangesTo() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(0);
			robot.reset();
			robot.setColors(0xffff0000, 0xff00ff00, 0xff0000ff);

			assertEquals(Color.rgb(255, 0, 0), robot.bodyMaterial.getDiffuseColor());
			assertEquals(Color.rgb(0, 255, 0), robot.gunMaterial.getDiffuseColor());
			assertEquals(Color.rgb(0, 0, 255), robot.radarMaterial.getDiffuseColor());
			// The glint follows the new color
			assertEquals(1, robot.bodyMaterial.getSpecularColor().getRed(), 1e-9);

			// Blinking after a hit, it stays white and takes the new color when the blink ends
			robot.flash();
			robot.setColors(0xff00ffff, 0xff00ff00, 0xff0000ff);
			assertEquals(Color.WHITE, robot.bodyMaterial.getDiffuseColor());
			assertEquals(Color.rgb(0, 255, 255), robot.bodyColor);
		});
	}

	@Test
	void aHitRobotRocksAwayFromTheBulletAndComesToRest() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(0);
			robot.reset();
			robot.place(100, 50, 0, 0, 0);
			// Struck on its right side
			robot.rock(130, 50, 3);
			robot.rocking.pause();
			robot.rocking.jumpTo(Duration.millis(60));
			Point3D top = robot.tank.localToParent(0, -25, 0);
			assertTrue(top.getX() < -1, "the top leans away from the hit: " + top);
			assertEquals(0, top.getZ(), 1e-6);
			double strong = Math.abs(robot.tilt.getAngle());

			// A weak bullet rocks it less
			robot.rock(130, 50, 0.1);
			robot.rocking.pause();
			robot.rocking.jumpTo(Duration.millis(60));
			assertTrue(Math.abs(robot.tilt.getAngle()) < strong);

			// And it comes to rest, standing straight
			robot.rocking.jumpTo(Duration.millis(520));
			assertEquals(0, robot.tilt.getAngle(), 1e-6);
		});
	}

	@Test
	void theGunRecoilsWhenItFiresAndComesBack() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(0);
			robot.reset();
			double rest = robot.gun.localToParent(0, 0, 0).getZ();

			robot.recoil(3);
			robot.recoiling.pause();
			robot.recoiling.jumpTo(Duration.millis(45));
			double strong = rest - robot.gun.localToParent(0, 0, 0).getZ();
			assertTrue(strong > 5, "the barrel slid back: " + strong);

			robot.recoil(0.1);
			robot.recoiling.pause();
			robot.recoiling.jumpTo(Duration.millis(45));
			assertTrue(rest - robot.gun.localToParent(0, 0, 0).getZ() < strong, "a weak shot kicks less");

			robot.recoiling.jumpTo(Duration.millis(380));
			assertEquals(rest, robot.gun.localToParent(0, 0, 0).getZ(), 1e-6);
		});
	}

	@Test
	void aDestroyedRobotDoesNotRock() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(0);
			robot.reset();
			robot.destroy();
			robot.rock(robot.position()[0] + 30, robot.position()[2], 3);

			assertNull(robot.rocking);
			assertEquals(0, robot.tilt.getAngle(), 1e-9);
		});
	}

	@Test
	void aDestroyedRobotKeepsItsColors() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(0);
			robot.reset();
			robot.setColors(0xffff0000, 0xffff0000, 0xffff0000);
			robot.destroy();
			robot.setColors(0xff00ff00, 0xff00ff00, 0xff00ff00);

			assertEquals(Color.rgb(255, 0, 0), robot.gunMaterial.getDiffuseColor());
			assertEquals(Color.rgb(255, 0, 0), robot.bodyColor);
		});
	}

	@Test
	void theEnergyBarOfTheRobotShowsTheEnergyLeft() {
		BattleArena3D arena = playedArena();

		FxSupport.runVoid(() -> {
			RobotView robot = arena.robots.get(0);
			robot.setEnergy(50);
			assertEquals(45, robot.energyFill.getMaxWidth(), 1e-9);
			robot.setEnergy(-10);
			assertEquals(0, robot.energyFill.getMaxWidth(), 1e-9);
		});
	}
}
