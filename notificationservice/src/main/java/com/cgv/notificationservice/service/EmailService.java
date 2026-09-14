package com.cgv.notificationservice.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp);
}
