package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Showtime;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ShowtimeRepository
        extends JpaRepository<Showtime, UUID>,
        JpaSpecificationExecutor<Showtime> {

    boolean existsByRoom_IdAndStatusAndEndTimeAfter(
            UUID roomId,
            ShowtimeStatus status,
            LocalDateTime endTime
    );
}
