package com.agency.crypto.repository;

import com.agency.crypto.entity.SecretMessage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing SecretMessage entities in the database.
 */
@Repository
public interface SecretMessageRepository extends JpaRepository<SecretMessage, String> {

  /**
   * Finds all messages created before the given timestamp.
   *
   * @param timestamp the cutoff timestamp
   * @return list of messages older than the timestamp
   */
  List<SecretMessage> findByCreatedAtBefore(Instant timestamp);

  /**
   * Deletes all messages created before the given timestamp.
   *
   * @param timestamp the cutoff timestamp
   * @return the number of deleted messages
   */
  @Modifying
  @Query("DELETE FROM SecretMessage sm WHERE sm.createdAt < :timestamp")
  int deleteByCreatedAtBefore(@Param("timestamp") Instant timestamp);
}
