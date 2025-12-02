package com.agency.crypto.service;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for AES encryption and decryption operations. Uses AES-GCM mode for authenticated
 * encryption.
 */
@Service
public class AesEncryptionService {

  private static final Logger logger = LoggerFactory.getLogger(AesEncryptionService.class);

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;
  private static final int IV_LENGTH = 12;

  private final int keyLength;
  private final SecureRandom secureRandom;

  /**
   * Creates a new AesEncryptionService with configurable key length.
   *
   * @param keyLength the AES key length in bits (128, 192, or 256)
   */
  public AesEncryptionService(
      @Value("${crypto.aes.key-length:256}") int keyLength) {
    this.keyLength = keyLength;
    this.secureRandom = new SecureRandom();
    logger.info("AesEncryptionService initialized with key length: {} bits", keyLength);
  }

  /**
   * Generates a new random AES key.
   *
   * @return the Base64-encoded AES key
   */
  public String generateKey() {
    try {
      KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
      keyGen.init(keyLength, secureRandom);
      SecretKey secretKey = keyGen.generateKey();
      String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
      logger.debug("Generated new AES key with length: {} bits", keyLength);
      return encodedKey;
    } catch (Exception ex) {
      logger.error("Failed to generate AES key", ex);
      throw new RuntimeException("Failed to generate AES key", ex);
    }
  }

  /**
   * Generates a random initialization vector (IV).
   *
   * @return the IV bytes
   */
  public byte[] generateIv() {
    byte[] iv = new byte[IV_LENGTH];
    secureRandom.nextBytes(iv);
    return iv;
  }

  /**
   * Encrypts the given plaintext with the provided AES key.
   *
   * @param plaintext the text to encrypt
   * @param base64Key the Base64-encoded AES key
   * @param iv the initialization vector
   * @return the encrypted bytes
   */
  public byte[] encrypt(String plaintext, String base64Key, byte[] iv) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(base64Key);
      SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

      byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
      logger.debug("Successfully encrypted message of length: {}", plaintext.length());
      return encrypted;
    } catch (Exception ex) {
      logger.error("Encryption failed", ex);
      throw new RuntimeException("Encryption failed", ex);
    }
  }

  /**
   * Decrypts the given ciphertext with the provided AES key.
   *
   * @param ciphertext the encrypted bytes
   * @param base64Key the Base64-encoded AES key
   * @param iv the initialization vector
   * @return the decrypted plaintext
   * @throws RuntimeException if decryption fails (wrong key or corrupted data)
   */
  public String decrypt(byte[] ciphertext, String base64Key, byte[] iv) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(base64Key);
      SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

      byte[] decrypted = cipher.doFinal(ciphertext);
      logger.debug("Successfully decrypted message");
      return new String(decrypted, "UTF-8");
    } catch (Exception ex) {
      logger.warn("Decryption failed - possibly wrong key: {}", ex.getMessage());
      throw new RuntimeException("Decryption failed", ex);
    }
  }
}
