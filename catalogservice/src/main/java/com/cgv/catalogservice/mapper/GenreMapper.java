package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.genre.GenreCreateRequest;
import com.cgv.catalogservice.dto.request.genre.GenreUpdateRequest;
import com.cgv.catalogservice.dto.response.GenreResponse;
import com.cgv.catalogservice.entity.Genre;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class)
public interface GenreMapper {

    Genre toEntity(GenreCreateRequest request);

    void updateEntity(GenreUpdateRequest request, @MappingTarget Genre genre);

    GenreResponse toResponse(Genre genre);

    List<GenreResponse> toResponseList(List<Genre> genres);
}
