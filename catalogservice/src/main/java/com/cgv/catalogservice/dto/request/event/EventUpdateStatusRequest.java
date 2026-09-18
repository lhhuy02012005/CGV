package com.cgv.catalogservice.dto.request.event;

import com.cgv.catalogservice.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public record EventUpdateStatusRequest(
        @NotNull(message = "Trạng thái sự kiện không được để trống")
        EventStatus status
) {
}
