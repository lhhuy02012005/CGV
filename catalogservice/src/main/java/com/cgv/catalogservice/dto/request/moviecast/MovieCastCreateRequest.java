package com.cgv.catalogservice.dto.request.moviecast;

import com.cgv.catalogservice.enums.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record MovieCastCreateRequest(

        @NotNull(message = "Movie ID không được để trống")
        UUID movieId,

        @NotBlank(message = "Tên diễn viên không được để trống")
        String actorName,

        String characterName,

        RoleType roleType,

        String avatarUrl,

        @PositiveOrZero(message = "Thứ tự hiển thị không được âm")
        Integer displayOrder
) {
}