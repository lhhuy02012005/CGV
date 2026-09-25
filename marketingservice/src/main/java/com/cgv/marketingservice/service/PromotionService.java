package com.cgv.marketingservice.service;

import com.cgv.commondto.dto.PageResponse;
import com.cgv.marketingservice.dto.request.PromotionCreateRequest;
import com.cgv.marketingservice.dto.response.PromotionResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PromotionService {
    PromotionResponse createPromotion(PromotionCreateRequest request);
    PromotionResponse getPromotion(UUID id);
    List<PromotionResponse> getActivePromotions();
    List<PromotionResponse> getAvailablePromotions(BigDecimal totalAmount, String tier);
    PageResponse<PromotionResponse> getAllPromotions(Pageable pageable);
    PromotionResponse updatePromotion(UUID id, com.cgv.marketingservice.dto.request.PromotionUpdateRequest request);
    void deletePromotion(UUID id);
}
