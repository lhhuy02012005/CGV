package com.cgv.paymentservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.paymentservice.service.PaymentService;
import com.cgv.paymentservice.service.BookingPaymentAccess;
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
    BookingPaymentAccess bookingPaymentAccess;

    /**
     * API Tạo link thanh toán VNPAY
     */
    @PostMapping("/vnpay/create-url")
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<String> createVnpayUrl(
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID bookingId,
            @RequestParam(required = false) BigDecimal amount
    ) {
        BookingPaymentAccess.Access access = bookingPaymentAccess.verify(bookingId, amount,
                jwt == null ? null : jwt.getSubject());
        String paymentUrl = paymentService.add(request, bookingId, amount, access.owner(), access.deadline());
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

    @GetMapping("/vnpay-ipn")
    public java.util.Map<String, String> vnpayIpn(HttpServletRequest request) {
        try {
            paymentService.callback(request);
            return java.util.Map.of("RspCode", "00", "Message", "Notification processed");
        } catch (com.cgv.commondto.exception.BusinessException exception) {
            return java.util.Map.of("RspCode", "97", "Message", "Invalid payment notification");
        } catch (Exception exception) {
            return java.util.Map.of("RspCode", "99", "Message", "Processing error");
        }
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
