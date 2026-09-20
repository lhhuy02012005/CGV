package com.cgv.catalogservice.dto.request.room;

import com.cgv.catalogservice.enums.Format;
import jakarta.validation.constraints.Positive;

public record RoomUpdateRequest(

        String name,

        Format format,

        @Positive(message = "Số hàng phải lớn hơn 0")
        Integer rowCount,

        @Positive(message = "Số cột phải lớn hơn 0")
        Integer columnCount
) {
}