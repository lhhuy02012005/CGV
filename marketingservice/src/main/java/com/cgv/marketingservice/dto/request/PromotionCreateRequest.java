package com.cgv.marketingservice.dto.request;

import com.cgv.marketingservice.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;


@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionCreateRequest {
    @NotBlank
    @Pattern(
            regexp = "[A-Za-z0-9_-]{1,50}",
            message = "Code must contain 1-50 letters, digits, underscores or hyphens"
    )
    String code;

    @NotBlank
    @Size(max = 200)
    String name;

    @Size(max = 5000)
    String description;

    @NotNull
    DiscountType discountType;

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    BigDecimal discountValue;

    @Positive
    @Digits(integer = 10, fraction = 2)
    BigDecimal maxDiscountAmount;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    BigDecimal minOrderValue = BigDecimal.ZERO;

    @Pattern(
            regexp = "MEMBER|VIP|VVIP",
            message = "Tier must be member, VIP OR VVIP"
    )
    String applicableTier;

    @NotNull
    Instant validFrom;

    @NotNull
    Instant validTo;

    @Positive
    Integer usageLimit;

    @NotNull
    @Positive
    Integer maxUsesPerUser = 1;

    @AssertTrue(message = "validFrom must be before validTo")

    public boolean isTimeRangeValid() {
        return validFrom == null || validTo == null || validFrom.isBefore(validTo);
    }

    @AssertTrue(message = "Percentage must not exceed 100")
    public boolean isPercentageValid() {
        return discountType != DiscountType.PERCENT || discountValue == null || discountValue.compareTo(BigDecimal.valueOf(100)) <= 0;
    }
}
