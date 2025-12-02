/*
 * Copyright 2024 Secret Agency
 */

package com.agency.crypto;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agency.crypto.dto.ReceiveMessageRequest;
import com.agency.crypto.dto.ReceiveMessageResponse;
import com.agency.crypto.dto.SaveMessageRequest;
import com.agency.crypto.dto.SaveMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the Secret Message Service using NATS messaging. This test class simulates
 * the jUnit NATS Client from the architecture diagram.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NatsClientIntegrationTest {

  private static final String SUBJECT_SAVE = "save.msg";
  private static final String SUBJECT_RECEIVE = "receive.msg";

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${nats.server.url:nats://localhost:4222}")
  private String natsUrl;

  private Connection clientConnection;

  // Shared state between tests
  private String savedMessageId;
  private String savedPassword;
  private String savedAesKey;

  @BeforeAll
  void setUp() throws Exception {
    // Wait for NATS server to be ready
    await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
      try {
        clientConnection = Nats.connect(natsUrl);
        return clientConnection.getStatus() == Connection.Status.CONNECTED;
      } catch (Exception ex) {
        return false;
      }
    });
  }

  @AfterAll
  void tearDown() throws Exception {
    if (clientConnection != null) {
      clientConnection.close();
    }
  }

  @Test
  @Order(1)
  @DisplayName("Creator: Save secret message and receive ID, password and AES key")
  void testSaveSecretMessage() throws Exception {
    // Given: A secret message from the creator (password is auto-generated)
    String secretMessage = "The eagle has landed at midnight. Proceed to extraction point.";
    SaveMessageRequest request = new SaveMessageRequest(secretMessage);
    String requestJson = objectMapper.writeValueAsString(request);

    // When: Send save request via NATS
    Message response = clientConnection.request(
        SUBJECT_SAVE,
        requestJson.getBytes(StandardCharsets.UTF_8),
        Duration.ofSeconds(10)
    );

    // Then: Receive ID, password and AES key
    assertNotNull(response, "Should receive a response from crypto service");

    SaveMessageResponse saveResponse = objectMapper.readValue(
        response.getData(), SaveMessageResponse.class);

    assertTrue(saveResponse.isSuccess(), "Save should be successful");
    assertNotNull(saveResponse.getId(), "Should receive a message ID");
    assertNotNull(saveResponse.getPassword(), "Should receive an auto-generated password");
    assertNotNull(saveResponse.getAesKey(), "Should receive an AES key");
    assertNull(saveResponse.getErrorMessage(), "Should not have error message");

    // Store for next test
    savedMessageId = saveResponse.getId();
    savedPassword = saveResponse.getPassword();
    savedAesKey = saveResponse.getAesKey();

    System.out.println("=== Creator received ===");
    System.out.println("Message ID: " + savedMessageId);
    System.out.println("Password: " + savedPassword);
    System.out.println("AES Key: " + savedAesKey);
    System.out.println("These must be shared with recipient via secure second factor!");
  }

  @Test
  @Order(2)
  @DisplayName("Recipient: Receive secret message with correct password and AES key")
  void testReceiveSecretMessageWithCorrectKey() throws Exception {
    // Given: The recipient has received ID, password and AES key via secure channel
    assertNotNull(savedMessageId, "Message ID should be available from previous test");
    assertNotNull(savedPassword, "Password should be available from previous test");
    assertNotNull(savedAesKey, "AES key should be available from previous test");

    ReceiveMessageRequest request = new ReceiveMessageRequest(savedMessageId, savedPassword, savedAesKey);
    String requestJson = objectMapper.writeValueAsString(request);

    // When: Send receive request via NATS
    Message response = clientConnection.request(
        SUBJECT_RECEIVE,
        requestJson.getBytes(StandardCharsets.UTF_8),
        Duration.ofSeconds(10)
    );

    // Then: Receive decrypted message
    assertNotNull(response, "Should receive a response from crypto service");

    ReceiveMessageResponse receiveResponse = objectMapper.readValue(
        response.getData(), ReceiveMessageResponse.class);

    assertTrue(receiveResponse.isSuccess(), "Receive should be successful");
    assertEquals(
        "The eagle has landed at midnight. Proceed to extraction point.",
        receiveResponse.getMessage(),
        "Decrypted message should match original"
    );
    assertTrue(receiveResponse.isDeleted(), "Message should be deleted after retrieval");

    System.out.println("=== Recipient received ===");
    System.out.println("Secret Message: " + receiveResponse.getMessage());
    System.out.println("Message has been deleted from server!");
  }

  @Test
  @Order(3)
  @DisplayName("Verify message is deleted after successful retrieval")
  void testMessageDeletedAfterRetrieval() throws Exception {
    // Given: The message was already retrieved in previous test
    ReceiveMessageRequest request = new ReceiveMessageRequest(savedMessageId, savedPassword, savedAesKey);
    String requestJson = objectMapper.writeValueAsString(request);

    // When: Try to retrieve again
    Message response = clientConnection.request(
        SUBJECT_RECEIVE,
        requestJson.getBytes(StandardCharsets.UTF_8),
        Duration.ofSeconds(10)
    );

    // Then: Message should not be found
    assertNotNull(response, "Should receive a response");

    ReceiveMessageResponse receiveResponse = objectMapper.readValue(
        response.getData(), ReceiveMessageResponse.class);

    assertFalse(receiveResponse.isSuccess(), "Should fail - message already deleted");
    assertNull(receiveResponse.getMessage(), "Should not contain message");
    assertTrue(
        receiveResponse.getErrorMessage().contains("not found"),
        "Error should indicate message not found"
    );

    System.out.println("=== Verification ===");
    System.out.println("Message is confirmed deleted: " + receiveResponse.getErrorMessage());
  }

  @Test
  @Order(4)
  @DisplayName("Test wrong AES key - decrement tries")
  void testWrongAesKey() throws Exception {
    // Given: Save a new message
    String secretMessage = "New secret for wrong key test";
    SaveMessageRequest saveRequest = new SaveMessageRequest(secretMessage);
    String saveJson = objectMapper.writeValueAsString(saveRequest);

    Message saveResponse = clientConnection.request(
        SUBJECT_SAVE,
        saveJson.getBytes(StandardCharsets.UTF_8),
        Duration.ofSeconds(10)
    );

    SaveMessageResponse saved = objectMapper.readValue(
        saveResponse.getData(), SaveMessageResponse.class);
    String messageId = saved.getId();
    String password = saved.getPassword();

    // When: Try with correct password but wrong AES key
    String wrongKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="; // Wrong 256-bit key
    ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest(messageId, password, wrongKey);
    String receiveJson = objectMapper.writeValueAsString(receiveRequest);

    Message response = clientConnection.request(
        SUBJECT_RECEIVE,
        receiveJson.getBytes(StandardCharsets.UTF_8),
        Duration.ofSeconds(10)
    );

    // Then: Should fail with remaining tries
    ReceiveMessageResponse receiveResponse = objectMapper.readValue(
        response.getData(), ReceiveMessageResponse.class);

    assertFalse(receiveResponse.isSuccess(), "Should fail with wrong key");
    assertEquals(2, receiveResponse.getRemainingTries(), "Should have 2 remaining tries");
    assertFalse(receiveResponse.isDeleted(), "Message should not be deleted yet");

    System.out.println("=== Wrong Key Attempt ===");
    System.out.println("Remaining tries: " + receiveResponse.getRemainingTries());
  }

  @Test
  @Order(5)
  @DisplayName("Test message deletion after max failed attempts")
  void testMessageDeletionAfterMaxFailedAttempts() throws Exception {
    // Given: Save a new message
    String secretMessage = "Message to be deleted after failed attempts";
    SaveMessageRequest saveRequest = new SaveMessageRequest(secretMessage);
    String saveJson = objectMapper.writeValueAsString(saveRequest);

    Message saveResponse = clientConnection.request(
        SUBJECT_SAVE,
        saveJson.getBytes(StandardCharsets.UTF_8),
        Duration.ofSeconds(10)
    );

    SaveMessageResponse saved = objectMapper.readValue(
        saveResponse.getData(), SaveMessageResponse.class);
    String messageId = saved.getId();
    String password = saved.getPassword();

    String wrongKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    // When: Make 3 failed attempts with correct password but wrong key
    for (int idx = 1; idx <= 3; idx++) {
      ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest(messageId, password, wrongKey);
      String receiveJson = objectMapper.writeValueAsString(receiveRequest);

      Message response = clientConnection.request(
          SUBJECT_RECEIVE,
          receiveJson.getBytes(StandardCharsets.UTF_8),
          Duration.ofSeconds(10)
      );

      ReceiveMessageResponse receiveResponse = objectMapper.readValue(
          response.getData(), ReceiveMessageResponse.class);

      System.out.println(
          "Attempt " + idx + ": Remaining tries = " + receiveResponse.getRemainingTries()
              + ", Deleted = " + receiveResponse.isDeleted());

      if (idx == 3) {
        // After 3rd attempt, message should be deleted
        assertEquals(0, receiveResponse.getRemainingTries(), "Should have 0 remaining tries");
        assertTrue(receiveResponse.isDeleted(), "Message should be deleted");
      }
    }

    // Then: Verify message is gone
    ReceiveMessageRequest verifyRequest = new ReceiveMessageRequest(messageId, password, wrongKey);
    String verifyJson = objectMapper.writeValueAsString(verifyRequest);

    Message verifyResponse = clientConnection.request(
        SUBJECT_RECEIVE,
        verifyJson.getBytes(StandardCharsets.UTF_8),
        Duration.ofSeconds(10)
    );

    ReceiveMessageResponse verifyResult = objectMapper.readValue(
        verifyResponse.getData(), ReceiveMessageResponse.class);

    assertFalse(verifyResult.isSuccess(), "Should fail - message deleted");
    assertTrue(
        verifyResult.getErrorMessage().contains("not found"),
        "Should indicate message not found"
    );

    System.out.println("=== Message Deleted After Max Attempts ===");
    System.out.println("Message confirmed deleted after 3 failed attempts");
  }
}
