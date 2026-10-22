package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import support.FxSupport;

/**
 * The pieces the robots lose: they come off as the energy goes, are thrown
 * away burning, stay on the floor until the end of the round, and come back
 * only for a new round or the replay.
 */
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

	/** Lets the thrown pieces burn and slide for the given seconds, a tenth of a second at a time */
	private static void burnFor(RobotView robot, double seconds) {
		for (int step = 0; step < Math.round(seconds * 10); step++) {
			robot.armor.update(0.1);
		}
	}

	/** The first thrown piece that is big, or small, already lying on the floor */
	private static RobotArmor.Thrown landed(RobotView robot, boolean big) {
		RobotArmor.Thrown piece = robot.armor.thrown.stream().filter(thrown -> thrown.big == big).findFirst().orElseThrow();
		piece.landed = true;
		return piece;
	}

	@Test
	void aRobotAtFullEnergyHasAllOfItsPieces() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			// 40 plates, 30 parts it can fight without and the 2 skirts
			assertEquals(72, robot.armor.pieces.size());
			assertEquals(72, robot.armor.remaining());
			// The antennas, the weapon station, the sight, the muzzle and the skirts (with the strip of light) are made of more than one shape
			assertEquals(6, robot.armor.pieces.stream().filter(piece -> piece.getChildren().size() > 1).count());
		});
	}

	@Test
	void thePiecesComeOffWithTheEnergy() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.setEnergy(75);
			assertEquals(54, robot.armor.remaining());
			robot.setEnergy(50);
			assertEquals(36, robot.armor.remaining());
			robot.setEnergy(1);
			assertEquals(1, robot.armor.remaining());
			robot.setEnergy(0);
			assertEquals(0, robot.armor.remaining());
		});
	}

	@Test
	void aSkirtComesOffOnlyAfterAllOfItsPlates() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			// The skirts are as long as the hull; their plates sit on them, and others hang under them
			List<Group> skirts = robot.armor.pieces.stream().filter(piece -> piece.getBoundsInLocal().getDepth() > 30).toList();
			assertEquals(2, skirts.size());
			boolean[] fell = new boolean[2];
			for (int energy = 100; energy >= 0; energy--) {
				robot.setEnergy(energy);
				for (int index = 0; index < 2; index++) {
					Group skirt = skirts.get(index);
					if (!skirt.isVisible()) {
						fell[index] = true;
						double side = Math.signum(skirt.getBoundsInLocal().getCenterX());
						for (Group piece : robot.armor.pieces) {
							Bounds bounds = piece.getBoundsInLocal();
							// On the skirt or hanging under it, outside the track: nothing else is out there
							boolean onTheSkirt = piece != skirt && Math.signum(bounds.getCenterX()) == side && Math.abs(bounds.getCenterX()) > 16;
							assertFalse(onTheSkirt && piece.isVisible() && bounds.getDepth() < 10, "a plate left in the air at energy " + energy);
						}
					}
				}
			}
			assertTrue(fell[0] && fell[1]);
		});
	}

	@Test
	void thePiecesDoNotGrowBackWhenTheRobotGainsEnergy() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.setEnergy(40);
			// A hit gives energy back, not the pieces
			robot.setEnergy(70);
			assertEquals(29, robot.armor.remaining());
		});
	}

	@Test
	void theLostPiecesStayOnTheFloorUntilTheEndOfTheRound() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.setEnergy(90);
			assertEquals(7, robot.armor.thrown.size());
			burnFor(robot, 10);
			// Long after the fire, still there, at their full size
			for (RobotArmor.Thrown piece : robot.armor.thrown) {
				assertTrue(robot.host.effects.getChildren().contains(piece.flying));
				assertEquals(1, piece.flying.getScaleX(), 1e-9);
			}
		});
	}

	@Test
	void theThrownPiecesBurnAndSmokeForHalfASecond() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.setEnergy(90);
			RobotArmor.Thrown piece = robot.armor.thrown.get(0);
			assertEquals(0.5, RobotArmor.FIRE_SECONDS, 1e-9);
			burnFor(robot, 0.3);
			assertTrue(piece.fire > 0, "still burning");
			burnFor(robot, 0.3);
			assertTrue(piece.fire <= 0, "the fire and the smoke stopped");
		});
	}

	@Test
	void theThrownPiecesKeepTheirColors() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.setEnergy(0);
			List<Color> before = robot.armor.thrown.stream().flatMap(piece -> piece.shapes.stream()).map(shape -> ((PhongMaterial) shape.getMaterial()).getDiffuseColor()).toList();
			// The robot blinks white when hit; the pieces on the floor do not, and the fire does not char them
			robot.flash();
			burnFor(robot, 2);
			List<Color> after = robot.armor.thrown.stream().flatMap(piece -> piece.shapes.stream()).map(shape -> ((PhongMaterial) shape.getMaterial()).getDiffuseColor()).toList();
			assertEquals(before, after);
			// A skirt has the color of the hull
			assertEquals(robot.bodyColor, ((PhongMaterial) landed(robot, true).shapes.get(0).getMaterial()).getDiffuseColor());
		});
	}

	@Test
	void aRobotPushesTheBigPiecesOutOfItsWay() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.setEnergy(0);
			RobotArmor.Thrown skirt = landed(robot, true);
			double[] before = skirt.position();
			double[] along = skirt.axis();
			// The robot runs onto the skirt, off its middle
			robot.place(before[0] + along[0] * skirt.halfLength * 0.7, before[2] + along[1] * skirt.halfLength * 0.7, 0, 0, 0);
			burnFor(robot, 1);

			double[] after = skirt.position();
			assertTrue(Math.hypot(after[0] - before[0], after[2] - before[2]) > 5, "pushed away");
			assertTrue(Math.abs(skirt.turn.getAngle()) > 1, "turned, hit off its middle");
		});
	}

	@Test
	void theWreckIsPushedAroundByTheRobotsStillFighting() {
		RobotView wreck = wholeRobot();

		FxSupport.runVoid(() -> {
			RobotView other = wreck.host.robots.values().stream().filter(robot -> robot != wreck).findFirst().orElseThrow();
			other.reset();
			other.root.setVisible(true);
			wreck.setEnergy(0);
			wreck.dead = true;
			wreck.armor.adopt(wreck.body);
			RobotArmor.Thrown hull = wreck.armor.thrown.get(wreck.armor.thrown.size() - 1);
			assertFalse(wreck.body.isVisible(), "the hull is now a piece of the floor");
			assertTrue(hull.big && hull.landed);

			double[] before = hull.position();
			double[] along = hull.axis();
			other.place(before[0] + along[0] * hull.halfLength * 0.7, before[2] + along[1] * hull.halfLength * 0.7, 0, 0, 0);
			burnFor(wreck, 1);

			double[] after = hull.position();
			assertTrue(Math.hypot(after[0] - before[0], after[2] - before[2]) > 5, "pushed away");
		});
	}

	@Test
	void theReplayStandsTheWreckUpAgain() {
		RobotView wreck = wholeRobot();

		FxSupport.runVoid(() -> {
			wreck.setEnergy(0);
			wreck.dead = true;
			wreck.armor.adopt(wreck.body);
			wreck.armor.adopt(wreck.turret);
			wreck.rewind(RobotArmor.FULL_ENERGY);

			assertTrue(wreck.body.isVisible());
			assertTrue(wreck.turret.isVisible());
			assertTrue(wreck.armor.thrown.isEmpty());
		});
	}

	@Test
	void theSmallPiecesStayWhereTheyFell() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.setEnergy(0);
			RobotArmor.Thrown plate = landed(robot, false);
			double[] before = plate.position();
			robot.place(before[0], before[2], 0, 0, 0);
			burnFor(robot, 1);

			double[] after = plate.position();
			assertEquals(before[0], after[0], 1e-9);
			assertEquals(before[2], after[2], 1e-9);
		});
	}

	@Test
	void aNewRoundBringsTheWholeRobotBack() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			int before = robot.host.effects.getChildren().size();
			robot.setEnergy(20);
			robot.reset();

			assertEquals(72, robot.armor.remaining());
			assertTrue(robot.armor.thrown.isEmpty());
			assertEquals(before, robot.host.effects.getChildren().size());
		});
	}

	@Test
	void theReplayKeepsOnTheFloorThePiecesLostBeforeItAndThrowsTheOthersAgain() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			// Lost long before the death: 72 pieces, 58 kept at 80 of energy
			robot.setEnergy(80);
			List<RobotArmor.Thrown> before = List.copyOf(robot.armor.thrown);
			assertEquals(14, before.size());
			// Lost in the moments the replay shows
			robot.setEnergy(40);
			assertEquals(43, robot.armor.thrown.size());

			// The replay starts: the effects are cleared and the robot goes back to 80
			robot.host.effects.getChildren().clear();
			robot.rewind(80);
			assertEquals(58, robot.armor.remaining());
			assertEquals(before, robot.armor.thrown);
			for (RobotArmor.Thrown piece : before) {
				assertTrue(robot.host.effects.getChildren().contains(piece.flying), "still on the floor");
				assertTrue(piece.landed);
			}

			// In the replay the later ones come off again, thrown
			robot.setEnergy(40);
			assertEquals(43, robot.armor.thrown.size());
			assertEquals(29, robot.armor.remaining());
		});
	}

	@Test
	void theReplayStartsWithThePiecesAlreadyGone() {
		RobotView robot = wholeRobot();

		FxSupport.runVoid(() -> {
			robot.reset();
			// The first energy after the reset is where the replay starts: nothing is thrown
			robot.setEnergy(30);
			assertEquals(22, robot.armor.remaining());
			assertTrue(robot.armor.thrown.isEmpty());
		});
	}

	@Test
	void theSameRobotLosesTheSamePiecesInEveryReplay() {
		RobotView first = wholeRobot();
		RobotView second = wholeRobot();

		FxSupport.runVoid(() -> {
			first.setEnergy(50);
			second.setEnergy(50);
			for (int index = 0; index < first.armor.pieces.size(); index++) {
				assertEquals(first.armor.pieces.get(index).isVisible(), second.armor.pieces.get(index).isVisible());
			}
		});
	}
}
