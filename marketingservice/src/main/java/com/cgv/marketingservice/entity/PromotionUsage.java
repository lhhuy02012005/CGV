package com.cgv.marketingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "promotion_usages",
        indexes = {
                @Index(name = "idx_promo_usage_user", columnList = "promotion_id, user_id"),
                @Index(name = "idx_promo_usage_booking", columnList = "booking_id", unique = true)
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "promotion_id", nullable = false)
    UUID promotionId;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(name = "booking_id", nullable = false, unique = true)
    UUID bookingId;

    @Column(name = "discount_applied", nullable = false, precision = 12, scale = 2)
    BigDecimal discountApplied;

    @CreationTimestamp
    @Column(name = "used_at", nullable = false, updatable = false)
    Instant usedAt;
}