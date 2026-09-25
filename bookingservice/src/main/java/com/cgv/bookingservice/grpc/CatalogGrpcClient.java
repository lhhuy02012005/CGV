package com.cgv.bookingservice.grpc;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.commondto.grpc.ShowtimePricingGrpcServiceGrpc;
import com.cgv.commondto.grpc.ShowtimePricingRequest;
import com.cgv.commondto.grpc.ShowtimePricingResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CatalogGrpcClient {
    @GrpcClient("catalog-service")
    ShowtimePricingGrpcServiceGrpc.ShowtimePricingGrpcServiceBlockingStub pricingStub;

    public ShowtimePricingResponse getShowtimePricing(UUID showtimeId){
        ShowtimePricingRequest request = ShowtimePricingRequest.newBuilder()
                .setShowtimeId(showtimeId.toString())
                .build();

        try {
            log.info("Gọi gRPC sang CatalogService lấy thông tin suất chiếu: {}", showtimeId);
            return pricingStub.getShowtimePricing(request);
        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.FAILED_PRECONDITION
                    || e.getStatus().getCode() == io.grpc.Status.Code.INVALID_ARGUMENT
                    || e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                log.warn("Catalog gRPC từ chối yêu cầu với lỗi nghiệp vụ: {}", e.getStatus().getDescription());
                throw new BusinessException(ErrorCode.BAD_REQUEST, e.getStatus().getDescription() != null ? e.getStatus().getDescription() : e.getMessage());
            }
            log.warn("Lỗi khi gọi gRPC qua stub cấu hình: {}. Đang thử fallback trực tiếp các port 9090 và 9098...", e.getMessage());

            for (int port : new int[]{9090, 9098}) {
                io.grpc.ManagedChannel channel = null;
                try {
                    channel = io.grpc.ManagedChannelBuilder.forAddress("127.0.0.1", port)
                            .usePlaintext()
                            .build();
                    ShowtimePricingGrpcServiceGrpc.ShowtimePricingGrpcServiceBlockingStub fallbackStub =
                            ShowtimePricingGrpcServiceGrpc.newBlockingStub(channel);
                    ShowtimePricingResponse res = fallbackStub.getShowtimePricing(request);
                    log.info("Kết nối thành công gRPC CatalogService tại port {}", port);
                    return res;
                } catch (Exception fallbackEx) {
                    log.debug("Thử port {} thất bại: {}", port, fallbackEx.getMessage());
                } finally {
                    if (channel != null) {
                        try {
                            channel.shutdown().awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS);
                        } catch (Exception ignored) {}
                    }
                }
            }

            log.error("Tất cả các kết nối gRPC sang CatalogService đều thất bại: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể kết nối đến hệ thống Catalog để lấy thông tin giá vé!");
        }
    }
}
