package widgets.scenes;

import static widgets.scenes.ArenaMaterials.material;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The airships of the Eden Solar sky: slow envelopes of pale fabric with a band of solar cells on the back, a strip of cyan light
 * and a gondola lit in warm light, high over the city, each going round the stadium at its own height and speed.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class EdenAir {

	/** How far the airships go from the middle, how high they are, and how fast they go, in units a second. */
	private static final double NEAREST = 3300;
	private static final double FARTHEST = 6500;
	private static final double LOWEST = 1250;
	private static final double HIGHEST = 1900;
	private static final double SPEED = 14;

	/** An airship on its orbit around the stadium. */
	private static final class Ship {

		private final Group node;
		private final double orbit;
		private final double altitude;
		private final double omega;
		private double angle;

		/** Puts the airship on an orbit, at an angle. */
		Ship(Group node, double orbit, double altitude, double angle, double speed) {
			this.node = node;
			this.orbit = orbit;
			this.altitude = altitude;
			this.angle = angle;
			this.omega = speed / orbit;
		}

		/** Moves it along its orbit, nose to the way it goes. */
		void move(double seconds) {
			this.angle += this.omega * seconds;
			this.node.setTranslateX(this.orbit * Math.cos(this.angle));
			this.node.setTranslateY(-this.altitude);
			this.node.setTranslateZ(this.orbit * Math.sin(this.angle));
			this.node.setRotate(Math.toDegrees(Math.atan2(-Math.sin(this.angle), Math.cos(this.angle))));
		}
	}

	private final BattleArena3D host;
	private final List<Ship> ships = new ArrayList<>();

	/** The airships of the host scene. */
	EdenAir(BattleArena3D host) {
		this.host = host;
	}

	/** How many airships there are. */
	int count() {
		return this.ships.size();
	}

	/** Builds the airships, each at its own place on its orbit. */
	void build(Random wild) {
		this.ships.clear();
		PhongMaterial fabric = material(Color.web("#e6dcc4"));
		PhongMaterial cells = material(Color.web("#12345a"));
		for (int index = 0; index < 5; index++) {
			LowPoly body = new LowPoly();
			double radius = 58 + wild.nextDouble() * 24;
			body.spindle(0, 0, 0, radius, radius * 3.6, radius * 3.6, 10);
			LowPoly back = new LowPoly();
			back.box(0, -radius * 0.82, 0, radius * 0.9, radius * 0.28, radius * 4.2);
			LowPoly stripe = new LowPoly();
			stripe.box(0, radius * 0.05, 0, radius * 2.05, radius * 0.1, radius * 3.2);
			LowPoly gondola = new LowPoly();
			gondola.box(0, radius * 1.18, 0, radius * 0.5, radius * 0.4, radius * 1.4);
			Group ship = new Group(body.view(fabric), back.view(cells), stripe.view(EdenStadium.glowing("#3be0d0")), gondola.view(EdenStadium.glowing("#ffb24a")));
			double orbit = NEAREST + (FARTHEST - NEAREST) * (index + wild.nextDouble() * 0.6) / 5;
			double altitude = LOWEST + (HIGHEST - LOWEST) * wild.nextDouble();
			Ship placed = new Ship(ship, orbit, altitude, Math.PI * 2 * (index + wild.nextDouble()) / 5, SPEED * (0.7 + 0.6 * wild.nextDouble()) * (index % 2 == 0 ? 1 : -1));
			placed.move(0);
			this.ships.add(placed);
			host.arena.getChildren().add(ship);
		}
	}

	/** The airships go on along their orbits. */
	void update(double seconds) {
		for (Ship ship : this.ships) {
			ship.move(seconds);
		}
	}
}
