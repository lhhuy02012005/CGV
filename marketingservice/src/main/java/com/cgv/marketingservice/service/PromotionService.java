package com.cgv.marketingservice.service;

import com.cgv.commondto.dto.PageResponse;
import com.cgv.marketingservice.dto.request.PromotionCreateRequest;
import com.cgv.marketingservice.dto.response.PromotionResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PromotionService {
    PromotionResponse createPromotion(PromotionCreateRequest request);
    PromotionResponse getPromotion(UUID id);
    PageResponse<PromotionResponse> getAllPromotions(Pageable pageable);
}
