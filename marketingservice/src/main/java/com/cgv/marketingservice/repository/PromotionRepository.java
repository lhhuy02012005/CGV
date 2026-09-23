package com.cgv.marketingservice.repository;

import com.cgv.commondto.enums.MembershipTier;
import com.cgv.marketingservice.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {
    Optional<Promotion> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
        SELECT p FROM Promotion p 
        WHERE p.isActive = true 
          AND p.validFrom <= :now 
          AND p.validTo >= :now
        ORDER BY p.createdAt DESC
    """)
    List<Promotion> findActivePromotions(@Param("now") Instant now);

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.isActive = true
          AND p.validFrom <= :now
          AND p.validTo >= :now
          AND (p.applicableTier IS NULL OR p.applicableTier IN :eligibleTiers)
          AND (:totalAmount IS NULL OR p.minOrderValue IS NULL OR p.minOrderValue <= :totalAmount)
        ORDER BY p.discountValue DESC, p.createdAt DESC
    """)
    List<Promotion> findAvailablePromotions(
            @Param("now") Instant now,
            @Param("eligibleTiers") Collection<MembershipTier> eligibleTiers,
            @Param("totalAmount") BigDecimal totalAmount
    );
}
