package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Seat;
import com.cgv.catalogservice.enums.SeatTypeName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SeatRepository
        extends JpaRepository<Seat, UUID> {

    List<Seat> findByRoomId(UUID roomId);

    Page<Seat> findAllByRoomId(UUID roomId, Pageable pageable);

    boolean existsByRoomIdAndRowCharAndSeatNumber(
            UUID roomId,
            String rowChar,
            Integer seatNumber
    );

    boolean existsByRoomIdAndRowCharAndSeatNumberAndIdNot(
            UUID roomId,
            String rowChar,
            Integer seatNumber,
            UUID seatId
    );

    int countByRoomId(UUID roomId);

    boolean existsBySeatTypeName(SeatTypeName seatTypeName);
}
