package com.cgv.commondto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentFailedEvent {
    UUID paymentId;
    UUID bookingId;
    String userId;
    String transactionId;
    String failureReason;
    BigDecimal amount;
    Instant failedAt;
}
