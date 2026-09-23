package com.cgv.bookingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatLockRequest {
    @NotNull(message = "Vui lòng chọn suất chiếu !")
    UUID showtimeId;

    @NotEmpty(message = "Vui lòng chọn danh sách ghế !")
    List<UUID> seatIds;
}
