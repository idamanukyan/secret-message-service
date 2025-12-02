package com.agency.crypto.service;

import com.agency.crypto.repository.SecretMessageRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service that automatically deletes old messages. By default, messages older than 2 days
 * are deleted.
 */
@Service
public class MessageCleanupScheduler {

  private static final Logger logger = LoggerFactory.getLogger(MessageCleanupScheduler.class);

  private final SecretMessageRepository repository;
  private final long maxAgeDays;

  /**
   * Creates a new MessageCleanupScheduler.
   *
   * @param repository the message repository
   * @param maxAgeDays the maximum age of messages in days before deletion
   */
  public MessageCleanupScheduler(
      SecretMessageRepository repository,
      @Value("${crypto.cleanup.max-age-days:2}") long maxAgeDays) {
    this.repository = repository;
    this.maxAgeDays = maxAgeDays;
    logger.info("MessageCleanupScheduler initialized with maxAgeDays: {}", maxAgeDays);
  }

  /**
   * Scheduled task that runs periodically to clean up old messages. Runs every hour by default.
   */
  @Scheduled(fixedRateString = "${crypto.cleanup.interval-ms:3600000}")
  @Transactional
  public void cleanupOldMessages() {
    logger.info("Starting scheduled cleanup of old messages");

    Instant cutoff = Instant.now().minus(Duration.ofDays(maxAgeDays));
    int deletedCount = repository.deleteByCreatedAtBefore(cutoff);

    if (deletedCount > 0) {
      logger.info("Deleted {} messages older than {} days", deletedCount, maxAgeDays);
    } else {
      logger.debug("No old messages to delete");
    }
  }

  /**
   * Manual trigger for cleanup, useful for testing.
   *
   * @return the number of deleted messages
   */
  @Transactional
  public int triggerCleanup() {
    logger.info("Manually triggered cleanup of old messages");

    Instant cutoff = Instant.now().minus(Duration.ofDays(maxAgeDays));
    int deletedCount = repository.deleteByCreatedAtBefore(cutoff);

    logger.info("Manual cleanup deleted {} messages", deletedCount);
    return deletedCount;
  }
}
