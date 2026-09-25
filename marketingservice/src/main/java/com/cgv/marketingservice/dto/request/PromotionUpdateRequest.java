package com.cgv.marketingservice.dto.request;

import com.cgv.marketingservice.enums.DiscountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionUpdateRequest {

    @Pattern(
            regexp = "[A-Za-z0-9_-]{1,50}",
            message = "Code must contain 1-50 letters, digits, underscores or hyphens"
    )
    String code;

    @Size(max = 200)
    String name;

    @Size(max = 5000)
    String description;

    DiscountType discountType;

    @Positive
    @Digits(integer = 10, fraction = 2)
    BigDecimal discountValue;

    @Positive
    @Digits(integer = 10, fraction = 2)
    BigDecimal maxDiscountAmount;

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    BigDecimal minOrderValue;

    String applicableTier;

    Instant validFrom;

    Instant validTo;

    @Positive
    Integer usageLimit;

    @Positive
    Integer maxUsesPerUser;

    Boolean isActive;
}
