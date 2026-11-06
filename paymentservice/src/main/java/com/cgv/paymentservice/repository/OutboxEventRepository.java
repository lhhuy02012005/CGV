package com.cgv.paymentservice.repository;

import com.cgv.paymentservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/** Append-only Outbox; Debezium publishes committed inserts from PostgreSQL WAL. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
