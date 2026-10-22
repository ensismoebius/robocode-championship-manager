/**
 * 
 */
package utils.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Network {

	/** Only static methods: this class is not instantiated. */
	private Network() {
	}

	/** The address of this machine on the local network, to show to the other computers. */
	public static String getLocalIpAddress() {
		Enumeration<NetworkInterface> enumeration;
		try {
			enumeration = NetworkInterface.getNetworkInterfaces();

			while (enumeration.hasMoreElements()) {
				NetworkInterface networkInterface = enumeration.nextElement();
				Enumeration<InetAddress> enumeration2 = networkInterface.getInetAddresses();

				while (enumeration2.hasMoreElements()) {
					InetAddress inetAddress = enumeration2.nextElement();

					if (inetAddress.isSiteLocalAddress()) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException socketException) {
			socketException.printStackTrace();
		}
		return "localhost";
	}
}
