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

    Page<Room> findAllByCinema_Id(UUID cinemaId, Pageable pageable);

    List<Room> findByCinema_IdAndStatus(
            UUID cinemaId,
            RoomStatus status
    );
}
