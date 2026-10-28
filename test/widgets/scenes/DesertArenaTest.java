package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.shape.TriangleMesh;

import widgets.Song;

/** The desert of the Middle East arena: the plain around the courtyard is flat, the dunes rise beyond it, and the arena has its song. */
class DesertArenaTest {

	private static final double WIDTH = 800;
	private static final double DEPTH = 600;

	@Test
	void thePlainIsFlatAndTheDunesRiseBeyondIt() {
		// The courtyard and the plain around it, up to the flat distance, have no dunes at all
		assertEquals(0, DesertDunes.height(0, 0, WIDTH, DEPTH), 1e-9);
		assertEquals(0, DesertDunes.height(300, 200, WIDTH, DEPTH), 1e-9);
		assertEquals(0, DesertDunes.height(600, 0, WIDTH, DEPTH), 1e-9);
		assertTrue(DesertDunes.height(WIDTH / 2 + (DesertDunes.FLAT + DesertDunes.END) / 2, 0, WIDTH, DEPTH) > 0, "no dune beyond the plain");
	}

	@Test
	void theDunesStayNearTheArenaAndTheCityBeyondThemIsFlat() {
		// Beyond the ring of dunes the ground is flat, in every direction, as far as the city goes
		for (double far = WIDTH / 2 + DesertDunes.END; far < 6000; far += 97) {
			assertEquals(0, DesertDunes.height(far, 0, WIDTH, DEPTH), 1e-9);
			assertEquals(0, DesertDunes.height(-far, 100, WIDTH, DEPTH), 1e-9);
			assertEquals(0, DesertDunes.height(0, far, WIDTH, DEPTH), 1e-9);
			assertEquals(0, DesertDunes.height(200, -far, WIDTH, DEPTH), 1e-9);
		}
		// In the ring the ground is never flat: the dunes have a base under their waves
		for (double along = DesertDunes.FLAT + 30; along < DesertDunes.END - 30; along += 10) {
			assertTrue(DesertDunes.height(WIDTH / 2 + along, 0, WIDTH, DEPTH) > 0, "flat ground in the ring of dunes at " + along);
		}
	}

	@Test
	void theArenaHasItsOwnDesertAndSong() {
		BattleArena3D arena = ArenaSimulation.arena(ArenaSimulation.fighters(), "MIDDLE_EAST");

		assertNotNull(arena.desert);
		assertEquals(Song.HIJAZ, ArenaStyle.MIDDLE_EAST.song);
	}

	@Test
	void thePointedArchNarrowsFromItsSpringlineToItsTip() {
		double half = 10;
		assertEquals(half, IslamicForms.archHalfWidth(-5, half), 1e-9);
		assertEquals(half, IslamicForms.archHalfWidth(0, half), 1e-9);
		double tip = Math.sqrt(3) * half;
		assertEquals(0, IslamicForms.archHalfWidth(tip, half), 1e-9);
		assertEquals(0, IslamicForms.archHalfWidth(tip + 5, half), 1e-9);
		double before = half;
		for (double height = 1; height < tip; height += 1) {
			double now = IslamicForms.archHalfWidth(height, half);
			assertTrue(now < before, "the arch does not narrow at " + height);
			before = now;
		}
	}

	@Test
	void theMonumentsStandOnLevelGround() {
		for (double[] site : DesertDunes.SITES) {
			double level = DesertDunes.height(site[0], site[1], WIDTH, DEPTH);
			for (double angle = 0; angle < Math.PI * 2; angle += 0.5) {
				double posX = site[0] + Math.cos(angle) * site[2] * 0.95;
				double posZ = site[1] + Math.sin(angle) * site[2] * 0.95;
				assertEquals(level, DesertDunes.height(posX, posZ, WIDTH, DEPTH), 1e-9, "the ground is not level at a site");
			}
		}
	}

	@Test
	void theMosaicIsTheStarAndCrossInTheColoursOfTheZellij() {
		Set<Integer> colours = new HashSet<>();
		for (int row = 0; row < 256; row++) {
			for (int column = 0; column < 256; column++) {
				int colour = DesertTexture.mosaicPixel(column, row);
				colours.add(colour);
				// The pattern looks the same mirrored and turned by a quarter, as the star and cross does
				assertEquals(colour, DesertTexture.mosaicPixel(255 - column, row));
				assertEquals(colour, DesertTexture.mosaicPixel(row, column));
			}
		}
		// Cobalt, emerald, honey, turquoise and the plaster, and no other colour
		assertEquals(Set.of(0x2450b0, 0x23935c, 0xe3ac47, 0x22b5ae, 0xf3e6c4), colours);
		// The heart of the star is a cobalt dot in a honey one, and the cross has a honey diamond at the corner of the tile
		assertEquals(0x2450b0, DesertTexture.mosaicPixel(128, 128));
		assertEquals(0xe3ac47, DesertTexture.mosaicPixel(0, 0));
	}

	/** The colour of a pixel of a picture, without its opacity */
	private static int rgb(Image picture, int column, int row) {
		PixelReader pixels = picture.getPixelReader();
		return pixels.getArgb(column, row) & 0xffffff;
	}

