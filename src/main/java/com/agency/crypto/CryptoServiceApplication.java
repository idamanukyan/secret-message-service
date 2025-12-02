package com.agency.crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Crypto Service. This service handles encryption and decryption of
 * secret messages that self-destruct after being read or after failed attempts.
 */
@SpringBootApplication
@EnableScheduling
public class CryptoServiceApplication {

  /**
   * Main entry point for the application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(CryptoServiceApplication.class, args);
  }
}
