package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.moviecast.MovieCastCreateRequest;
import com.cgv.catalogservice.dto.request.moviecast.MovieCastUpdateRequest;
import com.cgv.catalogservice.dto.response.MovieCastResponse;
import com.cgv.catalogservice.entity.MovieCast;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class)
public interface MovieCastMapper {

    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "displayOrder", defaultValue = "0")
    MovieCast toEntity(MovieCastCreateRequest request);

    @Mapping(target = "movie", ignore = true)
    void updateEntity(MovieCastUpdateRequest request, @MappingTarget MovieCast movieCast);

    @Mapping(target = "movieId", source = "movie.id")
    @Mapping(target = "movieTitle", source = "movie.title")
    MovieCastResponse toResponse(MovieCast movieCast);

    List<MovieCastResponse> toResponseList(List<MovieCast> movieCasts);
}
