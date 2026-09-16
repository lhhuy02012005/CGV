package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.MovieGenre;
import com.cgv.catalogservice.entity.MovieGenreId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovieGenreRepository
        extends JpaRepository<MovieGenre, MovieGenreId> {

    List<MovieGenre> findByIdMovieId(UUID movieId);

    List<MovieGenre> findByIdGenreId(Integer genreId);

    boolean existsByIdMovieIdAndIdGenreId(
            UUID movieId,
            Integer genreId
    );

    void deleteByIdMovieId(UUID movieId);
}
