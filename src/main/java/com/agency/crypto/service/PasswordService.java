package com.agency.crypto.service;

import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * Service for password generation, hashing and verification using BCrypt.
 */
@Service
public class PasswordService {

  private static final Logger logger = LoggerFactory.getLogger(PasswordService.class);
  private static final int BCRYPT_ROUNDS = 12;
  private static final String PASSWORD_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

  private final SecureRandom secureRandom;
  private final int passwordLength;

  public PasswordService(@Value("${crypto.password.length:16}") int passwordLength) {
    this.secureRandom = new SecureRandom();
    this.passwordLength = passwordLength;
    logger.info("PasswordService initialized with BCrypt rounds: {}, password length: {}",
        BCRYPT_ROUNDS, passwordLength);
  }

  /**
   * Generates a cryptographically secure random password.
   *
   * @return the generated password
   */
  public String generatePassword() {
    StringBuilder password = new StringBuilder(passwordLength);
    for (int i = 0; i < passwordLength; i++) {
      int index = secureRandom.nextInt(PASSWORD_CHARS.length());
      password.append(PASSWORD_CHARS.charAt(index));
    }
    logger.debug("Generated new password with length: {}", passwordLength);
    return password.toString();
  }

  /**
   * Hashes a password using BCrypt.
   *
   * @param password the plaintext password
   * @return the BCrypt hash
   */
  public String hashPassword(String password) {
    String salt = BCrypt.gensalt(BCRYPT_ROUNDS, secureRandom);
    String hash = BCrypt.hashpw(password, salt);
    logger.debug("Password hashed successfully");
    return hash;
  }

  /**
   * Verifies a password against a BCrypt hash.
   *
   * @param password the plaintext password to check
   * @param hash the BCrypt hash to check against
   * @return true if the password matches, false otherwise
   */
  public boolean verifyPassword(String password, String hash) {
    boolean matches = BCrypt.checkpw(password, hash);
    logger.debug("Password verification result: {}", matches);
    return matches;
  }
}
