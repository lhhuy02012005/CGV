package com.cgv.bookingservice.dto.request;

import com.cgv.bookingservice.enums.BookingStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingFilterRequest {
    BookingStatus status;
    Instant fromDate;
    Instant toDate;
}
