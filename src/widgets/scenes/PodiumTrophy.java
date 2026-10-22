package widgets.scenes;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.SVGPath;
import javafx.scene.Group;
import javafx.scene.paint.Color;

/**
 * The trophy of a place: a cup on its base, in the colour of the place
 */
final class PodiumTrophy {

	/**
	 * A golden cup, with handles, a star and a base
	 */
	static Group trophy(Color color) {
		Color light = color.interpolate(Color.WHITE, 0.55);
		Color dark = color.interpolate(Color.BLACK, 0.45);
		LinearGradient metal = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop(0, dark), new Stop(0.3, light), new Stop(0.55, color), new Stop(1, dark));
		SVGPath cup = new SVGPath();
		cup.setContent("M20,0 L140,0 L140,45 C140,100 112,122 80,128 C48,122 20,100 20,45 Z");
		SVGPath left = new SVGPath();
		left.setContent("M22,14 C-14,12 -12,72 32,88 L38,76 C8,64 8,30 22,28 Z");
		SVGPath right = new SVGPath();
		right.setContent("M138,14 C174,12 172,72 128,88 L122,76 C152,64 152,30 138,28 Z");
		SVGPath stem = new SVGPath();
		stem.setContent("M68,126 L92,126 L96,160 L64,160 Z");
		SVGPath base = new SVGPath();
		base.setContent("M40,160 L120,160 L132,190 L28,190 Z");
		for (SVGPath part : List.of(cup, left, right, stem, base)) {
			part.setFill(metal);
			part.setStroke(dark);
			part.setStrokeWidth(2);
		}
		SVGPath shine = new SVGPath();
		shine.setContent("M32,8 L46,8 L50,52 C52,84 66,104 80,112 C52,104 34,84 32,50 Z");
		shine.setFill(Color.color(1, 1, 1, 0.55));
		Polygon star = new Polygon();
		double cxValue = 80;
		double cyValue = 52;
		for (int index = 0; index < 10; index++) {
			double radius = index % 2 == 0 ? 24 : 10;
			double angle = -Math.PI / 2 + Math.PI * index / 5;
			star.getPoints().addAll(cxValue + Math.cos(angle) * radius, cyValue + Math.sin(angle) * radius);
		}
		star.setFill(Color.web("#fff6c0"));
		star.setStroke(dark);
		Group group = new Group(left, right, stem, base, cup, shine, star);
		group.setEffect(new DropShadow(36, color));
		return group;
	}
}
