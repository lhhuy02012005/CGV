package com.cgv.paymentservice.dto;

import com.cgv.paymentservice.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentStatusResponse {
    UUID paymentId;
    UUID bookingId;
    String provider;
    BigDecimal amount;
    PaymentStatus status;
    String transactionId;
    Instant paidAt;
}
