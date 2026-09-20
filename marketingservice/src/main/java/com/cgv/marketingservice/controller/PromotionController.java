package com.cgv.marketingservice.controller;

import com.cgv.commondto.dto.PageResponse;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.cgv.commondto.dto.ApiResponse;
import com.cgv.marketingservice.dto.request.PromotionCreateRequest;
import com.cgv.marketingservice.dto.response.PromotionResponse;
import com.cgv.marketingservice.service.PromotionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
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

    @GetMapping
    public ApiResponse<PageResponse<PromotionResponse>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "page must be >= 0 and size must be between 1 and 100"
            );
        }

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );

        return ApiResponse.<PageResponse<PromotionResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Promotion list")
                .data(promotionService.getAllPromotions(pageable))
                .build();
    }
}
