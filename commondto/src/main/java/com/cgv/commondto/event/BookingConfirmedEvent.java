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
public class BookingConfirmedEvent {
    UUID bookingId;
    UUID promotionId;
    BigDecimal discountAmount;
    String userId;
    String userEmail;
    String movieTitle;
    String posterUrl;
    String cinemaName;
    String cinemaAddress;
    String roomName;
    Instant showtimeStart;
    Instant usedAt;
    List<String> seatLabels; // VD: ["A1", "A2"]
    BigDecimal totalAmount;
    String qrCodeUrl;
    Instant confirmedAt;
}