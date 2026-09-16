package com.cgv.catalogservice.dto.request.region;

public record RegionUpdateRequest(
        String name,
        String slug
) {
}
