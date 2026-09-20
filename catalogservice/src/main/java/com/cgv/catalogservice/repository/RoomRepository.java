package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Room;
import com.cgv.catalogservice.enums.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomRepository
        extends JpaRepository<Room, UUID> {

    Page<Room> findAllByCinemaId(UUID cinemaId, Pageable pageable);

    List<Room> findByCinemaIdAndStatus(
            UUID cinemaId,
            RoomStatus status
    );

    boolean existsById(UUID id);
}
