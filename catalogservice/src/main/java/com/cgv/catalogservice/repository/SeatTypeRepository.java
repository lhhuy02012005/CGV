package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.SeatType;
import com.cgv.catalogservice.enums.SeatTypeName;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatTypeRepository
        extends JpaRepository<SeatType, SeatTypeName> {

    boolean existsByName(SeatTypeName name);
}
