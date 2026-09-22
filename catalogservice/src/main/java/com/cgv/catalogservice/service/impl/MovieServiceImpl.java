package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.movie.*;
import com.cgv.catalogservice.dto.response.MovieResponse;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.enums.MovieStatus;
import com.cgv.catalogservice.enums.ShowingStatus;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.MovieMapper;
import com.cgv.catalogservice.repository.MovieRepository;
import com.cgv.catalogservice.service.MovieService;
import com.cgv.catalogservice.specification.MovieSpecification;
import com.cgv.catalogservice.util.PageResponseUtils;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class MovieServiceImpl implements MovieService {

    MovieRepository movieRepository;
    MovieMapper movieMapper;

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "nowShowingMovies",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "comingSoonMovies",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public MovieResponse createMovie(MovieCreateRequest request) {

        validateMovieDates(request.releaseDate(), request.endDate());

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
    @Caching(
            put = {
                    @CachePut(
                            value = "movie",
                            key = "#movieId"
                    )
            },
            evict = {
                    @CacheEvict(
                            value = "nowShowingMovies",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "comingSoonMovies",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public MovieResponse updateMovie(UUID movieId, MovieUpdateRequest request) {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy movie với id: " + movieId
                        )
                );

        if (request.title() != null
                && movieRepository.existsByTitleIgnoreCaseAndIdNot(
                request.title().trim(),
                movieId
        )) {

            throw new ResourceConflictException(
                    "Phim với tên '" + request.title() + "' đã tồn tại"
            );
        }

        LocalDate releaseDate =
                request.releaseDate() != null
                        ? request.releaseDate()
                        : movie.getReleaseDate();

        LocalDate endDate =
                request.endDate() != null
                        ? request.endDate()
                        : movie.getEndDate();

        validateMovieDates(releaseDate, endDate);

        movieMapper.updateEntity(request, movie);

        movieRepository.saveAndFlush(movie);

        return movieMapper.toResponse(movie);
    }

    @Override
    @Caching(
            put = {
                    @CachePut(
                            value = "movie",
                            key = "#movieId"
                    )
            },
            evict = {
                    @CacheEvict(
                            value = "nowShowingMovies",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "comingSoonMovies",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public MovieResponse updateMovieStatus(UUID movieId, MovieUpdateStatusRequest request) {

        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy movie với id: " + movieId));

        movie.setStatus(request.status());

        movieRepository.saveAndFlush(movie);

        return movieMapper.toResponse(movie);
    }

    @Override
    @Caching(
            put = {
                    @CachePut(
                            value = "movie",
                            key = "#movieId"
                    )
            },
            evict = {
                    @CacheEvict(
                            value = "nowShowingMovies",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "comingSoonMovies",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public MovieResponse updateMovieShowingStatus(UUID movieId, MovieUpdateShowingStatusRequest request) {

        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phim với id: " + movieId));

        movie.setShowingStatus(request.showingStatus());

        movieRepository.saveAndFlush(movie);

        return movieMapper.toResponse(movie);
    }

    @Override
    @Cacheable(
            value = "movie",
            key = "#movieId"
    )
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(UUID movieId) {

        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy movie với id: " + movieId));

        return movieMapper.toResponse(movie);
    }

    @Override
    @Cacheable(
            value = "nowShowingMovies",
            key = "'page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"

    )
    @Transactional(readOnly = true)
    public PageResponse<MovieResponse> getNowShowingMovies(Pageable pageable) {

        Specification<Movie> spec =
                Specification.allOf(
                        MovieSpecification.hasShowingStatus(ShowingStatus.NOW_SHOWING),
                        MovieSpecification.hasStatus(MovieStatus.ACTIVE)
                );

        return PageResponseUtils.findAllAndMap(
                p -> movieRepository.findAll(spec, p),
                pageable,
                movieMapper::toResponseList
        );
    }

    @Override
    @Cacheable(
            value = "comingSoonMovies",
            key = "'page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"

    )
    @Transactional(readOnly = true)
    public PageResponse<MovieResponse> getComingSoonMovies(Pageable pageable) {

        Specification<Movie> spec =
                Specification.allOf(
                        MovieSpecification.hasShowingStatus(ShowingStatus.COMING_SOON),
                        MovieSpecification.hasStatus(MovieStatus.ACTIVE)
                );

        return PageResponseUtils.findAllAndMap(
                p -> movieRepository.findAll(spec, p),
                pageable,
                movieMapper::toResponseList
        );
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

        return PageResponseUtils.findAllAndMap(
                p -> movieRepository.findAll(specification, p),
                pageable,
                movieMapper::toResponseList
        );
    }

    private void validateMovieDates(
            LocalDate releaseDate,
            LocalDate endDate
    ) {
        if (releaseDate != null
                && endDate != null
                && endDate.isBefore(releaseDate)) {

            throw new IllegalArgumentException(
                    "Ngày kết thúc chiếu không được trước ngày phát hành"
            );
        }
    }
}
