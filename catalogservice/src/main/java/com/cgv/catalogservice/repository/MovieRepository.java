package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.enums.ShowingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface MovieRepository
        extends JpaRepository<Movie, UUID>,
        JpaSpecificationExecutor<Movie> {

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(
            String title,
            UUID movieId
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            UPDATE Movie m
            SET m.showingStatus = :endedStatus
            WHERE m.endDate IS NOT NULL
              AND m.endDate < :today
              AND m.showingStatus <> :endedStatus
            """)
    int updateExpiredMoviesToEnded(
            @Param("today") LocalDate today,
            @Param("endedStatus") ShowingStatus endedStatus
    );
}