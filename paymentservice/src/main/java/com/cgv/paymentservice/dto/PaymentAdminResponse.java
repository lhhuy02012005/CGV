package com.cgv.paymentservice.dto;

import com.cgv.paymentservice.enums.PaymentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentAdminResponse {
    UUID id;
    UUID bookingId;
    String userId;
    String provider;
    String paymentMethod;
    BigDecimal amount;
    String currency;
    PaymentStatus status;
    String transactionId;
    String gatewayTransactionNo;
    Instant createdAt;
    Instant paidAt;
    BigDecimal refundAmount;
    Instant refundedAt;
    /** Chỉ có ở API chi tiết. */
    List<LogEntry> logs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LogEntry {
        UUID id;
        String description;
        String rawResponse;
        String ipAddress;
        Instant createdAt;
    }
}
