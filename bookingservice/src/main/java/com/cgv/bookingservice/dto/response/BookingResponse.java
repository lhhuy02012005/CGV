package com.cgv.bookingservice.dto.response;

import com.cgv.bookingservice.enums.BookingStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {
    UUID bookingId;
    String userId;
    UUID showtimeId;
    BigDecimal totalBaseAmount;
    BigDecimal discountAmount;
    BigDecimal finalAmount;
    BookingStatus status;
    Instant paymentDeadline;
    List<UUID> seatIds;
    List<String> seatLabels;
    String qrCodeUrl;
    Instant createdAt;

    String movieTitle;
    String cinemaName;
    String cinemaAddress;
    String roomName;
    Instant showtimeStart;
    String posterUrl;
}

