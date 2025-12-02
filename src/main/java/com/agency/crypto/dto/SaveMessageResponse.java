package com.agency.crypto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for a saved secret message. Contains the generated ID, password and AES key
 * that must be shared with the recipient via a secure second factor.
 */
public class SaveMessageResponse {

  @JsonProperty("id")
  private String id;

  @JsonProperty("password")
  private String password;

  @JsonProperty("aesKey")
  private String aesKey;

  @JsonProperty("success")
  private boolean success;

  @JsonProperty("errorMessage")
  private String errorMessage;

  /** Default constructor. */
  public SaveMessageResponse() {}

  /**
   * Creates a successful response with ID, password and AES key.
   *
   * @param id the message ID
   * @param password the auto-generated password
   * @param aesKey the Base64-encoded AES key
   */
  public SaveMessageResponse(String id, String password, String aesKey) {
    this.id = id;
    this.password = password;
    this.aesKey = aesKey;
    this.success = true;
  }

  /**
   * Creates an error response with the given message.
   *
   * @param errorMessage the error message
   * @return a new SaveMessageResponse indicating failure
   */
  public static SaveMessageResponse error(String errorMessage) {
    SaveMessageResponse response = new SaveMessageResponse();
    response.success = false;
    response.errorMessage = errorMessage;
    return response;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAesKey() {
    return aesKey;
  }

  public void setAesKey(String aesKey) {
    this.aesKey = aesKey;
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
}
