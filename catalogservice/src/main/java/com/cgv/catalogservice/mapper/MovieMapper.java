package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.movie.MovieCreateRequest;
import com.cgv.catalogservice.dto.request.movie.MovieUpdateRequest;
import com.cgv.catalogservice.dto.response.MovieResponse;
import com.cgv.catalogservice.entity.Movie;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class, uses = {MovieCastMapper.class})
public interface MovieMapper {

    @Mapping(target = "status", defaultValue = "ACTIVE")
    @Mapping(target = "casts", ignore = true)
    Movie toEntity(MovieCreateRequest request);

    @Mapping(target = "casts", ignore = true)
    void updateEntity(MovieUpdateRequest request, @MappingTarget Movie movie);

    @Named("toDetailResponse")
    @Mapping(target = "casts", source = "casts")
    MovieResponse toDetailResponse(Movie movie);

    @Named("toSummaryResponse")
    @Mapping(target = "casts", ignore = true)
    MovieResponse toResponse(Movie movie);

    @IterableMapping(qualifiedByName = "toSummaryResponse")
    List<MovieResponse> toResponseList(List<Movie> movies);
}
