package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.MovieCast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovieCastRepository
        extends JpaRepository<MovieCast, UUID> {

    List<MovieCast> findByMovie_IdOrderByDisplayOrderAsc(
            UUID movieId
    );

    void deleteByMovie_Id(UUID movieId);
}
