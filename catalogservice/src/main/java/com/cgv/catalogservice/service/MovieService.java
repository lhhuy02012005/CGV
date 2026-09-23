package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.movie.MovieCreateRequest;
import com.cgv.catalogservice.dto.request.movie.MovieFilterRequest;
import com.cgv.catalogservice.dto.request.movie.MovieUpdateRequest;
import com.cgv.catalogservice.dto.request.movie.MovieUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.MovieResponse;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MovieService {

    MovieResponse createMovie(MovieCreateRequest request);

    MovieResponse updateMovie(
            UUID movieId,
            MovieUpdateRequest request
    );

    MovieResponse updateMovieStatus(
            UUID movieId,
            MovieUpdateStatusRequest request
    );

    MovieResponse getMovieById(UUID movieId);

    PageResponse<MovieResponse> getAllMovies(
            MovieFilterRequest filter,
            Pageable pageable
    );

    List<MovieResponse> searchMovies(
            String keyword,
            Pageable pageable
    );
}
