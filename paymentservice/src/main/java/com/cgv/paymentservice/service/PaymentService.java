package com.cgv.paymentservice;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface PaymentService {
    String add(HttpServletRequest request , UUID bookingId);
    boolean callback(HttpServletRequest request);
}
