package com.cgv.marketingservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.marketingservice.dto.request.PromotionCreateRequest;
import com.cgv.marketingservice.dto.response.PromotionResponse;
import com.cgv.marketingservice.service.PromotionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/marketings/promotions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasAuthority('promotion:manage')")
public class PromotionController {
    PromotionService promotionService;

    @PostMapping
    public ApiResponse<PromotionResponse> create(@RequestBody @Valid PromotionCreateRequest request) {
        PromotionResponse respone = promotionService.createPromotion(request);

        return ApiResponse.<PromotionResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Promotion created")
                        .data(respone)
                        .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PromotionResponse> getById(@PathVariable("id") UUID id) {
        return ApiResponse.<PromotionResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Promotion details")
                .data(promotionService.getPromotion(id))
                .build();
    }
}
