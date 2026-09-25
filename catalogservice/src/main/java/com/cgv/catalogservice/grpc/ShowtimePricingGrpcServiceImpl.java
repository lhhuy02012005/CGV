package com.cgv.catalogservice.grpc;

import com.cgv.catalogservice.entity.*;
import com.cgv.catalogservice.repository.SeatRepository;
import com.cgv.catalogservice.repository.ShowtimeRepository;
import com.cgv.commondto.grpc.SeatPricingInfo;
import com.cgv.commondto.grpc.ShowtimePricingGrpcServiceGrpc;
import com.cgv.commondto.grpc.ShowtimePricingRequest;
import com.cgv.commondto.grpc.ShowtimePricingResponse;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class ShowtimePricingGrpcServiceImpl extends ShowtimePricingGrpcServiceGrpc.ShowtimePricingGrpcServiceImplBase{
    ShowtimeRepository showtimeRepository;
    SeatRepository seatRepository;

    @Override
    @Transactional(readOnly = true)
    public void getShowtimePricing(ShowtimePricingRequest request, StreamObserver<ShowtimePricingResponse> responseObserver) {
        try {
            UUID showtimeId = UUID.fromString(request.getShowtimeId());
            Showtime showtime = showtimeRepository.findByIdWithDetails(showtimeId)
                    .orElseGet(() -> showtimeRepository.findById(showtimeId)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId)));

            Movie movie = showtime.getMovie();
            Room room = showtime.getRoom();
            Cinema cinema = room.getCinema();

            if (room.getStatus() == com.cgv.catalogservice.enums.RoomStatus.MAINTENANCE) {
                log.warn("gRPC Reject: Phòng chiếu {} đang trong trạng thái bảo trì", room.getName());
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                        .withDescription("Phòng chiếu '" + room.getName() + "' hiện đang bảo trì kỹ thuật, tạm ngưng nhận đặt vé!")
                        .asRuntimeException());
                return;
            }
            if (cinema.getStatus() != com.cgv.catalogservice.enums.CinemaStatus.ACTIVE) {
                log.warn("gRPC Reject: Rạp chiếu {} đang tạm ngưng hoạt động", cinema.getName());
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                        .withDescription("Rạp chiếu '" + cinema.getName() + "' hiện đang tạm ngưng hoạt động!")
                        .asRuntimeException());
                return;
            }

            List<Seat> seats = seatRepository.findByRoomIdWithSeatType(room.getId());

            List<SeatPricingInfo> seatProtos = seats.stream().map(seat ->
                    SeatPricingInfo.newBuilder()
                            .setSeatId(seat.getId().toString())
                            .setSeatLabel(seat.getRowChar() + seat.getSeatNumber())
                            .setSeatType(seat.getSeatType().getName().name())
                            .setSurcharge(seat.getSeatType().getSurcharge().doubleValue())
                            .setIsActive(seat.getIsActive() != null ? seat.getIsActive() : true)
                            .build()
            ).toList();

            ShowtimePricingResponse response = ShowtimePricingResponse.newBuilder()
                    .setShowtimeId(showtime.getId().toString())
                    .setMovieId(movie.getId().toString())
                    .setMovieTitle(movie.getTitle())
                    .setPosterUrl(movie.getPosterUrl() != null ? movie.getPosterUrl() : "")
                    .setCinemaId(cinema.getId().toString())
                    .setCinemaName(cinema.getName())
                    .setCinemaAddress(cinema.getAddress() != null ? cinema.getAddress() : "")
                    .setRoomName(room.getName())
                    .setBasePrice(showtime.getBasePrice().doubleValue())
                    .setStartTime(showtime.getStartTime().toString())
                    .setEndTime(showtime.getEndTime().toString())
                    .addAllSeats(seatProtos)
                    .setShowtimeStatus(showtime.getStatus() != null ? showtime.getStatus().name() : "SCHEDULED")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("gRPC Server: Đã gửi thông tin giá cho suất chiếu {}", showtimeId);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý gRPC getShowtimePricing: ", e);
            responseObserver.onError(e);
        }
    }
}
