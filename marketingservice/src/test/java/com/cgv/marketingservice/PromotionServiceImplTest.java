package com.cgv.marketingservice;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.marketingservice.dto.request.PromotionCreateRequest;
import com.cgv.marketingservice.entity.Promotion;
import com.cgv.marketingservice.enums.DiscountType;
import com.cgv.marketingservice.mapper.PromotionMapper;
import com.cgv.marketingservice.repository.PromotionRepository;
import com.cgv.marketingservice.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

class PromotionServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private com.cgv.marketingservice.repository.PromotionUsageRepository promotionUsageRepository;

    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    private PromotionServiceImpl promotionService;

    @BeforeEach
    void setUp() {
        PromotionMapper mapper = Mappers.getMapper(PromotionMapper.class);

        promotionService = new PromotionServiceImpl(promotionRepository, promotionUsageRepository, mapper, stringRedisTemplate);
    }

    @Test
    void createPromotionUppercaseCode() {
        var request = validRequest();
        request.setCode("cgv30");

        UUID savedId = UUID.randomUUID();

        when(promotionRepository.existsByCode("CGV30")).thenReturn(false);
        when(promotionRepository.saveAndFlush(any(Promotion.class)))
                .thenAnswer(invocation -> {
                    Promotion promotion = invocation.getArgument(0);
                    promotion.setId(savedId);
                    return promotion;
                });

        var response = promotionService.createPromotion(request);

        var captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).saveAndFlush(captor.capture());

        assertEquals("CGV30", captor.getValue().getCode());
        assertEquals("CGV30", response.getCode());
        assertEquals(savedId, response.getId());

        verify(promotionRepository).existsByCode("CGV30");
    }

    @Test
    void createPromotionRejectDuplicateCode() {
        var request = validRequest();

        when(promotionRepository.existsByCode("CGV30")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> promotionService.createPromotion(request));

        assertEquals(ErrorCode.DUPLICATE.getCode(), exception.getCode());
        assertEquals(ErrorCode.DUPLICATE.getHttpStatus(), exception.getStatus());

        verify(promotionRepository, never()).saveAndFlush(any(Promotion.class));
    }

    @Test
    void getPromotionReturnsExistingPromotion() {
        UUID id = UUID.randomUUID();

        var promotion = new  Promotion();
        promotion.setId(id);
        promotion.setCode("CGV30");
        promotion.setName("September promotion");
        promotion.setDiscountType(DiscountType.PERCENT);
        promotion.setDiscountValue(new BigDecimal("30"));

        when(promotionRepository.findById(id)).thenReturn(Optional.of(promotion));

        var response = promotionService.getPromotion(id);

        assertEquals(id, response.getId());
        assertEquals("CGV30", response.getCode());
        assertEquals("September promotion", response.getName());
        assertEquals(DiscountType.PERCENT, response.getDiscountType());
        assertEquals(new BigDecimal("30"), response.getDiscountValue());

        verify(promotionRepository).findById(id);
    }

    @Test
    void getPromotionRejectMissingId() {
        UUID id = UUID.randomUUID();

        when(promotionRepository.findById(id)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> promotionService.getPromotion(id));

        assertEquals(ErrorCode.NOT_EXISTED.getCode(),exception.getCode());
        assertEquals(ErrorCode.NOT_EXISTED.getHttpStatus(),exception.getStatus());
    }

    @Test
    void createPromotionTrimsName() {
        var request = validRequest();
        request.setName("  Sale  ");

        when(promotionRepository.existsByCode("CGV30")).thenReturn(false);

        when(promotionRepository.saveAndFlush(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = promotionService.createPromotion(request);

        var captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).saveAndFlush(captor.capture());

        assertEquals("Sale", captor.getValue().getName());
        assertEquals("Sale", response.getName());
    }

    @Test
    void getAllPromotionsReturnsPage() {
        var pageable = PageRequest.of(0, 10);

        var promotion = new Promotion();
        promotion.setId(UUID.randomUUID());
        promotion.setCode("CGV30");

        var page = new PageImpl<>(
                List.of(promotion),
                pageable,
                1
        );

        when(promotionRepository.findAll(pageable))
                .thenReturn(page);

        var response = promotionService.getAllPromotions(pageable);

        assertEquals(1, response.getData().size());
        assertEquals(promotion.getId(), response.getData().get(0).getId());
        assertEquals("CGV30", response.getData().get(0).getCode());
        assertEquals(1, response.getPageNumber());
        assertEquals(10, response.getPageSize());
        assertEquals(1, response.getTotalPages());
        assertEquals(1L, response.getTotalElements());

        verify(promotionRepository).findAll(pageable);
    }

    private PromotionCreateRequest validRequest() {
        var request = new PromotionCreateRequest();
        request.setCode("CGV30");
        request.setName("September promotion");
        request.setDiscountType(DiscountType.PERCENT);
        request.setDiscountValue(new BigDecimal("30"));
        request.setValidFrom(Instant.parse("2026-09-01T00:00:00Z"));
        request.setValidTo(Instant.parse("2026-10-01T00:00:00Z"));
        return request;
    }

}
