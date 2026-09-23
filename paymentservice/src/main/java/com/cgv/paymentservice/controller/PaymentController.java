package com.cgv.paymentservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.paymentservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {

    PaymentService paymentService;

    /**
     * API Tạo link thanh toán VNPAY
     */
    @PostMapping("/vnpay/create-url")
    public ApiResponse<String> createVnpayUrl(
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID bookingId,
            @RequestParam BigDecimal amount
    ) {
        String userId = jwt.getSubject();
        String paymentUrl = paymentService.add(request, bookingId, amount, userId);
        return ApiResponse.<String>builder()
                .status(200)
                .message("Tạo URL thanh toán VNPAY thành công!")
                .data(paymentUrl)
                .build();
    }

    /**
     * API Nhận Webhook / Return từ VNPAY sau khi khách quét mã xong
     */
    @GetMapping("/vnpay-callback")
    public ApiResponse<Boolean> vnpayCallback(HttpServletRequest request) {
        boolean success = paymentService.callback(request);
        return ApiResponse.<Boolean>builder()
                .status(success ? 200 : 400)
                .message(success ? "Thanh toán thành công!" : "Thanh toán thất bại hoặc đã bị huỷ!")
                .data(success)
                .build();
    }

    /**
     * API Tra cứu trạng thái thanh toán theo bookingId
     */
    @GetMapping("/booking/{bookingId}")
    public ApiResponse<com.cgv.paymentservice.dto.PaymentStatusResponse> getPaymentStatus(@PathVariable UUID bookingId) {
        return ApiResponse.<com.cgv.paymentservice.dto.PaymentStatusResponse>builder()
                .status(200)
                .message("Tra cứu trạng thái thanh toán thành công")
                .data(paymentService.getPaymentStatusByBookingId(bookingId))
                .build();
    }
}