package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.SeatTypeName;

import java.util.UUID;

public record SeatResponse(
        UUID id,
        UUID roomId,
        String roomName,
        String rowChar,
        Integer seatNumber,
        SeatTypeName seatTypeName,
        Boolean isActive
) {
}
