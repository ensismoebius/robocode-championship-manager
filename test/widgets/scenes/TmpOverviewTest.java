package widgets.scenes;

import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import support.FxSupport;

class TmpOverviewTest {

	@Test
	void shoot() throws Exception {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "EDEN_SOLAR");
		Stage window = FxSupport.show(arena);
		FxSupport.runVoid(() -> {
			window.setX(0);
			window.setY(0);
			window.setWidth(1200);
			window.setHeight(700);
		});
		FxSupport.sleep(800);
		FxSupport.runVoid(() -> arena.play(ArenaSimulation.recording(ArenaSimulation.OUTPUT), () -> {
		}));
		ArenaSimulation.frames(arena, 400, 0.03);
		FxSupport.sleep(500);
		FxSupport.runVoid(() -> {
			arena.timer.stop();
			arena.getChildren().stream().filter(node -> node != arena.subScene).forEach(node -> node.setVisible(false));
			double[] eye = { -2200, -2400, -2600 };
			double[] look = { 0, -150, 0 };
			double fx = look[0] - eye[0];
			double fy = look[1] - eye[1];
			double fz = look[2] - eye[2];
			double distance = Math.sqrt(fx * fx + fy * fy + fz * fz);
			arena.cameraLook.setX(look[0]);
			arena.cameraLook.setY(look[1]);
			arena.cameraLook.setZ(look[2]);
			arena.cameraYaw.setAngle(Math.toDegrees(Math.atan2(fx, fz)));
			arena.cameraPitch.setAngle(Math.toDegrees(Math.asin(-fy / distance)));
			arena.cameraRoll.setAngle(0);
			arena.cameraDistance.setZ(-distance);
			arena.camera.setFieldOfView(64);
		});
		FxSupport.sleep(1500);
		new ProcessBuilder("import", "-window", "root", "/tmp/claude-1000/-home-ensismoebius-Repos-robocode-championship-manager-code/0997c13f-7150-4f98-973f-d7c72207ed83/scratchpad/overview.png").inheritIO().start().waitFor();
		FxSupport.close(window);
	}
}
