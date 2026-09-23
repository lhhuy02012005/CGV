package com.cgv.paymentservice.entity;

import com.cgv.paymentservice.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
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
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String provider;

    @Column(name = "booking_id", nullable = false)
    UUID bookingId;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(nullable = false)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentStatus status;

    @Column(name = "transaction_id")
    String transactionId;

    @Column(name = "payment_method")
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

    @Builder.Default
    @Column(name = "payload" , columnDefinition = "TEXT")
    String raw = "";

    @OneToMany(mappedBy = "payment",cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<PaymentLog> paymentLogs = new ArrayList<>();
}
