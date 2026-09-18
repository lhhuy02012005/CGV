package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.cinema.CinemaCreateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateRequest;
import com.cgv.catalogservice.dto.response.CinemaResponse;
import com.cgv.catalogservice.entity.Cinema;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class)
public interface CinemaMapper {

    @Mapping(target = "region", ignore = true)
    @Mapping(target = "status", defaultValue = "ACTIVE")
    Cinema toEntity(CinemaCreateRequest request);

    @Mapping(target = "region", ignore = true)
    void updateEntity(CinemaUpdateRequest request, @MappingTarget Cinema cinema);

    @Mapping(target = "regionId", source = "region.id")
    @Mapping(target = "regionName", source = "region.name")
    CinemaResponse toResponse(Cinema cinema);

    List<CinemaResponse> toResponseList(List<Cinema> cinemas);
}
