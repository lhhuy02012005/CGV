package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Seat;
import com.cgv.catalogservice.enums.SeatTypeName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    @Override
    @EntityGraph(attributePaths = {"room", "seatType"})
    Optional<Seat> findById(UUID id);

    @EntityGraph(attributePaths = {"room", "seatType"})
    List<Seat> findByRoom_Id(UUID roomId);

    @EntityGraph(attributePaths = {"room", "seatType"})
    List<Seat> findByRoomId(UUID roomId);

    @Query("""
        SELECT s FROM Seat s
        JOIN FETCH s.seatType
        JOIN FETCH s.room
        WHERE s.room.id = :roomId
    """)
    List<Seat> findByRoomIdWithSeatType(@Param("roomId") UUID roomId);

    @EntityGraph(attributePaths = {"room", "seatType"})
    Page<Seat> findAllByRoom_Id(UUID roomId, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "seatType"})
    Page<Seat> findAllByRoomId(UUID roomId, Pageable pageable);

    boolean existsByRoom_IdAndRowCharAndSeatNumber(
            UUID roomId,
            String rowChar,
            Integer seatNumber
    );

    boolean existsByRoomIdAndRowCharAndSeatNumber(
            UUID roomId,
            String rowChar,
            Integer seatNumber
    );

    boolean existsByRoom_IdAndRowCharAndSeatNumberAndIdNot(
            UUID roomId,
            String rowChar,
            Integer seatNumber,
            UUID id
    );

    boolean existsByRoomIdAndRowCharAndSeatNumberAndIdNot(
            UUID roomId,
            String rowChar,
            Integer seatNumber,
            UUID id
    );

    int countByRoom_Id(UUID roomId);

    int countByRoomId(UUID roomId);

    boolean existsBySeatType_Name(SeatTypeName seatTypeName);

    boolean existsBySeatTypeName(SeatTypeName seatTypeName);
}
