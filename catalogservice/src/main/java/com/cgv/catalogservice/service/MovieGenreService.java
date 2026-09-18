package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.moviegenre.MovieGenreCreateRequest;
import com.cgv.catalogservice.dto.response.MovieGenreResponse;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MovieGenreService {

    MovieGenreResponse createMovieGenre(MovieGenreCreateRequest request);

    void deleteMovieGenre(UUID movieId, Integer genreId);

    List<MovieGenreResponse> getGenresByMovieId(UUID movieId);

    PageResponse<MovieGenreResponse> getMoviesByGenreId(
            Integer genreId,
            Pageable pageable
    );
}
