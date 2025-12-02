package com.agency.crypto.controller;

import com.agency.crypto.dto.ReceiveMessageRequest;
import com.agency.crypto.dto.ReceiveMessageResponse;
import com.agency.crypto.dto.SaveMessageRequest;
import com.agency.crypto.dto.SaveMessageResponse;
import com.agency.crypto.service.SecretMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * NATS message handler that subscribes to save.msg and receive.msg subjects. Handles incoming
 * requests and sends responses back via NATS reply subjects.
 */
@Component
public class NatsMessageHandler {

  private static final Logger logger = LoggerFactory.getLogger(NatsMessageHandler.class);

  private static final String SUBJECT_SAVE = "save.msg";
  private static final String SUBJECT_RECEIVE = "receive.msg";

  private final Connection natsConnection;
  private final SecretMessageService messageService;
  private final ObjectMapper objectMapper;

  private Dispatcher dispatcher;

  /**
   * Creates a new NatsMessageHandler.
   *
   * @param natsConnection the NATS connection
   * @param messageService the secret message service
   * @param objectMapper the JSON object mapper
   */
  public NatsMessageHandler(
      Connection natsConnection,
      SecretMessageService messageService,
      ObjectMapper objectMapper) {
    this.natsConnection = natsConnection;
    this.messageService = messageService;
    this.objectMapper = objectMapper;
  }

  /**
   * Initializes the NATS subscriptions after the bean is constructed.
   */
  @PostConstruct
  public void init() {
    logger.info("Initializing NATS message handlers");

    dispatcher = natsConnection.createDispatcher();

    dispatcher.subscribe(SUBJECT_SAVE, this::handleSaveMessage);
    logger.info("Subscribed to subject: {}", SUBJECT_SAVE);

    dispatcher.subscribe(SUBJECT_RECEIVE, this::handleReceiveMessage);
    logger.info("Subscribed to subject: {}", SUBJECT_RECEIVE);

    logger.info("NATS message handlers initialized successfully");
  }

  /**
   * Cleans up NATS subscriptions before the bean is destroyed.
   */
  @PreDestroy
  public void cleanup() {
    logger.info("Cleaning up NATS message handlers");
    if (dispatcher != null) {
      natsConnection.closeDispatcher(dispatcher);
    }
  }

  /**
   * Handles incoming save message requests.
   *
   * @param msg the NATS message
   */
  private void handleSaveMessage(Message msg) {
    logger.debug("Received save message request");

    try {
      String requestJson = new String(msg.getData(), StandardCharsets.UTF_8);
      SaveMessageRequest request = objectMapper.readValue(requestJson, SaveMessageRequest.class);

      logger.info("Processing save message request");
      SaveMessageResponse response = messageService.saveMessage(request.getMessage());

      sendReply(msg, response);
      logger.info("Save message response sent. Success: {}", response.isSuccess());

    } catch (Exception ex) {
      logger.error("Error handling save message request", ex);
      sendErrorReply(msg, "Failed to process save request: " + ex.getMessage());
    }
  }

  /**
   * Handles incoming receive message requests.
   *
   * @param msg the NATS message
   */
  private void handleReceiveMessage(Message msg) {
    logger.debug("Received receive message request");

    try {
      String requestJson = new String(msg.getData(), StandardCharsets.UTF_8);
      ReceiveMessageRequest request = objectMapper.readValue(
          requestJson, ReceiveMessageRequest.class);

      logger.info("Processing receive message request for ID: {}", request.getId());
      ReceiveMessageResponse response = messageService.receiveMessage(
          request.getId(), request.getPassword(), request.getAesKey());

      sendReply(msg, response);
      logger.info("Receive message response sent. Success: {}", response.isSuccess());

    } catch (Exception ex) {
      logger.error("Error handling receive message request", ex);
      sendErrorReply(msg, "Failed to process receive request: " + ex.getMessage());
    }
  }

  /**
   * Sends a reply message.
   *
   * @param originalMsg the original message to reply to
   * @param response the response object
   */
  private void sendReply(Message originalMsg, Object response) {
    if (originalMsg.getReplyTo() == null) {
      logger.warn("No reply-to subject specified, cannot send response");
      return;
    }

    try {
      String responseJson = objectMapper.writeValueAsString(response);
      natsConnection.publish(originalMsg.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
      logger.debug("Reply sent to: {}", originalMsg.getReplyTo());
    } catch (Exception ex) {
      logger.error("Failed to send reply", ex);
    }
  }

  /**
   * Sends an error reply for save message requests.
   *
   * @param originalMsg the original message to reply to
   * @param errorMessage the error message
   */
  private void sendErrorReply(Message originalMsg, String errorMessage) {
    String subject = originalMsg.getSubject();
    Object errorResponse;

    if (SUBJECT_SAVE.equals(subject)) {
      errorResponse = SaveMessageResponse.error(errorMessage);
    } else {
      errorResponse = ReceiveMessageResponse.error(errorMessage);
    }

    sendReply(originalMsg, errorResponse);
  }
}
