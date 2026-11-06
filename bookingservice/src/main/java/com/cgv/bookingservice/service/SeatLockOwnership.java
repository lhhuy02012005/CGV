package com.cgv.bookingservice.service;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
public class SeatLockOwnership {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> releaseScript = listScript("seat_release.lua");
    private final DefaultRedisScript<Long> claimScript = new DefaultRedisScript<>();

    public SeatLockOwnership(StringRedisTemplate redis) {
        this.redis = redis;
        claimScript.setResultType(Long.class);
        claimScript.setLocation(new ClassPathResource("seat_claim.lua"));
    }
    private static DefaultRedisScript<List> listScript(String path) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setLocation(new ClassPathResource(path));
        return script;
    }
    private List<String> keys(UUID showtimeId, List<UUID> seatIds) {
        return seatIds.stream().distinct().map(id -> "showtime:lock:" + showtimeId + ":" + id).toList();
    }
    public void claim(String owner, UUID showtime, List<UUID> seats, long ttlSeconds) {
        Long result = redis.execute(claimScript, keys(showtime, seats), owner, Long.toString(ttlSeconds));
        if (result == null || result != 1L) {
            throw new BusinessException(ErrorCode.DUPLICATE, "Ghế đang được giữ bởi phiên khác!");
        }
    }
    public List<UUID> release(String owner, UUID showtime, List<UUID> seats) {
        return ids(redis.execute(releaseScript, keys(showtime, seats), owner));
    }
    private List<UUID> ids(List result) {
        if (result == null) return List.of();
        return ((List<?>) result).stream().map(Object::toString)
                .map(key -> UUID.fromString(key.substring(key.lastIndexOf(':') + 1))).toList();
    }
}
