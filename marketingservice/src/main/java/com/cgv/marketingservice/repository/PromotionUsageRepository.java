package com.cgv.marketingservice.repository;

import com.cgv.marketingservice.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, UUID> {
    long countByPromotionIdAndUserId(UUID promotionId, String userId);
    long countByPromotionId(UUID promotionId);
    boolean existsByBookingId(UUID bookingId);
}
