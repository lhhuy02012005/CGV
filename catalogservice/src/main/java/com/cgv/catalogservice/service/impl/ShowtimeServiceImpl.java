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
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShowtimeServiceImpl implements ShowtimeService {

    ShowtimeRepository showtimeRepository;
    MovieRepository movieRepository;
    RoomRepository roomRepository;
    SeatRepository seatRepository;
    CinemaRepository cinemaRepository;

    ShowtimeMapper showtimeMapper;

    @Override
    @Transactional
    public ShowtimeResponse createShowtime(
            ShowtimeCreateRequest request
    ) {

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy movie với id: "
                                        + request.movieId()
                        )
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

        showtime.setMovie(movie);
        showtime.setRoom(room);

        int availableSeats =
                (int) seatRepository.findByRoom_Id(room.getId())
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

        return showtimeMapper.toResponse(savedShowtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse updateShowtime(
            UUID showtimeId,
            ShowtimeUpdateRequest request
    ) {

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

        boolean scheduleChanged =
                timeChanged || roomChanged;

        if (scheduleChanged) {

            Instant showDate =
                    request.showDate() != null
                            ? request.showDate()
                            : showtime.getShowDate();

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
                            .findByRoom_Id(room.getId())
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

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse updateShowtimeStatus(
            UUID showtimeId,
            ShowtimeUpdateStatusRequest request
    ) {

        Showtime showtime =
                showtimeRepository.findById(showtimeId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Không tìm thấy suất chiếu với id: "
                                                + showtimeId
                                )
                        );

        if (request.status() != ShowtimeStatus.CANCELLED) {

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

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(
            UUID showtimeId
    ) {

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
    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> getAllShowtimes(
            ShowtimeFilterRequest filter,
            Pageable pageable
    ) {

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

        Page<Showtime> showtimePage =
                showtimeRepository.findAll(spec, pageable);

        List<ShowtimeResponse> responses =
                showtimeMapper.toResponseList(
                        showtimePage.getContent()
                );

        return PageResponse.<ShowtimeResponse>builder()
                .data(responses)
                .pageNumber(showtimePage.getNumber() + 1)
                .pageSize(showtimePage.getSize())
                .totalPages(showtimePage.getTotalPages())
                .totalElements(showtimePage.getTotalElements())
                .build();
    }

    private void validateShowtimeTime(
            Instant showDate,
            Instant startTime,
            Instant endTime
    ) {

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc phải sau thời gian bắt đầu"
            );
        }
    }

    private boolean hasOverlappingShowtime(
            UUID roomId,
            Instant showDate,
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
        Instant startOfDay = targetDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Instant endOfDay = targetDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).minusNanos(1).toInstant();

        List<Showtime> showtimes = showtimeRepository.findActiveShowtimesByMovieAndDateRange(
                movieId,
                ShowtimeStatus.SCHEDULED,
                startOfDay,
                endOfDay
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
    public CinemaScheduleResponse getCinemaSchedule(UUID cinemaId, LocalDate date) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cinema với id: " + cinemaId));

        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Instant startOfDay = targetDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Instant endOfDay = targetDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).minusNanos(1).toInstant();

        List<Showtime> showtimes = showtimeRepository.findActiveShowtimesByCinemaAndDateRange(
                cinemaId,
                ShowtimeStatus.SCHEDULED,
                startOfDay,
                endOfDay
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

