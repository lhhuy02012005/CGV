package com.cgv.paymentservice.repository;

import com.cgv.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDesc(UUID bookingId);
}
