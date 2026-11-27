package com.cgv.paymentservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.commondto.dto.PageResponse;
import com.cgv.paymentservice.config.VnpayConfig;
import com.cgv.paymentservice.dto.PaymentAdminResponse;
import com.cgv.paymentservice.dto.ReconcileResponse;
import com.cgv.paymentservice.enums.PaymentStatus;
import com.cgv.paymentservice.service.PaymentAdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/payments/admin")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentAdminService paymentAdminService;

    /** 21.1 + 21.2: Tra cứu, lọc giao dịch theo trạng thái / cổng / thời gian. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('payment:view', 'ticket:refund', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<PageResponse<PaymentAdminResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<PaymentAdminResponse>>builder()
                .status(200)
                .message("Danh sách giao dịch")
                .data(paymentAdminService.search(keyword, status, provider, fromDate, toDate, pageable))
                .build();
    }

    /** 21.1.1: Chi tiết giao dịch kèm lịch sử log. */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyAuthority('payment:view', 'ticket:refund', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<PaymentAdminResponse> detail(@PathVariable UUID paymentId) {
        return ApiResponse.<PaymentAdminResponse>builder()
                .status(200)
                .message("Chi tiết giao dịch")
                .data(paymentAdminService.getDetail(paymentId))
                .build();
    }

    /** 21.3: Đối soát trạng thái với VNPay (querydr). */
    @PostMapping("/{paymentId}/reconcile")
    @PreAuthorize("hasAnyAuthority('payment:view', 'ticket:refund', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<ReconcileResponse> reconcile(@PathVariable UUID paymentId, HttpServletRequest request) {
        ReconcileResponse result = paymentAdminService.reconcile(paymentId, VnpayConfig.getIpAddress(request));
        return ApiResponse.<ReconcileResponse>builder()
                .status(200)
                .message(result.getMessage())
                .data(result)
                .build();
    }

    /** 15.3: Hoàn tiền qua VNPay cho đơn vé (gọi trước khi hủy vé ở booking-service). */
    @PostMapping("/refund/booking/{bookingId}")
    @PreAuthorize("hasAnyAuthority('ticket:refund', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<PaymentAdminResponse> refund(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        String actor = jwt == null ? null : (jwt.getClaimAsString("preferred_username") != null
                ? jwt.getClaimAsString("preferred_username") : jwt.getSubject());
        PaymentAdminResponse result = paymentAdminService.refundByBooking(bookingId, actor, VnpayConfig.getIpAddress(request));
        return ApiResponse.<PaymentAdminResponse>builder()
                .status(200)
                .message(result == null
                        ? "Đơn không có giao dịch thanh toán trực tuyến, không cần hoàn tiền"
                        : "Hoàn tiền VNPay thành công")
                .data(result)
                .build();
    }
}
