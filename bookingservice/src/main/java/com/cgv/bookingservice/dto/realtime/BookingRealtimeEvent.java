package com.cgv.bookingservice.dto.realtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRealtimeEvent implements Serializable {
    private String type;
    private UUID bookingId;
    private Object payload;
    private Instant timestamp;
}
