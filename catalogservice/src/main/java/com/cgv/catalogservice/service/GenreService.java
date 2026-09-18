package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.genre.GenreCreateRequest;
import com.cgv.catalogservice.dto.request.genre.GenreUpdateRequest;
import com.cgv.catalogservice.dto.response.GenreResponse;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface GenreService {

    GenreResponse createGenre(GenreCreateRequest request);

    GenreResponse updateGenre(Integer genreId, GenreUpdateRequest request);

    void deleteGenre(Integer genreId);

    GenreResponse getGenreById(Integer genreId);

    PageResponse<GenreResponse> getAllGenres(Pageable pageable);
}
