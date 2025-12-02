/*
 * Copyright 2024 Secret Agency
 */

package com.agency.crypto;

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
import java.util.Scanner;

/**
 * Standalone NATS client for manually testing the Secret Message Service. This simulates the jUnit
 * NATS Client from the architecture diagram.
 *
 * <p>Run after starting docker-compose: java -cp build/libs/crypto-service.jar
 * com.agency.crypto.StandaloneNatsClient
 */
public class StandaloneNatsClient {

  private static final String NATS_URL = "nats://localhost:4222";
  private static final String SUBJECT_SAVE = "save.msg";
  private static final String SUBJECT_RECEIVE = "receive.msg";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private final Connection connection;
  private final ObjectMapper objectMapper;

  /**
   * Creates a new StandaloneNatsClient.
   *
   * @throws Exception if connection fails
   */
  public StandaloneNatsClient() throws Exception {
    this.connection = Nats.connect(NATS_URL);
    this.objectMapper = new ObjectMapper();
    System.out.println("Connected to NATS server at " + NATS_URL);
  }

  /**
   * Saves a secret message and returns the ID, auto-generated password and AES key.
   *
   * @param secretMessage the message to save
   * @return the save response containing ID, password and key
   * @throws Exception if request fails
   */
  public SaveMessageResponse saveMessage(String secretMessage) throws Exception {
    SaveMessageRequest request = new SaveMessageRequest(secretMessage);
    String requestJson = objectMapper.writeValueAsString(request);

    System.out.println("\nSending save request...");
    Message response = connection.request(
        SUBJECT_SAVE,
        requestJson.getBytes(StandardCharsets.UTF_8),
        TIMEOUT
    );

    if (response == null) {
      throw new RuntimeException("No response from server");
    }

    return objectMapper.readValue(response.getData(), SaveMessageResponse.class);
  }

  /**
   * Receives and decrypts a secret message.
   *
   * @param messageId the message ID
   * @param password the password to unlock the message
   * @param aesKey the AES key
   * @return the receive response
   * @throws Exception if request fails
   */
  public ReceiveMessageResponse receiveMessage(String messageId, String password, String aesKey) throws Exception {
    ReceiveMessageRequest request = new ReceiveMessageRequest(messageId, password, aesKey);
    String requestJson = objectMapper.writeValueAsString(request);

    System.out.println("\nSending receive request...");
    Message response = connection.request(
        SUBJECT_RECEIVE,
        requestJson.getBytes(StandardCharsets.UTF_8),
        TIMEOUT
    );

    if (response == null) {
      throw new RuntimeException("No response from server");
    }

    return objectMapper.readValue(response.getData(), ReceiveMessageResponse.class);
  }

  /** Closes the NATS connection. */
  public void close() throws Exception {
    connection.close();
  }

  /**
   * Main method for interactive testing.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    try {
      StandaloneNatsClient client = new StandaloneNatsClient();
      Scanner scanner = new Scanner(System.in);

      System.out.println("\n=== Secret Agency Message Service Client ===");
      System.out.println("Commands: save, receive, quit\n");

      while (true) {
        System.out.print("Enter command: ");
        String command = scanner.nextLine().trim().toLowerCase();

        switch (command) {
          case "save":
            System.out.print("Enter secret message: ");
            String message = scanner.nextLine();

            SaveMessageResponse saveResponse = client.saveMessage(message);
            if (saveResponse.isSuccess()) {
              System.out.println("\n*** MESSAGE SAVED ***");
              System.out.println("ID: " + saveResponse.getId());
              System.out.println("Password: " + saveResponse.getPassword());
              System.out.println("AES Key: " + saveResponse.getAesKey());
              System.out.println("*** Share ID, password, and AES key via secure channel! ***\n");
            } else {
              System.out.println("Error: " + saveResponse.getErrorMessage());
            }
            break;

          case "receive":
            System.out.print("Enter message ID: ");
            String idInput = scanner.nextLine().trim();
            System.out.print("Enter password: ");
            String passwordInput = scanner.nextLine().trim();
            System.out.print("Enter AES key: ");
            String keyInput = scanner.nextLine().trim();

            ReceiveMessageResponse receiveResponse = client.receiveMessage(idInput, passwordInput, keyInput);
            if (receiveResponse.isSuccess()) {
              System.out.println("\n*** SECRET MESSAGE ***");
              System.out.println(receiveResponse.getMessage());
              System.out.println("*** Message has been deleted ***\n");
            } else {
              System.out.println("Error: " + receiveResponse.getErrorMessage());
              if (receiveResponse.getRemainingTries() != null) {
                System.out.println("Remaining tries: " + receiveResponse.getRemainingTries());
              }
              if (receiveResponse.isDeleted()) {
                System.out.println("Message has been deleted!");
              }
            }
            break;

          case "quit":
          case "exit":
            client.close();
            System.out.println("Goodbye, Agent.");
            return;

          default:
            System.out.println("Unknown command. Use: save, receive, quit");
        }
      }
    } catch (Exception ex) {
      System.err.println("Error: " + ex.getMessage());
      ex.printStackTrace();
    }
  }
}
