package com.cgv.notificationservice.service.impl;

import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;
import brevoModel.SendSmtpEmailAttachment;
import com.cgv.commondto.event.BookingConfirmedEvent;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
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
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    @Override
    public void sendTicket(BookingConfirmedEvent booking) {
        if (booking.getBookingId() == null || booking.getUserEmail() == null || booking.getUserEmail().isBlank()) {
            throw new IllegalArgumentException("Thiếu mã vé hoặc email nhận vé");
        }
        try {
            String code = "CGV_TICKET_" + booking.getBookingId();
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(new QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 300, 300), "PNG", png);

            SendSmtpEmail message = new SendSmtpEmail();
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setName(senderName);
            sender.setEmail(senderEmail);
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(booking.getUserEmail());
            message.setSender(sender);
            message.setTo(List.of(recipient));
            message.setSubject("Vé xem phim CGV - " + booking.getBookingId());
            String showtime = booking.getShowtimeStart() == null ? "Xem tại quầy"
                    : DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                            .format(booking.getShowtimeStart());
            message.setHtmlContent("<p>Đặt vé thành công. Mã vé: <strong>" + code + "</strong></p>"
                    + "<p>Phim: " + escape(booking.getMovieTitle()) + "<br>Rạp: " + escape(booking.getCinemaName())
                    + "<br>Phòng: " + escape(booking.getRoomName()) + "<br>Suất chiếu: " + showtime
                    + "<br>Ghế: " + escape(String.join(", ", booking.getSeatLabels() == null ? List.of() : booking.getSeatLabels()))
                    + "</p><p>Ảnh QR đính kèm dùng để soát vé một lần. Vui lòng giữ kín mã vé.</p>");
            SendSmtpEmailAttachment attachment = new SendSmtpEmailAttachment();
            attachment.setName("CGV-" + booking.getBookingId() + ".png");
            attachment.setContent(png.toByteArray());
            message.setAttachment(List.of(attachment));
            new TransactionalEmailsApi().sendTransacEmail(message);
            log.info("Đã gửi vé điện tử cho đơn {}", booking.getBookingId());
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể gửi vé điện tử cho đơn " + booking.getBookingId(), exception);
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
