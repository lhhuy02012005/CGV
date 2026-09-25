package com.cgv.bookingservice.grpc;

import com.cgv.commondto.grpc.PromotionApplyRequest;
import com.cgv.commondto.grpc.PromotionApplyResponse;
import com.cgv.commondto.grpc.PromotionGrpcServiceGrpc;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MarketingGrpcClient {
    @GrpcClient("marketing-service")
    PromotionGrpcServiceGrpc.PromotionGrpcServiceBlockingStub promotionGrpcServiceBlockingStub;

    public PromotionApplyResponse applyPromotion(UUID promotionId, String userId, String userTier, double orderAmount) {
        try {
            PromotionApplyRequest request = PromotionApplyRequest.newBuilder()
                    .setUserId(userId)
                    .setOrderAmount(orderAmount)
                    .setUserTier(userTier)
                    .setPromotionId(promotionId.toString())
                    .build();
            return promotionGrpcServiceBlockingStub.validateAndApplyPromotion(request);
        }catch (Exception e) {
            log.error("Lỗi khi gọi gRPC sang MarketingService: ", e);
            return PromotionApplyResponse.newBuilder()
                    .setIsValid(false)
                    .setMessage("Không thể kết nối tới dịch vụ khuyến mãi: " + e.getMessage())
                    .setDiscountAmount(0)
                    .setFinalAmount(orderAmount)
                    .build();
        }
    }

    public void releasePromotion(UUID promotionId, String userId) {
        if (promotionId == null || userId == null) return;
        try {
            com.cgv.commondto.grpc.PromotionReleaseRequest request = com.cgv.commondto.grpc.PromotionReleaseRequest.newBuilder()
                    .setPromotionId(promotionId.toString())
                    .setUserId(userId)
                    .build();
            promotionGrpcServiceBlockingStub.releasePromotion(request);
            log.info("Đã yêu cầu release voucher {} cho user {}", promotionId, userId);
        } catch (Exception e) {
            log.warn("Không thể release voucher {} qua gRPC: {}", promotionId, e.getMessage());
        }
    }
}
