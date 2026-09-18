package com.cgv.marketingservice.entity;

import com.cgv.commondto.entity.BaseEntity;
import com.cgv.marketingservice.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Check;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promotions")
@Check(constraints = """
        discount_value > 0
        AND (discount_type <> 'PERCENT' OR discount_value <= 100)
        AND (max_discount_amount IS NULL OR max_discount_amount > 0)
        AND min_order_value >= 0
        AND valid_from < valid_to
        AND (usage_limit IS NULL OR usage_limit > 0)
        AND max_uses_per_user > 0
        """)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

public class Promotion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, unique = true, length = 50)
    String code;

    @Column(nullable = false, length = 200)
    String name;

    @Column(columnDefinition = "text")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    DiscountType discountType;

    @Column(
            name = "discount_value",
            nullable = false,
            precision = 12,
            scale = 2
    )
    BigDecimal discountValue;

    @Column(
            name = "max_discount_amount",
            precision = 12,
            scale = 2
    )
    BigDecimal maxDiscountAmount;

    @Builder.Default
    @Column(
            name = "min_order_value",
            nullable = false,
            precision = 12,
            scale = 2
    )
    BigDecimal minOrderValue =  BigDecimal.ZERO;

    @Column(name = "applicable_tier", length = 20)
    String applicableTier;

    @Column(name = "valid_from", nullable = false)
    Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    Instant validTo;

    @Column(name = "usage_limit")
    Integer usageLimit;

    @Builder.Default
    @Column(name = "max_uses_per_user", nullable = false)
    Integer maxUsesPerUser = 1;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    boolean isActive = true;

    @Version
    @Column(nullable = false)
    long version;
}
