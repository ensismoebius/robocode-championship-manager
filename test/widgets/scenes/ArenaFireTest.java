package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import support.FxSupport;
import widgets.scenes.ArenaFire.Particle;
import widgets.scenes.ArenaFire.ParticleKind;

/** The fire of the burning robots: flames fixed to their spots, whose bodies trail back along the way of the robot, late to its movement. */
class ArenaFireTest {

	/** An arena in its battle, with the first robot burning and the camera looking along z, so the right of the screen is +x */
	private static RobotView burningRobot(BattleArena3D arena) {
		FxSupport.runVoid(() -> arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
		}));
		return FxSupport.run(() -> {
			RobotView robot = arena.robots.get(0);
			robot.reset();
			robot.root.setVisible(true);
			robot.setEnergy(10);
			arena.cameraYaw.setAngle(0);
			arena.fire.clear();
			return robot;
		});
	}

	/** The flames alive now */
	private static List<Particle> flames(BattleArena3D arena) {
		return arena.fire.particles.stream().filter(particle -> particle.kind == ParticleKind.FLAME).toList();
	}

	/** The flame born last */
	private static Particle newest(BattleArena3D arena) {
		return flames(arena).stream().min((first, second) -> Double.compare(first.age, second.age)).orElseThrow();
	}

	/** A flame that has grown: the ones just born are tiny, and what is far from them is far from their height */
	private static Particle grown(BattleArena3D arena) {
		return flames(arena).stream().filter(particle -> particle.age > 0.25).min((first, second) -> Double.compare(first.age, second.age)).orElseThrow();
	}

	/** Runs the robot toward +x, 4 units a frame of 0.02 seconds (200 a second), from where it is */
	private static void run(BattleArena3D arena, RobotView robot, int frames) {
		double from = robot.root.getTranslateX();
		for (int frame = 1; frame <= frames; frame++) {
			robot.place(from + 4 * frame, 50, 0, 0, 0);
			arena.fire.update(0.02);
		}
	}

	@Test
	void theFlamesOfARobotThatStandsStillStayUpright() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		RobotView robot = burningRobot(arena);

		FxSupport.runVoid(() -> {
			robot.place(100, 50, 0, 0, 0);
			for (int frame = 0; frame < 30; frame++) {
				arena.fire.update(0.02);
			}
			assertTrue(flames(arena).size() > 5, "the robot is burning");
			for (Particle flame : flames(arena)) {
				assertEquals(0, flame.lean, 1e-9);
			}
		});
	}

	@Test
	void theBodyOfAFlameTrailsBackAlongTheWayOfTheRobot() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		RobotView robot = burningRobot(arena);

		FxSupport.runVoid(() -> {
			robot.place(0, 50, 0, 0, 0);
			arena.fire.update(0.02);
			run(arena, robot, 40);

			// It leans against the movement, which is the left of the screen here, but does not lie down
			Particle fresh = newest(arena);
			assertTrue(fresh.lean < -30, "leans back: " + fresh.lean);
			assertTrue(fresh.lean >= -50 - 1e-9, "does not lie down: " + fresh.lean);
			// The tip is behind the base, against the movement
			double[] tip = fresh.ribbon.tip();
			double[] base = fresh.ribbon.base();
			assertTrue(tip[0] < base[0] - 1, "the tip is behind the base: " + tip[0] + " " + base[0]);
			// It bends: each section goes more aside than the one under it, it is not a board that leans
			double middle = fresh.ribbon.asideX[FlameRibbon.SECTIONS / 2];
			assertTrue(Math.abs(middle) < Math.abs(fresh.ribbon.asideX[FlameRibbon.SECTIONS]) * 0.6, "bends more toward the tip: " + middle + " " + fresh.ribbon.asideX[FlameRibbon.SECTIONS]);
		});
	}

	@Test
	void theGasOfAFlameStaysWhereTheRobotLeftItAndItsTipIsWhereTheRobotWasBefore() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		RobotView robot = burningRobot(arena);

		FxSupport.runVoid(() -> {
			robot.place(0, 50, 0, 0, 0);
			arena.fire.update(0.02);
			// Slowly, 1 unit a frame of 0.02 seconds (50 a second): the tip is 0.4 seconds back, 20 frames and 20 units
			for (int frame = 1; frame <= 60; frame++) {
				robot.place(frame, 50, 0, 0, 0);
				arena.fire.update(0.02);
			}
			Particle flame = grown(arena);
			assertEquals(-20, flame.ribbon.asideX[FlameRibbon.SECTIONS], 0.5);
			assertEquals(0, flame.ribbon.asideZ[FlameRibbon.SECTIONS], 1e-9);
			// The base is where the robot is, whatever it does
			assertEquals(0, flame.ribbon.asideX[0], 1e-9);
		});
	}

	@Test
	void theReactionOfTheFlamesIsLateToTheMovementOfTheRobot() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		RobotView robot = burningRobot(arena);

		FxSupport.runVoid(() -> {
			robot.place(0, 50, 0, 0, 0);
			for (int frame = 0; frame < 20; frame++) {
				arena.fire.update(0.02);
			}
			// The robot takes off: for a little while the tip has hardly moved from the vertical
			run(arena, robot, 2);
			double soon = Math.abs(grown(arena).lean);
			run(arena, robot, 38);
			double later = Math.abs(grown(arena).lean);

			assertTrue(soon < 27, "late: " + soon);
			assertTrue(later > 1.8 * soon, "then it leans: " + soon + " " + later);
		});
	}

	@Test
	void theBaseOfAFlameIsFixedToItsSpotOnTheRobotAsItGoes() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		RobotView robot = burningRobot(arena);

		FxSupport.runVoid(() -> {
			robot.place(0, 50, 0, 0, 0);
			arena.fire.update(0.02);
			for (int frame = 1; frame <= 60; frame++) {
				run(arena, robot, 1);
				for (Particle flame : flames(arena)) {
					// It does not rise or drift: the picture of the flame is what moves
					assertEquals(0, flame.vx, 1e-12);
					assertEquals(0, flame.vy, 1e-12);
					assertEquals(0, flame.vz, 1e-12);
					// The base of its strip, in the world, is on its spot of the robot
					double baseX = flame.view.getTranslateX() + flame.ribbon.base()[0];
					assertEquals(robot.root.getTranslateX() + flame.x, baseX, 1e-6, "frame " + frame);
				}
			}
		});
	}

	@Test
	void theFlamesStandUpAgainWhenTheRobotStops() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		RobotView robot = burningRobot(arena);

		FxSupport.runVoid(() -> {
			robot.place(0, 50, 0, 0, 0);
			arena.fire.update(0.02);
			run(arena, robot, 30);
			assertTrue(grown(arena).lean < -30);
			// It stops: after the time the gas takes to go up, the flames are upright
			for (int frame = 0; frame < 40; frame++) {
				arena.fire.update(0.02);
			}
			assertEquals(0, grown(arena).lean, 1.0);
		});
	}

	@Test
	void aJumpOfTheRobotDoesNotBendTheFlames() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		RobotView robot = burningRobot(arena);

		FxSupport.runVoid(() -> {
			robot.place(0, 0, 0, 0, 0);
			for (int frame = 0; frame < 20; frame++) {
				arena.fire.update(0.02);
			}
			// A new round puts the robot far away at once
			robot.place(300, 200, 0, 0, 0);
			arena.fire.update(0.02);
			for (Particle flame : flames(arena)) {
				assertEquals(0, flame.lean, 1e-9);
			}
		});
	}

	@Test
	void theSmokeIsDrawnBeforeTheFlamesWhateverItsDistanceToTheCamera() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		RobotView robot = burningRobot(arena);

		FxSupport.runVoid(() -> {
			robot.place(100, 50, 0, 0, 0);
			for (int frame = 0; frame < 60; frame++) {
				arena.fire.update(0.02);
			}
			List<javafx.scene.Node> drawn = arena.fireLayer.getChildren();
			int lastSmoke = -1;
			int firstFlame = drawn.size();
			for (Particle particle : arena.fire.particles) {
				int index = drawn.indexOf(particle.view);
				if (particle.kind == ParticleKind.SMOKE) {
					lastSmoke = Math.max(lastSmoke, index);
				} else if (particle.kind == ParticleKind.FLAME) {
					firstFlame = Math.min(firstFlame, index);
				}
			}
			assertTrue(lastSmoke >= 0 && firstFlame < drawn.size(), "there is smoke and fire");
			assertTrue(lastSmoke < firstFlame, "the smoke is behind the flames");
		});
	}

	@Test
	void theSmokeIsPushedBackFromTheCameraAndMadeBiggerToLookTheSame() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters());
		burningRobot(arena);

		FxSupport.runVoid(() -> {
			Particle smoke = arena.fire.spawn(ParticleKind.SMOKE, 100, -20, 300, 0.5);
			arena.fire.update(0.001);
			double eyeX = arena.eye[0];
			double eyeZ = arena.eye[2];
			double was = Math.hypot(smoke.x - eyeX, smoke.z - eyeZ);
			double now = Math.hypot(smoke.view.getTranslateX() - eyeX, smoke.view.getTranslateZ() - eyeZ);
			assertTrue(now > was + 20, "the smoke is farther from the eye than where it is");
		});
	}
}
