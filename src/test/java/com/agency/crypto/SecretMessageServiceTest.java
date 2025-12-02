/*
 * Copyright 2024 Secret Agency
 */

package com.agency.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agency.crypto.dto.ReceiveMessageResponse;
import com.agency.crypto.dto.SaveMessageResponse;
import com.agency.crypto.service.AesEncryptionService;
import com.agency.crypto.service.PasswordService;
import com.agency.crypto.service.SecretMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the crypto services without NATS dependency.
 */
@DataJpaTest
@Import({SecretMessageService.class, AesEncryptionService.class, PasswordService.class})
@ActiveProfiles("unit-test")
public class SecretMessageServiceTest {

  @Autowired
  private SecretMessageService messageService;

  @Autowired
  private AesEncryptionService encryptionService;

  @Autowired
  private PasswordService passwordService;

  @Test
  @DisplayName("AES key generation produces valid Base64 key")
  void testAesKeyGeneration() {
    String key = encryptionService.generateKey();

    assertNotNull(key);
    assertTrue(key.length() > 0);
    // 256-bit key = 32 bytes = 44 Base64 characters (with padding)
    assertEquals(44, key.length());
  }

  @Test
  @DisplayName("IV generation produces correct length")
  void testIvGeneration() {
    byte[] iv = encryptionService.generateIv();

    assertNotNull(iv);
    assertEquals(12, iv.length); // GCM IV is 12 bytes
  }

  @Test
  @DisplayName("Encrypt and decrypt round trip")
  void testEncryptDecrypt() {
    String plaintext = "Top secret message for testing";
    String key = encryptionService.generateKey();
    byte[] iv = encryptionService.generateIv();

    byte[] encrypted = encryptionService.encrypt(plaintext, key, iv);
    String decrypted = encryptionService.decrypt(encrypted, key, iv);

    assertEquals(plaintext, decrypted);
  }

  @Test
  @DisplayName("Decrypt with wrong key throws exception")
  void testDecryptWithWrongKey() {
    String plaintext = "Secret message";
    String correctKey = encryptionService.generateKey();
    String wrongKey = encryptionService.generateKey();
    byte[] iv = encryptionService.generateIv();

    byte[] encrypted = encryptionService.encrypt(plaintext, correctKey, iv);

    assertThrows(RuntimeException.class, () -> {
      encryptionService.decrypt(encrypted, wrongKey, iv);
    });
  }

  @Test
  @DisplayName("Password generation produces secure password")
  void testPasswordGeneration() {
    String password = passwordService.generatePassword();

    assertNotNull(password);
    assertTrue(password.length() >= 16);
  }

  @Test
  @DisplayName("Save and receive message flow with auto-generated password")
  void testSaveAndReceiveMessage() {
    String secretMessage = "The password is swordfish";

    // Save message - password is auto-generated
    SaveMessageResponse saveResponse = messageService.saveMessage(secretMessage);

    assertTrue(saveResponse.isSuccess());
    assertNotNull(saveResponse.getId());
    assertNotNull(saveResponse.getPassword());
    assertNotNull(saveResponse.getAesKey());

    // Receive message using the auto-generated password
    ReceiveMessageResponse receiveResponse = messageService.receiveMessage(
        saveResponse.getId(), saveResponse.getPassword(), saveResponse.getAesKey());

    assertTrue(receiveResponse.isSuccess());
    assertEquals(secretMessage, receiveResponse.getMessage());
    assertTrue(receiveResponse.isDeleted());
  }

  @Test
  @DisplayName("Message not found returns appropriate response")
  void testMessageNotFound() {
    ReceiveMessageResponse response = messageService.receiveMessage(
        "non-existent-id", "password", "some-key");

    assertFalse(response.isSuccess());
    assertTrue(response.getErrorMessage().contains("not found"));
  }

  @Test
  @DisplayName("Wrong key decrements remaining tries")
  void testWrongKeyDecrementsTries() {
    String secretMessage = "Secret";
    SaveMessageResponse saveResponse = messageService.saveMessage(secretMessage);

    String wrongKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    ReceiveMessageResponse response = messageService.receiveMessage(
        saveResponse.getId(), saveResponse.getPassword(), wrongKey);

    assertFalse(response.isSuccess());
    assertEquals(2, response.getRemainingTries()); // 3 max - 1 failed = 2
    assertFalse(response.isDeleted());
  }

  @Test
  @DisplayName("Wrong password decrements remaining tries")
  void testWrongPasswordDecrementsTries() {
    String secretMessage = "Secret";
    SaveMessageResponse saveResponse = messageService.saveMessage(secretMessage);

    ReceiveMessageResponse response = messageService.receiveMessage(
        saveResponse.getId(), "wrongPassword", saveResponse.getAesKey());

    assertFalse(response.isSuccess());
    assertEquals(2, response.getRemainingTries()); // 3 max - 1 failed = 2
    assertFalse(response.isDeleted());
    assertTrue(response.getErrorMessage().contains("password"));
  }

  @Test
  @DisplayName("Empty message returns error")
  void testEmptyMessage() {
    SaveMessageResponse response = messageService.saveMessage("");

    assertFalse(response.isSuccess());
    assertTrue(response.getErrorMessage().contains("empty"));
  }

  @Test
  @DisplayName("Null message returns error")
  void testNullMessage() {
    SaveMessageResponse response = messageService.saveMessage(null);

    assertFalse(response.isSuccess());
    assertTrue(response.getErrorMessage().contains("empty"));
  }
}
