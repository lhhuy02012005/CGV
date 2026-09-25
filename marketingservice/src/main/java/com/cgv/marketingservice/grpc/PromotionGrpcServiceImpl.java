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
    org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    org.springframework.data.redis.core.script.DefaultRedisScript<Long> voucherReserveScript;
    org.springframework.data.redis.core.script.DefaultRedisScript<Long> voucherReleaseScript;

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
            boolean isGuest = request.getUserTier() == null
                    || request.getUserTier().isBlank()
                    || request.getUserTier().equalsIgnoreCase("GUEST")
                    || (request.getUserId() != null && request.getUserId().startsWith("guest_"));

            if (promotion.getApplicableTier() != null && promotion.getApplicableTier() != MembershipTier.ALL) {
                if (isGuest) {
                    String errorMsg = String.format("Mã khuyến mãi chỉ dành cho tài khoản thành viên từ hạng %s trở lên. Vui lòng đăng nhập để áp dụng!", promotion.getApplicableTier().getName());
                    sendResponse(responseObserver, false, errorMsg, 0, orderAmount.doubleValue(), promotion.getCode());
                    return;
                }
                if (!promotion.getApplicableTier().canApply(membershipTier)) {
                    String errorMsg = String.format("Mã chỉ áp dụng cho hạng %s trở lên (Hạng hiện tại của bạn: %s)", promotion.getApplicableTier().getName(), membershipTier.getName());
                    sendResponse(responseObserver, false, errorMsg, 0, orderAmount.doubleValue(), promotion.getCode());
                    return;
                }
            }
            if(promotion.getMinOrderValue() != null && (orderAmount.compareTo(promotion.getMinOrderValue()) < 0)){
                String errorMsg = String.format("Mã chỉ áp dụng cho đơn %s trở lên (Đơn hàng hiện tại của bạn: %s)", promotion.getMinOrderValue(), orderAmount);
                sendResponse(responseObserver, false, errorMsg, 0, orderAmount.doubleValue(), promotion.getCode());
                return;
            }

            long usedCount = promotionUsageRepository.countByPromotionIdAndUserId(promotionId , request.getUserId());
            if(usedCount >= promotion.getMaxUsesPerUser()){
                String errorMsg = "Mã đã quá lượt sử dụng cho tài khoản này";
                sendResponse(responseObserver, false, errorMsg, 0, orderAmount.doubleValue(), promotion.getCode());
                return;
            }

            // Kiểm soát số lượng slot còn lại bằng Redis Lua Script (Atomic check-and-decrement)
            if (promotion.getUsageLimit() != null) {
                String slotKey = "{promo:" + promotionId + "}:slots";
                String userKey = "{promo:" + promotionId + "}:user:" + request.getUserId();
                long usedSoFar = promotionUsageRepository.countByPromotionId(promotionId);
                long initialSlots = Math.max(0, promotion.getUsageLimit() - usedSoFar);

                Long result = stringRedisTemplate.execute(
                        voucherReserveScript,
                        List.of(slotKey, userKey),
                        String.valueOf(promotion.getMaxUsesPerUser()),
                        "900", // Giữ slot trong 15 phút (tương đương payment deadline)
                        String.valueOf(initialSlots)
                );

                if (result == null || result == -1L) {
                    sendResponse(responseObserver, false, "Mã khuyến mãi đã hết lượt sử dụng trên hệ thống", 0, orderAmount.doubleValue(), promotion.getCode());
                    return;
                } else if (result == -2L) {
                    sendResponse(responseObserver, false, "Bạn đang có giao dịch giữ mã khuyến mãi này hoặc đã quá lượt sử dụng", 0, orderAmount.doubleValue(), promotion.getCode());
                    return;
                }
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

    @Override
    public void releasePromotion(PromotionReleaseRequest request, StreamObserver<PromotionReleaseResponse> responseObserver) {
        try {
            if (request.getPromotionId() != null && !request.getPromotionId().isBlank()) {
                String slotKey = "{promo:" + request.getPromotionId() + "}:slots";
                String userKey = "{promo:" + request.getPromotionId() + "}:user:" + request.getUserId();
                stringRedisTemplate.execute(voucherReleaseScript, List.of(slotKey, userKey));
                log.info("Đã giải phóng slot voucher {} cho user {}", request.getPromotionId(), request.getUserId());
            }
            responseObserver.onNext(PromotionReleaseResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Giải phóng slot voucher thành công")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Lỗi khi giải phóng slot voucher: ", e);
            responseObserver.onNext(PromotionReleaseResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Lỗi khi giải phóng voucher: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
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
