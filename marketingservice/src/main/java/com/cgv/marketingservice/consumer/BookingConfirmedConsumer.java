package com.cgv.marketingservice.consumer;

import com.cgv.commondto.event.BookingConfirmedEvent;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.marketingservice.entity.PromotionUsage;
import com.cgv.marketingservice.repository.PromotionUsageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class BookingConfirmedConsumer {
    PromotionUsageRepository promotionUsageRepository;
    com.cgv.marketingservice.repository.PromotionRepository promotionRepository;

    @KafkaListener(topics = "booking.confirmed" , groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onBookingConfirmed(BookingConfirmedEvent event , Acknowledgment ack) {
        try {
            if(event.getPromotionId() != null){
                UUID bookingId = event.getBookingId();
                if(promotionUsageRepository.existsByBookingId(bookingId)){
                    log.warn("Đơn vé {} đã được lưu vết sử dụng mã khuyến mãi trước đó!", bookingId);
                    ack.acknowledge();
                    return;
                }
                PromotionUsage usage = PromotionUsage.builder()
                        .bookingId(bookingId)
                        .promotionId(event.getPromotionId())
                        .discountApplied(event.getDiscountAmount())
                        .userId(event.getUserId())
                        .usedAt(event.getConfirmedAt())
                        .build();
                promotionUsageRepository.save(usage);

                // Cập nhật Optimistic Locking decrement số lượng trong database
                int updatedRows = promotionRepository.decrementUsageLimit(event.getPromotionId());
                log.info("Thành công: Đã lưu vết sử dụng Voucher {} cho User {} với số tiền giảm {}. DB usage_limit updated: {}",
                        event.getPromotionId(), event.getUserId(), event.getDiscountAmount(), updatedRows > 0);
            }else {
                log.info("Đơn vé {} không sử dụng mã khuyến mãi nào, bỏ qua.", event.getBookingId());
            }
            ack.acknowledge();
        }catch (Exception e){
            log.error("Lỗi khi xử lý lưu vết PromotionUsage cho Booking {}: ", event.getBookingId(), e);
            throw new BusinessException(ErrorCode.BAD_REQUEST,"Lỗi khi xử lý lưu vết sử dụng voucher !");
        }
    }
}
