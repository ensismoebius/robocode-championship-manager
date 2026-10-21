package utils.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class HtmlTest {

	@Test
	void theTextIsEscapedForTheBrowser() {
		assertNotNull(new Html());
		assertEquals("&lt;b&gt;&quot;oi&quot;&amp;", Html.escape("<b>\"oi\"&"));
	}
}
