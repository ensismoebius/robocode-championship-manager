package support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import javafx.scene.layout.Region;

class FxSupportTest {

	@Test
	void runsCodeOnTheToolkitThread() {
		Region region = FxSupport.run(Region::new);
		assertEquals(0, region.getChildrenUnmodifiable().size());
	}
}
