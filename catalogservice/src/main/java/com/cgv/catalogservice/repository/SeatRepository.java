package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SeatRepository
        extends JpaRepository<Seat, UUID> {

    List<Seat> findByRoom_Id(UUID roomId);

    boolean existsByRoom_IdAndRowCharAndSeatNumber(
            UUID roomId,
            String rowChar,
            Integer seatNumber
    );

    int countByRoom_Id(UUID roomId);
}
