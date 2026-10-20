package widgets.scenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TopicTest {

	@Test
	void everyPresenterHasItsPicture() throws Exception {
		Set<String> names = new HashSet<String>();
		for (Topic topic : Topic.values()) {
			try (InputStream in = Topic.class.getResourceAsStream(topic.image())) {
				assertNotNull(in, topic.image());
				assertTrue(in.readNBytes(8).length == 8);
			}
			names.add(topic.presenter);
		}
		// One presenter for each kind of event
		assertEquals(Topic.values().length, names.size());
	}
}
