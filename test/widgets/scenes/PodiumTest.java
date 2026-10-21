package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.animation.Animation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.stage.Stage;
import model.BattleMoment;
import model.Opponent;
import support.ChampionshipFixture;
import support.FxSupport;

class PodiumTest {

	private static final List<BattleMoment> MOMENTS = List.of(//
			ChampionshipFixture.moment("F1", 1, 2, 1, false), //
			ChampionshipFixture.moment("F1", 1, 3, 2, true), //
			ChampionshipFixture.moment("V1-1", 2, 3, 1, false));

	@Test
	void theCeremonyRevealsEveryPlaceAndItsFlashbacks() {
		List<Opponent> podium = ChampionshipFixture.opponents(3);
		Podium ceremony = FxSupport.run(() -> new Podium(podium, MOMENTS, ChampionshipFixture.byId(podium)));

		Stage window = FxSupport.show(ceremony);
		FxSupport.runVoid(ceremony::play);
		FxSupport.sleep(300);
		// Forward through the reveals, then back again, with the hint updated on each step
		for (int step = 0; step < 6; step++) {
			FxSupport.runVoid(ceremony::next);
			FxSupport.sleep(150);
		}
		for (int step = 0; step < 3; step++) {
			FxSupport.runVoid(ceremony::previous);
			FxSupport.sleep(100);
		}
		FxSupport.runVoid(() -> {
			ceremony.startMusic(0.5);
			ceremony.musicVolumeProperty().set(0.3);
			ceremony.updateHint();
		});
		FxSupport.sleep(300);
		FxSupport.runVoid(ceremony::stop);
		FxSupport.close(window);
	}

	@Test
	void theCeremonyWithoutMomentsStillCrowns() {
		List<Opponent> podium = ChampionshipFixture.opponents(3);
		Podium ceremony = FxSupport.run(() -> new Podium(podium, List.of(), ChampionshipFixture.byId(podium)));

		Stage window = FxSupport.show(ceremony);
		FxSupport.runVoid(ceremony::play);
		for (int step = 0; step < 6; step++) {
			FxSupport.runVoid(ceremony::next);
			FxSupport.sleep(120);
		}
		FxSupport.runVoid(ceremony::stop);
		FxSupport.close(window);
	}

	@Test
	void thePodiumAnimationsBuildTheirNodes() {
		List<Opponent> podium = ChampionshipFixture.opponents(3);
		Podium ceremony = FxSupport.run(() -> new Podium(podium, MOMENTS, ChampionshipFixture.byId(podium)));
		FxSupport.runVoid(() -> {
			Group cup = Podium.trophy(Color.GOLD);
			assertNotNull(cup);
			Node sparkle = new Box(1, 1, 1);
			ceremony.sparkles(cup);
			for (Animation animation : List.of(
					Podium.slam(sparkle, 0, 100),
					Podium.drop(sparkle, 0, 100),
					Podium.zoomSpin(sparkle, 0, 100),
					Podium.delayed(0.1, Podium.rise(sparkle, 0), Podium.slam(sparkle, 0, 50)),
					Podium.rise(sparkle, 0))) {
				assertNotNull(animation);
				animation.play();
				animation.stop();
			}
		});
	}
}
