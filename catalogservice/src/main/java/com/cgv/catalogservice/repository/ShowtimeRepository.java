package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ShowtimeRepository
        extends JpaRepository<Showtime, UUID>,
        JpaSpecificationExecutor<Showtime> {
}
