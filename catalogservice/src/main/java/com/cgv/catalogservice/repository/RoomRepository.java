package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Room;
import com.cgv.catalogservice.enums.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository
        extends JpaRepository<Room, UUID> {

    @Override
    @EntityGraph(attributePaths = {"cinema", "cinema.region"})
    Optional<Room> findById(UUID id);

    @EntityGraph(attributePaths = {"cinema", "cinema.region"})
    Page<Room> findAllByCinemaId(UUID cinemaId, Pageable pageable);

    @EntityGraph(attributePaths = {"cinema", "cinema.region"})
    List<Room> findByCinemaId(UUID cinemaId);

    @EntityGraph(attributePaths = {"cinema", "cinema.region"})
    List<Room> findByCinemaIdAndStatus(
            UUID cinemaId,
            RoomStatus status
    );

    boolean existsById(UUID roomId);

    boolean existsByCinemaIdAndNameIgnoreCase(
            UUID cinemaId,
            String name
    );

    boolean existsByCinemaIdAndNameIgnoreCaseAndIdNot(
            UUID cinemaId,
            String name,
            UUID roomId
    );
}
