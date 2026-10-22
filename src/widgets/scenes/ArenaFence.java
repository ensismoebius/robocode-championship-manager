package widgets.scenes;

import static widgets.scenes.BattleArena3D.FENCE_BASE;
import static widgets.scenes.ArenaMaterials.box;
import static widgets.scenes.ArenaMaterials.material;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.util.Duration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The force field of the battle arena: the invisible panels around the field
 * that flash when a bullet crosses them.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ArenaFence {

	private final BattleArena3D host;

	/** Creates the fence of the arena, drawn in the host scene. */
	ArenaFence(BattleArena3D host) {
		this.host = host;
	}

	/**
	 * Invisible panels around the field, they flash when hit
	 */
	void buildFence(double width, double height) {
		// Force field panels: 100% transparent/invisible until hit
		double wallHeight = 80;
		for (int wall = 0; wall < 4; wall++) {
			double span = wall <= 1 ? width : height;
			int count = (int) Math.round(span / 100);
			for (int index = 0; index < count; index++) {
				double along = index * 100 + 50 - span / 2;
				double normal = wall <= 1 ? height / 2 : width / 2;
				PhongMaterial panelMaterial = material(FENCE_BASE);
				Box panel = wall <= 1 ? box(98, wallHeight, 1.0, along, -wallHeight / 2, wall == 0 ? -normal : normal, panelMaterial)
						: box(1.0, wallHeight, 98, wall == 2 ? -normal : normal, -wallHeight / 2, along, panelMaterial);
				panel.setVisible(false);
				host.fence.getChildren().add(panel);
				host.fenceSegments.add(new FenceSegment(wall, index, panel, panelMaterial));
			}
		}
	}

	/**
	 * One segment of the invisible force field. It only appears when struck.
	 */
	static class FenceSegment {
		final int wall;
		final int index;
		final Box box;
		final PhongMaterial material;
		Timeline animation;

		/** A segment of the fence: its wall, its index in the wall, its box and its material. */
		FenceSegment(int wall, int index, Box box, PhongMaterial material) {
			this.wall = wall;
			this.index = index;
			this.box = box;
			this.material = material;
		}

		/** The wall of the segment. */
		int wall() {
			return wall;
		}

		/** The index of the segment in its wall. */
		int index() {
			return index;
		}

		/** The box of the segment. */
		Box box() {
			return box;
		}

		/** The material of the segment. */
		PhongMaterial material() {
			return material;
		}
	}

	/**
	 * The cage lights up where a robot or a bullet hit it, the glow spreads to
	 * the neighboring panels like a shockwave
	 */
	void flashFence(double posX, double posY) {
		double width = host.fieldWidth;
		double height = host.fieldHeight;
		double cxValue = Math.max(0, Math.min(width, posX));
		double cyValue = Math.max(0, Math.min(height, posY));

		double[] distances = { cyValue, height - cyValue, cxValue, width - cxValue };
		int wall = 0;
		for (int index2 = 1; index2 < distances.length; index2++) {
			if (distances[index2] < distances[wall]) {
				wall = index2;
			}
		}

		double along = wall <= 1 ? cxValue : cyValue;
		int count = (int) Math.round((wall <= 1 ? width : height) / 100);
		int index = Math.max(0, Math.min(count - 1, (int) Math.floor(along / 100)));

		this.flashSegment(wall, index, 1.0, Duration.ZERO);
		this.flashSegment(wall, index - 1, 0.6, Duration.millis(60));
		this.flashSegment(wall, index + 1, 0.6, Duration.millis(60));
		this.flashSegment(wall, index - 2, 0.3, Duration.millis(120));
		this.flashSegment(wall, index + 2, 0.3, Duration.millis(120));
	}

	/** Flashes the fence segments of a wall at the given index, with the given intensity, after the delay. */
	void flashSegment(int wall, int index, double intensity, Duration delay) {
		for (FenceSegment segment : host.fenceSegments) {
			if (segment.wall() != wall || segment.index() != index) {
				continue;
			}
			this.flashShield(segment, intensity, delay);
			if (intensity >= 0.8) {
				this.pulseSegment(segment);
			}
			return;
		}
	}

	/** The force field of one fence segment lights up after the delay and fades out */
	private void flashShield(FenceSegment segment, double intensity, Duration delay) {
		if (segment.animation != null) {
			segment.animation.stop();
			segment.animation = null;
		}

		segment.box().setVisible(true);

		// Electric force field energy flare: transparent translucent barrier
		double peakAlpha = 0.20 + 0.08 * Math.min(1.0, intensity);
		Color tint = host.style.fence;
		Color shieldBright = Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), peakAlpha);
		Color shieldMid = Color.color(tint.getRed() * 0.6, tint.getGreen() * 0.8, tint.getBlue(), peakAlpha * 0.45);
		Color shieldFade = Color.color(tint.getRed() * 0.4, tint.getGreen() * 0.8, tint.getBlue(), 0.0);

		segment.material().setSpecularColor(Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.20));

		Timeline flash = new Timeline();
		flash.getKeyFrames().addAll(
				new KeyFrame(delay, new KeyValue(segment.material().diffuseColorProperty(), shieldBright)),
				new KeyFrame(delay.add(Duration.millis(90)), new KeyValue(segment.material().diffuseColorProperty(), shieldMid)),
				new KeyFrame(delay.add(Duration.millis(320)), new KeyValue(segment.material().diffuseColorProperty(), shieldFade)));
		flash.setOnFinished(event -> {
			segment.box().setVisible(false);
			segment.material().setDiffuseColor(FENCE_BASE);
			segment.material().setSpecularColor(Color.TRANSPARENT);
			segment.animation = null;
		});
		segment.animation = flash;
		flash.play();
	}

	/** A strong hit makes the segment bulge a little */
	private void pulseSegment(FenceSegment segment) {
		ScaleTransition pulse = new ScaleTransition(Duration.millis(90), segment.box());
		pulse.setFromX(1);
		pulse.setToX(1.04);
		pulse.setFromY(1);
		pulse.setToY(1.04);
		pulse.setFromZ(1);
		pulse.setToZ(1.04);
		pulse.setCycleCount(2);
		pulse.setAutoReverse(true);
		pulse.play();
	}

	/** Back to the base look when a new round starts */
	void resetFence() {
		for (FenceSegment segment : host.fenceSegments) {
			if (segment.animation != null) {
				segment.animation.stop();
				segment.animation = null;
			}
			segment.box().setVisible(false);
			segment.material().setDiffuseColor(FENCE_BASE);
			segment.material().setSpecularColor(Color.TRANSPARENT);
			segment.box().setScaleX(1);
			segment.box().setScaleY(1);
			segment.box().setScaleZ(1);
		}
	}
}
