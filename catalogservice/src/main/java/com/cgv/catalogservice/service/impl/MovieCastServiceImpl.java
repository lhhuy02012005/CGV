package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.moviecast.MovieCastCreateRequest;
import com.cgv.catalogservice.dto.request.moviecast.MovieCastUpdateRequest;
import com.cgv.catalogservice.dto.response.MovieCastResponse;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.entity.MovieCast;
import com.cgv.catalogservice.mapper.MovieCastMapper;
import com.cgv.catalogservice.repository.MovieCastRepository;
import com.cgv.catalogservice.repository.MovieRepository;
import com.cgv.catalogservice.service.MovieCastService;
import com.cgv.catalogservice.util.PageResponseUtils;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j(topic = "MOVIE-CAST-SERVICE")
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovieCastServiceImpl implements MovieCastService {

    MovieCastRepository movieCastRepository;
    MovieRepository movieRepository;
    MovieCastMapper movieCastMapper;

    @Override
    @Transactional
    public MovieCastResponse createMovieCast(MovieCastCreateRequest request) {

        log.info("Creating movie cast");
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy movie với id: " + request.movieId()
                ));

        MovieCast movieCast = movieCastMapper.toEntity(request);
        movieCast.setMovie(movie);

        MovieCast savedMovieCast = movieCastRepository.save(movieCast);

        return movieCastMapper.toResponse(savedMovieCast);
    }

    @Override
    @Transactional
    public MovieCastResponse updateMovieCast(
            UUID movieCastId,
            MovieCastUpdateRequest request
    ) {

        log.info("Updating movie cast: movieCastId={}", movieCastId);
        MovieCast movieCast = movieCastRepository.findById(movieCastId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy movie cast với id: " + movieCastId
                ));

        movieCastMapper.updateEntity(request, movieCast);

        if (request.movieId() != null) {
            Movie movie = movieRepository.findById(request.movieId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy movie với id: " + request.movieId()
                    ));
            movieCast.setMovie(movie);
        }

        return movieCastMapper.toResponse(movieCast);
    }

    @Override
    @Transactional
    public void deleteMovieCast(UUID movieCastId) {

        log.info("Deleting movie cast: movieCastId={}", movieCastId);
        MovieCast movieCast = movieCastRepository.findById(movieCastId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy movie cast với id: " + movieCastId
                ));

        movieCastRepository.delete(movieCast);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieCastResponse getMovieCastById(UUID movieCastId) {

        log.debug("Getting movie cast by id: movieCastId={}", movieCastId);
        MovieCast movieCast = movieCastRepository.findById(movieCastId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy movie cast với id: " + movieCastId
                ));

        return movieCastMapper.toResponse(movieCast);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovieCastResponse> getAllMovieCasts(Pageable pageable) {

        log.debug("Getting all movie casts: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        return PageResponseUtils.findAllAndMap(
                movieCastRepository::findAll,
                pageable,
                movieCastMapper::toResponseList
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieCastResponse> getMovieCastsByMovieId(UUID movieId) {
        List<MovieCast> movieCasts = movieCastRepository.findByMovieIdOrderByDisplayOrderAsc(movieId);
        return movieCastMapper.toResponseList(movieCasts);
    }
}
