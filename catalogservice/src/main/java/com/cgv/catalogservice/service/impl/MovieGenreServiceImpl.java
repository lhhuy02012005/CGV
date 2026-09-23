package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.moviegenre.MovieGenreCreateRequest;
import com.cgv.catalogservice.dto.response.MovieGenreResponse;
import com.cgv.catalogservice.entity.Genre;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.entity.MovieGenre;
import com.cgv.catalogservice.entity.MovieGenreId;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.MovieGenreMapper;
import com.cgv.catalogservice.repository.GenreRepository;
import com.cgv.catalogservice.repository.MovieGenreRepository;
import com.cgv.catalogservice.repository.MovieRepository;
import com.cgv.catalogservice.service.MovieGenreService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j(topic = "MOVIE-GENRE-SERVICE")
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovieGenreServiceImpl implements MovieGenreService {

    MovieGenreRepository movieGenreRepository;
    MovieRepository movieRepository;
    GenreRepository genreRepository;

    MovieGenreMapper movieGenreMapper;

    @Override
    @Transactional
    public MovieGenreResponse createMovieGenre(
            MovieGenreCreateRequest request
    ) {

        log.info("Creating movie genre: movieId={}, genreId={}", request.movieId(), request.genreId());

        if (movieGenreRepository.existsByIdMovieIdAndIdGenreId(
                request.movieId(),
                request.genreId()
        )) {
            throw new ResourceConflictException(
                    "Phim đã được gán thể loại này"
            );
        }

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy phim với id: "
                                        + request.movieId()
                        )
                );

        Genre genre = genreRepository.findById(request.genreId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy thể loại với id: "
                                        + request.genreId()
                        )
                );

        MovieGenre movieGenre =
                movieGenreMapper.toEntity(request, movie, genre);

        MovieGenre savedMovieGenre =
                movieGenreRepository.save(movieGenre);

        return movieGenreMapper.toResponse(savedMovieGenre);
    }

    @Override
    @Transactional
    public void deleteMovieGenre(
            UUID movieId,
            Integer genreId
    ) {

        log.info("Deleting movie genre: movieId={}, genreId={}", movieId, genreId);

        MovieGenreId movieGenreId =
                new MovieGenreId(movieId, genreId);

        if (!movieGenreRepository.existsById(movieGenreId)) {
            throw new EntityNotFoundException(
                    "Không tìm thấy quan hệ giữa phim "
                            + movieId
                            + " và thể loại "
                            + genreId
            );
        }

        movieGenreRepository.deleteById(movieGenreId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieGenreResponse> getGenresByMovieId(
            UUID movieId
    ) {

        log.debug("Getting genres by movie: movieId={}", movieId);
        if (!movieRepository.existsById(movieId)) {
            throw new EntityNotFoundException(
                    "Không tìm thấy phim với id: " + movieId
            );
        }

        List<MovieGenre> movieGenreList =
                movieGenreRepository.findByIdMovieId(movieId);

        return movieGenreMapper.toResponseList(movieGenreList);
    }
}
