package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.movie.MovieCreateRequest;
import com.cgv.catalogservice.dto.request.movie.MovieFilterRequest;
import com.cgv.catalogservice.dto.request.movie.MovieUpdateRequest;
import com.cgv.catalogservice.dto.request.movie.MovieUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.MovieResponse;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.MovieMapper;
import com.cgv.catalogservice.repository.MovieRepository;
import com.cgv.catalogservice.service.MovieService;
import com.cgv.catalogservice.specification.MovieSpecification;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class MovieServiceImpl implements MovieService {

    MovieRepository movieRepository;
    MovieMapper movieMapper;

    @Override
    @Transactional
    public MovieResponse createMovie(MovieCreateRequest request) {

        if (movieRepository.existsByTitleIgnoreCase(request.title().trim())) {
            throw new ResourceConflictException(
                    "Phim với tên '" + request.title() + "' đã tồn tại"
            );
        }

        Movie movie = movieMapper.toEntity(request);

        Movie savedMovie = movieRepository.save(movie);

        return movieMapper.toResponse(savedMovie);
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(UUID movieId, MovieUpdateRequest request) {

        if (movieRepository.existsByTitleIgnoreCaseAndIdNot(request.title().trim(), movieId)) {
            throw new ResourceConflictException(
                    "Phim với tên '" + request.title() + "' đã tồn tại"
            );
        }

        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy movie với id: " + movieId));

        movieMapper.updateEntity(request, movie);

        movieRepository.saveAndFlush(movie);

        return movieMapper.toResponse(movie);
    }

    @Override
    @Transactional
    public MovieResponse updateMovieStatus(UUID movieId, MovieUpdateStatusRequest request) {

        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy movie với id: " + movieId));

        movie.setStatus(request.status());

        movieRepository.saveAndFlush(movie);

        return movieMapper.toResponse(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(UUID movieId) {

        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy movie với id: " + movieId));

        return movieMapper.toResponse(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovieResponse> getAllMovies(MovieFilterRequest filter, Pageable pageable) {

        Specification<Movie> specification =
                Specification.allOf(
                        MovieSpecification.containsKeyword(
                                filter.keyword()
                        ),
                        MovieSpecification.hasGenreId(
                                filter.genreId()
                        ),
                        MovieSpecification.hasShowingStatus(
                                filter.showingStatus()
                        ),
                        MovieSpecification.hasStatus(
                                filter.status()
                        ),
                        MovieSpecification.releaseDateFrom(
                                filter.releaseFrom()
                        ),
                        MovieSpecification.releaseDateTo(
                                filter.releaseTo()
                        ),
                        MovieSpecification.durationGreaterThanOrEqual(
                                filter.minDuration()
                        ),
                        MovieSpecification.durationLessThanOrEqual(
                                filter.maxDuration()
                        ),
                        MovieSpecification.hasLanguage(
                                filter.language()
                        ),
                        MovieSpecification.hasAgeRating(
                                filter.ageRating()
                        )
                );

        Page<Movie> moviePage = movieRepository.findAll(specification, pageable);

        List<MovieResponse> movieResponseList = movieMapper.toResponseList(moviePage.getContent());

        return PageResponse.<MovieResponse>builder()
                .data(movieResponseList)
                .pageNumber(moviePage.getNumber() + 1)
                .pageSize(moviePage.getSize())
                .totalPages(moviePage.getTotalPages())
                .totalElements(moviePage.getTotalElements())
                .build();
    }
}
