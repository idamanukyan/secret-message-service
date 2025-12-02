package com.agency.crypto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for receiving (decrypting) a secret message. The recipient must provide the message
 * ID, password, and the AES key received via secure second factor.
 */
public class ReceiveMessageRequest {

  @JsonProperty("id")
  private String id;

  @JsonProperty("password")
  private String password;

  @JsonProperty("aesKey")
  private String aesKey;

  /** Default constructor. */
  public ReceiveMessageRequest() {}

  /**
   * Creates a new ReceiveMessageRequest with the given parameters.
   *
   * @param id the message ID
   * @param password the password to unlock the message
   * @param aesKey the Base64-encoded AES key
   */
  public ReceiveMessageRequest(String id, String password, String aesKey) {
    this.id = id;
    this.password = password;
    this.aesKey = aesKey;
  }

  public String getId() {
    return id != null ? id.trim() : null;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPassword() {
    return password != null ? password.trim() : null;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAesKey() {
    return aesKey != null ? aesKey.trim() : null;
  }

  public void setAesKey(String aesKey) {
    this.aesKey = aesKey;
  }
}
