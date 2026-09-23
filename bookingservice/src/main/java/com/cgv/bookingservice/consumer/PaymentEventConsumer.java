package com.cgv.bookingservice.consumer;

import com.cgv.bookingservice.service.BookingService;
import com.cgv.commondto.event.PaymentCompletedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "BOOKING-PAYMENT-CONSUMER")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentEventConsumer {
    BookingService bookingService;

    @KafkaListener(topics = "payment.completed", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentCompleted(PaymentCompletedEvent event, Acknowledgment ack) {
        log.info("Nhận được PaymentCompletedEvent từ Kafka cho Booking: {}", event.getBookingId());
        try {
            bookingService.confirmBooking(event);
            ack.acknowledge();
            log.info("Đã commit Kafka offset cho đơn vé: {}", event.getBookingId());
        } catch (Exception e) {
            log.error("Lỗi khi xử lý PaymentCompletedEvent: ", e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentFailed(com.cgv.commondto.event.PaymentFailedEvent event, Acknowledgment ack) {
        log.warn("Nhận được PaymentFailedEvent từ Kafka cho Booking: {}, lý do: {}", event.getBookingId(), event.getFailureReason());
        try {
            bookingService.rollbackBooking(event.getBookingId(), event.getFailureReason());
            ack.acknowledge();
            log.info("Đã rollback đơn vé thành công và commit Kafka offset cho: {}", event.getBookingId());
        } catch (Exception e) {
            log.error("Lỗi khi xử lý rollback PaymentFailedEvent: ", e);
        }
    }
}
