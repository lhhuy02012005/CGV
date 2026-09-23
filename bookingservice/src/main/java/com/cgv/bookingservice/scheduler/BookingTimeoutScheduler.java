package com.cgv.bookingservice.scheduler;

import com.cgv.bookingservice.entity.Booking;
import com.cgv.bookingservice.enums.BookingStatus;
import com.cgv.bookingservice.repository.BookingRepository;
import com.cgv.bookingservice.service.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@EnableScheduling
@Slf4j(topic = "BOOKING-TIMEOUT-SCHEDULER")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class BookingTimeoutScheduler {
    BookingRepository bookingRepository;
    BookingService bookingService;

    @Scheduled(fixedRate = 30000)
    public void cleanupExpiredBookings() {
        Instant now = Instant.now();
        java.util.List<Booking> expiredBookings = bookingRepository.findByStatusAndPaymentDeadlineBefore(
                BookingStatus.PAYMENT_PENDING,
                now
        );
        if (expiredBookings != null && !expiredBookings.isEmpty()) {
            log.info("Tìm thấy {} đơn đặt vé quá hạn thanh toán. Đang tự động rollback và giải phóng ghế...", expiredBookings.size());
            for (Booking b : expiredBookings) {
                try {
                    bookingService.rollbackBooking(b.getId(), "HET_HAN_THANH_TOAN_10_PHUT");
                } catch (Exception e) {
                    log.error("Lỗi khi rollback đơn vé quá hạn {}: {}", b.getId(), e.getMessage());
                }
            }
        }
    }
}
