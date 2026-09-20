package com.cgv.marketingservice.service.impl;

import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.marketingservice.dto.request.PromotionCreateRequest;
import com.cgv.marketingservice.dto.response.PromotionResponse;
import com.cgv.marketingservice.entity.Promotion;
import com.cgv.marketingservice.mapper.PromotionMapper;
import com.cgv.marketingservice.repository.PromotionRepository;
import com.cgv.marketingservice.service.PromotionService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "PROMOTION-SERVICE")

public class PromotionServiceImpl implements PromotionService {
    PromotionRepository promotionRepository;
    PromotionMapper promotionMapper;

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionCreateRequest request) {
        String code = request.getCode().toUpperCase(Locale.ROOT);

        if (promotionRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.DUPLICATE,"Promotion code already exists");
        }

        Promotion promotion = promotionMapper.toPromotion(request);
        promotion.setCode(code);
        promotion.setName(request.getName().trim());

        Promotion saved = promotionRepository.saveAndFlush(promotion);

        log.info("Created promotion {}", saved.getId());

        return promotionMapper.toPromotionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotion(UUID id) {
        Promotion promotion = promotionRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTED, "Promotion not found"));
        return promotionMapper.toPromotionResponse(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> getAllPromotions(Pageable pageable) {
        var promotionPage = promotionRepository.findAll(pageable);

        var responses = promotionPage.getContent().stream()
                .map(promotionMapper::toPromotionResponse)
                .toList();

        return PageResponse.<PromotionResponse>builder()
                .data(responses)
                .pageNumber(promotionPage.getNumber() + 1)
                .pageSize(promotionPage.getSize())
                .totalPages(promotionPage.getTotalPages())
                .totalElements(promotionPage.getTotalElements())
                .build();
    }
}
