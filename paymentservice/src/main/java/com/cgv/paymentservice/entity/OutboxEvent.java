package com.cgv.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "outbox_payment_events",
        indexes = {
                @Index(name = "idx_cgv_outbox_created_at_is_published" , columnList = "is_published , created_at"),
                @Index(name = "idx_cgv_outbox_aggregate_id" , columnList = "aggregate_id")
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String aggregateId;

    @Column(nullable = false)
    String aggregateType;

    @Column(nullable = false)
    String eventType;

    @Column(name = "payload" , columnDefinition = "TEXT" , nullable = false)
    String payload;

    @Column(name = "is_published")
    Boolean isPublished;

    @Column
    @Builder.Default
    Integer retryCount = 0;
}
