package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import support.FxSupport;

class PresenterTest {

	@Test
	void thePresenterComesUpForEveryTopicAndGoesDownWhenNothingIsSaid() {
		Presenter presenter = FxSupport.run(() -> new Presenter(200));
		FxSupport.show(presenter);

		for (Topic topic : Topic.values()) {
			FxSupport.runVoid(() -> presenter.present(topic, 1));
			FxSupport.sleep(100);
		}
		assertTrue(FxSupport.run(presenter::isVisible), "the presenter is on the screen while it speaks");

		// Saying the same topic again while it is up only holds it longer
		FxSupport.runVoid(() -> presenter.present(Topic.values()[0], 2));
		FxSupport.runVoid(() -> presenter.tick(0.5));
		FxSupport.runVoid(() -> presenter.tick(5));
		FxSupport.sleep(500);

		assertFalse(FxSupport.run(presenter::isVisible), "the presenter goes down when nothing is said for a while");
	}
}
