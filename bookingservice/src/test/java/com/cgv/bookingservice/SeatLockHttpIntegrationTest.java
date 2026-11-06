package com.cgv.bookingservice;

import com.cgv.bookingservice.controller.SeatLockController;
import com.cgv.bookingservice.config.*;
import com.cgv.bookingservice.exception.GlobalExceptionHandler;
import com.cgv.bookingservice.repository.BookingSeatRepository;
import com.cgv.bookingservice.grpc.CatalogGrpcClient;
import com.cgv.bookingservice.service.*;
import com.cgv.bookingservice.service.impl.SeatLockServiceImpl;
import com.cgv.commondto.grpc.SeatPricingInfo;
import com.cgv.commondto.grpc.ShowtimePricingResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Embedded real HTTP + production controller/service/Lua + isolated Redis. DB query and STOMP are mocks. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,classes=SeatLockHttpIntegrationTest.Config.class,
 properties={"spring.profiles.active=","spring.flyway.enabled=false","management.endpoints.web.exposure.include=health,info,conditions"})
class SeatLockHttpIntegrationTest {
    @Configuration(proxyBeanMethods=false)
    @EnableAutoConfiguration(exclude={org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class,
            org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration.class})
    @Import({SeatLockController.class,SeatLockServiceImpl.class,SeatLockOwnership.class,GlobalExceptionHandler.class,SecurityConfig.class})
    static class Config {
        @Bean(destroyMethod="") org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory() { return SeatLockLuaIntegrationTest.factory; }
        @Bean StringRedisTemplate redis() { return SeatLockLuaIntegrationTest.redis; }
        @Bean DefaultRedisScript<Long> seatLockScript() { return new RedisScriptConfig().seatLockScript(); }
        @Bean BookingSeatRepository bookingSeats() { return mock(BookingSeatRepository.class); }
        @Bean CatalogGrpcClient catalog() { return mock(CatalogGrpcClient.class); }
        @Bean SimpMessagingTemplate messaging() { return mock(SimpMessagingTemplate.class); }
        @Bean JwtDecoder jwtDecoder() { return token -> org.springframework.security.oauth2.jwt.Jwt.withTokenValue(token)
                .header("alg", "RS256").subject(token).build(); }
    }
    @DynamicPropertySource static void redis(DynamicPropertyRegistry registry) throws Exception { SeatLockLuaIntegrationTest.startRedis(); }
    @AfterAll static void cleanup() { SeatLockLuaIntegrationTest.stopRedis(); }
    @LocalServerPort int port;
    @Autowired BookingSeatRepository repository;
    @Autowired CatalogGrpcClient catalog;
    @Test void fiftyHttpRequestsReturnOne200AndFortyNine409WithTiming() throws Exception {
        when(repository.findBookedSeatIdsByShowtimeId(any(),any())).thenReturn(List.of());
        HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var conditions=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/actuator/conditions")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,conditions.statusCode());
        Files.writeString(Path.of("target/auto-configuration-conditions.json"),conditions.body());
        // Warm HTTP/JIT with unique seats so the measured contest starts from a free seat.
        for(int i=0;i<20;i++) { UUID showtime=UUID.randomUUID(), seat=UUID.randomUUID(); allow(showtime,"SCHEDULED",seat); send(client,showtime,seat,"warm-"+i); }
        UUID showtime=UUID.randomUUID(),seat=UUID.randomUUID();
        allow(showtime,"SCHEDULED",seat);
        ExecutorService pool=Executors.newFixedThreadPool(50); CountDownLatch ready=new CountDownLatch(50),go=new CountDownLatch(1);
        record Sample(int status,double millis) {}
        List<Future<Sample>> futures=new ArrayList<>(); long wall=System.nanoTime();
        try {
            for(int i=0;i<50;i++) { String user="http-race-"+i;
                futures.add(pool.submit(() -> {ready.countDown(); assertTrue(go.await(10,TimeUnit.SECONDS)); long started=System.nanoTime();
                    var response=send(client,showtime,seat,user); return new Sample(response.statusCode(),(System.nanoTime()-started)/1_000_000.0); })); }
            assertTrue(ready.await(10,TimeUnit.SECONDS)); wall=System.nanoTime(); go.countDown();
            List<Sample> results=new ArrayList<>(); for(var f:futures) results.add(f.get(20,TimeUnit.SECONDS));
            double elapsed=(System.nanoTime()-wall)/1_000_000.0;
            assertEquals(1,results.stream().filter(s -> s.status()==200).count()); assertEquals(49,results.stream().filter(s -> s.status()==409).count());
            assertNotNull(SeatLockLuaIntegrationTest.redis.opsForValue().get(SeatLockLuaIntegrationTest.key(showtime,seat)));
            double[] times=results.stream().mapToDouble(Sample::millis).sorted().toArray();
            String json=String.format(Locale.ROOT,"{\"scope\":\"embedded HTTP controller+service+real Redis; DB and STOMP mocked; no Kong\",\"warmup\":20,\"requests\":50,\"http_200\":1,\"http_409\":49,\"elapsed_ms\":%.6f,\"p95_ms\":%.6f,\"p99_ms\":%.6f}%n",elapsed,times[47],times[49]);
            Files.writeString(Path.of("target/seat-lock-http.json"),json); System.out.println("HTTP_SEAT_RACE "+json);
        } finally {go.countDown(); pool.shutdownNow();}
    }
    @Test void rejectsWrongRoomInactiveAndCancelledBeforeWritingRedis() throws Exception {
        UUID showtime=UUID.randomUUID(),valid=UUID.randomUUID(),foreign=UUID.randomUUID(),inactive=UUID.randomUUID();
        allow(showtime,"SCHEDULED",valid);
        HttpClient client=HttpClient.newHttpClient();
        assertEquals(400,send(client,showtime,foreign,"other-room").statusCode());
        assertNull(SeatLockLuaIntegrationTest.redis.opsForValue().get(SeatLockLuaIntegrationTest.key(showtime,foreign)));
        assertEquals(400,send(client,showtime,List.of(valid,foreign),"mixed-room").statusCode());
        assertNull(SeatLockLuaIntegrationTest.redis.opsForValue().get(SeatLockLuaIntegrationTest.key(showtime,valid)));
        when(catalog.getShowtimePricing(showtime)).thenReturn(pricing(showtime,"SCHEDULED",false,inactive));
        assertEquals(400,send(client,showtime,inactive,"inactive").statusCode());
        assertNull(SeatLockLuaIntegrationTest.redis.opsForValue().get(SeatLockLuaIntegrationTest.key(showtime,inactive)));
        allow(showtime,"CANCELLED",valid);
        assertEquals(400,send(client,showtime,valid,"cancelled").statusCode());
        assertNull(SeatLockLuaIntegrationTest.redis.opsForValue().get(SeatLockLuaIntegrationTest.key(showtime,valid)));
    }
    @Test void bookedSeatStaysUnavailableAfterItsRedisKeyExpires() throws Exception {
        UUID showtime=UUID.randomUUID(),seat=UUID.randomUUID(); allow(showtime,"SCHEDULED",seat);
        when(repository.findBookedSeatIdsByShowtimeId(eq(showtime),any())).thenReturn(List.of(seat));
        String key=SeatLockLuaIntegrationTest.key(showtime,seat);
        SeatLockLuaIntegrationTest.redis.opsForValue().set(key,"first-owner",Duration.ofSeconds(1));
        for(int i=0;i<30 && Boolean.TRUE.equals(SeatLockLuaIntegrationTest.redis.hasKey(key));i++) Thread.sleep(50);
        assertNull(SeatLockLuaIntegrationTest.redis.opsForValue().get(key));
        assertEquals(409,send(HttpClient.newHttpClient(),showtime,seat,"second-owner").statusCode());
        assertNull(SeatLockLuaIntegrationTest.redis.opsForValue().get(key));
    }
    @Test void singleSeatCanBeHeldByOwnerReleasedThenHeldByAnotherAccount() throws Exception {
        UUID showtime=UUID.randomUUID(), seat=UUID.randomUUID(); allow(showtime,"SCHEDULED",seat);
        when(repository.findBookedSeatIdsByShowtimeId(any(),any())).thenReturn(List.of());
        HttpClient client=HttpClient.newHttpClient();
        String key=SeatLockLuaIntegrationTest.key(showtime,seat);
        assertEquals(200,send(client,showtime,seat,"first-owner").statusCode());
        assertEquals(200,send(client,showtime,seat,"first-owner").statusCode());
        assertEquals(409,send(client,showtime,seat,"second-owner").statusCode());
        assertEquals("first-owner",SeatLockLuaIntegrationTest.redis.opsForValue().get(key));
        assertEquals(200,release(client,showtime,seat,"second-owner").statusCode());
        assertEquals("first-owner",SeatLockLuaIntegrationTest.redis.opsForValue().get(key));
        assertEquals(200,release(client,showtime,seat,"first-owner").statusCode());
        assertNull(SeatLockLuaIntegrationTest.redis.opsForValue().get(key));
        assertEquals(200,send(client,showtime,seat,"second-owner").statusCode());
    }
    HttpResponse<String> release(HttpClient client,UUID showtime,UUID seat,String user) throws Exception {
        String body="{\"showtimeId\":\""+showtime+"\",\"seatIds\":[\""+seat+"\"]}";
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/bookings/seat-locks"))
                .header("Content-Type","application/json").header("Authorization","Bearer "+user)
                .method("DELETE",HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void twoSeatConflictWritesNoPartialLock() throws Exception {
        UUID showtime=UUID.randomUUID(),free=UUID.randomUUID(),taken=UUID.randomUUID();
        allow(showtime,"SCHEDULED",free,taken);
        String takenKey=SeatLockLuaIntegrationTest.key(showtime,taken);
        SeatLockLuaIntegrationTest.redis.opsForValue().set(takenKey,"first-owner",Duration.ofMinutes(1));
        var response=send(HttpClient.newHttpClient(),showtime,List.of(free,taken),"second-owner");
        assertEquals(409,response.statusCode());
        assertNull(SeatLockLuaIntegrationTest.redis.opsForValue().get(SeatLockLuaIntegrationTest.key(showtime,free)));
        assertEquals("first-owner",SeatLockLuaIntegrationTest.redis.opsForValue().get(takenKey));
    }
    @Test void moreThanEightDistinctSeatsAreRejectedBeforeRedis() throws Exception {
        UUID showtime=UUID.randomUUID();
        List<UUID> seats=java.util.stream.IntStream.range(0,9).mapToObj(index->UUID.randomUUID()).toList();
        allow(showtime,"SCHEDULED",seats.toArray(UUID[]::new));
        assertEquals(400,send(HttpClient.newHttpClient(),showtime,seats,"too-many").statusCode());
        for(UUID seat:seats) assertNull(SeatLockLuaIntegrationTest.redis.opsForValue().get(SeatLockLuaIntegrationTest.key(showtime,seat)));
    }
    void allow(UUID showtime,String status,UUID... seats) {
        when(catalog.getShowtimePricing(showtime)).thenReturn(pricing(showtime,status,true,seats));
    }
    ShowtimePricingResponse pricing(UUID showtime,String status,boolean active,UUID... seats) {
        var response=ShowtimePricingResponse.newBuilder().setShowtimeId(showtime.toString()).setShowtimeStatus(status);
        for(UUID seat:seats) response.addSeats(SeatPricingInfo.newBuilder().setSeatId(seat.toString()).setIsActive(active).build());
        return response.build();
    }
    HttpResponse<String> send(HttpClient client,UUID showtime,UUID seat,String user) throws Exception {
        return send(client,showtime,List.of(seat),user);
    }
    HttpResponse<String> send(HttpClient client,UUID showtime,List<UUID> seats,String user) throws Exception {
        String body="{\"showtimeId\":\""+showtime+"\",\"seatIds\":["+seats.stream().map(id->"\""+id+"\"").collect(java.util.stream.Collectors.joining(","))+"]}";
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/bookings/seat-locks"))
                .timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("Authorization","Bearer "+user)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
}
