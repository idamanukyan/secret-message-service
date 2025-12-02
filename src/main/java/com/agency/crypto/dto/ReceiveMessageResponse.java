package com.agency.crypto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for receiving a secret message. Contains the decrypted message on success or error
 * information on failure.
 */
public class ReceiveMessageResponse {

  @JsonProperty("message")
  private String message;

  @JsonProperty("success")
  private boolean success;

  @JsonProperty("errorMessage")
  private String errorMessage;

  @JsonProperty("remainingTries")
  private Integer remainingTries;

  @JsonProperty("deleted")
  private boolean deleted;

  /** Default constructor. */
  public ReceiveMessageResponse() {}

  /**
   * Creates a successful response with the decrypted message.
   *
   * @param message the decrypted message
   * @return a new ReceiveMessageResponse indicating success
   */
  public static ReceiveMessageResponse success(String message) {
    ReceiveMessageResponse response = new ReceiveMessageResponse();
    response.message = message;
    response.success = true;
    response.deleted = true;
    return response;
  }

  /**
   * Creates an error response indicating wrong key with remaining tries.
   *
   * @param remainingTries the number of remaining attempts
   * @return a new ReceiveMessageResponse indicating failure
   */
  public static ReceiveMessageResponse wrongKey(int remainingTries) {
    ReceiveMessageResponse response = new ReceiveMessageResponse();
    response.success = false;
    response.errorMessage = "Invalid AES key. Decryption failed.";
    response.remainingTries = remainingTries;
    response.deleted = remainingTries <= 0;
    return response;
  }

  /**
   * Creates an error response indicating wrong credentials (password or key) with remaining tries.
   *
   * @param errorMessage the specific error message
   * @param remainingTries the number of remaining attempts
   * @return a new ReceiveMessageResponse indicating failure
   */
  public static ReceiveMessageResponse wrongCredentials(String errorMessage, int remainingTries) {
    ReceiveMessageResponse response = new ReceiveMessageResponse();
    response.success = false;
    response.errorMessage = errorMessage;
    response.remainingTries = remainingTries;
    response.deleted = remainingTries <= 0;
    return response;
  }

  /**
   * Creates an error response indicating the message was not found.
   *
   * @return a new ReceiveMessageResponse indicating the message was not found
   */
  public static ReceiveMessageResponse notFound() {
    ReceiveMessageResponse response = new ReceiveMessageResponse();
    response.success = false;
    response.errorMessage = "Message not found or already deleted.";
    response.deleted = true;
    return response;
  }

  /**
   * Creates a generic error response.
   *
   * @param errorMessage the error message
   * @return a new ReceiveMessageResponse indicating failure
   */
  public static ReceiveMessageResponse error(String errorMessage) {
    ReceiveMessageResponse response = new ReceiveMessageResponse();
    response.success = false;
    response.errorMessage = errorMessage;
    return response;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Integer getRemainingTries() {
    return remainingTries;
  }

  public void setRemainingTries(Integer remainingTries) {
    this.remainingTries = remainingTries;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }
}
