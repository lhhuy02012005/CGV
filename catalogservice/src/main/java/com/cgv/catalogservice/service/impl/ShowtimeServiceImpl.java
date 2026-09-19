package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.showtime.ShowtimeCreateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeFilterRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.ShowtimeResponse;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.entity.Room;
import com.cgv.catalogservice.entity.Showtime;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import com.cgv.catalogservice.mapper.ShowtimeMapper;
import com.cgv.catalogservice.repository.MovieRepository;
import com.cgv.catalogservice.repository.RoomRepository;
import com.cgv.catalogservice.repository.SeatRepository;
import com.cgv.catalogservice.repository.ShowtimeRepository;
import com.cgv.catalogservice.service.ShowtimeService;
import com.cgv.catalogservice.specification.ShowtimeSpecification;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShowtimeServiceImpl implements ShowtimeService {

    ShowtimeRepository showtimeRepository;
    MovieRepository movieRepository;
    RoomRepository roomRepository;
    SeatRepository seatRepository;

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

            LocalDate showDate =
                    request.showDate() != null
                            ? request.showDate()
                            : showtime.getShowDate();

            LocalDateTime startTime =
                    request.startTime() != null
                            ? request.startTime()
                            : showtime.getStartTime();

            LocalDateTime endTime =
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
            LocalDate showDate,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc phải sau thời gian bắt đầu"
            );
        }

        if (!showDate.equals(startTime.toLocalDate())) {
            throw new IllegalArgumentException(
                    "Ngày chiếu phải trùng với ngày bắt đầu suất chiếu"
            );
        }
    }

    private boolean hasOverlappingShowtime(
            UUID roomId,
            LocalDate showDate,
            LocalDateTime startTime,
            LocalDateTime endTime,
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
}