	@Test
	void theLinesOfPlasterOfTheMosaicShineAndNothingElseDoes() {
		for (int row = 0; row < 256; row++) {
			for (int column = 0; column < 256; column++) {
				boolean plaster = DesertTexture.mosaicPixel(column, row) == 0xf3e6c4;
				assertEquals(plaster, rgb(DesertTexture.MOSAIC_LIGHT, column, row) != 0, "the light of the mosaic is not on its plaster only");
				assertEquals(plaster, rgb(DesertTexture.FLOOR_LIGHT, column, row) != 0, "the light of the floor is not on its plaster only");
			}
		}
	}

	@Test
	void theCeramicOfTheTownIsPaleAndItsWindowsOutlinedInLight() {
		long brightness = 0;
		for (int row = 0; row < 256; row++) {
			for (int column = 0; column < 256; column++) {
				int colour = rgb(DesertTexture.PANELS, column, row);
				brightness += (colour >> 16 & 0xff) + (colour >> 8 & 0xff) + (colour & 0xff);
			}
		}
		assertTrue(brightness / (256 * 256 * 3) > 180, "the ceramic is not pale");
		Set<Integer> shining = new HashSet<>();
		for (int row = 0; row < 128; row++) {
			for (int column = 0; column < 128; column++) {
				shining.add(rgb(DesertTexture.WINDOWS, column, row));
			}
		}
		// Black where nothing shines, the thin line of the trim of the arches, and the cyan and the gold of the lit windows
		assertEquals(Set.of(0x000000, 0x2a9fb0, 0x5ff0ff, 0xffc860), shining);
	}

	@Test
	void anArchIsVeiledWithLight() {
		IslamicForms forms = new IslamicForms();
		assertTrue(forms.views().isEmpty(), "there is something before anything is built");
		forms.veil(0, 0, true, 0, 30, 36);
		assertTrue(((TriangleMesh) forms.views().get(0).getMesh()).getFaces().size() > 0, "no veil was built");
	}

	@Test
	void theSkyscrapersStandOnTheirOwnPlazasBeyondTheDunes() {
		DesertCity city = new DesertCity(WIDTH, DEPTH);
		List<double[]> towers = city.towers();
		assertTrue(towers.size() >= 8, "too few skyscrapers");
		for (int index = 0; index < towers.size(); index++) {
			double[] tower = towers.get(index);
			int column = DesertCity.block(tower[0]);
			int row = DesertCity.block(tower[1]);
			assertTrue(city.built(column, row), "a skyscraper stands among the dunes");
			assertEquals(DesertCity.Kind.PLAZA, city.kind(column, row));
			for (double[] site : DesertDunes.SITES) {
				assertTrue(DesertCity.block(site[0]) != column || DesertCity.block(site[1]) != row, "a skyscraper stands on a monument");
			}
			for (int other = index + 1; other < towers.size(); other++) {
				assertTrue(Math.hypot(tower[0] - towers.get(other)[0], tower[1] - towers.get(other)[1]) >= 700, "two skyscrapers are too close");
			}
		}
	}

	@Test
	void theMonumentsStandOnPlazasAndTheHousesOnLotsBeyondTheDunes() {
		DesertCity city = new DesertCity(WIDTH, DEPTH);
		for (double[] site : DesertDunes.SITES) {
			assertEquals(DesertCity.Kind.PLAZA, city.kind(DesertCity.block(site[0]), DesertCity.block(site[1])));
			// A monument is in the middle of its block, so it keeps clear of the streets
			double[] middle = DesertCity.centre(DesertCity.block(site[0]), DesertCity.block(site[1]));
			assertEquals(middle[0], site[0], 1e-9);
			assertEquals(middle[1], site[1], 1e-9);
		}
		for (double[] house : city.houses(new Random(7))) {
			assertTrue(DesertDunes.outside(house[0], house[1], WIDTH, DEPTH) >= DesertDunes.END, "a house among the dunes");
			// Every house is inside its block, away from the streets
			double alongX = Math.floorMod((long) Math.floor(house[0]), (long) DesertCity.PITCH);
			double alongZ = Math.floorMod((long) Math.floor(house[1]), (long) DesertCity.PITCH);
			assertTrue(alongX > DesertCity.STREET / 2 + 70 && alongX < DesertCity.PITCH - DesertCity.STREET / 2 - 70, "a house on a street");
			assertTrue(alongZ > DesertCity.STREET / 2 + 70 && alongZ < DesertCity.PITCH - DesertCity.STREET / 2 - 70, "a house on a street");
			assertEquals(DesertCity.Kind.LOT, city.kind(DesertCity.block(house[0]), DesertCity.block(house[1])));
		}
		int gardens = 0;
		int blocks = 0;
		for (int column = -12; column < 12; column++) {
			for (int row = -12; row < 12; row++) {
				if (city.built(column, row) && city.kind(column, row) != DesertCity.Kind.PLAZA) {
					blocks++;
					gardens += city.kind(column, row) == DesertCity.Kind.GARDEN ? 1 : 0;
				}
			}
		}
		assertTrue(gardens > blocks / 10 && gardens < blocks / 2, "the gardens are " + gardens + " of " + blocks + " blocks");
	}
}
