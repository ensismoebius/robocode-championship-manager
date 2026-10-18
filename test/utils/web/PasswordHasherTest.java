package utils.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

	@Test
	void verifiesHashedPasswords() {
		String hash = PasswordHasher.hash("s3cret");

		assertTrue(hash.startsWith("pbkdf2$"));
		assertTrue(PasswordHasher.verify("s3cret", hash));
		assertFalse(PasswordHasher.verify("wrong", hash));
	}

	@Test
	void saltsEveryHash() {
		assertNotEquals(PasswordHasher.hash("same"), PasswordHasher.hash("same"));
	}

	@Test
	void acceptsLegacyPlainTextPasswords() {
		assertTrue(PasswordHasher.verify("old", "old"));
		assertFalse(PasswordHasher.verify("other", "old"));
		assertFalse(PasswordHasher.verify(null, "old"));
	}

	@Test
	void escapesHtml() {
		assertTrue(Html.escape("<a href=\"x\">'&'</a>").equals("&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;"));
	}
}
