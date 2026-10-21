package utils.web;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * PBKDF2 password hashing. Hashes are stored as
 * "pbkdf2$iterations$salt$hash" (salt and hash in base64).
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class PasswordHasher {

	private static final String PREFIX = "pbkdf2";
	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 210_000;
	private static final int SALT_BYTES = 16;
	private static final int HASH_BITS = 256;

	private static final SecureRandom RANDOM = new SecureRandom();

	public static String hash(String password) {
		byte[] salt = new byte[SALT_BYTES];
		RANDOM.nextBytes(salt);
		byte[] hash = PasswordHasher.pbkdf2(password, salt, ITERATIONS);

		Base64.Encoder encoder = Base64.getEncoder();
		return PREFIX + "$" + ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
	}

	public static boolean verify(String password, String stored) {
		if (password == null || stored == null) {
			return false;
		}

		String[] parts = stored.split("\\$");

		// Passwords saved by older versions are in plain text
		if (parts.length != 4 || !parts[0].equals(PREFIX)) {
			return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
		}

		try {
			int iterations = Integer.parseInt(parts[1]);
			Base64.Decoder decoder = Base64.getDecoder();
			byte[] salt = decoder.decode(parts[2]);
			byte[] expected = decoder.decode(parts[3]);
			return MessageDigest.isEqual(expected, PasswordHasher.pbkdf2(password, salt, iterations));
		} catch (IllegalArgumentException illegalArgumentException) {
			return false;
		}
	}

	private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
			return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
		} catch (GeneralSecurityException generalSecurityException) {
			throw new IllegalStateException(generalSecurityException);
		}
	}
}
