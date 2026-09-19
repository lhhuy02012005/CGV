package com.cgv.marketingservice;

import com.cgv.marketingservice.dto.request.PromotionCreateRequest;
import com.cgv.marketingservice.enums.DiscountType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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

    private PromotionCreateRequest valideRequest() {
        var request = new PromotionCreateRequest();
        request.setCode("CGV30");
        request.setName("September promotion");
        request.setDiscountType(DiscountType.PERCENT);
        request.setDiscountValue(new BigDecimal("30"));
        request.setValidFrom(Instant.parse("2026-09-01T00:00:00Z"));
        request.setValidTo(Instant.parse("2026-10-01T00:00:00Z"));
        return request;
    }

    private void assertInvalid(Validator validator, PromotionCreateRequest request, String property) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation ->
                        violation.getPropertyPath().toString().equals(property)), "Expected validation error for: " + property);
    }

    @Test
    void rejectsInvalidFieldValue() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            var request = valideRequest();
            request.setCode("");
            assertInvalid(validator, request, "code");

            request = valideRequest();
            request.setCode("CGV 30");
            assertInvalid(validator, request, "code");

            request = valideRequest();
            request.setName("  ");
            assertInvalid(validator, request, "name");

            request = valideRequest();
            request.setDiscountValue(BigDecimal.ZERO);
            assertInvalid(validator, request, "discountValue");

            request = valideRequest();
            request.setDiscountValue(new BigDecimal("-1"));
            assertInvalid(validator, request, "discountValue");

            request = valideRequest();
            request.setUsageLimit(0);
            assertInvalid(validator, request, "usageLimit");

            request = valideRequest();
            request.setMaxUsesPerUser(0);
            assertInvalid(validator, request, "maxUsesPerUser");

            request = valideRequest();
            request.setApplicableTier("GOLD");
            assertInvalid(validator, request, "applicableTier");

            request = valideRequest();
            request.setValidFrom(request.getValidTo().plusSeconds(1));
            assertInvalid(validator, request, "timeRangeValid");

            request = valideRequest();
            request.setMinOrderValue(new BigDecimal("-1"));
            assertInvalid(validator, request, "minOrderValue");
        }
    }

    @Test void rejectMissingRequiredField() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            var request = valideRequest();
            request.setDiscountType(null);
            assertInvalid(validator, request, "discountType");

            request = valideRequest();
            request.setValidFrom(null);
            assertInvalid(validator, request, "validFrom");

            request = valideRequest();
            request.setValidTo(null);
            assertInvalid(validator, request, "validTo");
        }
    }

    @Test
    void acceptsPercentageBoundaryAndFixedAmount() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            var request = valideRequest();
            request.setDiscountValue(new BigDecimal("100"));

            assertTrue(validator.validate(request).isEmpty(), "PERCENT = 100 must be valid");

            request = valideRequest();
            request.setDiscountType(DiscountType.FIXED);
            request.setDiscountValue(new BigDecimal("50000"));
            assertTrue(validator.validate(request).isEmpty(), "FIXED = 50000 must be valid");
        }
    }
}
