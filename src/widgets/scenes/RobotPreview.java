package widgets.scenes;

import javafx.animation.AnimationTimer;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import model.Opponent;
import utils.robocode.BattleRecording;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * A robot in 3D, turning slowly on its place and turning with the mouse, as the organizer looks at it
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RobotPreview extends StackPane {

	/** Degrees a frame, when the robot turns by itself */
	private static final double SPIN_PER_FRAME = 0.4;
	/** Marks the group of the robot in the world, so the next robot replaces it */
	private static final Object ROBOT = new Object();
	/** The body colors of the robots, one for each robot by its id, as ARGB */
	private static final int[] BODY_COLORS = { 0xff2ec4ff, 0xffff3d6e, 0xff5af08f, 0xffffd400, 0xffff9f43, 0xffb388ff };
	private static final int RADAR_COLOR = 0xffffd400;

	private final Group world = new Group();
	private final Rotate spin = new Rotate(0, Rotate.Y_AXIS);
	private final AnimationTimer timer;
	private double lastX = Double.NaN;

	/** Creates the view of a robot, empty until one is shown. */
	public RobotPreview() {
		super();

		PointLight light = new PointLight(Color.WHITE);
		light.setTranslateX(-120);
		light.setTranslateY(-140);
		light.setTranslateZ(-160);
		this.world.getChildren().addAll(new AmbientLight(Color.gray(0.5)), light);

		SubScene view = new SubScene(this.world, 10, 10, true, SceneAntialiasing.BALANCED);
		view.setFill(Color.TRANSPARENT);
		view.setCamera(camera());
		view.widthProperty().bind(this.widthProperty());
		view.heightProperty().bind(this.heightProperty());
		this.frame(view);

		this.timer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				spin.setAngle(spin.getAngle() + SPIN_PER_FRAME);
			}
		};

		// Dragging turns the robot by hand; letting go, it goes on turning by itself
		this.setOnMousePressed(event -> this.lastX = event.getSceneX());
		this.setOnMouseDragged(event -> {
			spin.setAngle(spin.getAngle() + (event.getSceneX() - this.lastX) * 0.6);
			this.lastX = event.getSceneX();
		});
	}

	/** The camera: above the robot and a little pitched down, so the robot is seen from its top too. */
	private static PerspectiveCamera camera() {
		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.setFieldOfView(34);
		camera.setNearClip(1);
		camera.setFarClip(2000);
		camera.getTransforms().add(new Rotate(-12.5, Rotate.X_AXIS));
		camera.setTranslateY(-35);
		camera.setTranslateZ(-120);
		return camera;
	}

	/** Puts the view in a frame, with a hint of what the mouse does on it. */
	private void frame(SubScene view) {
		this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.35); -fx-border-color: -rc-border; -fx-border-width: 1;");
		Label hint = new Label("Arraste para girar");
		hint.getStyleClass().add("form-hint");
		hint.setMouseTransparent(true);
		StackPane.setAlignment(hint, Pos.BOTTOM_LEFT);
		StackPane.setMargin(hint, new Insets(0, 0, 6, 8));
		this.getChildren().addAll(view, hint);
	}

	/** Shows the model of the robot, in place of the one that was shown. */
	public void show(Opponent robot) {
		this.world.getChildren().removeIf(node -> node.getUserData() == ROBOT);
		if (robot == null) {
			return;
		}
		// Each robot has its own colors, so the models of the robots can be told apart
		int index = (int) (robot.getId() % BODY_COLORS.length);
		int body = BODY_COLORS[index];
		int gun = BODY_COLORS[(index + 2) % BODY_COLORS.length];
		RobotView model = new RobotView(null, BattleRecording.Robot.named(robot.getFullName(), body, gun, RADAR_COLOR), robot, index);
		model.root.getTransforms().add(this.spin);
		Group holder = new Group(model.root);
		holder.setUserData(ROBOT);
		this.world.getChildren().add(holder);
		this.timer.start();
	}

	/** Stops the turning, when the screen is left. */
	public void stop() {
		this.timer.stop();
	}
}
