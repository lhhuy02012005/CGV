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
    public void getShowtimePricing(ShowtimePricingRequest request, StreamObserver<ShowtimePricingResponse> responseObserver) {
        try {
            UUID showtimeId = UUID.fromString(request.getShowtimeId());
            Showtime showtime = showtimeRepository.findById(showtimeId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId));

            Movie movie = showtime.getMovie();
            Room room = showtime.getRoom();
            Cinema cinema = room.getCinema();

            List<Seat> seats = seatRepository.findByRoom_Id(room.getId());

            List<SeatPricingInfo> seatProtos = seats.stream().map(seat ->
                    SeatPricingInfo.newBuilder()
                            .setSeatId(seat.getId().toString())
                            .setSeatLabel(seat.getRowChar() + seat.getSeatNumber())
                            .setSeatType(seat.getSeatType().getName().name())
                            .setSurcharge(seat.getSeatType().getSurcharge().doubleValue())
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
