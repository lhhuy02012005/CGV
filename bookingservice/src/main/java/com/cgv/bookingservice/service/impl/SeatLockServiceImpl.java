package com.cgv.bookingservice.service.impl;

import com.cgv.bookingservice.dto.event.SeatRealtimeEvent;
import com.cgv.bookingservice.dto.request.SeatLockRequest;
import com.cgv.bookingservice.dto.response.SeatLockResponse;
import com.cgv.bookingservice.service.SeatLockService;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.cgv.bookingservice.repository.BookingSeatRepository;
import com.cgv.bookingservice.enums.BookingStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
@Slf4j(topic = "SEAT-LOCK")
public class SeatLockServiceImpl implements SeatLockService {
    StringRedisTemplate redisTemplate;
    DefaultRedisScript<Long> seatLockScript;
    SimpMessagingTemplate messagingTemplate;
    BookingSeatRepository bookingSeatRepository;
    long LOCK_TTL_SECONDS = 240; // Phase 1: 4 phút (240s) giữ ghế
    String seatLockKey = "showtime:lock:";

    @Value("${booking.seat-lock.cooldown-enabled:false}")
    @NonFinal
    boolean cooldownEnabled;

    @Override
    public SeatLockResponse lockSeats(String userId, SeatLockRequest request) {
        if (userId == null || userId.isBlank() || userId.equalsIgnoreCase("guest") || userId.equalsIgnoreCase("anonymous")) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Vui lòng đăng nhập tài khoản trước khi thực hiện giữ ghế!");
        }

        // 1. Kiểm tra Cool-Down (chỉ khi cooldown-enabled = true)
        if (cooldownEnabled) {
            String cooldownKey = "user:cooldown:" + userId;
            Long cooldownTtl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            if (cooldownTtl != null && cooldownTtl > 0) {
                long minutes = (cooldownTtl + 59) / 60;
                log.warn("User {} bị chặn do đang trong thời gian phạt cooldown: {}s", userId, cooldownTtl);
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Tài khoản của bạn tạm thời bị khóa tính năng giữ ghế trong " + minutes + " phút do để hết hạn giữ ghế quá nhiều lần liên tiếp!");
            }
        }

        // 2. Chống Spam: Mỗi tài khoản chỉ được mở 1 phiên giữ ghế duy nhất tại một thời điểm
        String userActiveKey = "user:active_showtime:" + userId;
        String existingShowtimeId = redisTemplate.opsForValue().get(userActiveKey);
        if (existingShowtimeId != null && !existingShowtimeId.equals(request.getShowtimeId().toString())) {
            log.warn("User {} đang giữ ghế ở suất chiếu {} khác suất {}", userId, existingShowtimeId, request.getShowtimeId());
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Tài khoản của bạn đang có phiên giữ ghế tại một suất chiếu khác. Mỗi tài khoản chỉ được giữ ghế tại 1 suất chiếu cùng lúc!");
        }

