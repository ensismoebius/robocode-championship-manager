package widgets.scenes;

import static widgets.scenes.BattleArena3D.FENCE_BASE;
import static widgets.scenes.BattleArena3D.box;
import static widgets.scenes.BattleArena3D.material;

import javafx.animation.Timeline;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;

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

	ArenaFence(BattleArena3D host) {
		this.host = host;
	}

	/**
	 * Invisible panels around the field, they flash when hit
	 */
	void buildFence(double w, double h) {
		// Force field panels: 100% transparent/invisible until hit
		double wallHeight = 80;
		for (int wall = 0; wall < 4; wall++) {
			double span = wall <= 1 ? w : h;
			int count = (int) Math.round(span / 100);
			for (int index = 0; index < count; index++) {
				double along = index * 100 + 50 - span / 2;
				double normal = wall <= 1 ? h / 2 : w / 2;
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

		FenceSegment(int wall, int index, Box box, PhongMaterial material) {
			this.wall = wall;
			this.index = index;
			this.box = box;
			this.material = material;
		}

		int wall() {
			return wall;
		}

		int index() {
			return index;
		}

		Box box() {
			return box;
		}

		PhongMaterial material() {
			return material;
		}
	}
}
