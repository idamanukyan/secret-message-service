package com.agency.crypto.service;

import com.agency.crypto.dto.ReceiveMessageResponse;
import com.agency.crypto.dto.SaveMessageResponse;
import com.agency.crypto.entity.SecretMessage;
import com.agency.crypto.exception.DecryptionException;
import com.agency.crypto.exception.MessageNotFoundException;
import com.agency.crypto.repository.SecretMessageRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing secret messages. Handles saving encrypted messages and retrieving them with
 * proper decryption and self-destruction logic.
 */
@Service
public class SecretMessageService {

  private static final Logger logger = LoggerFactory.getLogger(SecretMessageService.class);

  private final SecretMessageRepository repository;
  private final AesEncryptionService encryptionService;
  private final PasswordService passwordService;
  private final int maxTries;

  /**
   * Creates a new SecretMessageService.
   *
   * @param repository the message repository
   * @param encryptionService the AES encryption service
   * @param passwordService the password hashing service
   * @param maxTries maximum number of decryption attempts allowed
   */
  public SecretMessageService(
      SecretMessageRepository repository,
      AesEncryptionService encryptionService,
      PasswordService passwordService,
      @Value("${crypto.max-tries:3}") int maxTries) {
    this.repository = repository;
    this.encryptionService = encryptionService;
    this.passwordService = passwordService;
    this.maxTries = maxTries;
    logger.info("SecretMessageService initialized with maxTries: {}", maxTries);
  }

  /**
   * Saves a new secret message. Generates a random AES key and password, encrypts the message,
   * hashes the password, stores them, and returns the ID, password and key. The key and password
   * are NOT stored in the database - only the password hash.
   *
   * @param plaintext the secret message to save
   * @return response containing the message ID, auto-generated password and AES key
   */
  @Transactional
  public SaveMessageResponse saveMessage(String plaintext) {
    logger.info("Saving new secret message");

    if (plaintext == null || plaintext.isEmpty()) {
      logger.warn("Attempted to save empty message");
      return SaveMessageResponse.error("Message cannot be empty");
    }

    try {
      String aesKey = encryptionService.generateKey();
      logger.debug("Generated AES key for new message");

      String password = passwordService.generatePassword();
      logger.debug("Generated password for new message");

      byte[] iv = encryptionService.generateIv();

      byte[] encryptedContent = encryptionService.encrypt(plaintext, aesKey, iv);
      logger.debug("Message encrypted successfully");

      String passwordHash = passwordService.hashPassword(password);
      logger.debug("Password hashed successfully");

      String messageId = UUID.randomUUID().toString();

      SecretMessage secretMessage = new SecretMessage(messageId, encryptedContent, iv, passwordHash);
      repository.save(secretMessage);
      logger.info("Secret message saved with ID: {}", messageId);

      return new SaveMessageResponse(messageId, password, aesKey);
    } catch (Exception ex) {
      logger.error("Failed to save secret message", ex);
      return SaveMessageResponse.error("Failed to save message: " + ex.getMessage());
    }
  }

  /**
   * Retrieves and decrypts a secret message. Both password and AES key must be correct.
   * If validation fails, the try counter is incremented. After maxTries failed attempts,
   * the message is deleted. On success, the message is also deleted (self-destruct).
   *
   * @param messageId the message ID
   * @param password the password to unlock the message
   * @param aesKey the Base64-encoded AES key
   * @return response containing the decrypted message or error information
   */
  @Transactional
  public ReceiveMessageResponse receiveMessage(String messageId, String password, String aesKey) {
    logger.info("Attempting to receive message with ID: {}", messageId);

    if (messageId == null || messageId.isEmpty()) {
      logger.warn("Attempted to receive message with empty ID");
      return ReceiveMessageResponse.error("Message ID cannot be empty");
    }

    if (password == null || password.isEmpty()) {
      logger.warn("Attempted to receive message with empty password");
      return ReceiveMessageResponse.error("Password cannot be empty");
    }

    if (aesKey == null || aesKey.isEmpty()) {
      logger.warn("Attempted to receive message with empty AES key");
      return ReceiveMessageResponse.error("AES key cannot be empty");
    }

    Optional<SecretMessage> optionalMessage = repository.findById(messageId);
    if (optionalMessage.isEmpty()) {
      logger.warn("Message not found with ID: {}", messageId);
      return ReceiveMessageResponse.notFound();
    }

    SecretMessage secretMessage = optionalMessage.get();

    if (!passwordService.verifyPassword(password, secretMessage.getPasswordHash())) {
      logger.warn("Password verification failed for message {}", messageId);
      return handleFailedAttempt(secretMessage, "Invalid password");
    }

    try {
      String decryptedMessage = encryptionService.decrypt(
          secretMessage.getEncryptedContent(),
          aesKey,
          secretMessage.getIv()
      );

      repository.delete(secretMessage);
      logger.info("Message {} successfully decrypted and deleted", messageId);

      return ReceiveMessageResponse.success(decryptedMessage);

    } catch (RuntimeException ex) {
      logger.warn("Decryption failed for message {}: {}", messageId, ex.getMessage());
      return handleFailedAttempt(secretMessage, "Invalid AES key. Decryption failed.");
    }
  }

  /**
   * Handles a failed authentication/decryption attempt by incrementing the try counter
   * and potentially deleting the message.
   */
  private ReceiveMessageResponse handleFailedAttempt(SecretMessage secretMessage, String errorMessage) {
    secretMessage.incrementTryCount();
    int remainingTries = maxTries - secretMessage.getTryCount();

    if (remainingTries <= 0) {
      repository.delete(secretMessage);
      logger.warn("Message {} deleted after {} failed attempts", secretMessage.getId(), maxTries);
      return ReceiveMessageResponse.wrongCredentials(errorMessage, 0);
    } else {
      repository.save(secretMessage);
      logger.info("Message {} has {} remaining tries", secretMessage.getId(), remainingTries);
      return ReceiveMessageResponse.wrongCredentials(errorMessage, remainingTries);
    }
  }

  /**
   * Deletes a message by ID. Used internally for cleanup operations.
   *
   * @param messageId the message ID to delete
   */
  @Transactional
  public void deleteMessage(String messageId) {
    logger.info("Deleting message with ID: {}", messageId);
    repository.deleteById(messageId);
  }

  /**
   * Gets the configured maximum number of tries.
   *
   * @return the maximum number of decryption attempts allowed
   */
  public int getMaxTries() {
    return maxTries;
  }
}
