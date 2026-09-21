package com.cgv.bookingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreateRequest {
    @NotBlank(message = "Vui lòng chọn suất chiếu !")
    UUID showtimeId;

    @NotEmpty(message = "Danh sách ghế không được để trống !")
    List<UUID> seatIds;

    UUID promotionId;
}
