package com.cgv.marketingservice.grpc;

import com.cgv.commondto.enums.MembershipTier;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.commondto.grpc.*;
import com.cgv.marketingservice.entity.Promotion;
import com.cgv.marketingservice.enums.DiscountType;
import com.cgv.marketingservice.repository.PromotionRepository;
import com.cgv.marketingservice.repository.PromotionUsageRepository;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class PromotionGrpcServiceImpl extends PromotionGrpcServiceGrpc.PromotionGrpcServiceImplBase{
    PromotionRepository promotionRepository;
    PromotionUsageRepository promotionUsageRepository;

    @Override
    public void validateAndApplyPromotion(PromotionApplyRequest request, StreamObserver<PromotionApplyResponse> responseObserver) {
        try {
            boolean isValid = true;
            UUID promotionId = UUID.fromString(request.getPromotionId());
            BigDecimal orderAmount = BigDecimal.valueOf(request.getOrderAmount());
            MembershipTier membershipTier = MembershipTier.fromCode(request.getUserTier());
            Promotion promotion = promotionRepository.findById(promotionId)
                            .orElse(null);
            if(promotion == null || !promotion.isActive()){
                sendResponse(responseObserver, false, "Mã khuyến mãi không tồn tại hoặc đã ngừng áp dụng", 0, orderAmount.doubleValue(), "");
                return;
            }
            Instant now = Instant.now();
            if(now.isBefore(promotion.getValidFrom()) || now.isAfter(promotion.getValidTo())){
                sendResponse(responseObserver, false, "Mã khuyến mãi chưa bắt đầu hoặc đã hết hạn", 0, orderAmount.doubleValue(), promotion.getCode());
                return;
            }
            if(promotion.getApplicableTier() != null && !promotion.getApplicableTier().canApply(membershipTier)){
                String errorMsg = String.format("Mã chỉ áp dụng cho hạng %s trở lên (Hạng hiện tại của bạn: %s)", promotion.getApplicableTier().getName(), membershipTier.getName());
                sendResponse(responseObserver, false, errorMsg, 0, orderAmount.doubleValue(), promotion.getCode());
                return;
            }
            if(promotion.getMinOrderValue() != null && (orderAmount.compareTo(promotion.getMinOrderValue()) < 0)){
                String errorMsg = String.format("Mã chỉ áp dụng cho đơn %s trở lên (Đơn hàng hiện tại của bạn: %s)", promotion.getMinOrderValue(), orderAmount);
                sendResponse(responseObserver, false, errorMsg, 0, orderAmount.doubleValue(), promotion.getCode());
                return;
            }

            long usedCount = promotionUsageRepository.countByPromotionIdAndUserId(promotionId , request.getUserId());
            if(usedCount >= promotion.getMaxUsesPerUser()){
                String errorMsg = String.format("Mã đã quá lượt sử dụng)");
                sendResponse(responseObserver, false, errorMsg, 0, orderAmount.doubleValue(), promotion.getCode());
                return;
            }
            BigDecimal discount = BigDecimal.ZERO;
            if(promotion.getDiscountType().equals(DiscountType.PERCENT)){
                discount = orderAmount.multiply(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(100),0, BigDecimal.ROUND_HALF_UP);
                if (promotion.getMaxDiscountAmount() != null && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                    discount = promotion.getMaxDiscountAmount();
                }
            }else {
                discount = promotion.getDiscountValue();
            }

            if (discount.compareTo(orderAmount) > 0) {
                discount = orderAmount;
            }
            BigDecimal finalAmount = orderAmount.subtract(discount);
            log.info("Áp dụng mã {} thành công cho user {}, giảm: {}, còn lại: {}",
                    promotion.getCode(), request.getUserId(), discount, finalAmount);
            sendResponse(responseObserver, true, "Áp dụng mã giảm giá thành công",
                    discount.doubleValue(), finalAmount.doubleValue(), promotion.getCode());
        } catch (Exception e) {
            log.error("Lỗi khi validate promotion: ", e);
            sendResponse(responseObserver, false, "Lỗi hệ thống khi kiểm tra khuyến mãi: " + e.getMessage(), 0, request.getOrderAmount(), "");
        }
    }
    private void sendResponse(StreamObserver<PromotionApplyResponse> responseObserver , boolean isSuccess, String message, double discount , double finalAmount , String code) {
        PromotionApplyResponse response = PromotionApplyResponse.newBuilder()
                .setIsValid(isSuccess)
                .setMessage(message)
                .setDiscountAmount(discount)
                .setFinalAmount(finalAmount)
                .setPromotionCode(code)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
