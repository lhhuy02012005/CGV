package com.cgv.marketingservice.dto.response;

import com.cgv.marketingservice.enums.DiscountType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionResponse {
    UUID id;
    String code;
    String name;
    String description;

    DiscountType discountType;
    BigDecimal discountValue;
    BigDecimal maxDiscountAmount;
    BigDecimal minOrderValue;

    String applicableTier;
    Instant validFrom;
    Instant validTo;

    Integer usageLimit;
    Integer maxUsesPerUser;
    boolean active;

    Instant createdAt;
    Instant updatedAt;
}
