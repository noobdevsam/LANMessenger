package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SecurityUtils {

    public static String hashPassword(String plaintextInput) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] salt = "LAN_MESSENGER_SALT_BYTES_XYZ".getBytes();
            digest.update(salt);
            byte[] hashedBytes = digest.digest(plaintextInput.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 cryptographic engine failure.", e);
        }
    }

    public static boolean verifyPassword(String input, String storedHash) {
        return hashPassword(input).equals(storedHash);
    }
}