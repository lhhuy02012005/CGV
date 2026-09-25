package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.showtime.ShowtimeCreateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeFilterRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.*;
import com.cgv.catalogservice.entity.Cinema;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.entity.Room;
import com.cgv.catalogservice.entity.Showtime;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import com.cgv.catalogservice.mapper.ShowtimeMapper;
import com.cgv.catalogservice.repository.*;
import com.cgv.catalogservice.service.ShowtimeService;
import com.cgv.catalogservice.specification.ShowtimeSpecification;
import com.cgv.catalogservice.util.GeoUtils;
import com.cgv.catalogservice.util.PageResponseUtils;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j(topic = "SHOWTIME-SERVICE")
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShowtimeServiceImpl implements ShowtimeService {

    private static final ZoneId VIETNAM_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    ShowtimeRepository showtimeRepository;
    MovieRepository movieRepository;
    RoomRepository roomRepository;
    SeatRepository seatRepository;
    CinemaRepository cinemaRepository;

    ShowtimeMapper showtimeMapper;
    com.cgv.catalogservice.service.CatalogRealtimeService catalogRealtimeService;

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "showtimesByMovieAndDate",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "showtimesByCinemaAndDate",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "cinemaSchedule",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public ShowtimeResponse createShowtime(
            ShowtimeCreateRequest request
    ) {

        log.info("Creating showtime: movieId={}, roomId={}, showDate={}", request.movieId(), request.roomId(), request.showDate());

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy movie với id: "
                                        + request.movieId()
                        )
                );

        validateShowDateWithinMoviePeriod(
                movie,
                request.showDate()
        );

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy phòng với id: "
                                        + request.roomId()
                        )
                );

        validateShowtimeTime(
                request.showDate(),
                request.startTime(),
                request.endTime()
        );

        if (hasOverlappingShowtime(
                room.getId(),
                request.showDate(),
                request.startTime(),
                request.endTime(),
                null
        )) {
            throw new IllegalArgumentException(
                    "Phòng đã có suất chiếu trong khoảng thời gian này"
            );
        }

        Showtime showtime =
                showtimeMapper.toEntity(request);
        if (showtime.getStatus() == null) {
            showtime.setStatus(ShowtimeStatus.SCHEDULED);
        }

        showtime.setMovie(movie);
        showtime.setRoom(room);

        int availableSeats =
                (int) seatRepository.findByRoomId(room.getId())
                        .stream()
                        .filter(seat ->
                                Boolean.TRUE.equals(
                                        seat.getIsActive()
                                )
                        )
                        .count();

        showtime.setAvailableSeats(availableSeats);

        Showtime savedShowtime =
                showtimeRepository.save(showtime);

        try {
            catalogRealtimeService.broadcast("SHOWTIME_CHANGED", java.util.Map.of(
                    "showtimeId", savedShowtime.getId().toString(),
                    "movieId", savedShowtime.getMovie() != null ? savedShowtime.getMovie().getId().toString() : "",
                    "roomId", savedShowtime.getRoom() != null ? savedShowtime.getRoom().getId().toString() : "",
                    "action", "CREATED"
            ));
        } catch (Exception e) {
            log.warn("Failed to broadcast SHOWTIME_CHANGED on create: {}", e.getMessage());
        }

        return showtimeMapper.toResponse(savedShowtime);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "showtimesByMovieAndDate",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "showtimesByCinemaAndDate",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "cinemaSchedule",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public ShowtimeResponse updateShowtime(
            UUID showtimeId,
            ShowtimeUpdateRequest request
    ) {

        log.info("Updating showtime: showtimeId={}", showtimeId);

        Showtime showtime =
                showtimeRepository.findById(showtimeId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Không tìm thấy suất chiếu với id: "
                                                + showtimeId
                                )
                        );

        Movie movie = showtime.getMovie();
        Room room = showtime.getRoom();

        if (request.movieId() != null) {
            movie = movieRepository.findById(request.movieId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Không tìm thấy movie với id: "
                                            + request.movieId()
                            )
                    );
        }

        LocalDate showDate =
                request.showDate() != null
                        ? request.showDate()
                        : showtime.getShowDate();

        validateShowDateWithinMoviePeriod(movie, showDate);

        boolean roomChanged = false;

        if (request.roomId() != null) {
            room = roomRepository.findById(request.roomId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Không tìm thấy phòng với id: "
                                            + request.roomId()
                            )
                    );

            roomChanged =
                    !room.getId().equals(
                            showtime.getRoom().getId()
                    );
        }

        boolean timeChanged =
                request.showDate() != null
                        || request.startTime() != null
                        || request.endTime() != null;

        long activeBookings = showtimeRepository.countActiveBookingsByShowtimeId(showtimeId);
        if (activeBookings > 0) {
            if (request.movieId() != null && !request.movieId().equals(showtime.getMovie().getId())) {
                throw new IllegalStateException("Không thể đổi phim cho suất chiếu này vì đã có " + activeBookings + " vé đang được đặt/đã xuất bill!");
            }
            if (request.roomId() != null && !request.roomId().equals(showtime.getRoom().getId())) {
                throw new IllegalStateException("Không thể đổi phòng chiếu vì đã có " + activeBookings + " vé đang được đặt/đã xuất bill (sơ đồ ghế của khán giả đã chốt)! Vui lòng hủy suất (CANCELLED) để hoàn vé theo quy trình.");
            }
            boolean dateChanged = request.showDate() != null && !request.showDate().equals(showtime.getShowDate());
            boolean startChanged = request.startTime() != null && !request.startTime().equals(showtime.getStartTime());
            boolean endChanged = request.endTime() != null && !request.endTime().equals(showtime.getEndTime());
            if (dateChanged || startChanged || endChanged) {
                throw new IllegalStateException("Không thể đổi ngày/giờ chiếu vì đã có " + activeBookings + " vé đang được đặt/đã xuất bill. Vui lòng chuyển trạng thái suất sang CANCELLED để thực hiện quy trình hủy suất và hoàn tiền tự động!");
            }
        }

        boolean scheduleChanged =
                timeChanged || roomChanged;

        if (scheduleChanged) {

            Instant startTime =
                    request.startTime() != null
                            ? request.startTime()
                            : showtime.getStartTime();

            Instant endTime =
                    request.endTime() != null
                            ? request.endTime()
                            : showtime.getEndTime();

            if (timeChanged) {
                validateShowtimeTime(
                        showDate,
                        startTime,
                        endTime
                );
            }

            if (hasOverlappingShowtime(
                    room.getId(),
                    showDate,
                    startTime,
                    endTime,
                    showtimeId
            )) {
                throw new IllegalArgumentException(
                        "Phòng đã có suất chiếu trong khoảng thời gian này"
                );
            }
        }

        showtimeMapper.updateEntity(request, showtime);

        showtime.setMovie(movie);
        showtime.setRoom(room);

        if (roomChanged) {
            int availableSeats =
                    (int) seatRepository
                            .findByRoomId(room.getId())
                            .stream()
                            .filter(seat ->
                                    Boolean.TRUE.equals(
                                            seat.getIsActive()
                                    )
                            )
                            .count();

            showtime.setAvailableSeats(availableSeats);
        }

        showtimeRepository.saveAndFlush(showtime);

        try {
            catalogRealtimeService.broadcast("SHOWTIME_CHANGED", java.util.Map.of(
                    "showtimeId", showtimeId.toString(),
                    "movieId", showtime.getMovie() != null ? showtime.getMovie().getId().toString() : "",
                    "action", "UPDATED"
            ));
        } catch (Exception e) {
            log.warn("Failed to broadcast SHOWTIME_CHANGED on update: {}", e.getMessage());
        }

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "showtimesByMovieAndDate",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "showtimesByCinemaAndDate",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "cinemaSchedule",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public ShowtimeResponse updateShowtimeStatus(
            UUID showtimeId,
            ShowtimeUpdateStatusRequest request
    ) {

        log.info("Updating showtime status: showtimeId={}, status={}", showtimeId, request.status());

        Showtime showtime =
                showtimeRepository.findById(showtimeId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Không tìm thấy suất chiếu với id: "
                                                + showtimeId
                                )
                        );

        if (request.status() == ShowtimeStatus.CANCELLED) {
            if (showtime.getEndTime() != null && showtime.getEndTime().isBefore(Instant.now())) {
                throw new IllegalStateException("Không thể hủy suất chiếu đã kết thúc trong quá khứ!");
            }
        } else {
            if (hasOverlappingShowtime(
                    showtime.getRoom().getId(),
                    showtime.getShowDate(),
                    showtime.getStartTime(),
                    showtime.getEndTime(),
                    showtimeId
            )) {
                throw new IllegalArgumentException(
                        "Không thể kích hoạt suất chiếu vì phòng đã có suất chiếu khác trong khoảng thời gian này"
                );
            }
        }

        showtime.setStatus(request.status());

        showtimeRepository.saveAndFlush(showtime);

        try {
            catalogRealtimeService.broadcast("SHOWTIME_CHANGED", java.util.Map.of(
                    "showtimeId", showtimeId.toString(),
                    "movieId", showtime.getMovie() != null ? showtime.getMovie().getId().toString() : "",
                    "status", request.status() != null ? request.status().name() : "",
                    "action", "STATUS_UPDATED"
            ));
        } catch (Exception e) {
            log.warn("Failed to broadcast SHOWTIME_CHANGED on status update: {}", e.getMessage());
        }

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(
            UUID showtimeId
    ) {

        log.debug("Getting showtime by id: showtimeId={}", showtimeId);

        Showtime showtime =
                showtimeRepository.findById(showtimeId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Không tìm thấy suất chiếu với id: "
                                                + showtimeId
                                )
                        );

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Cacheable(
            value = "showtimesByMovieAndDate",
            key = "#movieId"
                    + " + ':date=' + #showDate"
                    + " + ':page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> getShowtimesByMovieAndDate(
            UUID movieId,
            LocalDate showDate,
            Pageable pageable
    ) {

        log.debug("Getting showtimes by movie and date: movieId={}, showDate={}, page={}, size={}", movieId, showDate, pageable.getPageNumber(), pageable.getPageSize());

        if (!movieRepository.existsById(movieId)) {
            throw new EntityNotFoundException(
                    "Không tìm thấy movie với id: " + movieId
            );
        }

        Specification<Showtime> spec =
                Specification.allOf(
                        ShowtimeSpecification.hasMovieId(movieId),
                        ShowtimeSpecification.hasShowDate(showDate)
                );

        return PageResponseUtils.findAllAndMap(
                p -> showtimeRepository.findAll(spec, p),
                pageable,
                showtimeMapper::toResponseList
        );
    }

    @Override
    @Cacheable(
            value = "showtimesByCinemaAndDate",
            key = "#cinemaId"
                    + " + ':date=' + #showDate"
                    + " + ':page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> getShowtimesByCinemaAndDate(
            UUID cinemaId,
            LocalDate showDate,
            Pageable pageable
    ) {

        log.debug("Getting showtimes by cinema and date: cinemaId={}, showDate={}, page={}, size={}", cinemaId, showDate, pageable.getPageNumber(), pageable.getPageSize());

        if (!cinemaRepository.existsById(cinemaId)) {
            throw new EntityNotFoundException(
                    "Không tìm thấy cinema với id: " + cinemaId
            );
        }

        Specification<Showtime> spec =
                Specification.allOf(
                        ShowtimeSpecification.hasCinemaId(cinemaId),
                        ShowtimeSpecification.hasShowDate(showDate)
                );

        return PageResponseUtils.findAllAndMap(
                p -> showtimeRepository.findAll(spec, p),
                pageable,
                showtimeMapper::toResponseList
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> getAllShowtimes(
            ShowtimeFilterRequest filter,
            Pageable pageable
    ) {

        log.debug("Getting all showtimes: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Specification<Showtime> spec =
                Specification.allOf(
                        ShowtimeSpecification.hasMovieId(
                                filter.movieId()
                        ),
                        ShowtimeSpecification.hasRoomId(
                                filter.roomId()
                        ),
                        ShowtimeSpecification.hasCinemaId(
                                filter.cinemaId()
                        ),
                        ShowtimeSpecification.hasShowDate(
                                filter.showDate()
                        ),
                        ShowtimeSpecification.showDateFrom(
                                filter.showDateFrom()
                        ),
                        ShowtimeSpecification.showDateTo(
                                filter.showDateTo()
                        ),
                        ShowtimeSpecification.startsAtOrAfter(
                                filter.startTimeFrom()
                        ),
                        ShowtimeSpecification.startsAtOrBefore(
                                filter.startTimeTo()
                        ),
                        ShowtimeSpecification.hasStatus(
                                filter.status()
                        ),
                        ShowtimeSpecification.hasFormat(
                                filter.format()
                        ),
                        ShowtimeSpecification.hasLanguage(
                                filter.language()
                        ),
                        ShowtimeSpecification.hasSubtitleLanguage(
                                filter.subtitleLanguage()
                        ),
                        ShowtimeSpecification.priceGreaterThanOrEqual(
                                filter.minPrice()
                        ),
                        ShowtimeSpecification.priceLessThanOrEqual(
                                filter.maxPrice()
                        ),
                        ShowtimeSpecification.hasAvailableSeats(
                                filter.available()
                        )
                );

        return PageResponseUtils.findAllAndMap(
                p -> showtimeRepository.findAll(spec, p),
                pageable,
                showtimeMapper::toResponseList
        );
    }

    private void validateShowtimeTime(
            LocalDate showDate,
            Instant startTime,
            Instant endTime
    ) {

        LocalDate today =
                LocalDate.now(VIETNAM_ZONE);

        if (showDate.isBefore(today)) {
            throw new IllegalArgumentException(
                    "Ngày chiếu không được nhỏ hơn ngày hiện tại"
            );
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc phải sau thời gian bắt đầu"
            );
        }

        LocalDate startDate =
                startTime.atZone(VIETNAM_ZONE)
                        .toLocalDate();

        if (!showDate.equals(startDate)) {
            throw new IllegalArgumentException(
                    "Ngày chiếu phải trùng với ngày bắt đầu suất chiếu"
            );
        }

        if (startTime.isBefore(Instant.now())) {
            throw new IllegalArgumentException(
                    "Thời gian bắt đầu suất chiếu không được ở trong quá khứ"
            );
        }
    }

    private boolean hasOverlappingShowtime(
            UUID roomId,
            LocalDate showDate,
            Instant startTime,
            Instant endTime,
            UUID excludedShowtimeId
    ) {

        Specification<Showtime> overlapSpec =
                (root, query, cb) -> {

                    var predicate = cb.and(
                            cb.equal(
                                    root.get("room").get("id"),
                                    roomId
                            ),
                            cb.equal(
                                    root.get("showDate"),
                                    showDate
                            ),
                            cb.lessThan(
                                    root.get("startTime"),
                                    endTime
                            ),
                            cb.greaterThan(
                                    root.get("endTime"),
                                    startTime
                            ),
                            cb.notEqual(
                                    root.get("status"),
                                    ShowtimeStatus.CANCELLED
                            )
                    );

                    if (excludedShowtimeId != null) {
                        predicate = cb.and(
                                predicate,
                                cb.notEqual(
                                        root.get("id"),
                                        excludedShowtimeId
                                )
                        );
                    }

                    return predicate;
                };

        return showtimeRepository.exists(overlapSpec);
    }

    private void validateShowDateWithinMoviePeriod(
            Movie movie,
            LocalDate showDate
    ) {
        if (movie.getReleaseDate() != null
                && showDate.isBefore(movie.getReleaseDate())) {
            throw new IllegalArgumentException(
                    "Ngày chiếu không được trước ngày phát hành phim"
            );
        }

        if (movie.getEndDate() != null
                && showDate.isAfter(movie.getEndDate())) {
            throw new IllegalArgumentException(
                    "Ngày chiếu không được sau ngày kết thúc chiếu phim"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MovieNearbyCinemasResponse getNearbyMovieShowtimes(
            UUID movieId,
            double latitude,
            double longitude,
            LocalDate date,
            Double radiusKm
    ) {
        return getMovieSchedule(movieId, date, null, latitude, longitude, radiusKm);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieNearbyCinemasResponse getMovieSchedule(
            UUID movieId,
            LocalDate date,
            Integer regionId,
            Double latitude,
            Double longitude,
            Double radiusKm
    ) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy movie với id: " + movieId));

        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        List<Showtime> showtimes = showtimeRepository.findActiveShowtimesByMovieAndDateRange(
                movieId,
                ShowtimeStatus.SCHEDULED,
                targetDate,
                targetDate
        );

        // Group showtimes by Cinema
        Map<Cinema, List<Showtime>> showtimesByCinema = showtimes.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getRoom().getCinema(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<CinemaWithShowtimesResponse> cinemaResponses = showtimesByCinema.entrySet().stream()
                .map(entry -> {
                    Cinema cinema = entry.getKey();
                    Double dist = null;
                    if (latitude != null && longitude != null && cinema.getLatitude() != null && cinema.getLongitude() != null) {
                        dist = GeoUtils.calculateDistanceInKm(latitude, longitude, cinema.getLatitude(), cinema.getLongitude());
                    }

                    Integer cRegionId = cinema.getRegion() != null ? cinema.getRegion().getId() : null;
                    String cRegionName = cinema.getRegion() != null ? cinema.getRegion().getName() : null;

                    List<ShowtimeSlotResponse> slotResponses = entry.getValue().stream()
                            .sorted(Comparator.comparing(Showtime::getStartTime))
                            .map(s -> ShowtimeSlotResponse.builder()
                                    .showtimeId(s.getId())
                                    .roomId(s.getRoom().getId())
                                    .roomName(s.getRoom().getName())
                                    .startTime(s.getStartTime())
                                    .endTime(s.getEndTime())
                                    .format(s.getFormat())
                                    .viewingMode(s.getViewingMode())
                                    .language(s.getLanguage())
                                    .subtitleLanguage(s.getSubtitleLanguage())
                                    .basePrice(s.getBasePrice())
                                    .availableSeats(s.getAvailableSeats())
                                    .status(s.getStatus())
                                    .build())
                            .toList();

                    return CinemaWithShowtimesResponse.builder()
                            .cinemaId(cinema.getId())
                            .cinemaName(cinema.getName())
                            .address(cinema.getAddress())
                            .regionId(cRegionId)
                            .regionName(cRegionName)
                            .latitude(cinema.getLatitude())
                            .longitude(cinema.getLongitude())
                            .distanceInKm(dist)
                            .showtimes(slotResponses)
                            .build();
                })
                .filter(res -> regionId == null || (res.regionId() != null && res.regionId().equals(regionId)))
                .filter(res -> radiusKm == null || (res.distanceInKm() != null && res.distanceInKm() <= radiusKm))
                .sorted((a, b) -> {
                    if (latitude != null && longitude != null) {
                        double distA = a.distanceInKm() != null ? a.distanceInKm() : Double.MAX_VALUE;
                        double distB = b.distanceInKm() != null ? b.distanceInKm() : Double.MAX_VALUE;
                        return Double.compare(distA, distB);
                    }
                    return String.CASE_INSENSITIVE_ORDER.compare(
                            a.cinemaName() != null ? a.cinemaName() : "",
                            b.cinemaName() != null ? b.cinemaName() : ""
                    );
                })
                .toList();

        List<RegionResponse> availableRegions = showtimeRepository.findDistinctRegionsByMovie(
                movieId,
                ShowtimeStatus.SCHEDULED
        ).stream()
                .map(r -> new RegionResponse(r.getId(), r.getName(), r.getSlug()))
                .toList();

        return MovieNearbyCinemasResponse.builder()
                .movieId(movie.getId())
                .movieTitle(movie.getTitle())
                .posterUrl(movie.getPosterUrl())
                .date(targetDate)
                .availableRegions(availableRegions)
                .cinemas(cinemaResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "cinemaSchedule", key = "#cinemaId + ':' + (#date != null ? #date.toString() : 'today')")
    public CinemaScheduleResponse getCinemaSchedule(UUID cinemaId, LocalDate date) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cinema với id: " + cinemaId));

        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        List<Showtime> showtimes = showtimeRepository.findActiveShowtimesByCinemaAndDateRange(
                cinemaId,
                ShowtimeStatus.SCHEDULED,
                targetDate,
                targetDate
        );

        // Group showtimes by Movie
        Map<Movie, List<Showtime>> showtimesByMovie = showtimes.stream()
                .collect(Collectors.groupingBy(
                        Showtime::getMovie,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<MovieWithShowtimesResponse> movieResponses = showtimesByMovie.entrySet().stream()
                .map(entry -> {
                    Movie movie = entry.getKey();

                    List<ShowtimeSlotResponse> slotResponses = entry.getValue().stream()
                            .sorted(Comparator.comparing(Showtime::getStartTime))
                            .map(s -> ShowtimeSlotResponse.builder()
                                    .showtimeId(s.getId())
                                    .roomId(s.getRoom().getId())
                                    .roomName(s.getRoom().getName())
                                    .startTime(s.getStartTime())
                                    .endTime(s.getEndTime())
                                    .format(s.getFormat())
                                    .viewingMode(s.getViewingMode())
                                    .language(s.getLanguage())
                                    .subtitleLanguage(s.getSubtitleLanguage())
                                    .basePrice(s.getBasePrice())
                                    .availableSeats(s.getAvailableSeats())
                                    .status(s.getStatus())
                                    .build())
                            .toList();

                    return MovieWithShowtimesResponse.builder()
                            .movieId(movie.getId())
                            .movieTitle(movie.getTitle())
                            .posterUrl(movie.getPosterUrl())
                            .ageRating(movie.getAgeRating())
                            .durationMinutes(movie.getDurationMinutes())
                            .language(movie.getLanguage())
                            .supportedModes(movie.getSupportedModes())
                            .showtimes(slotResponses)
                            .build();
                })
                .toList();

        return CinemaScheduleResponse.builder()
                .cinemaId(cinema.getId())
                .cinemaName(cinema.getName())
                .address(cinema.getAddress())
                .latitude(cinema.getLatitude())
                .longitude(cinema.getLongitude())
                .date(targetDate)
                .movies(movieResponses)
                .build();
    }
}

