package com.cgv.identityservice.consumer;

import com.cgv.commondto.event.BookingConfirmedEvent;
import com.cgv.identityservice.entity.MemberShipTier;
import com.cgv.identityservice.entity.User;
import com.cgv.identityservice.repository.UserRepository;
import com.cgv.identityservice.service.MemberShipTierService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;

@Component
@Slf4j(topic = "USER-SPEND-CONSUMER")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserSpendConsumer {

    UserRepository userRepository;
    MemberShipTierService memberShipTierService;
    RedisTemplate<String, Object> redisTemplate;

    private static final String USER_TIER_KEY_PREFIX = "user:tier:";
    private static final long USER_TIER_KEY_DURATION = 24;

    @KafkaListener(topics = "booking.confirmed", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onBookingConfirmed(BookingConfirmedEvent event, Acknowledgment ack) {
        log.info("Nhận sự kiện booking.confirmed cho User: {}, số tiền: {}", event.getUserId(), event.getTotalAmount());
        try {
            if (event.getUserId() != null && event.getTotalAmount() != null) {
                userRepository.findById(event.getUserId()).ifPresent(user -> {
                    BigDecimal currentSpend = user.getTotal_spend_ytd() != null ? user.getTotal_spend_ytd() : BigDecimal.ZERO;
                    BigDecimal newSpend = currentSpend.add(event.getTotalAmount());
                    user.setTotal_spend_ytd(newSpend);

                    MemberShipTier newTier = memberShipTierService.determineTierBySpend(newSpend);
                    if (newTier != null && (user.getMembershipTier() == null || !newTier.getCode().equals(user.getMembershipTier().getCode()))) {
                        log.info("🎉 Chúc mừng User {} được nâng hạng từ {} lên {}",
                                user.getId(),
                                user.getMembershipTier() != null ? user.getMembershipTier().getCode() : "NONE",
                                newTier.getCode());
                        user.setMembershipTier(newTier);
                        redisTemplate.opsForValue().set(
                                USER_TIER_KEY_PREFIX + user.getId(),
                                newTier.getCode(),
                                Duration.ofHours(USER_TIER_KEY_DURATION)
                        );
                    }
                    userRepository.save(user);
                    log.info("Đã cập nhật tổng chi tiêu năm của User {}: {}", user.getId(), newSpend);
                });
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật chi tiêu và hạng cho User {}: ", event.getUserId(), e);
        }
    }
}
