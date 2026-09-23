package com.cgv.paymentservice.repository;

import com.cgv.paymentservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent , UUID> {
}
