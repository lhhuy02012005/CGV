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
import com.cgv.marketingservice.dto.request.PromotionUpdateRequest;
import com.cgv.marketingservice.repository.PromotionUsageRepository;
import org.springframework.data.redis.core.StringRedisTemplate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "PROMOTION-SERVICE")
public class PromotionServiceImpl implements PromotionService {
    PromotionRepository promotionRepository;
    PromotionUsageRepository promotionUsageRepository;
    PromotionMapper promotionMapper;
    StringRedisTemplate stringRedisTemplate;

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
        boolean isGuest = tier == null || tier.isBlank() || tier.equalsIgnoreCase("GUEST");
        List<MembershipTier> eligibleTiers;
        if (isGuest) {
            eligibleTiers = List.of(MembershipTier.ALL);
        } else {
            MembershipTier userTier = MembershipTier.fromCode(tier);
            eligibleTiers = Arrays.stream(MembershipTier.values())
                    .filter(t -> t.canApply(userTier))
                    .toList();
        }

        Instant now = Instant.now();
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

    @Override
    @Transactional
    @CacheEvict(value = {"promotions:active", "promotions:detail"}, allEntries = true)
    public PromotionResponse updatePromotion(java.util.UUID id, PromotionUpdateRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTED, "Không tìm thấy mã khuyến mãi với id: " + id));

        long usedCount = promotionUsageRepository.countByPromotionId(id);

        if (usedCount > 0) {
            // Đã có khách hàng sử dụng: CHẶN sửa các trường tài chính cốt lõi
            if (request.getCode() != null && !request.getCode().equalsIgnoreCase(promotion.getCode())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể đổi mã voucher vì đã có " + usedCount + " lượt khách hàng sử dụng!");
            }
            if (request.getDiscountType() != null && !request.getDiscountType().equals(promotion.getDiscountType())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể đổi loại giảm giá (DiscountType) của voucher đã có người sử dụng!");
            }
            if (request.getDiscountValue() != null && request.getDiscountValue().compareTo(promotion.getDiscountValue()) != 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể thay đổi giá trị giảm giá (DiscountValue) của voucher đã có người sử dụng!");
            }
            if (request.getMinOrderValue() != null && request.getMinOrderValue().compareTo(promotion.getMinOrderValue()) != 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể thay đổi điều kiện đơn tối thiểu (MinOrderValue) của voucher đã có người sử dụng!");
            }
            if (request.getUsageLimit() != null && request.getUsageLimit() < usedCount) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Giới hạn sử dụng mới (" + request.getUsageLimit() + ") không được nhỏ hơn số lượt đã dùng thực tế (" + usedCount + ")!");
            }
        } else {
            // Chưa có ai sử dụng: Cho phép sửa tự do
            if (request.getCode() != null && !request.getCode().isBlank()) {
                String newCode = request.getCode().toUpperCase(Locale.ROOT);
                if (!newCode.equals(promotion.getCode()) && promotionRepository.existsByCode(newCode)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã khuyến mãi '" + newCode + "' đã tồn tại!");
                }
                promotion.setCode(newCode);
            }
            if (request.getDiscountType() != null) promotion.setDiscountType(request.getDiscountType());
            if (request.getDiscountValue() != null) promotion.setDiscountValue(request.getDiscountValue());
            if (request.getMaxDiscountAmount() != null) promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
            if (request.getMinOrderValue() != null) promotion.setMinOrderValue(request.getMinOrderValue());
            if (request.getValidFrom() != null) promotion.setValidFrom(request.getValidFrom());
            if (request.getApplicableTier() != null) {
                promotion.setApplicableTier(MembershipTier.fromCode(request.getApplicableTier()));
            }
        }

        // Các trường luôn được phép sửa
        if (request.getName() != null && !request.getName().isBlank()) {
            promotion.setName(request.getName());
        }
        if (request.getDescription() != null) {
            promotion.setDescription(request.getDescription());
        }
        if (request.getValidTo() != null) {
            promotion.setValidTo(request.getValidTo());
        }
        if (request.getUsageLimit() != null) {
            promotion.setUsageLimit(request.getUsageLimit());
        }
        if (request.getMaxUsesPerUser() != null) {
            promotion.setMaxUsesPerUser(request.getMaxUsesPerUser());
        }
        if (request.getIsActive() != null) {
            promotion.setActive(request.getIsActive());
        }

        Promotion saved = promotionRepository.save(promotion);

        // Đồng bộ cache Redis nếu cần
        try {
            String redisKey = "voucher:" + saved.getCode();
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey))) {
                if (saved.getUsageLimit() != null) {
                    stringRedisTemplate.opsForHash().put(redisKey, "max_usage", String.valueOf(saved.getUsageLimit()));
                }
                if (!saved.isActive()) {
                    stringRedisTemplate.delete(redisKey);
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi đồng bộ Redis cache cho voucher {}: {}", saved.getCode(), e.getMessage());
        }

        log.info("Admin đã cập nhật thành công voucher: ID={}, Code={}", saved.getId(), saved.getCode());
        return promotionMapper.toPromotionResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"promotions:active", "promotions:detail"}, allEntries = true)
    public void deletePromotion(java.util.UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTED, "Không tìm thấy mã khuyến mãi với id: " + id));

        long usedCount = promotionUsageRepository.countByPromotionId(id);
        if (usedCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Không thể xóa voucher '" + promotion.getCode() + "' vì đã có " + usedCount +
                    " lượt khách hàng sử dụng trong các đơn đặt vé. Vui lòng ngưng kích hoạt (isActive = false) để đảm bảo toàn vẹn dữ liệu kế toán!");
        }

        promotionRepository.delete(promotion);
        try {
            stringRedisTemplate.delete("voucher:" + promotion.getCode());
            stringRedisTemplate.delete("voucher:" + promotion.getCode() + ":users");
        } catch (Exception e) {
            log.warn("Lỗi xóa cache Redis cho voucher {}: {}", promotion.getCode(), e.getMessage());
        }
        log.info("Đã xóa hoàn toàn voucher chưa sử dụng: ID={}, Code={}", id, promotion.getCode());
    }
}
