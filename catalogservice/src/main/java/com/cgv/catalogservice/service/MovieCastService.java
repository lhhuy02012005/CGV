package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.moviecast.MovieCastCreateRequest;
import com.cgv.catalogservice.dto.request.moviecast.MovieCastUpdateRequest;
import com.cgv.catalogservice.dto.response.MovieCastResponse;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MovieCastService {

    MovieCastResponse createMovieCast(MovieCastCreateRequest request);

    MovieCastResponse updateMovieCast(UUID movieCastId, MovieCastUpdateRequest request);

    void deleteMovieCast(UUID movieCastId);

    MovieCastResponse getMovieCastById(UUID movieCastId);

    PageResponse<MovieCastResponse> getAllMovieCasts(Pageable pageable);

    List<MovieCastResponse> getMovieCastsByMovieId(UUID movieId);
}
