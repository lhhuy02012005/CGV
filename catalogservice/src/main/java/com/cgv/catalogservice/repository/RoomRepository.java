package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Room;
import com.cgv.catalogservice.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomRepository
        extends JpaRepository<Room, UUID> {

    List<Room> findByCinema_Id(UUID cinemaId);

    List<Room> findByCinema_IdAndStatus(
            UUID cinemaId,
            RoomStatus status
    );
}
