package com.agency.crypto.exception;

/**
 * Exception thrown when a secret message is not found in the database.
 */
public class MessageNotFoundException extends RuntimeException {

  /**
   * Creates a new MessageNotFoundException with the given message ID.
   *
   * @param messageId the ID of the message that was not found
   */
  public MessageNotFoundException(String messageId) {
    super("Message not found with ID: " + messageId);
  }
}
