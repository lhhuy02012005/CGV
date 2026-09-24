package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.document.MovieDocument;
import com.cgv.catalogservice.dto.request.movie.MovieCreateRequest;
import com.cgv.catalogservice.dto.request.movie.MovieFilterRequest;
import com.cgv.catalogservice.dto.request.movie.MovieUpdateRequest;
import com.cgv.catalogservice.dto.request.movie.MovieUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.MovieResponse;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.MovieMapper;
import com.cgv.catalogservice.repository.MovieRepository;
import com.cgv.catalogservice.repository.search.MovieSearchRepository;
import com.cgv.catalogservice.service.MovieService;
import com.cgv.catalogservice.specification.MovieSpecification;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
@Slf4j
public class MovieServiceImpl implements MovieService {

    MovieRepository movieRepository;
    MovieMapper movieMapper;
    MovieSearchRepository movieSearchRepository;

    @Override
    @Transactional
    @CacheEvict(value = "movies:detail", allEntries = true)
    public MovieResponse createMovie(MovieCreateRequest request) {

        if (movieRepository.existsByTitleIgnoreCase(request.title().trim())) {
            throw new ResourceConflictException(
                    "Phim với tên '" + request.title() + "' đã tồn tại"
            );
        }

        Movie movie = movieMapper.toEntity(request);

        Movie savedMovie = movieRepository.save(movie);
        MovieDocument doc = MovieDocument.builder()
                .id(savedMovie.getId().toString())
                .title(savedMovie.getTitle())
                .originalTitle(savedMovie.getOriginalTitle())
                .synopsis(savedMovie.getSynopsis())
                .director(savedMovie.getDirector())
                .ageRating(savedMovie.getAgeRating())
                .showingStatus(savedMovie.getShowingStatus().name())
                .build();
        movieSearchRepository.save(doc);

        return movieMapper.toResponse(savedMovie);
    }

    @Override
    @Transactional
    @CacheEvict(value = "movies:detail", key = "#movieId")
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
    @CacheEvict(value = "movies:detail", key = "#movieId")
    public MovieResponse updateMovieStatus(UUID movieId, MovieUpdateStatusRequest request) {

        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy movie với id: " + movieId));

        movie.setStatus(request.status());

        movieRepository.saveAndFlush(movie);

        return movieMapper.toResponse(movie);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "movies:detail", key = "#movieId")
    public MovieResponse getMovieById(UUID movieId) {

        Movie movie = movieRepository.findByIdWithCasts(movieId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy movie với id: " + movieId));

        return movieMapper.toDetailResponse(movie);
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
                        ),
                        MovieSpecification.isFeatured(
                                filter.isFeatured()
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

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> searchMovies(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Page<MovieDocument> searchResult = movieSearchRepository.searchFuzzy(keyword.trim(), pageable);
            List<UUID> movieIds = searchResult.getContent().stream()
                    .map(doc -> {
                        try {
                            return UUID.fromString(doc.getId());
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (!movieIds.isEmpty()) {
                List<Movie> movies = movieRepository.findAllById(movieIds);
                Map<UUID, Movie> movieMap = movies.stream().collect(Collectors.toMap(Movie::getId, m -> m));
                return movieIds.stream()
                        .map(movieMap::get)
                        .filter(Objects::nonNull)
                        .map(movieMapper::toResponse)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Elasticsearch search error, fallback to SQL LIKE query: {}", e.getMessage());
        }

        // Database fallback
        Page<Movie> fallbackPage = movieRepository.findAll(MovieSpecification.containsKeyword(keyword.trim()), pageable);
        return movieMapper.toResponseList(fallbackPage.getContent());
    }
}
