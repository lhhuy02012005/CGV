package com.cgv.marketingservice;

import com.cgv.marketingservice.dto.request.PromotionCreateRequest;
import com.cgv.marketingservice.enums.DiscountType;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PromotionCreateRequestTest {
    @Test
    void validatesPercentageAndTimeRange() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            var request = new PromotionCreateRequest();
            request.setCode("CGV30");
            request.setName("September promotion");
            request.setDiscountType(DiscountType.PERCENT);
            request.setDiscountValue(new BigDecimal("30"));
            request.setValidFrom(Instant.parse("2026-09-01T00:00:00Z"));
            request.setValidTo(Instant.parse("2026-10-01T00:00:00Z"));

            assertTrue(validator.validate(request).isEmpty());

            request.setDiscountValue(new BigDecimal("101"));

            assertTrue(validator.validate(request).stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("percentageValid")));

            request.setDiscountValue(new BigDecimal("30"));
            request.setValidTo(request.getValidFrom());

            assertTrue(validator.validate(request).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("timeRangeValid")));

        }
    }
}
