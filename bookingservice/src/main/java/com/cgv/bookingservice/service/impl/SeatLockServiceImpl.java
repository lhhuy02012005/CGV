package com.cgv.bookingservice.service.impl;

import com.cgv.bookingservice.dto.request.SeatLockRequest;
import com.cgv.bookingservice.dto.response.SeatLockResponse;
import com.cgv.bookingservice.service.SeatLockService;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
@Slf4j(topic = "SEAT-LOCK")
public class SeatLockServiceImpl implements SeatLockService {
    StringRedisTemplate redisTemplate;
    DefaultRedisScript<Long> seatLockScript;
    static long LOCK_TTL_SECONDS = 300;
    static String seatLockKey = "showtime:lock:";

    @Override
    public SeatLockResponse lockSeats(String userId, SeatLockRequest request) {
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
            throw new BusinessException(ErrorCode.BAD_REQUEST , "Hàng ghế này đã có được chọn !");
        }
        Instant expiresAt = Instant.now().plusSeconds(LOCK_TTL_SECONDS);
        log.info("User {} đã lock thành công {} ghế tới {}", userId, request.getSeatIds().size(), expiresAt);

        return SeatLockResponse.builder()
                .showtimeId(request.getShowtimeId())
                .seatIds(request.getSeatIds())
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public void releaseSeats(String userId, SeatLockRequest request) {
        String showtimePrefix = seatLockKey + request.getShowtimeId();
        for(UUID seatId : request.getSeatIds()) {
            String lockKey = showtimePrefix + ":" + seatId;
            String currentHolder = redisTemplate.opsForValue().get(lockKey);
            if (userId.equals(currentHolder)) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}
