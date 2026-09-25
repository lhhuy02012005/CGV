package com.cgv.catalogservice.dto.request.cinema;

import com.cgv.catalogservice.enums.CinemaStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CinemaUpdateRequest(

        Integer regionId,

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
