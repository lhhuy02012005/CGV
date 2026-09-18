package com.cgv.catalogservice.dto.request.room;

import com.cgv.catalogservice.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;

public record RoomUpdateStatusRequest(

        @NotNull(message = "Trạng thái phòng chiếu không được để trống")
        RoomStatus status

) {
}
