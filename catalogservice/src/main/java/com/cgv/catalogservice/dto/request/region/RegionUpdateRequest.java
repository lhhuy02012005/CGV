package com.cgv.catalogservice.dto.request.region;

import jakarta.validation.constraints.NotBlank;

public record RegionUpdateRequest(
        @NotBlank(message = "Tên khu vực không được để trống")
        String name
) {
}
