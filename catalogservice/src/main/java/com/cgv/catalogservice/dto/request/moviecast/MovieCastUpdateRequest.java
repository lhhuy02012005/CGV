package com.cgv.catalogservice.dto.request.moviecast;

import com.cgv.catalogservice.enums.RoleType;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record MovieCastUpdateRequest(

        UUID movieId,

        String actorName,

        String characterName,

        RoleType roleType,

        String avatarUrl,

        @PositiveOrZero(message = "Thứ tự hiển thị không được âm")
        Integer displayOrder
) {
}