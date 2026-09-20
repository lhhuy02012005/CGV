package com.cgv.bookingservice.repository;

import com.cgv.bookingservice.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, UUID> {
}
