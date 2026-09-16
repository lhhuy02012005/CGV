package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.RoleType;

import java.util.UUID;

public record MovieCastResponse(
        UUID id,
        UUID movieId,
        String movieTitle,
        String actorName,
        String characterName,
        RoleType roleType,
        String avatarUrl,
        Integer displayOrder
) {
}
