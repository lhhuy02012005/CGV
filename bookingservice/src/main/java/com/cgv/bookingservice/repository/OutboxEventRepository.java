package com.cgv.bookingservice.repository;

import com.cgv.bookingservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/** Append-only Outbox; Debezium publishes committed inserts from PostgreSQL WAL. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
