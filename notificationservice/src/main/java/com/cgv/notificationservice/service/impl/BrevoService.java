package com.cgv.notificationservice.service.impl;

import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;
import com.cgv.notificationservice.service.EmailService;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import brevo.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j(topic = "BREVO-SERVICE")
public class BrevoService implements EmailService {

    @Value("${brevo.api-key}") String apiKey;
    @Value("${brevo.sender.name}") String senderName;
    @Value("${brevo.sender.email}") String senderEmail;
    @Value("${brevo.template-id}") Long templateId;
    @Value("${brevo.expiry-time}") int otpExpiry;

    @PostConstruct
    public void init() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKeyAuth.setApiKey(apiKey);
        log.info("Brevo Service initialized with Template ID: {}", templateId);
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();

        SendSmtpEmailSender sender = new SendSmtpEmailSender();
        sender.setName(senderName);
        sender.setEmail(senderEmail);

        SendSmtpEmailTo to = new SendSmtpEmailTo();
        to.setEmail(toEmail);


        Map<String, Object> params = new HashMap<>();
        params.put("OTP_CODE", otp);
        params.put("EXPIRE_MINUTES", otpExpiry);

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.setTemplateId(templateId);
        sendSmtpEmail.setSender(sender);
        sendSmtpEmail.setTo(Collections.singletonList(to));
        sendSmtpEmail.setParams(params);

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
            log.info("Email OTP đã được gửi tới {} qua Brevo Template {}", toEmail, templateId);
        }catch (Exception e) {
            log.error("Lỗi gửi Email Brevo: {}", e.getMessage());
            log.info("OTP dự phòng: {}", otp);
            throw new RuntimeException("Gửi Email qua Brevo thất bại!", e);
        }
    }
}
