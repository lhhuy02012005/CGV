package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Seat;
import com.cgv.catalogservice.enums.SeatTypeName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SeatRepository
        extends JpaRepository<Seat, UUID> {

    List<Seat> findByRoomId(UUID roomId);

    @Query("""
        SELECT s FROM Seat s
        JOIN FETCH s.seatType
        WHERE s.room.id = :roomId
    """)
    List<Seat> findByRoomIdWithSeatType(@org.springframework.data.repository.query.Param("roomId") UUID roomId);

    Page<Seat> findAllByRoomId(UUID roomId, Pageable pageable);

    boolean existsByRoomIdAndRowCharAndSeatNumber(
            UUID roomId,
            String rowChar,
            Integer seatNumber
    );

    int countByRoomId(UUID roomId);

    boolean existsBySeatTypeName(SeatTypeName seatTypeName);

    boolean existsByRoomIdAndRowCharAndSeatNumberAndIdNot(UUID targetRoomId, String targetRowChar, Integer targetSeatNumber, UUID seatId);
}
