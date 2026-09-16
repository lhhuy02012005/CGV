package com.cgv.catalogservice.dto.request.room;

import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.RoomStatus;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record RoomUpdateRequest(

        UUID cinemaId,

        String name,

        Format format,

        @Positive(message = "Số hàng phải lớn hơn 0")
        Integer rowCount,

        @Positive(message = "Số cột phải lớn hơn 0")
        Integer columnCount,

        RoomStatus status
) {
}