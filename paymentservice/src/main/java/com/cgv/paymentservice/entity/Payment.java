package com.cgv.paymentservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payments" , indexes = {
        @Index(name = "idx_cgv_payment_booking_id" , columnList = "booking_id"),
        @Index(name = "idx_cgv_payment_status", columnList = "status"),
        @Index(name = "idx_cgv_payment_transaction_id" , columnList = "transaction_id"),
        @Index(name = "idx_cgv_payment_provider", columnList = "provider")
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String provider;

    @Column(nullable = false)
    String transactionId;

    @Column
    String paymentMethod;

    @Builder.Default
    @Column
    String currency = "VND";

    @Column
    Instant paidAt;

    @Column
    BigDecimal refundAmount;

    @Column
    Instant refundedAt;

    @Column(name = "payload" , columnDefinition = "TEXT" , nullable = false)
    String raw;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<PaymentLog> paymentLogs = new ArrayList<>();
}
