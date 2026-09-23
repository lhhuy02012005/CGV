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

    List<Seat> findByRoom_Id(UUID roomId);

    @Query("""
        SELECT s FROM Seat s
        JOIN FETCH s.seatType
        WHERE s.room.id = :roomId
    """)
    List<Seat> findByRoomIdWithSeatType(@org.springframework.data.repository.query.Param("roomId") UUID roomId);

    Page<Seat> findAllByRoom_Id(UUID roomId, Pageable pageable);

    boolean existsByRoom_IdAndRowCharAndSeatNumber(
            UUID roomId,
            String rowChar,
            Integer seatNumber
    );

    int countByRoom_Id(UUID roomId);

    boolean existsBySeatType_Name(SeatTypeName seatTypeName);
}
