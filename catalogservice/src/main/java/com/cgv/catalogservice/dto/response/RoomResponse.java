package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.RoomStatus;

import java.util.UUID;

public record RoomResponse(
        UUID id,
        UUID cinemaId,
        String cinemaName,
        String name,
        Format format,
        Integer totalSeats,
        Integer rowCount,
        Integer columnCount,
        RoomStatus status
) {
}
