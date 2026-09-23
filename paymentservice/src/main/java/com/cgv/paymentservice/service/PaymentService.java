package com.cgv.paymentservice.service;

import com.cgv.paymentservice.dto.PaymentStatusResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    String add(HttpServletRequest request , UUID bookingId, BigDecimal amount, String userId);
    boolean callback(HttpServletRequest request);
    PaymentStatusResponse getPaymentStatusByBookingId(UUID bookingId);
}
