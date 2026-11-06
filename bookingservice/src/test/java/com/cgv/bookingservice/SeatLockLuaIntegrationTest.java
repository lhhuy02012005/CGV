package com.cgv.bookingservice;

import com.cgv.bookingservice.service.SeatLockOwnership;
import com.cgv.bookingservice.config.RedisScriptConfig;
import com.cgv.commondto.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.net.ServerSocket;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real isolated Redis, actual production Lua; no Docker or production data. */
class SeatLockLuaIntegrationTest {
    static Process process;
    static LettuceConnectionFactory factory;
    static StringRedisTemplate redis;
    static SeatLockOwnership ownership;
    static DefaultRedisScript<Long> lock;
    @BeforeAll static void startRedis() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        process = new ProcessBuilder(System.getenv().getOrDefault("REDIS_SERVER_BIN", "redis-server"),
                "--port", Integer.toString(port), "--bind", "127.0.0.1", "--save", "", "--appendonly", "no")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        factory = new LettuceConnectionFactory("127.0.0.1", port); factory.afterPropertiesSet(); factory.start();
        redis = new StringRedisTemplate(factory); redis.afterPropertiesSet();
        boolean ready = false;
        for (int i = 0; i < 100; i++) {
            try { try (var connection=redis.getConnectionFactory().getConnection()) { ready = "PONG".equals(connection.ping()); } if (ready) break; }
            catch (Exception ex) { Thread.sleep(25); }
        }
        assertTrue(ready, "Isolated Redis must start; this integration test is not silently skipped");
        ownership = new SeatLockOwnership(redis); lock = new RedisScriptConfig().seatLockScript();
    }
    @AfterAll static void stopRedis() {
        if (factory != null) factory.destroy();
        if (process != null) process.destroy();
    }
    static String key(UUID showtime, UUID seat) { return "showtime:lock:" + showtime + ":" + seat; }
    static long hold(String user, UUID showtime, UUID seat) {
        return redis.execute(lock, List.of("user:active_showtime:" + user, key(showtime, seat)), user, "240", showtime.toString());
    }
    @Test void fiftyDifferentOwnersProduceExactlyOneSuccessfulHold() throws Exception {
        UUID showtime = UUID.randomUUID(), seat = UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch ready = new CountDownLatch(50), go = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();
        long started = System.nanoTime();
        try {
            for (int i = 0; i < 50; i++) {
                String user = "race-" + i + "-" + showtime;
                futures.add(pool.submit(() -> { ready.countDown(); assertTrue(go.await(10, TimeUnit.SECONDS)); return hold(user, showtime, seat); }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS)); go.countDown();
            int success = 0, conflicts = 0;
            for (Future<Long> f : futures) { long result = f.get(10, TimeUnit.SECONDS); if (result == 1) success++; else if (result == 0) conflicts++; }
            assertEquals(1, success); assertEquals(49, conflicts);
            assertNotNull(redis.opsForValue().get(key(showtime, seat)));
            long ttl = redis.getExpire(key(showtime, seat)); assertTrue(ttl > 0 && ttl <= 240);
            System.out.printf("REDIS_RACE requests=50 success=%d conflicts=%d elapsed_ms=%.3f%n", success, conflicts, (System.nanoTime()-started)/1_000_000.0);
        } finally { go.countDown(); pool.shutdownNow(); }
    }
    @Test void oneSeatCanBeRenewedOnlyByItsOwnerAndExpires() throws Exception {
        UUID showtime = UUID.randomUUID(), seat = UUID.randomUUID();
        String owner = "owner-" + showtime;
        assertEquals(1, hold(owner, showtime, seat));
        assertEquals(1, hold(owner, showtime, seat));
        assertEquals(0, hold("contender-" + showtime, showtime, seat));
        assertEquals(owner, redis.opsForValue().get(key(showtime, seat)));
        redis.expire(key(showtime, seat), Duration.ofSeconds(1));
        for (int attempt = 0; attempt < 30 && Boolean.TRUE.equals(redis.hasKey(key(showtime, seat))); attempt++)
            Thread.sleep(50);
        assertNull(redis.opsForValue().get(key(showtime, seat)));
        assertEquals(1, hold("contender-" + showtime, showtime, seat));
    }
    @Test void ownerCannotHoldAnotherShowtimeUntilActiveShowtimeIsCleared() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), seat = UUID.randomUUID();
        String owner = "owner-" + first;
        assertEquals(1, hold(owner, first, seat));
        assertEquals(-1, hold(owner, second, seat));
        assertNull(redis.opsForValue().get(key(second, seat)));
        assertEquals(List.of(seat), ownership.release(owner, first, List.of(seat)));
        assertEquals(-1, hold(owner, second, seat));
        redis.delete("user:active_showtime:" + owner);
        assertEquals(1, hold(owner, second, seat));
    }
    @Test void staleOwnerCannotReleaseReplacementLock() {
        UUID showtime = UUID.randomUUID(), seat = UUID.randomUUID();
        redis.opsForValue().set(key(showtime, seat), "new-owner", Duration.ofSeconds(240));
        assertTrue(ownership.release("old-owner", showtime, List.of(seat)).isEmpty());
        assertEquals("new-owner", redis.opsForValue().get(key(showtime, seat)));
    }
    @Test void onlyOwnerCanRelease() {
        UUID showtime = UUID.randomUUID(), seat = UUID.randomUUID();
        redis.opsForValue().set(key(showtime, seat), "user-owned", Duration.ofSeconds(90));
        assertTrue(ownership.release("other-owner", showtime, List.of(seat)).isEmpty());
        assertEquals(List.of(seat), ownership.release("user-owned", showtime, List.of(seat)));
        assertNull(redis.opsForValue().get(key(showtime, seat)));
    }
    @Test void checkoutClaimDoesNotOverwriteAnotherUserAndIsAllOrNothing() {
        UUID showtime = UUID.randomUUID(), free = UUID.randomUUID(), taken = UUID.randomUUID();
        redis.opsForValue().set(key(showtime, taken), "other-owner", Duration.ofSeconds(90));
        assertThrows(BusinessException.class, () -> ownership.claim("owner", showtime, List.of(free, taken), 600));
        assertNull(redis.opsForValue().get(key(showtime, free)));
        assertEquals("other-owner", redis.opsForValue().get(key(showtime, taken)));
    }
    @Test void checkoutRequiresEveryLockToStillBelongToOwner() {
        UUID showtime = UUID.randomUUID(), held = UUID.randomUUID(), expired = UUID.randomUUID();
        redis.opsForValue().set(key(showtime, held), "owner", Duration.ofSeconds(90));
        assertThrows(BusinessException.class, () -> ownership.claim("owner", showtime, List.of(held, expired), 600));
        assertNull(redis.opsForValue().get(key(showtime, expired)));
        assertTrue(redis.getExpire(key(showtime, held)) <= 90);
        assertEquals("owner", redis.opsForValue().get(key(showtime, held)));
        redis.opsForValue().set(key(showtime, expired), "owner", Duration.ofSeconds(90));
        ownership.claim("owner", showtime, List.of(held, expired), 600);
        assertEquals("owner", redis.opsForValue().get(key(showtime, expired)));
    }
    @Test void warmScriptRoundTripTimingIsRecordedWithoutPerformanceThreshold() throws Exception {
        UUID showtime = UUID.randomUUID(), seat = UUID.randomUUID(); String user = "timing-" + showtime;
        for (int i=0;i<100;i++) assertEquals(1, hold(user, showtime, seat));
        double[] samples = new double[500];
        for (int i=0;i<samples.length;i++) { long t=System.nanoTime(); assertEquals(1, hold(user, showtime, seat)); samples[i]=(System.nanoTime()-t)/1_000_000.0; }
        Arrays.sort(samples); double avg=Arrays.stream(samples).average().orElseThrow();
        String json=String.format(Locale.ROOT,"{\"scope\":\"client RTT: StringRedisTemplate + actual Lua + local Redis\",\"warmup\":100,\"samples\":500,\"mean_ms\":%.6f,\"p95_ms\":%.6f,\"p99_ms\":%.6f}%n", avg,samples[474],samples[494]);
        Files.createDirectories(Path.of("target")); Files.writeString(Path.of("target/seat-lock-latency.json"),json);
        System.out.println("REDIS_LATENCY " + json);
    }
}
