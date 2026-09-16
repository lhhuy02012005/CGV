package com.cgv.catalogservice.dto.request.room;

import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record RoomCreateRequest(

        @NotNull(message = "Cinema ID không được để trống")
        UUID cinemaId,

        @NotBlank(message = "Tên phòng không được để trống")
        String name,

        Format format,

        @NotNull(message = "Số hàng không được để trống")
        @Positive(message = "Số hàng phải lớn hơn 0")
        Integer rowCount,

        @NotNull(message = "Số cột không được để trống")
        @Positive(message = "Số cột phải lớn hơn 0")
        Integer columnCount,

        RoomStatus status
) {
}