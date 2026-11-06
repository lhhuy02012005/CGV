package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.enums.ShowingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface MovieRepository
        extends JpaRepository<Movie, UUID>,
        JpaSpecificationExecutor<Movie> {

    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.casts WHERE m.id = :movieId")
    Optional<Movie> findByIdWithCasts(@Param("movieId") UUID movieId);

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
            SET m.showingStatus = CASE
                WHEN m.endDate IS NOT NULL AND m.endDate < :today THEN :endedStatus
                WHEN m.releaseDate > :today THEN :comingStatus
                WHEN m.endDate IS NULL AND m.showingStatus = :endedStatus THEN :endedStatus
                ELSE :showingStatus END
            WHERE m.releaseDate IS NOT NULL
            """)
    int synchronizeShowingStatuses(
            @Param("today") LocalDate today,
            @Param("endedStatus") ShowingStatus endedStatus,
            @Param("comingStatus") ShowingStatus comingStatus,
            @Param("showingStatus") ShowingStatus showingStatus
    );
}
