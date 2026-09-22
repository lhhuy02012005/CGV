package com.cgv.catalogservice.dto.request.genre;

import jakarta.validation.constraints.NotBlank;

public record GenreCreateRequest(

        @NotBlank(message = "Tên thể loại không được để trống")
        String name
) {
}