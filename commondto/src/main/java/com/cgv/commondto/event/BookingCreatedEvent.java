package com.cgv.commondto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreatedEvent {
    UUID bookingId;
    String userId;
    UUID showtimeId;
    BigDecimal totalAmount;
    Instant paymentDeadline;
    List<UUID> seatIds;
    Instant createdAt;
}
