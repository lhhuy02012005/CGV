package com.cgv.notificationservice.consumer;

import com.cgv.commondto.event.NotificationEvent;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.notificationservice.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "NOTIFICATION-CONSUMER")
public class NotificationConsumer {
    EmailService emailService;

    @KafkaListener(topics = "notification.send", groupId = "${spring.kafka.consumer.group-id}")
    public void handleNotification(NotificationEvent event , Acknowledgment ack) {
        log.info("Nhận được event gửi OTP tới email: {}", event.getEmail());
        try {
            emailService.sendOtpEmail(event.getEmail(),event.getOtp());
            ack.acknowledge();
            log.info("Đã xử lý thành công và commit offset cho email: {}", event.getEmail());
        }catch (Exception e){
            log.error("Xử lý gửi email thất bại cho địa chỉ {}: {}", event.getEmail(), e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST,"Gửi thông báo OTP thất bại");
        }
    }
}
