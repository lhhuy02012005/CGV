package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Region;
import com.cgv.catalogservice.entity.Showtime;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowtimeRepository
        extends JpaRepository<Showtime, UUID>,
        JpaSpecificationExecutor<Showtime> {

    @Override
    @EntityGraph(attributePaths = {"movie", "room", "room.cinema", "room.cinema.region"})
    Optional<Showtime> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"movie", "room", "room.cinema", "room.cinema.region"})
    Page<Showtime> findAll(Specification<Showtime> spec, Pageable pageable);

    boolean existsByRoomId(UUID roomId);

    boolean existsByRoomIdAndStatusAndEndTimeAfter(
            UUID roomId,
            ShowtimeStatus status,
            Instant endTime
    );

    boolean existsByRoom_IdAndStatusAndEndTimeAfter(
            UUID roomId,
            ShowtimeStatus status,
            Instant endTime
    );

    @Query(value = """
        SELECT COUNT(b.id) 
        FROM bookings b 
        WHERE b.showtime_id = :showtimeId 
          AND b.status IN ('PAYMENT_PENDING', 'CONFIRMED', 'USED')
    """, nativeQuery = true)
    long countActiveBookingsByShowtimeId(@Param("showtimeId") UUID showtimeId);

    @Query("""
        SELECT s FROM Showtime s
        JOIN FETCH s.room r
        JOIN FETCH r.cinema c
        LEFT JOIN FETCH c.region reg
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
          AND r.status = com.cgv.catalogservice.enums.RoomStatus.ACTIVE
          AND c.status = com.cgv.catalogservice.enums.CinemaStatus.ACTIVE
          AND s.showDate >= :fromDate
          AND s.showDate <= :toDate
        ORDER BY s.startTime ASC
    """)
    List<Showtime> findActiveShowtimesByMovieAndDateRange(
            @Param("movieId") UUID movieId,
            @Param("status") ShowtimeStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
        SELECT s FROM Showtime s
        JOIN FETCH s.room r
        JOIN FETCH r.cinema c
        LEFT JOIN FETCH c.region reg
        JOIN FETCH s.movie m
        WHERE c.id = :cinemaId
          AND s.status = :status
          AND r.status = com.cgv.catalogservice.enums.RoomStatus.ACTIVE
          AND c.status = com.cgv.catalogservice.enums.CinemaStatus.ACTIVE
          AND s.showDate >= :fromDate
          AND s.showDate <= :toDate
        ORDER BY s.startTime ASC
    """)
    List<Showtime> findActiveShowtimesByCinemaAndDateRange(
            @Param("cinemaId") UUID cinemaId,
            @Param("status") ShowtimeStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
        SELECT DISTINCT reg FROM Showtime s
        JOIN s.room r
        JOIN r.cinema c
        JOIN c.region reg
        WHERE s.movie.id = :movieId
          AND s.status = :status
          AND r.status = com.cgv.catalogservice.enums.RoomStatus.ACTIVE
          AND c.status = com.cgv.catalogservice.enums.CinemaStatus.ACTIVE
        ORDER BY reg.name ASC
    """)
    List<Region> findDistinctRegionsByMovie(
            @Param("movieId") UUID movieId,
            @Param("status") ShowtimeStatus status
    );
}
