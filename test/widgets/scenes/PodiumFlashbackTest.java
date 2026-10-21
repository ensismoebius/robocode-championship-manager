package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import model.BattleMoment;
import model.Opponent;
import support.ChampionshipFixture;
import support.FxSupport;
import support.TestConfiguration;
import utils.configuration.Configuration;

class PodiumFlashbackTest {

	/** Hits and destructions in both directions, so every kind of caption of a clip is written. */
	private static final List<BattleMoment> MOMENTS = List.of( //
			ChampionshipFixture.moment("F1", 1, 2, 1, false), //
			ChampionshipFixture.moment("F1", 1, 3, 2, true), //
			ChampionshipFixture.moment("V1-1", 2, 1, 1, false), //
			ChampionshipFixture.moment("V1-1", 3, 1, 2, true));

	private static Node find(Node root, Predicate<Node> match) {
		if (match.test(root)) {
			return root;
		}
		if (root instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				Node found = find(child, match);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/** The title of the flashback, which is only visible while the page of the moments is open. */
	private static Label flashbackTitle(Node ceremony) {
		return (Label) find(ceremony, node -> node instanceof Label label && label.getText() != null && label.getText().startsWith("MOMENTOS DE"));
	}

	/** Tells whether the node and all of its parents are visible. */
	private static boolean shown(Node node) {
		for (Node current = node; current != null; current = current.getParent()) {
			if (!current.isVisible()) {
				return false;
			}
		}
		return true;
	}

	/** The caption of the photo on screen, sampled once a second while the clips cycle. */
	private static Set<String> captions(Node ceremony) {
		Set<String> captions = new TreeSet<String>();
		for (int sample = 0; sample < 3; sample++) {
			FxSupport.sleep(1000);
			Label caption = (Label) FxSupport.run(() -> find(ceremony, node -> node instanceof Label label && label.getStyleClass().contains("podium-cry") && !label.getText().isEmpty()));
			if (caption != null) {
				captions.add(FxSupport.run(caption::getText));
			}
		}
		return captions;
	}

	@Test
	void theFlashbackOfEachRobotCyclesItsPhotosAndClosesBack() {
		TestConfiguration.load(Configuration.RANDOM, "/nowhere");
		List<Opponent> podium = ChampionshipFixture.opponents(3);
		Podium ceremony = FxSupport.run(() -> new Podium(podium, MOMENTS, ChampionshipFixture.byId(podium)));
		FxSupport.show(ceremony);
		FxSupport.runVoid(ceremony::play);

		Set<String> captions = new TreeSet<String>();
		for (int step = 0; step < 3; step++) {
			FxSupport.runVoid(ceremony::next);
			// The robot is seen first, then its photos come one after another
			FxSupport.sleep(2000);
			assertTrue(FxSupport.run(() -> shown(flashbackTitle(ceremony))), "the robot of the place shows its moments");
			captions.addAll(captions(ceremony));
		}
		FxSupport.runVoid(ceremony::previous);
		FxSupport.sleep(500);
		assertFalse(FxSupport.run(() -> shown(flashbackTitle(ceremony))), "the moments close when the ceremony goes back");
		FxSupport.runVoid(ceremony::stop);

		assertTrue(captions.stream().anyMatch(caption -> caption.contains("ACERTOU") || caption.contains("DESTRU")), captions.toString());
		assertFalse(captions.isEmpty());
	}
}
