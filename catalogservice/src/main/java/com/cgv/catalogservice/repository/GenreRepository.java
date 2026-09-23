package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository
        extends JpaRepository<Genre, Integer> {

    Optional<Genre> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Integer genreId
    );

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(
            String slug,
            Integer genreId
    );
}
