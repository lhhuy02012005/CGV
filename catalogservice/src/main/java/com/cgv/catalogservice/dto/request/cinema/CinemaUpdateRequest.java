package com.cgv.catalogservice.dto.request.cinema;

import jakarta.validation.constraints.Size;

public record CinemaUpdateRequest(

        Integer regionId,

        String name,

        String address,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phone,

        String openingHours
) {
}
