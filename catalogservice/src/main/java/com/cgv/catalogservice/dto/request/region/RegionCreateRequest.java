package com.cgv.catalogservice.dto.request.region;

import jakarta.validation.constraints.NotBlank;

public record RegionCreateRequest(

        @NotBlank(message = "Tên khu vực không được để trống")
        String name,

        @NotBlank(message = "Slug không được để trống")
        String slug
) {
}