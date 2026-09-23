package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Region;
import com.cgv.catalogservice.entity.Showtime;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowtimeRepository
        extends JpaRepository<Showtime, UUID>,
        JpaSpecificationExecutor<Showtime> {

    boolean existsByRoomIdAndStatusAndEndTimeAfter(
            UUID roomId,
            ShowtimeStatus status,
            Instant endTime
    );

    @Query("""
        SELECT s FROM Showtime s
        JOIN FETCH s.room r
        JOIN FETCH r.cinema c
        JOIN FETCH s.movie m
        WHERE s.id = :id
    """)
    Optional<Showtime> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT s FROM Showtime s
        JOIN FETCH s.room r
        JOIN FETCH r.cinema c
        LEFT JOIN FETCH c.region reg
        JOIN FETCH s.movie m
        WHERE s.movie.id = :movieId
          AND s.status = :status
          AND s.showDate >= :fromTime
          AND s.showDate <= :toTime
        ORDER BY s.startTime ASC
    """)
    List<Showtime> findActiveShowtimesByMovieAndDateRange(
            @Param("movieId") UUID movieId,
            @Param("status") ShowtimeStatus status,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime
    );

    @Query("""
        SELECT s FROM Showtime s
        JOIN FETCH s.room r
        JOIN FETCH r.cinema c
        LEFT JOIN FETCH c.region reg
        JOIN FETCH s.movie m
        WHERE c.id = :cinemaId
          AND s.status = :status
          AND s.showDate >= :fromTime
          AND s.showDate <= :toTime
        ORDER BY s.startTime ASC
    """)
    List<Showtime> findActiveShowtimesByCinemaAndDateRange(
            @Param("cinemaId") UUID cinemaId,
            @Param("status") ShowtimeStatus status,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime
    );

    @Query("""
        SELECT DISTINCT reg FROM Showtime s
        JOIN s.room r
        JOIN r.cinema c
        JOIN c.region reg
        WHERE s.movie.id = :movieId
          AND s.status = :status
        ORDER BY reg.name ASC
    """)
    List<Region> findDistinctRegionsByMovie(
            @Param("movieId") UUID movieId,
            @Param("status") ShowtimeStatus status
    );
}
