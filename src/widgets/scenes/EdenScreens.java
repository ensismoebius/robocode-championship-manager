package widgets.scenes;

import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The great screens of the Eden Solar arena: a ring of tall screens in the band at the top of the stands, framed in
 * dark metal with a line of light, that show the battle. What they show is the battle itself, filmed by a broadcast
 * camera that circles the field high over the garden: a few times a second the scene is drawn again from that
 * camera, in a small picture, and the picture is the one that all the screens show.
 *
 * <p>
 * The screens are as wide as they are tall (sixteen by nine) on the swollen plan, so they have more pieces of the ring
 * where the plan is narrow and fewer where it bulges.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenScreens {

	/** A screen at every this many pieces of the ring, centered on a piece, so the ribs of the roof pass between them */
	static final int EVERY = 32;
	/** How tall a screen is, and how far from the bottom of the band it begins */
	static final double HEIGHT = 230;
	private static final double MARGIN = 12;
	private static final double ASPECT = 16.0 / 9;
	/** How many pieces of the ring a screen takes at most and at least, and how much of its edge the frame takes */
	private static final int WIDEST = 10;
	private static final int NARROWEST = 4;
	private static final double FRAME = 5;

	/** The picture of the broadcast: this wide, the height following the scene, and how many seconds between pictures */
	private static final double PICTURE_WIDTH = 512;
	private static final double SECONDS_BETWEEN = 0.1;
	/** The camera that films the battle: its fov, how far it stays from the fighters (more when they are apart) and how high, and how fast it circles, in turns per second */
	private static final double FOV = 42;
	private static final double NEAREST = 340;
	private static final double FARTHEST = 760;
	private static final double LAPS = 0.012;
	/** The camera never leaves the garden: it is kept within this distance of the middle of the field */
	private static final double LEASH = 950;

	private final BattleArena3D host;
	private final EdenStadium stadium;
	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final Translate eye = new Translate();
	private final Rotate yaw = new Rotate(0, Rotate.Y_AXIS);
	private final Rotate pitch = new Rotate(0, Rotate.X_AXIS);
	private final PhongMaterial glass = new PhongMaterial(Color.BLACK);
	private WritableImage picture;
	/** What the camera looks at, the way to it softened so the picture does not jump when a robot dies */
	private final double[] focus = new double[3];
	private double spread = NEAREST;
	private double turn;
	private double sinceLast;
	private int pictures;

	/** The screens of the stadium, in the host scene. */
	EdenScreens(BattleArena3D host, EdenStadium stadium) {
		this.host = host;
		this.stadium = stadium;
		this.camera.setFieldOfView(FOV);
		this.camera.setNearClip(5);
		this.camera.setFarClip(30000);
		this.camera.getTransforms().setAll(this.eye, this.yaw, this.pitch);
		this.glass.setSelfIlluminationMap(standby());
	}

	/** How many pieces of the ring the screen centered on a piece takes: 16:9 on the plan, wider in the narrow places. */
	int width(int center) {
		double piece = this.stadium.outerEdge() * Math.PI * 2 / EdenStadium.SEGMENTS * (1 + EdenStadium.swell(EdenStadium.angle(center)));
		return Math.max(NARROWEST, Math.min(WIDEST, (int) Math.round(HEIGHT * ASPECT / piece)));
	}

	/** Whether the angle is in front of a screen, so what hangs from the top of the band stays away from it. */
	boolean covers(double angle) {
		double piece = angle / (Math.PI * 2) * EdenStadium.SEGMENTS;
		int center = (int) Math.round(piece / EVERY) * EVERY;
		return Math.abs(piece - center) < this.width(center) / 2.0 + 0.5;
	}

	/** The picture before the first one of the broadcast: a dim grid with scan lines. */
	private static Image standby() {
		return TextureMath.image(64, 36, (posX, posY) -> {
			int grid = posX % 8 == 0 || posY % 8 == 0 ? 0x16343a : 0x0a1519;
			return posY % 2 == 0 ? grid : TextureMath.mix(grid, 0x000000, 0.4);
		});
	}

	/** Builds the screens with their frames, and puts the broadcast camera in the scene. */
	void build() {
		SurfaceMesh screens = new SurfaceMesh();
		SurfaceMesh backs = new SurfaceMesh();
		LowPoly lines = new LowPoly();
		double radius = this.stadium.outerEdge();
		double bottom = EdenStadium.topOfStands() - MARGIN;
		double top = bottom - HEIGHT;
		for (int center = 0; center < EdenStadium.SEGMENTS; center += EVERY) {
			int pieces = this.width(center);
			int first = center - pieces / 2;
			this.frame(backs, lines, first, pieces, radius, bottom, top);
			this.glass(screens, first, pieces, radius, bottom, top);
		}
		PhongMaterial metal = new PhongMaterial(Color.web("#5a6a74"));
		host.arena.getChildren().addAll(backs.view(metal), screens.view(this.glass), lines.view(EdenStadium.glowing("#2cb8c8")));
		host.world.getChildren().add(this.camera);
	}

	/** The dark metal behind a screen and the lines of light along its top and bottom. */
	private void frame(SurfaceMesh backs, LowPoly lines, int first, int pieces, double radius, double bottom, double top) {
		for (int piece = 0; piece < pieces; piece++) {
			double from = EdenStadium.angle(first + piece);
			double after = EdenStadium.angle(first + piece + 1);
			double[] facing = { -Math.cos((from + after) / 2), 0, -Math.sin((from + after) / 2) };
			backs.quad(EdenStadium.point(from, radius - 0.4, bottom + FRAME), EdenStadium.point(after, radius - 0.4, bottom + FRAME), EdenStadium.point(after, radius - 0.4, top - FRAME), EdenStadium.point(from, radius - 0.4, top - FRAME),
					new float[] { 0, 0, 1, 0, 1, 1, 0, 1 }, facing);
		}
		for (int piece = 0; piece < pieces; piece++) {
			double from = EdenStadium.angle(first + piece);
			double after = EdenStadium.angle(first + piece + 1);
			for (double height : new double[] { bottom + FRAME, top - FRAME + 1.5 }) {
				lines.frustum(new double[][] { EdenStadium.flat(from, radius - 2.5), EdenStadium.flat(from, radius - 0.4), EdenStadium.flat(after, radius - 0.4), EdenStadium.flat(after, radius - 2.5) }, height, height - 1.5, 1);
			}
		}
	}

	/** The glass of a screen, one strip for each piece, showing the part of the picture that fits the shape of the screen. */
	private void glass(SurfaceMesh screens, int first, int pieces, double radius, double bottom, double top) {
		double[] shown = this.shown(first, pieces, radius);
		for (int piece = 0; piece < pieces; piece++) {
			double from = EdenStadium.angle(first + piece);
			double after = EdenStadium.angle(first + piece + 1);
			float left = (float) (shown[0] + (shown[1] - shown[0]) * piece / pieces);
			float right = (float) (shown[0] + (shown[1] - shown[0]) * (piece + 1) / pieces);
			double[] facing = { -Math.cos((from + after) / 2), 0, -Math.sin((from + after) / 2) };
			screens.quad(EdenStadium.point(from, radius - 1, bottom), EdenStadium.point(after, radius - 1, bottom), EdenStadium.point(after, radius - 1, top), EdenStadium.point(from, radius - 1, top),
					new float[] { left, (float) shown[3], right, (float) shown[3], right, (float) shown[2], left, (float) shown[2] }, facing);
		}
	}

	/** The part of the picture a screen shows, as the left and right and the top and bottom: all of it, cut where the screen is not 16:9. */
	private double[] shown(int first, int pieces, double radius) {
		double width = 0;
		for (int piece = 0; piece < pieces; piece++) {
			double[] start = EdenStadium.point(EdenStadium.angle(first + piece), radius, 0);
			double[] end = EdenStadium.point(EdenStadium.angle(first + piece + 1), radius, 0);
			width += Math.hypot(end[0] - start[0], end[2] - start[2]);
		}
		double wide = width / HEIGHT / ASPECT;
		return wide > 1 ? new double[] { 0, 1, 0.5 - 0.5 / wide, 0.5 + 0.5 / wide } : new double[] { 0.5 - 0.5 * wide, 0.5 + 0.5 * wide, 0, 1 };
	}

	/** The last picture of the broadcast, or null before the first one. */
	WritableImage picture() {
		return this.picture;
	}

	/** How many pictures of the broadcast the screens have shown. */
	int pictures() {
		return this.pictures;
	}

	/** Moves the broadcast camera along its circle and, from time to time, takes a new picture for the screens. */
	void update(double deltaTime) {
		this.turn += deltaTime * LAPS;
		this.sinceLast += deltaTime;
		if (this.sinceLast >= SECONDS_BETWEEN) {
			this.sinceLast = 0;
			this.film();
		}
	}

	/** Lets the focus and the distance move toward the middle of the robots still fighting, and how far apart they are. */
	private void follow() {
		double sumX = 0;
		double sumZ = 0;
		int alive = 0;
		for (RobotView robot : host.robots.values()) {
			if (!robot.dead) {
				double[] position = robot.position();
				sumX += position[0];
				sumZ += position[2];
				alive++;
			}
		}
		double targetX = alive == 0 ? 0 : sumX / alive;
		double targetZ = alive == 0 ? 0 : sumZ / alive;
		double wide = 0;
		for (RobotView robot : host.robots.values()) {
			wide = robot.dead ? wide : Math.max(wide, Math.hypot(robot.position()[0] - targetX, robot.position()[2] - targetZ));
		}
		double ease = 1 - Math.exp(-SECONDS_BETWEEN * 2.5);
		this.focus[0] += (targetX - this.focus[0]) * ease;
		this.focus[2] += (targetZ - this.focus[2]) * ease;
		this.spread += (Math.min(FARTHEST, NEAREST + wide * 1.3) - this.spread) * ease;
	}

	/** Sets the broadcast camera on its circle around the fighters, high and looking down at them. */
	private void place() {
		this.follow();
		double angle = Math.PI * 2 * this.turn;
		double high = this.spread * 0.55;
		double eyeX = this.focus[0] + this.spread * Math.cos(angle);
		double eyeZ = this.focus[2] + this.spread * Math.sin(angle);
		double reach = Math.hypot(eyeX, eyeZ);
		double keep = reach > LEASH ? LEASH / reach : 1;
		eyeX *= keep;
		eyeZ *= keep;
		this.eye.setX(eyeX);
		this.eye.setY(-high);
		this.eye.setZ(eyeZ);
		double lookX = this.focus[0] - eyeX;
		double lookZ = this.focus[2] - eyeZ;
		this.yaw.setAngle(Math.toDegrees(Math.atan2(lookX, lookZ)));
		this.pitch.setAngle(-Math.toDegrees(Math.atan2(high, Math.hypot(lookX, lookZ))));
	}

	/** Draws the scene again from the broadcast camera, in a small picture, for the screens; nothing is drawn before the scene is shown. */
	private void film() {
		SubScene scene = host.subScene;
		if (scene.getScene() == null || scene.getWidth() < 2 || scene.getHeight() < 2) {
			return;
		}
		this.place();
		double scale = PICTURE_WIDTH / scene.getWidth();
		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setTransform(new Scale(scale, scale));
		parameters.setFill(Color.BLACK);
		int width = (int) Math.ceil(scene.getWidth() * scale);
		int height = (int) Math.ceil(scene.getHeight() * scale);
		if (this.picture == null || this.picture.getWidth() != width || this.picture.getHeight() != height) {
			this.picture = new WritableImage(width, height);
		}
		PerspectiveCamera main = (PerspectiveCamera) scene.getCamera();
		scene.setCamera(this.camera);
		try {
			scene.snapshot(parameters, this.picture);
		} finally {
			scene.setCamera(main);
		}
		this.glass.setSelfIlluminationMap(this.picture);
		this.pictures++;
	}
}
