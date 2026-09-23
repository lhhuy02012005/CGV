package com.cgv.bookingservice.service.impl;

import com.cgv.bookingservice.dto.request.BookingCreateRequest;
import com.cgv.bookingservice.dto.response.BookingResponse;
import com.cgv.bookingservice.entity.Booking;
import com.cgv.bookingservice.entity.BookingSeat;
import com.cgv.bookingservice.entity.OutboxEvent;
import com.cgv.bookingservice.enums.BookingStatus;
import com.cgv.bookingservice.grpc.CatalogGrpcClient;
import com.cgv.bookingservice.grpc.MarketingGrpcClient;
import com.cgv.bookingservice.repository.BookingRepository;
import com.cgv.bookingservice.repository.OutboxEventRepository;
import com.cgv.bookingservice.service.BookingService;
import com.cgv.commondto.event.BookingConfirmedEvent;
import com.cgv.commondto.event.BookingCreatedEvent;
import com.cgv.commondto.event.PaymentCompletedEvent;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.commondto.grpc.PromotionApplyResponse;
import com.cgv.commondto.grpc.SeatPricingInfo;
import com.cgv.commondto.grpc.ShowtimePricingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import com.cgv.bookingservice.dto.request.BookingFilterRequest;
import com.cgv.bookingservice.specification.BookingSpecification;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cgv.bookingservice.dto.event.SeatRealtimeEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class BookingServiceImpl implements BookingService {
    BookingRepository bookingRepository;
    OutboxEventRepository outboxEventRepository;
    CatalogGrpcClient catalogGrpcClient;
    StringRedisTemplate redisTemplate;
    KafkaTemplate<String , Object> kafkaTemplate;
    ObjectMapper objectMapper;
    MarketingGrpcClient marketingGrpcClient;
    SimpMessagingTemplate messagingTemplate;

    static String USER_TIER_KEY_PREFIX = "user:tier:";
    static String LOCK_KEY_PREFIX = "showtime:lock:";
    static int LOCK_KEY_DURATION = 10;
    static int PAYMENT_DEADLINE_DURATION = 10;

    @org.springframework.beans.factory.annotation.Value("${booking.seat-lock.cooldown-enabled:false}")
    @lombok.experimental.NonFinal
    boolean cooldownEnabled;

    @Override
    @Transactional
    public BookingResponse createBooking(String userId, BookingCreateRequest request) {
        UUID showtimeId = request.getShowtimeId();
        List<UUID> seatIds = request.getSeatIds();

        for (UUID seatId : seatIds) {
            String seatKey = LOCK_KEY_PREFIX + showtimeId + ":" + seatId;
            String currentHolder = redisTemplate.opsForValue().get(seatKey);
            if(currentHolder == null || !currentHolder.equals(userId)){
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Ghế đã hết hạn giữ hoặc không thuộc quyền sở hữu của bạn. Vui lòng chọn lại ghế!");
            }
        }

        ShowtimePricingResponse pricingResponse = catalogGrpcClient.getShowtimePricing(showtimeId);
        if(BookingStatus.CANCELLED.equals(pricingResponse.getShowtimeStatus())){
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Suất chiếu này đã bị huỷ bởi ban quản lý rạp!");
        }

        Map<String , SeatPricingInfo> seatInfoMap =  pricingResponse.getSeatsList().stream()
                .collect(Collectors.toMap(SeatPricingInfo::getSeatId , s->s));

        BigDecimal basePrice = BigDecimal.valueOf(pricingResponse.getBasePrice());
        BigDecimal totalBaseAmount = BigDecimal.ZERO;
        List<BookingSeat> bookingSeats = new ArrayList<>();

        for (UUID seatId : seatIds) {
            SeatPricingInfo info = seatInfoMap.get(seatId.toString());
            if(info == null){
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy thông tin ghế: " + seatId);
            }
            if (!info.getIsActive()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Ghế " + info.getSeatLabel() + " hiện đang được bảo trì kỹ thuật tại rạp, vui lòng chọn ghế khác!");
            }
            BigDecimal seatPrice = basePrice.add(BigDecimal.valueOf(info.getSurcharge()));
            totalBaseAmount = totalBaseAmount.add(seatPrice);
            BookingSeat bookingSeat = BookingSeat.builder()
                    .seatId(seatId)
                    .showtimeId(showtimeId)
                    .price(seatPrice)
                    .seatLabel(info.getSeatLabel())
                    .build();
            bookingSeats.add(bookingSeat);
        }

        String userTier = (String) redisTemplate.opsForValue().get(USER_TIER_KEY_PREFIX + userId);
        if (userTier == null) {
            userTier = "MEMBER";
        }
        log.info("Khách hàng {} có hạng thành viên: {}", userId, userTier);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if(request.getPromotionId() != null){
            PromotionApplyResponse promoRes = marketingGrpcClient.applyPromotion(
                    request.getPromotionId(),
                    userId,
                    userTier,
                    totalBaseAmount.doubleValue()
            );
            if (!promoRes.getIsValid()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã giảm giá không hợp lệ: " + promoRes.getMessage());
            }
            discountAmount = BigDecimal.valueOf(promoRes.getDiscountAmount());
            log.info("Áp dụng voucher {} thành công, giảm: {}", promoRes.getPromotionCode(), discountAmount);
        }

        BigDecimal finalAmount = totalBaseAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        Instant paymentDeadline = Instant.now().plus(Duration.ofMinutes(PAYMENT_DEADLINE_DURATION));

        Booking booking = Booking.builder()
                .userId(userId)
                .showtimeId(showtimeId)
                .promotionId(request.getPromotionId())
                .totalBaseAmount(totalBaseAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(BookingStatus.PAYMENT_PENDING)
                .paymentDeadline(paymentDeadline)
                .bookingSeats(bookingSeats)
                .build();

        for (BookingSeat bs : bookingSeats) {
            bs.setBooking(booking);
        }

        Booking savedBooking = bookingRepository.save(booking);

        // 5. GIA HẠN LOCK REDIS THÊM 10 PHÚT THANH TOÁN
        for (UUID seatId : seatIds) {
            String lockKey = LOCK_KEY_PREFIX + showtimeId + ":" + seatId;
            redisTemplate.expire(lockKey, Duration.ofMinutes(LOCK_KEY_DURATION));
        }
        redisTemplate.expire("user:active_showtime:" + userId, Duration.ofMinutes(LOCK_KEY_DURATION));
        redisTemplate.delete("user:expired_count:" + userId);

        try {
            BookingCreatedEvent event = BookingCreatedEvent.builder()
                    .bookingId(savedBooking.getId())
                    .userId(userId)
                    .showtimeId(showtimeId)
                    .totalAmount(finalAmount)
                    .paymentDeadline(paymentDeadline)
                    .seatIds(seatIds)
                    .createdAt(savedBooking.getCreatedAt())
                    .build();

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(savedBooking.getId().toString())
                    .aggregateType("BOOKING")
                    .eventType(BookingCreatedEvent.class.getSimpleName())
                    .payload(objectMapper.writeValueAsString(event))
                    .isPublished(false)
                    .retryCount(0)
                    .build();
            outboxEventRepository.save(outboxEvent);
        }catch (Exception e){
            log.error("Lỗi khi serialize OutboxEvent: ", e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lỗi khi tạo sự kiện đặt vé!");
        }

        List<String> seatLabels = bookingSeats.stream().map(BookingSeat::getSeatLabel).toList();
        BookingResponse.BookingResponseBuilder resBuilder = BookingResponse.builder()
                .bookingId(savedBooking.getId())
                .userId(userId)
                .showtimeId(showtimeId)
                .totalBaseAmount(totalBaseAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(savedBooking.getStatus())
                .paymentDeadline(paymentDeadline)
                .seatIds(seatIds)
                .seatLabels(seatLabels)
                .qrCodeUrl(savedBooking.getQrCodeUrl())
                .createdAt(savedBooking.getCreatedAt());

        enrichShowtimeDetails(resBuilder, showtimeId);
        return resBuilder.build();
    }

    @Override
    @Transactional
    public void confirmBooking(PaymentCompletedEvent event) {
        UUID bookingId = event.getBookingId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy đơn đặt vé: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.warn("Đơn đặt vé {} đã được xác nhận trước đó.", bookingId);
            return;
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setQrCodeUrl("https://api.cgv.vn/api/v1/tickets/qr/" + booking.getId());
        bookingRepository.save(booking);

        for(BookingSeat seat : booking.getBookingSeats()){
            String lockKey = LOCK_KEY_PREFIX + booking.getShowtimeId() + ":" + seat.getSeatId();
            redisTemplate.delete(lockKey);
        }
        log.info("Đã giải phóng lock Redis cho các ghế của Booking: {}", bookingId);

        // Broadcast WebSocket BOOKED event
        try {
            List<UUID> bookedSeatIds = booking.getBookingSeats().stream()
                    .map(BookingSeat::getSeatId)
                    .toList();
            SeatRealtimeEvent realtimeEvent = SeatRealtimeEvent.builder()
                    .type("BOOKED")
                    .showtimeId(booking.getShowtimeId())
                    .seatIds(bookedSeatIds)
                    .userId(booking.getUserId())
                    .timestamp(Instant.now())
                    .build();
            messagingTemplate.convertAndSend("/topic/showtimes." + booking.getShowtimeId() + ".seats", realtimeEvent);
        } catch (Exception e) {
            log.error("Failed to broadcast BOOKED event: {}", e.getMessage());
        }

        ShowtimePricingResponse pricing = catalogGrpcClient.getShowtimePricing(booking.getShowtimeId());
        List<String> seatLabels = booking.getBookingSeats().stream()
                .map(BookingSeat::getSeatLabel)
                .toList();
        try {
            BookingConfirmedEvent confirmedEvent = BookingConfirmedEvent.builder()
                    .bookingId(booking.getId())
                    .userId(booking.getUserId())
                    .movieTitle(pricing.getMovieTitle())
                    .posterUrl(pricing.getPosterUrl())
                    .cinemaName(pricing.getCinemaName())
                    .cinemaAddress(pricing.getCinemaAddress())
                    .roomName(pricing.getRoomName())
                    .showtimeStart(Instant.parse(pricing.getStartTime()))
                    .seatLabels(seatLabels)
                    .promotionId(booking.getPromotionId())
                    .discountAmount(booking.getDiscountAmount())
                    .totalAmount(booking.getFinalAmount())
                    .qrCodeUrl(booking.getQrCodeUrl())
                    .confirmedAt(Instant.now())
                    .build();
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(booking.getId().toString())
                    .aggregateType("BOOKING")
                    .eventType(BookingConfirmedEvent.class.getSimpleName())
                    .payload(objectMapper.writeValueAsString(confirmedEvent))
                    .isPublished(false)
                    .retryCount(0)
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Đã lưu OutboxEvent BookingConfirmedEvent cho NotificationService!");
            kafkaTemplate.send("booking.confirmed", confirmedEvent);
            log.info("Đã phát sự kiện booking.confirmed sang Kafka cho đơn vé: {}", booking.getId());
        } catch (Exception e) {
            log.error("Lỗi khi serialize BookingConfirmedEvent: ", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy đơn vé: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền xem đơn vé này!");
        }

        List<String> seatLabels = booking.getBookingSeats() != null
                ? booking.getBookingSeats().stream().map(BookingSeat::getSeatLabel).toList()
                : List.of();
        List<UUID> seatIds = booking.getBookingSeats() != null
                ? booking.getBookingSeats().stream().map(BookingSeat::getSeatId).toList()
                : List.of();

        BookingResponse.BookingResponseBuilder resBuilder = BookingResponse.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .showtimeId(booking.getShowtimeId())
                .totalBaseAmount(booking.getTotalBaseAmount())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount())
                .status(booking.getStatus())
                .paymentDeadline(booking.getPaymentDeadline())
                .seatIds(seatIds)
                .seatLabels(seatLabels)
                .qrCodeUrl(booking.getQrCodeUrl())
                .createdAt(booking.getCreatedAt());

        enrichShowtimeDetails(resBuilder, booking.getShowtimeId());
        return resBuilder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(String userId, BookingFilterRequest filter, Pageable pageable) {
        Specification<Booking> spec = BookingSpecification.filterMyBookings(userId, filter);
        Page<Booking> bookingPage = bookingRepository.findAll(spec, pageable);

        List<BookingResponse> bookingResponses = bookingPage.getContent().stream()
                .map(b -> {
                    List<String> seatLabels = b.getBookingSeats() != null
                            ? b.getBookingSeats().stream().map(BookingSeat::getSeatLabel).toList()
                            : List.of();
                    List<UUID> seatIds = b.getBookingSeats() != null
                            ? b.getBookingSeats().stream().map(BookingSeat::getSeatId).toList()
                            : List.of();

                    BookingResponse.BookingResponseBuilder resBuilder = BookingResponse.builder()
                            .bookingId(b.getId())
                            .userId(b.getUserId())
                            .showtimeId(b.getShowtimeId())
                            .totalBaseAmount(b.getTotalBaseAmount())
                            .discountAmount(b.getDiscountAmount())
                            .finalAmount(b.getFinalAmount())
                            .status(b.getStatus())
                            .paymentDeadline(b.getPaymentDeadline())
                            .seatIds(seatIds)
                            .seatLabels(seatLabels)
                            .qrCodeUrl(b.getQrCodeUrl())
                            .createdAt(b.getCreatedAt());

                    enrichShowtimeDetails(resBuilder, b.getShowtimeId());
                    return resBuilder.build();
                })
                .toList();

        return PageResponse.<BookingResponse>builder()
                .data(bookingResponses)
                .pageNumber(bookingPage.getNumber() + 1)
                .pageSize(bookingPage.getSize())
                .totalPages(bookingPage.getTotalPages())
                .totalElements(bookingPage.getTotalElements())
                .build();
    }

    private void enrichShowtimeDetails(BookingResponse.BookingResponseBuilder builder, UUID showtimeId) {
        if (showtimeId == null) return;
        try {
            ShowtimePricingResponse pricing = catalogGrpcClient.getShowtimePricing(showtimeId);
            if (pricing != null) {
                builder.movieTitle(pricing.getMovieTitle())
                        .posterUrl(pricing.getPosterUrl())
                        .cinemaName(pricing.getCinemaName())
                        .cinemaAddress(pricing.getCinemaAddress())
                        .roomName(pricing.getRoomName());
                if (pricing.getStartTime() != null && !pricing.getStartTime().isBlank()) {
                    try {
                        builder.showtimeStart(Instant.parse(pricing.getStartTime()));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("Không thể lấy thông tin showtime {} qua gRPC: {}", showtimeId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cancelBooking(UUID bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy đơn đặt vé: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền hủy đơn vé này!");
        }

        if (booking.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Chỉ có thể hủy đơn đặt vé đang ở trạng thái chờ thanh toán!");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        bookingRepository.save(booking);

        // Giải phóng lock ghế trong Redis ngay lập tức
        List<UUID> releasedSeatIds = new ArrayList<>();
        for (BookingSeat seat : booking.getBookingSeats()) {
            String lockKey = LOCK_KEY_PREFIX + booking.getShowtimeId() + ":" + seat.getSeatId();
            redisTemplate.delete(lockKey);
            releasedSeatIds.add(seat.getSeatId());
        }
        log.info("Khách hàng {} đã chủ động hủy đơn vé {} và giải phóng ghế Redis.", userId, bookingId);

        // Broadcast WebSocket RELEASE event
        try {
            SeatRealtimeEvent realtimeEvent = SeatRealtimeEvent.builder()
                    .type("RELEASE")
                    .showtimeId(booking.getShowtimeId())
                    .seatIds(releasedSeatIds)
                    .userId(userId)
                    .timestamp(Instant.now())
                    .build();
            messagingTemplate.convertAndSend("/topic/showtimes." + booking.getShowtimeId() + ".seats", realtimeEvent);
        } catch (Exception e) {
            log.error("Failed to broadcast RELEASE event: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void rollbackBooking(UUID bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            log.warn("Không tìm thấy đơn đặt vé để rollback: {}", bookingId);
            return;
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.warn("Đơn đặt vé {} đã được xác nhận thanh toán trước đó, không thể rollback.", bookingId);
            return;
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.info("Đơn đặt vé {} đã ở trạng thái hủy trước đó.", bookingId);
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        bookingRepository.save(booking);

        // 1. Giải phóng ghế trên Redis
        List<UUID> releasedSeatIds = new ArrayList<>();
        if (booking.getBookingSeats() != null) {
            for (BookingSeat seat : booking.getBookingSeats()) {
                String lockKey = LOCK_KEY_PREFIX + booking.getShowtimeId() + ":" + seat.getSeatId();
                redisTemplate.delete(lockKey);
                releasedSeatIds.add(seat.getSeatId());
            }
        }

        // 2. Xóa active showtime session của user
        redisTemplate.delete("user:active_showtime:" + booking.getUserId());

        // 3. Ghi nhận vi phạm để kích hoạt cooldown nếu quá 3 lần (chỉ khi cooldownEnabled = true)
        if (cooldownEnabled) {
            String expCountKey = "user:expired_count:" + booking.getUserId();
            Long count = redisTemplate.opsForValue().increment(expCountKey);
            redisTemplate.expire(expCountKey, Duration.ofHours(1));
            if (count != null && count >= 3) {
                redisTemplate.opsForValue().set("user:cooldown:" + booking.getUserId(), "COOLDOWN_ACTIVE", Duration.ofMinutes(15));
                redisTemplate.delete(expCountKey);
                log.warn("User {} đã vi phạm để hết hạn/hủy thanh toán 3 lần liên tiếp. Kích hoạt Cool-Down 15 phút!", booking.getUserId());
            }
        }

        log.info("⚡ [SAGA ROLLBACK] Đã tự động rollback đơn vé {} (lý do: {}) và giải phóng {} ghế trên Redis.",
                bookingId, reason, releasedSeatIds.size());

        // 4. Phát sự kiện WebSocket RELEASE cho tất cả client
        if (!releasedSeatIds.isEmpty()) {
            try {
                SeatRealtimeEvent realtimeEvent = SeatRealtimeEvent.builder()
                        .type("RELEASE")
                        .showtimeId(booking.getShowtimeId())
                        .seatIds(releasedSeatIds)
                        .userId(booking.getUserId())
                        .timestamp(Instant.now())
                        .build();
                messagingTemplate.convertAndSend("/topic/showtimes." + booking.getShowtimeId() + ".seats", realtimeEvent);
                log.info("Broadcasted WebSocket RELEASE event for rollback of {} seats", releasedSeatIds.size());
            } catch (Exception e) {
                log.error("Failed to broadcast RELEASE event during rollback: {}", e.getMessage());
            }
        }
    }
}