        // 3. Giới hạn số lượng ghế tối đa 8 ghế
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Danh sách ghế không được để trống!");
        }
        if (request.getSeatIds().size() > 8) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mỗi tài khoản chỉ được giữ tối đa 8 ghế trong một suất chiếu!");
        }

        String showtimePrefix = seatLockKey + request.getShowtimeId();

        List<String> keys = List.of(showtimePrefix);
        List<String> args = new ArrayList<>();
        args.add(userId);
        args.add(String.valueOf(LOCK_TTL_SECONDS));
        for(UUID seatId : request.getSeatIds()) {
            args.add(String.valueOf(seatId));
        }

        Long result = redisTemplate.execute(seatLockScript,keys,args.toArray());

        if(result == null || result == 0L) {
            log.warn("User {} lock ghế thất bại cho suất chiếu {}", userId, request.getShowtimeId());
            throw new BusinessException(ErrorCode.BAD_REQUEST , "Hàng ghế này đã có người khác chọn hoặc giữ chỗ!");
        }

        // Cập nhật phiên giữ ghế active của user trong Redis (TTL bằng thời gian giữ ghế)
        redisTemplate.opsForValue().set(userActiveKey, request.getShowtimeId().toString(), java.time.Duration.ofSeconds(LOCK_TTL_SECONDS));

        Instant expiresAt = Instant.now().plusSeconds(LOCK_TTL_SECONDS);
        log.info("User {} đã lock thành công {} ghế tới {}", userId, request.getSeatIds().size(), expiresAt);

        // Broadcast realtime WebSocket LOCK event
        broadcastSeatEvent("LOCK", request.getShowtimeId(), request.getSeatIds(), userId);

        return SeatLockResponse.builder()
                .showtimeId(request.getShowtimeId())
                .seatIds(request.getSeatIds())
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public void releaseSeats(String userId, SeatLockRequest request) {
        String showtimePrefix = seatLockKey + request.getShowtimeId();
        List<UUID> releasedSeatIds = new ArrayList<>();
        for(UUID seatId : request.getSeatIds()) {
            String lockKey = showtimePrefix + ":" + seatId;
            String currentHolder = redisTemplate.opsForValue().get(lockKey);
            if (userId != null && userId.equals(currentHolder)) {
                redisTemplate.delete(lockKey);
                releasedSeatIds.add(seatId);
            }
        }
        if (!releasedSeatIds.isEmpty()) {
            // Broadcast realtime WebSocket RELEASE event
            broadcastSeatEvent("RELEASE", request.getShowtimeId(), releasedSeatIds, userId);
        }

        // Kiểm tra xem user còn ghế nào khác trong suất chiếu này không, nếu không còn thì xóa active session
        String userActiveKey = "user:active_showtime:" + userId;
        String pattern = seatLockKey + request.getShowtimeId() + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        boolean stillHasSeats = false;
        if (keys != null && userId != null) {
            for (String k : keys) {
                if (userId.equals(redisTemplate.opsForValue().get(k))) {
                    stillHasSeats = true;
                    break;
                }
            }
        }
        if (!stillHasSeats) {
            redisTemplate.delete(userActiveKey);
        }
    }

    @Override
    public void reportExpiredLock(String userId, UUID showtimeId) {
        if (!cooldownEnabled || userId == null || userId.isBlank() || userId.equalsIgnoreCase("guest") || userId.equalsIgnoreCase("anonymous")) {
            return;
        }
        String expCountKey = "user:expired_count:" + userId;
        Long count = redisTemplate.opsForValue().increment(expCountKey);
        redisTemplate.expire(expCountKey, Duration.ofHours(1));
        log.warn("User {} để hết hạn phiên giữ ghế tại suất chiếu {}. Số lần vi phạm gần đây: {}", userId, showtimeId, count);

        if (count != null && count >= 3) {
            String cooldownKey = "user:cooldown:" + userId;
            redisTemplate.opsForValue().set(cooldownKey, "COOLDOWN_ACTIVE", java.time.Duration.ofMinutes(15));
            redisTemplate.delete(expCountKey);
            redisTemplate.delete("user:active_showtime:" + userId);
            log.warn("User {} đã vi phạm để hết hạn 3 lần liên tiếp. Kích hoạt Cool-Down 15 phút!", userId);
        }
    }

    @Override
    public List<UUID> getActiveLockedSeatIds(UUID showtimeId) {
        String pattern = seatLockKey + showtimeId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<UUID> lockedSeats = new ArrayList<>();
        String prefix = seatLockKey + showtimeId + ":";
        for (String key : keys) {
            try {
                String seatIdStr = key.substring(prefix.length());
                lockedSeats.add(UUID.fromString(seatIdStr));
            } catch (Exception e) {
                log.warn("Invalid seat key: {}", key);
            }
        }
        return lockedSeats;
    }

    @Override
    public List<UUID> getBookedSeatIds(UUID showtimeId) {
        if (showtimeId == null) {
            return Collections.emptyList();
        }
        List<BookingStatus> activeStatuses = List.of(
                BookingStatus.CONFIRMED,
                BookingStatus.PAYMENT_PENDING,
                BookingStatus.SEAT_RESERVED,
                BookingStatus.USED
        );
        return bookingSeatRepository.findBookedSeatIdsByShowtimeId(showtimeId, activeStatuses);
    }

    private void broadcastSeatEvent(String type, UUID showtimeId, List<UUID> seatIds, String userId) {
        try {
            SeatRealtimeEvent event = SeatRealtimeEvent.builder()
                    .type(type)
                    .showtimeId(showtimeId)
                    .seatIds(seatIds)
                    .userId(userId)
                    .timestamp(Instant.now())
                    .build();
            String destination = "/topic/showtimes." + showtimeId + ".seats";
            messagingTemplate.convertAndSend(destination, event);
            log.info("Broadcasted WebSocket event {} to {} for {} seats", type, destination, seatIds.size());
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket seat event: {}", e.getMessage());
        }
    }
}
