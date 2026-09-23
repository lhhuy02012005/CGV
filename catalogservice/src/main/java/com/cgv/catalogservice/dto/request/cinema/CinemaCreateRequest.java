package com.cgv.catalogservice.dto.request.cinema;

import com.cgv.catalogservice.enums.CinemaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CinemaCreateRequest(

        @NotNull(message = "Khu vực không được để trống")
        Integer regionId,

        @NotBlank(message = "Tên rạp không được để trống")
        String name,

        String address,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phone,

        String openingHours,

        Double latitude,

        Double longitude,

        CinemaStatus status
) {
}
