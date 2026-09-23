package com.cgv.marketingservice.service.impl;

import com.cgv.commondto.dto.PageResponse;
import com.cgv.commondto.enums.MembershipTier;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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
    @CacheEvict(value = {"promotions:active", "promotions:detail"}, allEntries = true)
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
    @Cacheable(value = "promotions:detail", key = "#id")
    public PromotionResponse getPromotion(UUID id) {
        Promotion promotion = promotionRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTED, "Promotion not found"));
        return promotionMapper.toPromotionResponse(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "promotions:active")
    public List<PromotionResponse> getActivePromotions() {
        return promotionRepository.findActivePromotions(Instant.now())
                .stream()
                .map(promotionMapper::toPromotionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> getAvailablePromotions(BigDecimal totalAmount, String tier) {
        MembershipTier userTier = MembershipTier.fromCode(tier);
        Instant now = Instant.now();

        List<MembershipTier> eligibleTiers = Arrays.stream(MembershipTier.values())
                .filter(t -> t.canApply(userTier))
                .toList();

        BigDecimal filterAmount = (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0)
                ? totalAmount
                : null;

        return promotionRepository.findAvailablePromotions(now, eligibleTiers, filterAmount).stream()
                .map(promotionMapper::toPromotionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> getAllPromotions(Pageable pageable) {
        Page<Promotion> page = promotionRepository.findAll(pageable);
        List<PromotionResponse> list = page.getContent().stream()
                .map(promotionMapper::toPromotionResponse)
                .toList();

        return PageResponse.<PromotionResponse>builder()
                .data(list)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }
}
