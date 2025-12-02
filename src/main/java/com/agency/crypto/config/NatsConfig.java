package com.agency.crypto.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.NKey;
import io.nats.client.Options;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for NATS connection. Supports optional TLS and NKey authentication.
 */
@Configuration
public class NatsConfig {

  private static final Logger logger = LoggerFactory.getLogger(NatsConfig.class);

  @Value("${nats.server.url:nats://localhost:4222}")
  private String natsUrl;

  @Value("${nats.connection.name:crypto-service}")
  private String connectionName;

  @Value("${nats.connection.timeout:5}")
  private int connectionTimeoutSeconds;

  @Value("${nats.reconnect.max-attempts:10}")
  private int maxReconnectAttempts;

  @Value("${nats.reconnect.wait-ms:2000}")
  private long reconnectWaitMs;

  @Value("${nats.tls.enabled:false}")
  private boolean tlsEnabled;

  @Value("${nats.nkey.seed:}")
  private String nkeySeed;

  /**
   * Creates and configures the NATS connection.
   *
   * @return the configured NATS connection
   * @throws IOException if connection fails
   * @throws InterruptedException if connection is interrupted
   * @throws GeneralSecurityException if NKey processing fails
   */
  @Bean(destroyMethod = "close")
  public Connection natsConnection() throws IOException, InterruptedException, GeneralSecurityException {
    logger.info("Connecting to NATS server at: {}", natsUrl);

    Options.Builder builder = new Options.Builder()
        .server(natsUrl)
        .connectionName(connectionName)
        .connectionTimeout(Duration.ofSeconds(connectionTimeoutSeconds))
        .maxReconnects(maxReconnectAttempts)
        .reconnectWait(Duration.ofMillis(reconnectWaitMs))
        .connectionListener((conn, type) -> {
          logger.info("NATS connection event: {}", type);
        })
        .errorListener(new NatsErrorListener());

    if (tlsEnabled) {
      logger.info("TLS enabled for NATS connection");
      SSLContext sslContext = SSLContext.getDefault();
      builder.sslContext(sslContext);
    }

    if (nkeySeed != null && !nkeySeed.isEmpty()) {
      logger.info("NKey authentication enabled for NATS connection");
      NKey nkey = NKey.fromSeed(nkeySeed.toCharArray());
      builder.authHandler(new NKeyAuthHandler(nkey));
    }

    Options options = builder.build();
    Connection connection = Nats.connect(options);

    logger.info("Successfully connected to NATS server. Status: {}", connection.getStatus());
    return connection;
  }

  /**
   * NKey authentication handler for NATS.
   */
  private static class NKeyAuthHandler implements io.nats.client.AuthHandler {

    private final NKey nkey;

    NKeyAuthHandler(NKey nkey) {
      this.nkey = nkey;
    }

    @Override
    public char[] getID() {
      try {
        return nkey.getPublicKey();
      } catch (Exception e) {
        throw new RuntimeException("Failed to get NKey public key", e);
      }
    }

    @Override
    public byte[] sign(byte[] nonce) {
      try {
        return nkey.sign(nonce);
      } catch (Exception e) {
        throw new RuntimeException("Failed to sign with NKey", e);
      }
    }

    @Override
    public char[] getJWT() {
      return null;
    }
  }

  /**
   * Custom error listener for NATS connection errors.
   */
  private static class NatsErrorListener implements io.nats.client.ErrorListener {

    private static final Logger errorLogger = LoggerFactory.getLogger(NatsErrorListener.class);

    @Override
    public void errorOccurred(Connection conn, String error) {
      errorLogger.error("NATS error occurred: {}", error);
    }

    @Override
    public void exceptionOccurred(Connection conn, Exception exp) {
      errorLogger.error("NATS exception occurred", exp);
    }

    @Override
    public void slowConsumerDetected(Connection conn, io.nats.client.Consumer consumer) {
      errorLogger.warn("NATS slow consumer detected");
    }
  }
}
