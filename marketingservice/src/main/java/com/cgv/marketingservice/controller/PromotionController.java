package com.cgv.marketingservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.commondto.dto.PageResponse;
import com.cgv.marketingservice.dto.request.PromotionCreateRequest;
import com.cgv.marketingservice.dto.response.PromotionResponse;
import com.cgv.marketingservice.service.PromotionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/marketings/promotions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromotionController {
    PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasAuthority('promotion:manage')")
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

    @GetMapping("/active")
    public ApiResponse<List<PromotionResponse>> getActivePromotions() {
        return ApiResponse.<List<PromotionResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Danh sách mã khuyến mãi đang hoạt động")
                .data(promotionService.getActivePromotions())
                .build();
    }

    @GetMapping("/available")
    public ApiResponse<List<PromotionResponse>> getAvailablePromotions(
            @RequestParam(value = "totalAmount", required = false) java.math.BigDecimal totalAmount,
            @RequestParam(value = "tier", required = false) String tier
    ) {
        return ApiResponse.<List<PromotionResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Danh sách mã khuyến mãi khả dụng theo hạng và tổng tiền")
                .data(promotionService.getAvailablePromotions(totalAmount, tier))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('promotion:manage', 'SUPER_ADMIN')")
    public ApiResponse<PageResponse<PromotionResponse>> getAll(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<PromotionResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Danh sách toàn bộ khuyến mãi")
                .data(promotionService.getAllPromotions(pageable))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('promotion:manage', 'SUPER_ADMIN')")
    public ApiResponse<PromotionResponse> update(
            @PathVariable("id") UUID id,
            @RequestBody @Valid com.cgv.marketingservice.dto.request.PromotionUpdateRequest request
    ) {
        PromotionResponse response = promotionService.updatePromotion(id, request);
        return ApiResponse.<PromotionResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật khuyến mãi thành công")
                .data(response)
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('promotion:manage', 'SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable("id") UUID id) {
        promotionService.deletePromotion(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa mã khuyến mãi thành công")
                .build();
    }
}
