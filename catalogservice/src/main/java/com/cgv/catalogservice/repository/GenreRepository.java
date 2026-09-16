package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository
        extends JpaRepository<Genre, Integer> {

    Optional<Genre> findBySlug(String slug);

    Optional<Genre> findByName(String name);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);
}
