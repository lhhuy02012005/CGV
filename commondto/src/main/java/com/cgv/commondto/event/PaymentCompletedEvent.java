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
public class PaymentCompletedEvent {
    UUID paymentId;
    UUID bookingId;
    UUID promotionId;
    String userId;
    String transactionId;
    String paymentMethod; // "VNPAY", "MOMO", "ZALOPAY"
    BigDecimal amount;
    Instant paidAt;
}