package utils.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;

import org.junit.jupiter.api.Test;

class NetworkTest {

	@Test
	void theAddressShownToTheTeamsIsOfTheLocalNetworkOrTheLocalhost() throws Exception {
		String address = Network.getLocalIpAddress();

		if ("localhost".equals(address)) {
			// No local network interface in this machine: the teams can only reach the program here
			assertEquals("localhost", address);
		} else {
			assertTrue(InetAddress.getByName(address).isSiteLocalAddress(), address);
		}
	}
}
