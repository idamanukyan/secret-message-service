package com.agency.crypto.exception;

/**
 * Exception thrown when decryption fails, typically due to an invalid AES key.
 */
public class DecryptionException extends RuntimeException {

  private final int remainingTries;
  private final boolean messageDeleted;

  /**
   * Creates a new DecryptionException with the given parameters.
   *
   * @param message the error message
   * @param remainingTries the number of remaining attempts
   * @param messageDeleted whether the message was deleted due to exceeded attempts
   */
  public DecryptionException(String message, int remainingTries, boolean messageDeleted) {
    super(message);
    this.remainingTries = remainingTries;
    this.messageDeleted = messageDeleted;
  }

  public int getRemainingTries() {
    return remainingTries;
  }

  public boolean isMessageDeleted() {
    return messageDeleted;
  }
}
