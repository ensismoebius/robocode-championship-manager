package utils.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ServletAuthenticatorTest {

	@Test
	void aTeamWithoutLoginOrPasswordIsNotLoggedIn() {
		assertNotNull(new ServletAuthenticator());
		assertFalse(ServletAuthenticator.authenticate(null, null, "pass1"));
		assertFalse(ServletAuthenticator.authenticate(null, "time1", null));
	}
}
