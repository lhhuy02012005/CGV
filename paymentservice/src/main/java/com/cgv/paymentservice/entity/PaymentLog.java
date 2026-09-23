package com.cgv.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "payment_logs", indexes = {
        @Index(name = "idx_cgv_payment_log_payment_id", columnList = "payment_id"),
        @Index(name = "idx_cgv_payment_created_at", columnList = "created_at")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentLog extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    Payment payment;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "raw_request" , columnDefinition = "TEXT")
    String rawRequest;

    @Column(name = "raw_response" , columnDefinition = "TEXT")
    String rawResponse;

    @Column
    String ipAddress;







}
