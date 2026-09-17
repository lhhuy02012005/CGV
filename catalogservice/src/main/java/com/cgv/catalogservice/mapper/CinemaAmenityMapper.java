package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.cinemaamenity.CinemaAmenityCreateRequest;
import com.cgv.catalogservice.dto.request.cinemaamenity.CinemaAmenityUpdateRequest;
import com.cgv.catalogservice.dto.response.CinemaAmenityResponse;
import com.cgv.catalogservice.entity.Cinema;
import com.cgv.catalogservice.entity.CinemaAmenity;
import com.cgv.catalogservice.entity.CinemaAmenityId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(config = CatalogMapperConfig.class)
public interface CinemaAmenityMapper {

    default CinemaAmenity toEntity(
            CinemaAmenityCreateRequest request,
            Cinema cinema
    ) {
        if (request == null) {
            return null;
        }

        UUID cinemaId = cinema != null ? cinema.getId() : request.cinemaId();

        return CinemaAmenity.builder()
                .id(new CinemaAmenityId(cinemaId, request.amenity()))
                .cinema(cinema)
                .build();
    }

    default CinemaAmenity toEntity(
            CinemaAmenityUpdateRequest request,
            Cinema cinema
    ) {
        if (request == null) {
            return null;
        }

        UUID cinemaId = cinema != null ? cinema.getId() : request.cinemaId();

        return CinemaAmenity.builder()
                .id(new CinemaAmenityId(cinemaId, request.amenity()))
                .cinema(cinema)
                .build();
    }

    @Mapping(target = "cinemaId", source = "id.cinemaId")
    @Mapping(target = "cinemaName", source = "cinema.name")
    @Mapping(target = "amenity", source = "id.amenity")
    CinemaAmenityResponse toResponse(CinemaAmenity cinemaAmenity);

    List<CinemaAmenityResponse> toResponseList(List<CinemaAmenity> cinemaAmenities);
}
