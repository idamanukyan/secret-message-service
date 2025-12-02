package com.agency.crypto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entity representing an encrypted secret message stored in the database. The message is encrypted
 * with AES and the key is NOT stored - only the encrypted content, IV, and password hash.
 */
@Entity
@Table(name = "secret_messages")
public class SecretMessage {

  @Id
  @Column(name = "id", nullable = false, length = 36)
  private String id;

  @Lob
  @Column(name = "encrypted_content", nullable = false)
  private byte[] encryptedContent;

  @Column(name = "iv", nullable = false)
  private byte[] iv;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "try_count", nullable = false)
  private int tryCount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Default constructor for JPA. */
  public SecretMessage() {}

  /**
   * Creates a new SecretMessage with the given parameters.
   *
   * @param id unique identifier for the message
   * @param encryptedContent the AES encrypted content
   * @param iv the initialization vector used for encryption
   * @param passwordHash the BCrypt hash of the password
   */
  public SecretMessage(String id, byte[] encryptedContent, byte[] iv, String passwordHash) {
    this.id = id;
    this.encryptedContent = encryptedContent;
    this.iv = iv;
    this.passwordHash = passwordHash;
    this.tryCount = 0;
    this.createdAt = Instant.now();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public byte[] getEncryptedContent() {
    return encryptedContent;
  }

  public void setEncryptedContent(byte[] encryptedContent) {
    this.encryptedContent = encryptedContent;
  }

  public byte[] getIv() {
    return iv;
  }

  public void setIv(byte[] iv) {
    this.iv = iv;
  }

  public int getTryCount() {
    return tryCount;
  }

  public void setTryCount(int tryCount) {
    this.tryCount = tryCount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /** Increments the try counter by one. */
  public void incrementTryCount() {
    this.tryCount++;
  }
}
