package com.cgv.bookingservice.service.impl;

import com.cgv.bookingservice.dto.event.SeatRealtimeEvent;
import com.cgv.bookingservice.dto.request.BookingCreateRequest;
import com.cgv.bookingservice.dto.request.BookingFilterRequest;
import com.cgv.bookingservice.dto.response.BookingResponse;
import com.cgv.bookingservice.dto.response.DashboardStatisticsResponse;
import com.cgv.bookingservice.entity.Booking;
import com.cgv.bookingservice.entity.BookingSeat;
import com.cgv.bookingservice.entity.OutboxEvent;
import com.cgv.bookingservice.entity.SeatClaim;
import com.cgv.bookingservice.enums.BookingStatus;
import com.cgv.bookingservice.grpc.CatalogGrpcClient;
import com.cgv.bookingservice.grpc.MarketingGrpcClient;
import com.cgv.bookingservice.repository.BookingRepository;
import com.cgv.bookingservice.repository.BookingSeatRepository;
import com.cgv.bookingservice.repository.OutboxEventRepository;
import com.cgv.bookingservice.service.BookingService;
import com.cgv.bookingservice.specification.BookingSpecification;
import com.cgv.commondto.dto.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j(topic = "BOOKING-SERVICE")
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingServiceImpl implements BookingService {
    BookingRepository bookingRepository;
    BookingSeatRepository bookingSeatRepository;
    OutboxEventRepository outboxEventRepository;
    CatalogGrpcClient catalogGrpcClient;
    StringRedisTemplate redisTemplate;
    com.cgv.bookingservice.repository.SeatClaimRepository seatClaimRepository;
    com.cgv.bookingservice.service.SeatLockOwnership seatLockOwnership;
    com.cgv.bookingservice.service.AfterCommit afterCommit;
    ObjectMapper objectMapper;
    MarketingGrpcClient marketingGrpcClient;
    SimpMessagingTemplate messagingTemplate;
    com.cgv.bookingservice.service.BookingRealtimeService bookingRealtimeService;
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    static String USER_TIER_KEY_PREFIX = "user:tier:";
    static int LOCK_KEY_DURATION = 10;
    static int PAYMENT_DEADLINE_DURATION = 10;

    @org.springframework.beans.factory.annotation.Value("${booking.seat-lock.cooldown-enabled:false}")
    @lombok.experimental.NonFinal
    boolean cooldownEnabled;

    private boolean hasUnsettledPayment(UUID bookingId) {
        // ponytail: Unresolved VNPay attempts keep seats until IPN/QUERYDR or operator reconciliation.
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM payments WHERE booking_id = ? AND provider = 'VNPAY' " +
                        "AND status IN ('PENDING', 'SUCCESS', 'REFUND_PENDING'))",
                Boolean.class, bookingId));
    }

    @Override
    @Transactional
    public BookingResponse createBooking(String userId, String userEmail, BookingCreateRequest request) {
        UUID showtimeId = request.getShowtimeId();
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()
                || request.getSeatIds().stream().anyMatch(Objects::isNull) || request.getShowtimeId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Suất chiếu và danh sách ghế phải hợp lệ!");
        }
        List<UUID> seatIds = request.getSeatIds().stream().distinct().toList();
        if (seatIds.size() > 8) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mỗi đơn chỉ được đặt tối đa 8 ghế!");
        }

        if (userId == null || userId.isBlank() || userId.startsWith("guest_") || "guest".equalsIgnoreCase(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Vui lòng đăng nhập để đặt vé");
        }

        // 1. Tự động dọn dẹp đơn cũ: Nếu user đã có đơn vé PAYMENT_PENDING ở chính suất chiếu này (ví dụ quay lại từ cổng VNPay để thanh toán lại),
        // hủy đơn cũ để giải phóng trạng thái trong DB trước khi tạo đơn mới
        List<Booking> oldPendingBookings = bookingRepository.findByUserIdAndShowtimeIdAndStatus(userId, showtimeId, BookingStatus.PAYMENT_PENDING);
        if (oldPendingBookings != null && !oldPendingBookings.isEmpty()) {
            for (Booking candidate : oldPendingBookings) {
                Booking oldB = bookingRepository.findByIdForUpdate(candidate.getId()).orElseThrow();
                if (oldB.getStatus() != BookingStatus.PAYMENT_PENDING) continue;
                if (hasUnsettledPayment(oldB.getId())) {
                    throw new BusinessException(ErrorCode.INVALID_OPERATION, "Đơn trước còn giao dịch VNPay cần đối soát; không thể giải phóng ghế");
                }
                oldB.setStatus(BookingStatus.CANCELLED);
                oldB.setCancelledAt(Instant.now());
                bookingRepository.save(oldB);
                seatClaimRepository.releaseByBookingId(oldB.getId());
                log.info("Tự động hủy đơn pending cũ {} của user {} để khởi tạo đơn mới", oldB.getId(), userId);
            }
        }

        // 2. Kiểm tra xem ghế đã bị ai mua hoặc ai khác đang thanh toán chưa
        List<UUID> activeBookedSeatIds = bookingSeatRepository.findBookedSeatIdsByShowtimeId(
                showtimeId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.PAYMENT_PENDING, BookingStatus.SEAT_RESERVED, BookingStatus.USED)
        );
        for (UUID seatId : seatIds) {
            if (activeBookedSeatIds.contains(seatId)) {
                throw new BusinessException(ErrorCode.DUPLICATE, "Ghế đã có người đặt hoặc đang trong quá trình thanh toán. Vui lòng chọn ghế khác!");
            }
        }

        ShowtimePricingResponse pricingResponse = catalogGrpcClient.getShowtimePricing(showtimeId);
        if (!"SCHEDULED".equals(pricingResponse.getShowtimeStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Suất chiếu không còn mở bán!");
        }

        Map<String, SeatPricingInfo> seatInfoMap = pricingResponse.getSeatsList().stream()
                .collect(Collectors.toMap(SeatPricingInfo::getSeatId, s -> s));

        BigDecimal basePrice = BigDecimal.valueOf(pricingResponse.getBasePrice());
        BigDecimal totalBaseAmount = BigDecimal.ZERO;
        List<BookingSeat> bookingSeats = new ArrayList<>();

        for (UUID seatId : seatIds) {
            SeatPricingInfo info = seatInfoMap.get(seatId.toString());
            if (info == null) {
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

        seatLockOwnership.claim(userId, showtimeId, seatIds,
                Duration.ofMinutes(LOCK_KEY_DURATION).toSeconds());

        String userTier = (String) redisTemplate.opsForValue().get(USER_TIER_KEY_PREFIX + userId);
        if (userTier == null) userTier = "MEMBER";
        log.info("Khách hàng {} có hạng thành viên: {}", userId, userTier);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getPromotionId() != null) {
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
                .guestEmail(userEmail)
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

        Booking savedBooking = bookingRepository.saveAndFlush(booking);
        for (UUID seatId : seatIds) {
            seatClaimRepository.saveAndFlush(SeatClaim.builder()
                    .showtimeId(showtimeId).seatId(seatId).bookingId(savedBooking.getId()).build());
        }

        redisTemplate.opsForValue().set("user:active_showtime:" + userId, showtimeId.toString(), Duration.ofMinutes(LOCK_KEY_DURATION));
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
        } catch (Exception e) {
            log.error("Lỗi khi serialize OutboxEvent: ", e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lỗi khi tạo sự kiện đặt vé!");
        }

        return mapToBookingResponse(savedBooking, null);
    }

    @Override
    @Transactional
    public void confirmBooking(PaymentCompletedEvent event) {
        UUID bookingId = event.getBookingId();

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy đơn đặt vé: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.USED) {
            log.warn("Đơn đặt vé {} đã được xác nhận trước đó.", bookingId);
            return;
        }
        if (booking.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Đơn vé không còn chờ thanh toán!");
        }
        if (event.getAmount() == null || event.getAmount().compareTo(booking.getFinalAmount()) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Số tiền thanh toán không khớp đơn vé!");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setQrCodeUrl("https://api.cgv.vn/api/v1/tickets/qr/" + booking.getId());
        bookingRepository.save(booking);

        afterCommit.run(() -> {
            seatLockOwnership.release(booking.getUserId(), booking.getShowtimeId(),
                    booking.getBookingSeats().stream().map(BookingSeat::getSeatId).toList());
            bookingRealtimeService.broadcastBooking(bookingId, "PAYMENT_CONFIRMED", Map.of(
                    "bookingId", bookingId.toString(), "status", "CONFIRMED",
                    "qrCodeUrl", booking.getQrCodeUrl(), "showtimeId", booking.getShowtimeId().toString(),
                    "finalAmount", booking.getFinalAmount()));
            SeatRealtimeEvent realtime = SeatRealtimeEvent.builder().type("BOOKED")
                    .showtimeId(booking.getShowtimeId()).userId(booking.getUserId())
                    .seatIds(booking.getBookingSeats().stream().map(BookingSeat::getSeatId).toList())
                    .timestamp(Instant.now()).build();
            messagingTemplate.convertAndSend("/topic/showtimes." + booking.getShowtimeId() + ".seats", realtime);
        });

        ShowtimePricingResponse pricing = catalogGrpcClient.getShowtimePricing(booking.getShowtimeId());
        List<String> seatLabels = booking.getBookingSeats().stream()
                .map(BookingSeat::getSeatLabel)
                .toList();
        try {
            String targetEmail = booking.getGuestEmail();
            BookingConfirmedEvent confirmedEvent = BookingConfirmedEvent.builder()
                    .bookingId(booking.getId())
                    .userId(booking.getUserId())
                    .userEmail(targetEmail)
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

        } catch (Exception e) {
            throw new IllegalStateException("Cannot persist BookingConfirmedEvent; transaction must roll back", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy đơn vé: " + bookingId));

        if (!Objects.equals(booking.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền xem đơn vé này!");
        }

        return mapToBookingResponse(booking, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(String userId, BookingFilterRequest filter, Pageable pageable) {
        Specification<Booking> spec = BookingSpecification.filterMyBookings(userId, filter);
        Page<Booking> bookingPage = bookingRepository.findAll(spec, pageable);

        Map<UUID, ShowtimePricingResponse> showtimeCache = new HashMap<>();

        List<BookingResponse> bookingResponses = bookingPage.getContent().stream()
                .map(b -> mapToBookingResponse(b, showtimeCache))
                .toList();

        return PageResponse.<BookingResponse>builder()
                .data(bookingResponses)
                .pageNumber(bookingPage.getNumber() + 1)
                .pageSize(bookingPage.getSize())
                .totalPages(bookingPage.getTotalPages())
                .totalElements(bookingPage.getTotalElements())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getPendingBooking(String userId, UUID showtimeId) {
        return bookingRepository.findFirstByUserIdAndShowtimeIdAndStatusOrderByCreatedAtDesc(
                        userId, showtimeId, BookingStatus.PAYMENT_PENDING)
                .map(booking -> mapToBookingResponse(booking, null))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> adminSearchBookings(String keyword, Pageable pageable) {
        Page<Booking> page = bookingRepository.searchBookingsByKeyword(keyword, pageable);
        Map<UUID, ShowtimePricingResponse> showtimeCache = new HashMap<>();
        List<BookingResponse> responses = page.getContent().stream()
                .map(b -> mapToBookingResponse(b, showtimeCache))
                .toList();

        return PageResponse.<BookingResponse>builder()
                .data(responses)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboard:statistics", allEntries = true)
    public BookingResponse adminCheckIn(UUID bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTED, "Không tìm thấy vé: " + bookingId));

        if (booking.getStatus() == BookingStatus.USED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Vé này đã được soát vé vào phòng chiếu trước đó!");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Chỉ có thể soát vé có trạng thái ĐÃ XÁC NHẬN (CONFIRMED). Trạng thái hiện tại: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.USED);
        Booking saved = bookingRepository.save(booking);
        log.info("Soát vé thành công cho đơn vé ID: {}", bookingId);

        try {
            bookingRealtimeService.broadcastGeneral("TICKET_CHECKED_IN", java.util.Map.of(
                    "bookingId", bookingId.toString(),
                    "status", "USED",
                    "checkedInAt", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to broadcast TICKET_CHECKED_IN: {}", e.getMessage());
        }

        return mapToBookingResponse(saved, null);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboard:statistics", allEntries = true)
    public BookingResponse adminRefund(UUID bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTED, "Không tìm thấy vé: " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Chỉ có thể hoàn tiền cho vé ĐÃ XÁC NHẬN (CONFIRMED)!");
        }

        seatClaimRepository.releaseByBookingId(bookingId);
        booking.setStatus(BookingStatus.REFUNDED);
        booking.setCancelledAt(Instant.now());
        Booking saved = bookingRepository.save(booking);

        try {
            bookingRealtimeService.broadcastGeneral("BOOKING_REFUNDED", java.util.Map.of(
                    "bookingId", bookingId.toString(),
                    "status", "REFUNDED"
            ));
        } catch (Exception e) {
            log.warn("Failed to broadcast BOOKING_REFUNDED: {}", e.getMessage());
        }

        // Giải phóng ghế
        for (BookingSeat seat : booking.getBookingSeats()) {
            seatLockOwnership.release(booking.getUserId(), booking.getShowtimeId(), List.of(seat.getSeatId()));
        }
        if (booking.getPromotionId() != null) {
            marketingGrpcClient.releasePromotion(booking.getPromotionId(), booking.getUserId());
        }

        log.info("Admin đã hoàn vé và giải phóng ghế cho đơn vé ID: {}", bookingId);
        return mapToBookingResponse(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard:statistics")
    public DashboardStatisticsResponse getDashboardStatistics() {
        Instant now = Instant.now();
        Instant startOfToday = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant startOfWeek = LocalDate.now().minusDays(7).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        List<BookingStatus> activePaidStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.USED);

        BigDecimal revenueToday = bookingRepository.sumRevenueByStatusesAndCreatedAtAfter(activePaidStatuses, startOfToday);
        BigDecimal revenueThisWeek = bookingRepository.sumRevenueByStatusesAndCreatedAtAfter(activePaidStatuses, startOfWeek);
        BigDecimal revenueTotal = bookingRepository.sumTotalRevenueByStatuses(activePaidStatuses);

        long ticketsSoldToday = bookingRepository.countBookingsByStatusesAndCreatedAtAfter(activePaidStatuses, startOfToday);
        long ticketsSoldTotal = bookingRepository.countBookingsByStatuses(activePaidStatuses);
        long activeBookingsCount = bookingRepository.countBookingsByStatuses(List.of(BookingStatus.CONFIRMED));

        Map<String, Long> statusBreakdown = new HashMap<>();
        List<Object[]> statusCounts = bookingRepository.countBookingsByStatus();
        if (statusCounts != null) {
            for (Object[] row : statusCounts) {
                if (row.length >= 2 && row[0] != null && row[1] != null) {
                    statusBreakdown.put(row[0].toString(), ((Number) row[1]).longValue());
                }
            }
        }

        List<DashboardStatisticsResponse.DailyRevenueItem> weeklyRevenue = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};

        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate targetDate = today.minusDays(i);
            Instant start = targetDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

            BigDecimal dayRev = bookingRepository.sumRevenueByStatusesAndCreatedAtAfter(activePaidStatuses, start);
            long dayCount = bookingRepository.countBookingsByStatusesAndCreatedAtAfter(activePaidStatuses, start);

            String dayLabel = dayNames[targetDate.getDayOfWeek().getValue() % 7];
            weeklyRevenue.add(com.cgv.bookingservice.dto.response.DashboardStatisticsResponse.DailyRevenueItem.builder()
                    .day(dayLabel)
                    .date(targetDate.toString())
                    .amount(dayRev != null ? dayRev.divide(BigDecimal.valueOf(1000000), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO)
                    .count(dayCount)
                    .build());
        }

        List<DashboardStatisticsResponse.TopMovieItem> topMovies = new ArrayList<>();
        List<Object[]> topShowtimes = bookingRepository.findTopShowtimesByBookings(activePaidStatuses, org.springframework.data.domain.PageRequest.of(0, 5));
        if (topShowtimes != null) {
            for (Object[] row : topShowtimes) {
                if (row.length >= 3 && row[0] != null) {
                    UUID stId = (UUID) row[0];
                    long count = ((Number) row[1]).longValue();
                    BigDecimal amt = (BigDecimal) row[2];

                    String movieTitle = "Phim CGV Cinema";
                    String posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300";
                    try {
                        ShowtimePricingResponse pr = catalogGrpcClient.getShowtimePricing(stId);
                        if (pr != null) {
                            if (pr.getMovieTitle() != null) movieTitle = pr.getMovieTitle();
                            if (pr.getPosterUrl() != null) posterUrl = pr.getPosterUrl();
                        }
                    } catch (Exception ignored) {}

                    topMovies.add(DashboardStatisticsResponse.TopMovieItem.builder()
                            .movieId(stId.toString())
                            .movieTitle(movieTitle)
                            .posterUrl(posterUrl)
                            .bookingCount(count)
                            .totalAmount(amt)
                            .build());
                }
            }
        }

        return DashboardStatisticsResponse.builder()
                .revenueToday(revenueToday != null ? revenueToday : BigDecimal.ZERO)
                .revenueThisWeek(revenueThisWeek != null ? revenueThisWeek : BigDecimal.ZERO)
                .revenueTotal(revenueTotal != null ? revenueTotal : BigDecimal.ZERO)
                .ticketsSoldToday(ticketsSoldToday)
                .ticketsSoldTotal(ticketsSoldTotal)
                .activeBookingsCount(activeBookingsCount)
                .statusBreakdown(statusBreakdown)
                .weeklyRevenue(weeklyRevenue)
                .topMovies(topMovies)
                .build();
    }

    private BookingResponse mapToBookingResponse(Booking b, Map<UUID, ShowtimePricingResponse> showtimeCache) {
        List<String> seatLabels = b.getBookingSeats() != null
                ? b.getBookingSeats().stream().map(BookingSeat::getSeatLabel).toList()
                : List.of();
        List<UUID> seatIds = b.getBookingSeats() != null
                ? b.getBookingSeats().stream().map(BookingSeat::getSeatId).toList()
                : List.of();

        BookingResponse.BookingResponseBuilder builder = BookingResponse.builder()
                .bookingId(b.getId())
                .userId(b.getUserId())
                .guestName(b.getGuestName())
                .guestEmail(b.getGuestEmail())
                .guestPhone(b.getGuestPhone())
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

        enrichShowtimeDetailsWithCache(builder, b.getShowtimeId(), showtimeCache);
        return builder.build();
    }

    private void enrichShowtimeDetailsWithCache(BookingResponse.BookingResponseBuilder builder, UUID showtimeId, Map<UUID, ShowtimePricingResponse> cache) {
        if (showtimeId == null) return;
        try {
            ShowtimePricingResponse pricing;
            if (cache != null) {
                pricing = cache.computeIfAbsent(showtimeId, id -> {
                    try {
                        return catalogGrpcClient.getShowtimePricing(id);
                    } catch (Exception e) {
                        log.warn("Không thể lấy thông tin showtime {} qua gRPC: {}", id, e.getMessage());
                        return null;
                    }
                });
            } else {
                pricing = catalogGrpcClient.getShowtimePricing(showtimeId);
            }

            if (pricing != null) {
                builder.movieTitle(pricing.getMovieTitle())
                        .posterUrl(pricing.getPosterUrl())
                        .cinemaName(pricing.getCinemaName())
                        .cinemaAddress(pricing.getCinemaAddress())
                        .roomName(pricing.getRoomName());
                if (pricing.getStartTime() != null && !pricing.getStartTime().isBlank()) {
                    try {
                        builder.showtimeStart(Instant.parse(pricing.getStartTime()));
                    } catch (Exception ex) {
                        log.warn("Cannot parse showtime start: {}", pricing.getStartTime());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể lấy thông tin showtime {} qua gRPC: {}", showtimeId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cancelBooking(UUID bookingId, String userId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy đơn đặt vé: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền hủy đơn vé này!");
        }

        if (booking.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Chỉ có thể hủy đơn vé đang chờ thanh toán!");
        }
        if (hasUnsettledPayment(bookingId)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Giao dịch VNPay còn chờ đối soát; không thể hủy vé");
        }

        seatClaimRepository.releaseByBookingId(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        bookingRepository.save(booking);

        List<UUID> releasedSeatIds = new ArrayList<>();
        for (BookingSeat seat : booking.getBookingSeats()) {
            seatLockOwnership.release(booking.getUserId(), booking.getShowtimeId(), List.of(seat.getSeatId()));
            releasedSeatIds.add(seat.getSeatId());
        }
        redisTemplate.delete("user:active_showtime:" + userId);
        if (booking.getPromotionId() != null) {
            marketingGrpcClient.releasePromotion(booking.getPromotionId(), userId);
        }
        log.info("Khách hàng {} đã chủ động hủy đơn vé {} và giải phóng ghế Redis.", userId, bookingId);

        // Broadcast WebSocket RELEASE event
        if (!releasedSeatIds.isEmpty()) {
            try {
                SeatRealtimeEvent realtimeEvent = SeatRealtimeEvent.builder()
                        .type("RELEASE")
                        .showtimeId(booking.getShowtimeId())
                        .seatIds(releasedSeatIds)
                        .userId(userId)
                        .timestamp(Instant.now())
                        .build();
                messagingTemplate.convertAndSend("/topic/showtimes." + booking.getShowtimeId() + ".seats", realtimeEvent);
                log.info("Broadcasted WebSocket RELEASE event for {} seats", releasedSeatIds.size());
            } catch (Exception e) {
                log.error("Failed to broadcast RELEASE event: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void rollbackBooking(UUID bookingId, String reason) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy đơn đặt vé: " + bookingId));

        if (booking.getStatus() != BookingStatus.PAYMENT_PENDING && booking.getStatus() != BookingStatus.SEAT_RESERVED) {
            log.warn("Đơn đặt vé {} đã bị hủy trước đó.", bookingId);
            return;
        }
        if (hasUnsettledPayment(bookingId)) {
            log.warn("Giữ ghế đơn {} quá hạn: giao dịch VNPay chưa đối soát; không tự bán lại", bookingId);
            return;
        }

        seatClaimRepository.releaseByBookingId(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        bookingRepository.save(booking);

        try {
            bookingRealtimeService.broadcastBooking(bookingId, "PAYMENT_FAILED", java.util.Map.of(
                    "bookingId", bookingId.toString(),
                    "status", "CANCELLED",
                    "reason", reason != null ? reason : "Hủy đơn đặt vé"
            ));
        } catch (Exception e) {
            log.warn("Failed to broadcast PAYMENT_FAILED: {}", e.getMessage());
        }

        List<UUID> releasedSeatIds = new ArrayList<>();
        for (BookingSeat seat : booking.getBookingSeats()) {
            seatLockOwnership.release(booking.getUserId(), booking.getShowtimeId(), List.of(seat.getSeatId()));
            releasedSeatIds.add(seat.getSeatId());
        }

        redisTemplate.delete("user:active_showtime:" + booking.getUserId());

        if (booking.getPromotionId() != null) {
            marketingGrpcClient.releasePromotion(booking.getPromotionId(), booking.getUserId());
        }

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

        log.info("[SAGA ROLLBACK] Đã tự động rollback đơn vé {} (lý do: {}) và giải phóng {} ghế trên Redis.",
                bookingId, reason, releasedSeatIds.size());

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
